package com.hacklab.minecraft.skills.util

import com.hacklab.minecraft.skills.Skills
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CooldownManager(private val plugin: Skills) {

    // Map of player UUID -> (action -> expiry timestamp)
    private val cooldowns: MutableMap<UUID, MutableMap<CooldownAction, Long>> = ConcurrentHashMap()

    /**
     * Check if a player is on cooldown for an action
     */
    fun isOnCooldown(playerId: UUID, action: CooldownAction): Boolean {
        val playerCooldowns = cooldowns[playerId] ?: return false
        val expiryTime = playerCooldowns[action] ?: return false
        return System.currentTimeMillis() < expiryTime
    }

    /**
     * Get remaining cooldown time in seconds
     */
    fun getRemainingCooldown(playerId: UUID, action: CooldownAction): Int {
        val playerCooldowns = cooldowns[playerId] ?: return 0
        val expiryTime = playerCooldowns[action] ?: return 0
        val remaining = (expiryTime - System.currentTimeMillis()) / 1000
        return remaining.toInt().coerceAtLeast(0)
    }

    /**
     * Set cooldown for a player action
     */
    fun setCooldown(playerId: UUID, action: CooldownAction) {
        val cooldownSeconds = getCooldownDuration(action)
        if (cooldownSeconds <= 0) return

        val playerCooldowns = cooldowns.getOrPut(playerId) { ConcurrentHashMap() }
        playerCooldowns[action] = System.currentTimeMillis() + (cooldownSeconds * 1000L)
    }

    /**
     * Get cooldown duration from config
     */
    private fun getCooldownDuration(action: CooldownAction): Int {
        return plugin.config.getInt("cooldowns.${action.configKey}", action.defaultCooldown)
    }

    /**
     * Clear all cooldowns for a player (e.g., on logout)
     */
    fun clearCooldowns(playerId: UUID) {
        cooldowns.remove(playerId)
    }

    /**
     * Clear expired cooldowns (can be called periodically)
     */
    fun cleanupExpiredCooldowns() {
        val now = System.currentTimeMillis()
        cooldowns.forEach { (_, playerCooldowns) ->
            playerCooldowns.entries.removeIf { it.value < now }
        }
        cooldowns.entries.removeIf { it.value.isEmpty() }
    }
}

enum class CooldownAction(val configKey: String, val defaultCooldown: Int) {
    HIDE("hide", 10),
    DETECT("detect", 5),
    SNOOP("snoop", 5),
    STEAL("steal", 3),
    POISON("poison", 10),
    TAME("tame", 5),
    ANIMAL_LORE("animal_lore", 3),
    ARMS_LORE("arms_lore", 3),
    EVALUATE("evaluate", 5),
    CAST_SPELL("cast_spell", 2),  // Base cooldown between spell casts
    USE_SCROLL("use_scroll", 1),  // Prevent double-firing
    MEDITATION("meditation", 30)
}
