package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ScribeCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "list" -> listAvailableSpells(sender)
            else -> {
                val spellName = args.joinToString(" ")
                scribeScroll(sender, spellName)
            }
        }

        return true
    }

    private fun showUsage(player: Player) {
        plugin.messageSender.send(player, MessageKey.SCRIBE_HELP_HEADER)
        plugin.messageSender.send(player, MessageKey.SCRIBE_HELP_LIST)
        plugin.messageSender.send(player, MessageKey.SCRIBE_HELP_SPELL)
        player.sendMessage("")
        plugin.messageSender.send(player, MessageKey.SCRIBE_HELP_REQUIREMENTS)
    }

    private fun listAvailableSpells(player: Player) {
        val spellbook = plugin.spellbookManager.getSpellbook(player)
        if (spellbook == null) {
            plugin.messageSender.send(player, MessageKey.MAGIC_NO_SPELLBOOK)
            return
        }

        val spells = plugin.spellbookManager.getSpells(spellbook)
        if (spells.isEmpty()) {
            plugin.messageSender.send(player, MessageKey.SCRIBE_SPELLBOOK_EMPTY)
            return
        }

        plugin.messageSender.send(player, MessageKey.SCRIBE_AVAILABLE_HEADER)
        spells.sortedBy { it.circle.number }.forEach { spell ->
            player.sendMessage("  ${spell.displayName} (${spell.circle.name.lowercase().replaceFirstChar { it.uppercase() }} Circle)")
        }
    }

    private fun scribeScroll(player: Player, spellName: String) {
        val spell = SpellType.fromDisplayName(spellName)
            ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

        if (spell == null) {
            plugin.messageSender.send(player, MessageKey.SCRIBE_UNKNOWN_SPELL, "spell" to spellName)
            plugin.messageSender.send(player, MessageKey.SCRIBE_UNKNOWN_SPELL_HINT)
            return
        }

        // craftScroll handles all checks (spellbook, paper, reagents) and skill gain
        plugin.scrollManager.craftScroll(player, spell)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) return emptyList()

        if (args.size == 1) {
            val suggestions = mutableListOf("list")

            // Add spells from player's spellbook
            val spellbook = plugin.spellbookManager.getSpellbook(sender)
            if (spellbook != null) {
                suggestions.addAll(
                    plugin.spellbookManager.getSpells(spellbook).map { it.displayName }
                )
            }

            return suggestions.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }

        return emptyList()
    }
}
