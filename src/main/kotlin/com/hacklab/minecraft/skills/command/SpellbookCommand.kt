package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SpellbookCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        if (args.isEmpty()) {
            // Give empty spellbook when no arguments
            val spellbook = plugin.spellbookManager.createSpellbook(sender)
            sender.inventory.addItem(spellbook)
            sender.sendMessage("You received an empty spellbook!")
            return true
        }

        when (args[0].lowercase()) {
            "list" -> listSpells(sender)
            "all" -> giveAllSpellbook(sender)
            else -> {
                val spellName = args.joinToString(" ")
                giveSpellbook(sender, spellName)
            }
        }

        return true
    }

    private fun showUsage(player: Player) {
        player.sendMessage("=== Spellbook Commands ===")
        player.sendMessage("/spellbook list - Show all available spells")
        player.sendMessage("/spellbook all - Get a spellbook with all spells")
        player.sendMessage("/spellbook <spell> - Get a spellbook with specific spell")
    }

    private fun listSpells(player: Player) {
        player.sendMessage("=== Available Spells ===")

        SpellCircle.entries.forEach { circle ->
            val spells = SpellType.entries.filter { it.circle == circle }
            if (spells.isNotEmpty()) {
                player.sendMessage("--- ${circle.number}${getOrdinalSuffix(circle.number)} Circle ---")
                spells.forEach { spell ->
                    player.sendMessage("  ${spell.displayName} (Mana: ${circle.baseMana})")
                }
            }
        }
    }

    private fun getOrdinalSuffix(n: Int): String {
        return when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    private fun giveAllSpellbook(player: Player) {
        val spellbook = plugin.spellbookManager.createSpellbookWith(player, SpellType.entries.toSet())
        player.inventory.addItem(spellbook)
        player.sendMessage("You received a complete spellbook with all spells!")
    }

    private fun giveSpellbook(player: Player, spellName: String) {
        val spell = SpellType.fromDisplayName(spellName)
            ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

        if (spell == null) {
            player.sendMessage("Unknown spell: $spellName")
            player.sendMessage("Use /spellbook list to see available spells.")
            return
        }

        val spellbook = plugin.spellbookManager.createSpellbookWith(player, setOf(spell))
        player.inventory.addItem(spellbook)
        player.sendMessage("You received a spellbook containing ${spell.displayName}!")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            val suggestions = mutableListOf("list", "all")
            suggestions.addAll(SpellType.entries.map { it.displayName })
            return suggestions.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
