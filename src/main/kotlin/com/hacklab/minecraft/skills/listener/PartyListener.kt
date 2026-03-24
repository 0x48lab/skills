package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class PartyListener(private val plugin: Skills) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val party = plugin.partyManager.getParty(player.uniqueId) ?: return

        // Cancel pending teleport
        plugin.partyTeleportManager.cancelTeleport(player.uniqueId)

        // Notify online party members
        for (memberId in party.members) {
            if (memberId == player.uniqueId) continue
            val member = Bukkit.getPlayer(memberId) ?: continue
            plugin.messageSender.send(member, MessageKey.PARTY_MEMBER_LEFT, "player" to "${player.name} §7(offline)")
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val party = plugin.partyManager.getParty(player.uniqueId) ?: return

        // Notify online party members
        for (memberId in party.members) {
            if (memberId == player.uniqueId) continue
            val member = Bukkit.getPlayer(memberId) ?: continue
            plugin.messageSender.send(member, MessageKey.PARTY_MEMBER_JOINED, "player" to "${player.name} §7(online)")
        }

        // Re-apply party teams
        plugin.partyManager.updatePartyTeams(party)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!plugin.partyTeleportManager.hasPendingTeleport(player.uniqueId)) return

        val distance = event.from.distance(event.to)
        if (distance > 0.5) {
            plugin.partyTeleportManager.cancelTeleport(player.uniqueId)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (event.isCancelled) return

        // Record damage time for combat cooldown
        plugin.partyTeleportManager.recordDamage(player.uniqueId)

        // Cancel pending teleport
        if (plugin.partyTeleportManager.hasPendingTeleport(player.uniqueId)) {
            plugin.partyTeleportManager.cancelTeleport(player.uniqueId)
        }
    }
}
