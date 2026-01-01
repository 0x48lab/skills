package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.thief.StealResult
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

        // Get clicked item
        val clickedItem = event.currentItem
        if (clickedItem == null || clickedItem.type.isAir) {
            return
        }

        // Attempt to steal the item
        val result = plugin.snoopingManager.handleSnoopClick(player, event.rawSlot)

        when (result) {
            StealResult.SUCCESS -> {
                // Already handled in StealingManager
            }
            StealResult.FAILED -> {
                // Already handled in StealingManager - target was notified
            }
            StealResult.ITEM_MOVED -> {
                player.sendMessage("§7The item was moved.")
            }
            StealResult.TARGET_OFFLINE -> {
                player.sendMessage("§cTarget went offline.")
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
