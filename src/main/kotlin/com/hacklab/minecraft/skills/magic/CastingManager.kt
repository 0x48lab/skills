package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CastingManager(private val plugin: Skills) {

    private val castingStates = ConcurrentHashMap<UUID, CastingState>()

    data class CastingState(
        val spell: SpellType,
        val useScroll: Boolean,
        val startTime: Long,
        val castDuration: Long,
        val startLocation: Location,
        val bossBar: BossBar,
        var phase: CastPhase = CastPhase.CASTING,
        var taskId: Int = -1,
        val preSelectedTarget: Player? = null  // For PLAYER_OR_SELF spells with command argument
    )

    enum class CastPhase {
        CASTING,      // Chanting the spell
        TARGETING     // Selecting target after casting
    }

    /**
     * Start casting a spell
     * @param targetPlayer For PLAYER_OR_SELF spells, pre-selected target from command argument
     */
    fun startCasting(player: Player, spell: SpellType, useScroll: Boolean, targetPlayer: Player? = null): Boolean {
        // Cancel any existing casting
        cancelCasting(player.uniqueId, silent = true)

        // Calculate casting time based on circle and magery skill
        val data = plugin.playerDataManager.getPlayerData(player)
        val magerySkill = data.getSkillValue(com.hacklab.minecraft.skills.skill.SkillType.MAGERY)

        val baseCastTime = plugin.skillsConfig.castingTimeBase
        val circlePenalty = spell.circle.number * 500L
        // Skill reduces casting time (up to 30% reduction at skill 100)
        val skillReduction = (magerySkill / 100.0 * 0.3).coerceIn(0.0, 0.3)
        val castTime = ((baseCastTime + circlePenalty) * (1.0 - skillReduction)).toLong()

        // Create boss bar for casting display
        val bossBar = BossBar.bossBar(
            Component.text("${spell.displayName} ").color(NamedTextColor.AQUA)
                .append(Component.text("Casting...").color(NamedTextColor.WHITE)),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        )
        player.showBossBar(bossBar)

        // Create casting state
        val state = CastingState(
            spell = spell,
            useScroll = useScroll,
            startTime = System.currentTimeMillis(),
            castDuration = castTime,
            startLocation = player.location.clone(),
            bossBar = bossBar,
            preSelectedTarget = targetPlayer
        )
        castingStates[player.uniqueId] = state

        // Start casting progress task
        startCastingProgress(player, state)

        // Announce Power Words (UO-style incantation)
        broadcastPowerWords(player, spell)

        // Play casting start sound
        player.world.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f)

        return true
    }

    private fun startCastingProgress(player: Player, state: CastingState) {
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    cancelCasting(player.uniqueId, silent = true)
                    cancel()
                    return
                }

                val currentState = castingStates[player.uniqueId]
                if (currentState == null) {
                    cancel()
                    return
                }

                val elapsed = System.currentTimeMillis() - currentState.startTime

                when (currentState.phase) {
                    CastPhase.CASTING -> {
                        val progress = (elapsed.toFloat() / currentState.castDuration).coerceIn(0f, 1f)
                        currentState.bossBar.progress(1f - progress)

                        // Casting complete
                        if (elapsed >= currentState.castDuration) {
                            onCastingComplete(player, currentState)
                        }
                    }
                    CastPhase.TARGETING -> {
                        val targetingTime = getTargetingDuration(currentState.spell)
                        val targetingElapsed = elapsed - currentState.castDuration
                        val progress = (targetingElapsed.toFloat() / targetingTime).coerceIn(0f, 1f)
                        currentState.bossBar.progress(1f - progress)

                        // Targeting time expired
                        if (targetingElapsed >= targetingTime) {
                            onTargetingExpired(player, currentState)
                            cancel()
                        }
                    }
                }
            }
        }
        task.runTaskTimer(plugin, 0L, 2L) // Update every 2 ticks (100ms)
        state.taskId = task.taskId
    }

    private fun onCastingComplete(player: Player, state: CastingState) {
        // Check if spell needs targeting
        if (state.spell.targetType == SpellTargetType.SELF ||
            state.spell.targetType == SpellTargetType.NONE ||
            state.spell.targetType == SpellTargetType.TARGET_ITEM) {
            // No targeting needed, cast immediately
            finishCasting(player, state, null, null)
            return
        }

        // PLAYER_OR_SELF: if pre-selected target exists, cast immediately on that target
        if (state.spell.targetType == SpellTargetType.PLAYER_OR_SELF && state.preSelectedTarget != null) {
            finishCasting(player, state, state.preSelectedTarget, null)
            return
        }

        // Switch to targeting phase
        state.phase = CastPhase.TARGETING
        state.bossBar.color(BossBar.Color.RED)
        state.bossBar.name(
            Component.text("${state.spell.displayName} ").color(NamedTextColor.RED)
                .append(Component.text("Select Target!").color(NamedTextColor.YELLOW))
        )
        state.bossBar.progress(1f)

        // Play targeting ready sound
        player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f)

        plugin.messageSender.send(player, MessageKey.MAGIC_SELECT_TARGET)
    }

    private fun onTargetingExpired(player: Player, state: CastingState) {
        plugin.messageSender.send(player, MessageKey.MAGIC_TARGET_TIMEOUT)
        cancelCasting(player.uniqueId, silent = true)

        // Play fail sound
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_BURP, 0.5f, 0.5f)
    }

    /**
     * Get targeting duration based on spell
     */
    private fun getTargetingDuration(spell: SpellType): Long {
        return 5000L // 5 seconds to select target
    }

    /**
     * Check if player has moved too far (cancel casting)
     */
    fun checkMovement(player: Player) {
        val state = castingStates[player.uniqueId] ?: return

        // Only cancel during CASTING phase, not targeting
        if (state.phase != CastPhase.CASTING) return

        val distance = player.location.distance(state.startLocation)
        if (distance > 0.5) { // Moved more than half a block
            cancelCasting(player.uniqueId, silent = false)
            plugin.messageSender.send(player, MessageKey.MAGIC_INTERRUPTED)
        }
    }

    /**
     * Process target selection during targeting phase
     */
    fun processTargetClick(player: Player): Boolean {
        val state = castingStates[player.uniqueId] ?: return false

        if (state.phase != CastPhase.TARGETING) return false

        // Raycast to find target
        val result = when (state.spell.targetType) {
            SpellTargetType.TARGET_ENTITY -> {
                plugin.spellManager.raycastForEntity(player, 30.0)
            }
            SpellTargetType.TARGET_LOCATION, SpellTargetType.AREA -> {
                plugin.spellManager.raycastForLocation(player, 30.0)
            }
            SpellTargetType.PLAYER_OR_SELF -> {
                // For PLAYER_OR_SELF: raycast for player, if not found use self
                val rayResult = plugin.spellManager.raycastForEntity(player, 30.0)
                if (rayResult.entity is Player) {
                    rayResult
                } else {
                    // No player hit - target self
                    SpellManager.RaycastResult(player, player.location)
                }
            }
            else -> return false
        }

        finishCasting(player, state, result.entity, result.location)
        return true
    }

    /**
     * Complete the spell casting
     */
    private fun finishCasting(
        player: Player,
        state: CastingState,
        entityTarget: org.bukkit.entity.LivingEntity?,
        locationTarget: Location?
    ) {
        // Remove boss bar
        player.hideBossBar(state.bossBar)
        castingStates.remove(player.uniqueId)

        // Cancel the progress task
        if (state.taskId != -1) {
            plugin.server.scheduler.cancelTask(state.taskId)
        }

        // Execute the spell
        when (state.spell.targetType) {
            SpellTargetType.SELF -> {
                plugin.spellManager.finalizeCast(player, state.spell, player, null, state.useScroll)
            }
            SpellTargetType.NONE -> {
                plugin.spellManager.finalizeCast(player, state.spell, null, null, state.useScroll)
            }
            SpellTargetType.TARGET_ITEM -> {
                plugin.spellManager.finalizeCast(player, state.spell, null, player.location, state.useScroll)
            }
            else -> {
                plugin.spellManager.finalizeCast(player, state.spell, entityTarget, locationTarget, state.useScroll)
            }
        }
    }

    /**
     * Cancel casting
     */
    fun cancelCasting(playerId: UUID, silent: Boolean = false): Boolean {
        val state = castingStates.remove(playerId) ?: return false

        // Get player and hide boss bar
        plugin.server.getPlayer(playerId)?.let { player ->
            player.hideBossBar(state.bossBar)

            // Clear any pending runebook spell locations
            plugin.runebookListener.clearPending(player)

            if (!silent) {
                player.world.playSound(player.location, Sound.ENTITY_PLAYER_BURP, 0.5f, 0.5f)
            }
        }

        // Cancel the progress task
        if (state.taskId != -1) {
            plugin.server.scheduler.cancelTask(state.taskId)
        }

        return true
    }

    /**
     * Check if player is currently casting
     */
    fun isCasting(playerId: UUID): Boolean {
        return castingStates.containsKey(playerId)
    }

    /**
     * Get current casting state
     */
    fun getCastingState(playerId: UUID): CastingState? {
        return castingStates[playerId]
    }

    /**
     * Clean up on disable
     */
    fun cleanup() {
        castingStates.keys.forEach { playerId ->
            cancelCasting(playerId, silent = true)
        }
    }

    /**
     * Broadcast Power Words to nearby players (UO-style)
     * The caster speaks the incantation aloud
     */
    private fun broadcastPowerWords(caster: Player, spell: SpellType) {
        val powerWords = spell.powerWords
        val message = Component.text("* ${caster.name}: ").color(NamedTextColor.GRAY)
            .append(Component.text(powerWords).color(NamedTextColor.LIGHT_PURPLE).decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC))
            .append(Component.text(" *").color(NamedTextColor.GRAY))

        // Broadcast to nearby players (within 20 blocks)
        val nearbyPlayers = caster.world.getNearbyEntities(caster.location, 20.0, 20.0, 20.0)
            .filterIsInstance<Player>()

        nearbyPlayers.forEach { player ->
            player.sendMessage(message)
        }

        // Also show to the caster themselves if not in the list
        if (caster !in nearbyPlayers) {
            caster.sendMessage(message)
        }
    }
}
