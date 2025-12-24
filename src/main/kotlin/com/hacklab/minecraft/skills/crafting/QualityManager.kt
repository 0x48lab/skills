package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class QualityManager(private val plugin: Skills) {
    private val qualityKey = NamespacedKey(plugin, "quality")

    /**
     * Get quality from an item
     */
    fun getQuality(item: ItemStack?): QualityType {
        if (item == null) return QualityType.NORMAL_QUALITY

        val qualityName = item.itemMeta?.persistentDataContainer
            ?.get(qualityKey, PersistentDataType.STRING)

        return qualityName?.let {
            try { QualityType.valueOf(it) } catch (e: Exception) { QualityType.NORMAL_QUALITY }
        } ?: QualityType.NORMAL_QUALITY
    }

    /**
     * Get quality modifier for damage/effect calculations
     */
    fun getQualityModifier(item: ItemStack?): Double {
        return getQuality(item).modifier
    }

    /**
     * Set quality on an item
     */
    fun setQuality(item: ItemStack, quality: QualityType): ItemStack {
        val meta = item.itemMeta ?: return item

        // Store quality in persistent data
        meta.persistentDataContainer.set(qualityKey, PersistentDataType.STRING, quality.name)

        // Update display name with quality prefix
        val baseName = meta.displayName() ?: Component.translatable(item.type.translationKey())
        val qualityPrefix = Component.text("[${quality.shortName}] ")
            .color(quality.color)
            .decoration(TextDecoration.ITALIC, false)

        meta.displayName(qualityPrefix.append(baseName))

        // Update lore
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        // Remove existing quality lore if present
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
            plain.startsWith("Quality:")
        }
        // Add quality lore
        lore.add(0, Component.text("Quality: ${quality.displayName}").color(quality.color))
        meta.lore(lore)

        item.itemMeta = meta
        return item
    }

    /**
     * Check if item has quality set
     */
    fun hasQuality(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.itemMeta?.persistentDataContainer?.has(qualityKey, PersistentDataType.STRING) == true
    }

    /**
     * Calculate and apply quality based on skill
     */
    fun applyQualityFromSkill(item: ItemStack, skillValue: Double): ItemStack {
        val quality = QualityType.calculateQuality(skillValue)
        return setQuality(item, quality)
    }

    /**
     * Get quality display for arms lore
     */
    fun getQualityDescription(item: ItemStack): Component {
        val quality = getQuality(item)
        return Component.text(quality.displayName).color(quality.color)
    }
}
