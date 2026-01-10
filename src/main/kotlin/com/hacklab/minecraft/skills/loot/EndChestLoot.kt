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
        // End City rates - High circle focus (C5-C8 only)
        // End City is endgame content, so rewards are better quality
        const val END_C5_C6_DROP_RATE = 0.10  // 10% for C5-C6
        const val END_C7_DROP_RATE = 0.05     // 5% for C7
        const val END_C8_DROP_RATE = 0.02     // 2% for C8
        // Total: ~17% per chest, ~4 scrolls per hour (25 chests)
        // C7-C8: ~1-2 per hour

        // Trial Chamber rates (C1-C8) - higher tier = rarer
        const val TRIAL_C1_C3_DROP_RATE = 0.08   // 8% for common spells
        const val TRIAL_C4_C5_DROP_RATE = 0.04   // 4% for mid-tier spells
        const val TRIAL_C6_DROP_RATE = 0.02      // 2% for high-tier spells
        const val TRIAL_C7_DROP_RATE = 0.008     // 0.8% for rare spells
        const val TRIAL_C8_DROP_RATE = 0.003     // 0.3% for legendary spells
    }

    /**
     * Try to generate a high-circle scroll for End chest loot
     * End City drops C5-C8 only (high circle focus for endgame content)
     * @return ItemStack of scroll if drop succeeds, null otherwise
     */
    fun tryGetEndChestScroll(): ItemStack? {
        // Roll from highest to lowest (rarest first)
        // C8 - Legendary (2%)
        if (Random.nextDouble() < END_C8_DROP_RATE) {
            val spell = getRandomC8Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C7 - Rare (5%)
        if (Random.nextDouble() < END_C7_DROP_RATE) {
            val spell = getRandomC7Spell() ?: return null
            return plugin.scrollManager.createScroll(spell)
        }

        // C5-C6 - High-tier (10%)
        if (Random.nextDouble() < END_C5_C6_DROP_RATE) {
            val spell = getRandomSpellByCircles(SpellCircle.FIFTH, SpellCircle.SIXTH) ?: return null
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
