package com.hacklab.minecraft.skills.combat

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatCalculator
import com.hacklab.minecraft.skills.skill.WeaponType
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
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
        const val MOVEMENT_PENALTY = 0.7  // 30% damage reduction when moving while shooting
        const val MOVEMENT_THRESHOLD = 0.1  // Velocity threshold to consider "moving"
        const val STUN_DURATION_TICKS = 20  // 1 second
        const val STUN_COOLDOWN_MS = 5000L  // 5 seconds
    }

    // Track shooter movement state for projectiles (projectile UUID -> was moving)
    private val projectileMovementState = java.util.concurrent.ConcurrentHashMap<UUID, Boolean>()

    // Stun cooldown tracking: attacker UUID -> target UUID -> last stun time
    private val stunCooldowns = java.util.concurrent.ConcurrentHashMap<UUID, MutableMap<UUID, Long>>()

    /**
     * Record if player was moving when shooting a projectile
     */
    fun recordProjectileShot(projectileId: UUID, wasMoving: Boolean) {
        projectileMovementState[projectileId] = wasMoving
        // Clean up after 30 seconds to prevent memory leak
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            projectileMovementState.remove(projectileId)
        }, 600L)
    }

    /**
     * Check if shooter was moving when the projectile was shot
     */
    fun wasShooterMoving(projectileId: UUID): Boolean {
        return projectileMovementState.remove(projectileId) ?: false
    }

    /**
     * Check if player is currently moving (for shooting penalty)
     */
    fun isPlayerMoving(player: Player): Boolean {
        val velocity = player.velocity
        val horizontalSpeed = kotlin.math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
        return horizontalSpeed > MOVEMENT_THRESHOLD
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
     * Calculate parry chance for projectiles (arrows/bolts)
     * Only shields can block projectiles - weapons cannot parry arrows
     * - Shield: Parrying * 0.6 (max 60%) - slightly higher than melee
     * - No shield: 0%
     */
    fun calculateProjectileParryChance(player: Player, parryingSkill: Double): Double {
        val offHand = player.inventory.itemInOffHand

        // Only shields can block projectiles
        return if (offHand.type == org.bukkit.Material.SHIELD) {
            // Shield block: skill * 0.6, max 60%
            (parryingSkill * 0.6).coerceAtMost(60.0)
        } else {
            // Cannot block projectiles without shield
            0.0
        }
    }

    /**
     * Calculate damage (UO-style formula)
     *
     * Final Damage = Base Damage × Tactics Mod × Anatomy Mod × Wrestling Mod × STR Mod × Quality Mod × DI Mod × Critical Mod
     * After Defense = Final Damage × (1 - Defense Reduction)
     * Defense Reduction = Target Defense / (Target Defense + 50)
     *
     * Tactics and Anatomy are ESSENTIAL skills for combat:
     * - Tactics 10, Anatomy 10: ~30 hits to kill skeleton
     * - Tactics 100, Anatomy 100: 1 hit to kill skeleton
     *
     * Wrestling modifier (unarmed only):
     * - Skill 0: 0.5 (50% damage penalty)
     * - Skill 50: 1.0 (100% damage)
     * - Skill 100: 1.5 (150% damage bonus)
     *
     * @param baseDamage Weapon's base damage (vanilla damage including crit bonus)
     * @param str Player's STR stat
     * @param tacticsSkill Player's Tactics skill (main damage multiplier: 0.1 to 1.0)
     * @param anatomySkill Player's Anatomy skill (damage multiplier: 0.6 to 1.5, also affects crit)
     * @param qualityModifier Weapon quality modifier (0.85 for LQ, 1.0 for NQ, 1.15 for HQ, 1.25 for EX)
     * @param diPercent Total DI percentage from enchantments
     * @param targetDefense Target's defense for reduction calculation
     * @param wrestlingSkill Player's Wrestling skill (only applied when isUnarmed is true)
     * @param isUnarmed true if attacking with bare hands (applies Wrestling modifier)
     * @return DamageResult with final damage and critical info
     */
    fun calculateDamage(
        baseDamage: Double,
        str: Int,
        tacticsSkill: Double,
        anatomySkill: Double,
        qualityModifier: Double,
        diPercent: Int,
        targetDefense: Int,
        wrestlingSkill: Double = 0.0,
        isUnarmed: Boolean = false
    ): DamageResult {
        // Tactics modifier: Tactics / 100 → main damage multiplier (0.1 to 1.0)
        // Low Tactics = very low damage, High Tactics = full damage potential
        val tacticsMod = (tacticsSkill / 100.0).coerceIn(0.1, 1.0)

        // Anatomy modifier: 0.5 + (Anatomy / 100) → 0.6 to 1.5
        // Low Anatomy reduces damage, High Anatomy boosts damage
        val anatomyMod = 0.5 + (anatomySkill / 100.0)

        // Wrestling modifier: Only for unarmed combat
        // 0.5 + (skill / 100) → 0.5 to 1.5
        val wrestlingMod = if (isUnarmed) {
            0.5 + (wrestlingSkill / 100.0)
        } else {
            1.0  // No modifier for weapons
        }

        // STR modifier: 1 + (STR / 200) → STR 100 = +50%
        val strMod = 1.0 + (str / 200.0)

        // DI modifier: 1 + (DI% / 100)
        val diMod = 1.0 + (diPercent / 100.0)

        // Critical hit chance: Anatomy / 2 → Anatomy 100 = 50%
        val critChance = (anatomySkill / 2.0).coerceAtMost(plugin.skillsConfig.criticalChanceMax)
        val isCritical = Random.nextDouble() * 100 < critChance
        val critMod = if (isCritical) CRITICAL_MULTIPLIER else 1.0

        // Calculate raw damage before defense
        // Order: Base × Tactics × Anatomy × Wrestling × STR × Quality × DI × Crit
        val rawDamage = baseDamage * tacticsMod * anatomyMod * wrestlingMod * strMod * qualityModifier * diMod * critMod

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

        // Check if unarmed (true bare hands, not pickaxe/shovel/hoe)
        val isUnarmed = weaponStats.weaponType == WeaponType.FIST &&
                        (weapon == null || weapon.type == Material.AIR)
        val wrestlingSkill = if (isUnarmed) attackerData.getSkillValue(SkillType.WRESTLING) else 0.0

        // Calculate damage with defense reduction (uses damageDefense which includes AR)
        val damageResult = calculateDamage(
            baseDamage = baseDamage,
            str = str,
            tacticsSkill = tacticsSkill,
            anatomySkill = anatomySkill,
            qualityModifier = qualityModifier,
            diPercent = diPercent,
            targetDefense = damageDefense,
            wrestlingSkill = wrestlingSkill,
            isUnarmed = isUnarmed
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
     * Includes distance falloff and movement penalty for balance
     */
    fun processRangedAttack(
        attacker: Player,
        target: LivingEntity,
        weapon: ItemStack?,
        projectileDamage: Double,
        distance: Double = 0.0,
        wasMoving: Boolean = false
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

        // Apply distance falloff (damage reduction beyond 15 blocks)
        val distanceFalloff = calculateDistanceFalloff(distance)

        val damageResult = calculateDamage(
            baseDamage = baseDamage,
            str = str,
            tacticsSkill = tacticsSkill,
            anatomySkill = anatomySkill,
            qualityModifier = qualityModifier,
            diPercent = diPercent,
            targetDefense = targetDefense
        )

        // Apply movement penalty (30% damage reduction when shooting while moving)
        val movementMod = if (wasMoving) MOVEMENT_PENALTY else 1.0

        // Apply distance falloff and movement penalty to final damage
        val finalDamage = damageResult.damage * distanceFalloff * movementMod

        // Skill gains for ranged (hit is confirmed by vanilla)
        plugin.skillManager.tryGainSkill(attacker, weaponSkillType, difficulty)
        plugin.skillManager.tryGainSkill(attacker, SkillType.TACTICS, difficulty)

        if (damageResult.isCritical) {
            plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_CRITICAL)
        }

        return AttackResult(
            damage = finalDamage,
            isCritical = damageResult.isCritical,
            isHit = true,
            hitChance = 100.0, // Already hit via vanilla
            skillsUsed = listOf(weaponSkillType, SkillType.TACTICS)
        )
    }

    /**
     * Calculate distance falloff for ranged attacks
     * - 0-15 blocks: 100% damage
     * - 15-30 blocks: Linear falloff to 50%
     * - 30+ blocks: 50% damage (minimum)
     */
    fun calculateDistanceFalloff(distance: Double): Double {
        return when {
            distance <= 15.0 -> 1.0
            distance <= 30.0 -> 1.0 - ((distance - 15.0) / 30.0)  // 100% to 50%
            else -> 0.5
        }
    }

    /**
     * Process damage received by player
     * @param isProjectile true if damage is from arrow/bolt (can be parried with shield)
     */
    fun processPlayerDefense(
        defender: Player,
        attacker: Entity?,
        baseDamage: Double,
        isMagicDamage: Boolean = false,
        isProjectile: Boolean = false
    ): DefenseResult {
        val defenderData = plugin.playerDataManager.getPlayerData(defender)
        var finalDamage = baseDamage

        // Check parrying (only for physical damage - UO style)
        // Projectiles can be parried with shield only
        if (!isMagicDamage) {
            val parryingSkill = defenderData.getSkillValue(SkillType.PARRYING)

            // UO-style parry chance depends on equipment
            // For projectiles, only shields work (weapons cannot parry arrows)
            val parryChance = if (isProjectile) {
                calculateProjectileParryChance(defender, parryingSkill)
            } else {
                calculateParryChance(defender, parryingSkill)
            }

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

            val resistReduction = resistSkill * 0.7 / 100.0  // Max 70% reduction at skill 100
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

    /**
     * Attempt to stun target on unarmed hit
     * Stun chance = Wrestling skill / 4 (max 25% at skill 100)
     * Stun duration = 1 second (20 ticks)
     * Cooldown = 5 seconds per target
     *
     * @return true if stun was applied
     */
    fun tryStunOnUnarmedHit(
        attacker: Player,
        target: LivingEntity,
        wrestlingSkill: Double
    ): Boolean {
        // Calculate stun chance: skill / 4 (max 25% at skill 100)
        val stunChance = (wrestlingSkill / 4.0).coerceAtMost(25.0)

        if (stunChance <= 0) return false

        // Check cooldown
        if (isStunOnCooldown(attacker.uniqueId, target.uniqueId)) {
            return false
        }

        // Roll for stun
        if (Random.nextDouble() * 100 >= stunChance) {
            return false
        }

        // Apply stun (Slowness 255 = immobile)
        target.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                STUN_DURATION_TICKS,
                255,  // Level 256 = immobile
                false,
                false,
                true
            )
        )

        // Record cooldown
        recordStunCooldown(attacker.uniqueId, target.uniqueId)

        // Visual/audio feedback
        val loc = target.location.add(0.0, 1.0, 0.0)
        target.world.spawnParticle(Particle.CRIT, loc, 15, 0.3, 0.5, 0.3, 0.1)
        target.world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f)

        // Notify attacker
        plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_STUN)

        return true
    }

    private fun isStunOnCooldown(attackerId: UUID, targetId: UUID): Boolean {
        val targetCooldowns = stunCooldowns[attackerId] ?: return false
        val lastStun = targetCooldowns[targetId] ?: return false
        return System.currentTimeMillis() - lastStun < STUN_COOLDOWN_MS
    }

    private fun recordStunCooldown(attackerId: UUID, targetId: UUID) {
        stunCooldowns.getOrPut(attackerId) { mutableMapOf() }[targetId] = System.currentTimeMillis()
    }

    /**
     * Cleanup old cooldown entries (call periodically)
     */
    fun cleanupStunCooldowns() {
        val now = System.currentTimeMillis()
        stunCooldowns.entries.removeIf { (_, targets) ->
            targets.entries.removeIf { (_, time) -> now - time > STUN_COOLDOWN_MS * 2 }
            targets.isEmpty()
        }
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
