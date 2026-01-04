package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Listener for applying stack size bonuses based on production skills.
 *
 * Applies stack bonus when:
 * - Player picks up items from the ground
 * - Player takes items from containers (chests, furnaces, etc.)
 * - Player joins the game (updates existing inventory)
 */
class StackBonusListener(private val plugin: Skills) : Listener {

    /**
     * Apply stack bonus when player picks up items from the ground.
     * Uses sync version to ensure items can stack with existing inventory items.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item.itemStack

        // Apply stack bonus with inventory sync to maintain stacking compatibility
        plugin.stackBonusManager.applyStackBonusWithSync(item, player)

        // Update the dropped item entity
        event.item.itemStack = item
    }

    /**
     * Apply stack bonus when player takes items from containers.
     * Uses sync version to ensure items can stack with existing inventory items.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Skip if clicking in player's own inventory top section
        val clickedInventory = event.clickedInventory ?: return

        // Check if player is taking items from a non-player inventory
        if (clickedInventory.type != InventoryType.PLAYER &&
            clickedInventory.type != InventoryType.CRAFTING) {

            // Player is clicking on a container inventory
            val item = event.currentItem ?: return

            // Apply stack bonus with inventory sync to maintain stacking compatibility
            plugin.stackBonusManager.applyStackBonusWithSync(item, player)
        }

        // Also handle shift-click from container to player inventory
        if (event.isShiftClick && clickedInventory.type != InventoryType.PLAYER) {
            val item = event.currentItem ?: return
            plugin.stackBonusManager.applyStackBonusWithSync(item, player)
        }
    }

    /**
     * Update stack bonus for all items when player joins.
     * This ensures returning players have their inventory updated
     * if their skills have changed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Schedule update for next tick to ensure player data is loaded
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            updatePlayerInventory(player)
        }, 5L)
    }

    /**
     * Update stack sizes for all items in player's inventory.
     */
    private fun updatePlayerInventory(player: Player) {
        if (!player.isOnline) return

        val inventory = player.inventory
        val maxStackSize = plugin.stackBonusManager.calculateMaxStackSize(player)

        // Update main inventory
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type.maxStackSize > 1) {
                val meta = item.itemMeta ?: continue
                val currentMax = if (meta.hasMaxStackSize()) meta.maxStackSize else item.type.maxStackSize

                // Only update if new max is higher (don't reduce stack size)
                if (maxStackSize > currentMax) {
                    meta.setMaxStackSize(maxStackSize)
                    item.itemMeta = meta
                    inventory.setItem(i, item)
                }
            }
        }
    }
}
