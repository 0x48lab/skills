package com.hacklab.minecraft.skills.economy

import com.hacklab.minecraft.skills.Skills
import org.bukkit.GameMode
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

/**
 * Listener for mob death events to process rewards
 */
class MobRewardListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // Only process if killed by a player
        val killer = entity.killer ?: return

        // No rewards in Creative mode
        if (killer.gameMode == GameMode.CREATIVE) {
            return
        }

        // Don't give rewards for player kills (that's PvP)
        if (entity is Player) {
            return
        }

        // Don't give rewards for tamed animals
        if (entity is org.bukkit.entity.Tameable && entity.isTamed) {
            return
        }

        // Process the mob kill reward
        plugin.mobRewardManager.processMobKill(killer, entity.type)
    }
}
