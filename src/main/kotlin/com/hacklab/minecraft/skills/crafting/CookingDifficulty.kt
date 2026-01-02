package com.hacklab.minecraft.skills.crafting

import org.bukkit.Material

/**
 * Cooking difficulty table for all food items
 * Based on CLAUDE.md specification
 */
object CookingDifficulty {

    data class CookingInfo(
        val difficulty: Int,
        val baseHealing: Int,      // Base food points restored
        val baseSaturation: Float  // Base saturation restored
    )

    private val cookingInfoMap: Map<Material, CookingInfo> = buildMap {
        // ========================================
        // Furnace/Smoker cooked foods (Basic: 10-20)
        // ========================================

        // Cooked meats - Basic (difficulty 10-15)
        put(Material.COOKED_BEEF, CookingInfo(15, 8, 12.8f))
        put(Material.COOKED_PORKCHOP, CookingInfo(15, 8, 12.8f))
        put(Material.COOKED_MUTTON, CookingInfo(12, 6, 9.6f))
        put(Material.COOKED_CHICKEN, CookingInfo(10, 6, 7.2f))
        put(Material.COOKED_RABBIT, CookingInfo(15, 5, 6.0f))

        // Cooked fish - Basic (difficulty 12-15)
        put(Material.COOKED_COD, CookingInfo(12, 5, 6.0f))
        put(Material.COOKED_SALMON, CookingInfo(15, 6, 9.6f))

        // Simple cooked items (difficulty 5-10)
        put(Material.BAKED_POTATO, CookingInfo(10, 5, 6.0f))
        put(Material.DRIED_KELP, CookingInfo(5, 1, 0.6f))

        // ========================================
        // Crafted foods - Simple (difficulty 10-20)
        // ========================================

        // Bread - Simple grain processing
        put(Material.BREAD, CookingInfo(10, 5, 6.0f))

        // Cookies - Simple baking
        put(Material.COOKIE, CookingInfo(15, 2, 0.4f))

        // Melon slice - No cooking required
        put(Material.MELON_SLICE, CookingInfo(5, 2, 1.2f))

        // ========================================
        // Crafted foods - Complex (difficulty 25-35)
        // ========================================

        // Pumpkin Pie - Complex baking
        put(Material.PUMPKIN_PIE, CookingInfo(30, 8, 4.8f))

        // Cake - Complex multi-ingredient
        put(Material.CAKE, CookingInfo(35, 14, 2.8f))  // Total when fully eaten

        // Beetroot Soup - Simple soup
        put(Material.BEETROOT_SOUP, CookingInfo(25, 6, 7.2f))

        // Mushroom Stew - Medium soup
        put(Material.MUSHROOM_STEW, CookingInfo(25, 6, 7.2f))

        // Rabbit Stew - Complex soup (many ingredients)
        put(Material.RABBIT_STEW, CookingInfo(35, 10, 12.0f))

        // Suspicious Stew - Requires knowledge of effects
        put(Material.SUSPICIOUS_STEW, CookingInfo(40, 6, 7.2f))

        // ========================================
        // Golden foods - Special (difficulty 40-60)
        // ========================================

        // Golden Carrot - Valuable ingredient
        put(Material.GOLDEN_CARROT, CookingInfo(40, 6, 14.4f))

        // Golden Apple - Magical food
        put(Material.GOLDEN_APPLE, CookingInfo(50, 4, 9.6f))

        // Enchanted Golden Apple - Legendary (not craftable but included for reference)
        put(Material.ENCHANTED_GOLDEN_APPLE, CookingInfo(100, 4, 9.6f))

        // ========================================
        // Raw foods - No cooking skill needed (difficulty 0-5)
        // ========================================

        // Fruits and vegetables (no cooking)
        put(Material.APPLE, CookingInfo(5, 4, 2.4f))
        put(Material.CARROT, CookingInfo(5, 3, 3.6f))
        put(Material.POTATO, CookingInfo(5, 1, 0.6f))
        put(Material.BEETROOT, CookingInfo(5, 1, 1.2f))
        put(Material.SWEET_BERRIES, CookingInfo(5, 2, 0.4f))
        put(Material.GLOW_BERRIES, CookingInfo(5, 2, 0.4f))
        put(Material.CHORUS_FRUIT, CookingInfo(10, 4, 2.4f))

        // Honey
        put(Material.HONEY_BOTTLE, CookingInfo(15, 6, 1.2f))

        // ========================================
        // Raw meats and fish (difficulty 0 - eating raw)
        // ========================================

        put(Material.BEEF, CookingInfo(0, 3, 1.8f))
        put(Material.PORKCHOP, CookingInfo(0, 3, 1.8f))
        put(Material.CHICKEN, CookingInfo(0, 2, 1.2f))  // Can cause hunger
        put(Material.MUTTON, CookingInfo(0, 2, 1.2f))
        put(Material.RABBIT, CookingInfo(0, 3, 1.8f))
        put(Material.COD, CookingInfo(0, 2, 0.4f))
        put(Material.SALMON, CookingInfo(0, 2, 0.4f))
        put(Material.TROPICAL_FISH, CookingInfo(0, 1, 0.2f))

        // ========================================
        // Dangerous foods (difficulty 0 - no skill helps)
        // ========================================

        put(Material.ROTTEN_FLESH, CookingInfo(0, 4, 0.8f))
        put(Material.SPIDER_EYE, CookingInfo(0, 2, 3.2f))
        put(Material.PUFFERFISH, CookingInfo(0, 1, 0.2f))
        put(Material.POISONOUS_POTATO, CookingInfo(0, 2, 1.2f))
    }

    /**
     * Get cooking info for a food item
     */
    fun getCookingInfo(material: Material): CookingInfo? = cookingInfoMap[material]

    /**
     * Get difficulty for a food item
     */
    fun getDifficulty(material: Material): Int = cookingInfoMap[material]?.difficulty ?: 0

    /**
     * Check if material is a food that can benefit from Cooking skill
     */
    fun isCookableFood(material: Material): Boolean = cookingInfoMap.containsKey(material)

    /**
     * Check if material is cooked in furnace/smoker
     */
    fun isFurnaceFood(material: Material): Boolean = material in listOf(
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
        Material.COOKED_CHICKEN, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.COOKED_SALMON, Material.BAKED_POTATO, Material.DRIED_KELP
    )

    /**
     * Check if material is crafted food (uses crafting table)
     */
    fun isCraftedFood(material: Material): Boolean = material in listOf(
        Material.BREAD, Material.COOKIE, Material.CAKE, Material.PUMPKIN_PIE,
        Material.BEETROOT_SOUP, Material.MUSHROOM_STEW, Material.RABBIT_STEW,
        Material.SUSPICIOUS_STEW, Material.GOLDEN_CARROT, Material.GOLDEN_APPLE
    )
}
