package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

/**
 * Listener for chunk-based mob limit system.
 * Prevents excessive mob spawning and breeding to reduce server lag.
 */
class MobLimitListener(private val plugin: Skills) : Listener {

    /**
     * Check if spawn is allowed based on chunk limits.
     * HIGH priority to cancel early if limit is reached.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCreatureSpawnCheck(event: CreatureSpawnEvent) {
        val entity = event.entity

        // Check if spawn is allowed
        if (!plugin.mobLimitManager.canSpawn(entity, event.spawnReason)) {
            event.isCancelled = true
        }
    }

    /**
     * Check if breeding is allowed based on chunk limits.
     * HIGH priority to cancel early if limit is reached.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityBreed(event: EntityBreedEvent) {
        val child = event.entity

        // Get the breeder (player who fed the animal)
        val breeder = event.breeder as? Player

        // Check if breeding is allowed (also sends notification to player)
        if (!plugin.mobLimitManager.canBreed(child, breeder)) {
            event.isCancelled = true
        }
    }

    /**
     * Update cache when a creature successfully spawns.
     * MONITOR priority to ensure event is not cancelled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCreatureSpawnMonitor(event: CreatureSpawnEvent) {
        plugin.mobLimitManager.onMobSpawned(event.entity)
    }

    /**
     * Update cache when an entity leaves the world for any reason:
     * death, despawn, a bee entering a hive, plugin removal, ...
     *
     * Tracking only deaths leaks counts - most notably bees, which are removed
     * from the world when they enter a hive and re-spawned when they leave it.
     * That alone inflates a chunk's passive count until breeding is impossible.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityRemove(event: EntityRemoveEvent) {
        // Chunk unload drops the whole cache entry, no per-entity work needed
        if (event.cause == EntityRemoveEvent.Cause.UNLOAD) return

        val entity = event.entity as? LivingEntity ?: return
        plugin.mobLimitManager.onMobRemoved(entity)
    }

    /**
     * Initialize cache when a chunk is loaded.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        plugin.mobLimitManager.onChunkLoad(event.chunk)
    }

    /**
     * Clear cache when a chunk is unloaded.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        plugin.mobLimitManager.onChunkUnload(event.chunk)
    }
}
