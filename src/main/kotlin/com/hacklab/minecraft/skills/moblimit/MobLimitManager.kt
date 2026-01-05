package com.hacklab.minecraft.skills.moblimit

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Chunk
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages mob limits per chunk to prevent server lag from mob farms.
 */
class MobLimitManager(private val plugin: Skills) {

    /**
     * Cache of mob counts per chunk.
     * Key format: "worldName:chunkX:chunkZ"
     * Value: Map of category to count
     */
    private val cache = ConcurrentHashMap<String, MutableMap<MobLimitCategory, Int>>()

    /**
     * Check if a mob is allowed to spawn based on chunk limits.
     *
     * @param entity The entity attempting to spawn
     * @param spawnReason The reason for spawning
     * @return true if spawn is allowed, false if blocked
     */
    fun canSpawn(entity: LivingEntity, spawnReason: SpawnReason): Boolean {
        // Check if this entity type is exempt
        if (ProtectedEntityTypes.isExempt(entity, spawnReason)) {
            return true
        }

        // Get the mob category
        val category = MobLimitCategory.fromSpawnCategory(entity.spawnCategory) ?: return true

        // Get current count in chunk
        val chunk = entity.location.chunk
        val currentCount = getMobCount(chunk, category)

        // Check against limit
        val limit = getLimit(category)
        return currentCount < limit
    }

    /**
     * Check if breeding is allowed based on chunk limits.
     * This also sends a notification to the player if breeding is blocked.
     *
     * @param entity The child entity being bred
     * @param breeder The player who triggered the breeding (nullable)
     * @return true if breeding is allowed, false if blocked
     */
    fun canBreed(entity: LivingEntity, breeder: Player?): Boolean {
        // Get the mob category
        val category = MobLimitCategory.fromSpawnCategory(entity.spawnCategory) ?: return true

        // Get current count in chunk
        val chunk = entity.location.chunk
        val currentCount = getMobCount(chunk, category)

        // Check against limit
        val limit = getLimit(category)

        if (currentCount >= limit) {
            // Notify player if configured and breeder is present
            if (plugin.skillsConfig.chunkMobLimitNotify && breeder != null) {
                val displayName = if (plugin.localeManager.getLanguage(breeder).code == "ja") {
                    category.displayNameJa
                } else {
                    category.displayNameEn
                }
                plugin.messageSender.send(
                    breeder,
                    MessageKey.CHUNK_MOB_LIMIT_BREEDING,
                    "category" to displayName,
                    "limit" to limit
                )
            }
            return false
        }
        return true
    }

    /**
     * Called when a mob successfully spawns.
     * Updates the cache to include this new mob.
     */
    fun onMobSpawned(entity: LivingEntity) {
        val category = MobLimitCategory.fromSpawnCategory(entity.spawnCategory) ?: return
        val key = getCacheKey(entity.location.chunk)

        cache.getOrPut(key) { mutableMapOf() }
            .merge(category, 1) { old, _ -> old + 1 }
    }

    /**
     * Called when a mob is removed from the world (death, despawn, etc.).
     * Updates the cache to reflect the removal.
     */
    fun onMobRemoved(entity: LivingEntity) {
        val category = MobLimitCategory.fromSpawnCategory(entity.spawnCategory) ?: return
        val key = getCacheKey(entity.location.chunk)

        cache[key]?.merge(category, 0) { old, _ -> (old - 1).coerceAtLeast(0) }
    }

    /**
     * Called when a chunk is loaded.
     * Scans the chunk and initializes the cache with current mob counts.
     */
    fun onChunkLoad(chunk: Chunk) {
        val key = getCacheKey(chunk)
        val counts = mutableMapOf<MobLimitCategory, Int>()

        chunk.entities
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                MobLimitCategory.fromSpawnCategory(entity.spawnCategory)?.let { category ->
                    counts.merge(category, 1) { old, _ -> old + 1 }
                }
            }

        cache[key] = counts
    }

    /**
     * Called when a chunk is unloaded.
     * Removes the chunk from the cache to free memory.
     */
    fun onChunkUnload(chunk: Chunk) {
        cache.remove(getCacheKey(chunk))
    }

    /**
     * Get the current mob count for a category in a chunk.
     */
    fun getMobCount(chunk: Chunk, category: MobLimitCategory): Int {
        return cache[getCacheKey(chunk)]?.get(category) ?: 0
    }

    /**
     * Get the limit for a mob category from config.
     */
    fun getLimit(category: MobLimitCategory): Int {
        return when (category) {
            MobLimitCategory.PASSIVE -> plugin.skillsConfig.chunkMobLimitPassive
            MobLimitCategory.HOSTILE -> plugin.skillsConfig.chunkMobLimitHostile
            MobLimitCategory.AMBIENT -> plugin.skillsConfig.chunkMobLimitAmbient
            MobLimitCategory.WATER_CREATURE -> plugin.skillsConfig.chunkMobLimitWaterCreature
            MobLimitCategory.WATER_AMBIENT -> plugin.skillsConfig.chunkMobLimitWaterAmbient
        }
    }

    /**
     * Generate cache key for a chunk.
     */
    private fun getCacheKey(chunk: Chunk): String {
        return "${chunk.world.name}:${chunk.x}:${chunk.z}"
    }
}
