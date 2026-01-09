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
         * For backwards compatibility - assumes difficulty 0
         */
        fun calculateQuality(skill: Double): QualityType {
            return calculateQuality(skill, 0)
        }

        /**
         * Calculate quality based on skill value and item difficulty (DIFFICULTY-RELATIVE SYSTEM)
         *
         * Quality is determined relative to the item's difficulty, not absolute skill value.
         * This makes progression meaningful for each material tier.
         *
         * Quality thresholds (relative to difficulty):
         * - LQ: skill < difficulty (attempting items above your level)
         * - NQ: difficulty ≤ skill < difficulty + 20 (competent)
         * - HQ: skill ≥ difficulty + 20 (probability-based, mastering the material)
         * - HQ guaranteed: skill ≥ difficulty + 40 (complete mastery)
         * - EX: skill ≥ difficulty + 30 (probability-based, exceptional work)
         *
         * Examples:
         * - Iron (25): HQ starts at 45, guaranteed at 65, EX possible at 55+
         * - Diamond (40): HQ starts at 60, guaranteed at 80, EX possible at 70+
         * - Netherite (60): HQ starts at 80, guaranteed at 100, EX possible at 90+
         *
         * @param skill The player's skill value (0-100)
         * @param difficulty The item's crafting difficulty (0-100)
         */
        fun calculateQuality(skill: Double, difficulty: Int): QualityType {
            val diff = difficulty.toDouble()
            val skillOverDifficulty = skill - diff

            // LQ: skill below difficulty (item is too hard)
            if (skillOverDifficulty < 0) {
                return LOW_QUALITY
            }

            // NQ: skill meets difficulty but not yet mastering
            if (skillOverDifficulty < 20) {
                return NORMAL_QUALITY
            }

            val roll = Math.random() * 100

            // HQ guaranteed at +40, EX possible at +30
            if (skillOverDifficulty >= 40) {
                // At +40 or more: HQ is guaranteed, check for EX
                // EX probability: (skillOverDifficulty - 30) * 2, so at +40 = 20%, at +50 = 40%, etc.
                val exChance = (skillOverDifficulty - 30) * 2.0
                if (roll < exChance) {
                    return EXCEPTIONAL
                }
                return HIGH_QUALITY
            }

            // HQ probability zone: +20 to +40
            // HQ probability: (skillOverDifficulty - 20) * 5, so at +20 = 0%, at +30 = 50%, at +40 = 100%
            val hqChance = (skillOverDifficulty - 20) * 5.0

            // Also check for EX if skill is +30 or more
            if (skillOverDifficulty >= 30) {
                val exChance = (skillOverDifficulty - 30) * 2.0
                if (roll < exChance) {
                    return EXCEPTIONAL
                }
            }

            if (roll < hqChance) {
                return HIGH_QUALITY
            }

            return NORMAL_QUALITY
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
