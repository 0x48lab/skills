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

/**
 * Command for casting spells using Power Words (UO-style incantations).
 * Example: /rune In Mani (Heal), /rune Vas Flam (Fireball)
 */
class RuneCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        if (args.isEmpty()) {
            plugin.messageSender.send(sender, MessageKey.RUNE_USAGE)
            plugin.messageSender.send(sender, MessageKey.RUNE_EXAMPLE1)
            plugin.messageSender.send(sender, MessageKey.RUNE_EXAMPLE2)
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

        // Join all args as Power Words
        val powerWords = args.joinToString(" ")

        // Find spell by Power Words only
        val spell = SpellType.fromPowerWords(powerWords)

        if (spell == null) {
            plugin.messageSender.send(sender, MessageKey.RUNE_UNKNOWN_POWER_WORDS, "words" to powerWords)
            plugin.messageSender.send(sender, MessageKey.RUNE_POWER_WORDS_HINT)
            return true
        }

        // Cast spell and set cooldown
        plugin.spellManager.castSpell(sender, spell)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.CAST_SPELL)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (sender !is Player) return emptyList()

        // For first argument, suggest "cancel" and first words of Power Words
        if (args.size <= 1) {
            val currentInput = args.firstOrNull()?.lowercase() ?: ""
            val suggestions = mutableListOf("cancel")

            // Get unique first words from all Power Words
            plugin.spellbookManager.getAvailableSpells(sender).forEach { spell ->
                val firstWord = spell.powerWords.split(" ").firstOrNull() ?: return@forEach
                if (firstWord.lowercase().startsWith(currentInput)) {
                    suggestions.add(firstWord)
                }
            }

            return suggestions.distinct()
        }

        // For subsequent arguments, find matching Power Words and suggest next word
        val previousWords = args.dropLast(1).joinToString(" ")
        val currentInput = args.last().lowercase()

        val suggestions = mutableListOf<String>()

        plugin.spellbookManager.getAvailableSpells(sender).forEach { spell ->
            val powerWords = spell.powerWords
            // Check if this spell's Power Words start with the previous words
            if (powerWords.lowercase().startsWith(previousWords.lowercase())) {
                val words = powerWords.split(" ")
                val nextWordIndex = args.size - 1
                if (nextWordIndex < words.size) {
                    val nextWord = words[nextWordIndex]
                    if (nextWord.lowercase().startsWith(currentInput)) {
                        suggestions.add(nextWord)
                    }
                }
            }
        }

        return suggestions.distinct()
    }
}
