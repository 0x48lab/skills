package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

/**
 * Listener for armor equipment restrictions based on STR requirements
 */
class ArmorListener(private val plugin: Skills) : Listener {

    // Armor slot indices in player inventory
    companion object {
        const val HELMET_SLOT = 39
        const val CHESTPLATE_SLOT = 38
        const val LEGGINGS_SLOT = 37
        const val BOOTS_SLOT = 36
        const val OFFHAND_SLOT = 40
    }

    /**
     * Handle clicking armor into armor slots
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if clicking in player inventory
        if (event.clickedInventory?.type != InventoryType.PLAYER) {
            // Check if shift-clicking into player inventory
            if (event.isShiftClick && event.clickedInventory?.type != InventoryType.PLAYER) {
                val item = event.currentItem ?: return
                if (isArmor(item) && !canEquipArmor(player, item)) {
                    event.isCancelled = true
                    return
                }
            }
            return
        }

        val slot = event.slot

        // Check if it's an armor slot
        if (slot in BOOTS_SLOT..HELMET_SLOT || slot == OFFHAND_SLOT) {
            // What item is being placed?
            val newItem = when {
                event.isShiftClick -> event.currentItem // Shift-click moves the clicked item
                event.cursor != null && !event.cursor!!.type.isAir -> event.cursor // Placing cursor item
                else -> null
            }

            if (newItem != null && isArmor(newItem)) {
                if (!canEquipArmor(player, newItem)) {
                    event.isCancelled = true
                }
            }
        }

        // Handle shift-clicking armor from anywhere into armor slots
        if (event.isShiftClick) {
            val item = event.currentItem ?: return
            if (isArmor(item) && !canEquipArmor(player, item)) {
                event.isCancelled = true
            }
        }
    }

    /**
     * Handle dragging armor into armor slots
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if dragging into armor slots
        val armorSlots = setOf(HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, OFFHAND_SLOT)
        val dragsToArmor = event.rawSlots.any { it in armorSlots }

        if (dragsToArmor) {
            val item = event.oldCursor
            if (isArmor(item) && !canEquipArmor(player, item)) {
                event.isCancelled = true
            }
        }
    }

    /**
     * Handle right-click equipping armor directly
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Right-click with armor in hand equips it
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item ?: return
        if (!isArmor(item)) return

        if (!canEquipArmor(event.player, item)) {
            event.isCancelled = true
        } else {
            // Armor will be equipped, schedule stats update
            scheduleStatsUpdate(event.player)
        }
    }

    /**
     * Check if item is armor
     */
    private fun isArmor(item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) return false
        return plugin.armorConfig.isArmor(item.type)
    }

    /**
     * Check if player can equip armor and show message if not
     */
    private fun canEquipArmor(player: Player, item: ItemStack): Boolean {
        return plugin.armorManager.tryEquip(player, item)
    }

    /**
     * Recalculate DEX modifiers when inventory is closed (armor may have changed)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        updatePlayerStats(player)
    }

    /**
     * Update player stats after armor changes (called with 1 tick delay for right-click equip)
     */
    private fun scheduleStatsUpdate(player: Player) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            updatePlayerStats(player)
        }, 1L)
    }

    /**
     * Update player DEX-based attribute modifiers with armor penalty
     */
    private fun updatePlayerStats(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        val armorDexPenalty = plugin.armorManager.getTotalDexPenalty(player)
        StatCalculator.applyAttributeModifiers(player, data, armorDexPenalty)
    }
}
