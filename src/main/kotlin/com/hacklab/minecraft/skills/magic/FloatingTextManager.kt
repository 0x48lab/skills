package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manages enchantment-style particle effects for spell casting
 * Spawns magical glyphs (like enchantment table) around the caster during casting
 */
class FloatingTextManager(private val plugin: Skills) {

    data class CastingEffect(
        val spell: SpellType,
        val createdAt: Long,
        var taskId: Int = -1
    )

    private val activeEffects = ConcurrentHashMap<UUID, CastingEffect>()

    /**
     * Start enchantment-style particle effect around the caster
     */
    fun spawnFloatingText(player: Player, spell: SpellType) {
        // Remove any existing effect for this player
        removeFloatingText(player.uniqueId)

        val effect = CastingEffect(
            spell = spell,
            createdAt = System.currentTimeMillis()
        )
        activeEffects[player.uniqueId] = effect

        // Start particle animation task
        startParticleTask(player.uniqueId, effect)
    }

    /**
     * Start particle animation: enchantment glyphs spiral around and rise toward the caster
     */
    private fun startParticleTask(playerId: UUID, effect: CastingEffect) {
        val task = object : BukkitRunnable() {
            var ticks = 0

            override fun run() {
                val currentEffect = activeEffects[playerId]
                if (currentEffect == null) {
                    cancel()
                    return
                }

                val player = plugin.server.getPlayer(playerId)
                if (player == null || !player.isOnline) {
                    removeFloatingText(playerId)
                    cancel()
                    return
                }

                val world = player.world
                val playerLoc = player.location

                // Spawn enchantment glyphs based on spell circle
                val circle = effect.spell.circle.number
                val particleCount = 2 + circle  // More particles for higher circles

                // Spiral pattern around the player
                for (i in 0 until particleCount) {
                    val angle = (ticks * 0.1) + (i * 2 * Math.PI / particleCount)
                    val radius = 0.8 + (sin(ticks * 0.05) * 0.2)  // Pulsing radius
                    val height = 0.5 + (ticks % 40) * 0.05  // Rising motion, resets every 2 seconds

                    val x = playerLoc.x + cos(angle) * radius
                    val z = playerLoc.z + sin(angle) * radius
                    val y = playerLoc.y + height

                    val particleLoc = Location(world, x, y, z)

                    // Enchantment table glyphs (the magical runes)
                    world.spawnParticle(
                        Particle.ENCHANT,
                        particleLoc,
                        1,
                        0.1, 0.1, 0.1,
                        0.5  // Speed - makes them float toward player
                    )
                }

                // Additional effects for higher circles
                if (circle >= 3 && ticks % 4 == 0) {
                    // Add some particles at player's feet
                    world.spawnParticle(
                        Particle.ENCHANT,
                        playerLoc.clone().add(0.0, 0.2, 0.0),
                        5,
                        0.4, 0.1, 0.4,
                        0.3
                    )
                }

                if (circle >= 6 && ticks % 6 == 0) {
                    // Add END_ROD for higher circle spells
                    world.spawnParticle(
                        Particle.END_ROD,
                        playerLoc.clone().add(0.0, 1.5, 0.0),
                        2,
                        0.3, 0.3, 0.3,
                        0.02
                    )
                }

                if (circle >= 7 && ticks % 8 == 0) {
                    // Add SPELL_WITCH for highest circles
                    world.spawnParticle(
                        Particle.WITCH,
                        playerLoc.clone().add(0.0, 1.8, 0.0),
                        3,
                        0.2, 0.2, 0.2,
                        0.05
                    )
                }

                ticks++
            }
        }

        task.runTaskTimer(plugin, 0L, 2L)  // Every 2 ticks (10 times per second)
        effect.taskId = task.taskId
    }

    /**
     * Remove particle effect for a player
     */
    fun removeFloatingText(playerId: UUID) {
        val effect = activeEffects.remove(playerId) ?: return

        // Cancel animation task
        if (effect.taskId != -1) {
            plugin.server.scheduler.cancelTask(effect.taskId)
        }
    }

    /**
     * Check if player has active effect
     */
    fun hasFloatingText(playerId: UUID): Boolean {
        return activeEffects.containsKey(playerId)
    }

    /**
     * Clean up all effects (called on plugin disable)
     */
    fun cleanup() {
        activeEffects.keys.toList().forEach { playerId ->
            removeFloatingText(playerId)
        }
    }
}
