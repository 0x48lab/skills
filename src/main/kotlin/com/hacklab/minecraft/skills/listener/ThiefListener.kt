package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.thief.StealResult
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

/**
 * Handles thief-related inventory events (snooping and stealing)
 */
class ThiefListener(private val plugin: Skills) : Listener {

    /**
     * Handle clicking in snooping inventory to attempt stealing
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onSnoopInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if player is in a snooping session
        if (!plugin.snoopingManager.isSnooping(player.uniqueId)) {
            return
        }

        // Cancel all clicks in snooping inventory to prevent taking items directly
        event.isCancelled = true

        // Check if clicking in the top inventory (the snooped inventory)
        if (event.rawSlot < 0 || event.rawSlot >= event.inventory.size) {
            return
        }

        // Check steal cooldown
        if (plugin.cooldownManager.isOnCooldown(player.uniqueId, CooldownAction.STEAL)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(player.uniqueId, CooldownAction.STEAL)
            plugin.messageSender.send(player, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return
        }

        // Get clicked item
        val clickedItem = event.currentItem
        if (clickedItem == null || clickedItem.type.isAir) {
            return
        }

        // Attempt to steal the item
        val result = plugin.snoopingManager.handleSnoopClick(player, event.rawSlot)

        // Set cooldown after steal attempt (success or failure)
        if (result == StealResult.SUCCESS || result == StealResult.FAILED) {
            plugin.cooldownManager.setCooldown(player.uniqueId, CooldownAction.STEAL)
        }

        when (result) {
            StealResult.SUCCESS -> {
                // Already handled in StealingManager
            }
            StealResult.FAILED -> {
                // Already handled in StealingManager - target was notified
            }
            StealResult.ITEM_MOVED -> {
                plugin.messageSender.send(player, MessageKey.THIEF_STEAL_ITEM_MOVED)
            }
            StealResult.TARGET_OFFLINE -> {
                plugin.messageSender.send(player, MessageKey.THIEF_STEAL_TARGET_OFFLINE)
            }
            StealResult.NO_ITEM -> {
                // Empty slot, do nothing
            }
            StealResult.NO_SESSION -> {
                // Session ended unexpectedly
            }
        }
    }

    /**
     * Clean up snooping session when inventory is closed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // End snooping session if active
        if (plugin.snoopingManager.isSnooping(player.uniqueId)) {
            plugin.snoopingManager.endSession(player.uniqueId)
        }
    }
}
