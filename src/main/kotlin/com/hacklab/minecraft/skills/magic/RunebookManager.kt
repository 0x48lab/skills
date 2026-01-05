package com.hacklab.minecraft.skills.magic

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * ルーンブック管理 - チェストのようにルーンを保存するシンプルな仕組み
 */
class RunebookManager(private val plugin: Skills) {

    private val runebookKey = NamespacedKey(plugin, "runebook")
    private val runesKey = NamespacedKey(plugin, "runebook_runes")

    // ルーン用PDCキー（RuneManagerと互換）
    private val runeKey = NamespacedKey(plugin, "rune")
    private val runeLocationKey = NamespacedKey(plugin, "rune_location")
    private val runeWorldKey = NamespacedKey(plugin, "rune_world")
    private val runeNameKey = NamespacedKey(plugin, "rune_name")

    private val gson = Gson()

    companion object {
        const val MAX_RUNES = 16
        const val GUI_SIZE = 18  // 2行
    }

    data class RuneEntry(
        val name: String,
        val world: String,
        val x: Double,
        val y: Double,
        val z: Double
    )

    // ==================== ルーンブック操作 ====================

    /**
     * 空のルーンブックを作成
     */
    fun createRunebook(useJapanese: Boolean): ItemStack {
        val runebook = ItemStack(Material.BOOK)
        val meta = runebook.itemMeta ?: return runebook

        val displayName = if (useJapanese) "ルーンの書" else "Runebook"
        meta.displayName(
            Component.text(displayName)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        updateLore(meta, 0, useJapanese)

        meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        meta.persistentDataContainer.set(runebookKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(runesKey, PersistentDataType.STRING, "[]")

        runebook.itemMeta = meta
        return runebook
    }

    fun isRunebook(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.BOOK) return false
        return item.itemMeta?.persistentDataContainer?.has(runebookKey, PersistentDataType.BYTE) == true
    }

    // ==================== GUI操作 ====================

    /**
     * ルーンブックGUIを開く - チェストのように実際のルーンアイテムを配置
     */
    fun openGUI(player: Player, runebook: ItemStack) {
        if (!isRunebook(runebook)) return

        val useJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE
        val title = if (useJapanese) "ルーンの書" else "Runebook"

        val inventory = Bukkit.createInventory(null, GUI_SIZE, Component.text(title).color(NamedTextColor.DARK_PURPLE))

        // 保存されたルーンエントリからルーンアイテムを生成して配置
        val runes = getRunes(runebook)
        for (i in runes.indices) {
            if (i >= MAX_RUNES) break
            val slot = if (i < 8) i else (i + 1)  // スロット8と17はスキップ
            inventory.setItem(slot, createRuneFromEntry(runes[i]))
        }

        player.openInventory(inventory)
    }

    /**
     * インベントリの内容をルーンブックに保存
     */
    fun saveFromInventory(runebook: ItemStack, inventory: Inventory, useJapanese: Boolean) {
        if (!isRunebook(runebook)) return

        val entries = mutableListOf<RuneEntry>()

        // 全スロットをスキャンしてルーンを収集
        for (i in 0 until GUI_SIZE) {
            val item = inventory.getItem(i) ?: continue
            val entry = extractRuneEntry(item)
            if (entry != null) {
                entries.add(entry)
            }
        }

        // ルーンブックに保存
        val meta = runebook.itemMeta ?: return
        meta.persistentDataContainer.set(runesKey, PersistentDataType.STRING, gson.toJson(entries))
        updateLore(meta, entries.size, useJapanese)
        runebook.itemMeta = meta
    }

    private fun updateLore(meta: org.bukkit.inventory.meta.ItemMeta, runeCount: Int, useJapanese: Boolean) {
        val lore = if (useJapanese) {
            listOf(
                Component.text("複数のルーンを保存できる魔法の本").color(NamedTextColor.GRAY),
                Component.text(""),
                Component.text("右クリックで開く").color(NamedTextColor.YELLOW),
                Component.text("登録ルーン: $runeCount/$MAX_RUNES").color(NamedTextColor.AQUA)
            )
        } else {
            listOf(
                Component.text("A magical book that stores multiple runes").color(NamedTextColor.GRAY),
                Component.text(""),
                Component.text("Right-click to open").color(NamedTextColor.YELLOW),
                Component.text("Runes: $runeCount/$MAX_RUNES").color(NamedTextColor.AQUA)
            )
        }
        meta.lore(lore)
    }

    // ==================== ルーンデータ操作 ====================

    /**
     * ルーンブックに保存されたエントリを取得
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
            emptyList()
        }
    }

    /**
     * ルーンアイテムからエントリを抽出
     */
    fun extractRuneEntry(item: ItemStack): RuneEntry? {
        if (item.type != Material.AMETHYST_SHARD) return null

        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer

        val locationStr = pdc.get(runeLocationKey, PersistentDataType.STRING) ?: return null
        val worldName = pdc.get(runeWorldKey, PersistentDataType.STRING) ?: return null
        val name = pdc.get(runeNameKey, PersistentDataType.STRING) ?: "Unknown"

        val parts = locationStr.split(",")
        if (parts.size != 3) return null

        return try {
            RuneEntry(name, worldName, parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * エントリからルーンアイテムを作成
     */
    fun createRuneFromEntry(entry: RuneEntry): ItemStack {
        val rune = ItemStack(Material.AMETHYST_SHARD)
        val meta = rune.itemMeta ?: return rune

        meta.displayName(
            Component.text("Rune: ${entry.name}")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
        )

        meta.lore(listOf(
            Component.text("A marked magical rune").color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("Location: ${entry.x.toInt()}, ${entry.y.toInt()}, ${entry.z.toInt()}").color(NamedTextColor.AQUA),
            Component.text("World: ${entry.world}").color(NamedTextColor.AQUA)
        ))

        val pdc = meta.persistentDataContainer
        pdc.set(runeKey, PersistentDataType.BYTE, 1)
        pdc.set(runeLocationKey, PersistentDataType.STRING, "${entry.x},${entry.y},${entry.z}")
        pdc.set(runeWorldKey, PersistentDataType.STRING, entry.world)
        pdc.set(runeNameKey, PersistentDataType.STRING, entry.name)

        rune.itemMeta = meta
        return rune
    }

    /**
     * エントリからLocationを取得
     */
    fun getLocation(entry: RuneEntry): Location? {
        val world = plugin.server.getWorld(entry.world) ?: return null
        return Location(world, entry.x, entry.y, entry.z)
    }

    /**
     * マーク済みルーンかどうか確認
     */
    fun isValidMarkedRune(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.AMETHYST_SHARD) return false
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(runeLocationKey, PersistentDataType.STRING)
    }
}
