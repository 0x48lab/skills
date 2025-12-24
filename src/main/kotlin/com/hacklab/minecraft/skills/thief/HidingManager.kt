package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HidingManager(private val plugin: Skills) {
    // Track hidden players and their stealth distance
    private val hiddenPlayers: MutableMap<UUID, HiddenState> = ConcurrentHashMap()

    data class HiddenState(
        val startTime: Long,
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

        // Try skill gain regardless of success
        plugin.skillManager.tryGainSkill(player, SkillType.HIDING, 50)

        if (roll > successChance) {
            plugin.messageSender.send(player, MessageKey.THIEF_HIDE_FAILED)
            return false
        }

        // Apply invisibility
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY,
                Int.MAX_VALUE,  // Indefinite until broken
                0,
                false,  // No particles
                false   // No icon
            )
        )

        // Track hidden state
        hiddenPlayers[player.uniqueId] = HiddenState(
            startTime = System.currentTimeMillis(),
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

        plugin.messageSender.send(player, MessageKey.THIEF_HIDE_BROKEN)
    }

    /**
     * Process movement while hidden (Stealth)
     */
    fun processMovement(player: Player, distance: Double): Boolean {
        val state = hiddenPlayers[player.uniqueId] ?: return true

        // Update distance traveled
        state.distanceTraveled += distance
        state.lastLocation = player.location

        // Get max stealth distance
        val data = plugin.playerDataManager.getPlayerData(player)
        val stealthSkill = data.getSkillValue(SkillType.STEALTH)
        val maxDistance = stealthSkill / 10.0  // Max 10 blocks at skill 100

        // Try stealth skill gain
        if (distance > 0.1) {
            plugin.skillManager.tryGainSkill(player, SkillType.STEALTH, 50)
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
}
