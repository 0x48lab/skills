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
            sender.sendMessage("For Power Words, use: /rune <Power Words>")
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

        // Try to match spell by: 1) display name, 2) enum name, 3) Power Words
        var spellName = args.joinToString(" ")
        var spell = findSpell(spellName)

        var targetPlayer: Player? = null

        // If no match and multiple args, try with last arg as player name
        if (spell == null && args.size > 1) {
            val spellArgs = args.dropLast(1)
            val playerArg = args.last()
            spellName = spellArgs.joinToString(" ")
            spell = findSpell(spellName)

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
            sender.sendMessage("Use /spellbook list to see available spells.")
            sender.sendMessage("For Power Words, use: /rune <Power Words>")
            return true
        }

        // Cast spell and set cooldown
        plugin.spellManager.castSpell(sender, spell, targetPlayer = targetPlayer)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.CAST_SPELL)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) return emptyList()

        val input = args.joinToString(" ").lowercase()

        // Include "cancel" and spell display names (Power Words are via /rune)
        val suggestions = mutableListOf("cancel")
        suggestions.addAll(plugin.spellbookManager.getSpellNameCompletions(sender, input))

        return suggestions.distinct().filter { it.lowercase().startsWith(input) }
    }

    /**
     * Find spell by display name or enum name (not Power Words - use /rune for that)
     */
    private fun findSpell(input: String): SpellType? {
        // 1. Try display name (e.g., "Fireball")
        SpellType.fromDisplayName(input)?.let { return it }

        // 2. Try enum name (e.g., "FIREBALL" or "fire_wall")
        SpellType.entries.find { it.name.equals(input.replace(" ", "_"), ignoreCase = true) }?.let { return it }

        return null
    }
}
