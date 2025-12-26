package com.hacklab.minecraft.skills.combat

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * UO-style combat manager
 *
 * Attack Flow:
 * 1. Attack occurs
 * 2. Hit chance calculation (weapon skill vs target defense)
 *    - Miss → No damage, weapon skill gain check
 *    - Hit → Continue to step 3
 * 3. Damage calculation
 * 4. Defense reduction applied
 * 5. Final damage applied
 * 6. Skill gain check (Tactics on hit, Anatomy on kill)
 */
class CombatManager(private val plugin: Skills) {

    companion object {
        const val MIN_HIT_CHANCE = 5.0
        const val MAX_HIT_CHANCE = 95.0
        const val CRITICAL_MULTIPLIER = 2.0  // UO-style: crits deal double damage
    }

    /**
     * Calculate hit chance (UO formula)
     * Hit Rate = Weapon Skill - (Target Defense / 2) + 50
     * Range: 5% ~ 95%
     *
     * @param weaponSkill Attacker's weapon skill (0-100)
     * @param targetDefense Target's defense value (Mob's physical_defense or Player's Parrying)
     * @return Hit chance percentage (5-95)
     */
    fun calculateHitChance(weaponSkill: Double, targetDefense: Int): Double {
        val hitChance = weaponSkill - (targetDefense / 2.0) + 50.0
        return hitChance.coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)
    }

    /**
     * Roll hit chance
     * @return true if attack hits
     */
    fun rollHit(hitChance: Double): Boolean {
        return Random.nextDouble() * 100 < hitChance
    }

    /**
     * Calculate parry chance based on equipment (UO-style)
     * - Shield: Parrying * 0.5 (max 50%)
     * - Weapon only: Parrying * 0.25 (max 25%)
     * - Unarmed: 0%
     */
    fun calculateParryChance(player: Player, parryingSkill: Double): Double {
        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand

        // Check if holding a shield
        val hasShield = offHand.type == org.bukkit.Material.SHIELD

        // Check if holding a weapon
        val hasWeapon = plugin.combatConfig.isWeapon(mainHand)

        return when {
            hasShield -> {
                // Shield parry: skill * 0.5, max 50%
                (parryingSkill * 0.5).coerceAtMost(50.0)
            }
            hasWeapon -> {
                // Weapon parry: skill * 0.25, max 25%
                (parryingSkill * 0.25).coerceAtMost(25.0)
            }
            else -> {
                // Unarmed: cannot parry
                0.0
            }
        }
    }

    /**
     * Calculate damage (UO-style formula)
     *
     * Final Damage = Base Damage × Tactics Mod × Anatomy Mod × STR Mod × Quality Mod × DI Mod × Critical Mod
     * After Defense = Final Damage × (1 - Defense Reduction)
     * Defense Reduction = Target Defense / (Target Defense + 50)
     *
     * Tactics and Anatomy are ESSENTIAL skills for combat:
     * - Tactics 10, Anatomy 10: ~30 hits to kill skeleton
     * - Tactics 100, Anatomy 100: 1 hit to kill skeleton
     *
     * @param baseDamage Weapon's base damage (vanilla damage including crit bonus)
     * @param str Player's STR stat
     * @param tacticsSkill Player's Tactics skill (main damage multiplier: 0.1 to 1.0)
     * @param anatomySkill Player's Anatomy skill (damage multiplier: 0.6 to 1.5, also affects crit)
     * @param qualityModifier Weapon quality modifier (0.85 for LQ, 1.0 for NQ, 1.15 for HQ, 1.25 for EX)
     * @param diPercent Total DI percentage from enchantments
     * @param targetDefense Target's defense for reduction calculation
     * @return DamageResult with final damage and critical info
     */
    fun calculateDamage(
        baseDamage: Double,
        str: Int,
        tacticsSkill: Double,
        anatomySkill: Double,
        qualityModifier: Double,
        diPercent: Int,
        targetDefense: Int
    ): DamageResult {
        // Tactics modifier: Tactics / 100 → main damage multiplier (0.1 to 1.0)
        // Low Tactics = very low damage, High Tactics = full damage potential
        val tacticsMod = (tacticsSkill / 100.0).coerceIn(0.1, 1.0)

        // Anatomy modifier: 0.5 + (Anatomy / 100) → 0.6 to 1.5
        // Low Anatomy reduces damage, High Anatomy boosts damage
        val anatomyMod = 0.5 + (anatomySkill / 100.0)

        // STR modifier: 1 + (STR / 200) → STR 100 = +50%
        val strMod = 1.0 + (str / 200.0)

        // DI modifier: 1 + (DI% / 100)
        val diMod = 1.0 + (diPercent / 100.0)

        // Critical hit chance: Anatomy / 2 → Anatomy 100 = 50%
        val critChance = (anatomySkill / 2.0).coerceAtMost(plugin.skillsConfig.criticalChanceMax)
        val isCritical = Random.nextDouble() * 100 < critChance
        val critMod = if (isCritical) CRITICAL_MULTIPLIER else 1.0

        // Calculate raw damage before defense
        // Order: Base × Tactics × Anatomy × STR × Quality × DI × Crit
        val rawDamage = baseDamage * tacticsMod * anatomyMod * strMod * qualityModifier * diMod * critMod

        // Defense reduction: defense / (defense + 50)
        val defenseReduction = targetDefense / (targetDefense + 50.0)
        val finalDamage = rawDamage * (1.0 - defenseReduction)

        return DamageResult(
            damage = finalDamage,
            rawDamage = rawDamage,
            isCritical = isCritical,
            criticalChance = critChance,
            defenseReduction = defenseReduction * 100
        )
    }

    /**
     * Process melee attack from player to entity (UO style)
     */
    fun processPlayerAttack(
        attacker: Player,
        target: LivingEntity,
        weapon: ItemStack?,
        baseDamageOverride: Double? = null
    ): AttackResult {
        val attackerData = plugin.playerDataManager.getPlayerData(attacker)

        // Get weapon info from config
        val weaponStats = plugin.combatConfig.getWeaponStats(weapon)
        val weaponSkillType = weaponStats.skill
        val weaponSkill = attackerData.getSkillValue(weaponSkillType)

        // Get target defense for hit chance (Parrying for PvP)
        val hitDefense: Int
        // Get target defense for damage reduction (AR only for PvP - UO style)
        val damageDefense: Int

        if (target is Player) {
            // PvP UO-style:
            // - Hit chance: Parrying affects dodge
            // - Damage reduction: AR only (Parrying is separate block chance)
            val targetData = plugin.playerDataManager.getPlayerData(target)
            val parrying = targetData.getSkillValue(SkillType.PARRYING).toInt()
            val ar = plugin.armorManager.getTotalAR(target).toInt()

            hitDefense = parrying
            damageDefense = ar  // AR only, Parrying handled separately as block chance
        } else {
            // PvE: Use mob's physical_defense from config for both
            val mobDefense = plugin.combatConfig.getPhysicalDefense(target.type)
            hitDefense = mobDefense
            damageDefense = mobDefense
        }

        // Calculate hit chance
        val hitChance = calculateHitChance(weaponSkill, hitDefense)
        val isHit = rollHit(hitChance)

        // Get difficulty for skill gain (from mob stats or player skills)
        val difficulty = if (target is Player) {
            val targetData = plugin.playerDataManager.getPlayerData(target)
            val combatAvg = (targetData.getSkillValue(SkillType.SWORDSMANSHIP) +
                    targetData.getSkillValue(SkillType.TACTICS) +
                    targetData.getSkillValue(SkillType.PARRYING)) / 3
            combatAvg.toInt()
        } else {
            plugin.combatConfig.getDifficulty(target.type)
        }

        // Weapon skill gain: Always try on attack attempt (hit or miss)
        plugin.skillManager.tryGainSkill(attacker, weaponSkillType, difficulty)

        if (!isHit) {
            // Miss - no damage, send miss message
            plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_MISS)
            return AttackResult(
                damage = 0.0,
                isCritical = false,
                isHit = false,
                hitChance = hitChance,
                skillsUsed = listOf(weaponSkillType)
            )
        }

        // Hit - calculate damage

        // Get stats
        val str = attackerData.str
        val tacticsSkill = attackerData.getSkillValue(SkillType.TACTICS)
        val anatomySkill = attackerData.getSkillValue(SkillType.ANATOMY)

        // Get weapon quality modifier
        val qualityModifier = plugin.qualityManager.getQualityModifier(weapon)

        // Get DI from enchantments
        val diPercent = plugin.combatConfig.calculateTotalDI(weapon, target.type)

        // Get base damage (use vanilla damage if provided to preserve crit bonus)
        val baseDamage = baseDamageOverride ?: weaponStats.baseDamage.toDouble()

        // Calculate damage with defense reduction (uses damageDefense which includes AR)
        val damageResult = calculateDamage(
            baseDamage = baseDamage,
            str = str,
            tacticsSkill = tacticsSkill,
            anatomySkill = anatomySkill,
            qualityModifier = qualityModifier,
            diPercent = diPercent,
            targetDefense = damageDefense
        )

        // Tactics skill gain: Only on hit
        plugin.skillManager.tryGainSkill(attacker, SkillType.TACTICS, difficulty)

        // Notify if critical
        if (damageResult.isCritical) {
            plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_CRITICAL)
        }

        return AttackResult(
            damage = damageResult.damage,
            isCritical = damageResult.isCritical,
            isHit = true,
            hitChance = hitChance,
            skillsUsed = listOf(weaponSkillType, SkillType.TACTICS)
        )
    }

    /**
     * Process ranged attack (bow/crossbow) from player to entity
     */
    fun processRangedAttack(
        attacker: Player,
        target: LivingEntity,
        weapon: ItemStack?,
        projectileDamage: Double
    ): AttackResult {
        // For ranged, we use the projectile damage as base but still apply our modifiers
        // Hit is already determined by vanilla projectile hit detection
        val attackerData = plugin.playerDataManager.getPlayerData(attacker)

        val weaponStats = plugin.combatConfig.getWeaponStats(weapon)
        val weaponSkillType = weaponStats.skill
        val weaponSkill = attackerData.getSkillValue(weaponSkillType)

        // Get target defense for damage reduction (AR only for PvP - UO style)
        val targetDefense = if (target is Player) {
            // PvP UO-style: AR only for damage reduction
            // Parrying handled separately as block chance in processPlayerDefense
            plugin.armorManager.getTotalAR(target).toInt()
        } else {
            plugin.combatConfig.getPhysicalDefense(target.type)
        }

        val difficulty = if (target is Player) {
            val targetData = plugin.playerDataManager.getPlayerData(target)
            val combatAvg = (targetData.getSkillValue(SkillType.ARCHERY) +
                    targetData.getSkillValue(SkillType.TACTICS)) / 2
            combatAvg.toInt()
        } else {
            plugin.combatConfig.getDifficulty(target.type)
        }

        // Get stats
        val str = attackerData.str
        val tacticsSkill = attackerData.getSkillValue(SkillType.TACTICS)
        val anatomySkill = attackerData.getSkillValue(SkillType.ANATOMY)
        val qualityModifier = plugin.qualityManager.getQualityModifier(weapon)
        val diPercent = plugin.combatConfig.calculateTotalDI(weapon, target.type)

        // Use projectile damage as base (vanilla arrow damage varies by draw strength)
        val baseDamage = projectileDamage.coerceAtLeast(1.0)

        val damageResult = calculateDamage(
            baseDamage = baseDamage,
            str = str,
            tacticsSkill = tacticsSkill,
            anatomySkill = anatomySkill,
            qualityModifier = qualityModifier,
            diPercent = diPercent,
            targetDefense = targetDefense
        )

        // Skill gains for ranged (hit is confirmed by vanilla)
        plugin.skillManager.tryGainSkill(attacker, weaponSkillType, difficulty)
        plugin.skillManager.tryGainSkill(attacker, SkillType.TACTICS, difficulty)

        if (damageResult.isCritical) {
            plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_CRITICAL)
        }

        return AttackResult(
            damage = damageResult.damage,
            isCritical = damageResult.isCritical,
            isHit = true,
            hitChance = 100.0, // Already hit via vanilla
            skillsUsed = listOf(weaponSkillType, SkillType.TACTICS)
        )
    }

    /**
     * Process damage received by player
     */
    fun processPlayerDefense(
        defender: Player,
        attacker: Entity?,
        baseDamage: Double,
        isMagicDamage: Boolean = false
    ): DefenseResult {
        val defenderData = plugin.playerDataManager.getPlayerData(defender)
        var finalDamage = baseDamage

        // Check parrying (only for physical damage - UO style)
        if (!isMagicDamage) {
            val parryingSkill = defenderData.getSkillValue(SkillType.PARRYING)

            // UO-style parry chance depends on equipment
            val parryChance = calculateParryChance(defender, parryingSkill)

            if (parryChance > 0 && Random.nextDouble() * 100 < parryChance) {
                // Successful parry - reduce damage by 50%
                finalDamage *= 0.5
                plugin.messageSender.sendActionBar(defender, MessageKey.COMBAT_PARRY)

                // Try skill gain
                val difficulty = if (attacker is Player) {
                    val attackerData = plugin.playerDataManager.getPlayerData(attacker)
                    attackerData.getSkillValue(SkillType.TACTICS).toInt()
                } else if (attacker != null) {
                    plugin.combatConfig.getDifficulty(attacker.type)
                } else 30

                plugin.skillManager.tryGainSkill(defender, SkillType.PARRYING, difficulty)

                return DefenseResult(
                    damage = finalDamage,
                    wasParried = true,
                    wasResisted = false
                )
            }
            // Note: Parrying skill gain only happens on successful parry (above)
            // This is UO-style behavior where you only train by successfully parrying
        }

        // Check magic resistance
        if (isMagicDamage) {
            val resistSkill = defenderData.getSkillValue(SkillType.RESISTING_SPELLS)

            // Get magic defense from mob config for attacker
            val magicDefenseBonus = if (attacker != null && attacker !is Player) {
                // Use defender's resist skill, not attacker's magic defense
                0.0
            } else {
                0.0
            }

            val resistReduction = resistSkill / 2.0 / 100.0  // Max 50% reduction
            finalDamage *= (1.0 - resistReduction)

            if (resistReduction > 0.1) {
                plugin.messageSender.sendActionBar(defender, MessageKey.COMBAT_RESIST)
            }

            // Try skill gain
            plugin.skillManager.tryGainSkill(defender, SkillType.RESISTING_SPELLS, 50)

            return DefenseResult(
                damage = finalDamage,
                wasParried = false,
                wasResisted = true
            )
        }

        return DefenseResult(
            damage = finalDamage,
            wasParried = false,
            wasResisted = false
        )
    }

    /**
     * Apply internal damage to player
     */
    fun applyInternalDamage(player: Player, damage: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Convert to internal damage (vanilla * multiplier)
        val internalDamage = damage * plugin.skillsConfig.baseDamageMultiplier
        data.damage(internalDamage)

        // Sync to vanilla health
        StatCalculator.syncHealthToVanilla(player, data)
    }

    /**
     * Heal player's internal HP
     */
    fun healPlayer(player: Player, amount: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.heal(amount)
        StatCalculator.syncHealthToVanilla(player, data)
    }

    /**
     * Check if a kill grants Anatomy skill
     * Anatomy skill gain: Only on kill
     */
    fun processKill(killer: Player, victim: LivingEntity) {
        val difficulty = if (victim is Player) {
            val victimData = plugin.playerDataManager.getPlayerData(victim)
            val combatAvg = (victimData.getSkillValue(SkillType.SWORDSMANSHIP) +
                    victimData.getSkillValue(SkillType.TACTICS)) / 2
            combatAvg.toInt()
        } else {
            plugin.combatConfig.getDifficulty(victim.type)
        }

        plugin.skillManager.tryGainSkill(killer, SkillType.ANATOMY, difficulty)
    }
}

data class AttackResult(
    val damage: Double,
    val isCritical: Boolean,
    val isHit: Boolean,
    val hitChance: Double,
    val skillsUsed: List<SkillType>
)

data class DefenseResult(
    val damage: Double,
    val wasParried: Boolean,
    val wasResisted: Boolean
)

data class DamageResult(
    val damage: Double,
    val rawDamage: Double,
    val isCritical: Boolean,
    val criticalChance: Double,
    val defenseReduction: Double
)
