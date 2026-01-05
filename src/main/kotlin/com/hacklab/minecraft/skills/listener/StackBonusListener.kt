package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory

/**
 * Listener for applying stack size bonuses based on production skills.
 *
 * Applies stack bonus when:
 * - Player picks up items from the ground
 * - Player takes items from containers (chests, furnaces, etc.)
 * - Player joins the game (syncs all items of same type to highest maxStackSize)
 * - Player tries to stack items in inventory (syncs maxStackSize for stacking)
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
     * Apply stack bonus when player takes items from containers or
     * tries to stack items within their inventory.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory ?: return

        // Skip Vault (Trial Chamber treasure vault) to preserve vanilla behavior
        // Vault generates loot on first open and consumes Trial Key - we must not interfere
        if (isVaultInventory(clickedInventory)) return

        // Handle taking items from containers (non-player inventory)
        if (clickedInventory.type != InventoryType.PLAYER &&
            clickedInventory.type != InventoryType.CRAFTING) {

            val item = event.currentItem ?: return
            plugin.stackBonusManager.applyStackBonusWithSync(item, player)
        }

        // Handle shift-click from container to player inventory
        if (event.isShiftClick && clickedInventory.type != InventoryType.PLAYER) {
            val item = event.currentItem ?: return
            plugin.stackBonusManager.applyStackBonusWithSync(item, player)
        }

        // Handle drag & drop within player inventory to stack items
        if (clickedInventory.type == InventoryType.PLAYER) {
            val cursor = event.cursor
            val slotItem = event.currentItem

            // If player is trying to combine items of the same type
            if (!cursor.type.isAir &&
                slotItem != null && !slotItem.type.isAir &&
                cursor.type == slotItem.type &&
                cursor.type.maxStackSize > 1) {

                // Sync maxStackSize for both items so they can stack
                plugin.stackBonusManager.syncItemsForStacking(cursor, slotItem, player)
            }
        }
    }

    /**
     * Update stack bonus for all items when player joins.
     * Syncs all items of the same type to the highest maxStackSize found.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Schedule update for next tick to ensure player data is loaded
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            syncPlayerInventory(player)
        }, 5L)
    }

    /**
     * Sync stack sizes for all items in player's inventory.
     * Groups items by type and sets all items of each type to the highest maxStackSize.
     */
    private fun syncPlayerInventory(player: Player) {
        if (!player.isOnline) return

        val inventory = player.inventory
        val calculatedMax = plugin.stackBonusManager.calculateMaxStackSize(player)

        // Group slots by material type
        val slotsByType = mutableMapOf<Material, MutableList<Int>>()
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type.maxStackSize > 1) {
                slotsByType.getOrPut(item.type) { mutableListOf() }.add(i)
            }
        }

        // For each material type, find the highest maxStackSize and sync all items
        for ((type, slots) in slotsByType) {
            if (slots.size <= 1) continue // No need to sync single items

            // Find highest maxStackSize among items of this type
            var highestMax = calculatedMax
            for (slot in slots) {
                val item = inventory.getItem(slot) ?: continue
                val meta = item.itemMeta ?: continue
                if (meta.hasMaxStackSize()) {
                    highestMax = maxOf(highestMax, meta.maxStackSize)
                }
            }

            // Update all items of this type to the highest maxStackSize
            if (highestMax > 64) {
                for (slot in slots) {
                    val item = inventory.getItem(slot) ?: continue
                    val meta = item.itemMeta ?: continue
                    val currentMax = if (meta.hasMaxStackSize()) meta.maxStackSize else item.type.maxStackSize
                    if (currentMax != highestMax) {
                        meta.setMaxStackSize(highestMax)
                        item.itemMeta = meta
                        inventory.setItem(slot, item)
                    }
                }
            }
        }
    }

    /**
     * Check if the inventory belongs to a Vault block (Trial Chamber treasure vault).
     * Minecraft 1.21+ introduced Vault blocks that require Trial Keys to open.
     * We must not interfere with Vault inventory operations to preserve vanilla behavior.
     */
    private fun isVaultInventory(inventory: Inventory): Boolean {
        // Check by block type at inventory location
        val location = inventory.location ?: return false
        return location.block.type == Material.VAULT
    }
}
