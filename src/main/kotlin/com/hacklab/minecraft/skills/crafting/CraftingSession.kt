package com.hacklab.minecraft.skills.crafting

import java.util.UUID

/**
 * Tracks a batch crafting session for quality summary reporting.
 * A session starts when a player crafts an item and ends after 250ms of inactivity.
 */
data class CraftingSession(
    val playerId: UUID,
    val startTime: Long = System.currentTimeMillis(),
    var lastCraftTime: Long = startTime,
    var lqCount: Int = 0,
    var nqCount: Int = 0,
    var hqCount: Int = 0,
    var exCount: Int = 0,
    var skillGainAttempts: Int = 0,
    var skillGainSuccesses: Int = 0
) {
    /**
     * Total number of items crafted in this session
     */
    val totalCount: Int
        get() = lqCount + nqCount + hqCount + exCount

    /**
     * Returns the highest quality achieved in this session
     */
    val highestQuality: QualityType
        get() = when {
            exCount > 0 -> QualityType.EXCEPTIONAL
            hqCount > 0 -> QualityType.HIGH_QUALITY
            lqCount > 0 && nqCount == 0 -> QualityType.LOW_QUALITY
            else -> QualityType.NORMAL_QUALITY
        }

    /**
     * Returns true if this is a batch craft (more than 1 item)
     */
    val isBatchCraft: Boolean
        get() = totalCount > 1

    /**
     * Record a crafted item's quality
     */
    fun recordQuality(quality: QualityType) {
        lastCraftTime = System.currentTimeMillis()
        when (quality) {
            QualityType.LOW_QUALITY -> lqCount++
            QualityType.NORMAL_QUALITY -> nqCount++
            QualityType.HIGH_QUALITY -> hqCount++
            QualityType.EXCEPTIONAL -> exCount++
        }
    }

    /**
     * Record a skill gain attempt
     * @param success Whether the skill gain was successful
     */
    fun recordSkillGainAttempt(success: Boolean) {
        skillGainAttempts++
        if (success) skillGainSuccesses++
    }

    /**
     * Check if the session has expired (no activity for timeout period)
     * @param timeoutMs Timeout in milliseconds (default 250ms)
     */
    fun isExpired(timeoutMs: Long = 250): Boolean {
        return System.currentTimeMillis() - lastCraftTime > timeoutMs
    }
}
