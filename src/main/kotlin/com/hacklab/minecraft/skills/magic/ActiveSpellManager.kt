package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages all ongoing spell effects with a single BukkitRunnable.
 * Regardless of how many players have active effects, only one task runs.
 * When no effects are active, the task is stopped for zero overhead.
 */
class ActiveSpellManager(private val plugin: Skills) {

    private val activeEffects = ConcurrentHashMap<UUID, MutableList<OngoingSpellEffect>>()
    private var taskId: Int = -1

    /**
     * Add an ongoing effect for a player.
     * Calls onStart() and starts the tick task if not already running.
     */
    fun addEffect(effect: OngoingSpellEffect) {
        val player = Bukkit.getPlayer(effect.playerId) ?: return

        activeEffects.computeIfAbsent(effect.playerId) { mutableListOf() }.add(effect)
        effect.onStart(player)

        if (taskId == -1) {
            startTask()
        }
    }

    /**
     * Cancel all effects for a player with the given reason.
     */
    fun cancelEffects(playerId: UUID, reason: EndReason) {
        val effects = activeEffects.remove(playerId) ?: return
        val player = Bukkit.getPlayer(playerId)

        effects.forEach { effect ->
            if (player != null) {
                effect.onEnd(player, reason)
            }
        }

        if (activeEffects.isEmpty()) {
            stopTask()
        }
    }

    /**
     * Cancel a specific spell type for a player.
     */
    fun cancelEffect(playerId: UUID, spellType: SpellType, reason: EndReason) {
        val effects = activeEffects[playerId] ?: return
        val player = Bukkit.getPlayer(playerId)

        val iterator = effects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            if (effect.spellType == spellType) {
                if (player != null) {
                    effect.onEnd(player, reason)
                }
                iterator.remove()
            }
        }

        // Clean up empty lists
        if (effects.isEmpty()) {
            activeEffects.remove(playerId)
        }

        if (activeEffects.isEmpty()) {
            stopTask()
        }
    }

    /**
     * Check if a player has a specific effect active.
     */
    fun hasEffect(playerId: UUID, spellType: SpellType): Boolean {
        return activeEffects[playerId]?.any { it.spellType == spellType } == true
    }

    /**
     * Check if a player has any active effect.
     */
    fun hasAnyEffect(playerId: UUID): Boolean {
        return activeEffects.containsKey(playerId)
    }

    /**
     * Start the single tick task (20 tick / 1 second interval).
     */
    private fun startTask() {
        if (taskId != -1) return

        taskId = object : BukkitRunnable() {
            override fun run() {
                tickAll()
            }
        }.runTaskTimer(plugin, 20L, 20L).taskId
    }

    /**
     * Stop the tick task when no effects are active.
     */
    private fun stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
    }

    /**
     * Process all active effects for all players.
     */
    private fun tickAll() {
        val toRemove = mutableListOf<Pair<UUID, OngoingSpellEffect>>()

        for ((playerId, effects) in activeEffects) {
            val player = Bukkit.getPlayer(playerId)

            if (player == null || !player.isOnline) {
                // Player offline - mark all effects for removal
                effects.forEach { toRemove.add(playerId to it) }
                continue
            }

            val iterator = effects.iterator()
            while (iterator.hasNext()) {
                val effect = iterator.next()
                val shouldContinue = effect.tick(player)
                if (!shouldContinue) {
                    effect.onEnd(player, EndReason.EXPIRED)
                    iterator.remove()
                }
            }
        }

        // Handle offline players
        for ((playerId, effect) in toRemove) {
            val player = Bukkit.getPlayer(playerId)
            if (player != null) {
                effect.onEnd(player, EndReason.DISCONNECT)
            }
            activeEffects[playerId]?.remove(effect)
        }

        // Clean up empty lists
        activeEffects.entries.removeIf { it.value.isEmpty() }

        // Stop task if no effects remain
        if (activeEffects.isEmpty()) {
            stopTask()
        }
    }

    /**
     * Shutdown: cancel all effects and stop the task.
     * Called from Skills.onDisable().
     */
    fun shutdown() {
        for ((playerId, effects) in activeEffects) {
            val player = Bukkit.getPlayer(playerId)
            effects.forEach { effect ->
                if (player != null) {
                    effect.onEnd(player, EndReason.DISCONNECT)
                }
            }
        }
        activeEffects.clear()
        stopTask()
    }
}
