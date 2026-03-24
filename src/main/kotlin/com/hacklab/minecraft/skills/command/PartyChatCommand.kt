package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PartyChatCommand(private val plugin: Skills) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        val party = plugin.partyManager.getParty(sender.uniqueId)
        if (party == null) {
            plugin.messageSender.send(sender, MessageKey.PARTY_NOT_IN)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /pc <message>")
            return true
        }

        val message = args.joinToString(" ")
        val onlineMembers = plugin.partyManager.getOnlineMembers(party)

        for (member in onlineMembers) {
            plugin.messageSender.send(member, MessageKey.PARTY_CHAT,
                "player" to sender.name, "message" to message)
        }

        // Log to console
        plugin.logger.info("[Party Chat] ${sender.name}: $message")
        return true
    }
}
