package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.BundleMeta

class ReagentManager(private val plugin: Skills) {

    companion object {
        /**
         * Set of all shulker box materials (all 17 colors including uncolored)
         */
        private val SHULKER_BOX_MATERIALS: Set<Material> = Material.entries
            .filter { it.name.endsWith("SHULKER_BOX") }
            .toSet()
    }

    /**
     * Check if the material is a shulker box
     */
    fun isShulkerBox(material: Material): Boolean = material in SHULKER_BOX_MATERIALS

    /**
     * Check if the material is a bundle
     */
    fun isBundle(material: Material): Boolean = material == Material.BUNDLE

    /**
     * Get the inventory contents of a shulker box item
     * @return the shulker box inventory, or null if not a valid shulker box
     */
    fun getShulkerBoxContents(item: ItemStack): Inventory? {
        if (!isShulkerBox(item.type)) return null
        val meta = item.itemMeta as? BlockStateMeta ?: return null
        val shulkerBox = meta.blockState as? ShulkerBox ?: return null
        return shulkerBox.inventory
    }

    /**
     * Get the contents of a bundle item
     * @return the list of items in the bundle, or null if not a valid bundle
     */
    fun getBundleContents(item: ItemStack): List<ItemStack>? {
        if (!isBundle(item.type)) return null
        val meta = item.itemMeta as? BundleMeta ?: return null
        return meta.items
    }

    /**
     * Check if player has all required reagents for a spell
     * Handles multiple of the same material (e.g., GOLDEN_CARROT x2)
     */
    fun hasReagents(player: Player, spell: SpellType): Boolean {
        val required = getRequiredCounts(spell)

        for ((material, count) in required) {
            if (getMaterialCount(player, material) < count) {
                return false
            }
        }
        return true
    }

    /**
     * Check if player has a specific material
     */
    fun hasMaterial(player: Player, material: Material): Boolean {
        return player.inventory.contains(material)
    }

    /**
     * Get count of a specific material in player's direct inventory slots
     */
    private fun getInventoryMaterialCount(player: Player, material: Material): Int {
        return player.inventory.all(material).values.sumOf { it.amount }
    }

    /**
     * Get count of a specific material inside shulker boxes in player's inventory
     */
    private fun getShulkerBoxMaterialCount(player: Player, material: Material): Int {
        var count = 0
        val inventory = player.inventory

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (!isShulkerBox(item.type)) continue

            val shulkerInventory = getShulkerBoxContents(item) ?: continue
            for (shulkerItem in shulkerInventory.contents) {
                if (shulkerItem != null && shulkerItem.type == material) {
                    count += shulkerItem.amount
                }
            }
        }

