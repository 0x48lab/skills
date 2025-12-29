package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemMendEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Listener for Mending enchantment integration with crafting skills.
 *
 * Mending success rate depends on the appropriate crafting skill for each item type:
 * - Metal items (iron/gold/diamond/netherite/chain/stone) → Blacksmithy
 * - Leather/wooden items, shields → Craftsmanship
 * - Bows, crossbows → Bowcraft
 * - Fishing rod, shears, flint and steel → Tinkering
 * - Elytra, turtle helmet → No skill required (100% success)
 *
 * Formula: min(100, skill * 100 / 60)
 * - Skill 0: 0% success rate
 * - Skill 30: 50% success rate
 * - Skill 60+: 100% success rate
 */
class MendingListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onItemMend(event: PlayerItemMendEvent) {
        val player = event.player
        val item = event.item

        // Get required skill for this item type
        val requiredSkill = getRequiredSkillForMending(item)

        // If no skill required (e.g., elytra), always succeed
        if (requiredSkill == null) {
            return
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val skillValue = data.getSkillValue(requiredSkill)

        // Calculate success rate: skill 60+ = 100%, skill 0 = 0%
        val successRate = calculateMendingSuccessRate(skillValue)

        // Roll for success
        val roll = Random.nextDouble() * 100

        if (roll >= successRate) {
            // Mending failed - cancel the event
            // XP will go to player normally instead of repairing
            event.isCancelled = true
        }
    }

    /**
     * Determine which crafting skill is required for mending the given item.
     *
     * @param item The item being mended
     * @return The required SkillType, or null if no skill is required (always 100% success)
     */
    private fun getRequiredSkillForMending(item: ItemStack): SkillType? {
        val typeName = item.type.name

        return when {
            // Metal weapons, armor, and tools → Blacksmithy
            typeName.contains("IRON_") ||
            typeName.contains("GOLDEN_") ||
            typeName.contains("DIAMOND_") ||
            typeName.contains("NETHERITE_") ||
            typeName.contains("CHAINMAIL_") ||
            typeName.contains("STONE_") ||
            item.type == Material.TRIDENT ||
            item.type == Material.MACE -> SkillType.BLACKSMITHY

            // Spears → Blacksmithy (metal-tipped)
            typeName.contains("_SPEAR") -> SkillType.BLACKSMITHY

            // Bows and crossbows → Bowcraft
            item.type == Material.BOW ||
            item.type == Material.CROSSBOW -> SkillType.BOWCRAFT

            // Leather armor, wooden items, shields → Craftsmanship
            typeName.contains("LEATHER_") ||
            typeName.contains("WOODEN_") ||
            item.type == Material.SHIELD -> SkillType.CRAFTSMANSHIP

            // Tinkering items
            item.type == Material.FISHING_ROD ||
            item.type == Material.SHEARS ||
            item.type == Material.FLINT_AND_STEEL ||
            item.type == Material.CARROT_ON_A_STICK ||
            item.type == Material.WARPED_FUNGUS_ON_A_STICK -> SkillType.TINKERING

            // Special items - no skill required (always 100%)
            item.type == Material.ELYTRA ||
            item.type == Material.TURTLE_HELMET -> null

            // Default: no skill required
            else -> null
        }
    }

    /**
     * Calculate mending success rate based on skill value.
     *
     * @param skill The player's skill value (0-100)
     * @return Success rate as percentage (0-100)
     */
    private fun calculateMendingSuccessRate(skill: Double): Double {
        // Formula: min(100, skill * 100 / 60)
        // Skill 0 = 0%, Skill 30 = 50%, Skill 60+ = 100%
        return (skill * 100.0 / 60.0).coerceIn(0.0, 100.0)
    }
}
