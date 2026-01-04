package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.roundToInt

/**
 * Manages food bonus from Cooking skill
 *
 * Food items can have a bonus that affects:
 * - Healing amount (HP recovery when eaten)
 * - Saturation bonus
 *
 * The bonus is stored in PersistentDataContainer and applied when eaten
 */
class FoodBonusManager(private val plugin: Skills) {

    private val bonusKey = NamespacedKey(plugin, "cooking_bonus")
    private val cookerKey = NamespacedKey(plugin, "cooker")
    private val qualityKey = NamespacedKey(plugin, "food_quality")

    /**
     * Apply cooking bonus to a food item
     *
     * @param item The food item
     * @param cookingSkill The player's Cooking skill value
     * @param difficulty The food's cooking difficulty
     * @param cookerName The name of the player who cooked it
     * @return The modified item with bonus applied
     */
    fun applyFoodBonus(
        item: ItemStack,
        cookingSkill: Double,
        difficulty: Int,
        cookerName: String
    ): ItemStack {
        // Calculate bonus based on skill and difficulty
        val quality = QualityType.calculateQuality(cookingSkill, difficulty)
        val bonusPercent = calculateBonusPercent(cookingSkill, quality)

        val meta = item.itemMeta ?: return item

        // Store bonus in persistent data (rounded to 1% precision to match Lore display)
        // This ensures items with same displayed percentage will stack
        val roundedBonus = (bonusPercent * 100).roundToInt() / 100.0
        meta.persistentDataContainer.set(bonusKey, PersistentDataType.DOUBLE, roundedBonus)
        meta.persistentDataContainer.set(cookerKey, PersistentDataType.STRING, cookerName)
        meta.persistentDataContainer.set(qualityKey, PersistentDataType.STRING, quality.name)

        // Update lore to show bonus
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        // Remove existing cooking lore if present
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
            plain.contains("Recovery:") || plain.contains("Cooked by:")
        }

        // Add quality indicator
        if (quality != QualityType.NORMAL_QUALITY) {
            val qualityText = Component.text("[${quality.shortName}] ")
                .color(quality.color)
                .decoration(TextDecoration.ITALIC, false)

            // Update display name with quality prefix if not already present
            val currentName = meta.displayName() ?: Component.translatable(item.type.translationKey())
            val plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(currentName)
            if (!plainName.startsWith("[")) {
                meta.displayName(qualityText.append(currentName))
            }
        }

        // Add bonus lore (use roundedBonus to match stored value)
        val bonusDisplay = if (roundedBonus >= 0) "+${(roundedBonus * 100).toInt()}%" else "${(roundedBonus * 100).toInt()}%"
        val bonusColor = when {
            bonusPercent > 0.1 -> NamedTextColor.GREEN
            bonusPercent > 0 -> NamedTextColor.DARK_GREEN
            bonusPercent < 0 -> NamedTextColor.RED
            else -> NamedTextColor.GRAY
        }

        lore.add(Component.text("Recovery: $bonusDisplay")
            .color(bonusColor)
            .decoration(TextDecoration.ITALIC, false))

