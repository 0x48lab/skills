package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.entity.Player
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSprintEvent

class PlayerListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Load player data and check if new player
        val (data, isNewPlayer) = plugin.playerDataManager.loadPlayerWithStatus(player)

        // Load language preference
        plugin.localeManager.loadPlayerLocale(player.uniqueId, data.language)

        // Sync health and mana to vanilla
        StatCalculator.syncHealthToVanilla(player, data)
        StatCalculator.syncManaToVanilla(player, data)

        // Validate equipment (remove armor player can no longer wear due to STR)
        plugin.armorManager.validateEquipment(player)

        // Apply attribute modifiers (including armor DEX penalty)
        val armorDexPenalty = plugin.armorManager.getTotalDexPenalty(player)
        StatCalculator.applyAttributeModifiers(player, data, armorDexPenalty)

        // Initialize stamina
        plugin.staminaManager.initializePlayer(player)

        // Give guide book to new players
        if (isNewPlayer) {
            plugin.guideManager.giveGuideBook(player)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // Save and unload player data
        plugin.playerDataManager.unloadPlayer(player)

        // Clean up manager states
        plugin.hidingManager.removePlayer(player.uniqueId)
        plugin.snoopingManager.endSession(player.uniqueId)
        plugin.targetManager.cancelTargeting(player.uniqueId)
        plugin.tamingManager.removeCooldown(player.uniqueId)
        plugin.localeManager.removePlayer(player.uniqueId)
        plugin.cooldownManager.clearCooldowns(player.uniqueId)
        plugin.survivalListener.removePlayer(player.uniqueId)
        plugin.scoreboardManager.cleanup(player.uniqueId)
        plugin.staminaManager.cleanup(player.uniqueId)
        plugin.chunkLimitManager.cleanup(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        // Reset internal HP/Mana on respawn
        plugin.healthManager.handleRespawn(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        // Skip if no actual movement (only rotation)
        if (event.from.x == event.to.x && event.from.y == event.to.y && event.from.z == event.to.z) {
            return
        }

        // Check if player is casting - movement cancels casting
        if (plugin.castingManager.isCasting(player.uniqueId)) {
            plugin.castingManager.checkMovement(player)
        }

        // Check sprinting while hidden
        if (plugin.hidingManager.isHidden(player.uniqueId)) {
            if (!plugin.hidingManager.checkSprinting(player)) {
                return
            }

            // Process stealth movement
            val distance = event.from.distance(event.to)
            plugin.hidingManager.processMovement(player, distance)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        val newFoodLevel = event.foodLevel

        // Sync food level changes to internal mana (both increase and decrease)
        plugin.manaManager.syncFromVanilla(player, newFoodLevel)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerToggleSprint(event: PlayerToggleSprintEvent) {
        val player = event.player

        if (event.isSprinting) {
            // Player is trying to start sprinting
            if (!plugin.staminaManager.canSprint(player)) {
                event.isCancelled = true
            } else {
                // Track sprint intent for jump detection
                plugin.staminaManager.onSprintStart(player)
            }
        } else {
            // Player stopped sprinting
            plugin.staminaManager.onSprintStop(player)
        }
    }
}
