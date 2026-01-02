package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta

class CraftingManager(private val plugin: Skills) {

    // Bonus managers for stackable items
    val foodBonusManager = FoodBonusManager(plugin)
    val potionBonusManager = PotionBonusManager(plugin)

    /**
     * Process a crafting event
     * Called when player crafts an item
     */
    fun processCraft(player: Player, result: ItemStack): ItemStack {
        // Check if this is a craftable food
        if (CookingDifficulty.isCraftedFood(result.type)) {
            return processCraftedFood(player, result)
        }

        val craftInfo = CraftDifficulty.getCraftInfo(result.type) ?: return result

        val data = plugin.playerDataManager.getPlayerData(player)
        val skillValue = data.getSkillValue(craftInfo.skill)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, craftInfo.skill, craftInfo.difficulty)

        // Apply quality based on skill and difficulty (only for non-stackable items)
        val qualifiedResult = plugin.qualityManager.applyQualityFromSkill(result, skillValue, craftInfo.difficulty)

        // Notify player only if quality was applied (non-stackable items)
        if (result.maxStackSize == 1) {
            val quality = plugin.qualityManager.getQuality(qualifiedResult)
            plugin.messageSender.send(
                player, MessageKey.CRAFTING_QUALITY,
                "item" to result.type.name.lowercase().replace("_", " "),
                "quality" to quality.displayName
            )
        }

        return qualifiedResult
    }

    /**
     * Process crafted food items (bread, cake, etc.)
     */
    private fun processCraftedFood(player: Player, result: ItemStack): ItemStack {
        val data = plugin.playerDataManager.getPlayerData(player)
        val cookingSkill = data.getSkillValue(SkillType.COOKING)
        val difficulty = CookingDifficulty.getDifficulty(result.type)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.COOKING, difficulty)

        // Apply food bonus
        return foodBonusManager.applyFoodBonus(result, cookingSkill, difficulty, player.name)
    }

    /**
     * Process anvil repair
     */
    fun processAnvilRepair(player: Player, item: ItemStack): Boolean {
        // Anvil repair uses Blacksmithy
        val data = plugin.playerDataManager.getPlayerData(player)

        // Difficulty based on item material
        val difficulty = when {
            item.type.name.contains("NETHERITE") -> 80
            item.type.name.contains("DIAMOND") -> 60
            item.type.name.contains("IRON") -> 40
            item.type.name.contains("GOLD") -> 35
            else -> 30
        }

        plugin.skillManager.tryGainSkill(player, SkillType.BLACKSMITHY, difficulty)
        return true
    }

    /**
     * Process brewing (Alchemy)
     * Applies duration bonus and quality based on Alchemy skill
     */
    fun processBrewing(player: Player, result: ItemStack): ItemStack {
        val data = plugin.playerDataManager.getPlayerData(player)
        val alchemySkill = data.getSkillValue(SkillType.ALCHEMY)

        // Get potion metadata for difficulty calculation
        val meta = result.itemMeta as? PotionMeta
        val potionType = meta?.basePotionType

        // Calculate difficulty based on potion type and modifiers
        val difficulty = if (potionType != null) {
            val isExtended = potionType.name.startsWith("LONG_")
            val isUpgraded = potionType.name.startsWith("STRONG_")
            AlchemyDifficulty.calculateDifficulty(potionType, isExtended, isUpgraded, result.type)
        } else {
            when {
                result.type == Material.SPLASH_POTION -> 50
                result.type == Material.LINGERING_POTION -> 60
                else -> 30
            }
        }

        plugin.skillManager.tryGainSkill(player, SkillType.ALCHEMY, difficulty)

        // Apply potion bonus (duration extension, quality)
        return potionBonusManager.applyPotionBonus(result, alchemySkill, player.name)
    }

    /**
     * Process cooking (food from furnace/smoker)
     * Applies recovery bonus based on Cooking skill
     */
    fun processCooking(player: Player, result: ItemStack): ItemStack {
        val data = plugin.playerDataManager.getPlayerData(player)
        val cookingSkill = data.getSkillValue(SkillType.COOKING)

        // Get difficulty from cooking difficulty table
        val difficulty = CookingDifficulty.getDifficulty(result.type)

        plugin.skillManager.tryGainSkill(player, SkillType.COOKING, difficulty)

        // Apply food bonus (recovery bonus based on skill and quality)
        return foodBonusManager.applyFoodBonus(result, cookingSkill, difficulty, player.name)
    }

    /**
     * Get the crafting skill required for a material
     */
    fun getCraftingSkill(material: Material): SkillType? {
        return CraftDifficulty.getSkillForMaterial(material)
    }

    /**
     * Check if an item is craftable with skills
     */
    fun isSkillCraftable(material: Material): Boolean {
        return CraftDifficulty.getCraftInfo(material) != null ||
               CookingDifficulty.isCraftedFood(material)
    }
}
