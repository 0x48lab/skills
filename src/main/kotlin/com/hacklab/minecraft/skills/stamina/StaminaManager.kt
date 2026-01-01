package com.hacklab.minecraft.skills.stamina

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages internal stamina system (Monster Hunter style).
 *
 * Stamina is consumed while sprinting and regenerates when not sprinting.
 * When stamina is depleted:
 * - Walking speed is reduced (setWalkSpeed)
 * - Jumping is disabled (Jump Boost -200)
 * - Player pants (visual/audio effect)
 * - Must recover to threshold before normal movement returns
 *
 * Focus skill reduces stamina consumption and increases regeneration.
 */
class StaminaManager(private val plugin: Skills) {

    companion object {
        // Stamina consumption per tick while sprinting (20 ticks = 1 second)
        const val STAMINA_CONSUMPTION_PER_TICK = 1.0  // 20/sec base consumption

        // Stamina regeneration per tick when not sprinting
        const val STAMINA_REGEN_PER_TICK = 2.0  // 40/sec base regen

        // Minimum stamina required to recover from exhausted state (Monster Hunter style)
        const val STAMINA_RECOVERY_THRESHOLD = 50.0

        // Update interval in ticks
        const val UPDATE_INTERVAL_TICKS = 1L

        // Panting effect interval in ticks
        const val PANTING_INTERVAL_TICKS = 10

        // Normal walking speed
        const val NORMAL_WALK_SPEED = 0.2f

        // Exhausted walking speed (half speed)
        const val EXHAUSTED_WALK_SPEED = 0.1f
    }

    // Track which players are in exhausted state
    private val exhaustedPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // Track panting animation tick counter
    private val pantingCounter: MutableMap<UUID, Int> = ConcurrentHashMap()

    // Track sprinting duration for Focus skill gain
    private val sprintDuration: MutableMap<UUID, Int> = ConcurrentHashMap()

    // Store original walk speed for each player
    private val originalWalkSpeed: MutableMap<UUID, Float> = ConcurrentHashMap()

    private var updateTask: BukkitRunnable? = null

