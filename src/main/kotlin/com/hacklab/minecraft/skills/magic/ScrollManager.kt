package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random

class ScrollManager(private val plugin: Skills) {
    private val scrollKey = NamespacedKey(plugin, "scroll")
    private val scrollSpellKey = NamespacedKey(plugin, "scroll_spell")

    /**
     * Check if an item is a spell scroll
     */
    fun isScroll(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.PAPER) {
            return false
        }
        return item.itemMeta?.persistentDataContainer?.has(scrollKey, PersistentDataType.BYTE) == true
    }

    /**
     * Get the spell from a scroll
     */
    fun getSpell(scroll: ItemStack): SpellType? {
        if (!isScroll(scroll)) return null

        val spellName = scroll.itemMeta?.persistentDataContainer
            ?.get(scrollSpellKey, PersistentDataType.STRING) ?: return null

        return try {
            SpellType.valueOf(spellName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Create a spell scroll
     */
    fun createScroll(spell: SpellType): ItemStack {
        val scroll = ItemStack(Material.PAPER)
        val meta = scroll.itemMeta ?: return scroll

        // Set display name based on circle
        val color = getCircleColor(spell.circle)
        meta.displayName(
            Component.text("Scroll of ${spell.displayName}")
                .color(color)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Set lore
        meta.lore(listOf(
            Component.text("${spell.circle.name.lowercase().replaceFirstChar { it.uppercase() }} Circle Spell")
                .color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("Right-click to cast").color(NamedTextColor.DARK_GRAY),
            Component.text("Mana cost: ${spell.baseMana / 2}").color(NamedTextColor.BLUE)
        ))

        // Store scroll data
        meta.persistentDataContainer.set(scrollKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(scrollSpellKey, PersistentDataType.STRING, spell.name)

        scroll.itemMeta = meta
        return scroll
    }

    /**
     * Attempt to craft a scroll (Inscription skill)
     */
    fun craftScroll(player: Player, spell: SpellType): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val inscriptionSkill = data.getSkillValue(SkillType.INSCRIPTION)
        val magerySkill = data.getSkillValue(SkillType.MAGERY)

        // Check if player knows the spell (needs it in spellbook)
        if (!plugin.spellbookManager.hasSpell(player, spell)) {
            plugin.messageSender.send(player, MessageKey.MAGIC_SPELL_NOT_IN_BOOK)
            return false
        }

        // Check materials (paper + reagents for the spell)
        if (!player.inventory.contains(Material.PAPER)) {
            return false
        }

        if (!plugin.reagentManager.hasReagents(player, spell)) {
            plugin.messageSender.send(player, MessageKey.MAGIC_NO_REAGENTS)
            return false
        }

        // Calculate success chance
        // Base: inscription skill - (circle * 10) + 50
        val difficulty = spell.circle.number * 10
        val successChance = (inscriptionSkill - difficulty + 50).coerceIn(10.0, 95.0)

        // Consume materials regardless of success
        player.inventory.removeItem(ItemStack(Material.PAPER, 1))
        plugin.reagentManager.consumeReagents(player, spell)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.INSCRIPTION, difficulty)

        // Roll for success
        if (Random.nextDouble() * 100 > successChance) {
            plugin.messageSender.send(player, MessageKey.SCROLL_FAILED)
            return false
        }

        // Create and give scroll
        val scroll = createScroll(spell)
        player.inventory.addItem(scroll)

        plugin.messageSender.send(player, MessageKey.SCROLL_CREATED, "spell" to spell.displayName)
        return true
    }

    /**
     * Use a scroll to cast a spell (one-time use, does not learn)
     * To learn a spell, drag the scroll onto a spellbook in inventory
     * Note: Scroll is NOT consumed here - it's consumed in finalizeCast
     */
    fun useScroll(player: Player, scroll: ItemStack): Boolean {
        val spell = getSpell(scroll) ?: return false

        // Don't consume scroll here - consume when spell actually fires (finalizeCast)
        // This allows cancellation to keep the scroll

        // Cast through spell manager with scroll flag
        plugin.spellManager.castSpell(player, spell, useScroll = true)

        return true
    }

    /**
     * Consume a scroll of the specified spell from player's inventory
     * Called by SpellManager.finalizeCast when spell fires
     */
    fun consumeScroll(player: Player, spell: SpellType): Boolean {
        try {
            // Check main hand first
            val mainHand = player.inventory.itemInMainHand
            if (isScroll(mainHand)) {
                val scrollSpell = getSpell(mainHand)
                if (scrollSpell == spell) {
                    if (mainHand.amount > 1) {
                        mainHand.amount -= 1
                    } else {
                        player.inventory.setItemInMainHand(null)
                    }
                    plugin.messageSender.send(player, MessageKey.SCROLL_USED, "spell" to spell.displayName)
                    return true
                }
            }

            // Check off hand
            val offHand = player.inventory.itemInOffHand
            if (isScroll(offHand)) {
                val scrollSpell = getSpell(offHand)
                if (scrollSpell == spell) {
                    if (offHand.amount > 1) {
                        offHand.amount -= 1
                    } else {
                        player.inventory.setItemInOffHand(null)
                    }
                    plugin.messageSender.send(player, MessageKey.SCROLL_USED, "spell" to spell.displayName)
                    return true
                }
            }

            // Search inventory (limited to avoid timeout)
            val inventorySize = minOf(player.inventory.size, 36)
            for (i in 0 until inventorySize) {
                val item = player.inventory.getItem(i) ?: continue
                if (isScroll(item)) {
                    val scrollSpell = getSpell(item)
                    if (scrollSpell == spell) {
                        if (item.amount > 1) {
                            item.amount -= 1
                        } else {
                            player.inventory.setItem(i, null)
                        }
                        plugin.messageSender.send(player, MessageKey.SCROLL_USED, "spell" to spell.displayName)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error consuming scroll: ${e.message}")
        }

        return false
    }

    /**
     * Get color for spell circle
     */
    private fun getCircleColor(circle: SpellCircle): NamedTextColor {
        return when (circle) {
            SpellCircle.FIRST -> NamedTextColor.WHITE
            SpellCircle.SECOND -> NamedTextColor.GREEN
            SpellCircle.THIRD -> NamedTextColor.AQUA
            SpellCircle.FOURTH -> NamedTextColor.BLUE
            SpellCircle.FIFTH -> NamedTextColor.LIGHT_PURPLE
            SpellCircle.SIXTH -> NamedTextColor.YELLOW
            SpellCircle.SEVENTH -> NamedTextColor.GOLD
            SpellCircle.EIGHTH -> NamedTextColor.RED
        }
    }
}
