package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MeditationListener(private val plugin: Skills) : Listener {

    // Track meditating players and their tasks
    private val meditatingPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val meditationTasks: MutableMap<UUID, BukkitRunnable> = ConcurrentHashMap()

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player

        if (event.isSneaking) {
            // Don't start a new task if one is already running
            if (meditatingPlayers.contains(player.uniqueId)) {
                return
            }
            // Start meditation check
            meditatingPlayers.add(player.uniqueId)
            startMeditationTask(player)
        } else {
            // Stop meditation
            stopMeditation(player.uniqueId)
        }
    }

    private fun stopMeditation(playerId: UUID) {
        meditatingPlayers.remove(playerId)
        meditationTasks.remove(playerId)?.cancel()
    }

    private fun startMeditationTask(player: Player) {
        val task = object : BukkitRunnable() {
            private var lastLocation = player.location.clone()

            override fun run() {
                // Check if still meditating
                if (!meditatingPlayers.contains(player.uniqueId)) {
                    stopMeditation(player.uniqueId)
                    return
                }

                // Check if player is still online
                if (!player.isOnline) {
                    stopMeditation(player.uniqueId)
                    return
                }

                // Check if still sneaking
                if (!player.isSneaking) {
                    stopMeditation(player.uniqueId)
                    return
                }

                // Check if player moved (must be stationary to meditate)
                val currentLocation = player.location
                val moved = currentLocation.x != lastLocation.x ||
                        currentLocation.y != lastLocation.y ||
                        currentLocation.z != lastLocation.z

                if (moved) {
                    lastLocation = currentLocation.clone()
                    return // Skip this tick, player moved
                }

                // Process meditation (mana regen)
                plugin.manaManager.processMeditation(player)
            }
        }
        meditationTasks[player.uniqueId] = task
        task.runTaskTimer(plugin, 10L, 10L) // Every 0.5 seconds
    }

    fun cleanup() {
        meditatingPlayers.clear()
    }
}
