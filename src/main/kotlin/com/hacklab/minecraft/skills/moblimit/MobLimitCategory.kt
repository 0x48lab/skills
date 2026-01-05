package com.hacklab.minecraft.skills.moblimit

import org.bukkit.entity.SpawnCategory

/**
 * Categories for mob limit system, mapped to Minecraft's SpawnCategory.
 */
enum class MobLimitCategory(
    val configKey: String,
    val defaultLimit: Int,
    val displayNameEn: String,
    val displayNameJa: String,
    val spawnCategories: Set<SpawnCategory>
) {
    PASSIVE(
        configKey = "passive",
        defaultLimit = 24,
        displayNameEn = "Passive",
        displayNameJa = "友好的",
        spawnCategories = setOf(SpawnCategory.ANIMAL)
    ),
    HOSTILE(
        configKey = "hostile",
        defaultLimit = 32,
        displayNameEn = "Hostile",
        displayNameJa = "敵対的",
        spawnCategories = setOf(SpawnCategory.MONSTER)
    ),
    AMBIENT(
        configKey = "ambient",
        defaultLimit = 8,
        displayNameEn = "Ambient",
        displayNameJa = "環境",
        spawnCategories = setOf(SpawnCategory.AMBIENT)
    ),
    WATER_CREATURE(
        configKey = "water_creature",
        defaultLimit = 8,
        displayNameEn = "Water Creature",
        displayNameJa = "水生生物",
        spawnCategories = setOf(SpawnCategory.WATER_ANIMAL)
    ),
    WATER_AMBIENT(
        configKey = "water_ambient",
        defaultLimit = 16,
        displayNameEn = "Water Ambient",
        displayNameJa = "水生環境",
        spawnCategories = setOf(SpawnCategory.WATER_AMBIENT)
    );

    companion object {
        /**
         * Get MobLimitCategory from Minecraft's SpawnCategory.
         * Returns null if the SpawnCategory is not tracked (e.g., MISC).
         */
        fun fromSpawnCategory(category: SpawnCategory): MobLimitCategory? =
            entries.find { category in it.spawnCategories }
    }
}