        return count
    }

    /**
     * Get count of a specific material inside bundles in player's inventory
     */
    private fun getBundleMaterialCount(player: Player, material: Material): Int {
        var count = 0
        val inventory = player.inventory

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (!isBundle(item.type)) continue

            val bundleContents = getBundleContents(item) ?: continue
            for (bundleItem in bundleContents) {
                if (bundleItem.type == material) {
                    count += bundleItem.amount
                }
            }
        }

        return count
    }

    /**
     * Get count of a specific material in player's inventory (including shulker boxes and bundles)
     * Search order: inventory direct slots -> shulker boxes -> bundles
     */
    fun getMaterialCount(player: Player, material: Material): Int {
        var count = getInventoryMaterialCount(player, material)

        // Search shulker boxes if enabled
        if (plugin.skillsConfig.reagentSearchShulkerBoxes) {
            count += getShulkerBoxMaterialCount(player, material)
        }

        // Search bundles if enabled
        if (plugin.skillsConfig.reagentSearchBundles) {
            count += getBundleMaterialCount(player, material)
        }

        return count
    }

    /**
     * Consume reagents for a spell
     * @return true if successful
     */
    fun consumeReagents(player: Player, spell: SpellType): Boolean {
        // First verify all reagents are available
        if (!hasReagents(player, spell)) {
            return false
        }

        // Consume each material (handling multiples)
        val required = getRequiredCounts(spell)
        for ((material, count) in required) {
            removeMaterial(player, material, count)
        }

        return true
    }

    /**
     * Get required material counts for a spell
     * Groups materials and counts how many of each is needed
     */
    private fun getRequiredCounts(spell: SpellType): Map<Material, Int> {
        return spell.reagents.groupingBy { it }.eachCount()
    }

    /**
     * Remove a specific amount of material from player's direct inventory slots
     * @return the number of items actually removed
     */
    private fun removeFromInventory(player: Player, material: Material, amount: Int): Int {
        val inventory = player.inventory
        var remaining = amount
        var removed = 0

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type != material) continue
            // Skip shulker boxes and bundles (they are containers, not direct items)
            if (isShulkerBox(item.type) || isBundle(item.type)) continue

            if (item.amount > remaining) {
                item.amount -= remaining
                removed += remaining
                return removed
            } else {
                remaining -= item.amount
                removed += item.amount
                inventory.setItem(i, null)
                if (remaining <= 0) return removed
            }
        }

        return removed
    }

    /**
     * Remove a specific amount of material from shulker boxes in player's inventory
     * @return the number of items actually removed
     */
    private fun removeFromShulkerBox(player: Player, material: Material, amount: Int): Int {
        val inventory = player.inventory
        var remaining = amount
        var removed = 0

        for (i in 0 until inventory.size) {
            val containerItem = inventory.getItem(i) ?: continue
            if (!isShulkerBox(containerItem.type)) continue

            val meta = containerItem.itemMeta as? BlockStateMeta ?: continue
            val shulkerBox = meta.blockState as? ShulkerBox ?: continue
            val shulkerInventory = shulkerBox.inventory
            var modified = false

            for (j in 0 until shulkerInventory.size) {
                val item = shulkerInventory.getItem(j) ?: continue
                if (item.type != material) continue

                if (item.amount > remaining) {
                    item.amount -= remaining
                    removed += remaining
                    remaining = 0
                    modified = true
                    break
                } else {
                    remaining -= item.amount
                    removed += item.amount
                    shulkerInventory.setItem(j, null)
                    modified = true
                    if (remaining <= 0) break
                }
            }

            // Update the shulker box in the inventory if modified
            if (modified) {
                meta.blockState = shulkerBox
                containerItem.itemMeta = meta
            }

            if (remaining <= 0) break
        }

        return removed
    }

    /**
     * Remove a specific amount of material from bundles in player's inventory
     * @return the number of items actually removed
     */
    private fun removeFromBundle(player: Player, material: Material, amount: Int): Int {
        val inventory = player.inventory
        var remaining = amount
        var removed = 0

        for (i in 0 until inventory.size) {
            val containerItem = inventory.getItem(i) ?: continue
            if (!isBundle(containerItem.type)) continue

            val meta = containerItem.itemMeta as? BundleMeta ?: continue
            val bundleContents = meta.items.toMutableList()
            var modified = false

            for (j in bundleContents.indices) {
                val item = bundleContents[j]
                if (item.type != material) continue

                if (item.amount > remaining) {
                    val newItem = item.clone()
                    newItem.amount = item.amount - remaining
                    bundleContents[j] = newItem
                    removed += remaining
                    remaining = 0
                    modified = true
                    break
                } else {
                    remaining -= item.amount
                    removed += item.amount
                    bundleContents[j] = ItemStack(Material.AIR)
                    modified = true
                    if (remaining <= 0) break
                }
            }

            // Update the bundle in the inventory if modified
            if (modified) {
                // Filter out AIR items
                val filteredContents = bundleContents.filter { it.type != Material.AIR }
                meta.setItems(filteredContents)
                containerItem.itemMeta = meta
            }

            if (remaining <= 0) break
        }

        return removed
    }

    /**
     * Remove a specific amount of material from player's inventory (including containers)
     * Consumption order: inventory direct slots -> shulker boxes -> bundles
     */
    private fun removeMaterial(player: Player, material: Material, amount: Int): Boolean {
        var remaining = amount

        // First, remove from direct inventory slots
        val removedFromInventory = removeFromInventory(player, material, remaining)
        remaining -= removedFromInventory

        // If more needed, remove from shulker boxes (if enabled)
        if (remaining > 0 && plugin.skillsConfig.reagentSearchShulkerBoxes) {
            val removedFromShulker = removeFromShulkerBox(player, material, remaining)
            remaining -= removedFromShulker
        }

        // If more needed, remove from bundles (if enabled)
        if (remaining > 0 && plugin.skillsConfig.reagentSearchBundles) {
            val removedFromBundle = removeFromBundle(player, material, remaining)
            remaining -= removedFromBundle
        }

        return remaining <= 0
    }

    /**
     * Get a formatted list of missing reagents
     */
    fun getMissingReagents(player: Player, spell: SpellType): List<Material> {
        val required = getRequiredCounts(spell)
        return required.filter { (material, count) ->
            getMaterialCount(player, material) < count
        }.keys.toList()
    }

    /**
     * Add materials to player (for admin/testing)
     */
    fun giveMaterial(player: Player, material: Material, amount: Int) {
        player.inventory.addItem(ItemStack(material, amount))
    }

    /**
     * Give a full set of reagents for a spell
     */
    fun giveReagentsForSpell(player: Player, spell: SpellType, quantity: Int = 1) {
        val required = getRequiredCounts(spell)
        for ((material, count) in required) {
            giveMaterial(player, material, count * quantity)
        }
    }
}
