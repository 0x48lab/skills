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

        // Try to match spell name (with optional player argument for PLAYER_OR_SELF spells)
        var spellName = args.joinToString(" ")
        var spell = SpellType.fromDisplayName(spellName)
            ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

        var targetPlayer: Player? = null

        // If no match and multiple args, try with last arg as player name
        if (spell == null && args.size > 1) {
            val spellArgs = args.dropLast(1)
            val playerArg = args.last()
            spellName = spellArgs.joinToString(" ")
            spell = SpellType.fromDisplayName(spellName)
                ?: SpellType.entries.find { it.name.equals(spellName.replace(" ", "_"), ignoreCase = true) }

            if (spell != null && spell.targetType == com.hacklab.minecraft.skills.magic.SpellTargetType.PLAYER_OR_SELF) {
                targetPlayer = plugin.server.getPlayer(playerArg)
                if (targetPlayer == null) {
                    sender.sendMessage("Player not found: $playerArg")
                    return true
                }
            } else if (spell != null) {
                // Spell matched but doesn't support player argument
                spell = null  // Reset to show "unknown spell" with full args
                spellName = args.joinToString(" ")
            }
        }

        if (spell == null) {
            sender.sendMessage("Unknown spell: $spellName")
            sender.sendMessage("Use /spellbook to see available spells.")
            return true
        }

        // Cast spell and set cooldown
        plugin.spellManager.castSpell(sender, spell, targetPlayer = targetPlayer)
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
