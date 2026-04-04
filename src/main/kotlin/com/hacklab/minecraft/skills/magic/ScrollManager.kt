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
    val scrollQualityKey = NamespacedKey(plugin, "scroll_quality")

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
     * Create a spell scroll with optional quality
     */
    fun createScroll(spell: SpellType, quality: String? = null): ItemStack {
        val scroll = ItemStack(Material.PAPER)
        val meta = scroll.itemMeta ?: return scroll

        // Set display name based on circle and quality
        val color = getCircleColor(spell.circle)
        val qualityLabel = quality?.let { " [$it]" } ?: ""
        meta.displayName(
            Component.text("Scroll of ${spell.displayName}$qualityLabel")
                .color(color)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Set lore
        val loreList = mutableListOf(
            Component.text("${spell.circle.name.lowercase().replaceFirstChar { it.uppercase() }} Circle Spell")
                .color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("Right-click to cast").color(NamedTextColor.DARK_GRAY),
            Component.text("Mana cost: ${spell.baseMana / 2}").color(NamedTextColor.BLUE)
        )
        if (quality != null) {
            val bonus = getQualityBonusFromName(quality)
            loreList.add(Component.text("Success bonus: +${bonus.toInt()}%").color(NamedTextColor.GREEN))
        }
        meta.lore(loreList)

        // Store scroll data
        meta.persistentDataContainer.set(scrollKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(scrollSpellKey, PersistentDataType.STRING, spell.name)
        if (quality != null) {
            meta.persistentDataContainer.set(scrollQualityKey, PersistentDataType.STRING, quality)
        }

        scroll.itemMeta = meta
        return scroll
    }

    /**
     * Get scroll quality from item
     */
    fun getScrollQuality(item: ItemStack): String? {
        if (!isScroll(item)) return null
        return item.itemMeta?.persistentDataContainer?.get(scrollQualityKey, PersistentDataType.STRING)
    }

    /**
     * Get success bonus percentage for a quality name
     * LQ: +10%, NQ: +20%, HQ: +30%, EX: +40%
     */
    fun getQualityBonusFromName(quality: String): Double {
        return when (quality) {
            "LQ" -> 10.0
            "NQ" -> 20.0
            "HQ" -> 30.0
            "EX" -> 40.0
            else -> 0.0
        }
    }

    /**
     * Attempt to craft a scroll (Inscription skill)
     */
    fun craftScroll(player: Player, spell: SpellType): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val inscriptionSkill = data.getSkillValue(SkillType.INSCRIPTION)

        // Check if player knows the spell (needs it in spellbook)
        if (!plugin.spellbookManager.hasSpell(player, spell)) {
            plugin.messageSender.send(player, MessageKey.MAGIC_SPELL_NOT_IN_BOOK)
            return false
        }

        // Check materials (paper + reagents for the spell)
        if (!player.inventory.contains(Material.PAPER)) {
            plugin.messageSender.send(player, MessageKey.SCRIBE_NO_PAPER)
            return false
        }

        if (!plugin.reagentManager.hasReagents(player, spell)) {
            val missing = plugin.reagentManager.getMissingReagents(player, spell)
            val missingNames = missing.joinToString(", ") { formatMaterialName(it) }
            plugin.messageSender.send(player, MessageKey.SCRIBE_NO_REAGENTS, "reagents" to missingNames)
            return false
        }

        // Calculate success chance
        // Base: inscription skill - (circle * 10) + 50
        val difficulty = spell.circle.number * 10
        val successChance = (inscriptionSkill - difficulty + 50).coerceIn(10.0, 95.0)

        // Show attempt message with success chance
        plugin.messageSender.send(player, MessageKey.SCRIBE_ATTEMPTING,
            "spell" to spell.displayName,
            "chance" to String.format("%.0f", successChance))

        // Consume paper (always consumed on attempt)
        player.inventory.removeItem(ItemStack(Material.PAPER, 1))

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.INSCRIPTION, difficulty)

        // Roll for success
        if (Random.nextDouble() * 100 > successChance) {
            // Failed: only paper is consumed, reagents are NOT consumed (per spec)
            plugin.messageSender.send(player, MessageKey.SCRIBE_FAILED)
            return false
        }

        // Success: consume reagents
        plugin.reagentManager.consumeReagents(player, spell)

        // Determine scroll quality based on Inscription skill
        val quality = when {
            inscriptionSkill >= 90 -> "EX"
            inscriptionSkill >= 70 -> "HQ"
            inscriptionSkill >= 50 -> "NQ"
            else -> "LQ"
        }

        // Create and give scroll with quality
        val scroll = createScroll(spell, quality)
        player.inventory.addItem(scroll)

        plugin.messageSender.send(player, MessageKey.SCRIBE_SUCCESS, "spell" to spell.displayName)
        return true
    }

    /**
     * Format material name for display (e.g., SPIDER_EYE -> Spider Eye)
     */
    private fun formatMaterialName(material: Material): String {
        return material.name.lowercase().split("_").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
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
