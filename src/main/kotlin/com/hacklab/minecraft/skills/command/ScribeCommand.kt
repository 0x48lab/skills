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
            sender.sendMessage("This command is for players only.")
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
        player.sendMessage("=== Scribe Commands ===")
        player.sendMessage("/scribe list - Show spells you can scribe")
        player.sendMessage("/scribe <spell> - Create a scroll of the spell")
        player.sendMessage("")
        player.sendMessage("Requirements: Paper + Reagents + Spell in spellbook")
    }

    private fun listAvailableSpells(player: Player) {
        val spellbook = plugin.spellbookManager.getSpellbook(player)
        if (spellbook == null) {
            plugin.messageSender.send(player, MessageKey.MAGIC_NO_SPELLBOOK)
            return
        }

        val spells = plugin.spellbookManager.getSpells(spellbook)
        if (spells.isEmpty()) {
            player.sendMessage("Your spellbook is empty.")
            return
        }

        player.sendMessage("=== Available Spells for Scribing ===")
        spells.sortedBy { it.circle.number }.forEach { spell ->
            player.sendMessage("  ${spell.displayName} (${spell.circle.name.lowercase().replaceFirstChar { it.uppercase() }} Circle)")
        }
    }

    private fun scribeScroll(player: Player, spellName: String) {
        val spell = SpellType.fromDisplayName(spellName)
            ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

        if (spell == null) {
            player.sendMessage("Unknown spell: $spellName")
            player.sendMessage("Use /scribe list to see available spells.")
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
