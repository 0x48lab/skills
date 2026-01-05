package com.hacklab.minecraft.skills.loot

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Manages scroll drops from treasure chests (End city, Trial Chamber)
 */
class EndChestLoot(private val plugin: Skills) {

    companion object {
        // End City rates
        const val C7_DROP_RATE = 0.005  // 0.5%
        const val C8_DROP_RATE = 0.002  // 0.2%

        // Trial Chamber rates (C1-C8) - higher tier = rarer
        const val TRIAL_C1_C3_DROP_RATE = 0.08   // 8% for common spells
        const val TRIAL_C4_C5_DROP_RATE = 0.04   // 4% for mid-tier spells
        const val TRIAL_C6_DROP_RATE = 0.02      // 2% for high-tier spells
        const val TRIAL_C7_DROP_RATE = 0.008     // 0.8% for rare spells
        const val TRIAL_C8_DROP_RATE = 0.003     // 0.3% for legendary spells
    }

    /**
     * Try to generate a high-circle scroll for End chest loot
     * @return ItemStack of scroll if drop succeeds, null otherwise
     */
    fun tryGetEndChestScroll(): ItemStack? {
        // Roll for C7 first
        if (Random.nextDouble() < C7_DROP_RATE) {
            val spell = getRandomC7Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // Then roll for C8
        if (Random.nextDouble() < C8_DROP_RATE) {
            val spell = getRandomC8Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        return null
    }

    /**
     * Get a random 7th circle spell
     */
    private fun getRandomC7Spell(): SpellType? {
        return SpellType.entries
            .filter { it.circle == SpellCircle.SEVENTH }
            .randomOrNull()
    }

    /**
     * Get a random 8th circle spell
     */
    private fun getRandomC8Spell(): SpellType? {
        return SpellType.entries
            .filter { it.circle == SpellCircle.EIGHTH }
            .randomOrNull()
    }

    /**
     * Check if End chest loot is enabled
     */
    fun isEndChestEnabled(): Boolean {
        return plugin.config.getBoolean("scroll.end_chest_enabled", true)
    }

    /**
     * Try to generate a scroll for Trial Chamber vault loot
     * Drops C1-C8 scrolls with decreasing probability for higher circles
     * @return ItemStack of scroll if drop succeeds, null otherwise
     */
    fun tryGetTrialChamberScroll(): ItemStack? {
        // Roll from highest to lowest circle (rarest first)
        // C8 - Legendary (0.3%)
        if (Random.nextDouble() < TRIAL_C8_DROP_RATE) {
            val spell = getRandomC8Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C7 - Rare (0.8%)
        if (Random.nextDouble() < TRIAL_C7_DROP_RATE) {
            val spell = getRandomC7Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C6 - High-tier (2%)
        if (Random.nextDouble() < TRIAL_C6_DROP_RATE) {
            val spell = getRandomSpellByCircle(SpellCircle.SIXTH) ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C4-C5 - Mid-tier (4%)
        if (Random.nextDouble() < TRIAL_C4_C5_DROP_RATE) {
            val spell = getRandomSpellByCircles(SpellCircle.FOURTH, SpellCircle.FIFTH) ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C1-C3 - Common (8%)
        if (Random.nextDouble() < TRIAL_C1_C3_DROP_RATE) {
            val spell = getRandomSpellByCircles(SpellCircle.FIRST, SpellCircle.SECOND, SpellCircle.THIRD) ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        return null
    }

    /**
     * Get a random spell from a specific circle
     */
    private fun getRandomSpellByCircle(circle: SpellCircle): SpellType? {
        return SpellType.entries
            .filter { it.circle == circle }
            .randomOrNull()
    }

    /**
     * Get a random spell from multiple circles
     */
    private fun getRandomSpellByCircles(vararg circles: SpellCircle): SpellType? {
        return SpellType.entries
            .filter { it.circle in circles }
            .randomOrNull()
    }

    /**
     * Check if Trial Chamber loot is enabled
     */
    fun isTrialChamberEnabled(): Boolean {
        return plugin.config.getBoolean("scroll.trial_chamber_enabled", true)
    }
}
