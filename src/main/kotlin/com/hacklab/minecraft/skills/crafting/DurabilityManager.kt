package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * Manages custom max durability for items (UO-style repair degradation)
 *
 * In UO, repairing items reduces their max durability over time.
 * Higher Blacksmithy skill = less max durability lost per repair.
 * Even GM blacksmiths lose some max durability (minimum 1).
 */
class DurabilityManager(private val plugin: Skills) {

    private val maxDurabilityKey = NamespacedKey(plugin, "max_durability")
    private val originalMaxDurabilityKey = NamespacedKey(plugin, "original_max_durability")

    /**
     * Get the custom max durability of an item
     * Returns null if item has no custom max (uses vanilla max)
     */
    fun getCustomMaxDurability(item: ItemStack): Int? {
        val meta = item.itemMeta ?: return null
        val pdc = meta.persistentDataContainer
        return if (pdc.has(maxDurabilityKey, PersistentDataType.INTEGER)) {
            pdc.get(maxDurabilityKey, PersistentDataType.INTEGER)
        } else {
            null
        }
    }

    /**
     * Get the effective max durability (custom or vanilla)
     */
    fun getEffectiveMaxDurability(item: ItemStack): Int {
        return getCustomMaxDurability(item) ?: getVanillaMaxDurability(item)
    }

    /**
     * Get the original (vanilla) max durability of an item
     */
    fun getVanillaMaxDurability(item: ItemStack): Int {
        return item.type.maxDurability.toInt()
    }

    /**
     * Get current durability (remaining uses)
     */
    fun getCurrentDurability(item: ItemStack): Int {
        val meta = item.itemMeta as? Damageable ?: return 0
        val vanillaMax = getVanillaMaxDurability(item)
        return vanillaMax - meta.damage
    }

    /**
     * Set custom max durability on an item
     */
    fun setCustomMaxDurability(item: ItemStack, maxDurability: Int) {
        val meta = item.itemMeta ?: return
        val pdc = meta.persistentDataContainer

        // Store original max if not already stored
        if (!pdc.has(originalMaxDurabilityKey, PersistentDataType.INTEGER)) {
            pdc.set(originalMaxDurabilityKey, PersistentDataType.INTEGER, getVanillaMaxDurability(item))
        }

        pdc.set(maxDurabilityKey, PersistentDataType.INTEGER, maxDurability.coerceAtLeast(1))
        item.itemMeta = meta

        // Update lore to show durability
        updateDurabilityLore(item)
    }

    /**
     * Calculate max durability reduction based on Blacksmithy skill
     * GM (100) = 1 reduction, Skill 0 = 11 reduction
     */
    fun calculateDurabilityReduction(blacksmithySkill: Double): Int {
        return ((110 - blacksmithySkill) / 10).toInt().coerceAtLeast(1)
    }

    /**
     * Process a repair - reduces max durability and returns the new max
     */
    fun processRepair(item: ItemStack, blacksmithySkill: Double): RepairResult {
        if (!isRepairable(item)) {
            return RepairResult(false, 0, 0, 0)
        }

        val currentMax = getEffectiveMaxDurability(item)
        val reduction = calculateDurabilityReduction(blacksmithySkill)
        val newMax = (currentMax - reduction).coerceAtLeast(1)

        setCustomMaxDurability(item, newMax)

        // Cap current durability to new max
        capDurabilityToMax(item)

        return RepairResult(
            success = true,
            oldMax = currentMax,
            newMax = newMax,
            reduction = reduction
        )
    }

    /**
     * Cap the current durability to the custom max
     */
    fun capDurabilityToMax(item: ItemStack) {
        val meta = item.itemMeta as? Damageable ?: return
        val vanillaMax = getVanillaMaxDurability(item)
        val customMax = getEffectiveMaxDurability(item)
        val currentDurability = vanillaMax - meta.damage

        if (currentDurability > customMax) {
            // Set damage so that remaining durability equals customMax
            meta.damage = vanillaMax - customMax
            item.itemMeta = meta
            updateDurabilityLore(item)
        }
    }

    /**
     * Check if an item is repairable (has durability)
     */
    fun isRepairable(item: ItemStack): Boolean {
        if (item.type == Material.AIR) return false
        if (item.type.maxDurability <= 0) return false
        return item.itemMeta is Damageable
    }

    /**
     * Check if item is broken (max durability too low to use)
     */
    fun isBroken(item: ItemStack): Boolean {
        val customMax = getCustomMaxDurability(item) ?: return false
        return customMax <= 0
    }

    /**
     * Update item lore to show durability information
     * Uses Adventure Components to avoid legacy formatting codes
     */
    fun updateDurabilityLore(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val customMax = getCustomMaxDurability(item) ?: return
        val vanillaMax = getVanillaMaxDurability(item)
        val current = getCurrentDurability(item).coerceAtMost(customMax)

        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        // Remove old durability line if exists
        lore.removeIf { component ->
            val plain = PlainTextComponentSerializer.plainText().serialize(component)
            plain.contains("耐久度:") || plain.contains("Durability:")
        }

        // Calculate percentage for color
        val percentage = (customMax.toDouble() / vanillaMax) * 100
        val percentColor = when {
            percentage > 75 -> NamedTextColor.GREEN
            percentage > 50 -> NamedTextColor.YELLOW
            percentage > 25 -> NamedTextColor.GOLD
            else -> NamedTextColor.RED
        }

        // Build durability line using Components
        val durabilityLine = Component.text("耐久度: $current/$customMax ")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(
                Component.text("(${percentage.toInt()}%)")
                    .color(percentColor)
            )
        lore.add(0, durabilityLine)

        meta.lore(lore)
        item.itemMeta = meta
    }

    /**
     * Get repair difficulty based on item material
     */
    fun getRepairDifficulty(item: ItemStack): Int {
        val name = item.type.name
        return when {
            name.contains("NETHERITE") -> 80
            name.contains("DIAMOND") -> 60
            name.contains("IRON") || name.contains("CHAINMAIL") -> 40
            name.contains("GOLDEN") || name.contains("GOLD") -> 35
            name.contains("STONE") -> 20
            name.contains("LEATHER") || name.contains("WOODEN") || name.contains("WOOD") -> 10
            else -> 30
        }
    }

    data class RepairResult(
        val success: Boolean,
        val oldMax: Int,
        val newMax: Int,
        val reduction: Int
    )
}
