package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExhaustionEvent

/**
 * Listener to disable vanilla exhaustion (hunger depletion from actions)
 * In this plugin, the food bar represents Mana, and mana is only consumed by spells.
 * Actions like running, jumping, mining etc. should NOT reduce mana.
 *
 * Note: Food level is capped at 19 (via StatCalculator.syncManaToVanilla) so players
 * can always eat. No need to temporarily lower food level on right-click.
 */
class ExhaustionListener(private val plugin: Skills) : Listener {

    /**
     * Disable vanilla exhaustion completely.
     * Mana (food bar) is managed by the magic system, not by vanilla mechanics.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onExhaustion(event: EntityExhaustionEvent) {
        if (event.entity is Player) {
            // Cancel all exhaustion - mana is managed separately
            event.isCancelled = true
        }
    }
}
