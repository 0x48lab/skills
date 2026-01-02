package com.hacklab.minecraft.skills.crafting

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class QualityType(
    val displayName: String,
    val shortName: String,
    val modifier: Double,
    val color: TextColor
) {
    LOW_QUALITY("Low Quality", "LQ", 0.8, NamedTextColor.GRAY),
    NORMAL_QUALITY("Normal Quality", "NQ", 1.0, NamedTextColor.WHITE),
    HIGH_QUALITY("High Quality", "HQ", 1.2, NamedTextColor.BLUE),
    EXCEPTIONAL("Exceptional", "EX", 1.5, NamedTextColor.GOLD);

    companion object {
        /**
         * Calculate quality based on skill value (legacy, no difficulty)
         * For backwards compatibility
         */
        fun calculateQuality(skill: Double): QualityType {
            return calculateQuality(skill, 0)
        }

        /**
         * Calculate quality based on skill value and item difficulty
         *
         * The difficulty affects the effective skill:
         * - If skill >= difficulty: full quality potential
         * - If skill < difficulty: reduced quality potential
         *
         * Quality thresholds (based on effective skill):
         * - LQ: effectiveSkill < 30
         * - NQ: effectiveSkill 30-49
         * - HQ: effectiveSkill 50-69 with probability check
         * - EX: effectiveSkill 70+ with probability check
         *
         * @param skill The player's skill value (0-100)
         * @param difficulty The item's crafting difficulty (0-100)
         */
        fun calculateQuality(skill: Double, difficulty: Int): QualityType {
            // Calculate effective skill based on difficulty
            // If skill is below difficulty, reduce effective skill
            val skillDifference = skill - difficulty
            val effectiveSkill = when {
                skillDifference >= 0 -> skill  // Skill meets or exceeds difficulty
                skillDifference >= -20 -> skill + (skillDifference * 0.5)  // Slight penalty
                else -> skill + (skillDifference * 1.0)  // Heavy penalty for very hard items
            }.coerceIn(0.0, 100.0)

            val roll = Math.random() * 100
            val randomness = Math.random() * 10 - 5  // ±5 randomness

            val adjustedSkill = (effectiveSkill + randomness).coerceIn(0.0, 100.0)

            return when {
                // EX: Need 70+ effective skill and pass probability check
                adjustedSkill >= 70 && roll < (adjustedSkill - 60) * 2.5 -> EXCEPTIONAL
                // HQ: Need 50+ effective skill and pass probability check
                adjustedSkill >= 50 && roll < (adjustedSkill - 30) * 1.5 -> HIGH_QUALITY
                // NQ: Need 30+ effective skill
                adjustedSkill >= 30 -> NORMAL_QUALITY
                // LQ: Low skill or hard item
                else -> LOW_QUALITY
            }
        }

        /**
         * Calculate quality for stackable items (food, potions, etc.)
         * Returns a bonus multiplier instead of a quality enum
         *
         * @param skill The player's skill value (0-100)
         * @param difficulty The item's crafting difficulty (0-100)
         * @return Bonus multiplier (0.85 to 1.25)
         */
        fun calculateStackableBonus(skill: Double, difficulty: Int): Double {
            val quality = calculateQuality(skill, difficulty)
            return quality.modifier
        }

        fun fromShortName(name: String): QualityType? =
            entries.find { it.shortName.equals(name, ignoreCase = true) }
    }
}
