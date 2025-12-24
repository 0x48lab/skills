package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SkillAdminCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("skills.admin")) {
            plugin.messageSender.send(sender as? Player ?: return true, MessageKey.SYSTEM_NO_PERMISSION)
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "set" -> handleSet(sender, args.drop(1))
            "reset" -> handleReset(sender, args.drop(1))
            "reload" -> handleReload(sender)
            "give" -> handleGive(sender, args.drop(1))
            else -> showHelp(sender)
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("═══ Skills Admin Commands ═══").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/skilladmin set <player> <skill> <value>").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin reset <player>").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin reload").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin give <player> spellbook").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin give <player> rune").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin give <player> reagents").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("/skilladmin give <player> scroll <spell|all>").color(NamedTextColor.WHITE))
    }

    private fun handleSet(sender: CommandSender, args: List<String>) {
        if (args.size < 3) {
            sender.sendMessage("Usage: /skilladmin set <player> <skill> <value>")
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("Player not found: ${args[0]}")
            return
        }

        val skillName = args[1]
        val skill = SkillType.fromDisplayName(skillName)
            ?: SkillType.entries.find { it.name.equals(skillName.replace(" ", "_"), ignoreCase = true) }

        if (skill == null) {
            sender.sendMessage("Unknown skill: $skillName")
            return
        }

        val value = args[2].toDoubleOrNull()
        if (value == null || value < 0 || value > 100) {
            sender.sendMessage("Invalid value: ${args[2]} (must be 0-100)")
            return
        }

        plugin.skillManager.setSkill(target, skill, value)
        sender.sendMessage("Set ${target.name}'s ${skill.displayName} to $value")
    }

    private fun handleReset(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage("Usage: /skilladmin reset <player>")
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("Player not found: ${args[0]}")
            return
        }

        val data = plugin.playerDataManager.getPlayerData(target)
        SkillType.entries.forEach { skill ->
            data.setSkillValue(skill, 0.0)
        }
        data.updateMaxStats()

        sender.sendMessage("Reset all skills for ${target.name}")
    }

    private fun handleReload(sender: CommandSender) {
        plugin.skillsConfig.reload()
        plugin.messageManager.reload()
        sender.sendMessage("Configuration reloaded")
    }

    private fun handleGive(sender: CommandSender, args: List<String>) {
        if (args.size < 2) {
            sender.sendMessage("Usage: /skilladmin give <player> <item>")
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("Player not found: ${args[0]}")
            return
        }

        when (args[1].lowercase()) {
            "spellbook" -> {
                val spellbook = plugin.spellbookManager.createFullSpellbook()
                target.inventory.addItem(spellbook)
                sender.sendMessage("Gave full spellbook to ${target.name}")
            }
            "rune" -> {
                val rune = plugin.runeManager.createRune()
                target.inventory.addItem(rune)
                sender.sendMessage("Gave blank rune to ${target.name}")
            }
            "reagents" -> {
                // Collect all unique materials used by spells
                val allMaterials = com.hacklab.minecraft.skills.magic.SpellType.entries
                    .flatMap { it.reagents }
                    .distinct()
                allMaterials.forEach { material ->
                    target.inventory.addItem(org.bukkit.inventory.ItemStack(material, 64))
                }
                sender.sendMessage("Gave all reagents to ${target.name}")
            }
            "scroll" -> {
                if (args.size < 3) {
                    sender.sendMessage("Usage: /skilladmin give <player> scroll <spell|all>")
                    return
                }
                val spellArg = args.drop(2).joinToString(" ")
                if (spellArg.equals("all", ignoreCase = true)) {
                    // Give all scrolls
                    com.hacklab.minecraft.skills.magic.SpellType.entries.forEach { spell ->
                        val scroll = plugin.scrollManager.createScroll(spell)
                        target.inventory.addItem(scroll)
                    }
                    sender.sendMessage("Gave all spell scrolls to ${target.name}")
                } else {
                    // Give specific scroll
                    val spell = com.hacklab.minecraft.skills.magic.SpellType.fromDisplayName(spellArg)
                        ?: com.hacklab.minecraft.skills.magic.SpellType.entries.find {
                            it.name.equals(spellArg.replace(" ", "_"), ignoreCase = true)
                        }
                    if (spell == null) {
                        sender.sendMessage("Unknown spell: $spellArg")
                        sender.sendMessage("Available spells: ${com.hacklab.minecraft.skills.magic.SpellType.entries.joinToString(", ") { it.displayName }}")
                        return
                    }
                    val scroll = plugin.scrollManager.createScroll(spell)
                    target.inventory.addItem(scroll)
                    sender.sendMessage("Gave Scroll of ${spell.displayName} to ${target.name}")
                }
            }
            else -> sender.sendMessage("Unknown item: ${args[1]}")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("set", "reset", "reload", "give").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "set", "reset", "give" -> Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set" -> SkillType.entries.map { it.displayName }
                    .filter { it.lowercase().startsWith(args[2].lowercase()) }
                "give" -> listOf("spellbook", "rune", "reagents", "scroll")
                    .filter { it.startsWith(args[2].lowercase()) }
                else -> emptyList()
            }
            4 -> when {
                args[0].equals("give", ignoreCase = true) && args[2].equals("scroll", ignoreCase = true) -> {
                    val suggestions = mutableListOf("all")
                    suggestions.addAll(com.hacklab.minecraft.skills.magic.SpellType.entries.map { it.displayName })
                    suggestions.filter { it.lowercase().startsWith(args[3].lowercase()) }
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
