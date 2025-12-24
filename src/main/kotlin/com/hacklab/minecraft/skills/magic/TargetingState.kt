package com.hacklab.minecraft.skills.magic

import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.*

/**
 * Represents the current targeting state for a player
 */
sealed class TargetingAction {
    data class CastSpell(val spell: SpellType, val useScroll: Boolean = false) : TargetingAction()
    data object Evaluate : TargetingAction()
    data object Lore : TargetingAction()
    data object Tame : TargetingAction()
    data object Snoop : TargetingAction()
    data object Detect : TargetingAction()
}

data class TargetingState(
    val playerId: UUID,
    val action: TargetingAction,
    val startTime: Long = System.currentTimeMillis(),
    val timeoutMs: Long = 10000 // 10 seconds default
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - startTime > timeoutMs

    fun getRemainingSeconds(): Int {
        val remaining = timeoutMs - (System.currentTimeMillis() - startTime)
        return (remaining / 1000).toInt().coerceAtLeast(0)
    }
}

sealed class TargetResult {
    data class EntityTarget(val entity: Entity) : TargetResult()
    data class LocationTarget(val location: Location) : TargetResult()
    data object Cancelled : TargetResult()
    data object Expired : TargetResult()
    data object InvalidTarget : TargetResult()
}
