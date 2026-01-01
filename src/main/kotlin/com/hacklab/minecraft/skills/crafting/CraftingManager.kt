package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CraftingManager(private val plugin: Skills) {

    /**
     * Process a crafting event
     * Called when player crafts an item
     */
    fun processCraft(player: Player, result: ItemStack): ItemStack {
        val craftInfo = CraftDifficulty.getCraftInfo(result.type) ?: return result

        val data = plugin.playerDataManager.getPlayerData(player)
        val skillValue = data.getSkillValue(craftInfo.skill)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, craftInfo.skill, craftInfo.difficulty)

        // Apply quality based on skill (only for non-stackable items)
        val qualifiedResult = plugin.qualityManager.applyQualityFromSkill(result, skillValue)

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
     */
    fun processBrewing(player: Player, result: ItemStack): ItemStack {
        val data = plugin.playerDataManager.getPlayerData(player)
        val alchemySkill = data.getSkillValue(SkillType.ALCHEMY)

        // Determine difficulty based on potion type
        val difficulty = when {
            result.type == Material.SPLASH_POTION -> 50
            result.type == Material.LINGERING_POTION -> 60
            else -> 30
        }

        plugin.skillManager.tryGainSkill(player, SkillType.ALCHEMY, difficulty)

        // Apply quality (affects duration/potency conceptually)
        return plugin.qualityManager.applyQualityFromSkill(result, alchemySkill)
    }

    /**
     * Process cooking (food from furnace/smoker)
     */
    fun processCooking(player: Player, result: ItemStack): ItemStack {
        val data = plugin.playerDataManager.getPlayerData(player)
        val cookingSkill = data.getSkillValue(SkillType.COOKING)

        // Difficulty based on food complexity
        val difficulty = when (result.type) {
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP -> 15
            Material.COOKED_CHICKEN, Material.COOKED_MUTTON -> 10
            Material.COOKED_RABBIT -> 20
            Material.COOKED_COD, Material.COOKED_SALMON -> 15
            Material.BAKED_POTATO -> 10
            Material.DRIED_KELP -> 5
            else -> 10
        }

        plugin.skillManager.tryGainSkill(player, SkillType.COOKING, difficulty)

        // Apply quality (affects saturation conceptually)
        return plugin.qualityManager.applyQualityFromSkill(result, cookingSkill)
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
        return CraftDifficulty.getCraftInfo(material) != null
    }
}
