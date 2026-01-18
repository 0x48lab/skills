package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class LanguageCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        if (!plugin.localeManager.canPlayerChangeLanguage()) {
            plugin.messageSender.send(sender, MessageKey.LANGUAGE_DISABLED)
            return true
        }

        if (args.isEmpty()) {
            // Show current language
            val playerData = plugin.playerDataManager.getPlayerData(sender)
            val current = plugin.localeManager.getLanguage(sender)

            if (playerData.language == null) {
                // Using client language
                plugin.messageSender.send(sender, MessageKey.LANGUAGE_USING_CLIENT,
                    "language" to current.displayName)
            } else {
                plugin.messageSender.send(sender, MessageKey.LANGUAGE_CURRENT,
                    "language" to current.displayName)
            }

            // Show available languages
            plugin.messageSender.send(sender, MessageKey.LANGUAGE_AVAILABLE_LIST,
                "languages" to Language.entries.joinToString { "${it.code} (${it.displayName})" })
            return true
        }

        // Reset language
        if (args[0].equals("reset", ignoreCase = true)) {
            plugin.localeManager.resetLanguage(sender)
            plugin.messageSender.send(sender, MessageKey.LANGUAGE_RESET)
            return true
        }

        // Set language
        val lang = Language.fromCode(args[0])
        plugin.localeManager.setLanguage(sender, lang)
        plugin.messageSender.send(sender, MessageKey.LANGUAGE_CHANGED,
            "language" to lang.displayName)

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            val options = mutableListOf("reset")
            options.addAll(Language.entries.map { it.code })
            return options.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
