package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

/**
 * Fly spell ongoing effect.
 * Drains 0.2 MP/sec while active. At INT 100, natural regen matches drain = infinite flight.
 */
class FlyEffect(
    override val playerId: UUID,
    private val plugin: Skills
) : OngoingSpellEffect {

    override val spellType = SpellType.FLY

    private companion object {
        const val MANA_DRAIN_PER_SECOND = 0.1
        const val MANA_WARNING_THRESHOLD = 3.0
        const val SLOW_FALLING_DURATION_TICKS = 100 // 5 seconds
    }

    override fun onStart(player: Player) {
        player.allowFlight = true
        player.isFlying = true

        // Start particles: CLOUD + END_ROD rising from feet
        val loc = player.location
        player.world.spawnParticle(Particle.CLOUD, loc.clone().add(0.0, 0.5, 0.0), 30, 0.5, 0.3, 0.5, 0.05)
        player.world.spawnParticle(Particle.END_ROD, loc.clone().add(0.0, 0.2, 0.0), 15, 0.3, 0.5, 0.3, 0.08)

        // Lift player a few blocks for clear visual feedback
        player.velocity = player.velocity.setY(0.6)

        // Sound
        player.world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f)

        // Message
        plugin.messageSender.send(player, MessageKey.FLY_START)
    }

    override fun tick(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Drain mana
        data.mana = (data.mana - MANA_DRAIN_PER_SECOND).coerceAtLeast(0.0)
        data.dirty = true

        val currentMana = data.mana
        val maxMana = data.maxMana

        // Flying particles (subtle cloud at feet, visible to others)
        player.world.spawnParticle(Particle.CLOUD, player.location, 3, 0.2, 0.0, 0.2, 0.01)

        // Check mana depleted
        if (currentMana <= 0) {
            return false // Effect ends
        }

        return true // Continue
    }

    override fun onEnd(player: Player, reason: EndReason) {
        // Disable flight
        player.allowFlight = false
        player.isFlying = false

        // End particles
        player.world.spawnParticle(Particle.CLOUD, player.location, 20, 0.8, 0.3, 0.8, 0.05)

        // End sound
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f)

        // Slow Falling for safe landing (depends on reason)
        val grantSlowFalling = when (reason) {
            EndReason.EXPIRED, EndReason.CANCELLED, EndReason.DISPELLED, EndReason.WORLD_CHANGE -> true
            EndReason.DEATH, EndReason.DISCONNECT, EndReason.ENVIRONMENTAL -> false
        }

        if (grantSlowFalling) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0)
            )
        }

        // Send appropriate message
        when (reason) {
            EndReason.EXPIRED -> plugin.messageSender.send(player, MessageKey.FLY_END)
            EndReason.CANCELLED -> plugin.messageSender.send(player, MessageKey.FLY_CANCELLED)
            EndReason.DISPELLED -> plugin.messageSender.send(player, MessageKey.FLY_DISPELLED)
            EndReason.ENVIRONMENTAL -> plugin.messageSender.send(player, MessageKey.FLY_WATER)
            EndReason.WORLD_CHANGE -> plugin.messageSender.send(player, MessageKey.FLY_END_WORLD)
            EndReason.DEATH, EndReason.DISCONNECT -> {} // No message needed
        }
    }
}
