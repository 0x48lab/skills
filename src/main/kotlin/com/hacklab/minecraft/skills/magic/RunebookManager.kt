package com.hacklab.minecraft.skills.magic

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Manages Runebooks - items that store multiple runes for easy Recall/Gate Travel
 * Similar to UO's Runebook system
 */
class RunebookManager(private val plugin: Skills) {

    private val runebookKey = NamespacedKey(plugin, "runebook")
    private val runesKey = NamespacedKey(plugin, "runebook_runes")

    companion object {
        const val MAX_RUNES = 16
        const val GUI_ROWS = 3
        const val GUI_SIZE = GUI_ROWS * 9  // 27 slots

        // GUI slot positions
        const val INFO_SLOT = 8          // Row 1, last slot
        const val CLOSE_SLOT = 17        // Row 2, last slot
        const val DROP_ZONE_START = 18   // Row 3 start
        const val DROP_ZONE_END = 26     // Row 3 end
    }

    /**
     * Data class for rune entries stored in runebook
     */
    data class RuneEntry(
        val name: String,
        val world: String,
        val x: Double,
        val y: Double,
        val z: Double
    )

    /**
     * Create a new empty runebook
     */
    fun createRunebook(useJapanese: Boolean): ItemStack {
        val runebook = ItemStack(Material.BOOK)
        val meta = runebook.itemMeta ?: return runebook

        // Display name
        val displayName = if (useJapanese) "ルーンブック" else "Runebook"
        meta.displayName(
            Component.text(displayName)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Lore
        val lore = mutableListOf<Component>()
        if (useJapanese) {
            lore.add(Component.text("複数のルーンを保存できる魔法の本").color(NamedTextColor.GRAY))
            lore.add(Component.text(""))
            lore.add(Component.text("右クリックで開く").color(NamedTextColor.YELLOW))
            lore.add(Component.text("登録ルーン: 0/${MAX_RUNES}").color(NamedTextColor.AQUA))
        } else {
            lore.add(Component.text("A magical book that stores multiple runes").color(NamedTextColor.GRAY))
            lore.add(Component.text(""))
            lore.add(Component.text("Right-click to open").color(NamedTextColor.YELLOW))
            lore.add(Component.text("Runes: 0/${MAX_RUNES}").color(NamedTextColor.AQUA))
        }
        meta.lore(lore)

        // Enchant glow effect
        meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        // PDC data
        meta.persistentDataContainer.set(runebookKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(runesKey, PersistentDataType.STRING, "[]")

        runebook.itemMeta = meta
        return runebook
    }

    /**
     * Check if an item is a runebook
     */
    fun isRunebook(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.BOOK) return false
        return item.itemMeta?.persistentDataContainer?.has(runebookKey, PersistentDataType.BYTE) == true
    }

    private val gson = Gson()

    /**
     * Get all rune entries from a runebook
     */
    fun getRunes(runebook: ItemStack): List<RuneEntry> {
        if (!isRunebook(runebook)) return emptyList()

        val meta = runebook.itemMeta ?: return emptyList()
        val jsonString = meta.persistentDataContainer.get(runesKey, PersistentDataType.STRING)
            ?: return emptyList()

        return try {
            val listType = object : TypeToken<List<RuneEntry>>() {}.type
            gson.fromJson<List<RuneEntry>>(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to parse runebook runes: ${e.message}")
            emptyList()
        }
    }

    /**
     * Add a rune to the runebook
     * @return true if successful, false if book is full or rune is invalid
     */
    fun addRune(runebook: ItemStack, rune: ItemStack, useJapanese: Boolean): Boolean {
        if (!isRunebook(runebook)) return false
        if (!plugin.runeManager.isRune(rune) || !plugin.runeManager.isMarked(rune)) return false

        val currentRunes = getRunes(runebook).toMutableList()
        if (currentRunes.size >= MAX_RUNES) return false

        // Extract rune data
        val location = plugin.runeManager.getMarkedLocation(rune) ?: return false
        val name = plugin.runeManager.getRuneName(rune) ?: "Unknown"

        val entry = RuneEntry(
            name = name,
            world = location.world?.name ?: return false,
            x = location.x,
            y = location.y,
            z = location.z
        )

        currentRunes.add(entry)
        saveRunes(runebook, currentRunes, useJapanese)
        return true
    }

    /**
     * Remove a rune from the runebook by index
     * @return the removed rune entry, or null if invalid index
     */
    fun removeRune(runebook: ItemStack, index: Int, useJapanese: Boolean): RuneEntry? {
        if (!isRunebook(runebook)) return null

        val currentRunes = getRunes(runebook).toMutableList()
        if (index < 0 || index >= currentRunes.size) return null

        val removed = currentRunes.removeAt(index)
        saveRunes(runebook, currentRunes, useJapanese)
        return removed
    }

    /**
     * Save runes to the runebook and update lore
     */
    private fun saveRunes(runebook: ItemStack, runes: List<RuneEntry>, useJapanese: Boolean) {
        val meta = runebook.itemMeta ?: return

        // Save to PDC as JSON using Gson
        val jsonString = gson.toJson(runes)
        meta.persistentDataContainer.set(runesKey, PersistentDataType.STRING, jsonString)

        // Update lore
        val lore = mutableListOf<Component>()
        if (useJapanese) {
            lore.add(Component.text("複数のルーンを保存できる魔法の本").color(NamedTextColor.GRAY))
            lore.add(Component.text(""))
            lore.add(Component.text("右クリックで開く").color(NamedTextColor.YELLOW))
            lore.add(Component.text("登録ルーン: ${runes.size}/${MAX_RUNES}").color(NamedTextColor.AQUA))
        } else {
            lore.add(Component.text("A magical book that stores multiple runes").color(NamedTextColor.GRAY))
            lore.add(Component.text(""))
            lore.add(Component.text("Right-click to open").color(NamedTextColor.YELLOW))
            lore.add(Component.text("Runes: ${runes.size}/${MAX_RUNES}").color(NamedTextColor.AQUA))
        }

        // Add rune names to lore (first 5)
        if (runes.isNotEmpty()) {
            lore.add(Component.text(""))
            runes.take(5).forEach { entry ->
                lore.add(Component.text("  - ${entry.name}").color(NamedTextColor.LIGHT_PURPLE))
            }
            if (runes.size > 5) {
                val moreText = if (useJapanese) "他${runes.size - 5}件..." else "...and ${runes.size - 5} more"
                lore.add(Component.text("  $moreText").color(NamedTextColor.DARK_GRAY))
            }
        }

        meta.lore(lore)
        runebook.itemMeta = meta
    }

    /**
     * Open the runebook GUI for a player
     */
    fun openGUI(player: Player, runebook: ItemStack) {
        if (!isRunebook(runebook)) return

        val useJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE
        val title = if (useJapanese) "ルーンブック" else "Runebook"

        val inventory = Bukkit.createInventory(null, GUI_SIZE, Component.text(title).color(NamedTextColor.DARK_PURPLE))

        val runes = getRunes(runebook)

        // Row 1-2: Rune slots (0-7, 9-16)
        for (i in 0 until MAX_RUNES) {
            val slot = if (i < 8) i else (i + 1)  // Skip slot 8 (info) and continue at slot 9

            if (i < runes.size) {
                // Filled rune slot
                val entry = runes[i]
                inventory.setItem(slot, createRuneSlotItem(entry, i, useJapanese))
            } else {
                // Empty slot
                inventory.setItem(slot, createEmptySlotItem(useJapanese))
            }
        }

        // Info button (slot 8)
        inventory.setItem(INFO_SLOT, createInfoItem(runes.size, useJapanese))

        // Close button (slot 17)
        inventory.setItem(CLOSE_SLOT, createCloseItem(useJapanese))

        // Row 3: Drop zone for adding runes (slots 18-26)
        val dropZoneItem = createDropZoneItem(useJapanese)
        for (slot in DROP_ZONE_START..DROP_ZONE_END) {
            inventory.setItem(slot, dropZoneItem)
        }

        player.openInventory(inventory)
    }

    /**
     * Create an item representing a filled rune slot
     */
    private fun createRuneSlotItem(entry: RuneEntry, index: Int, useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.ENDER_EYE)
        val meta = item.itemMeta ?: return item

        meta.displayName(
            Component.text(entry.name)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        val lore = mutableListOf<Component>()
        lore.add(Component.text("${entry.world}").color(NamedTextColor.GRAY))
        lore.add(Component.text("X: ${entry.x.toInt()}, Y: ${entry.y.toInt()}, Z: ${entry.z.toInt()}").color(NamedTextColor.AQUA))
        lore.add(Component.text(""))

        if (useJapanese) {
            lore.add(Component.text("左クリック: Recall").color(NamedTextColor.GREEN))
            lore.add(Component.text("Shift+左: Gate Travel").color(NamedTextColor.LIGHT_PURPLE))
            lore.add(Component.text("右クリック: 削除").color(NamedTextColor.RED))
        } else {
            lore.add(Component.text("Left-click: Recall").color(NamedTextColor.GREEN))
            lore.add(Component.text("Shift+Left: Gate Travel").color(NamedTextColor.LIGHT_PURPLE))
            lore.add(Component.text("Right-click: Remove").color(NamedTextColor.RED))
        }

        meta.lore(lore)

        // Store index in PDC for click handling
        meta.persistentDataContainer.set(
            NamespacedKey(plugin, "rune_index"),
            PersistentDataType.INTEGER,
            index
        )

        item.itemMeta = meta
        return item
    }

    /**
     * Create an item representing an empty rune slot
     */
    private fun createEmptySlotItem(useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item

        val name = if (useJapanese) "空きスロット" else "Empty Slot"
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false)
        )

        item.itemMeta = meta
        return item
    }

