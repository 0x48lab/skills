package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.ItemMeta

/**
 * Manages stack size bonus based on production skills.
 *
 * Stack size increases from 64 to 99 based on the sum of:
 * - Craftsmanship
 * - Blacksmithy
 * - Cooking
 * - Alchemy
 *
 * Formula: stackSize = 64 + (skillSum / 400 * 35)
 * - Skill sum 0: 64 stack
 * - Skill sum 200: 82 stack
 * - Skill sum 400: 99 stack
 */
class StackBonusManager(private val plugin: Skills) {

    companion object {
        const val BASE_STACK_SIZE = 64
        const val MAX_STACK_SIZE = 99
        const val BONUS_STACK_SIZE = MAX_STACK_SIZE - BASE_STACK_SIZE // 35
        const val MAX_SKILL_SUM = 400.0 // 4 skills * 100 each

        // Skills that contribute to stack size bonus
        val CONTRIBUTING_SKILLS = listOf(
            SkillType.CRAFTSMANSHIP,
            SkillType.BLACKSMITHY,
            SkillType.COOKING,
            SkillType.ALCHEMY
        )
    }

    /**
     * Calculate the stack size bonus for a player based on their production skills.
     *
     * @param player The player to calculate for
     * @return The maximum stack size (64-99)
     */
    fun calculateMaxStackSize(player: Player): Int {
        val data = plugin.playerDataManager.getPlayerData(player)

        val skillSum = CONTRIBUTING_SKILLS.sumOf { skill ->
            data.getSkillValue(skill)
        }

        // Formula: 64 + (skillSum / 400 * 35)
        val bonus = (skillSum / MAX_SKILL_SUM * BONUS_STACK_SIZE).toInt()
        return BASE_STACK_SIZE + bonus
    }

    /**
     * Get the sum of contributing skills for a player.
     *
     * @param player The player
     * @return Sum of Craftsmanship + Blacksmithy + Cooking + Alchemy
     */
    fun getSkillSum(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return CONTRIBUTING_SKILLS.sumOf { skill ->
            data.getSkillValue(skill)
        }
    }

    /**
     * Apply stack size bonus to an item based on player's skills.
     *
     * @param item The item to modify
     * @param player The player whose skills determine the stack size
     * @return The modified item (same instance)
     */
    fun applyStackBonus(item: ItemStack, player: Player): ItemStack {
        // Only apply to stackable items (original max > 1)
        if (item.type.maxStackSize <= 1) {
            return item
        }

        val maxStackSize = calculateMaxStackSize(player)

        // Only modify if bonus applies (> 64)
        if (maxStackSize > BASE_STACK_SIZE) {
            val meta = item.itemMeta
            if (meta != null) {
                meta.setMaxStackSize(maxStackSize)
                item.itemMeta = meta
            }
        }

        return item
    }

    /**
     * Apply stack size bonus to multiple items.
     *
     * @param items The items to modify
     * @param player The player whose skills determine the stack size
     */
    fun applyStackBonusToAll(items: Array<ItemStack?>, player: Player) {
        val maxStackSize = calculateMaxStackSize(player)

        if (maxStackSize <= BASE_STACK_SIZE) {
            return // No bonus to apply
        }

        for (item in items) {
            if (item != null && item.type.maxStackSize > 1) {
                val meta = item.itemMeta
                if (meta != null) {
                    meta.setMaxStackSize(maxStackSize)
                    item.itemMeta = meta
                }
            }
        }
    }

    /**
     * Check if an item has a custom stack size set.
     *
     * @param item The item to check
     * @return true if item has custom max stack size
     */
    fun hasCustomStackSize(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.hasMaxStackSize()
    }

    /**
     * Get the current max stack size of an item.
     *
     * @param item The item
     * @return The max stack size (custom if set, otherwise default)
     */
    fun getMaxStackSize(item: ItemStack): Int {
        val meta = item.itemMeta
        return if (meta != null && meta.hasMaxStackSize()) {
            meta.maxStackSize
        } else {
            item.type.maxStackSize
        }
    }

    /**
     * Apply stack bonus with inventory synchronization.
     * Finds existing items of the same type and uses the higher MaxStackSize.
     * This ensures items can stack properly by maintaining consistent MaxStackSize.
     *
     * @param item The item to modify
     * @param player The player whose skills determine the stack size
     * @return The modified item (same instance)
     */
    fun applyStackBonusWithSync(item: ItemStack, player: Player): ItemStack {
        // Only apply to stackable items (original max > 1)
        if (item.type.maxStackSize <= 1) {
            return item
        }

        // Normalize food bonus values to fix floating point precision issues
        // This allows old items to stack with new items
        if (item.type.isEdible) {
            plugin.craftingManager.foodBonusManager.normalizeBonus(item)
            normalizeFoodBonusInInventory(player.inventory, item.type)

            // Debug: Log item comparison
            debugItemComparison(player, item)
        }

        val calculatedMax = calculateMaxStackSize(player)
        val existingMax = findHighestMaxStackSize(player.inventory, item.type)
        val targetMax = maxOf(calculatedMax, existingMax, BASE_STACK_SIZE)

        if (targetMax > BASE_STACK_SIZE) {
            // Update picked up item
            setMaxStackSize(item, targetMax)

            // Update existing items in inventory to match
            updateInventoryItems(player.inventory, item.type, targetMax)
        }

        return item
    }

