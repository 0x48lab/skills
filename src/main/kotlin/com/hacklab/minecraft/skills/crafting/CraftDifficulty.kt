package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material

object CraftDifficulty {
    /**
     * Crafting information for skill gain calculation
     * @param skill The skill type used for this craft
     * @param difficulty The difficulty level (affects success rate and skill gain probability)
     * @param gainMultiplier How many skill gain attempts per craft (based on material complexity)
     *                       1 = simple (1-3 materials), 2 = medium (4-5), 3 = large (6-7), 4 = complex (8-9)
     */
    data class CraftInfo(
        val skill: SkillType,
        val difficulty: Int,
        val gainMultiplier: Int = 1
    )

    private val craftInfoMap: Map<Material, CraftInfo> = buildMap {
        // Blacksmithy - Stone tools (difficulty aligned with stone mining: 5)
        // Sword: 2 stone + 1 stick = 3 materials, Pickaxe/Axe: 3+2 = 5, Shovel: 1+2 = 3, Hoe: 2+2 = 4
        put(Material.STONE_SWORD, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.STONE_AXE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.STONE_PICKAXE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.STONE_SHOVEL, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.STONE_HOE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))

        // Blacksmithy - Iron (difficulty aligned with iron ore mining: 25)
        // Helmet: 5, Chestplate: 8, Leggings: 7, Boots: 4
        put(Material.IRON_SWORD, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.IRON_AXE, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))
        put(Material.IRON_PICKAXE, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))
        put(Material.IRON_SHOVEL, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.IRON_HOE, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))
        put(Material.IRON_HELMET, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))
        put(Material.IRON_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        put(Material.IRON_LEGGINGS, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 3))
        put(Material.IRON_BOOTS, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))

        // Blacksmithy - Gold (slightly easier than iron)
        put(Material.GOLDEN_SWORD, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.GOLDEN_AXE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.GOLDEN_PICKAXE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.GOLDEN_SHOVEL, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.GOLDEN_HOE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.GOLDEN_HELMET, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.GOLDEN_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 4))
        put(Material.GOLDEN_LEGGINGS, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 3))
        put(Material.GOLDEN_BOOTS, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))

        // Blacksmithy - Diamond (difficulty aligned with diamond ore mining: 60 -> 40)
        put(Material.DIAMOND_SWORD, CraftInfo(SkillType.CRAFTING, 40))
        put(Material.DIAMOND_AXE, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))
        put(Material.DIAMOND_PICKAXE, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))
        put(Material.DIAMOND_SHOVEL, CraftInfo(SkillType.CRAFTING, 40))
        put(Material.DIAMOND_HOE, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))
        put(Material.DIAMOND_HELMET, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))
        put(Material.DIAMOND_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 4))
        put(Material.DIAMOND_LEGGINGS, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 3))
        put(Material.DIAMOND_BOOTS, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))

        // Blacksmithy - Netherite (high-end crafting - smithing table upgrade, 2 materials)
        put(Material.NETHERITE_SWORD, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_AXE, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_PICKAXE, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_SHOVEL, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_HOE, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_HELMET, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_LEGGINGS, CraftInfo(SkillType.CRAFTING, 60))
        put(Material.NETHERITE_BOOTS, CraftInfo(SkillType.CRAFTING, 60))

        // Blacksmithy - Chain armor (same pattern as other armor)
        put(Material.CHAINMAIL_HELMET, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 2))
        put(Material.CHAINMAIL_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 4))
        put(Material.CHAINMAIL_LEGGINGS, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 3))
        put(Material.CHAINMAIL_BOOTS, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 2))

        // Blacksmithy - Other
        // Anvil: 3 iron blocks + 4 iron = complex (31 iron total!) -> multiplier 4
        put(Material.ANVIL, CraftInfo(SkillType.CRAFTING, 35, gainMultiplier = 4))
        // Mace: breeze rod + heavy core = 2 materials
        put(Material.MACE, CraftInfo(SkillType.CRAFTING, 50))

        // Bows and Arrows (now part of Craftsmanship)
        // Bow: 3 sticks + 3 string = 6 materials
        put(Material.BOW, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 3))
        // Crossbow: 3 sticks + 2 string + 1 iron + 1 tripwire hook = 7 materials
        put(Material.CROSSBOW, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 3))
        // Arrow: 1 flint + 1 stick + 1 feather = 3 materials -> 4 arrows, so per-craft is cheap
        put(Material.ARROW, CraftInfo(SkillType.CRAFTING, 5))
        // Spectral Arrow: 4 glowstone + 1 arrow = 5 materials -> 2 arrows
        put(Material.SPECTRAL_ARROW, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        // Tipped Arrow: 8 arrows + 1 lingering potion = 9 materials -> 8 arrows
        put(Material.TIPPED_ARROW, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))

        // Craftsmanship - Basic wood items (very easy, entry level)
        put(Material.STICK, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.WOODEN_SWORD, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.WOODEN_AXE, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 2))
        put(Material.WOODEN_PICKAXE, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 2))
        put(Material.WOODEN_SHOVEL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.WOODEN_HOE, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 2))
        // Shield: 6 planks + 1 iron = 7 materials
        put(Material.SHIELD, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 3))
        // Chest: 8 planks -> multiplier 4
        put(Material.CHEST, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 4))
        put(Material.TRAPPED_CHEST, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 4))
        // Barrel: 6 planks + 2 slabs = 8 -> multiplier 4
        put(Material.BARREL, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 4))
        // Bookshelf: 6 planks + 3 books = 9 -> multiplier 4
        put(Material.BOOKSHELF, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 4))
        // Chiseled Bookshelf: 6 planks + 3 slabs = 9 -> multiplier 4
        put(Material.CHISELED_BOOKSHELF, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))
        put(Material.CRAFTING_TABLE, CraftInfo(SkillType.CRAFTING, 3, gainMultiplier = 2))
        put(Material.LOOM, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 2))
        // Cartography Table: 4 planks + 2 paper = 6 -> multiplier 3
        put(Material.CARTOGRAPHY_TABLE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        // Fletching Table: 4 planks + 2 flint = 6 -> multiplier 3
        put(Material.FLETCHING_TABLE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        // Smithing Table: 4 planks + 2 iron = 6 -> multiplier 3
        put(Material.SMITHING_TABLE, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        // Composter: 7 slabs -> multiplier 3
        put(Material.COMPOSTER, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 3))
        // Lectern: 4 slabs + 1 bookshelf = 5 (but bookshelf is complex)
        put(Material.LECTERN, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))
        // Jukebox: 8 planks + 1 diamond = 9 -> multiplier 4
        put(Material.JUKEBOX, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 4))
        // Note Block: 8 planks + 1 redstone = 9 -> multiplier 4
        put(Material.NOTE_BLOCK, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 4))
        // Ladder: 7 sticks -> multiplier 3
        put(Material.LADDER, CraftInfo(SkillType.CRAFTING, 3, gainMultiplier = 3))
        put(Material.BOWL, CraftInfo(SkillType.CRAFTING, 2))
        // Item Frame: 8 sticks + 1 leather = 9 -> multiplier 4
        put(Material.ITEM_FRAME, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 4))
        put(Material.GLOW_ITEM_FRAME, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 4))
        // Painting: 8 sticks + 1 wool = 9 -> multiplier 4
        put(Material.PAINTING, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 4))
        // Armor Stand: 6 sticks + 1 smooth stone slab = 7 -> multiplier 3
        put(Material.ARMOR_STAND, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 3))

        // Craftsmanship - Stairs (all wood types)
        put(Material.OAK_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.SPRUCE_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BIRCH_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.JUNGLE_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.ACACIA_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.DARK_OAK_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.CRIMSON_STAIRS, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.WARPED_STAIRS, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.MANGROVE_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.CHERRY_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BAMBOO_STAIRS, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BAMBOO_MOSAIC_STAIRS, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Slabs (all wood types)
        put(Material.OAK_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.SPRUCE_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BIRCH_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.JUNGLE_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.ACACIA_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.DARK_OAK_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CRIMSON_SLAB, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.WARPED_SLAB, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.MANGROVE_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CHERRY_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BAMBOO_SLAB, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BAMBOO_MOSAIC_SLAB, CraftInfo(SkillType.CRAFTING, 3))

        // Craftsmanship - Doors (all wood types)
        put(Material.OAK_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.SPRUCE_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BIRCH_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.JUNGLE_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.ACACIA_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.DARK_OAK_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CRIMSON_DOOR, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.WARPED_DOOR, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.MANGROVE_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CHERRY_DOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BAMBOO_DOOR, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Trapdoors (all wood types)
        put(Material.OAK_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.SPRUCE_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BIRCH_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.JUNGLE_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.ACACIA_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.DARK_OAK_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CRIMSON_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.WARPED_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.MANGROVE_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CHERRY_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BAMBOO_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Fences (all wood types)
        put(Material.OAK_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.SPRUCE_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.BIRCH_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.JUNGLE_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.ACACIA_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.DARK_OAK_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.CRIMSON_FENCE, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.WARPED_FENCE, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.MANGROVE_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.CHERRY_FENCE, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.BAMBOO_FENCE, CraftInfo(SkillType.CRAFTING, 4))

        // Craftsmanship - Fence Gates (all wood types)
        put(Material.OAK_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.SPRUCE_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BIRCH_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.JUNGLE_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.ACACIA_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.DARK_OAK_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CRIMSON_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 7))
        put(Material.WARPED_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 7))
        put(Material.MANGROVE_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CHERRY_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BAMBOO_FENCE_GATE, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Signs (all wood types)
        put(Material.OAK_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.SPRUCE_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.BIRCH_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.JUNGLE_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.ACACIA_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.DARK_OAK_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.CRIMSON_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.WARPED_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.MANGROVE_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.CHERRY_SIGN, CraftInfo(SkillType.CRAFTING, 4))
        put(Material.BAMBOO_SIGN, CraftInfo(SkillType.CRAFTING, 4))

        // Craftsmanship - Hanging Signs (all wood types)
        put(Material.OAK_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.SPRUCE_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.BIRCH_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.JUNGLE_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.ACACIA_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.DARK_OAK_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.CRIMSON_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.WARPED_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.MANGROVE_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.CHERRY_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.BAMBOO_HANGING_SIGN, CraftInfo(SkillType.CRAFTING, 6))

        // Craftsmanship - Pressure Plates (wood)
        put(Material.OAK_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.SPRUCE_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BIRCH_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.JUNGLE_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.ACACIA_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.DARK_OAK_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.CRIMSON_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.WARPED_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.MANGROVE_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.CHERRY_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BAMBOO_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 3))

        // Craftsmanship - Buttons (wood)
        put(Material.OAK_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.SPRUCE_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BIRCH_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.JUNGLE_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.ACACIA_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.DARK_OAK_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CRIMSON_BUTTON, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.WARPED_BUTTON, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.MANGROVE_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CHERRY_BUTTON, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BAMBOO_BUTTON, CraftInfo(SkillType.CRAFTING, 2))

        // Craftsmanship - Boats (all wood types) - 5 planks -> multiplier 2
        put(Material.OAK_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.SPRUCE_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.BIRCH_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.JUNGLE_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.ACACIA_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.DARK_OAK_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.MANGROVE_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.CHERRY_BOAT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        put(Material.BAMBOO_RAFT, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 2))
        // Chest boats: boat + chest (complex item) -> multiplier 3
        put(Material.OAK_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.SPRUCE_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.BIRCH_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.JUNGLE_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.ACACIA_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.DARK_OAK_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.MANGROVE_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.CHERRY_CHEST_BOAT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))
        put(Material.BAMBOO_CHEST_RAFT, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 3))

        // Craftsmanship - Beds (all colors) - 3 wool + 3 planks = 6 -> multiplier 3
        put(Material.WHITE_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.ORANGE_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.MAGENTA_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.LIGHT_BLUE_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.YELLOW_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.LIME_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.PINK_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.GRAY_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.LIGHT_GRAY_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.CYAN_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.PURPLE_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.BLUE_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.BROWN_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.GREEN_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.RED_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))
        put(Material.BLACK_BED, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 3))

        // Craftsmanship - Banners (all colors) - 6 wool + 1 stick = 7 -> multiplier 3
        put(Material.WHITE_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.ORANGE_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.MAGENTA_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.LIGHT_BLUE_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.YELLOW_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.LIME_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.PINK_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.GRAY_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.LIGHT_GRAY_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.CYAN_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.PURPLE_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.BLUE_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.BROWN_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.GREEN_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.RED_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))
        put(Material.BLACK_BANNER, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 3))

        // Craftsmanship - Carpets (all colors)
        put(Material.WHITE_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.ORANGE_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.MAGENTA_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.LIGHT_BLUE_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.YELLOW_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.LIME_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.PINK_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.GRAY_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.LIGHT_GRAY_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CYAN_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.PURPLE_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BLUE_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BROWN_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.GREEN_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.RED_CARPET, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BLACK_CARPET, CraftInfo(SkillType.CRAFTING, 2))

        // Craftsmanship - Leather (same pattern as metal armor: helmet 5, chest 8, legs 7, boots 4)
        put(Material.LEATHER_HELMET, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))
        put(Material.LEATHER_CHESTPLATE, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))
        put(Material.LEATHER_LEGGINGS, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 3))
        put(Material.LEATHER_BOOTS, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))

        // Craftsmanship - Tools & Gadgets (formerly Tinkering)
        put(Material.SHEARS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.FLINT_AND_STEEL, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.IRON_NUGGET, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.GOLD_NUGGET, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Complex items
        put(Material.FISHING_ROD, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.LANTERN, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.SOUL_LANTERN, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.GUNPOWDER, CraftInfo(SkillType.CRAFTING, 20))

        // Craftsmanship - Precision items
        put(Material.CLOCK, CraftInfo(SkillType.CRAFTING, 35))
        put(Material.COMPASS, CraftInfo(SkillType.CRAFTING, 35))
        put(Material.SPYGLASS, CraftInfo(SkillType.CRAFTING, 35))
        put(Material.FIREWORK_ROCKET, CraftInfo(SkillType.CRAFTING, 30))

        // Craftsmanship - Mud and Clay (pottery work)
        put(Material.PACKED_MUD, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.MUD_BRICKS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.MUD_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.MUD_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.MUD_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.BRICK, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.BRICKS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.BRICK_WALL, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.FLOWER_POT, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.DECORATED_POT, CraftInfo(SkillType.CRAFTING, 25))

        // Craftsmanship - Glass items
        put(Material.GLASS_PANE, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.WHITE_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.ORANGE_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.MAGENTA_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIGHT_BLUE_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.YELLOW_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIME_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.PINK_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.GRAY_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIGHT_GRAY_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CYAN_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.PURPLE_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BLUE_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BROWN_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.GREEN_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.RED_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BLACK_STAINED_GLASS_PANE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.GLASS_BOTTLE, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.TINTED_GLASS, CraftInfo(SkillType.CRAFTING, 15))

        // Craftsmanship - Terracotta (glazed)
        put(Material.WHITE_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.ORANGE_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.MAGENTA_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.YELLOW_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.LIME_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.PINK_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.GRAY_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.LIGHT_GRAY_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.CYAN_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.PURPLE_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.BLUE_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.BROWN_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.GREEN_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.RED_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.BLACK_GLAZED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 20))

        // Craftsmanship - Concrete powder (all colors)
        put(Material.WHITE_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.ORANGE_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.MAGENTA_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIGHT_BLUE_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.YELLOW_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIME_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.PINK_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.GRAY_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIGHT_GRAY_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.CYAN_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.PURPLE_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BLUE_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BROWN_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.GREEN_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.RED_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BLACK_CONCRETE_POWDER, CraftInfo(SkillType.CRAFTING, 12))

        // Craftsmanship - Redstone components (mechanical tinkering)
        // Piston: 4 cobblestone + 3 planks + 1 iron + 1 redstone = 9 -> multiplier 4
        put(Material.PISTON, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        put(Material.STICKY_PISTON, CraftInfo(SkillType.CRAFTING, 28, gainMultiplier = 4))
        // Dispenser: 7 cobblestone + 1 bow + 1 redstone = 9 -> multiplier 4
        put(Material.DISPENSER, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        // Dropper: 7 cobblestone + 1 redstone = 8 -> multiplier 4
        put(Material.DROPPER, CraftInfo(SkillType.CRAFTING, 22, gainMultiplier = 4))
        // Observer: 6 cobblestone + 2 redstone + 1 quartz = 9 -> multiplier 4
        put(Material.OBSERVER, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 4))
        // Hopper: 5 iron + 1 chest = 6 (but chest is complex) -> multiplier 3
        put(Material.HOPPER, CraftInfo(SkillType.CRAFTING, 28, gainMultiplier = 3))
        put(Material.REPEATER, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.COMPARATOR, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 2))
        put(Material.LEVER, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.TRIPWIRE_HOOK, CraftInfo(SkillType.CRAFTING, 12))
        // Daylight Detector: 3 glass + 3 quartz + 3 slabs = 9 -> multiplier 4
        put(Material.DAYLIGHT_DETECTOR, CraftInfo(SkillType.CRAFTING, 30, gainMultiplier = 4))
        // Target: 4 redstone + 1 hay bale = 5 (hay bale is 9 wheat) -> multiplier 2
        put(Material.TARGET, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        put(Material.REDSTONE_TORCH, CraftInfo(SkillType.CRAFTING, 8))
        // Redstone Lamp: 4 redstone + 1 glowstone = 5 -> multiplier 2
        put(Material.REDSTONE_LAMP, CraftInfo(SkillType.CRAFTING, 18, gainMultiplier = 2))
        // TNT: 5 gunpowder + 4 sand = 9 -> multiplier 4
        put(Material.TNT, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        put(Material.SCULK_SENSOR, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 2))
        put(Material.CALIBRATED_SCULK_SENSOR, CraftInfo(SkillType.CRAFTING, 45, gainMultiplier = 2))

        // Craftsmanship - Rails
        // Rail: 6 iron + 1 stick = 7 but makes 16 rails, so per-rail cost is low
        put(Material.RAIL, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))
        // Powered Rail: 6 gold + 1 stick + 1 redstone = 8 but makes 6 rails
        put(Material.POWERED_RAIL, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 3))
        // Detector Rail: 6 iron + 1 stone plate + 1 redstone = 8 but makes 6 rails
        put(Material.DETECTOR_RAIL, CraftInfo(SkillType.CRAFTING, 22, gainMultiplier = 3))
        // Activator Rail: 6 iron + 2 sticks + 1 redstone torch = 9 but makes 6 rails
        put(Material.ACTIVATOR_RAIL, CraftInfo(SkillType.CRAFTING, 22, gainMultiplier = 3))
        // Minecart: 5 iron -> multiplier 2
        put(Material.MINECART, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        // Chest/Furnace Minecart: minecart + chest/furnace -> multiplier 3
        put(Material.CHEST_MINECART, CraftInfo(SkillType.CRAFTING, 22, gainMultiplier = 3))
        put(Material.FURNACE_MINECART, CraftInfo(SkillType.CRAFTING, 22, gainMultiplier = 3))
        put(Material.HOPPER_MINECART, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        put(Material.TNT_MINECART, CraftInfo(SkillType.CRAFTING, 28, gainMultiplier = 4))

        // Blacksmithy - Stone building blocks (masonry)
        put(Material.STONE_BRICKS, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.STONE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.STONE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.STONE_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CHISELED_STONE_BRICKS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.MOSSY_STONE_BRICKS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.MOSSY_STONE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.MOSSY_STONE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.MOSSY_STONE_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.COBBLESTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.COBBLESTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.COBBLESTONE_WALL, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.MOSSY_COBBLESTONE_SLAB, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.MOSSY_COBBLESTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.MOSSY_COBBLESTONE_WALL, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.SMOOTH_STONE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.SMOOTH_STONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.STONECUTTER, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))
        // Furnace: 8 cobblestone -> multiplier 4
        put(Material.FURNACE, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 4))
        // Blast Furnace: 5 iron + 3 smooth stone + 1 furnace = 9 -> multiplier 4
        put(Material.BLAST_FURNACE, CraftInfo(SkillType.CRAFTING, 25, gainMultiplier = 4))
        // Smoker: 4 logs + 1 furnace = 5 -> multiplier 2
        put(Material.SMOKER, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))
        // Grindstone: 2 sticks + 1 stone slab + 2 planks = 5 -> multiplier 2
        put(Material.GRINDSTONE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))

        // Blacksmithy - Polished stone variants
        put(Material.POLISHED_ANDESITE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.POLISHED_ANDESITE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.POLISHED_ANDESITE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.POLISHED_DIORITE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.POLISHED_DIORITE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.POLISHED_DIORITE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.POLISHED_GRANITE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.POLISHED_GRANITE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.POLISHED_GRANITE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))

        // Blacksmithy - Deepslate variants
        put(Material.POLISHED_DEEPSLATE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.POLISHED_DEEPSLATE_SLAB, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.POLISHED_DEEPSLATE_STAIRS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.POLISHED_DEEPSLATE_WALL, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.DEEPSLATE_BRICKS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.DEEPSLATE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.DEEPSLATE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.DEEPSLATE_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.DEEPSLATE_TILES, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.DEEPSLATE_TILE_SLAB, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.DEEPSLATE_TILE_STAIRS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.DEEPSLATE_TILE_WALL, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.CHISELED_DEEPSLATE, CraftInfo(SkillType.CRAFTING, 25))

        // Blacksmithy - Nether bricks
        put(Material.NETHER_BRICKS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.NETHER_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.NETHER_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.NETHER_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.NETHER_BRICK_FENCE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.RED_NETHER_BRICKS, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.RED_NETHER_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.RED_NETHER_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.RED_NETHER_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.CHISELED_NETHER_BRICKS, CraftInfo(SkillType.CRAFTING, 25))

        // Blacksmithy - End stone
        put(Material.END_STONE_BRICKS, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.END_STONE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.END_STONE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.END_STONE_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 22))

        // Blacksmithy - Quartz
        put(Material.QUARTZ_BLOCK, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.QUARTZ_SLAB, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.QUARTZ_STAIRS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.QUARTZ_PILLAR, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.CHISELED_QUARTZ_BLOCK, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.QUARTZ_BRICKS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.SMOOTH_QUARTZ, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.SMOOTH_QUARTZ_SLAB, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.SMOOTH_QUARTZ_STAIRS, CraftInfo(SkillType.CRAFTING, 15))

        // Blacksmithy - Prismarine
        put(Material.PRISMARINE, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.PRISMARINE_SLAB, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.PRISMARINE_STAIRS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.PRISMARINE_WALL, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.PRISMARINE_BRICKS, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.PRISMARINE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.PRISMARINE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.DARK_PRISMARINE, CraftInfo(SkillType.CRAFTING, 28))
        put(Material.DARK_PRISMARINE_SLAB, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.DARK_PRISMARINE_STAIRS, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.SEA_LANTERN, CraftInfo(SkillType.CRAFTING, 25))

        // Blacksmithy - Blackstone
        put(Material.POLISHED_BLACKSTONE, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.POLISHED_BLACKSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.POLISHED_BLACKSTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.POLISHED_BLACKSTONE_WALL, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.POLISHED_BLACKSTONE_BRICKS, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.POLISHED_BLACKSTONE_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.POLISHED_BLACKSTONE_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.POLISHED_BLACKSTONE_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.CHISELED_POLISHED_BLACKSTONE, CraftInfo(SkillType.CRAFTING, 28))
        put(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.POLISHED_BLACKSTONE_BUTTON, CraftInfo(SkillType.CRAFTING, 15))

        // Blacksmithy - Iron/Metal items
        put(Material.IRON_BARS, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.IRON_DOOR, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.IRON_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.CAULDRON, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.LIGHTNING_ROD, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.BELL, CraftInfo(SkillType.CRAFTING, 35))
        put(Material.BREWING_STAND, CraftInfo(SkillType.CRAFTING, 30))

        // Blacksmithy - Copper items
        put(Material.CUT_COPPER, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.CUT_COPPER_SLAB, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.CUT_COPPER_STAIRS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.COPPER_BLOCK, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.COPPER_GRATE, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.COPPER_BULB, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.COPPER_DOOR, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.COPPER_TRAPDOOR, CraftInfo(SkillType.CRAFTING, 22))
        put(Material.CHISELED_COPPER, CraftInfo(SkillType.CRAFTING, 22))

        // Blacksmithy - Waxed copper (same difficulty as unwaxed)
        put(Material.WAXED_CUT_COPPER, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.WAXED_CUT_COPPER_SLAB, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.WAXED_CUT_COPPER_STAIRS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.WAXED_COPPER_BLOCK, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.WAXED_COPPER_GRATE, CraftInfo(SkillType.CRAFTING, 20))
        put(Material.WAXED_COPPER_BULB, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.WAXED_CHISELED_COPPER, CraftInfo(SkillType.CRAFTING, 22))

        // Blacksmithy - Sandstone variants
        put(Material.SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SANDSTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SANDSTONE_WALL, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.CHISELED_SANDSTONE, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.CUT_SANDSTONE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CUT_SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SMOOTH_SANDSTONE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.SMOOTH_SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SMOOTH_SANDSTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.RED_SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.RED_SANDSTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.RED_SANDSTONE_WALL, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.CHISELED_RED_SANDSTONE, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.CUT_RED_SANDSTONE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CUT_RED_SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SMOOTH_RED_SANDSTONE, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.SMOOTH_RED_SANDSTONE_SLAB, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.SMOOTH_RED_SANDSTONE_STAIRS, CraftInfo(SkillType.CRAFTING, 8))

        // Blacksmithy - Purpur
        put(Material.PURPUR_BLOCK, CraftInfo(SkillType.CRAFTING, 28))
        put(Material.PURPUR_PILLAR, CraftInfo(SkillType.CRAFTING, 28))
        put(Material.PURPUR_SLAB, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.PURPUR_STAIRS, CraftInfo(SkillType.CRAFTING, 25))
        put(Material.END_ROD, CraftInfo(SkillType.CRAFTING, 25))

        // Blacksmithy - Tuff variants
        put(Material.TUFF_BRICKS, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.TUFF_BRICK_SLAB, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.TUFF_BRICK_STAIRS, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.TUFF_BRICK_WALL, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.POLISHED_TUFF, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.POLISHED_TUFF_SLAB, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.POLISHED_TUFF_STAIRS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.POLISHED_TUFF_WALL, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CHISELED_TUFF, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.CHISELED_TUFF_BRICKS, CraftInfo(SkillType.CRAFTING, 20))

        // Craftsmanship - Paper/Book items
        put(Material.PAPER, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BOOK, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.WRITABLE_BOOK, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.MAP, CraftInfo(SkillType.CRAFTING, 15))

        // Craftsmanship - Miscellaneous
        put(Material.TORCH, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.SOUL_TORCH, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.LEAD, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.NAME_TAG, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.SCAFFOLDING, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.BUNDLE, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.CAMPFIRE, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.SOUL_CAMPFIRE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.BEEHIVE, CraftInfo(SkillType.CRAFTING, 18))
        put(Material.BRUSH, CraftInfo(SkillType.CRAFTING, 15))

        // Craftsmanship - Wool (all colors)
        put(Material.WHITE_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.ORANGE_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.MAGENTA_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.LIGHT_BLUE_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.YELLOW_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.LIME_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.PINK_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.GRAY_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.LIGHT_GRAY_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.CYAN_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.PURPLE_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BLUE_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BROWN_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.GREEN_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.RED_WOOL, CraftInfo(SkillType.CRAFTING, 5))
        put(Material.BLACK_WOOL, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Colored candles (all colors)
        put(Material.WHITE_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.ORANGE_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.MAGENTA_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.LIGHT_BLUE_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.YELLOW_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.LIME_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.PINK_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.GRAY_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.LIGHT_GRAY_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.CYAN_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.PURPLE_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.BLUE_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.BROWN_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.GREEN_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.RED_CANDLE, CraftInfo(SkillType.CRAFTING, 15))
        put(Material.BLACK_CANDLE, CraftInfo(SkillType.CRAFTING, 15))

        // Craftsmanship - Shulker boxes (all colors + default)
        put(Material.SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.WHITE_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.ORANGE_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.MAGENTA_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.LIGHT_BLUE_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.YELLOW_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.LIME_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.PINK_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.GRAY_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.LIGHT_GRAY_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.CYAN_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.PURPLE_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.BLUE_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.BROWN_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.GREEN_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.RED_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))
        put(Material.BLACK_SHULKER_BOX, CraftInfo(SkillType.CRAFTING, 30))

        // Craftsmanship - Stained glass blocks (all colors)
        put(Material.WHITE_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.ORANGE_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.MAGENTA_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIGHT_BLUE_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.YELLOW_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIME_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.PINK_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.GRAY_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.LIGHT_GRAY_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.CYAN_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.PURPLE_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BLUE_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BROWN_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.GREEN_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.RED_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))
        put(Material.BLACK_STAINED_GLASS, CraftInfo(SkillType.CRAFTING, 10))

        // Craftsmanship - Colored terracotta (all colors)
        put(Material.WHITE_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.ORANGE_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.MAGENTA_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIGHT_BLUE_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.YELLOW_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIME_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.PINK_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.GRAY_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.LIGHT_GRAY_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.CYAN_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.PURPLE_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BLUE_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BROWN_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.GREEN_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.RED_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))
        put(Material.BLACK_TERRACOTTA, CraftInfo(SkillType.CRAFTING, 12))

        // Blacksmithy - High-tier utility blocks
        put(Material.ENDER_CHEST, CraftInfo(SkillType.CRAFTING, 50, gainMultiplier = 4))       // 8 obsidian + 1 eye
        put(Material.ENCHANTING_TABLE, CraftInfo(SkillType.CRAFTING, 55, gainMultiplier = 3)) // 4 obsidian + 2 diamonds + 1 book
        put(Material.BEACON, CraftInfo(SkillType.CRAFTING, 80, gainMultiplier = 4))           // 5 glass + 3 obsidian + 1 nether star
        put(Material.RESPAWN_ANCHOR, CraftInfo(SkillType.CRAFTING, 60, gainMultiplier = 4))   // 6 crying obsidian + 3 glowstone
        put(Material.LODESTONE, CraftInfo(SkillType.CRAFTING, 55, gainMultiplier = 4))        // 8 chiseled stone + 1 netherite
        put(Material.CONDUIT, CraftInfo(SkillType.CRAFTING, 65, gainMultiplier = 4))          // 8 nautilus shells + 1 heart

        // Craftsmanship - Compressed blocks
        put(Material.SLIME_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))       // 9 slime balls
        put(Material.HONEY_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))       // 4 honey bottles
        put(Material.HONEYCOMB_BLOCK, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 2))   // 4 honeycomb
        put(Material.HAY_BLOCK, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 4))          // 9 wheat
        put(Material.DRIED_KELP_BLOCK, CraftInfo(SkillType.CRAFTING, 8, gainMultiplier = 4))   // 9 dried kelp
        put(Material.BONE_BLOCK, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 4))        // 9 bone meal
        put(Material.MELON, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 4))             // 9 melon slices
        put(Material.SNOW_BLOCK, CraftInfo(SkillType.CRAFTING, 5, gainMultiplier = 2))         // 4 snowballs
        put(Material.MAGMA_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 2))       // 4 magma cream
        put(Material.PACKED_ICE, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))        // 9 ice
        put(Material.BLUE_ICE, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 4))          // 9 packed ice
        put(Material.MOSS_BLOCK, CraftInfo(SkillType.CRAFTING, 8))                              // not craftable (spread)
        put(Material.NETHER_WART_BLOCK, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 4)) // 9 nether wart
        put(Material.WARPED_WART_BLOCK, CraftInfo(SkillType.CRAFTING, 12, gainMultiplier = 4)) // 9 warped wart

        // Craftsmanship - Pumpkin items
        put(Material.CARVED_PUMPKIN, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.JACK_O_LANTERN, CraftInfo(SkillType.CRAFTING, 10))

        // Alchemy - Ingredients (for skill gain when crafting reagents)
        put(Material.FERMENTED_SPIDER_EYE, CraftInfo(SkillType.COOKING, 25))
        put(Material.GLISTERING_MELON_SLICE, CraftInfo(SkillType.COOKING, 20))
        put(Material.MAGMA_CREAM, CraftInfo(SkillType.COOKING, 25))
        put(Material.BLAZE_POWDER, CraftInfo(SkillType.COOKING, 20))
        put(Material.FIRE_CHARGE, CraftInfo(SkillType.COOKING, 22))
        put(Material.ENDER_EYE, CraftInfo(SkillType.COOKING, 35))

        // Cooking - Food items
        put(Material.SUGAR, CraftInfo(SkillType.COOKING, 5))
        put(Material.GOLDEN_APPLE, CraftInfo(SkillType.COOKING, 60, gainMultiplier = 4))  // 8 gold ingots + 1 apple
        put(Material.PUMPKIN_PIE, CraftInfo(SkillType.COOKING, 20))                        // 3 materials

        // Craftsmanship - Firework and misc
        put(Material.FIREWORK_STAR, CraftInfo(SkillType.CRAFTING, 25))                          // variable materials
        put(Material.END_CRYSTAL, CraftInfo(SkillType.CRAFTING, 50, gainMultiplier = 4))        // 7 glass + 1 eye + 1 ghast tear
        put(Material.RECOVERY_COMPASS, CraftInfo(SkillType.CRAFTING, 45, gainMultiplier = 4))   // 8 echo shards + 1 compass

        // Blacksmithy - Storage blocks (metal compression) - all 9 materials
        put(Material.IRON_BLOCK, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 4))
        put(Material.GOLD_BLOCK, CraftInfo(SkillType.CRAFTING, 18, gainMultiplier = 4))
        put(Material.DIAMOND_BLOCK, CraftInfo(SkillType.CRAFTING, 35, gainMultiplier = 4))
        put(Material.EMERALD_BLOCK, CraftInfo(SkillType.CRAFTING, 40, gainMultiplier = 4))
        put(Material.LAPIS_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))
        put(Material.REDSTONE_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))
        put(Material.COAL_BLOCK, CraftInfo(SkillType.CRAFTING, 10, gainMultiplier = 4))
        put(Material.NETHERITE_BLOCK, CraftInfo(SkillType.CRAFTING, 55, gainMultiplier = 4))
        put(Material.RAW_IRON_BLOCK, CraftInfo(SkillType.CRAFTING, 18, gainMultiplier = 4))
        put(Material.RAW_GOLD_BLOCK, CraftInfo(SkillType.CRAFTING, 18, gainMultiplier = 4))
        put(Material.RAW_COPPER_BLOCK, CraftInfo(SkillType.CRAFTING, 15, gainMultiplier = 4))
        put(Material.AMETHYST_BLOCK, CraftInfo(SkillType.CRAFTING, 20, gainMultiplier = 2))   // 4 amethyst shards

        // Blacksmithy - Stone pressure plates and buttons
        put(Material.STONE_PRESSURE_PLATE, CraftInfo(SkillType.CRAFTING, 8))
        put(Material.STONE_BUTTON, CraftInfo(SkillType.CRAFTING, 5))

        // Craftsmanship - Dyes (craftable ones)
        put(Material.BLACK_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BLUE_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BROWN_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.CYAN_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.GRAY_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.GREEN_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.LIGHT_BLUE_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.LIGHT_GRAY_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.LIME_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.MAGENTA_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.ORANGE_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.PINK_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.PURPLE_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.RED_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.WHITE_DYE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.YELLOW_DYE, CraftInfo(SkillType.CRAFTING, 3))

        // Craftsmanship - Planks (all wood types)
        put(Material.OAK_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.SPRUCE_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.BIRCH_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.JUNGLE_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.ACACIA_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.DARK_OAK_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.CRIMSON_PLANKS, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.WARPED_PLANKS, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.MANGROVE_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.CHERRY_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.BAMBOO_PLANKS, CraftInfo(SkillType.CRAFTING, 1))
        put(Material.BAMBOO_MOSAIC, CraftInfo(SkillType.CRAFTING, 3))

        // Craftsmanship - Wood blocks (stripped/etc)
        put(Material.STRIPPED_OAK_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_SPRUCE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_BIRCH_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_JUNGLE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_ACACIA_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_DARK_OAK_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_MANGROVE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_CHERRY_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.STRIPPED_CRIMSON_HYPHAE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.STRIPPED_WARPED_HYPHAE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.STRIPPED_BAMBOO_BLOCK, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.OAK_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.SPRUCE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.BIRCH_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.JUNGLE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.ACACIA_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.DARK_OAK_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.MANGROVE_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CHERRY_WOOD, CraftInfo(SkillType.CRAFTING, 2))
        put(Material.CRIMSON_HYPHAE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.WARPED_HYPHAE, CraftInfo(SkillType.CRAFTING, 3))
        put(Material.BAMBOO_BLOCK, CraftInfo(SkillType.CRAFTING, 2))

        // Blacksmithy - Andesite/Diorite/Granite base blocks
        put(Material.ANDESITE_SLAB, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.ANDESITE_STAIRS, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.ANDESITE_WALL, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.DIORITE_SLAB, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.DIORITE_STAIRS, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.DIORITE_WALL, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.GRANITE_SLAB, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.GRANITE_STAIRS, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.GRANITE_WALL, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.STONE_SLAB, CraftInfo(SkillType.CRAFTING, 6))
        put(Material.STONE_STAIRS, CraftInfo(SkillType.CRAFTING, 6))
    }

    fun getCraftInfo(material: Material): CraftInfo? = craftInfoMap[material]

    fun getSkillForMaterial(material: Material): SkillType? = craftInfoMap[material]?.skill

    fun getDifficulty(material: Material): Int = craftInfoMap[material]?.difficulty ?: 20
}