    /**
     * Create the info item
     */
    private fun createInfoItem(runeCount: Int, useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta ?: return item

        val name = if (useJapanese) "情報" else "Info"
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
        )

        val lore = mutableListOf<Component>()
        if (useJapanese) {
            lore.add(Component.text("登録ルーン: $runeCount/${MAX_RUNES}").color(NamedTextColor.WHITE))
            lore.add(Component.text(""))
            lore.add(Component.text("下の行にルーンをドロップして追加").color(NamedTextColor.YELLOW))
        } else {
            lore.add(Component.text("Runes: $runeCount/${MAX_RUNES}").color(NamedTextColor.WHITE))
            lore.add(Component.text(""))
            lore.add(Component.text("Drop runes on bottom row to add").color(NamedTextColor.YELLOW))
        }

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    /**
     * Create the close button item
     */
    private fun createCloseItem(useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta ?: return item

        val name = if (useJapanese) "閉じる" else "Close"
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
        )

        item.itemMeta = meta
        return item
    }

    /**
     * Create the drop zone indicator item
     */
    private fun createDropZoneItem(useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item

        val name = if (useJapanese) "ルーンをここにドロップ" else "Drop Rune Here"
        meta.displayName(
            Component.text(name)
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
        )

        val lore = mutableListOf<Component>()
        if (useJapanese) {
            lore.add(Component.text("記録済みのルーンをドロップして").color(NamedTextColor.GRAY))
            lore.add(Component.text("ルーンブックに追加").color(NamedTextColor.GRAY))
        } else {
            lore.add(Component.text("Drop a marked rune here to").color(NamedTextColor.GRAY))
            lore.add(Component.text("add it to the runebook").color(NamedTextColor.GRAY))
        }

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    /**
     * Get the rune index from a clicked item
     */
    fun getRuneIndex(item: ItemStack?): Int? {
        if (item == null) return null
        return item.itemMeta?.persistentDataContainer?.get(
            NamespacedKey(plugin, "rune_index"),
            PersistentDataType.INTEGER
        )
    }

    /**
     * Create a rune ItemStack from a RuneEntry (for returning runes to player)
     */
    fun createRuneFromEntry(entry: RuneEntry): ItemStack {
        val rune = plugin.runeManager.createRune()
        val world = plugin.server.getWorld(entry.world) ?: return rune
        val location = org.bukkit.Location(world, entry.x, entry.y, entry.z)
        plugin.runeManager.markRune(rune, location, entry.name)
        return rune
    }
}
