package com.hacklab.minecraft.skills.loot

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Manages C7/C8 scroll drops from End city treasure chests
 */
class EndChestLoot(private val plugin: Skills) {

    companion object {
        const val C7_DROP_RATE = 0.005  // 0.5%
        const val C8_DROP_RATE = 0.002  // 0.2%
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
}
