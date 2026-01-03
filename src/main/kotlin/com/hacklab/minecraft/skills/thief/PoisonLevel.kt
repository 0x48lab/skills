package com.hacklab.minecraft.skills.thief

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * UO-style poison levels
 * Each level has different potency and skill requirements
 */
enum class PoisonLevel(
    val displayName: String,
    val displayNameJa: String,
    val damagePerTick: Double,  // Damage per 20 ticks (1 second)
    val applyDifficulty: Int,   // Difficulty to apply poison to weapon
    val alchemyRequired: Int,   // Alchemy skill required to brew
    val color: TextColor
) {
    LESSER(
        displayName = "Lesser Poison",
        displayNameJa = "弱毒",
        damagePerTick = 0.5,
        applyDifficulty = 20,
        alchemyRequired = 0,
        color = NamedTextColor.GREEN
    ),
    REGULAR(
        displayName = "Poison",
        displayNameJa = "毒",
        damagePerTick = 1.0,
        applyDifficulty = 40,
        alchemyRequired = 30,
        color = NamedTextColor.DARK_GREEN
    ),
    GREATER(
        displayName = "Greater Poison",
        displayNameJa = "強毒",
        damagePerTick = 1.5,
        applyDifficulty = 60,
        alchemyRequired = 60,
        color = NamedTextColor.DARK_AQUA
    ),
    DEADLY(
        displayName = "Deadly Poison",
        displayNameJa = "猛毒",
        damagePerTick = 2.0,
        applyDifficulty = 80,
        alchemyRequired = 90,
        color = NamedTextColor.DARK_PURPLE
    );

    companion object {
        /**
         * Get poison level from vanilla potion type name
         */
        fun fromVanillaPotion(potionTypeName: String): PoisonLevel? {
            return when {
                potionTypeName.contains("STRONG_POISON") ||
                potionTypeName.contains("POISON") && potionTypeName.contains("II") -> REGULAR
                potionTypeName.contains("POISON") -> LESSER
                else -> null
            }
        }
    }
}
