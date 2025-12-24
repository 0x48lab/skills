package com.hacklab.minecraft.skills.taming

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.entity.Animals
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class VeterinaryManager(private val plugin: Skills) {

    // Food items and their healing values
    private val healingFoods = mapOf(
        // Generic animal foods
        Material.WHEAT to 10.0,
        Material.CARROT to 10.0,
        Material.APPLE to 10.0,
        Material.BEETROOT to 8.0,
        Material.POTATO to 8.0,
        Material.MELON_SLICE to 5.0,

        // Meat (for wolves/cats)
        Material.BEEF to 15.0,
        Material.PORKCHOP to 15.0,
        Material.CHICKEN to 12.0,
        Material.MUTTON to 12.0,
        Material.RABBIT to 10.0,
        Material.COD to 10.0,
        Material.SALMON to 12.0,

        // Cooked versions (better healing)
        Material.COOKED_BEEF to 25.0,
        Material.COOKED_PORKCHOP to 25.0,
        Material.COOKED_CHICKEN to 20.0,
        Material.COOKED_MUTTON to 20.0,
        Material.COOKED_RABBIT to 18.0,
        Material.COOKED_COD to 18.0,
        Material.COOKED_SALMON to 20.0,

        // Special foods
        Material.GOLDEN_APPLE to 50.0,
        Material.GOLDEN_CARROT to 40.0,
        Material.HAY_BLOCK to 30.0,
        Material.SUGAR to 5.0
    )

    /**
     * Attempt to heal a pet with food
     * Called when player right-clicks a tamed animal with food
     */
    fun tryHeal(player: Player, entity: LivingEntity, food: ItemStack): HealResult {
        // Check if player owns this animal
        if (!plugin.tamingManager.isOwner(player, entity)) {
            plugin.messageSender.send(player, MessageKey.VETERINARY_NOT_OWNER)
            return HealResult.NOT_OWNER
        }

        // Check if food is valid
        val baseHeal = healingFoods[food.type]
        if (baseHeal == null) {
            plugin.messageSender.send(player, MessageKey.VETERINARY_WRONG_FOOD)
            return HealResult.WRONG_FOOD
        }

        // Check if animal needs healing
        if (entity.health >= entity.maxHealth) {
            return HealResult.ALREADY_FULL
        }

        // Calculate healing based on Veterinary skill
        val data = plugin.playerDataManager.getPlayerData(player)
        val vetSkill = data.getSkillValue(SkillType.VETERINARY)

        // Skill bonus: +1% per skill point (max +100%)
        val skillBonus = 1.0 + (vetSkill / 100.0)
        val actualHeal = baseHeal * skillBonus

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.VETERINARY, 30)

        // Apply healing
        entity.health = (entity.health + actualHeal).coerceAtMost(entity.maxHealth)

        // Consume food
        if (food.amount > 1) {
            food.amount -= 1
        } else {
            player.inventory.removeItem(food)
        }

        plugin.messageSender.send(player, MessageKey.VETERINARY_HEAL,
            "amount" to actualHeal.toInt(),
            "entity" to entity.type.name.lowercase().replace("_", " "))

        return HealResult.SUCCESS
    }

    /**
     * Check if item is valid healing food
     */
    fun isHealingFood(material: Material): Boolean {
        return healingFoods.containsKey(material)
    }

    /**
     * Get base healing value for a food
     */
    fun getBaseHealing(material: Material): Double {
        return healingFoods[material] ?: 0.0
    }

    /**
     * Get food preferences for an entity type
     */
    fun getPreferredFood(entity: LivingEntity): List<Material> {
        return when (entity) {
            is org.bukkit.entity.Wolf -> listOf(
                Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
                Material.BEEF, Material.PORKCHOP, Material.CHICKEN
            )
            is org.bukkit.entity.Cat -> listOf(
                Material.COOKED_COD, Material.COOKED_SALMON, Material.COD, Material.SALMON
            )
            is org.bukkit.entity.Horse, is org.bukkit.entity.Donkey -> listOf(
                Material.GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.HAY_BLOCK,
                Material.APPLE, Material.SUGAR, Material.WHEAT
            )
            is org.bukkit.entity.Parrot -> listOf(
                Material.WHEAT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS
            )
            else -> healingFoods.keys.toList()
        }
    }
}

enum class HealResult {
    SUCCESS,
    NOT_OWNER,
    WRONG_FOOD,
    ALREADY_FULL
}
