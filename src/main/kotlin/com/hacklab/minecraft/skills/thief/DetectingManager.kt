package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Particle
import org.bukkit.entity.Player
import kotlin.random.Random

class DetectingManager(private val plugin: Skills) {

    /**
     * Attempt to detect hidden players in range
     */
    fun tryDetect(player: Player): DetectResult {
        val data = plugin.playerDataManager.getPlayerData(player)
        val detectSkill = data.getSkillValue(SkillType.DETECTING_HIDDEN)

        // Calculate detection range: skill / 10 (max 10 blocks)
        val range = (detectSkill / 10.0).coerceAtMost(plugin.skillsConfig.detectRange)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.DETECTING_HIDDEN, 50)

        // Find hidden players in range
        val nearbyPlayers = player.world.players.filter { other ->
            other != player &&
                    plugin.hidingManager.isHidden(other.uniqueId) &&
                    other.location.distance(player.location) <= range
        }

        if (nearbyPlayers.isEmpty()) {
            plugin.messageSender.send(player, MessageKey.THIEF_DETECT_NONE)
            return DetectResult(found = emptyList(), range = range)
        }

        // Check detection success for each hidden player
        val detected = mutableListOf<Player>()

        for (hidden in nearbyPlayers) {
            val hiddenData = plugin.playerDataManager.getPlayerData(hidden)
            val hidingSkill = hiddenData.getSkillValue(SkillType.HIDING)

            // Detection chance = DetectSkill - HidingSkill + 50, clamped 10-95%
            val detectChance = (detectSkill - hidingSkill + 50).coerceIn(10.0, 95.0)

            if (Random.nextDouble() * 100 < detectChance) {
                // Successfully detected!
                detected.add(hidden)

                // Break their hiding
                plugin.hidingManager.breakHiding(hidden, "detected")

                // Show particle at their location
                player.world.spawnParticle(
                    Particle.FLASH,
                    hidden.location.add(0.0, 1.0, 0.0),
                    1
                )

                plugin.messageSender.send(
                    player, MessageKey.THIEF_DETECT_FOUND,
                    "player" to hidden.name
                )
            }
        }

        return DetectResult(found = detected, range = range)
    }

    /**
     * Get detection range for a player
     */
    fun getDetectionRange(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val detectSkill = data.getSkillValue(SkillType.DETECTING_HIDDEN)
        return (detectSkill / 10.0).coerceAtMost(plugin.skillsConfig.detectRange)
    }
}

data class DetectResult(
    val found: List<Player>,
    val range: Double
)
