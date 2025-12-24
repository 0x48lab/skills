package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ReagentManager(private val plugin: Skills) {

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
     * Get count of a specific material in player's inventory
     */
    fun getMaterialCount(player: Player, material: Material): Int {
        return player.inventory.all(material).values.sumOf { it.amount }
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
     * Remove a specific amount of material from player's inventory
     */
    private fun removeMaterial(player: Player, material: Material, amount: Int): Boolean {
        val inventory = player.inventory
        var remaining = amount

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type != material) continue

            if (item.amount > remaining) {
                item.amount -= remaining
                return true
            } else {
                remaining -= item.amount
                inventory.setItem(i, null)
                if (remaining <= 0) return true
            }
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
