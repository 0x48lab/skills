package com.hacklab.minecraft.skills.economy

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages chunk-based reward limits to prevent farming
 *
 * Uses 11x11 chunk groups (chunk_radius=5) to limit rewards per time window.
 * Each world has different limits defined in config.yml.
 */
class ChunkLimitManager(private val plugin: Skills) {

    // Player UUID -> (ChunkGroupKey -> RewardData)
    private val playerRewards: MutableMap<UUID, MutableMap<String, ChunkRewardData>> = ConcurrentHashMap()

    /**
     * Data class for tracking rewards in a chunk group
     */
    data class ChunkRewardData(
        var totalEarned: Double,
        var firstEarnTime: Long,
        var lastEarnTime: Long
    )

    /**
     * Get chunk group key for a location
     * Groups chunks into 11x11 regions (when chunk_radius=5)
     */
    private fun getChunkGroupKey(location: Location): String {
        val chunkRadius = plugin.skillsConfig.economyChunkRadius
        val groupSize = chunkRadius * 2 + 1  // 11 when radius=5

        val chunkX = location.chunk.x
        val chunkZ = location.chunk.z
        val worldName = location.world.name

        // Integer division to get group coordinates
        val groupX = if (chunkX >= 0) chunkX / groupSize else (chunkX - groupSize + 1) / groupSize
        val groupZ = if (chunkZ >= 0) chunkZ / groupSize else (chunkZ - groupSize + 1) / groupSize

        return "$worldName:$groupX:$groupZ"
    }

    /**
     * Get the reward limit for a world
     */
    private fun getWorldLimit(worldName: String): Double {
        return plugin.skillsConfig.getWorldRewardLimit(worldName)
    }

    /**
     * Get time window in milliseconds
     */
    private fun getTimeWindowMs(): Long {
        return plugin.skillsConfig.economyTimeWindowMinutes * 60 * 1000L
    }

    /**
     * Check if player can receive reward and get remaining capacity
     * @return remaining capacity (0 if limit reached), or null if limits disabled
     */
    fun getRemainingCapacity(playerId: UUID, location: Location): Double? {
        if (!plugin.skillsConfig.economyChunkLimitEnabled) {
            return null  // No limit
        }

        val chunkKey = getChunkGroupKey(location)
        val worldLimit = getWorldLimit(location.world.name)
        val timeWindow = getTimeWindowMs()
        val now = System.currentTimeMillis()

        val playerData = playerRewards.getOrPut(playerId) { ConcurrentHashMap() }
        val chunkData = playerData[chunkKey]

        if (chunkData == null) {
            return worldLimit  // Fresh start
        }

        // Check if time window has expired
        if (now - chunkData.firstEarnTime > timeWindow) {
            // Reset the data
            playerData.remove(chunkKey)
            return worldLimit
        }

        // Calculate remaining capacity
        val remaining = worldLimit - chunkData.totalEarned
        return remaining.coerceAtLeast(0.0)
    }

    /**
     * Record a reward earned by a player
     * @return the actual amount that was recorded (may be less if limit reached)
     */
    fun recordReward(playerId: UUID, location: Location, amount: Double): Double {
        if (!plugin.skillsConfig.economyChunkLimitEnabled) {
            return amount  // No limit, full amount
        }

        val chunkKey = getChunkGroupKey(location)
        val worldLimit = getWorldLimit(location.world.name)
        val timeWindow = getTimeWindowMs()
        val now = System.currentTimeMillis()

        val playerData = playerRewards.getOrPut(playerId) { ConcurrentHashMap() }
        val chunkData = playerData[chunkKey]

        if (chunkData == null) {
            // First reward in this chunk group
            val actualAmount = amount.coerceAtMost(worldLimit)
            playerData[chunkKey] = ChunkRewardData(
                totalEarned = actualAmount,
                firstEarnTime = now,
                lastEarnTime = now
            )
            return actualAmount
        }

        // Check if time window has expired
        if (now - chunkData.firstEarnTime > timeWindow) {
            // Reset and start fresh
            val actualAmount = amount.coerceAtMost(worldLimit)
            playerData[chunkKey] = ChunkRewardData(
                totalEarned = actualAmount,
                firstEarnTime = now,
                lastEarnTime = now
            )
            return actualAmount
        }

        // Check remaining capacity
        val remaining = worldLimit - chunkData.totalEarned
        if (remaining <= 0) {
            return 0.0  // Limit reached
        }

        val actualAmount = amount.coerceAtMost(remaining)
        chunkData.totalEarned += actualAmount
        chunkData.lastEarnTime = now
        return actualAmount
    }

    /**
     * Get time remaining until limit resets (in minutes)
     */
    fun getTimeUntilReset(playerId: UUID, location: Location): Int {
        if (!plugin.skillsConfig.economyChunkLimitEnabled) {
            return 0
        }

        val chunkKey = getChunkGroupKey(location)
        val timeWindow = getTimeWindowMs()
        val now = System.currentTimeMillis()

        val playerData = playerRewards[playerId] ?: return 0
        val chunkData = playerData[chunkKey] ?: return 0

        val elapsed = now - chunkData.firstEarnTime
        val remaining = timeWindow - elapsed

        return if (remaining > 0) {
            (remaining / 60000).toInt() + 1  // Round up to next minute
        } else {
            0
        }
    }

    /**
     * Cleanup data for a player (on logout)
     */
    fun cleanup(playerId: UUID) {
        playerRewards.remove(playerId)
    }

    /**
     * Cleanup expired entries periodically
     */
    fun cleanupExpired() {
        val timeWindow = getTimeWindowMs()
        val now = System.currentTimeMillis()

        playerRewards.forEach { (_, chunkMap) ->
            chunkMap.entries.removeIf { (_, data) ->
                now - data.firstEarnTime > timeWindow
            }
        }

        // Remove empty player entries
        playerRewards.entries.removeIf { (_, chunkMap) -> chunkMap.isEmpty() }
    }
}