    /**
     * Start the stamina update task
     */
    fun startUpdateTask() {
        updateTask = object : BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { player ->
                    updateStamina(player)
                }
            }
        }
        updateTask?.runTaskTimer(plugin, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS)
    }

    /**
     * Stop the stamina update task
     */
    fun stopUpdateTask() {
        updateTask?.cancel()
        updateTask = null
    }

    /**
     * Update stamina for a player each tick
     */
    private fun updateStamina(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        val isExhausted = exhaustedPlayers.contains(player.uniqueId)

        // FR-1: Force stop sprinting if player is sprinting but cannot sprint
        // This catches cases where PlayerToggleSprintEvent was bypassed (e.g., jumping)
        if (player.isSprinting && !canSprint(player)) {
            player.isSprinting = false
        }

        // Re-check sprinting state after potential force-stop
        val isSprinting = player.isSprinting

        // If exhausted, handle recovery
        if (isExhausted) {
            // Panting effect
            val counter = pantingCounter.getOrPut(player.uniqueId) { 0 }
            if (counter % PANTING_INTERVAL_TICKS == 0) {
                playPantingEffect(player)
            }
            pantingCounter[player.uniqueId] = counter + 1

            // Regenerate stamina (fixed rate, ignore Focus bonus when exhausted)
            if (data.stamina < data.maxStamina) {
                data.stamina = (data.stamina + STAMINA_REGEN_PER_TICK).coerceAtMost(data.maxStamina)
            }

            // Check if recovered enough to exit exhausted state
            if (data.stamina >= STAMINA_RECOVERY_THRESHOLD) {
                removeExhaustedState(player)
                // Recovery sound
                player.world.playSound(player.location, Sound.ENTITY_PLAYER_BREATH, 0.5f, 1.2f)
            }
        } else if (isSprinting) {
            // Consume stamina while sprinting
            val consumed = data.consumeStamina(STAMINA_CONSUMPTION_PER_TICK)

            if (!consumed || data.stamina <= 0) {
                // Become exhausted
                applyExhaustedState(player)
                sprintDuration.remove(player.uniqueId)
                // Exhaustion sound
                player.world.playSound(player.location, Sound.ENTITY_PLAYER_BREATH, 0.8f, 0.6f)
            } else {
                // Track sprint duration for Focus skill gain
                val duration = sprintDuration.getOrPut(player.uniqueId) { 0 }
                sprintDuration[player.uniqueId] = duration + 1

                // Focus skill gain every 100 ticks (5 seconds) of sprinting
                if ((duration + 1) % 100 == 0) {
                    plugin.skillManager.tryGainSkill(
                        player,
                        SkillType.FOCUS,
                        difficulty = 30  // Medium difficulty
                    )
                }
            }
        } else {
            // Not sprinting, not exhausted - regenerate normally
            if (data.stamina < data.maxStamina) {
                data.restoreStamina(STAMINA_REGEN_PER_TICK)
            }
            sprintDuration.remove(player.uniqueId)
        }
    }

    /**
     * Apply exhausted state - reduce walk speed and disable jumping
     */
    private fun applyExhaustedState(player: Player) {
        if (exhaustedPlayers.contains(player.uniqueId)) return

        exhaustedPlayers.add(player.uniqueId)
        pantingCounter[player.uniqueId] = 0

        // Store original walk speed
        originalWalkSpeed[player.uniqueId] = player.walkSpeed

        // Reduce walking speed
        player.walkSpeed = EXHAUSTED_WALK_SPEED

        // Stop sprinting and sneaking
        player.isSprinting = false
        player.isSneaking = false

        // Disable jumping with Jump Boost -200
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.JUMP_BOOST,
                Int.MAX_VALUE,  // Permanent until removed
                -200,           // Negative level = can't jump
                false,          // No ambient particles
                false,          // No particles
                false           // No icon
            )
        )
    }

    /**
     * Remove exhausted state - restore normal movement
     */
    private fun removeExhaustedState(player: Player) {
        if (!exhaustedPlayers.contains(player.uniqueId)) return

        exhaustedPlayers.remove(player.uniqueId)
        pantingCounter.remove(player.uniqueId)

        // Restore original walk speed
        val originalSpeed = originalWalkSpeed.remove(player.uniqueId) ?: NORMAL_WALK_SPEED
        player.walkSpeed = originalSpeed

        // Remove jump restriction
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)
    }

    /**
     * Play panting effect (visual and audio)
     */
    private fun playPantingEffect(player: Player) {
        // Breath particles
        val eyeLoc = player.eyeLocation
        val direction = eyeLoc.direction.multiply(0.5)
        val particleLoc = eyeLoc.add(direction)

        player.world.spawnParticle(
            Particle.CLOUD,
            particleLoc,
            3,
            0.1, 0.1, 0.1,
            0.02
        )

        // Panting sound (alternating between two pitches for breathing rhythm)
        val counter = pantingCounter[player.uniqueId] ?: 0
        val pitch = if ((counter / PANTING_INTERVAL_TICKS) % 2 == 0) 0.8f else 1.0f
        player.world.playSound(player.location, Sound.ENTITY_WOLF_PANT, 0.4f, pitch)
    }

    /**
     * Check if player can start sprinting (has enough stamina and not exhausted)
     */
    fun canSprint(player: Player): Boolean {
        if (exhaustedPlayers.contains(player.uniqueId)) {
            return false
        }
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.stamina >= STAMINA_RECOVERY_THRESHOLD
    }

    /**
     * Check if player is currently exhausted
     */
    fun isExhausted(player: Player): Boolean {
        return exhaustedPlayers.contains(player.uniqueId)
    }

    /**
     * Get current stamina for a player
     */
    fun getStamina(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).stamina
    }

    /**
     * Get max stamina for a player
     */
    fun getMaxStamina(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).maxStamina
    }

    /**
     * Cleanup when player leaves
     */
    fun cleanup(playerId: UUID) {
        sprintDuration.remove(playerId)
        exhaustedPlayers.remove(playerId)
        pantingCounter.remove(playerId)
        originalWalkSpeed.remove(playerId)
    }

    /**
     * Initialize stamina for a player (on join)
     */
    fun initializePlayer(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.updateMaxStats()
        // Start with full stamina if not loaded from database
        if (data.stamina <= 0) {
            data.stamina = data.maxStamina
        }
        // Clear any exhausted state and restore normal movement
        removeExhaustedState(player)
    }
}
