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
         * Calculate quality based on skill value
         * LQ: skill 0-25, NQ: 25-50, HQ: 50-75, EX: 75-100
         * With some randomness based on skill
         */
        fun calculateQuality(skill: Double): QualityType {
            val roll = Math.random() * 100
            val threshold = skill + (Math.random() * 20 - 10) // ±10 randomness

            return when {
                threshold >= 90 && roll < skill -> EXCEPTIONAL
                threshold >= 60 && roll < skill * 1.2 -> HIGH_QUALITY
                threshold >= 30 -> NORMAL_QUALITY
                else -> LOW_QUALITY
            }
        }

        fun fromShortName(name: String): QualityType? =
            entries.find { it.shortName.equals(name, ignoreCase = true) }
    }
}
