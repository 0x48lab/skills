package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class LanguageCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        if (!plugin.localeManager.canPlayerChangeLanguage()) {
            sender.sendMessage(Component.text("Language change is disabled.").color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            // Show current language
            val current = plugin.localeManager.getLanguage(sender)
            plugin.messageSender.send(sender, MessageKey.LANGUAGE_CURRENT,
                "language" to current.displayName)

            // Show available languages
            sender.sendMessage(Component.text("Available: ${Language.entries.joinToString { "${it.code} (${it.displayName})" }}")
                .color(NamedTextColor.GRAY))
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
            return Language.entries.map { it.code }
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
