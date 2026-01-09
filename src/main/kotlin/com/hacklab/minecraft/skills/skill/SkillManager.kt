package com.hacklab.minecraft.skills.skill

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.GameMode
import org.bukkit.entity.Player
import kotlin.random.Random

class SkillManager(private val plugin: Skills) {

    /**
     * Attempt to gain skill points
     * @return true if skill was gained
     */
    fun tryGainSkill(player: Player, skillType: SkillType, difficulty: Int): Boolean {
        // No skill gain in Creative mode
        if (player.gameMode == GameMode.CREATIVE) {
            return false
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val skillData = data.getSkill(skillType)
        val currentValue = skillData.value

        // Update last used time
        skillData.updateLastUsed()

        // Check if skill can increase (lock mode check)
        if (!skillData.canIncrease()) {
            return false
        }

        // Check if skill is already maxed
        if (currentValue >= SkillType.MAX_SKILL_VALUE) {
            return false
        }

        // Skill gain logic:
        // - If difficulty >= skill: GUARANTEED gain (using appropriate/challenging material)
        // - If difficulty < skill: probability-based gain (material is below your level)
        //
        // This rewards using appropriate materials and makes progression feel natural:
        // - Iron (25) guarantees gains until skill 25, then probability-based
        // - Diamond (40) guarantees gains until skill 40, then probability-based
        // - Netherite (60) guarantees gains until skill 60, then probability-based
        val guaranteedGain = difficulty >= currentValue

        if (!guaranteedGain) {
            // Calculate gain chance for lower difficulty materials
            // Base chance is higher at low skill, lower at high skill
            // Formula: (100 - skill) / 5 gives 20% at skill 0, 10% at skill 50, 2% at skill 90
            val baseChance = (100 - currentValue) / 5.0
            val difficultyModifier = calculateDifficultyModifier(currentValue, difficulty.toDouble())
            val gainChance = (baseChance * difficultyModifier).coerceIn(0.5, 50.0)

            // Roll for skill gain
            if (Random.nextDouble() * 100 > gainChance) {
                return false
            }
        }

        // Calculate gain amount
        val gainAmount = plugin.skillsConfig.skillGainAmount

        // Check total skill cap
        val totalSkills = data.getTotalSkillPoints()
        if (totalSkills + gainAmount > SkillType.TOTAL_SKILL_CAP) {
            // Need to decrease another skill (respects lock mode)
            val skillToDecrease = data.getSkillToDecrease(exclude = skillType)
            if (skillToDecrease != null) {
                val decreaseData = data.getSkill(skillToDecrease)
                if (decreaseData.value >= gainAmount && decreaseData.canDecrease()) {
                    decreaseData.addValue(-gainAmount)
                    plugin.messageSender.send(
                        player, MessageKey.SKILL_DECREASE,
                        "skill" to skillToDecrease.displayName,
                        "amount" to gainAmount,
                        "current" to String.format("%.1f", decreaseData.value)
                    )
                } else {
                    // Not enough to decrease or locked, cap reached
                    plugin.messageSender.send(player, MessageKey.SKILL_CAP_REACHED)
                    return false
                }
            } else {
                // No skill available to decrease (all locked)
                plugin.messageSender.send(player, MessageKey.SKILL_CAP_REACHED)
                return false
            }
        }

        // Apply skill gain
        skillData.addValue(gainAmount)
        data.dirty = true

        // Notify player
        plugin.messageSender.send(
            player, MessageKey.SKILL_GAIN,
            "skill" to skillType.displayName,
            "amount" to gainAmount,
            "current" to String.format("%.1f", skillData.value)
        )

        // Try to gain associated stats (UO-style)
        // Track STR before to check if it decreased (for armor validation)
        val strBefore = data.str
        tryGainStats(player, data, skillType)
        val strAfter = data.str

        // Update max stats (STR affects HP, etc.)
        data.updateMaxStats()

        // If STR decreased, validate equipment (may need to remove armor)
        if (strAfter < strBefore) {
            plugin.armorManager.validateEquipment(player)
        }

        // Update attribute modifiers (DEX affects movement/attack speed, minus armor penalty)
        val armorDexPenalty = plugin.armorManager.getTotalDexPenalty(player)
        StatCalculator.applyAttributeModifiers(player, data, armorDexPenalty)

        return true
    }

    /**
     * Try to gain stats based on skill type (UO-style)
     * Each skill has associated stats with weights (0.0-1.0)
     * Higher weight = higher chance to gain that stat
     */
    private fun tryGainStats(player: Player, data: PlayerData, skillType: SkillType) {
        // Base stat gain chance: 10% per skill gain
        val baseStatGainChance = 0.10

        // Try to gain STR if skill has STR weight
        if (skillType.strWeight > 0 && Random.nextDouble() < baseStatGainChance * skillType.strWeight) {
            if (data.tryGainStat(StatType.STR)) {
                plugin.messageSender.send(
                    player, MessageKey.STAT_GAIN,
                    "stat" to StatType.STR.displayName,
                    "current" to data.str
                )
            }
        }

        // Try to gain DEX if skill has DEX weight
        if (skillType.dexWeight > 0 && Random.nextDouble() < baseStatGainChance * skillType.dexWeight) {
            if (data.tryGainStat(StatType.DEX)) {
                plugin.messageSender.send(
                    player, MessageKey.STAT_GAIN,
                    "stat" to StatType.DEX.displayName,
                    "current" to data.dex
                )
            }
        }

        // Try to gain INT if skill has INT weight
        if (skillType.intWeight > 0 && Random.nextDouble() < baseStatGainChance * skillType.intWeight) {
            if (data.tryGainStat(StatType.INT)) {
                plugin.messageSender.send(
                    player, MessageKey.STAT_GAIN,
                    "stat" to StatType.INT.displayName,
                    "current" to data.int
                )
            }
        }
    }

    /**
     * Calculate difficulty modifier for skill gain
     * Higher difficulty = better gain (rewarding challenging tasks)
     * Lower difficulty = worse gain (you've outgrown this task)
     *
     * Design:
     * - difficulty > skill: bonus (stretching yourself, rewarded)
     * - difficulty ≈ skill (±30): normal
     * - difficulty < skill - 30: penalty (too easy, you've outgrown it)
     *
     * This ensures using valuable materials (higher difficulty) is rewarded,
     * and skill gain caps out when the task becomes trivial.
     * e.g., Diamond (difficulty 40) will stop giving good gains around skill 70+
     *       Iron (difficulty 25) will stop giving good gains around skill 55+
     *       Netherite (difficulty 60) will stop giving good gains around skill 90+
     */
    private fun calculateDifficultyModifier(skillValue: Double, difficulty: Double): Double {
        val difference = difficulty - skillValue
        return when {
            difference >= 0 -> 1.0 + (difference / 50.0).coerceAtMost(0.5)  // Bonus for challenging tasks (up to +50%)
            difference > -30 -> 1.0                                          // Optimal range (wider: ±30)
            else -> 0.3                                                      // Too easy (skill > difficulty + 30), less harsh penalty
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
            WeaponType.SPEAR -> SkillType.SPEAR
            WeaponType.BOW, WeaponType.CROSSBOW -> SkillType.ARCHERY
            WeaponType.TRIDENT -> SkillType.THROWING
            WeaponType.FIST -> SkillType.WRESTLING
        }
    }
}

enum class WeaponType {
    SWORD, AXE, MACE, SPEAR, BOW, CROSSBOW, TRIDENT, FIST
}
