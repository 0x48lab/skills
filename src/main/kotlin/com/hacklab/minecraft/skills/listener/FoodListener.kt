package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.QualityType
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Listener for food consumption events.
 *
 * Mana is NOT tied to food bar (UO-style).
 * - Golden Apple: Restores HP (via Regeneration) + MP
 * - Enchanted Golden Apple: Restores HP (via Regeneration) + more MP
 * - Other food: Normal vanilla behavior (restores hunger/saturation)
 *
 * MP recovery is primarily through:
 * - Natural regeneration over time (INT-based)
 * - Meditation skill (accelerates recovery when sneaking still)
 * - Golden Apples (bonus MP recovery)
 */
class FoodListener(private val plugin: Skills) : Listener {

    companion object {
        // MP restoration amounts for golden apples
        private const val GOLDEN_APPLE_MANA = 5.0
        private const val ENCHANTED_GOLDEN_APPLE_MANA = 10.0
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item
        val data = plugin.playerDataManager.getPlayerData(player)

        // Golden Apples restore MP in addition to their normal effects
        when (item.type) {
            Material.GOLDEN_APPLE -> {
                val restored = data.restoreMana(GOLDEN_APPLE_MANA)
                if (restored > 0) {
                    plugin.messageSender.send(player, MessageKey.FOOD_MANA_RESTORE,
                        "amount" to restored.toInt())
                }
            }
            Material.ENCHANTED_GOLDEN_APPLE -> {
                val restored = data.restoreMana(ENCHANTED_GOLDEN_APPLE_MANA)
                if (restored > 0) {
                    plugin.messageSender.send(player, MessageKey.FOOD_MANA_RESTORE,
                        "amount" to restored.toInt())
                }
            }
            else -> {
                // Normal food - apply cooking quality bonus (Health Boost for HQ/EX)
                val foodBonusManager = plugin.craftingManager.foodBonusManager
                val quality = foodBonusManager.getFoodQuality(item)

                when (quality) {
                    QualityType.HIGH_QUALITY -> {
                        // HQ: Health Boost I (+2 hearts) for 30 seconds
                        player.addPotionEffect(PotionEffect(
                            PotionEffectType.HEALTH_BOOST,
                            20 * 30,  // 30 seconds
                            0,        // Level I
                            true, true, true
                        ))
                    }
                    QualityType.EXCEPTIONAL -> {
                        // EX: Health Boost I (+2 hearts) for 1 minute
                        player.addPotionEffect(PotionEffect(
                            PotionEffectType.HEALTH_BOOST,
                            20 * 60,  // 60 seconds
                            0,        // Level I
                            true, true, true
                        ))
                    }
                    else -> { /* No Health Boost for LQ/NQ */ }
                }
            }
        }
    }
}