    /**
     * Normalize food bonus values for all items of a given type in the inventory.
     * This fixes floating point precision issues that prevent stacking.
     */
    private fun normalizeFoodBonusInInventory(inventory: PlayerInventory, type: Material) {
        for (i in 0 until inventory.size) {
            val invItem = inventory.getItem(i) ?: continue
            if (invItem.type == type) {
                if (plugin.craftingManager.foodBonusManager.normalizeBonus(invItem)) {
                    inventory.setItem(i, invItem)
                }
            }
        }
    }

    /**
     * Debug: Compare picked up item with inventory items to find differences
     */
    private fun debugItemComparison(player: Player, pickedItem: ItemStack) {
        val logger = plugin.logger
        val bonusKey = org.bukkit.NamespacedKey(plugin, "cooking_bonus")
        val cookerKey = org.bukkit.NamespacedKey(plugin, "cooker")

        logger.info("=== DEBUG: Item Comparison for ${player.name} ===")
        logger.info("Picked item: ${pickedItem.type}")

        val pickedMeta = pickedItem.itemMeta
        if (pickedMeta != null) {
            logger.info("  MaxStackSize: ${if (pickedMeta.hasMaxStackSize()) pickedMeta.maxStackSize else "not set"}")
            val pdc = pickedMeta.persistentDataContainer
            val cookingBonus = pdc.get(bonusKey, org.bukkit.persistence.PersistentDataType.DOUBLE)
            val cooker = pdc.get(cookerKey, org.bukkit.persistence.PersistentDataType.STRING)
            logger.info("  cooking_bonus: $cookingBonus")
            logger.info("  cooker: $cooker")
        }

        // Compare with first matching item in inventory
        for (i in 0 until player.inventory.size) {
            val invItem = player.inventory.getItem(i) ?: continue
            if (invItem.type == pickedItem.type && invItem !== pickedItem) {
                logger.info("--- Comparing with inventory slot $i (amount: ${invItem.amount}) ---")
                val invMeta = invItem.itemMeta
                if (invMeta != null) {
                    logger.info("  MaxStackSize: ${if (invMeta.hasMaxStackSize()) invMeta.maxStackSize else "not set"}")
                    val pdc = invMeta.persistentDataContainer
                    val cookingBonus = pdc.get(bonusKey, org.bukkit.persistence.PersistentDataType.DOUBLE)
                    val cooker = pdc.get(cookerKey, org.bukkit.persistence.PersistentDataType.STRING)
                    logger.info("  cooking_bonus: $cookingBonus")
                    logger.info("  cooker: $cooker")
                }
                logger.info("  isSimilar: ${pickedItem.isSimilar(invItem)}")
            }
        }
        logger.info("=== END DEBUG ===")
    }

    /**
     * Find the highest MaxStackSize among items of a given type in the inventory.
     *
     * @param inventory The player's inventory
     * @param type The material type to search for
     * @return The highest MaxStackSize found, or BASE_STACK_SIZE if none found
     */
    private fun findHighestMaxStackSize(inventory: PlayerInventory, type: Material): Int {
        var highest = BASE_STACK_SIZE
        for (i in 0 until inventory.size) {
            val invItem = inventory.getItem(i) ?: continue
            if (invItem.type == type) {
                highest = maxOf(highest, getMaxStackSize(invItem))
            }
        }
        return highest
    }

    /**
     * Update all items of a given type in the inventory to have the target MaxStackSize.
     *
     * @param inventory The player's inventory
     * @param type The material type to update
     * @param targetMax The target MaxStackSize
     */
    private fun updateInventoryItems(inventory: PlayerInventory, type: Material, targetMax: Int) {
        for (i in 0 until inventory.size) {
            val invItem = inventory.getItem(i) ?: continue
            if (invItem.type == type && getMaxStackSize(invItem) < targetMax) {
                setMaxStackSize(invItem, targetMax)
                inventory.setItem(i, invItem)
            }
        }
    }

    /**
     * Set the MaxStackSize on an item.
     *
     * @param item The item to modify
     * @param size The MaxStackSize to set
     */
    private fun setMaxStackSize(item: ItemStack, size: Int) {
        val meta = item.itemMeta ?: return
        meta.setMaxStackSize(size)
        item.itemMeta = meta
    }
}
