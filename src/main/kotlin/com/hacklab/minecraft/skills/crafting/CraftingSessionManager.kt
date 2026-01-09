package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages crafting sessions for batch craft quality tracking and feedback.
 *
 * When a player shift-clicks to craft multiple items, this manager:
 * 1. Creates a session to track all crafted items
 * 2. Records the quality of each crafted item
 * 3. After 250ms of inactivity, ends the session and shows summary
 * 4. Provides HQ/EX feedback with sounds and particles
 */
class CraftingSessionManager(private val plugin: Skills) {

    private val sessions = ConcurrentHashMap<UUID, CraftingSession>()
    private val pendingTasks = ConcurrentHashMap<UUID, BukkitTask>()

    companion object {
        /** Session timeout in milliseconds */
        const val SESSION_TIMEOUT_MS = 250L
        /** Session timeout in ticks (250ms = 5 ticks) */
        const val SESSION_TIMEOUT_TICKS = 5L
    }

    /**
     * Get or create a crafting session for a player
     */
    fun getOrCreateSession(player: Player): CraftingSession {
        return sessions.computeIfAbsent(player.uniqueId) {
            CraftingSession(player.uniqueId)
        }
    }

    /**
     * Record a craft event with quality
     * @param player The player who crafted
     * @param quality The quality of the crafted item
     * @param skillGained Whether skill was gained this craft
     */
    fun recordCraft(player: Player, quality: QualityType, skillGained: Boolean) {
        val session = getOrCreateSession(player)
        session.recordQuality(quality)
        session.recordSkillGainAttempt(skillGained)

        // Schedule session end (cancels previous task if exists)
        scheduleSessionEnd(player)
    }

    /**
     * Schedule the session to end after timeout
     * Cancels any existing pending task for this player
     */
    fun scheduleSessionEnd(player: Player) {
        // Cancel existing task
        pendingTasks[player.uniqueId]?.cancel()

        // Schedule new task
        val task = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            endSession(player)
        }, SESSION_TIMEOUT_TICKS)

        pendingTasks[player.uniqueId] = task
    }

    /**
     * End a crafting session and send feedback
     * @return The ended session, or null if no session existed
     */
    fun endSession(player: Player): CraftingSession? {
        pendingTasks.remove(player.uniqueId)?.cancel()
        val session = sessions.remove(player.uniqueId) ?: return null

        // Only send feedback if player is still online
        if (!player.isOnline) return session

        // Send summary message for batch crafts
        if (session.isBatchCraft) {
            sendSummaryMessage(player, session)
        }

        // Send HQ/EX feedback based on highest quality achieved
        sendQualityFeedback(player, session)

        return session
    }

    /**
     * Send quality summary message for batch crafts
     */
    private fun sendSummaryMessage(player: Player, session: CraftingSession) {
        plugin.messageSender.send(
            player, MessageKey.CRAFTING_QUALITY_SUMMARY,
            "ex" to session.exCount,
            "hq" to session.hqCount,
            "nq" to session.nqCount,
            "lq" to session.lqCount
        )
    }

    /**
     * Send HQ/EX feedback with sound and particles
     * For batch crafts, only the highest quality feedback is shown
     */
    fun sendQualityFeedback(player: Player, session: CraftingSession) {
        when (session.highestQuality) {
            QualityType.EXCEPTIONAL -> sendExFeedback(player)
            QualityType.HIGH_QUALITY -> sendHqFeedback(player)
            else -> { /* No special feedback for NQ/LQ */ }
        }
    }

    /**
     * Send HQ feedback (sound + message)
     */
    private fun sendHqFeedback(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f)
        plugin.messageSender.send(player, MessageKey.CRAFTING_QUALITY_HQ)
    }

    /**
     * Send EX feedback (sound + message + particles)
     */
    private fun sendExFeedback(player: Player) {
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        plugin.messageSender.send(player, MessageKey.CRAFTING_QUALITY_EX)

        // Spawn enchantment particles around the player
        player.world.spawnParticle(
            Particle.ENCHANT,
            player.location.add(0.0, 1.0, 0.0),
            30, // count
            0.5, 0.5, 0.5, // offset
            0.1 // speed
        )
    }

    /**
     * Send immediate feedback for single-item crafts (non-batch)
     * Called directly when not using batch crafting
     */
    fun sendImmediateFeedback(player: Player, quality: QualityType) {
        when (quality) {
            QualityType.EXCEPTIONAL -> sendExFeedback(player)
            QualityType.HIGH_QUALITY -> sendHqFeedback(player)
            else -> { /* No special feedback for NQ/LQ */ }
        }
    }

    /**
     * Check if a player has an active crafting session
     */
    fun hasActiveSession(player: Player): Boolean {
        return sessions.containsKey(player.uniqueId)
    }

    /**
     * Clear all sessions (for plugin disable)
     */
    fun clearAllSessions() {
        pendingTasks.values.forEach { it.cancel() }
        pendingTasks.clear()
        sessions.clear()
    }
}
