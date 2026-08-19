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
 *
 * Counts are kept as an incrementally maintained cache for speed, but the cache
 * is only ever treated as a hint: it is re-scanned from the actual chunk contents
 * when it goes stale and always before a spawn/breed is blocked. Incremental
 * counters alone drift out of sync (mobs walk across chunk borders, bees enter
 * hives, mobs despawn silently) and drift in the "too many mobs" direction would
 * permanently block breeding in a chunk.
 */
class MobLimitManager(private val plugin: Skills) {

    /**
     * Cache of mob counts per chunk.
     * Key format: "worldName:chunkX:chunkZ"
     */
    private val cache = ConcurrentHashMap<String, ChunkCounts>()

    private class ChunkCounts(
        val counts: MutableMap<MobLimitCategory, Int>,
        var lastScan: Long
    )

    /** Cache lifetime in milliseconds (config value is in ticks) */
    private val cacheLifetimeMs: Long
        get() = plugin.skillsConfig.chunkMobLimitCheckInterval * 50L

    /**
     * Check if a mob is allowed to spawn based on chunk limits.
     *
     * @param entity The entity attempting to spawn
     * @param spawnReason The reason for spawning
     * @return true if spawn is allowed, false if blocked
     */
    fun canSpawn(entity: LivingEntity, spawnReason: SpawnReason): Boolean {
        // Check if this entity type / spawn reason is exempt
        if (ProtectedEntityTypes.isExempt(entity, spawnReason)) {
            return true
        }

        val category = trackedCategory(entity) ?: return true
        return isUnderLimit(entity.location.chunk, category)
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
        val category = trackedCategory(entity) ?: return true
        val limit = getLimit(category)

        if (isUnderLimit(entity.location.chunk, category)) {
            return true
        }

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

    /**
     * Called when a mob successfully spawns.
     * Updates the cache to include this new mob.
     */
    fun onMobSpawned(entity: LivingEntity) {
        val category = trackedCategory(entity) ?: return
        val chunk = entity.location.chunk

        cache[getCacheKey(chunk)]?.counts?.merge(category, 1) { old, _ -> old + 1 }
    }

    /**
     * Called when a mob is removed from the world (death, despawn, entering a
     * beehive, plugin removal, ...).
     * Updates the cache to reflect the removal.
     */
    fun onMobRemoved(entity: LivingEntity) {
        val category = trackedCategory(entity) ?: return
        val chunk = entity.location.chunk

        cache[getCacheKey(chunk)]?.counts?.merge(category, 0) { old, _ -> (old - 1).coerceAtLeast(0) }
    }

    /**
     * Called when a chunk is loaded.
     * Scans the chunk and initializes the cache with current mob counts.
     */
    fun onChunkLoad(chunk: Chunk) {
        refresh(chunk)
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
        return counts(chunk)[category] ?: 0
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
     * Drop all cached counts (used on reload).
     */
    fun clearCache() {
        cache.clear()
    }

    // === Internals ===

    /**
     * Whether the chunk is below the limit for the category.
     * Never blocks on a cached count alone - the chunk is re-scanned before
     * reporting "full" so a drifted cache can not lock out breeding.
     */
    private fun isUnderLimit(chunk: Chunk, category: MobLimitCategory): Boolean {
        val limit = getLimit(category)
        if ((counts(chunk)[category] ?: 0) < limit) return true

        // Cache says full - verify against the real chunk contents
        return (refresh(chunk)[category] ?: 0) < limit
    }

    /**
     * Get counts for a chunk, re-scanning if the cache is missing or stale.
     */
    private fun counts(chunk: Chunk): Map<MobLimitCategory, Int> {
        val entry = cache[getCacheKey(chunk)] ?: return refresh(chunk)
        if (System.currentTimeMillis() - entry.lastScan >= cacheLifetimeMs) {
            return refresh(chunk)
        }
        return entry.counts
    }

    /**
     * Re-scan a chunk and replace its cached counts with the real values.
     */
    private fun refresh(chunk: Chunk): Map<MobLimitCategory, Int> {
        val counts = mutableMapOf<MobLimitCategory, Int>()

        chunk.entities
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                trackedCategory(entity)?.let { category ->
                    counts.merge(category, 1) { old, _ -> old + 1 }
                }
            }

        cache[getCacheKey(chunk)] = ChunkCounts(counts, System.currentTimeMillis())
        return counts
    }

    /**
     * Category an entity counts towards, or null if it is not tracked at all.
     * Entity-type exemptions (bosses, villagers, golems) are applied here so
     * live scans and incremental updates always agree.
     */
    private fun trackedCategory(entity: LivingEntity): MobLimitCategory? {
        if (ProtectedEntityTypes.isExempt(entity, null)) return null
        return MobLimitCategory.fromSpawnCategory(entity.spawnCategory)
    }

    /**
     * Generate cache key for a chunk.
     */
    private fun getCacheKey(chunk: Chunk): String {
        return "${chunk.world.name}:${chunk.x}:${chunk.z}"
    }
}
