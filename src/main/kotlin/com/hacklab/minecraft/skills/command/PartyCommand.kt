package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PartyCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "invite" -> handleInvite(sender, args)
            "accept" -> plugin.partyManager.acceptInvite(sender)
            "decline" -> plugin.partyManager.declineInvite(sender)
            "leave" -> plugin.partyManager.leaveParty(sender)
            "kick" -> handleKick(sender, args)
            "disband" -> plugin.partyManager.disbandParty(sender)
            "list" -> handleList(sender)
            "leader" -> handleLeader(sender, args)
            "tp" -> handleTeleport(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun handleInvite(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /party invite <player>")
            return
        }
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_TARGET_OFFLINE, "player" to args[1])
            return
        }
        plugin.partyManager.invitePlayer(player, target)
    }

    private fun handleKick(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /party kick <player>")
            return
        }
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_TARGET_OFFLINE, "player" to args[1])
            return
        }
        plugin.partyManager.kickPlayer(player, target)
    }

    private fun handleList(player: Player) {
        val party = plugin.partyManager.getParty(player.uniqueId)
        if (party == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_NOT_IN)
            return
        }

        plugin.messageSender.send(player, MessageKey.PARTY_LIST_HEADER)
        for (memberId in party.members) {
            val isLeader = party.isLeader(memberId)
            val memberPlayer = Bukkit.getPlayer(memberId)

            if (memberPlayer != null) {
                val data = plugin.playerDataManager.getPlayerData(memberPlayer)
                val currentHp = String.format("%.0f", data.internalHp)
                val maxHp = String.format("%.0f", data.maxInternalHp)

                if (isLeader) {
                    plugin.messageSender.send(player, MessageKey.PARTY_LIST_LEADER,
                        "player" to memberPlayer.name, "hp" to currentHp, "maxhp" to maxHp)
                } else {
                    plugin.messageSender.send(player, MessageKey.PARTY_LIST_ENTRY,
                        "player" to memberPlayer.name, "hp" to currentHp, "maxhp" to maxHp)
                }
            } else {
                val offlineName = Bukkit.getOfflinePlayer(memberId).name ?: "Unknown"
                if (isLeader) {
                    plugin.messageSender.send(player, MessageKey.PARTY_LIST_ENTRY_OFFLINE,
                        "player" to "★ $offlineName (Leader)")
                } else {
                    plugin.messageSender.send(player, MessageKey.PARTY_LIST_ENTRY_OFFLINE,
                        "player" to offlineName)
                }
            }
        }
    }

    private fun handleLeader(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /party leader <player>")
            return
        }
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_TARGET_OFFLINE, "player" to args[1])
            return
        }
        plugin.partyManager.transferLeader(player, target)
    }

    private fun handleTeleport(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§cUsage: /party tp <player>")
            return
        }
        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            plugin.messageSender.send(player, MessageKey.PARTY_TARGET_OFFLINE, "player" to args[1])
            return
        }
        plugin.partyTeleportManager.startTeleport(player, target)
    }

    private fun sendUsage(player: Player) {
        player.sendMessage("§6=== Party Commands ===")
        player.sendMessage("§e/party invite <player> §7- Invite a player")
        player.sendMessage("§e/party accept §7- Accept an invite")
        player.sendMessage("§e/party decline §7- Decline an invite")
        player.sendMessage("§e/party leave §7- Leave the party")
        player.sendMessage("§e/party kick <player> §7- Kick a member (leader only)")
        player.sendMessage("§e/party disband §7- Disband the party (leader only)")
        player.sendMessage("§e/party list §7- List party members")
        player.sendMessage("§e/party leader <player> §7- Transfer leadership")
        player.sendMessage("§e/party tp <player> §7- Teleport to a member")
        player.sendMessage("§e/pc <message> §7- Party chat")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return emptyList()

        if (args.size == 1) {
            return listOf("invite", "accept", "decline", "leave", "kick", "disband", "list", "leader", "tp")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "invite" -> {
                    return Bukkit.getOnlinePlayers()
                        .filter { it != sender && !plugin.partyManager.isInParty(it.uniqueId) }
                        .map { it.name }
                        .filter { it.lowercase().startsWith(args[1].lowercase()) }
                }
                "kick", "leader" -> {
                    val party = plugin.partyManager.getParty(sender.uniqueId)
                    if (party != null) {
                        return party.members
                            .filter { it != sender.uniqueId }
                            .mapNotNull { Bukkit.getPlayer(it)?.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
                "tp" -> {
                    val party = plugin.partyManager.getParty(sender.uniqueId)
                    if (party != null) {
                        return party.members
                            .filter { it != sender.uniqueId }
                            .mapNotNull { Bukkit.getPlayer(it) }
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
            }
        }
        return emptyList()
    }
}
