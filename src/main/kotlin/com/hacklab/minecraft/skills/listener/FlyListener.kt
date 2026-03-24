package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.EndReason
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleFlightEvent

class FlyListener(private val plugin: Skills) : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        // Quick filter: skip if player has no active fly effect
        if (!plugin.activeSpellManager.hasEffect(player.uniqueId, SpellType.FLY)) return

        // Water submersion check (head in water)
        if (player.eyeLocation.block.type == Material.WATER) {
            plugin.activeSpellManager.cancelEffect(player.uniqueId, SpellType.FLY, EndReason.ENVIRONMENTAL)
        }
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player

        // Only handle when fly spell is active and player is turning flight OFF
        if (!event.isFlying && plugin.activeSpellManager.hasEffect(player.uniqueId, SpellType.FLY)) {
            // Player voluntarily stopped flying (double-tap space on ground, or landed)
            // Cancel the fly spell effect with safe landing
            plugin.activeSpellManager.cancelEffect(player.uniqueId, SpellType.FLY, EndReason.CANCELLED)
        }
    }

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        // Currently no world restrictions on flight
        // Kept as extension point for future world-specific rules
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        if (plugin.activeSpellManager.hasAnyEffect(player.uniqueId)) {
            plugin.activeSpellManager.cancelEffects(player.uniqueId, EndReason.DEATH)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        if (plugin.activeSpellManager.hasAnyEffect(player.uniqueId)) {
            plugin.activeSpellManager.cancelEffects(player.uniqueId, EndReason.DISCONNECT)
        }
    }
}
