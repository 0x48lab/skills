package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.moblimit.ProtectedEntityTypes
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
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
        val entity = event.entity

        // Skip protected entities (they don't count toward limits)
        if (ProtectedEntityTypes.isExempt(entity, event.spawnReason)) return

        // Update cache
        plugin.mobLimitManager.onMobSpawned(entity)
    }

    /**
     * Update cache when an entity dies.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // Update cache
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
