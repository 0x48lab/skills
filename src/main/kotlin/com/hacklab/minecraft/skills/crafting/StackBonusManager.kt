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
 * - Crafting (equipment, tools)
 * - Cooking (food, potions)
 * - Inscription (scrolls)
 *
 * Formula: stackSize = 64 + (skillSum / 300 * 35)
 * - Skill sum 0: 64 stack
 * - Skill sum 150: 82 stack
 * - Skill sum 300: 99 stack
 */
class StackBonusManager(private val plugin: Skills) {

    companion object {
        const val BASE_STACK_SIZE = 64
        const val MAX_STACK_SIZE = 99
        const val BONUS_STACK_SIZE = MAX_STACK_SIZE - BASE_STACK_SIZE // 35
        const val MAX_SKILL_SUM = 300.0 // 3 skills * 100 each

        /** Hotbar + main storage slots (armor and off-hand come after these) */
        const val STORAGE_SLOT_COUNT = 36

        // Skills that contribute to stack size bonus
        val CONTRIBUTING_SKILLS = listOf(
            SkillType.CRAFTING,
            SkillType.COOKING,
            SkillType.INSCRIPTION
        )

        /**
         * Items that should NOT have their ItemMeta modified.
         * These items have special validation that breaks when meta is changed.
         *
         * Note: Items with maxStackSize=1 (potions, most tools) are already excluded
         * by the stackability check, so only stackable items need to be listed here.
         *
         * CONFIRMED ISSUES:
         * - Trial Keys: Vault blocks validate keys and reject modified ones
         *
         * Other items (banners, maps, enchanted books, fireworks) preserve their
         * data correctly when only maxStackSize is modified, because ItemMeta
         * cloning preserves all fields.
         */
        val EXCLUDED_MATERIALS: Set<Material> = setOf(
            // Trial Chamber items - Vault blocks validate keys (CONFIRMED BROKEN)
            Material.TRIAL_KEY,
            Material.OMINOUS_TRIAL_KEY
        )

        /**
         * Check if an item type should be excluded from stack bonus processing.
         */
        fun isExcludedMaterial(material: Material): Boolean {
            return material in EXCLUDED_MATERIALS
        }
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

        // Skip items with special NBT data that breaks when meta is modified
        if (isExcludedMaterial(item.type)) {
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
            if (item != null && item.type.maxStackSize > 1 && !isExcludedMaterial(item.type)) {
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

        // Skip items with special NBT data that breaks when meta is modified
        // (Trial Keys, potions, maps, written books, etc.)
        if (isExcludedMaterial(item.type)) {
            return item
        }

        // Normalize food bonus values to fix floating point precision issues
        // This allows old items to stack with new items
        if (item.type.isEdible) {
            plugin.craftingManager.foodBonusManager.normalizeBonus(item)
            normalizeFoodBonusInInventory(player.inventory, item.type)
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
     * Bring a player's whole inventory in line with their current stack bonus.
     *
     * Runs in two steps per material type:
     * 1. Unify MaxStackSize, so items of the same type are stackable with each other.
     * 2. Optionally merge stacks that sit below the raised limit.
     *
     * Step 2 is what repairs shift-click results: the vanilla quick-move happens
     * after the click event, so a stack can still end up split at the old limit.
     * Raising MaxStackSize afterwards does not re-merge those stacks by itself,
     * which looks to the player like the bonus "did not apply".
     *
     * @param player The player whose inventory is normalized
     * @param consolidate Whether to merge stacks left below the raised limit
     */
    fun normalizeInventory(player: Player, consolidate: Boolean) {
        if (!player.isOnline) return

        val inventory = player.inventory
        val calculatedMax = calculateMaxStackSize(player)

        // Group slots by material type
        val slotsByType = mutableMapOf<Material, MutableList<Int>>()
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type.maxStackSize <= 1) continue
            if (isExcludedMaterial(item.type)) continue
            slotsByType.getOrPut(item.type) { mutableListOf() }.add(i)
        }

        var changed = false

        for ((type, slots) in slotsByType) {
            // Never lower an item that already carries a higher limit
            var targetMax = calculatedMax
            for (slot in slots) {
                val item = inventory.getItem(slot) ?: continue
                targetMax = maxOf(targetMax, getMaxStackSize(item))
            }
            if (targetMax <= BASE_STACK_SIZE) continue

            // Food bonus values must match exactly for items to be stackable
            if (type.isEdible) {
                normalizeFoodBonusInInventory(inventory, type)
            }

            for (slot in slots) {
                val item = inventory.getItem(slot) ?: continue
                if (getMaxStackSize(item) != targetMax) {
                    setMaxStackSize(item, targetMax)
                    inventory.setItem(slot, item)
                    changed = true
                }
            }

            if (consolidate && consolidateStacks(inventory, slots, targetMax)) {
                changed = true
            }
        }

        // Keep the client in sync - the inventory may be open while this runs
        if (changed) {
            player.updateInventory()
        }
    }

    /**
     * Merge stacks of the same item that are below the target stack size.
     * Only storage and hotbar slots are touched - armor and off-hand slots are
     * left alone so equipped items are never moved.
     */
    private fun consolidateStacks(inventory: PlayerInventory, slots: List<Int>, targetMax: Int): Boolean {
        val storageSlots = slots.filter { it < STORAGE_SLOT_COUNT }
        var merged = false

        for (i in storageSlots.indices) {
            val destSlot = storageSlots[i]
            var dest = inventory.getItem(destSlot) ?: continue
            if (dest.amount >= targetMax) continue

            for (j in i + 1 until storageSlots.size) {
                val srcSlot = storageSlots[j]
                val src = inventory.getItem(srcSlot) ?: continue

                // isSimilar compares everything except amount, so differing
                // quality / crafter / food bonus items are never merged
                if (!dest.isSimilar(src)) continue

                val space = targetMax - dest.amount
                if (space <= 0) break

                val moved = minOf(space, src.amount)
                dest.amount += moved
                src.amount -= moved

                inventory.setItem(destSlot, dest)
                inventory.setItem(srcSlot, if (src.amount <= 0) null else src)
                merged = true

                dest = inventory.getItem(destSlot) ?: break
                if (dest.amount >= targetMax) break
            }
        }

        return merged
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

    /**
     * Sync two items' MaxStackSize so they can be stacked together.
     * Sets both items to the higher MaxStackSize of the two (or calculated max if higher).
     *
     * @param item1 First item
     * @param item2 Second item
     * @param player The player whose skills determine the stack size
     */
    fun syncItemsForStacking(item1: ItemStack, item2: ItemStack, player: Player) {
        if (item1.type != item2.type) return
        if (item1.type.maxStackSize <= 1) return
        if (isExcludedMaterial(item1.type)) return

        val calculatedMax = calculateMaxStackSize(player)
        val max1 = getMaxStackSize(item1)
        val max2 = getMaxStackSize(item2)
        val targetMax = maxOf(calculatedMax, max1, max2)

        if (targetMax > BASE_STACK_SIZE) {
            if (max1 != targetMax) {
                setMaxStackSize(item1, targetMax)
            }
            if (max2 != targetMax) {
                setMaxStackSize(item2, targetMax)
            }
        }
    }
}
