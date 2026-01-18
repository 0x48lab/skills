package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Manages auto-replanting and auto-growth for Farming skill.
 * When harvesting mature crops, automatically replants if player has seeds.
 * After planting, advances growth stages based on skill level.
 */
class AutoFarmingManager(private val plugin: Skills) {

    /**
     * Crop to seed mapping for auto-replanting.
     * Only includes crops that can be replanted (excludes melon, pumpkin, berry bush, etc.)
     */
    private val cropToSeed = mapOf(
        // Regular field crops
        Material.WHEAT to Material.WHEAT_SEEDS,
        Material.CARROTS to Material.CARROT,
        Material.POTATOES to Material.POTATO,
        Material.BEETROOTS to Material.BEETROOT_SEEDS,
        // Nether crop
        Material.NETHER_WART to Material.NETHER_WART,
        // 1.20+ crops
        Material.TORCHFLOWER_CROP to Material.TORCHFLOWER_SEEDS,
        Material.PITCHER_CROP to Material.PITCHER_POD
    )

    /**
     * Seed to crop block mapping for planting.
     */
    private val seedToCrop = mapOf(
        Material.WHEAT_SEEDS to Material.WHEAT,
        Material.CARROT to Material.CARROTS,
        Material.POTATO to Material.POTATOES,
        Material.BEETROOT_SEEDS to Material.BEETROOTS,
        Material.NETHER_WART to Material.NETHER_WART,
        Material.TORCHFLOWER_SEEDS to Material.TORCHFLOWER_CROP,
        Material.PITCHER_POD to Material.PITCHER_CROP
    )

    /**
     * Check if a crop type supports auto-replanting.
     */
    fun isAutoReplantableCrop(cropType: Material): Boolean {
        return cropToSeed.containsKey(cropType)
    }

    /**
     * Get the seed type for a given crop.
     */
    fun getSeedForCrop(cropType: Material): Material? {
        return cropToSeed[cropType]
    }

    /**
     * Get the crop block type for a given seed.
     */
    fun getCropForSeed(seedType: Material): Material? {
        return seedToCrop[seedType]
    }

    /**
     * Calculate growth stages to advance based on Farming skill.
     * @return growth stages (1-5, or Int.MAX_VALUE for instant maturity at GM)
     */
    fun getGrowthStages(player: Player): Int {
        val data = plugin.playerDataManager.getPlayerData(player)
        val skill = data.getSkillValue(SkillType.FARMING)

        // GM (skill 100) = instant maturity
        if (skill >= 100.0) {
            return Int.MAX_VALUE
        }

        // Skill 0-19: 1 stage, 20-39: 2 stages, etc.
        return 1 + (skill / 20).toInt()
    }

    /**
     * Find and consume a seed from player's inventory.
     * @return true if seed was found and consumed
     */
    fun consumeSeed(player: Player, seedType: Material): Boolean {
        val inventory = player.inventory

        // Search for the seed in inventory
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type == seedType && item.amount > 0) {
                // Consume one seed
                if (item.amount > 1) {
                    item.amount -= 1
                } else {
                    inventory.setItem(i, null)
                }
                return true
            }
        }
        return false
    }

    /**
     * Plant a crop at the given block location and advance its growth.
     * @param block The block where to plant (should be farmland or soul sand)
     * @param cropType The crop block type to plant
     * @param growthStages Number of growth stages to advance
     */
    fun plantAndGrow(block: Block, cropType: Material, growthStages: Int) {
        // Set the crop block
        block.type = cropType

        // Get the block data and advance growth
        val blockData = block.blockData
        if (blockData is Ageable) {
            val maxAge = blockData.maximumAge
            val newAge = minOf(growthStages, maxAge)
            blockData.age = newAge
            block.blockData = blockData
        }
    }

    /**
     * Process auto-replanting when a mature crop is harvested.
     * @param player The player who harvested the crop
     * @param harvestedBlock The block that was harvested (now air or about to be)
     * @param cropType The type of crop that was harvested
     * @return true if auto-replant was successful
     */
    fun processAutoReplant(player: Player, harvestedBlock: Block, cropType: Material): Boolean {
        // Check if this crop supports auto-replanting
        val seedType = getSeedForCrop(cropType) ?: return false

        // Check if player has the seed
        if (!consumeSeed(player, seedType)) {
            return false
        }

        // Get the block below (for verification) and calculate growth
        val growthStages = getGrowthStages(player)

        // Schedule planting for next tick (after the block is broken)
        plugin.server.scheduler.runTask(plugin, Runnable {
            // Verify the block is now air (crop was broken)
            if (harvestedBlock.type == Material.AIR) {
                // Plant the crop
                plantAndGrow(harvestedBlock, cropType, growthStages)

                // Send message to player
                val isInstantMature = growthStages == Int.MAX_VALUE
                if (isInstantMature) {
                    plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_REPLANT_MATURE)
                } else {
                    plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_REPLANT, "stages" to growthStages)
                }
            }
        })

        return true
    }

    /**
     * Apply auto-growth when manually planting a seed.
     * Called from PlayerInteractEvent when planting seeds.
     * @param player The player planting the seed
     * @param plantedBlock The block where the crop was planted
     */
    fun processAutoGrowth(player: Player, plantedBlock: Block) {
        val blockData = plantedBlock.blockData
        if (blockData !is Ageable) return

        val growthStages = getGrowthStages(player)
        val maxAge = blockData.maximumAge
        val newAge = minOf(growthStages, maxAge)

        // Only apply if there's actual growth to add
        if (newAge > blockData.age) {
            blockData.age = newAge
            plantedBlock.blockData = blockData
        }
    }
}
