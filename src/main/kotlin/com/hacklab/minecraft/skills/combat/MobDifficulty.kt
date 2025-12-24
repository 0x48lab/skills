package com.hacklab.minecraft.skills.combat

import org.bukkit.entity.EntityType

object MobDifficulty {
    private val difficulties = mapOf(
        // Basic mobs
        EntityType.ZOMBIE to 20,
        EntityType.SKELETON to 20,
        EntityType.SPIDER to 25,
        EntityType.CREEPER to 30,
        EntityType.PHANTOM to 30,

        // Nether mobs
        EntityType.GHAST to 35,
        EntityType.WITCH to 40,
        EntityType.BLAZE to 45,
        EntityType.VINDICATOR to 45,
        EntityType.ENDERMAN to 50,
        EntityType.WITHER_SKELETON to 50,

        // Strong mobs
        EntityType.PIGLIN_BRUTE to 55,
        EntityType.EVOKER to 55,
        EntityType.RAVAGER to 60,

        // Boss-tier
        EntityType.WARDEN to 100,
        EntityType.WITHER to 100,
        EntityType.ENDER_DRAGON to 100,

        // Animals (low difficulty)
        EntityType.CHICKEN to 5,
        EntityType.COW to 5,
        EntityType.PIG to 5,
        EntityType.SHEEP to 5,
        EntityType.RABBIT to 5,
        EntityType.WOLF to 15,
        EntityType.IRON_GOLEM to 50,

        // Default for unlisted
        EntityType.PLAYER to 50 // Will be calculated from player skills
    )

    fun getDifficulty(entityType: EntityType): Int = difficulties[entityType] ?: 25

    /**
     * Calculate player difficulty based on their combat skill average
     */
    fun getPlayerDifficulty(combatSkillAverage: Double): Int = combatSkillAverage.toInt()
}
