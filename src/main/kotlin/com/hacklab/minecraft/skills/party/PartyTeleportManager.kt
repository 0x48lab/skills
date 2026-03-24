package com.hacklab.minecraft.skills.party

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PartyTeleportManager(private val plugin: Skills) {

    data class PendingTeleport(
        val playerId: UUID,
        val targetId: UUID,
        val startLocation: Location,
        val startTime: Long = System.currentTimeMillis(),
        var taskId: Int = -1
    )

    private val pendingTeleports: MutableMap<UUID, PendingTeleport> = ConcurrentHashMap()
    private val lastDamageTimes: MutableMap<UUID, Long> = ConcurrentHashMap()

    fun startTeleport(player: Player, target: Player) {
        val party = plugin.partyManager.getParty(player.uniqueId)
        if (party == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_NOT_IN)
            return
        }
        if (!party.isMember(target.uniqueId)) {
            plugin.messageSender.send(player, MessageKey.PARTY_TARGET_NOT_MEMBER, "player" to target.name)
            return
        }
        if (player.world != target.world) {
            plugin.messageSender.send(player, MessageKey.PARTY_TP_DIFFERENT_WORLD, "player" to target.name)
            return
        }

        // Check combat cooldown
        val lastDamage = lastDamageTimes[player.uniqueId]
        if (lastDamage != null && System.currentTimeMillis() - lastDamage < plugin.skillsConfig.partyTpCombatCooldown * 1000L) {
            plugin.messageSender.send(player, MessageKey.PARTY_TP_IN_COMBAT)
            return
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(player.uniqueId, CooldownAction.PARTY_TP)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(player.uniqueId, CooldownAction.PARTY_TP)
            plugin.messageSender.send(player, MessageKey.PARTY_TP_COOLDOWN, "seconds" to remaining)
            return
        }

        // Cancel any existing teleport
        cancelTeleport(player.uniqueId)

        val castTime = plugin.skillsConfig.partyTpCastTime
        val pending = PendingTeleport(
            playerId = player.uniqueId,
            targetId = target.uniqueId,
            startLocation = player.location.clone()
        )

        // Start countdown
        val task = object : BukkitRunnable() {
            var secondsLeft = castTime

            override fun run() {
                val p = Bukkit.getPlayer(pending.playerId)
                val t = Bukkit.getPlayer(pending.targetId)

                if (p == null || t == null || !p.isOnline || !t.isOnline) {
                    cancelTeleport(pending.playerId)
                    cancel()
                    return
                }

                if (secondsLeft <= 0) {
                    executeTeleport(p, t)
                    cancel()
                    return
                }

                // Show action bar
                plugin.messageSender.sendActionBar(p, MessageKey.PARTY_TP_START,
                    "player" to t.name, "seconds" to secondsLeft)
                secondsLeft--
            }
        }

        val bukkitTask = task.runTaskTimer(plugin, 0L, 20L)
        pending.taskId = bukkitTask.taskId
        pendingTeleports[player.uniqueId] = pending
    }

    private fun executeTeleport(player: Player, target: Player) {
        pendingTeleports.remove(player.uniqueId)

        // Break hiding if hidden
        if (plugin.hidingManager.isHidden(player.uniqueId)) {
            plugin.hidingManager.breakHiding(player, "teleport")
        }

        // Teleport to target location
        val targetLoc = target.location
        player.teleport(targetLoc)

        // Effects
        player.world.spawnParticle(Particle.PORTAL, player.location, 50, 0.5, 1.0, 0.5, 0.1)
        target.world.spawnParticle(Particle.PORTAL, target.location, 30, 0.5, 1.0, 0.5, 0.1)
        player.world.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

        plugin.messageSender.send(player, MessageKey.PARTY_TP_SUCCESS, "player" to target.name)

        // Set cooldown
        plugin.cooldownManager.setCooldown(player.uniqueId, CooldownAction.PARTY_TP)
    }

    fun cancelTeleport(playerId: UUID) {
        val pending = pendingTeleports.remove(playerId) ?: return
        if (pending.taskId >= 0) {
            Bukkit.getScheduler().cancelTask(pending.taskId)
        }
        val player = Bukkit.getPlayer(playerId)
        if (player != null) {
            plugin.messageSender.send(player, MessageKey.PARTY_TP_CANCELLED)
        }
    }

    fun hasPendingTeleport(playerId: UUID): Boolean = pendingTeleports.containsKey(playerId)

    fun getPendingTeleport(playerId: UUID): PendingTeleport? = pendingTeleports[playerId]

    fun recordDamage(playerId: UUID) {
        lastDamageTimes[playerId] = System.currentTimeMillis()
    }

    fun cleanup() {
        for ((_, pending) in pendingTeleports) {
            if (pending.taskId >= 0) {
                Bukkit.getScheduler().cancelTask(pending.taskId)
            }
        }
        pendingTeleports.clear()
        lastDamageTimes.clear()
    }
}
