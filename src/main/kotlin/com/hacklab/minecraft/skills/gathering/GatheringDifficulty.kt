package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material

object GatheringDifficulty {
    data class GatherInfo(
        val skill: SkillType,
        val difficulty: Int
    )

    // Mining difficulties (ores)
    private val oreDifficulties = mapOf(
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

    // Mining difficulties (stone blocks - pickaxe)
    private val stoneDifficulties = mapOf(
        // Basic stone
        Material.STONE to 5,
        Material.COBBLESTONE to 5,
        Material.MOSSY_COBBLESTONE to 5,
        // Deepslate
        Material.DEEPSLATE to 10,
        Material.COBBLED_DEEPSLATE to 10,
        // Decorative stones
        Material.GRANITE to 5,
        Material.DIORITE to 5,
        Material.ANDESITE to 5,
        Material.POLISHED_GRANITE to 5,
        Material.POLISHED_DIORITE to 5,
        Material.POLISHED_ANDESITE to 5,
        // Tuff and Calcite
        Material.TUFF to 5,
        Material.CALCITE to 5,
        // Dripstone
        Material.DRIPSTONE_BLOCK to 10,
        Material.POINTED_DRIPSTONE to 10,
        // Basalt
        Material.BASALT to 10,
        Material.POLISHED_BASALT to 10,
        Material.SMOOTH_BASALT to 10,
        // Blackstone
        Material.BLACKSTONE to 15,
        Material.POLISHED_BLACKSTONE to 15,
        Material.GILDED_BLACKSTONE to 25,
        // Nether/End
        Material.NETHERRACK to 5,
        Material.END_STONE to 15,
        // Obsidian (hard)
        Material.OBSIDIAN to 40,
        Material.CRYING_OBSIDIAN to 45,
        // Bricks and processed
        Material.STONE_BRICKS to 5,
        Material.MOSSY_STONE_BRICKS to 5,
        Material.CRACKED_STONE_BRICKS to 5,
        Material.CHISELED_STONE_BRICKS to 5,
        Material.DEEPSLATE_BRICKS to 10,
        Material.DEEPSLATE_TILES to 10,
        Material.POLISHED_DEEPSLATE to 10,
        Material.CHISELED_DEEPSLATE to 10,
        // Sandstone
        Material.SANDSTONE to 5,
        Material.RED_SANDSTONE to 5,
        // Prismarine
        Material.PRISMARINE to 15,
        Material.PRISMARINE_BRICKS to 15,
        Material.DARK_PRISMARINE to 15,
        // Purpur
        Material.PURPUR_BLOCK to 20,
        Material.PURPUR_PILLAR to 20,
        // End stone bricks
        Material.END_STONE_BRICKS to 15
    )

    // Combined mining difficulties (ores + stone)
    private val miningDifficulties = oreDifficulties + stoneDifficulties

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

    // Digging difficulties (soft blocks for shovels)
    private val diggingDifficulties = mapOf(
        // Basic soft blocks (very easy)
        Material.DIRT to 3,
        Material.GRASS_BLOCK to 3,
        Material.COARSE_DIRT to 3,
        Material.DIRT_PATH to 3,
        Material.FARMLAND to 3,
        // Slightly harder dirt variants
        Material.ROOTED_DIRT to 5,
        Material.PODZOL to 5,
        Material.MUD to 5,
        Material.MYCELIUM to 10,
        Material.MUDDY_MANGROVE_ROOTS to 10,
        // Sand variants (easy)
        Material.SAND to 3,
        Material.RED_SAND to 3,
        Material.SUSPICIOUS_SAND to 20,
        Material.SUSPICIOUS_GRAVEL to 20,
        // Gravel
        Material.GRAVEL to 5,
        // Clay
        Material.CLAY to 10,
        // Soul blocks (harder - Nether)
        Material.SOUL_SAND to 15,
        Material.SOUL_SOIL to 15,
        // Snow (easy)
        Material.SNOW to 3,
        Material.SNOW_BLOCK to 5,
        Material.POWDER_SNOW to 10,
        // Concrete powder
        Material.WHITE_CONCRETE_POWDER to 5,
        Material.ORANGE_CONCRETE_POWDER to 5,
        Material.MAGENTA_CONCRETE_POWDER to 5,
        Material.LIGHT_BLUE_CONCRETE_POWDER to 5,
        Material.YELLOW_CONCRETE_POWDER to 5,
        Material.LIME_CONCRETE_POWDER to 5,
        Material.PINK_CONCRETE_POWDER to 5,
        Material.GRAY_CONCRETE_POWDER to 5,
        Material.LIGHT_GRAY_CONCRETE_POWDER to 5,
        Material.CYAN_CONCRETE_POWDER to 5,
        Material.PURPLE_CONCRETE_POWDER to 5,
        Material.BLUE_CONCRETE_POWDER to 5,
        Material.BROWN_CONCRETE_POWDER to 5,
        Material.GREEN_CONCRETE_POWDER to 5,
        Material.RED_CONCRETE_POWDER to 5,
        Material.BLACK_CONCRETE_POWDER to 5
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

    fun getDiggingDifficulty(material: Material): Int = diggingDifficulties[material] ?: 5

    /** Check if block is mineable with pickaxe (ores + stone) */
    fun isMineable(material: Material): Boolean = miningDifficulties.containsKey(material)

    /** Check if block is an ore (gives bonus drops) */
    fun isOre(material: Material): Boolean = oreDifficulties.containsKey(material)

    fun isDiggable(material: Material): Boolean = diggingDifficulties.containsKey(material)

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