        // Add cooker name for exceptional quality
        if (quality == QualityType.EXCEPTIONAL) {
            lore.add(Component.text("Cooked by: $cookerName")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false))
        }

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    /**
     * Calculate bonus percent based on skill and quality
     *
     * @param cookingSkill The player's Cooking skill (0-100)
     * @param quality The determined quality
     * @return Bonus multiplier (-0.15 to +0.50)
     */
    private fun calculateBonusPercent(cookingSkill: Double, quality: QualityType): Double {
        // Base bonus from skill: 0 to +25% (skill/4)
        val skillBonus = cookingSkill / 400.0  // 0 to 0.25

        // Quality modifier
        val qualityBonus = when (quality) {
            QualityType.LOW_QUALITY -> -0.15
            QualityType.NORMAL_QUALITY -> 0.0
            QualityType.HIGH_QUALITY -> 0.15
            QualityType.EXCEPTIONAL -> 0.25
        }

        return skillBonus + qualityBonus
    }

    /**
     * Get the cooking bonus from a food item
     *
     * @param item The food item
     * @return Bonus multiplier (0 if no bonus)
     */
    fun getFoodBonus(item: ItemStack?): Double {
        if (item == null) return 0.0
        return item.itemMeta?.persistentDataContainer
            ?.get(bonusKey, PersistentDataType.DOUBLE) ?: 0.0
    }

    /**
     * Check if food item has cooking bonus
     */
    fun hasFoodBonus(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.itemMeta?.persistentDataContainer?.has(bonusKey, PersistentDataType.DOUBLE) == true
    }

    /**
     * Normalize the cooking bonus value to fix floating point precision issues.
     * This allows old items (before the fix) to stack with new items.
     * Also updates the Lore to match the normalized value.
     *
     * @param item The food item to normalize
     * @return true if the item was modified, false otherwise
     */
    fun normalizeBonus(item: ItemStack): Boolean {
        if (!hasFoodBonus(item)) return false

        val meta = item.itemMeta ?: return false
        val currentBonus = meta.persistentDataContainer.get(bonusKey, PersistentDataType.DOUBLE) ?: return false

        // Round to 1% precision to match Lore display
        val roundedBonus = (currentBonus * 100).roundToInt() / 100.0

        // Calculate expected Lore display
        val expectedDisplay = if (roundedBonus >= 0) "+${(roundedBonus * 100).toInt()}%" else "${(roundedBonus * 100).toInt()}%"

        // Check if Lore needs updating
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        val recoveryIndex = lore.indexOfFirst { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
            plain.contains("Recovery:")
        }

        val currentLoreText = if (recoveryIndex >= 0) {
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore[recoveryIndex])
        } else {
            ""
        }

        val needsUpdate = currentBonus != roundedBonus || !currentLoreText.contains(expectedDisplay)

        if (needsUpdate) {
            // Update PDC value
            meta.persistentDataContainer.set(bonusKey, PersistentDataType.DOUBLE, roundedBonus)

            // Update Lore
            val bonusColor = when {
                roundedBonus > 0.1 -> NamedTextColor.GREEN
                roundedBonus > 0 -> NamedTextColor.DARK_GREEN
                roundedBonus < 0 -> NamedTextColor.RED
                else -> NamedTextColor.GRAY
            }

            val newRecoveryLine = Component.text("Recovery: $expectedDisplay")
                .color(bonusColor)
                .decoration(TextDecoration.ITALIC, false)

            if (recoveryIndex >= 0) {
                lore[recoveryIndex] = newRecoveryLine
            } else {
                lore.add(newRecoveryLine)
            }

            meta.lore(lore)
            item.itemMeta = meta
            return true
        }
        return false
    }

    /**
     * Get the cooker name from a food item
     */
    fun getCookerName(item: ItemStack?): String? {
        if (item == null) return null
        return item.itemMeta?.persistentDataContainer
            ?.get(cookerKey, PersistentDataType.STRING)
    }

    /**
     * Get the quality from a food item
     */
    fun getFoodQuality(item: ItemStack?): QualityType {
        if (item == null) return QualityType.NORMAL_QUALITY
        val qualityName = item.itemMeta?.persistentDataContainer
            ?.get(qualityKey, PersistentDataType.STRING)
        return qualityName?.let {
            try { QualityType.valueOf(it) } catch (e: Exception) { QualityType.NORMAL_QUALITY }
        } ?: QualityType.NORMAL_QUALITY
    }

    /**
     * Apply food bonus when player eats
     *
     * @param player The player eating
     * @param item The food item being eaten
     * @param baseHealing The base food points being restored
     * @return Additional healing to apply
     */
    fun calculateBonusHealing(player: Player, item: ItemStack, baseHealing: Int): Int {
        val bonus = getFoodBonus(item)
        if (bonus == 0.0) return 0

        return (baseHealing * bonus).toInt()
    }

    /**
     * Apply food bonus when player eats (for saturation)
     *
     * @param player The player eating
     * @param item The food item being eaten
     * @param baseSaturation The base saturation being restored
     * @return Additional saturation to apply
     */
    fun calculateBonusSaturation(player: Player, item: ItemStack, baseSaturation: Float): Float {
        val bonus = getFoodBonus(item)
        if (bonus == 0.0) return 0f

        return (baseSaturation * bonus).toFloat()
    }
}
