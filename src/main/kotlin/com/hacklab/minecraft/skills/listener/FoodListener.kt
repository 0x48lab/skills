package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.CookingDifficulty
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent

/**
 * Listener for food consumption events
 * Applies cooking skill bonuses when players eat food
 */
class FoodListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item

        // Check if the food has a cooking bonus
        val foodBonusManager = plugin.craftingManager.foodBonusManager
        val bonus = foodBonusManager.getFoodBonus(item)

        if (bonus != 0.0) {
            // Get base healing from the food
            val cookingInfo = CookingDifficulty.getCookingInfo(item.type)
            if (cookingInfo != null) {
                // Calculate bonus healing
                val bonusHealing = (cookingInfo.baseHealing * bonus).toInt()
                val bonusSaturation = (cookingInfo.baseSaturation * bonus).toFloat()

                // Apply bonus after the event completes
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    applyFoodBonus(player, bonusHealing, bonusSaturation)
                }, 1L)
            }
        }
    }

    /**
     * Apply food bonus to player
     *
     * @param player The player who ate
     * @param bonusHealing Additional food points to restore
     * @param bonusSaturation Additional saturation to restore
     */
    private fun applyFoodBonus(player: Player, bonusHealing: Int, bonusSaturation: Float) {
        // Apply bonus food level (capped at 20)
        if (bonusHealing > 0) {
            player.foodLevel = (player.foodLevel + bonusHealing).coerceAtMost(20)
        } else if (bonusHealing < 0) {
            // Low quality food reduces the benefit (already happened, so no additional reduction needed)
        }

        // Apply bonus saturation (capped at food level)
        if (bonusSaturation > 0) {
            player.saturation = (player.saturation + bonusSaturation).coerceAtMost(player.foodLevel.toFloat())
        }

        // Also apply a small HP bonus for high quality food
        if (bonusHealing > 0) {
            val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
            val healthBonus = bonusHealing * 0.25  // 25% of food bonus as HP
            player.health = (player.health + healthBonus).coerceAtMost(maxHealth)
        }
    }
}
