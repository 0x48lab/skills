package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class GatheringManager(private val plugin: Skills) {

    /**
     * Process mining event
     * @return true if plugin should handle drops (item drops), false if vanilla should handle (block drops)
     */
    fun processMining(player: Player, block: Block, drops: MutableList<ItemStack>): Boolean {
        if (player.gameMode == GameMode.CREATIVE) return false
        if (!GatheringDifficulty.isOre(block.type)) return false

        val data = plugin.playerDataManager.getPlayerData(player)
        val miningSkill = data.getSkillValue(SkillType.MINING)
        val difficulty = GatheringDifficulty.getMiningDifficulty(block.type)

        // Try skill gain (always, regardless of drop type)
        plugin.skillManager.tryGainSkill(player, SkillType.MINING, difficulty)

        // Check if drops are blocks (silk touch or ancient debris)
        // If all drops are blocks, let vanilla handle it - no bonus for block drops
        val hasItemDrops = drops.any { !it.type.isBlock }
        if (!hasItemDrops) {
            // Block drops (silk touch, ancient debris) - let vanilla handle
            return false
        }

        // Item drops - apply bonus chance based on skill
        val bonusChance = miningSkill / 2.0  // Max 50%
        if (Random.nextDouble() * 100 < bonusChance) {
            // Add bonus drop (duplicate first non-block drop)
            drops.firstOrNull { !it.type.isBlock }?.let { itemDrop ->
                drops.add(itemDrop.clone())
                plugin.messageSender.send(player, MessageKey.GATHERING_BONUS_DROP)
            }
        }

        return true
    }

    /**
     * Process lumberjacking event
     */
    fun processLumberjacking(player: Player, block: Block, drops: MutableList<ItemStack>) {
        if (player.gameMode == GameMode.CREATIVE) return
        if (!GatheringDifficulty.isLog(block.type)) return

        val data = plugin.playerDataManager.getPlayerData(player)
        val lumberSkill = data.getSkillValue(SkillType.LUMBERJACKING)
        val difficulty = GatheringDifficulty.getLumberDifficulty(block.type)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.LUMBERJACKING, difficulty)

        // Bonus log drop
        val bonusChance = lumberSkill / 2.0  // Max 50%
        if (Random.nextDouble() * 100 < bonusChance) {
            drops.add(ItemStack(block.type))
            plugin.messageSender.send(player, MessageKey.GATHERING_BONUS_DROP)
        }

        // Increased sapling drop chance
        val saplingBonus = lumberSkill / 5.0  // Max +20%
        val saplingType = getSaplingForLog(block.type)
        if (saplingType != null && Random.nextDouble() * 100 < saplingBonus) {
            drops.add(ItemStack(saplingType))
        }
    }

    /**
     * Process fishing event
     */
    fun processFishing(player: Player, caught: Item): FishingResult? {
        if (player.gameMode == GameMode.CREATIVE) return null
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)

        // Determine what was caught and difficulty
        val caughtItem = caught.itemStack
        val difficulty = when (caughtItem.type) {
            Material.COD, Material.SALMON -> GatheringDifficulty.FISHING_FISH_DIFFICULTY
            Material.TROPICAL_FISH -> GatheringDifficulty.FISHING_TROPICAL_DIFFICULTY
            Material.PUFFERFISH -> GatheringDifficulty.FISHING_PUFFER_DIFFICULTY
            Material.ENCHANTED_BOOK, Material.BOW, Material.FISHING_ROD,
            Material.NAME_TAG, Material.NAUTILUS_SHELL, Material.SADDLE ->
                GatheringDifficulty.FISHING_TREASURE_DIFFICULTY
            else -> GatheringDifficulty.FISHING_FISH_DIFFICULTY
        }

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.FISHING, difficulty)

        // Calculate if this was a "treasure" catch
        val isTreasure = difficulty >= GatheringDifficulty.FISHING_TREASURE_DIFFICULTY
        if (isTreasure) {
            plugin.messageSender.send(player, MessageKey.GATHERING_RARE_FIND)
        }

        return FishingResult(
            item = caughtItem,
            isTreasure = isTreasure,
            skillGained = true
        )
    }

    /**
     * Calculate fishing wait time reduction
     */
    fun getFishingWaitReduction(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)
        return fishingSkill / 5.0  // Max -20%
    }

    /**
     * Calculate treasure chance bonus for fishing
     */
    fun getFishingTreasureBonus(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)
        return fishingSkill / 4.0  // Max +25%
    }

    /**
     * Calculate junk reduction for fishing
     */
    fun getFishingJunkReduction(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)
        return fishingSkill / 2.0  // Max -50%
    }

    /**
     * Get mining speed bonus based on STR and skill
     */
    fun getMiningSpeedBonus(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val str = data.str
        val miningSkill = data.getSkillValue(SkillType.MINING)
        return (str / 10.0) + (miningSkill / 10.0)  // Max +20%
    }

    /**
     * Get pickaxe durability reduction chance for mining ores
     * @return chance to cancel durability damage (0.0 to 1.0)
     */
    fun getPickaxeDurabilityReduction(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val miningSkill = data.getSkillValue(SkillType.MINING)

        // GM (skill 100) gets 100% reduction, otherwise skill * 0.9%
        return if (miningSkill >= 100.0) {
            1.0  // 100% chance to cancel durability damage
        } else {
            miningSkill * 0.9 / 100.0  // 0-89% chance
        }
    }

    /**
     * Get lumberjacking speed bonus based on STR and skill
     */
    fun getLumberSpeedBonus(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val str = data.str
        val lumberSkill = data.getSkillValue(SkillType.LUMBERJACKING)
        return (str / 10.0) + (lumberSkill / 10.0)  // Max +20%
    }

    private fun getSaplingForLog(logType: Material): Material? {
        return when (logType) {
            Material.OAK_LOG -> Material.OAK_SAPLING
            Material.BIRCH_LOG -> Material.BIRCH_SAPLING
            Material.SPRUCE_LOG -> Material.SPRUCE_SAPLING
            Material.JUNGLE_LOG -> Material.JUNGLE_SAPLING
            Material.ACACIA_LOG -> Material.ACACIA_SAPLING
            Material.DARK_OAK_LOG -> Material.DARK_OAK_SAPLING
            Material.CHERRY_LOG -> Material.CHERRY_SAPLING
            Material.MANGROVE_LOG -> Material.MANGROVE_PROPAGULE
            else -> null
        }
    }

    /**
     * Process farming harvest event (mature crop broken)
     */
    fun processFarmingHarvest(player: Player, block: Block, drops: MutableList<ItemStack>) {
        if (player.gameMode == GameMode.CREATIVE) return
        val data = plugin.playerDataManager.getPlayerData(player)
        val farmingSkill = data.getSkillValue(SkillType.FARMING)
        val difficulty = GatheringDifficulty.getFarmingDifficulty(block.type)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.FARMING, difficulty)

        // Bonus harvest chance based on skill (max 50%)
        val bonusChance = farmingSkill / 2.0
        if (Random.nextDouble() * 100 < bonusChance) {
            // Add bonus drop (duplicate first drop)
            drops.firstOrNull()?.let { firstDrop ->
                drops.add(firstDrop.clone())
                plugin.messageSender.send(player, MessageKey.GATHERING_BONUS_DROP)
            }
        }

        // Extra seed drop chance (max 25%)
        val seedChance = farmingSkill / 4.0
        val seedType = getSeedForCrop(block.type)
        if (seedType != null && Random.nextDouble() * 100 < seedChance) {
            drops.add(ItemStack(seedType))
        }
    }

    /**
     * Process tilling soil with hoe
     */
    fun processTilling(player: Player) {
        if (player.gameMode == GameMode.CREATIVE) return
        plugin.skillManager.tryGainSkill(
            player,
            SkillType.FARMING,
            GatheringDifficulty.FARMING_TILL_DIFFICULTY
        )
    }

    /**
     * Process planting seeds
     */
    fun processPlanting(player: Player, seedType: Material) {
        if (player.gameMode == GameMode.CREATIVE) return
        val difficulty = when (seedType) {
            Material.WHEAT_SEEDS -> 10
            Material.CARROT -> 15
            Material.POTATO -> 15
            Material.BEETROOT_SEEDS -> 20
            Material.NETHER_WART -> 35
            Material.MELON_SEEDS -> 25
            Material.PUMPKIN_SEEDS -> 25
            Material.TORCHFLOWER_SEEDS -> 40
            Material.PITCHER_POD -> 45
            else -> GatheringDifficulty.FARMING_PLANT_DIFFICULTY
        }
        plugin.skillManager.tryGainSkill(player, SkillType.FARMING, difficulty)
    }

    /**
     * Process bone meal usage on crops
     */
    fun processBoneMeal(player: Player) {
        if (player.gameMode == GameMode.CREATIVE) return
        plugin.skillManager.tryGainSkill(
            player,
            SkillType.FARMING,
            GatheringDifficulty.FARMING_BONEMEAL_DIFFICULTY
        )
    }

    /**
     * Get bonus bone meal effectiveness based on farming skill
     */
    fun getBoneMealBonus(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val farmingSkill = data.getSkillValue(SkillType.FARMING)
        return farmingSkill / 5.0  // Max +20% effectiveness
    }

    private fun getSeedForCrop(cropType: Material): Material? {
        return when (cropType) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            Material.MELON_STEM, Material.MELON -> Material.MELON_SEEDS
            Material.PUMPKIN_STEM, Material.PUMPKIN -> Material.PUMPKIN_SEEDS
            Material.TORCHFLOWER -> Material.TORCHFLOWER_SEEDS
            Material.PITCHER_PLANT -> Material.PITCHER_POD
            else -> null
        }
    }
}

data class FishingResult(
    val item: ItemStack,
    val isTreasure: Boolean,
    val skillGained: Boolean
)
