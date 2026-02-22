package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HidingManager(private val plugin: Skills) {
    // Track hidden players and their stealth distance
    private val hiddenPlayers: MutableMap<UUID, HiddenState> = ConcurrentHashMap()

    // Timeout checker task
    private var timeoutTask: BukkitTask? = null

    data class HiddenState(
        val startTime: Long,
        val expiresAt: Long,
        var warningShown: Boolean = false,
        var distanceTraveled: Double = 0.0,
        var lastLocation: org.bukkit.Location? = null
    )

    /**
     * Attempt to hide
     */
    fun tryHide(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val hidingSkill = data.getSkillValue(SkillType.HIDING)

        // Success chance = skill value %
        val successChance = hidingSkill
        val roll = Random.nextDouble() * 100

        // Dynamic difficulty based on nearby player count (FR-2)
        val nearbyPlayers = player.getNearbyEntities(10.0, 10.0, 10.0).count { it is Player }
        val hidingDifficulty = (20 + nearbyPlayers * 20).coerceAtMost(80)

        // Try skill gain regardless of success
        plugin.skillManager.tryGainSkill(player, SkillType.HIDING, hidingDifficulty)

        if (roll > successChance) {
            plugin.messageSender.send(player, MessageKey.THIEF_HIDE_FAILED)
            return false
        }

        // Calculate timeout (FR-3)
        val config = plugin.skillsConfig
        val timeoutSeconds = config.hideTimeoutBase + (hidingSkill / 100.0 * (config.hideTimeoutMax - config.hideTimeoutBase))
        val expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000).toLong()

        // Apply invisibility
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY,
                Int.MAX_VALUE,  // Managed by timeout checker
                0,
                false,  // No particles
                false   // No icon
            )
        )

        // Track hidden state
        hiddenPlayers[player.uniqueId] = HiddenState(
            startTime = System.currentTimeMillis(),
            expiresAt = expiresAt,
            lastLocation = player.location
        )

        plugin.messageSender.send(player, MessageKey.THIEF_HIDE_SUCCESS)
        return true
    }

    /**
     * Check if player is hidden
     */
    fun isHidden(playerId: UUID): Boolean {
        return hiddenPlayers.containsKey(playerId)
    }

    /**
     * Break hiding (called when player attacks, takes damage, etc.)
     */
    fun breakHiding(player: Player, reason: String = "action") {
        if (!isHidden(player.uniqueId)) return

        hiddenPlayers.remove(player.uniqueId)
        player.removePotionEffect(PotionEffectType.INVISIBILITY)

        if (reason == "timeout") {
            plugin.messageSender.send(player, MessageKey.THIEF_HIDE_TIMEOUT_EXPIRED)
            // Apply short cooldown (3 seconds) for timeout break
            plugin.cooldownManager.setCooldown(player.uniqueId, CooldownAction.HIDE)
        } else {
            plugin.messageSender.send(player, MessageKey.THIEF_HIDE_BROKEN)
        }
    }

    /**
     * Process movement while hidden (Stealth)
     */
    fun processMovement(player: Player, distance: Double): Boolean {
        val state = hiddenPlayers[player.uniqueId] ?: return true

        // Update distance traveled
        state.distanceTraveled += distance
        state.lastLocation = player.location

        // Get max stealth distance (FR-1: use configurable divisor)
        val data = plugin.playerDataManager.getPlayerData(player)
        val stealthSkill = data.getSkillValue(SkillType.STEALTH)
        val maxDistance = stealthSkill / plugin.skillsConfig.stealthDistanceDivisor

        // Dynamic difficulty based on distance ratio (FR-2)
        if (distance > 0.1) {
            val stealthDifficulty = ((state.distanceTraveled / maxDistance) * 80).toInt().coerceIn(1, 80)
            plugin.skillManager.tryGainSkill(player, SkillType.STEALTH, stealthDifficulty)
        }

        // Check if exceeded distance
        if (state.distanceTraveled > maxDistance) {
            breakHiding(player, "distance")
            plugin.messageSender.send(player, MessageKey.THIEF_STEALTH_ENDED)
            return false
        }

        // Show remaining distance
        val remaining = maxDistance - state.distanceTraveled
        plugin.messageSender.sendActionBar(
            player, MessageKey.THIEF_STEALTH_DISTANCE,
            "distance" to String.format("%.1f", remaining)
        )

        return true
    }

    /**
     * Check if player is sprinting (breaks hiding)
     */
    fun checkSprinting(player: Player): Boolean {
        if (player.isSprinting && isHidden(player.uniqueId)) {
            breakHiding(player, "sprinting")
            return false
        }
        return true
    }

    /**
     * Get hidden state for a player
     */
    fun getHiddenState(playerId: UUID): HiddenState? = hiddenPlayers[playerId]

    /**
     * Clean up when player disconnects
     */
    fun removePlayer(playerId: UUID) {
        hiddenPlayers.remove(playerId)
    }

    /**
     * Start the timeout checker task (FR-3)
     * Runs every second (20 ticks) to check for expired hiding states
     */
    fun startTimeoutChecker() {
        timeoutTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val now = System.currentTimeMillis()
            val warningMs = plugin.skillsConfig.hideTimeoutWarning * 1000L

            for ((uuid, state) in hiddenPlayers.toMap()) {
                val player = plugin.server.getPlayer(uuid) ?: continue
                val remainingMs = state.expiresAt - now

                // Show warning when approaching timeout
                if (!state.warningShown && remainingMs in 1..warningMs) {
                    state.warningShown = true
                    val remainingSec = (remainingMs / 1000).toInt().coerceAtLeast(1)
                    plugin.messageSender.sendActionBar(
                        player, MessageKey.THIEF_HIDE_TIMEOUT_WARNING,
                        "seconds" to remainingSec.toString()
                    )
                }

                // Expire hiding
                if (now >= state.expiresAt) {
                    breakHiding(player, "timeout")
                }
            }
        }, 20L, 20L)
    }

    /**
     * Stop the timeout checker task
     */
    fun stopTimeoutChecker() {
        timeoutTask?.cancel()
        timeoutTask = null
    }

    /**
     * Reveal all hidden players in a specified radius
     * Returns the list of revealed players
     * @param caster Optional caster to exclude from reveal (for self-cast spells)
     */
    fun revealInArea(center: org.bukkit.Location, radius: Double, caster: Player? = null): List<Player> {
        val revealed = mutableListOf<Player>()
        val radiusSquared = radius * radius

        // Find all hidden players within radius
        for ((uuid, _) in hiddenPlayers.toMap()) {
            // Skip caster
            if (caster != null && uuid == caster.uniqueId) continue

            val player = plugin.server.getPlayer(uuid) ?: continue

            // Must be in same world
            if (player.world != center.world) continue

            // Check distance
            if (player.location.distanceSquared(center) <= radiusSquared) {
                breakHiding(player, "revealed")
                revealed.add(player)
            }
        }

        return revealed
    }
}
