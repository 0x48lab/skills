package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material

object CraftDifficulty {
    data class CraftInfo(
        val skill: SkillType,
        val difficulty: Int
    )

    private val craftInfoMap: Map<Material, CraftInfo> = buildMap {
        // Blacksmithy - Stone tools
        put(Material.STONE_SWORD, CraftInfo(SkillType.BLACKSMITHY, 20))
        put(Material.STONE_AXE, CraftInfo(SkillType.BLACKSMITHY, 20))
        put(Material.STONE_PICKAXE, CraftInfo(SkillType.BLACKSMITHY, 20))
        put(Material.STONE_SHOVEL, CraftInfo(SkillType.BLACKSMITHY, 20))
        put(Material.STONE_HOE, CraftInfo(SkillType.BLACKSMITHY, 20))

        // Blacksmithy - Iron
        put(Material.IRON_SWORD, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_AXE, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_PICKAXE, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_SHOVEL, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_HOE, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_HELMET, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_CHESTPLATE, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_LEGGINGS, CraftInfo(SkillType.BLACKSMITHY, 40))
        put(Material.IRON_BOOTS, CraftInfo(SkillType.BLACKSMITHY, 40))

        // Blacksmithy - Gold
        put(Material.GOLDEN_SWORD, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_AXE, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_PICKAXE, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_SHOVEL, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_HOE, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_HELMET, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_CHESTPLATE, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_LEGGINGS, CraftInfo(SkillType.BLACKSMITHY, 35))
        put(Material.GOLDEN_BOOTS, CraftInfo(SkillType.BLACKSMITHY, 35))

        // Blacksmithy - Diamond
        put(Material.DIAMOND_SWORD, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_AXE, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_PICKAXE, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_SHOVEL, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_HOE, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_HELMET, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_CHESTPLATE, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_LEGGINGS, CraftInfo(SkillType.BLACKSMITHY, 60))
        put(Material.DIAMOND_BOOTS, CraftInfo(SkillType.BLACKSMITHY, 60))

        // Blacksmithy - Netherite
        put(Material.NETHERITE_SWORD, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_AXE, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_PICKAXE, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_SHOVEL, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_HOE, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_HELMET, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_CHESTPLATE, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_LEGGINGS, CraftInfo(SkillType.BLACKSMITHY, 80))
        put(Material.NETHERITE_BOOTS, CraftInfo(SkillType.BLACKSMITHY, 80))

        // Blacksmithy - Chain armor
        put(Material.CHAINMAIL_HELMET, CraftInfo(SkillType.BLACKSMITHY, 45))
        put(Material.CHAINMAIL_CHESTPLATE, CraftInfo(SkillType.BLACKSMITHY, 45))
        put(Material.CHAINMAIL_LEGGINGS, CraftInfo(SkillType.BLACKSMITHY, 45))
        put(Material.CHAINMAIL_BOOTS, CraftInfo(SkillType.BLACKSMITHY, 45))

        // Blacksmithy - Other
        put(Material.ANVIL, CraftInfo(SkillType.BLACKSMITHY, 50))
        put(Material.MACE, CraftInfo(SkillType.BLACKSMITHY, 70))

        // Bowcraft
        put(Material.BOW, CraftInfo(SkillType.BOWCRAFT, 40))
        put(Material.CROSSBOW, CraftInfo(SkillType.BOWCRAFT, 40))
        put(Material.ARROW, CraftInfo(SkillType.BOWCRAFT, 15))
        put(Material.SPECTRAL_ARROW, CraftInfo(SkillType.BOWCRAFT, 35))
        put(Material.TIPPED_ARROW, CraftInfo(SkillType.BOWCRAFT, 35))

        // Craftsmanship - Wood
        put(Material.WOODEN_SWORD, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.WOODEN_AXE, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.WOODEN_PICKAXE, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.WOODEN_SHOVEL, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.WOODEN_HOE, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.SHIELD, CraftInfo(SkillType.CRAFTSMANSHIP, 25))
        put(Material.CHEST, CraftInfo(SkillType.CRAFTSMANSHIP, 10))
        put(Material.BARREL, CraftInfo(SkillType.CRAFTSMANSHIP, 15))
        put(Material.BOOKSHELF, CraftInfo(SkillType.CRAFTSMANSHIP, 20))
        put(Material.CRAFTING_TABLE, CraftInfo(SkillType.CRAFTSMANSHIP, 5))
        put(Material.LOOM, CraftInfo(SkillType.CRAFTSMANSHIP, 15))
        put(Material.CARTOGRAPHY_TABLE, CraftInfo(SkillType.CRAFTSMANSHIP, 15))

        // Craftsmanship - Leather
        put(Material.LEATHER_HELMET, CraftInfo(SkillType.CRAFTSMANSHIP, 25))
        put(Material.LEATHER_CHESTPLATE, CraftInfo(SkillType.CRAFTSMANSHIP, 25))
        put(Material.LEATHER_LEGGINGS, CraftInfo(SkillType.CRAFTSMANSHIP, 25))
        put(Material.LEATHER_BOOTS, CraftInfo(SkillType.CRAFTSMANSHIP, 25))

        // Tinkering - Basic
        put(Material.SHEARS, CraftInfo(SkillType.TINKERING, 15))
        put(Material.FLINT_AND_STEEL, CraftInfo(SkillType.TINKERING, 15))
        put(Material.IRON_NUGGET, CraftInfo(SkillType.TINKERING, 15))
        put(Material.GOLD_NUGGET, CraftInfo(SkillType.TINKERING, 15))

        // Tinkering - Complex
        put(Material.FISHING_ROD, CraftInfo(SkillType.TINKERING, 35))
        put(Material.LANTERN, CraftInfo(SkillType.TINKERING, 35))
        put(Material.SOUL_LANTERN, CraftInfo(SkillType.TINKERING, 35))
        put(Material.CANDLE, CraftInfo(SkillType.TINKERING, 35))
        // CHAIN is not craftable in vanilla, removed
        put(Material.GUNPOWDER, CraftInfo(SkillType.TINKERING, 35))

        // Tinkering - Precision
        put(Material.CLOCK, CraftInfo(SkillType.TINKERING, 50))
        put(Material.COMPASS, CraftInfo(SkillType.TINKERING, 50))
        put(Material.SPYGLASS, CraftInfo(SkillType.TINKERING, 50))
        put(Material.FIREWORK_ROCKET, CraftInfo(SkillType.TINKERING, 50))
    }

    fun getCraftInfo(material: Material): CraftInfo? = craftInfoMap[material]

    fun getSkillForMaterial(material: Material): SkillType? = craftInfoMap[material]?.skill

    fun getDifficulty(material: Material): Int = craftInfoMap[material]?.difficulty ?: 20
}
