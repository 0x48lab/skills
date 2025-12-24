package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class RuneManager(private val plugin: Skills) {
    private val runeKey = NamespacedKey(plugin, "rune")
    private val runeLocationKey = NamespacedKey(plugin, "rune_location")
    private val runeWorldKey = NamespacedKey(plugin, "rune_world")
    private val runeNameKey = NamespacedKey(plugin, "rune_name")

    /**
     * Check if an item is a rune
     */
    fun isRune(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.AMETHYST_SHARD) {
            return false
        }
        return item.itemMeta?.persistentDataContainer?.has(runeKey, PersistentDataType.BYTE) == true
    }

    /**
     * Check if a rune is marked (has a location)
     */
    fun isMarked(rune: ItemStack): Boolean {
        if (!isRune(rune)) return false
        return rune.itemMeta?.persistentDataContainer?.has(runeLocationKey, PersistentDataType.STRING) == true
    }

    /**
     * Create a new blank rune
     */
    fun createRune(): ItemStack {
        val rune = ItemStack(Material.AMETHYST_SHARD)
        val meta = rune.itemMeta ?: return rune

        meta.displayName(Component.text("Blank Rune").color(NamedTextColor.LIGHT_PURPLE))
        meta.lore(listOf(
            Component.text("An unmarked magical rune").color(NamedTextColor.GRAY),
            Component.text("Use Mark spell to bind a location").color(NamedTextColor.DARK_GRAY)
        ))

        meta.persistentDataContainer.set(runeKey, PersistentDataType.BYTE, 1)

        rune.itemMeta = meta
        return rune
    }

    /**
     * Mark a rune with a location
     */
    fun markRune(rune: ItemStack, location: Location, name: String? = null): Boolean {
        if (!isRune(rune)) return false

        val meta = rune.itemMeta ?: return false

        // Store location
        val locationString = "${location.x},${location.y},${location.z}"
        meta.persistentDataContainer.set(runeLocationKey, PersistentDataType.STRING, locationString)
        meta.persistentDataContainer.set(runeWorldKey, PersistentDataType.STRING, location.world.name)

        // Store name if provided
        val displayName = name ?: "Marked Location"
        meta.persistentDataContainer.set(runeNameKey, PersistentDataType.STRING, displayName)

        // Update display
        meta.displayName(Component.text("Rune: $displayName").color(NamedTextColor.GOLD))
        meta.lore(listOf(
            Component.text("A marked magical rune").color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("Location: ${location.blockX}, ${location.blockY}, ${location.blockZ}").color(NamedTextColor.AQUA),
            Component.text("World: ${location.world.name}").color(NamedTextColor.AQUA)
        ))

        rune.itemMeta = meta
        return true
    }

    /**
     * Get the marked location from a rune
     */
    fun getMarkedLocation(rune: ItemStack): Location? {
        if (!isRune(rune) || !isMarked(rune)) return null

        val meta = rune.itemMeta ?: return null
        val container = meta.persistentDataContainer

        val locationString = container.get(runeLocationKey, PersistentDataType.STRING) ?: return null
        val worldName = container.get(runeWorldKey, PersistentDataType.STRING) ?: return null

        val world = plugin.server.getWorld(worldName) ?: return null
        val parts = locationString.split(",")
        if (parts.size != 3) return null

        return try {
            Location(world, parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Get the name of a marked rune
     */
    fun getRuneName(rune: ItemStack): String? {
        if (!isRune(rune)) return null
        return rune.itemMeta?.persistentDataContainer?.get(runeNameKey, PersistentDataType.STRING)
    }

    /**
     * Get rune from player's main hand or off hand
     */
    fun getHeldRune(player: Player): ItemStack? {
        val mainHand = player.inventory.itemInMainHand
        if (isRune(mainHand)) return mainHand

        val offHand = player.inventory.itemInOffHand
        if (isRune(offHand)) return offHand

        return null
    }

    /**
     * Get marked rune from player (for Recall/Gate Travel)
     * For scroll usage, checks off-hand specifically
     */
    fun getMarkedRune(player: Player, isScroll: Boolean = false): ItemStack? {
        if (isScroll) {
            // For scrolls, rune must be in off-hand
            val offHand = player.inventory.itemInOffHand
            if (isRune(offHand) && isMarked(offHand)) return offHand
            return null
        }

        // For regular casting, check both hands
        val held = getHeldRune(player)
        if (held != null && isMarked(held)) return held
        return null
    }
}
