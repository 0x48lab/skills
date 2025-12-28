package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.gathering.GatheringDifficulty
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent

class GatheringListener(private val plugin: Skills) : Listener {

    // Crop block types for farming
    private val cropBlocks = setOf(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.MELON,
        Material.PUMPKIN,
        Material.NETHER_WART,
        Material.SWEET_BERRY_BUSH,
        Material.COCOA,
        Material.BAMBOO,
        Material.SUGAR_CANE,
        Material.CACTUS,
        Material.TORCHFLOWER,
        Material.PITCHER_PLANT,
        Material.TORCHFLOWER_CROP,
        Material.PITCHER_CROP
    )

    // Seed items for planting
    private val seedItems = setOf(
        Material.WHEAT_SEEDS,
        Material.CARROT,
        Material.POTATO,
        Material.BEETROOT_SEEDS,
        Material.MELON_SEEDS,
        Material.PUMPKIN_SEEDS,
        Material.NETHER_WART,
        Material.SWEET_BERRIES,
        Material.COCOA_BEANS,
        Material.BAMBOO,
        Material.SUGAR_CANE,
        Material.CACTUS,
        Material.TORCHFLOWER_SEEDS,
        Material.PITCHER_POD
    )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val drops = event.block.getDrops(player.inventory.itemInMainHand).toMutableList()

        // Check if it's an ore (Mining)
        if (GatheringDifficulty.isOre(block.type)) {
            plugin.gatheringManager.processMining(player, block, drops)

            // Apply modified drops
            event.isDropItems = false
            drops.forEach { drop ->
                block.world.dropItemNaturally(block.location, drop)
            }
        }
        // Check if it's a log (Lumberjacking)
        else if (GatheringDifficulty.isLog(block.type)) {
            plugin.gatheringManager.processLumberjacking(player, block, drops)

            // Apply modified drops
            event.isDropItems = false
            drops.forEach { drop ->
                block.world.dropItemNaturally(block.location, drop)
            }
        }
        // Check if it's a crop (Farming)
        else if (cropBlocks.contains(block.type)) {
            // Only process mature crops
            if (GatheringDifficulty.isMatureCrop(block.type, block.blockData)) {
                plugin.gatheringManager.processFarmingHarvest(player, block, drops)

                // Apply modified drops
                event.isDropItems = false
                drops.forEach { drop ->
                    block.world.dropItemNaturally(block.location, drop)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        val block = event.clickedBlock ?: return

        // Check for right-click actions
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        // Tilling soil with hoe
        if (item.type.name.endsWith("_HOE") &&
            (block.type == Material.DIRT || block.type == Material.GRASS_BLOCK ||
             block.type == Material.COARSE_DIRT || block.type == Material.ROOTED_DIRT)) {
            plugin.gatheringManager.processTilling(player)
        }
        // Planting seeds
        else if (seedItems.contains(item.type)) {
            // Check if planting on farmland or appropriate block
            val canPlant = when (item.type) {
                Material.NETHER_WART -> block.type == Material.SOUL_SAND
                Material.COCOA_BEANS -> block.type.name.contains("JUNGLE_LOG")
                Material.BAMBOO, Material.SUGAR_CANE, Material.CACTUS -> true
                else -> block.type == Material.FARMLAND
            }
            if (canPlant) {
                plugin.gatheringManager.processPlanting(player, item.type)
            }
        }
        // Bone meal on crops
        else if (item.type == Material.BONE_MEAL && cropBlocks.contains(block.type)) {
            plugin.gatheringManager.processBoneMeal(player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player

        // Only process when actually catching something
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH &&
            event.state != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return
        }

        val caught = event.caught as? Item ?: return

        // Process fishing
        plugin.gatheringManager.processFishing(player, caught)
    }
}
