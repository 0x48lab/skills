package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TargetManager(private val plugin: Skills) {
    private val targetingStates: MutableMap<UUID, TargetingState> = ConcurrentHashMap()

    /**
     * Start targeting mode for a player
     */
    fun startTargeting(player: Player, action: TargetingAction): Boolean {
        // Cancel any existing targeting
        cancelTargeting(player.uniqueId)

        val state = TargetingState(
            playerId = player.uniqueId,
            action = action,
            timeoutMs = plugin.skillsConfig.targetingTimeout
        )

        targetingStates[player.uniqueId] = state

        // Notify player based on action type
        val messageKey = when (action) {
            is TargetingAction.CastSpell -> MessageKey.MAGIC_TARGETING
            is TargetingAction.Evaluate -> MessageKey.MAGIC_TARGETING
            is TargetingAction.Lore -> MessageKey.MAGIC_TARGETING
            is TargetingAction.Tame -> MessageKey.MAGIC_TARGETING
            is TargetingAction.Snoop -> MessageKey.MAGIC_TARGETING
            is TargetingAction.Detect -> MessageKey.MAGIC_TARGETING
        }

        val actionName = when (action) {
            is TargetingAction.CastSpell -> action.spell.displayName
            is TargetingAction.Evaluate -> "Evaluate"
            is TargetingAction.Lore -> "Animal Lore"
            is TargetingAction.Tame -> "Tame"
            is TargetingAction.Snoop -> "Snoop"
            is TargetingAction.Detect -> "Detect Hidden"
        }

        plugin.messageSender.sendActionBar(
            player, messageKey,
            "action" to actionName,
            "seconds" to (state.timeoutMs / 1000)
        )

        return true
    }

    /**
     * Check if player is in targeting mode
     */
    fun isTargeting(playerId: UUID): Boolean {
        val state = targetingStates[playerId] ?: return false
        if (state.isExpired()) {
            cancelTargeting(playerId)
            return false
        }
        return true
    }

    /**
     * Get current targeting state
     */
    fun getTargetingState(playerId: UUID): TargetingState? {
        val state = targetingStates[playerId]
        if (state?.isExpired() == true) {
            cancelTargeting(playerId)
            return null
        }
        return state
    }

    /**
     * Process a target selection (left-click on entity)
     */
    fun processEntityTarget(player: Player, target: Entity): TargetResult {
        val state = getTargetingState(player.uniqueId) ?: return TargetResult.Expired

        // Validate target based on action
        val isValid = when (val action = state.action) {
            is TargetingAction.CastSpell -> validateSpellTarget(action.spell, target, player)
            is TargetingAction.Evaluate -> target is Player
            is TargetingAction.Lore -> true // Any living entity
            is TargetingAction.Tame -> true // Will be validated in taming handler
            is TargetingAction.Snoop -> target is Player && target != player
            is TargetingAction.Detect -> false // Detect doesn't target entities
        }

        if (!isValid) {
            plugin.messageSender.send(player, MessageKey.MAGIC_INVALID_TARGET)
            return TargetResult.InvalidTarget
        }

        // Clear targeting state
        targetingStates.remove(player.uniqueId)

        return TargetResult.EntityTarget(target)
    }

    /**
     * Process a target selection (left-click on block/location)
     */
    fun processLocationTarget(player: Player, location: org.bukkit.Location): TargetResult {
        val state = getTargetingState(player.uniqueId) ?: return TargetResult.Expired

        // Validate location-based targeting
        val isValid = when (val action = state.action) {
            is TargetingAction.CastSpell ->
                action.spell.targetType == SpellTargetType.TARGET_LOCATION ||
                        action.spell.targetType == SpellTargetType.AREA
            is TargetingAction.Detect -> true // Detect uses location
            else -> false
        }

        if (!isValid) {
            // For entity-targeting actions, clicking a block is invalid
            plugin.messageSender.send(player, MessageKey.MAGIC_INVALID_TARGET)
            return TargetResult.InvalidTarget
        }

        // Clear targeting state
        targetingStates.remove(player.uniqueId)

        return TargetResult.LocationTarget(location)
    }

    /**
     * Cancel targeting (right-click)
     */
    fun cancelTargeting(playerId: UUID): Boolean {
        val state = targetingStates.remove(playerId)
        if (state != null) {
            val player = plugin.server.getPlayer(playerId)
            player?.let {
                plugin.messageSender.send(it, MessageKey.MAGIC_CANCELLED)
            }
            return true
        }
        return false
    }

    /**
     * Check timeout for a targeting state
     */
    fun checkTimeout(playerId: UUID) {
        val state = targetingStates[playerId]
        if (state?.isExpired() == true) {
            targetingStates.remove(playerId)
            val player = plugin.server.getPlayer(playerId)
            player?.let {
                plugin.messageSender.send(it, MessageKey.MAGIC_TARGET_TIMEOUT)
            }
        }
    }

    /**
     * Validate spell target
     */
    private fun validateSpellTarget(spell: SpellType, target: Entity, caster: Player): Boolean {
        return when (spell.targetType) {
            SpellTargetType.TARGET_ENTITY -> true
            SpellTargetType.SELF -> target == caster
            else -> false
        }
    }

    /**
     * Clean up expired targeting states (call periodically)
     */
    fun cleanupExpired() {
        val expired = targetingStates.entries.filter { it.value.isExpired() }
        expired.forEach { (playerId, _) ->
            targetingStates.remove(playerId)
            val player = plugin.server.getPlayer(playerId)
            player?.let {
                plugin.messageSender.send(it, MessageKey.MAGIC_TARGET_TIMEOUT)
            }
        }
    }
}
