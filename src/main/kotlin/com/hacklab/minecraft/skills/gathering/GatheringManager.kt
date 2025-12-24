package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class GatheringManager(private val plugin: Skills) {

    /**
     * Process mining event
     */
    fun processMining(player: Player, block: Block, drops: MutableList<ItemStack>) {
        if (!GatheringDifficulty.isOre(block.type)) return

        val data = plugin.playerDataManager.getPlayerData(player)
        val miningSkill = data.getSkillValue(SkillType.MINING)
        val difficulty = GatheringDifficulty.getMiningDifficulty(block.type)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.MINING, difficulty)

        // Bonus drop chance based on skill
        val bonusChance = miningSkill / 2.0  // Max 50%
        if (Random.nextDouble() * 100 < bonusChance) {
            // Add bonus drop (duplicate first drop)
            drops.firstOrNull()?.let { firstDrop ->
                drops.add(firstDrop.clone())
                plugin.messageSender.send(player, MessageKey.GATHERING_BONUS_DROP)
            }
        }

        // Bonus experience orb chance
        val expBonus = miningSkill / 5.0  // Max +20%
        // Experience bonus would be applied through event modification
    }

    /**
     * Process lumberjacking event
     */
    fun processLumberjacking(player: Player, block: Block, drops: MutableList<ItemStack>) {
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
    fun processFishing(player: Player, caught: Item): FishingResult {
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
        val str = data.getStr()
        val miningSkill = data.getSkillValue(SkillType.MINING)
        return (str / 10.0) + (miningSkill / 10.0)  // Max +20%
    }

    /**
     * Get lumberjacking speed bonus based on STR and skill
     */
    fun getLumberSpeedBonus(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val str = data.getStr()
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
}

data class FishingResult(
    val item: ItemStack,
    val isTreasure: Boolean,
    val skillGained: Boolean
)
