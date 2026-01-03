package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionType

/**
 * Manages custom poison potion items (Greater and Deadly)
 */
class PoisonItemManager(private val plugin: Skills) {
    private val poisonLevelKey = NamespacedKey(plugin, "poison_level")

    /**
     * Create a custom poison potion
     */
    fun createPoisonPotion(level: PoisonLevel, useJapanese: Boolean = false): ItemStack {
        val potion = ItemStack(Material.POTION)
        val meta = potion.itemMeta as PotionMeta

        // Set base potion type (visual only)
        meta.basePotionType = PotionType.POISON

        // Set custom name
        val displayName = if (useJapanese) level.displayNameJa else level.displayName
        meta.displayName(
            Component.text(displayName)
                .color(level.color)
                .decoration(TextDecoration.ITALIC, false)
        )

        // Set lore
        val lore = mutableListOf<Component>()
        if (useJapanese) {
            lore.add(Component.text("ダメージ: ${level.damagePerTick}/秒").color(NamedTextColor.GRAY))
            lore.add(Component.text("必要Alchemy: ${level.alchemyRequired}").color(NamedTextColor.GRAY))
        } else {
            lore.add(Component.text("Damage: ${level.damagePerTick}/sec").color(NamedTextColor.GRAY))
            lore.add(Component.text("Required Alchemy: ${level.alchemyRequired}").color(NamedTextColor.GRAY))
        }
        meta.lore(lore)

        // Store poison level in PDC
        meta.persistentDataContainer.set(poisonLevelKey, PersistentDataType.STRING, level.name)

        // Set custom potion color
        meta.setColor(org.bukkit.Color.fromRGB(
            when (level) {
                PoisonLevel.LESSER -> 0x4E9A06      // Light green
                PoisonLevel.REGULAR -> 0x2E7D32    // Green
                PoisonLevel.GREATER -> 0x00838F   // Cyan
                PoisonLevel.DEADLY -> 0x6A1B9A    // Purple
            }
        ))

        potion.itemMeta = meta
        return potion
    }

    /**
     * Get poison level from an item
     */
    fun getPoisonLevel(item: ItemStack?): PoisonLevel? {
        if (item == null || item.type != Material.POTION) return null

        val meta = item.itemMeta as? PotionMeta ?: return null

        // Check for custom poison level first
        val customLevel = meta.persistentDataContainer.get(poisonLevelKey, PersistentDataType.STRING)
        if (customLevel != null) {
            return try {
                PoisonLevel.valueOf(customLevel)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // Check vanilla poison potions
        val potionType = meta.basePotionType ?: return null
        val typeName = potionType.name

        return when {
            typeName == "STRONG_POISON" -> PoisonLevel.REGULAR  // Poison II = Regular
            typeName == "LONG_POISON" -> PoisonLevel.LESSER     // Extended = Lesser (longer but weaker)
            typeName == "POISON" -> PoisonLevel.LESSER          // Regular poison = Lesser
            else -> null
        }
    }

    /**
     * Check if item is a poison potion (custom or vanilla)
     */
    fun isPoisonPotion(item: ItemStack?): Boolean {
        return getPoisonLevel(item) != null
    }
}
