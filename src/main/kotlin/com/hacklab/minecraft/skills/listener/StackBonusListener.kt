package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.StackBonusManager
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryAction
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

    private var taskId: Int = -1

    /**
     * Start a periodic task that syncs maxStackSize of nearby ground items
     * with the player's stack bonus. This ensures NMS pickup logic sees matching
     * DataComponents, allowing items to stack into existing slots even when
     * the inventory has no empty slots.
     */
    fun startNearbyItemSyncTask() {
        taskId = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            syncNearbyGroundItems()
        }, 20L, 4L).taskId
    }

    /**
     * Stop the nearby item sync task.
     */
    fun stopNearbyItemSyncTask() {
        if (taskId != -1) {
            plugin.server.scheduler.cancelTask(taskId)
            taskId = -1
        }
    }

    /**
     * Sync maxStackSize of ground items near each online player.
     * Runs every 4 ticks (~200ms) to pre-apply stack bonus before NMS pickup check.
     */
    private fun syncNearbyGroundItems() {
        for (player in plugin.server.onlinePlayers) {
            val maxStackSize = plugin.stackBonusManager.calculateMaxStackSize(player)
            if (maxStackSize <= StackBonusManager.BASE_STACK_SIZE) continue

            for (entity in player.getNearbyEntities(2.0, 2.0, 2.0)) {
                if (entity !is Item) continue
                val itemStack = entity.itemStack
                if (itemStack.type.maxStackSize <= 1) continue
                if (StackBonusManager.isExcludedMaterial(itemStack.type)) continue

                val currentMax = plugin.stackBonusManager.getMaxStackSize(itemStack)
                if (currentMax == maxStackSize) continue

                plugin.stackBonusManager.applyStackBonusWithSync(itemStack, player)
                entity.itemStack = itemStack
            }
        }
    }

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
     * Repair the inventory after the click has actually been carried out.
     *
     * Shift-click (and double-click collect) are resolved by the server *after*
     * this event returns, so anything the pre-click sync did can still end up
     * split at the vanilla limit - the bonus then looks like it was ignored.
     * Re-normalizing on the next tick fixes the end state regardless of how the
     * quick-move decided to distribute the items.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClickMonitor(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Never touch Vault (Trial Chamber) interactions
        event.clickedInventory?.let { if (isVaultInventory(it)) return }
        if (isVaultInventory(event.view.topInventory)) return

        // Only bulk moves can leave stacks split below the raised limit.
        // Plain clicks are the player arranging things by hand - leave those alone.
        val consolidate = when (event.action) {
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.COLLECT_TO_CURSOR -> true
            else -> false
        }

        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.stackBonusManager.normalizeInventory(player, consolidate)
        })
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
            plugin.stackBonusManager.normalizeInventory(player, consolidate = true)
        }, 5L)
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
