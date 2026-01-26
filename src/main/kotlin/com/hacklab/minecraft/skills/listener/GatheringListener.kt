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
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
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

    // Track players who just dug soft blocks for durability reduction
    private val recentDigging: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

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

    /**
     * Apply Haste effect when player starts mining/digging
     * This gives the speed bonus based on Mining skill
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player

        // Skip in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val heldItem = player.inventory.itemInMainHand

        // Check if mining with pickaxe or digging soft block with shovel
        val speedBonus: Double = when {
            GatheringDifficulty.isMineable(block.type) && isPickaxe(heldItem.type) -> {
                plugin.gatheringManager.getMiningSpeedBonus(player)
            }
            GatheringDifficulty.isDiggable(block.type) && isShovel(heldItem.type) -> {
                plugin.gatheringManager.getDiggingSpeedBonus(player)
            }
            GatheringDifficulty.isLog(block.type) && isAxe(heldItem.type) -> {
                plugin.gatheringManager.getLumberSpeedBonus(player)
            }
            else -> 0.0
        }

        // Apply Haste effect if there's a speed bonus
        if (speedBonus > 0) {
            applyMiningSpeedBonus(player, speedBonus)
        }
    }

    /**
     * Apply mining speed bonus as Haste effect
     * Haste amplifier: 0 = Haste I (+20%), 1 = Haste II (+40%)
     */
    private fun applyMiningSpeedBonus(player: Player, bonusPercent: Double) {
        // Convert bonus percentage to Haste amplifier
        // Each Haste level adds ~20% speed
        val hasteLevel = when {
            bonusPercent >= 40.0 -> 1  // Haste II (+40%)
            bonusPercent >= 20.0 -> 0  // Haste I (+20%)
            else -> return  // Below 20% - no effect applied
        }

        // Check if player already has a stronger Haste effect
        val existingHaste = player.getPotionEffect(PotionEffectType.HASTE)
        if (existingHaste != null && existingHaste.amplifier >= hasteLevel) {
            return  // Don't override stronger effects (beacons, potions)
        }

        // Apply short Haste effect (3 seconds = 60 ticks)
        // Using ambient=true, particles=false for less visual clutter
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.HASTE,
                60,  // 3 seconds
                hasteLevel,
                true,  // ambient
                false, // particles
                true   // icon
            )
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player

        // Breaking blocks breaks hiding/invisibility
        if (plugin.hidingManager.isHidden(player.uniqueId)) {
            plugin.hidingManager.breakHiding(player, "block_break")
        } else if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // Magic invisibility - remove it on block break
            player.removePotionEffect(PotionEffectType.INVISIBILITY)
        }

        // Skip skill processing in Creative mode
        if (player.gameMode == GameMode.CREATIVE) return

        val block = event.block
        val drops = event.block.getDrops(player.inventory.itemInMainHand).toMutableList()

        // Check if it's mineable with pickaxe (ores + stone)
        if (GatheringDifficulty.isMineable(block.type)) {
            // Mark player as mining for durability reduction
            recentOreMining.add(player.uniqueId)

            val shouldHandleDrops = plugin.gatheringManager.processMining(player, block, drops)

            // Only handle drops if processMining returns true (item drops with potential bonus)
            // If false (block drops like silk touch, ancient debris, or stone), let vanilla handle it
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

            // Store log type before it's broken
            val logType = block.type

            // Process skill gain
            plugin.gatheringManager.processLumberjacking(player, block)

            // Process chain chopping (upward only)
            plugin.chainChoppingManager.processChainChopping(player, block, logType)
        }
        // Check if it's a crop (Farming)
        else if (cropBlocks.contains(block.type)) {
            // Only process mature crops
            if (GatheringDifficulty.isMatureCrop(block.type, block.blockData)) {
                val cropType = block.type

                // Process skill gain
                plugin.gatheringManager.processFarmingHarvest(player, block)

                // Process auto-replanting (if supported and player has seeds)
                if (plugin.autoFarmingManager.isAutoReplantableCrop(cropType)) {
                    plugin.autoFarmingManager.processAutoReplant(player, block, cropType)
                }
            }
        }
        // Check if it's a diggable block (soft blocks with shovel)
        else if (GatheringDifficulty.isDiggable(block.type)) {
            val heldItem = player.inventory.itemInMainHand
            // Only process if player is using a shovel
            if (isShovel(heldItem.type)) {
                // Mark player as digging for durability reduction
                recentDigging.add(player.uniqueId)

                // Process skill gain - uses Mining skill
                plugin.gatheringManager.processDigging(player, block)
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
            // When casting the fishing rod
            PlayerFishEvent.State.FISHING -> {
                // Track the hook for auto-fishing
                // No wait time reduction anymore (removed old system)
            }

            // When a fish bites (before catching)
            PlayerFishEvent.State.BITE -> {
                // Try auto-fishing (handles skill check, probability, and GM logic internally)
                val hook = event.hook
                plugin.autoFishingManager.processFishBite(player, hook)
            }

            // When catching something, process skill gain and track for durability
            PlayerFishEvent.State.CAUGHT_FISH, PlayerFishEvent.State.CAUGHT_ENTITY -> {
                val caught = event.caught as? Item ?: return

                // Mark player as fishing for durability reduction
                recentFishing.add(player.uniqueId)

                // Process fishing skill
                plugin.gatheringManager.processFishing(player, caught)

                // Process auto-recast if auto-fishing is active
                plugin.autoFishingManager.processAfterCatch(player)
            }

            // When fishing is cancelled or fails
            PlayerFishEvent.State.REEL_IN, PlayerFishEvent.State.IN_GROUND,
            PlayerFishEvent.State.FAILED_ATTEMPT -> {
                // Stop auto-fishing if player manually interacts
                if (event.state == PlayerFishEvent.State.REEL_IN) {
                    plugin.autoFishingManager.cancelAutoFishing(player)
                }
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
            return
        }

        // Check if player just dug soft blocks with a shovel
        if (recentDigging.remove(player.uniqueId) && isShovel(item.type)) {
            val reductionChance = plugin.gatheringManager.getShovelDurabilityReduction(player)
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

    private fun isShovel(material: Material): Boolean {
        return material == Material.WOODEN_SHOVEL ||
               material == Material.STONE_SHOVEL ||
               material == Material.IRON_SHOVEL ||
               material == Material.GOLDEN_SHOVEL ||
               material == Material.DIAMOND_SHOVEL ||
               material == Material.NETHERITE_SHOVEL
    }
}
