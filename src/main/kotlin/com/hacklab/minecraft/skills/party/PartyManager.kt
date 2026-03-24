package com.hacklab.minecraft.skills.party

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PartyManager(private val plugin: Skills) {

    // Core data stores
    private val parties: MutableMap<UUID, Party> = ConcurrentHashMap()
    private val playerPartyMap: MutableMap<UUID, UUID> = ConcurrentHashMap()
    private val pendingInvites: MutableMap<UUID, PartyInvite> = ConcurrentHashMap()

    private var cleanupTask: BukkitTask? = null

    fun initialize() {
        startCleanupTask()
    }

    fun cleanup() {
        cleanupTask?.cancel()
        parties.clear()
        playerPartyMap.clear()
        pendingInvites.clear()
    }

    // === Query Methods ===

    fun getParty(playerId: UUID): Party? {
        val partyId = playerPartyMap[playerId] ?: return null
        return parties[partyId]
    }

    fun getPartyById(partyId: UUID): Party? = parties[partyId]

    fun isInParty(playerId: UUID): Boolean = playerPartyMap.containsKey(playerId)

    fun isInSameParty(player1: UUID, player2: UUID): Boolean {
        val partyId1 = playerPartyMap[player1] ?: return false
        val partyId2 = playerPartyMap[player2] ?: return false
        return partyId1 == partyId2
    }

    fun getOnlineMembers(party: Party): List<Player> {
        return party.members.mapNotNull { Bukkit.getPlayer(it) }
    }

    fun getPendingInvite(inviteeId: UUID): PartyInvite? {
        val invite = pendingInvites[inviteeId] ?: return null
        if (invite.isExpired()) {
            pendingInvites.remove(inviteeId)
            return null
        }
        return invite
    }

    // === Party Lifecycle ===

    fun createParty(leader: Player): Party {
        val party = Party(leaderId = leader.uniqueId)
        parties[party.id] = party
        playerPartyMap[leader.uniqueId] = party.id
        updatePartyTeams(party)
        return party
    }

    fun invitePlayer(inviter: Player, target: Player): Boolean {
        val config = plugin.skillsConfig

        // Validate target
        if (inviter.uniqueId == target.uniqueId) {
            plugin.messageSender.send(inviter, MessageKey.PARTY_SELF_INVITE)
            return false
        }
        if (isInParty(target.uniqueId)) {
            plugin.messageSender.send(inviter, MessageKey.PARTY_TARGET_ALREADY_IN, "player" to target.name)
            return false
        }

        // Get or create party
        var party = getParty(inviter.uniqueId)
        if (party == null) {
            party = createParty(inviter)
            plugin.messageSender.send(inviter, MessageKey.PARTY_CREATED)
        }

        // Check party size
        if (party.isFull(config.partyMaxSize)) {
            plugin.messageSender.send(inviter, MessageKey.PARTY_FULL, "max" to config.partyMaxSize)
            return false
        }

        // Create invite
        val invite = PartyInvite(
            inviterId = inviter.uniqueId,
            inviteeId = target.uniqueId,
            partyId = party.id,
            expiresAt = System.currentTimeMillis() + config.partyInviteTimeout * 1000L
        )
        pendingInvites[target.uniqueId] = invite

        // Send clickable invite message to target
        val inviteMsg = plugin.messageSender.format(target, MessageKey.PARTY_INVITE_RECEIVED, "player" to inviter.name)
        val acceptText = plugin.messageSender.format(target, MessageKey.PARTY_INVITE_ACCEPT)
        val declineText = plugin.messageSender.format(target, MessageKey.PARTY_INVITE_DECLINE)

        val message = Component.text(inviteMsg)
            .append(Component.text(acceptText)
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/party accept")))
            .append(Component.text(" "))
            .append(Component.text(declineText)
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/party decline")))

        target.sendMessage(message)
        plugin.messageSender.send(inviter, MessageKey.PARTY_INVITE_SENT, "player" to target.name)
        return true
    }

    fun acceptInvite(player: Player): Boolean {
        if (isInParty(player.uniqueId)) {
            plugin.messageSender.send(player, MessageKey.PARTY_ALREADY_IN)
            return false
        }

        val invite = getPendingInvite(player.uniqueId)
        if (invite == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_NO_INVITE)
            return false
        }

        val party = parties[invite.partyId]
        if (party == null) {
            pendingInvites.remove(player.uniqueId)
            plugin.messageSender.send(player, MessageKey.PARTY_NO_INVITE)
            return false
        }

        if (party.isFull(plugin.skillsConfig.partyMaxSize)) {
            plugin.messageSender.send(player, MessageKey.PARTY_FULL, "max" to plugin.skillsConfig.partyMaxSize)
            pendingInvites.remove(player.uniqueId)
            return false
        }

        // Add to party
        party.members.add(player.uniqueId)
        playerPartyMap[player.uniqueId] = party.id
        pendingInvites.remove(player.uniqueId)

        // Notify
        plugin.messageSender.send(player, MessageKey.PARTY_JOINED)
        notifyParty(party, MessageKey.PARTY_MEMBER_JOINED, "player" to player.name, exclude = player.uniqueId)
        updatePartyTeams(party)
        return true
    }

    fun declineInvite(player: Player): Boolean {
        val invite = getPendingInvite(player.uniqueId)
        if (invite == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_NO_INVITE)
            return false
        }

        pendingInvites.remove(player.uniqueId)
        plugin.messageSender.send(player, MessageKey.PARTY_INVITE_DECLINE_CONFIRM)

        // Notify inviter
        val inviter = Bukkit.getPlayer(invite.inviterId)
        if (inviter != null) {
            plugin.messageSender.send(inviter, MessageKey.PARTY_INVITE_DECLINED, "player" to player.name)
        }
        return true
    }

    fun leaveParty(player: Player): Boolean {
        val party = getParty(player.uniqueId)
        if (party == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_NOT_IN)
            return false
        }

        removeFromParty(party, player.uniqueId)
        plugin.messageSender.send(player, MessageKey.PARTY_LEFT)
        removePartyTeamForPlayer(player)

        if (party.members.isEmpty()) {
            // Auto-disband
            parties.remove(party.id)
        } else {
            // Leader succession
            if (party.leaderId == player.uniqueId) {
                val newLeader = party.members.first()
                party.leaderId = newLeader
                notifyParty(party, MessageKey.PARTY_LEADER_CHANGED, "player" to (Bukkit.getOfflinePlayer(newLeader).name ?: "Unknown"))
            }
            notifyParty(party, MessageKey.PARTY_MEMBER_LEFT, "player" to player.name)
            updatePartyTeams(party)
        }
        return true
    }

    fun kickPlayer(leader: Player, target: Player): Boolean {
        val party = getParty(leader.uniqueId)
        if (party == null) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_IN)
            return false
        }
        if (!party.isLeader(leader.uniqueId)) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_LEADER)
            return false
        }
        if (leader.uniqueId == target.uniqueId) {
            plugin.messageSender.send(leader, MessageKey.PARTY_SELF_KICK)
            return false
        }
        if (!party.isMember(target.uniqueId)) {
            plugin.messageSender.send(leader, MessageKey.PARTY_TARGET_NOT_MEMBER, "player" to target.name)
            return false
        }

        removeFromParty(party, target.uniqueId)
        plugin.messageSender.send(target, MessageKey.PARTY_KICKED)
        removePartyTeamForPlayer(target)
        notifyParty(party, MessageKey.PARTY_MEMBER_KICKED, "player" to target.name)
        updatePartyTeams(party)
        return true
    }

    fun disbandParty(leader: Player): Boolean {
        val party = getParty(leader.uniqueId)
        if (party == null) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_IN)
            return false
        }
        if (!party.isLeader(leader.uniqueId)) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_LEADER)
            return false
        }

        // Notify and cleanup
        notifyParty(party, MessageKey.PARTY_DISBANDED)

        // Remove team for all members
        for (memberId in party.members) {
            playerPartyMap.remove(memberId)
            val member = Bukkit.getPlayer(memberId)
            if (member != null) {
                removePartyTeamForPlayer(member)
            }
        }
        parties.remove(party.id)
        return true
    }

    fun transferLeader(leader: Player, newLeader: Player): Boolean {
        val party = getParty(leader.uniqueId)
        if (party == null) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_IN)
            return false
        }
        if (!party.isLeader(leader.uniqueId)) {
            plugin.messageSender.send(leader, MessageKey.PARTY_NOT_LEADER)
            return false
        }
        if (!party.isMember(newLeader.uniqueId)) {
            plugin.messageSender.send(leader, MessageKey.PARTY_TARGET_NOT_MEMBER, "player" to newLeader.name)
            return false
        }
        if (leader.uniqueId == newLeader.uniqueId) {
            return false
        }

        party.leaderId = newLeader.uniqueId
        notifyParty(party, MessageKey.PARTY_LEADER_CHANGED, "player" to newLeader.name)
        return true
    }

    // === Scoreboard Team Management ===

    fun updatePartyTeams(party: Party) {
        // Use main scoreboard's team so all players see the color
        // and it doesn't interfere with per-player BELOW_NAME objectives
        val mainBoard = Bukkit.getScoreboardManager().mainScoreboard

        val team = mainBoard.getTeam("skills_party")
            ?: mainBoard.registerNewTeam("skills_party")
        team.color(NamedTextColor.AQUA)

        // Calculate all party member names
        val expectedEntries = party.members
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }
            .toSet()

        // Remove entries no longer in the party
        val currentEntries = team.entries.toSet()
        for (entry in currentEntries) {
            if (entry !in expectedEntries) {
                team.removeEntry(entry)
            }
        }

        // Add new entries
        for (entry in expectedEntries) {
            if (entry !in currentEntries) {
                team.addEntry(entry)
            }
        }

        // Trigger ScoreboardManager to sync the team to per-player scoreboards
        if (plugin.skillsConfig.scoreboardEnabled && runCatching { plugin.scoreboardManager }.isSuccess) {
            for (member in getOnlineMembers(party)) {
                plugin.scoreboardManager.syncTeamsForPlayer(member)
            }
        }
    }

    private fun removePartyTeamForPlayer(player: Player) {
        val mainBoard = Bukkit.getScoreboardManager().mainScoreboard
        val team = mainBoard.getTeam("skills_party") ?: return

        // Remove this player from the team
        val playerName = player.name
        if (team.hasEntry(playerName)) {
            team.removeEntry(playerName)
        }

        // If team is empty, unregister it
        if (team.entries.isEmpty()) {
            team.unregister()
        }

        // Sync to per-player scoreboard
        if (plugin.skillsConfig.scoreboardEnabled && runCatching { plugin.scoreboardManager }.isSuccess) {
            plugin.scoreboardManager.syncTeamsForPlayer(player)
        }
    }

    // === Internal Helpers ===

    private fun removeFromParty(party: Party, playerId: UUID) {
        party.members.remove(playerId)
        playerPartyMap.remove(playerId)
    }

    private fun notifyParty(party: Party, key: MessageKey, vararg placeholders: Pair<String, Any>, exclude: UUID? = null) {
        for (memberId in party.members) {
            if (memberId == exclude) continue
            val member = Bukkit.getPlayer(memberId) ?: continue
            plugin.messageSender.send(member, key, *placeholders)
        }
    }

    private fun startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            pendingInvites.entries.removeIf { (_, invite) -> invite.isExpired() }
        }, 100L, 100L) // Every 5 seconds
    }
}
