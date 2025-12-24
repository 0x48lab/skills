package com.hacklab.minecraft.skills.skill

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.entity.Player
import kotlin.random.Random

class SkillManager(private val plugin: Skills) {

    /**
     * Attempt to gain skill points
     * @return true if skill was gained
     */
    fun tryGainSkill(player: Player, skillType: SkillType, difficulty: Int): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val skillData = data.getSkill(skillType)
        val currentValue = skillData.value

        // Update last used time
        skillData.updateLastUsed()

        // Check if skill is already maxed
        if (currentValue >= SkillType.MAX_SKILL_VALUE) {
            return false
        }

        // Calculate gain chance
        val baseChance = (100 - currentValue) / 10.0  // Higher skill = lower chance
        val difficultyModifier = calculateDifficultyModifier(currentValue, difficulty.toDouble())
        val gainChance = (baseChance * difficultyModifier).coerceIn(0.1, 50.0)

        // Roll for skill gain
        if (Random.nextDouble() * 100 > gainChance) {
            return false
        }

        // Calculate gain amount
        val gainAmount = plugin.skillsConfig.skillGainAmount

        // Check total skill cap
        val totalSkills = data.getTotalSkillPoints()
        if (totalSkills + gainAmount > SkillType.TOTAL_SKILL_CAP) {
            // Need to decrease another skill
            val skillToDecrease = data.getLeastRecentlyUsedSkill(exclude = skillType)
            if (skillToDecrease != null) {
                val decreaseData = data.getSkill(skillToDecrease)
                if (decreaseData.value >= gainAmount) {
                    decreaseData.addValue(-gainAmount)
                    plugin.messageSender.send(
                        player, MessageKey.SKILL_DECREASE,
                        "skill" to skillToDecrease.displayName,
                        "amount" to gainAmount,
                        "current" to String.format("%.1f", decreaseData.value)
                    )
                } else {
                    // Not enough to decrease, cap reached
                    plugin.messageSender.send(player, MessageKey.SKILL_CAP_REACHED)
                    return false
                }
            } else {
                plugin.messageSender.send(player, MessageKey.SKILL_CAP_REACHED)
                return false
            }
        }

        // Apply skill gain
        skillData.addValue(gainAmount)
        data.dirty = true

        // Update max stats (STR affects HP, etc.)
        data.updateMaxStats()

        // Update attribute modifiers (DEX affects movement/attack speed)
        StatCalculator.applyAttributeModifiers(player, data)

        // Notify player
        plugin.messageSender.send(
            player, MessageKey.SKILL_GAIN,
            "skill" to skillType.displayName,
            "amount" to gainAmount,
            "current" to String.format("%.1f", skillData.value)
        )

        return true
    }

    /**
     * Calculate difficulty modifier for skill gain
     * Optimal when difficulty matches skill level
     */
    private fun calculateDifficultyModifier(skillValue: Double, difficulty: Double): Double {
        val difference = difficulty - skillValue
        return when {
            difference > 20 -> 0.5   // Too hard
            difference < -20 -> 0.2  // Too easy
            else -> 1.0              // Optimal range
        }
    }

    /**
     * Set a player's skill value directly (admin use)
     */
    fun setSkill(player: Player, skillType: SkillType, value: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.setSkillValue(skillType, value)
        data.updateMaxStats()
    }

    /**
     * Get a player's skill value
     */
    fun getSkillValue(player: Player, skillType: SkillType): Double {
        return plugin.playerDataManager.getPlayerData(player).getSkillValue(skillType)
    }

    /**
     * Check success based on skill vs difficulty
     * @return true if action succeeds
     */
    fun checkSuccess(player: Player, skillType: SkillType, difficulty: Int): Boolean {
        val skillValue = getSkillValue(player, skillType)
        val successChance = calculateSuccessChance(skillValue, difficulty.toDouble())
        return Random.nextDouble() * 100 < successChance
    }

    /**
     * Calculate success chance for an action
     */
    fun calculateSuccessChance(skillValue: Double, difficulty: Double): Double {
        // Base: skill value directly affects success
        // At skill 50 vs difficulty 50: 50% success
        // Each point difference shifts by ~1%
        val baseChance = 50.0 + (skillValue - difficulty)
        return baseChance.coerceIn(5.0, 95.0)
    }

    /**
     * Get combat skill for weapon type
     */
    fun getWeaponSkill(weaponType: WeaponType): SkillType {
        return when (weaponType) {
            WeaponType.SWORD -> SkillType.SWORDSMANSHIP
            WeaponType.AXE -> SkillType.AXE
            WeaponType.MACE -> SkillType.MACE_FIGHTING
            WeaponType.BOW, WeaponType.CROSSBOW -> SkillType.ARCHERY
            WeaponType.TRIDENT -> SkillType.THROWING
            WeaponType.FIST -> SkillType.WRESTLING
        }
    }
}

enum class WeaponType {
    SWORD, AXE, MACE, BOW, CROSSBOW, TRIDENT, FIST
}
