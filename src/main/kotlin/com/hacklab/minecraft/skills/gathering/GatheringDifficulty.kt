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
            else -> null
        }
    }
}
