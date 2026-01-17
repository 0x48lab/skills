package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import io.papermc.paper.event.player.PlayerPickBlockEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

/**
 * Listener to fix pick block functionality when items have custom maxStackSize.
 *
 * The StackBonusManager modifies maxStackSize of items, which causes
 * ItemStack.isSimilar() to fail when comparing inventory items to world blocks.
 * This listener intercepts PlayerPickBlockEvent and performs a custom search
 * that ignores maxStackSize differences.
 */
class PickBlockListener(private val plugin: Skills) : Listener {

    /**
     * Handle pick block event when vanilla search fails due to maxStackSize mismatch.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPickBlock(event: PlayerPickBlockEvent) {
        // Skip if vanilla already found a matching slot
        if (event.sourceSlot != -1) return

        val player = event.player
        val block = event.block
        val targetMaterial = block.type

        // Skip air blocks
        if (targetMaterial.isAir) return

        // Get the item that would be dropped by this block
        val blockItem = getBlockItem(block.type)
        if (blockItem.type.isAir) return

        // Search inventory for matching item, ignoring maxStackSize
        val matchingSlot = findMatchingSlotIgnoringMaxStack(player, blockItem.type)
        if (matchingSlot != -1) {
            event.sourceSlot = matchingSlot
        }
    }

    /**
     * Find a slot containing the specified material, ignoring maxStackSize differences.
     *
     * @param player The player whose inventory to search
     * @param material The material to find
     * @return The slot index, or -1 if not found
     */
    private fun findMatchingSlotIgnoringMaxStack(player: Player, material: Material): Int {
        val inventory = player.inventory

        // First, check hotbar (slots 0-8) - prefer hotbar for faster access
        for (i in 0..8) {
            val item = inventory.getItem(i) ?: continue
            if (item.type == material) {
                return i
            }
        }

        // Then check main inventory (slots 9-35)
        for (i in 9..35) {
            val item = inventory.getItem(i) ?: continue
            if (item.type == material) {
                return i
            }
        }

        return -1
    }

    /**
     * Get the item that would be obtained from a block.
     * Most blocks drop themselves, but some have special drops.
     */
    private fun getBlockItem(blockType: Material): ItemStack {
        // Handle special cases where block != item
        val itemType = when (blockType) {
            // Ores that drop different items
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE -> Material.COAL
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE -> Material.EMERALD
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE
            Material.NETHER_QUARTZ_ORE -> Material.QUARTZ
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> Material.RAW_IRON
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD

            // Crops
            Material.WHEAT -> Material.WHEAT
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT

            // Other special blocks
            Material.GLOWSTONE -> Material.GLOWSTONE_DUST
            Material.SEA_LANTERN -> Material.PRISMARINE_CRYSTALS
            Material.MELON -> Material.MELON_SLICE

            // Wall signs to sign items
            Material.OAK_WALL_SIGN -> Material.OAK_SIGN
            Material.SPRUCE_WALL_SIGN -> Material.SPRUCE_SIGN
            Material.BIRCH_WALL_SIGN -> Material.BIRCH_SIGN
            Material.JUNGLE_WALL_SIGN -> Material.JUNGLE_SIGN
            Material.ACACIA_WALL_SIGN -> Material.ACACIA_SIGN
            Material.DARK_OAK_WALL_SIGN -> Material.DARK_OAK_SIGN
            Material.MANGROVE_WALL_SIGN -> Material.MANGROVE_SIGN
            Material.CHERRY_WALL_SIGN -> Material.CHERRY_SIGN
            Material.BAMBOO_WALL_SIGN -> Material.BAMBOO_SIGN
            Material.CRIMSON_WALL_SIGN -> Material.CRIMSON_SIGN
            Material.WARPED_WALL_SIGN -> Material.WARPED_SIGN

            // Wall torches
            Material.WALL_TORCH -> Material.TORCH
            Material.SOUL_WALL_TORCH -> Material.SOUL_TORCH
            Material.REDSTONE_WALL_TORCH -> Material.REDSTONE_TORCH

            // Wall banners
            Material.WHITE_WALL_BANNER -> Material.WHITE_BANNER
            Material.ORANGE_WALL_BANNER -> Material.ORANGE_BANNER
            Material.MAGENTA_WALL_BANNER -> Material.MAGENTA_BANNER
            Material.LIGHT_BLUE_WALL_BANNER -> Material.LIGHT_BLUE_BANNER
            Material.YELLOW_WALL_BANNER -> Material.YELLOW_BANNER
            Material.LIME_WALL_BANNER -> Material.LIME_BANNER
            Material.PINK_WALL_BANNER -> Material.PINK_BANNER
            Material.GRAY_WALL_BANNER -> Material.GRAY_BANNER
            Material.LIGHT_GRAY_WALL_BANNER -> Material.LIGHT_GRAY_BANNER
            Material.CYAN_WALL_BANNER -> Material.CYAN_BANNER
            Material.PURPLE_WALL_BANNER -> Material.PURPLE_BANNER
            Material.BLUE_WALL_BANNER -> Material.BLUE_BANNER
            Material.BROWN_WALL_BANNER -> Material.BROWN_BANNER
            Material.GREEN_WALL_BANNER -> Material.GREEN_BANNER
            Material.RED_WALL_BANNER -> Material.RED_BANNER
            Material.BLACK_WALL_BANNER -> Material.BLACK_BANNER

            // Default: block drops itself
            else -> blockType
        }

        return ItemStack(itemType)
    }
}
