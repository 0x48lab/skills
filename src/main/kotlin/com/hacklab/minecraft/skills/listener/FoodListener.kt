package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.CookingDifficulty
import com.hacklab.minecraft.skills.crafting.QualityType
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.StatCalculator
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
 * In this plugin, the food bar represents Mana (magic power).
 * When eating:
 * 1. Food restores Mana first (based on food's healing value + cooking bonus)
 * 2. If Mana overflows (exceeds max), the overflow is converted to HP
 * 3. HQ/EX quality food also grants Health Boost effect
 *
 * Vanilla food mechanics (hunger/saturation) are disabled by ExhaustionListener.
 */
class FoodListener(private val plugin: Skills) : Listener {

    companion object {
        // Conversion rate: 1 mana overflow = 5 internal HP (0.25 vanilla hearts)
        private const val HP_PER_MANA_OVERFLOW = 5.0
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerEat(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item

        // Get player data for mana/HP management
        val data = plugin.playerDataManager.getPlayerData(player)

        // Get food's base healing value
        val cookingInfo = CookingDifficulty.getCookingInfo(item.type)
        val baseHealing = cookingInfo?.baseHealing ?: getDefaultFoodValue(item.type)

        // Apply cooking skill bonus
        val foodBonusManager = plugin.craftingManager.foodBonusManager
        val bonus = foodBonusManager.getFoodBonus(item)
        val quality = foodBonusManager.getFoodQuality(item)

        // Calculate total mana restoration (base + bonus)
        val bonusHealing = if (bonus != 0.0) (baseHealing * bonus).toInt() else 0
        val totalManaRestoration = baseHealing + bonusHealing

        // Calculate available mana space
        val manaSpace = data.maxMana - data.mana

        // Restore mana and handle overflow
        if (totalManaRestoration <= manaSpace) {
            // All goes to mana - no overflow
            data.restoreMana(totalManaRestoration.toDouble())
        } else {
            // Mana overflows - restore full mana, convert overflow to HP
            val overflow = totalManaRestoration - manaSpace.toInt()
            data.restoreMana(manaSpace)

            // Convert overflow to HP
            if (overflow > 0) {
                val hpHealing = overflow * HP_PER_MANA_OVERFLOW
                val actualHealed = data.heal(hpHealing)

                // Notify player if HP was restored
                if (actualHealed > 0) {
                    plugin.messageSender.send(player, MessageKey.FOOD_OVERFLOW_HEAL,
                        "amount" to String.format("%.1f", actualHealed))
                }

                // Sync internal HP to vanilla health
                StatCalculator.syncHealthToVanilla(player, data)
            }
        }

        // Sync mana to vanilla food level
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            StatCalculator.syncManaToVanilla(player, data)
        }, 1L)

        // Apply Health Boost for HQ/EX quality food
        // Balanced to not overshadow golden apples (which give Absorption + Regeneration)
        when (quality) {
            QualityType.HIGH_QUALITY -> {
                // HQ: Health Boost I (+2 hearts) for 30 seconds
                player.addPotionEffect(PotionEffect(
                    PotionEffectType.HEALTH_BOOST,
                    20 * 30,  // 30 seconds
                    0,        // Level I (amplifier 0 = +4 HP = +2 hearts)
                    true,     // ambient (subtle particles)
                    true,     // show particles
                    true      // show icon
                ))
            }
            QualityType.EXCEPTIONAL -> {
                // EX: Health Boost I (+2 hearts) for 1 minute
                player.addPotionEffect(PotionEffect(
                    PotionEffectType.HEALTH_BOOST,
                    20 * 60,  // 60 seconds
                    0,        // Level I
                    true,
                    true,
                    true
                ))
            }
            else -> { /* No Health Boost for LQ/NQ */ }
        }
    }

    /**
     * Get default food value for items not in CookingDifficulty table.
     * Returns the vanilla food point value.
     */
    private fun getDefaultFoodValue(type: org.bukkit.Material): Int {
        return when (type) {
            org.bukkit.Material.APPLE -> 4
            org.bukkit.Material.GOLDEN_APPLE -> 4
            org.bukkit.Material.ENCHANTED_GOLDEN_APPLE -> 4
            org.bukkit.Material.MELON_SLICE -> 2
            org.bukkit.Material.SWEET_BERRIES -> 2
            org.bukkit.Material.GLOW_BERRIES -> 2
            org.bukkit.Material.CHORUS_FRUIT -> 4
            org.bukkit.Material.CARROT -> 3
            org.bukkit.Material.GOLDEN_CARROT -> 6
            org.bukkit.Material.POTATO -> 1
            org.bukkit.Material.BEETROOT -> 1
            org.bukkit.Material.DRIED_KELP -> 1
            org.bukkit.Material.BEEF -> 3
            org.bukkit.Material.COOKED_BEEF -> 8
            org.bukkit.Material.PORKCHOP -> 3
            org.bukkit.Material.COOKED_PORKCHOP -> 8
            org.bukkit.Material.MUTTON -> 2
            org.bukkit.Material.COOKED_MUTTON -> 6
            org.bukkit.Material.CHICKEN -> 2
            org.bukkit.Material.COOKED_CHICKEN -> 6
            org.bukkit.Material.RABBIT -> 3
            org.bukkit.Material.COOKED_RABBIT -> 5
            org.bukkit.Material.COD -> 2
            org.bukkit.Material.COOKED_COD -> 5
            org.bukkit.Material.SALMON -> 2
            org.bukkit.Material.COOKED_SALMON -> 6
            org.bukkit.Material.TROPICAL_FISH -> 1
            org.bukkit.Material.PUFFERFISH -> 1
            org.bukkit.Material.BREAD -> 5
            org.bukkit.Material.COOKIE -> 2
            org.bukkit.Material.PUMPKIN_PIE -> 8
            org.bukkit.Material.MUSHROOM_STEW -> 6
            org.bukkit.Material.BEETROOT_SOUP -> 6
            org.bukkit.Material.RABBIT_STEW -> 10
            org.bukkit.Material.SUSPICIOUS_STEW -> 6
            org.bukkit.Material.BAKED_POTATO -> 5
            org.bukkit.Material.POISONOUS_POTATO -> 2
            org.bukkit.Material.ROTTEN_FLESH -> 4
            org.bukkit.Material.SPIDER_EYE -> 2
            org.bukkit.Material.HONEY_BOTTLE -> 6
            else -> 2  // Default fallback
        }
    }
}
