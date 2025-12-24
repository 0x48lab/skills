package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.SpellType
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CastCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /cast <spell name>")
            sender.sendMessage("Example: /cast fireball")
            return true
        }

        // Handle cancel command
        if (args[0].equals("cancel", ignoreCase = true)) {
            plugin.targetManager.cancelTargeting(sender.uniqueId)
            plugin.messageSender.send(sender, MessageKey.MAGIC_CANCELLED)
            return true
        }

        // Check cooldown between spell casts
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.CAST_SPELL)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.CAST_SPELL)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        val spellName = args.joinToString(" ")
        val spell = SpellType.fromDisplayName(spellName)
            ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

        if (spell == null) {
            sender.sendMessage("Unknown spell: $spellName")
            sender.sendMessage("Use /spellbook to see available spells.")
            return true
        }

        // Cast spell and set cooldown
        plugin.spellManager.castSpell(sender, spell)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.CAST_SPELL)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) return emptyList()

        // Include "cancel" as first suggestion
        val suggestions = mutableListOf("cancel")

        // Only suggest spells the player has in their spellbook
        val spellbook = plugin.spellbookManager.getSpellbook(sender)
        val availableSpells = if (spellbook != null) {
            plugin.spellbookManager.getSpells(spellbook)
        } else {
            emptySet()
        }

        val input = args.joinToString(" ").lowercase()
        suggestions.addAll(
            availableSpells
                .map { it.displayName }
                .filter { it.lowercase().startsWith(input) }
                .sorted()
        )

        return suggestions.filter { it.lowercase().startsWith(input) }
    }
}
