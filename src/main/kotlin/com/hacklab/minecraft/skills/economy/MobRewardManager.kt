package com.hacklab.minecraft.skills.economy

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

/**
 * Manages mob kill rewards
 *
 * Handles reward calculation, drop chance, and distribution via Vault
 */
class MobRewardManager(private val plugin: Skills) {

    /**
     * Process a mob kill and potentially give reward
     * @return the actual reward amount given (0 if no reward)
     */
    fun processMobKill(player: Player, entityType: EntityType): Double {
        // Check if economy is enabled
        if (!plugin.skillsConfig.economyEnabled) {
            return 0.0
        }

        // Check if Vault is available
        if (!plugin.vaultHook.isEnabled()) {
            return 0.0
        }

        // Get mob stats
        val mobStats = plugin.combatConfig.getMobStats(entityType)

        // Check reward chance
        if (mobStats.rewardChance <= 0) {
            return 0.0
        }

        // Roll for drop
        val roll = ThreadLocalRandom.current().nextInt(100)
        if (plugin.skillsConfig.debugMode) {
            plugin.logger.info("[Economy] ${entityType.name}: roll=$roll, chance=${mobStats.rewardChance}, success=${roll < mobStats.rewardChance}")
        }
        if (roll >= mobStats.rewardChance) {
            return 0.0
        }

        // Calculate random reward amount
        val rewardAmount = if (mobStats.rewardMax > mobStats.rewardMin) {
            ThreadLocalRandom.current().nextInt(
                mobStats.rewardMin,
                mobStats.rewardMax + 1
            ).toDouble()
        } else {
            mobStats.rewardMin.toDouble()
        }

        if (rewardAmount <= 0) {
            return 0.0
        }

        // Check chunk limit
        val chunkLimitManager = plugin.chunkLimitManager
        val remaining = chunkLimitManager.getRemainingCapacity(player.uniqueId, player.location)

        // If chunk limit is enabled and reached
        if (remaining != null && remaining <= 0) {
            // Silently return without message (to avoid macro detection)
            return 0.0
        }

        // Record reward and get actual amount (may be limited)
        val actualAmount = chunkLimitManager.recordReward(
            player.uniqueId,
            player.location,
            rewardAmount
        )

        if (actualAmount <= 0) {
            return 0.0
        }

        // Deposit to player's account
        if (plugin.vaultHook.deposit(player, actualAmount)) {
            // Send reward message to chat
            val formatted = plugin.vaultHook.format(actualAmount)
            plugin.messageSender.send(
                player,
                MessageKey.ECONOMY_MOB_REWARD,
                "amount" to formatted
            )
            return actualAmount
        }

        return 0.0
    }

    /**
     * Get remaining capacity for a player in their current chunk
     */
    fun getRemainingCapacity(player: Player): Double? {
        return plugin.chunkLimitManager.getRemainingCapacity(player.uniqueId, player.location)
    }

    /**
     * Get time until chunk limit resets (in minutes)
     */
    fun getTimeUntilReset(player: Player): Int {
        return plugin.chunkLimitManager.getTimeUntilReset(player.uniqueId, player.location)
    }
}
