package com.hacklab.minecraft.skills.loot

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Manages scroll drops from magic mobs
 */
class ScrollDropManager(private val plugin: Skills) {

    /**
     * Try to generate a scroll drop for a killed mob
     * @param entityType The type of mob killed
     * @return ItemStack of scroll if drop succeeds, null otherwise
     */
    fun tryGetScrollDrop(entityType: EntityType): ItemStack? {
        val magicMob = MagicMob.fromEntityType(entityType) ?: return null

        // Roll for drop
        if (Random.nextDouble() >= magicMob.dropRate) {
            return null
        }

        // Get random circle within mob's range
        val circle = Random.nextInt(magicMob.minCircle, magicMob.maxCircle + 1)
        val spellCircle = SpellCircle.entries.find { it.number == circle } ?: return null

        // Get random spell from that circle
        val spell = getRandomSpellFromCircle(spellCircle) ?: return null

        // Create and return scroll
        return plugin.scrollManager.createScroll(spell)
    }

    /**
     * Get a random spell from a specific circle
     */
    private fun getRandomSpellFromCircle(circle: SpellCircle): SpellType? {
        val spellsInCircle = SpellType.entries.filter { it.circle == circle }
        return spellsInCircle.randomOrNull()
    }

    /**
     * Check if mob drops are enabled
     */
    fun isMobDropEnabled(): Boolean {
        return plugin.config.getBoolean("scroll.mob_drop_enabled", true)
    }
}
