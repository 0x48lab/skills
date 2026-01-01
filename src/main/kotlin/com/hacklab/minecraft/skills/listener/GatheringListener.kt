package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.gathering.GatheringDifficulty
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.FishHook
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class GatheringListener(private val plugin: Skills) : Listener {

    // Track players who just mined ore for durability reduction
    private val recentOreMining: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // Track players who just cut logs for durability reduction
    private val recentLogCutting: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // Track players who just caught fish for durability reduction
    private val recentFishing: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // Track players who just tilled soil for durability reduction
    private val recentTilling: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

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

        // Skip skill processing in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val drops = event.block.getDrops(player.inventory.itemInMainHand).toMutableList()

        // Check if it's an ore (Mining)
        if (GatheringDifficulty.isOre(block.type)) {
            // Mark player as mining ore for durability reduction
            recentOreMining.add(player.uniqueId)

            val shouldHandleDrops = plugin.gatheringManager.processMining(player, block, drops)

            // Only handle drops if processMining returns true (item drops with potential bonus)
            // If false (block drops like silk touch or ancient debris), let vanilla handle it
            if (shouldHandleDrops) {
                event.isDropItems = false
                drops.forEach { drop ->
                    block.world.dropItemNaturally(block.location, drop)
                }
            }
        }
        // Check if it's a log (Lumberjacking)
        else if (GatheringDifficulty.isLog(block.type)) {
            // Mark player as cutting log for durability reduction
            recentLogCutting.add(player.uniqueId)

            // Process skill gain only - no bonus drops, vanilla handles drops
            plugin.gatheringManager.processLumberjacking(player, block)
        }
        // Check if it's a crop (Farming)
        else if (cropBlocks.contains(block.type)) {
            // Only process mature crops - skill gain only, vanilla handles drops
            if (GatheringDifficulty.isMatureCrop(block.type, block.blockData)) {
                plugin.gatheringManager.processFarmingHarvest(player, block)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // Skip skill processing in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        val item = event.item ?: return
        val block = event.clickedBlock ?: return

        // Check for right-click actions
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        // Tilling soil with hoe
        if (item.type.name.endsWith("_HOE") &&
            (block.type == Material.DIRT || block.type == Material.GRASS_BLOCK ||
             block.type == Material.COARSE_DIRT || block.type == Material.ROOTED_DIRT)) {
            // Mark player for durability reduction
            recentTilling.add(player.uniqueId)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player

        // Skip skill processing in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        when (event.state) {
            // When casting the fishing rod, reduce wait time based on skill
            PlayerFishEvent.State.FISHING -> {
                val hook = event.hook
                val reduction = plugin.gatheringManager.getFishingWaitReduction(player)

                if (reduction > 0) {
                    // Default: 100-600 ticks (5-30 seconds)
                    // Reduce both min and max wait times
                    val newMinWait = (hook.minWaitTime * (1 - reduction)).toInt().coerceAtLeast(20)
                    val newMaxWait = (hook.maxWaitTime * (1 - reduction)).toInt().coerceAtLeast(newMinWait + 20)

                    hook.minWaitTime = newMinWait
                    hook.maxWaitTime = newMaxWait
                }
            }

            // When catching something, process skill gain and track for durability
            PlayerFishEvent.State.CAUGHT_FISH, PlayerFishEvent.State.CAUGHT_ENTITY -> {
                val caught = event.caught as? Item ?: return

                // Mark player as fishing for durability reduction
                recentFishing.add(player.uniqueId)

                // Process fishing skill
                plugin.gatheringManager.processFishing(player, caught)
            }

            else -> { /* ignore other states */ }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        val player = event.player

        // Skip in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        val item = event.item

        // Check if player just mined an ore with a pickaxe
        if (recentOreMining.remove(player.uniqueId) && isPickaxe(item.type)) {
            val reductionChance = plugin.gatheringManager.getPickaxeDurabilityReduction(player)
            if (Random.nextDouble() < reductionChance) {
                event.isCancelled = true
            }
            return
        }

        // Check if player just cut a log with an axe
        if (recentLogCutting.remove(player.uniqueId) && isAxe(item.type)) {
            val reductionChance = plugin.gatheringManager.getAxeDurabilityReduction(player)
            if (Random.nextDouble() < reductionChance) {
                event.isCancelled = true
            }
            return
        }

        // Check if player just caught fish with a fishing rod
        if (recentFishing.remove(player.uniqueId) && isFishingRod(item.type)) {
            val reductionChance = plugin.gatheringManager.getFishingRodDurabilityReduction(player)
            if (Random.nextDouble() < reductionChance) {
                event.isCancelled = true
            }
            return
        }

        // Check if player just tilled soil with a hoe
        if (recentTilling.remove(player.uniqueId) && isHoe(item.type)) {
            val reductionChance = plugin.gatheringManager.getHoeDurabilityReduction(player)
            if (Random.nextDouble() < reductionChance) {
                event.isCancelled = true
            }
        }
    }

    private fun isPickaxe(material: Material): Boolean {
        return material == Material.WOODEN_PICKAXE ||
               material == Material.STONE_PICKAXE ||
               material == Material.IRON_PICKAXE ||
               material == Material.GOLDEN_PICKAXE ||
               material == Material.DIAMOND_PICKAXE ||
               material == Material.NETHERITE_PICKAXE
    }

    private fun isAxe(material: Material): Boolean {
        return material == Material.WOODEN_AXE ||
               material == Material.STONE_AXE ||
               material == Material.IRON_AXE ||
               material == Material.GOLDEN_AXE ||
               material == Material.DIAMOND_AXE ||
               material == Material.NETHERITE_AXE
    }

    private fun isFishingRod(material: Material): Boolean {
        return material == Material.FISHING_ROD
    }

    private fun isHoe(material: Material): Boolean {
        return material == Material.WOODEN_HOE ||
               material == Material.STONE_HOE ||
               material == Material.IRON_HOE ||
               material == Material.GOLDEN_HOE ||
               material == Material.DIAMOND_HOE ||
               material == Material.NETHERITE_HOE
    }
}
