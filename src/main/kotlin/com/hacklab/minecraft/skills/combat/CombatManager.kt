package com.hacklab.minecraft.skills.combat

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatCalculator
import com.hacklab.minecraft.skills.util.WeaponUtil
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class CombatManager(private val plugin: Skills) {

    /**
     * Process melee attack from player to entity
     */
    fun processPlayerAttack(
        attacker: Player,
        target: LivingEntity,
        weapon: ItemStack?,
        baseDamage: Double
    ): AttackResult {
        val attackerData = plugin.playerDataManager.getPlayerData(attacker)

        // Get relevant skills
        val weaponSkillType = WeaponUtil.getWeaponSkillType(weapon)
        val weaponSkill = attackerData.getSkillValue(weaponSkillType)
        val tacticsSkill = attackerData.getSkillValue(SkillType.TACTICS)
        val anatomySkill = attackerData.getSkillValue(SkillType.ANATOMY)

        // Get weapon quality modifier
        val qualityModifier = plugin.qualityManager.getQualityModifier(weapon)

        // Calculate damage
        val damageResult = StatCalculator.calculateDamage(
            baseDamage = baseDamage,
            weaponSkill = weaponSkill,
            tacticsSkill = tacticsSkill,
            anatomySkill = anatomySkill,
            qualityModifier = qualityModifier
        )

        // Get difficulty based on target
        val difficulty = if (target is Player) {
            val targetData = plugin.playerDataManager.getPlayerData(target)
            val combatAvg = (targetData.getSkillValue(SkillType.SWORDSMANSHIP) +
                    targetData.getSkillValue(SkillType.TACTICS) +
                    targetData.getSkillValue(SkillType.PARRYING)) / 3
            MobDifficulty.getPlayerDifficulty(combatAvg)
        } else {
            MobDifficulty.getDifficulty(target.type)
        }

        // Try skill gains
        plugin.skillManager.tryGainSkill(attacker, weaponSkillType, difficulty)
        plugin.skillManager.tryGainSkill(attacker, SkillType.TACTICS, difficulty)

        // Notify if critical
        if (damageResult.isCritical) {
            plugin.messageSender.sendActionBar(attacker, MessageKey.COMBAT_CRITICAL)
        }

        return AttackResult(
            damage = damageResult.damage,
            isCritical = damageResult.isCritical,
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

        // Check parrying (only for physical damage with shield or weapon)
        if (!isMagicDamage) {
            val parryingSkill = defenderData.getSkillValue(SkillType.PARRYING)
            val parryChance = (parryingSkill / 2.0).coerceAtMost(plugin.skillsConfig.parryChanceMax)

            if (Random.nextDouble() * 100 < parryChance) {
                // Successful parry - reduce damage by 50%
                finalDamage *= 0.5
                plugin.messageSender.sendActionBar(defender, MessageKey.COMBAT_PARRY)

                // Try skill gain
                val difficulty = if (attacker is Player) {
                    val attackerData = plugin.playerDataManager.getPlayerData(attacker)
                    attackerData.getSkillValue(SkillType.TACTICS).toInt()
                } else if (attacker != null) {
                    MobDifficulty.getDifficulty(attacker.type)
                } else 30

                plugin.skillManager.tryGainSkill(defender, SkillType.PARRYING, difficulty)

                return DefenseResult(
                    damage = finalDamage,
                    wasParried = true,
                    wasResisted = false
                )
            }
        }

        // Check magic resistance
        if (isMagicDamage) {
            val resistSkill = defenderData.getSkillValue(SkillType.RESISTING_SPELLS)
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
     */
    fun processKill(killer: Player, victim: LivingEntity) {
        val difficulty = if (victim is Player) {
            val victimData = plugin.playerDataManager.getPlayerData(victim)
            val combatAvg = (victimData.getSkillValue(SkillType.SWORDSMANSHIP) +
                    victimData.getSkillValue(SkillType.TACTICS)) / 2
            MobDifficulty.getPlayerDifficulty(combatAvg)
        } else {
            MobDifficulty.getDifficulty(victim.type)
        }

        plugin.skillManager.tryGainSkill(killer, SkillType.ANATOMY, difficulty)
    }
}

data class AttackResult(
    val damage: Double,
    val isCritical: Boolean,
    val skillsUsed: List<SkillType>
)

data class DefenseResult(
    val damage: Double,
    val wasParried: Boolean,
    val wasResisted: Boolean
)
