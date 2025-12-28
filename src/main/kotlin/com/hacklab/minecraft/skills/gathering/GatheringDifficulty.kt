package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material

object GatheringDifficulty {
    data class GatherInfo(
        val skill: SkillType,
        val difficulty: Int
    )

    // Mining difficulties
    private val miningDifficulties = mapOf(
        Material.COAL_ORE to 10,
        Material.DEEPSLATE_COAL_ORE to 15,
        Material.COPPER_ORE to 20,
        Material.DEEPSLATE_COPPER_ORE to 25,
        Material.IRON_ORE to 25,
        Material.DEEPSLATE_IRON_ORE to 30,
        Material.REDSTONE_ORE to 35,
        Material.DEEPSLATE_REDSTONE_ORE to 40,
        Material.LAPIS_ORE to 35,
        Material.DEEPSLATE_LAPIS_ORE to 40,
        Material.GOLD_ORE to 40,
        Material.DEEPSLATE_GOLD_ORE to 45,
        Material.NETHER_GOLD_ORE to 35,
        Material.DIAMOND_ORE to 60,
        Material.DEEPSLATE_DIAMOND_ORE to 65,
        Material.EMERALD_ORE to 70,
        Material.DEEPSLATE_EMERALD_ORE to 75,
        Material.NETHER_QUARTZ_ORE to 30,
        Material.ANCIENT_DEBRIS to 90
    )

    // Lumberjacking difficulties
    private val lumberDifficulties = mapOf(
        Material.OAK_LOG to 10,
        Material.BIRCH_LOG to 10,
        Material.SPRUCE_LOG to 10,
        Material.JUNGLE_LOG to 15,
        Material.ACACIA_LOG to 15,
        Material.DARK_OAK_LOG to 20,
        Material.MANGROVE_LOG to 25,
        Material.CHERRY_LOG to 30,
        Material.CRIMSON_STEM to 35,
        Material.WARPED_STEM to 35
    )

    // Fishing base difficulty
    const val FISHING_FISH_DIFFICULTY = 15
    const val FISHING_TROPICAL_DIFFICULTY = 25
    const val FISHING_PUFFER_DIFFICULTY = 30
    const val FISHING_TREASURE_DIFFICULTY = 70

    // Farming difficulties
    private val cropDifficulties = mapOf(
        // Basic crops
        Material.WHEAT to 10,
        Material.CARROTS to 15,
        Material.POTATOES to 15,
        Material.BEETROOTS to 20,
        // Vegetables
        Material.MELON to 25,
        Material.PUMPKIN to 25,
        // Nether crops
        Material.NETHER_WART to 35,
        // Specialty
        Material.SWEET_BERRY_BUSH to 20,
        Material.COCOA to 30,
        Material.BAMBOO to 15,
        Material.SUGAR_CANE to 15,
        Material.CACTUS to 20,
        // 1.21 crops
        Material.TORCHFLOWER to 40,
        Material.PITCHER_PLANT to 45
    )

    // Farming action difficulties
    const val FARMING_TILL_DIFFICULTY = 5
    const val FARMING_PLANT_DIFFICULTY = 10
    const val FARMING_BONEMEAL_DIFFICULTY = 15

    fun getMiningDifficulty(material: Material): Int = miningDifficulties[material] ?: 20

    fun getLumberDifficulty(material: Material): Int = lumberDifficulties[material] ?: 10

    fun isOre(material: Material): Boolean = miningDifficulties.containsKey(material)

    fun isLog(material: Material): Boolean = lumberDifficulties.containsKey(material) ||
            material.name.endsWith("_LOG") || material.name.endsWith("_STEM")

    fun getGatherInfo(material: Material): GatherInfo? {
        return when {
            miningDifficulties.containsKey(material) ->
                GatherInfo(SkillType.MINING, miningDifficulties[material]!!)
            lumberDifficulties.containsKey(material) ->
                GatherInfo(SkillType.LUMBERJACKING, lumberDifficulties[material]!!)
            cropDifficulties.containsKey(material) ->
                GatherInfo(SkillType.FARMING, cropDifficulties[material]!!)
            else -> null
        }
    }

    fun getFarmingDifficulty(material: Material): Int = cropDifficulties[material] ?: 15

    fun isCrop(material: Material): Boolean = cropDifficulties.containsKey(material) ||
            material == Material.WHEAT || material.name.contains("CROP")

    fun isMatureCrop(material: Material, blockData: org.bukkit.block.data.BlockData): Boolean {
        // Check if crop is fully grown
        return when (blockData) {
            is org.bukkit.block.data.Ageable -> blockData.age >= blockData.maximumAge
            else -> true // Non-ageable crops like melon/pumpkin are always "mature"
        }
    }
}
