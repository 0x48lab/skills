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
        const val GUI_ROWS = 2
        const val GUI_SIZE = GUI_ROWS * 9  // 18 slots (16 rune slots + 2 unused)
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
        val displayName = if (useJapanese) "ルーンの書" else "Runebook"
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
     * Simple 2-row layout: 16 rune slots, empty slots are just empty (AIR)
     */
    fun openGUI(player: Player, runebook: ItemStack) {
        if (!isRunebook(runebook)) return

        val useJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE
        val title = if (useJapanese) "ルーンの書" else "Runebook"

        val inventory = Bukkit.createInventory(null, GUI_SIZE, Component.text(title).color(NamedTextColor.DARK_PURPLE))

        val runes = getRunes(runebook)

        // Slots 0-15: Rune slots (row 1: 0-7, row 2: 9-16, skip 8 and 17)
        for (i in 0 until MAX_RUNES) {
            val slot = if (i < 8) i else (i + 1)  // Skip slot 8, continue at slot 9

            if (i < runes.size) {
                // Filled rune slot
                val entry = runes[i]
                inventory.setItem(slot, createRuneSlotItem(entry, i, useJapanese))
            }
            // Empty slots remain null (AIR) - no glass panes
        }

        player.openInventory(inventory)
    }

    /**
     * Create an item representing a filled rune slot
     */
    private fun createRuneSlotItem(entry: RuneEntry, index: Int, useJapanese: Boolean): ItemStack {
        val item = ItemStack(Material.AMETHYST_SHARD)
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
            lore.add(Component.text("左クリック: 移動").color(NamedTextColor.YELLOW))
            lore.add(Component.text("右クリック: Recall").color(NamedTextColor.GREEN))
            lore.add(Component.text("Shift+右: Gate Travel").color(NamedTextColor.LIGHT_PURPLE))
        } else {
            lore.add(Component.text("Left-click: Move").color(NamedTextColor.YELLOW))
            lore.add(Component.text("Right-click: Recall").color(NamedTextColor.GREEN))
            lore.add(Component.text("Shift+Right: Gate Travel").color(NamedTextColor.LIGHT_PURPLE))
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
