package com.hacklab.minecraft.skills.moblimit

import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason

/**
 * Defines entity types and spawn reasons that are exempt from mob limits.
 */
object ProtectedEntityTypes {

    /**
     * Boss mobs that should never be limited.
     */
    val BOSS_TYPES: Set<EntityType> = setOf(
        EntityType.ENDER_DRAGON,
        EntityType.WITHER,
        EntityType.ELDER_GUARDIAN
    )

    /**
     * System-important mobs that should never be limited.
     * Includes villagers (economy), golems (village defense), and traders.
     */
    val PROTECTED_TYPES: Set<EntityType> = setOf(
        EntityType.VILLAGER,
        EntityType.IRON_GOLEM,
        EntityType.SNOW_GOLEM,
        EntityType.WANDERING_TRADER,
        EntityType.TRADER_LLAMA
    )

    /**
     * Spawn reasons that should be exempt from limits.
     * These are typically admin/plugin actions or special game mechanics.
     */
    val EXCLUDED_SPAWN_REASONS: Set<SpawnReason> = setOf(
        SpawnReason.COMMAND,           // /summon command
        SpawnReason.CUSTOM,            // Plugin-spawned
        SpawnReason.BUILD_SNOWMAN,     // Snow golem construction
        SpawnReason.BUILD_IRONGOLEM,   // Iron golem construction
        SpawnReason.BUILD_WITHER,      // Wither construction
        SpawnReason.CURED,             // Zombie villager cured
        SpawnReason.DROWNED,           // Mob conversion (drowning)
        SpawnReason.PIGLIN_ZOMBIFIED,  // Piglin to zombified piglin
        SpawnReason.INFECTION,         // Zombie villager infection
        SpawnReason.METAMORPHOSIS,     // Tadpole to frog
        SpawnReason.SHEARED,           // Mushroom cow shearing
        SpawnReason.ENDER_PEARL        // Endermite from pearl
    )

    /**
     * Check if an entity should be exempt from mob limits.
     *
     * @param entity The entity to check
     * @param spawnReason The reason the entity is spawning (null if unknown)
     * @return true if the entity should be exempt from limits
     */
    fun isExempt(entity: LivingEntity, spawnReason: SpawnReason?): Boolean {
        // Boss mobs are always exempt
        if (entity.type in BOSS_TYPES) return true

        // Protected types (villagers, golems) are always exempt
        if (entity.type in PROTECTED_TYPES) return true

        // Certain spawn reasons are exempt
        if (spawnReason != null && spawnReason in EXCLUDED_SPAWN_REASONS) return true

        return false
    }
}
