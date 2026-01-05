package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.RunebookManager
import com.hacklab.minecraft.skills.magic.SpellType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Listener for Runebook interactions and GUI events
 */
class RunebookListener(private val plugin: Skills) : Listener {

    // Track which players have a runebook GUI open and their runebook item
    private val openRunebooks: MutableMap<UUID, ItemStack> = ConcurrentHashMap()

    // Track pending rune entries for spell execution (player -> RuneEntry)
    private val pendingRecall: MutableMap<UUID, RunebookManager.RuneEntry> = ConcurrentHashMap()
    private val pendingGate: MutableMap<UUID, RunebookManager.RuneEntry> = ConcurrentHashMap()

    /**
     * Handle right-click to open runebook
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (!plugin.runebookManager.isRunebook(item)) return

        event.isCancelled = true

        val player = event.player
        openRunebooks[player.uniqueId] = item
        plugin.runebookManager.openGUI(player, item)

        player.world.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f)
    }

    /**
     * Handle clicks in the runebook GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if this is a runebook GUI
        val runebook = openRunebooks[player.uniqueId] ?: return

        val title = event.view.title()
        val isRunebookGUI = title.toString().contains("Runebook") || title.toString().contains("ルーンの書")
        if (!isRunebookGUI) return

        val clickedSlot = event.rawSlot
        val clickedItem = event.currentItem
        val cursorItem = event.cursor
        val useJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE

        // Block shift-click and number key transfers that would affect the runebook GUI
        if (event.click.isShiftClick || event.click == ClickType.NUMBER_KEY) {
            // If clicking in player inventory with shift, check if it's a rune to add
            if (clickedSlot >= RunebookManager.GUI_SIZE && event.click.isShiftClick && clickedItem != null) {
                if (plugin.runeManager.isRune(clickedItem) && plugin.runeManager.isMarked(clickedItem)) {
                    event.isCancelled = true
                    val success = plugin.runebookManager.addRune(runebook, clickedItem, useJapanese)
                    if (success) {
                        event.currentItem = null
                        player.world.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f)
                        plugin.messageSender.send(player, MessageKey.RUNEBOOK_RUNE_ADDED)
                        plugin.runebookManager.openGUI(player, runebook)
                    } else {
                        plugin.messageSender.send(player, MessageKey.RUNEBOOK_FULL)
                    }
                    return
                }
            }
            // Block all other shift-clicks and number key transfers
            event.isCancelled = true
            return
        }

        // Handle clicks in the runebook inventory (top inventory)
        if (clickedSlot in 0 until RunebookManager.GUI_SIZE) {
            // Always cancel clicks in the runebook GUI
            event.isCancelled = true

            // Check if clicking on a registered rune
            val runeIndex = plugin.runebookManager.getRuneIndex(clickedItem)
            if (runeIndex != null) {
                // Handle rune operations (Recall, Gate Travel, Remove)
                handleRuneSlotClick(player, runebook, runeIndex, event.click, useJapanese)
            } else if (cursorItem.type != Material.AIR) {
                // Clicking on empty slot with a rune on cursor - try to add it
                handleRuneDrop(player, runebook, cursorItem, useJapanese)
            }
            // Empty slot clicked with nothing on cursor - do nothing
        }
        // Normal clicks in player inventory are allowed (to pick up runes)
    }

    /**
     * Handle dragging items in the runebook GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if this is a runebook GUI
        if (!openRunebooks.containsKey(player.uniqueId)) return

        val title = event.view.title()
        val isRunebookGUI = title.toString().contains("Runebook") || title.toString().contains("ルーンの書")
        if (!isRunebookGUI) return

        // Cancel dragging in the runebook GUI
        if (event.rawSlots.any { it < RunebookManager.GUI_SIZE }) {
            event.isCancelled = true
        }
    }

    /**
     * Handle dropping a rune in the drop zone
     */
    private fun handleRuneDrop(player: Player, runebook: ItemStack, rune: ItemStack, useJapanese: Boolean) {
        if (!plugin.runeManager.isRune(rune)) {
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_NOT_A_RUNE)
            return
        }

        if (!plugin.runeManager.isMarked(rune)) {
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_RUNE_NOT_MARKED)
            return
        }

        val success = plugin.runebookManager.addRune(runebook, rune, useJapanese)
        if (success) {
            player.world.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f)
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_RUNE_ADDED)
            // Refresh GUI and clear cursor in next tick to avoid timing issues
            plugin.server.scheduler.runTask(plugin, Runnable {
                player.setItemOnCursor(null)
                plugin.runebookManager.openGUI(player, runebook)
            })
        } else {
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_FULL)
        }
    }

    /**
     * Handle clicking on a rune slot
     */
    private fun handleRuneSlotClick(
        player: Player,
        runebook: ItemStack,
        runeIndex: Int,
        click: ClickType,
        useJapanese: Boolean
    ) {
        val runes = plugin.runebookManager.getRunes(runebook)
        if (runeIndex < 0 || runeIndex >= runes.size) return

        val entry = runes[runeIndex]

        when {
            // Right-click: Remove rune
            click.isRightClick -> {
                val removed = plugin.runebookManager.removeRune(runebook, runeIndex, useJapanese)
                if (removed != null) {
                    // Create rune item and give to player
                    val runeItem = plugin.runebookManager.createRuneFromEntry(removed)
                    val leftover = player.inventory.addItem(runeItem)
                    if (leftover.isNotEmpty()) {
                        leftover.values.forEach { item ->
                            player.world.dropItemNaturally(player.location, item)
                        }
                    }
                    player.world.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f)
                    plugin.messageSender.send(player, MessageKey.RUNEBOOK_RUNE_REMOVED, "name" to removed.name)
                    // Refresh GUI
                    plugin.runebookManager.openGUI(player, runebook)
                }
            }

            // Shift+Left-click: Gate Travel
            click.isShiftClick && click.isLeftClick -> {
                player.closeInventory()
                startGateTravel(player, entry)
            }

            // Left-click: Recall
            click.isLeftClick -> {
                player.closeInventory()
                startRecall(player, entry)
            }
        }
    }

    /**
     * Start Recall spell with the given rune entry
     */
    private fun startRecall(player: Player, entry: RunebookManager.RuneEntry) {
        // Validate destination world exists
        val world = plugin.server.getWorld(entry.world)
        if (world == null) {
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_WORLD_NOT_FOUND)
            return
        }

        // Store pending entry for when casting completes
        pendingRecall[player.uniqueId] = entry

        // Cast Recall spell (will check reagents, mana, etc.)
        val result = plugin.spellManager.castSpell(player, SpellType.RECALL, false)

        if (result != com.hacklab.minecraft.skills.magic.CastResult.CASTING) {
            // Casting failed at requirements check, clear pending
            pendingRecall.remove(player.uniqueId)
        }
    }

    /**
     * Start Gate Travel spell with the given rune entry
     */
    private fun startGateTravel(player: Player, entry: RunebookManager.RuneEntry) {
        // Validate destination world exists
        val world = plugin.server.getWorld(entry.world)
        if (world == null) {
            plugin.messageSender.send(player, MessageKey.RUNEBOOK_WORLD_NOT_FOUND)
            return
        }

        // Store pending entry for when casting completes
        pendingGate[player.uniqueId] = entry

        // Cast Gate Travel spell (will check reagents, mana, etc.)
        val result = plugin.spellManager.castSpell(player, SpellType.GATE_TRAVEL, false)

        if (result != com.hacklab.minecraft.skills.magic.CastResult.CASTING) {
            // Casting failed at requirements check, clear pending
            pendingGate.remove(player.uniqueId)
        }
    }

    /**
     * Get pending Recall location for a player (called by CastingManager when spell completes)
     */
    fun getPendingRecallLocation(player: Player): Location? {
        val entry = pendingRecall.remove(player.uniqueId) ?: return null
        val world = plugin.server.getWorld(entry.world) ?: return null
        return Location(world, entry.x, entry.y, entry.z)
    }

    /**
     * Get pending Gate Travel location for a player (called by CastingManager when spell completes)
     */
    fun getPendingGateLocation(player: Player): Location? {
        val entry = pendingGate.remove(player.uniqueId) ?: return null
        val world = plugin.server.getWorld(entry.world) ?: return null
        return Location(world, entry.x, entry.y, entry.z)
    }

    /**
     * Check if player has a pending runebook spell
     */
    fun hasPendingRunebookSpell(player: Player): Boolean {
        return pendingRecall.containsKey(player.uniqueId) || pendingGate.containsKey(player.uniqueId)
    }

    /**
     * Clear pending spells for a player (when cancelled)
     */
    fun clearPending(player: Player) {
        pendingRecall.remove(player.uniqueId)
        pendingGate.remove(player.uniqueId)
    }

    /**
     * Clean up when player closes inventory
     */
    @EventHandler
    fun onInventoryClose(event: org.bukkit.event.inventory.InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        openRunebooks.remove(player.uniqueId)
    }

    /**
     * Clean up when player quits
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        openRunebooks.remove(uuid)
        pendingRecall.remove(uuid)
        pendingGate.remove(uuid)
    }
}
