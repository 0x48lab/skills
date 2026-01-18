package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillCategory
import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatType
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
            plugin.messageSender.send(sender, MessageKey.SYSTEM_NO_PERMISSION)
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "check" -> handleCheck(sender, args.drop(1))
            "set" -> handleSet(sender, args.drop(1))
            "setstat" -> handleSetStat(sender, args.drop(1))
            "reset" -> handleReset(sender, args.drop(1))
            "reload" -> handleReload(sender)
            "give" -> handleGive(sender, args.drop(1))
            else -> showHelp(sender)
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_HEADER)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_CHECK)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_SET)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_SETSTAT)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_RESET)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_RELOAD)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_GIVE_SPELLBOOK)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_GIVE_RUNEBOOK)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_GIVE_RUNE)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_GIVE_REAGENTS)
        plugin.messageSender.send(sender, MessageKey.ADMIN_HELP_GIVE_SCROLL)
    }

    private fun handleCheck(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_CHECK)
            return
        }

        // Try online player first, then offline
        val onlinePlayer = Bukkit.getPlayer(args[0])
        val offlinePlayer = Bukkit.getOfflinePlayer(args[0])

        val data = if (onlinePlayer != null) {
            plugin.playerDataManager.getPlayerData(onlinePlayer)
        } else {
            plugin.playerDataManager.loadOfflinePlayerData(offlinePlayer.uniqueId)
        }

        if (data == null) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_PLAYER_NOT_FOUND_OR_NEVER_JOINED, "player" to args[0])
            return
        }

        val playerName = onlinePlayer?.name ?: offlinePlayer.name ?: args[0]

        // Header with player name and title
        val title = plugin.skillTitleManager.getTitleFromData(data)
        plugin.messageSender.send(sender, MessageKey.ADMIN_PLAYER_SKILLS_HEADER, "player" to playerName)
        plugin.messageSender.send(sender, MessageKey.ADMIN_PLAYER_TITLE, "title" to title)
        sender.sendMessage(Component.text(""))

        // Stats
        plugin.messageSender.send(sender, MessageKey.ADMIN_PLAYER_STATS,
            "str" to data.str.toString(),
            "dex" to data.dex.toString(),
            "int" to data.int.toString()
        )
        plugin.messageSender.send(sender, MessageKey.ADMIN_PLAYER_TOTAL,
            "current" to String.format("%.1f", data.getTotalSkillPoints()),
            "max" to SkillType.TOTAL_SKILL_CAP.toString()
        )
        sender.sendMessage(Component.text(""))

        // Skills by category (same display as /skills command)
        SkillCategory.entries.forEach { category ->
            val categorySkills = SkillType.entries.filter { it.category == category }

            if (categorySkills.isNotEmpty()) {
                sender.sendMessage(Component.text("─── ${category.displayName} ───").color(NamedTextColor.GRAY))
                categorySkills.forEach { skill ->
                    val value = data.getSkillValue(skill)
                    val lockMode = data.getSkillLock(skill)
                    val color = when {
                        value >= 100 -> NamedTextColor.LIGHT_PURPLE
                        value >= 90 -> NamedTextColor.GOLD
                        value >= 70 -> NamedTextColor.GREEN
                        value >= 50 -> NamedTextColor.YELLOW
                        value > 0 -> NamedTextColor.WHITE
                        else -> NamedTextColor.GRAY
                    }
                    val lockColor = when (lockMode) {
                        SkillLockMode.UP -> NamedTextColor.GREEN
                        SkillLockMode.DOWN -> NamedTextColor.RED
                        SkillLockMode.LOCKED -> NamedTextColor.YELLOW
                    }
                    sender.sendMessage(
                        Component.text("  ${skill.displayName}: ")
                            .color(NamedTextColor.WHITE)
                            .append(Component.text(String.format("%.1f", value)).color(color))
                            .append(Component.text(" ${lockMode.getSymbol()}").color(lockColor))
                    )
                }
            }
        }
    }

    private fun handleSet(sender: CommandSender, args: List<String>) {
        if (args.size < 3) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_SET)
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_SET_NOTE)
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_NOT_FOUND, "player" to args[0])
            return
        }

        // Value is the last argument, skill name is everything in between
        val valueStr = args.last()
        val value = valueStr.toDoubleOrNull()
        if (value == null || value < 0 || value > 100) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_INVALID_VALUE,
                "value" to valueStr,
                "min" to "0",
                "max" to "100"
            )
            return
        }

        // Join all arguments between player and value as skill name
        val skillName = args.drop(1).dropLast(1).joinToString(" ")
        val skill = SkillType.fromDisplayName(skillName)
            ?: SkillType.entries.find { it.name.equals(skillName.replace(" ", "_"), ignoreCase = true) }
            ?: SkillType.entries.find { it.displayName.equals(skillName, ignoreCase = true) }

        if (skill == null) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_SKILL, "skill" to skillName)
            plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_SKILL_LIST,
                "skills" to SkillType.entries.joinToString(", ") { it.displayName }
            )
            return
        }

        plugin.skillManager.setSkill(target, skill, value)
        plugin.messageSender.send(sender, MessageKey.ADMIN_SKILL_SET_SUCCESS,
            "player" to target.name,
            "skill" to skill.displayName,
            "value" to value.toString()
        )
    }

    private fun handleSetStat(sender: CommandSender, args: List<String>) {
        if (args.size < 3) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_SETSTAT)
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_NOT_FOUND, "player" to args[0])
            return
        }

        val statName = args[1].uppercase()
        val stat = when (statName) {
            "STR" -> StatType.STR
            "DEX" -> StatType.DEX
            "INT" -> StatType.INT
            else -> {
                plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_STAT, "stat" to args[1])
                return
            }
        }

        val value = args[2].toIntOrNull()
        if (value == null || value < StatType.MIN_STAT_VALUE || value > StatType.MAX_STAT_VALUE) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_INVALID_VALUE,
                "value" to args[2],
                "min" to StatType.MIN_STAT_VALUE.toString(),
                "max" to StatType.MAX_STAT_VALUE.toString()
            )
            return
        }

        val data = plugin.playerDataManager.getPlayerData(target)
        data.setStat(stat, value)
        data.updateMaxStats()

        // Apply attribute modifiers
        val armorDexPenalty = plugin.armorManager.getTotalDexPenalty(target)
        com.hacklab.minecraft.skills.skill.StatCalculator.applyAttributeModifiers(target, data, armorDexPenalty)

        plugin.messageSender.send(sender, MessageKey.ADMIN_STAT_SET_SUCCESS,
            "player" to target.name,
            "stat" to statName,
            "value" to value.toString()
        )
    }

    private fun handleReset(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_RESET)
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_NOT_FOUND, "player" to args[0])
            return
        }

        val data = plugin.playerDataManager.getPlayerData(target)
        SkillType.entries.forEach { skill ->
            data.setSkillValue(skill, 0.0)
        }
        data.updateMaxStats()

        plugin.messageSender.send(sender, MessageKey.ADMIN_SKILLS_RESET, "player" to target.name)
    }

    private fun handleReload(sender: CommandSender) {
        plugin.skillsConfig.reload()
        plugin.messageManager.reload()
        plugin.messageSender.send(sender, MessageKey.ADMIN_CONFIG_RELOADED)
    }

    private fun handleGive(sender: CommandSender, args: List<String>) {
        if (args.size < 2) {
            plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_GIVE)
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_NOT_FOUND, "player" to args[0])
            return
        }

        when (args[1].lowercase()) {
            "spellbook" -> {
                val spellbook = plugin.spellbookManager.createFullSpellbook()
                target.inventory.addItem(spellbook)
                plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_SPELLBOOK, "player" to target.name)
            }
            "runebook" -> {
                val useJapanese = plugin.localeManager.getLanguage(target) == com.hacklab.minecraft.skills.i18n.Language.JAPANESE
                val runebook = plugin.runebookManager.createRunebook(useJapanese)
                target.inventory.addItem(runebook)
                plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_RUNEBOOK, "player" to target.name)
            }
            "rune" -> {
                val rune = plugin.runeManager.createRune()
                target.inventory.addItem(rune)
                plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_RUNE, "player" to target.name)
            }
            "reagents" -> {
                // Collect all unique materials used by spells
                val allMaterials = com.hacklab.minecraft.skills.magic.SpellType.entries
                    .flatMap { it.reagents }
                    .distinct()
                allMaterials.forEach { material ->
                    target.inventory.addItem(org.bukkit.inventory.ItemStack(material, 64))
                }
                plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_REAGENTS, "player" to target.name)
            }
            "scroll" -> {
                if (args.size < 3) {
                    plugin.messageSender.send(sender, MessageKey.ADMIN_USAGE_SCROLL)
                    return
                }
                val spellArg = args.drop(2).joinToString(" ")
                if (spellArg.equals("all", ignoreCase = true)) {
                    // Give all scrolls
                    com.hacklab.minecraft.skills.magic.SpellType.entries.forEach { spell ->
                        val scroll = plugin.scrollManager.createScroll(spell)
                        target.inventory.addItem(scroll)
                    }
                    plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_ALL_SCROLLS, "player" to target.name)
                } else {
                    // Give specific scroll
                    val spell = com.hacklab.minecraft.skills.magic.SpellType.fromDisplayName(spellArg)
                        ?: com.hacklab.minecraft.skills.magic.SpellType.entries.find {
                            it.name.equals(spellArg.replace(" ", "_"), ignoreCase = true)
                        }
                    if (spell == null) {
                        plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_SPELL, "spell" to spellArg)
                        plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_SPELL_LIST,
                            "spells" to com.hacklab.minecraft.skills.magic.SpellType.entries.joinToString(", ") { it.displayName }
                        )
                        return
                    }
                    val scroll = plugin.scrollManager.createScroll(spell)
                    target.inventory.addItem(scroll)
                    plugin.messageSender.send(sender, MessageKey.ADMIN_GAVE_SCROLL,
                        "spell" to spell.displayName,
                        "player" to target.name
                    )
                }
            }
            else -> plugin.messageSender.send(sender, MessageKey.ADMIN_UNKNOWN_ITEM, "item" to args[1])
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return when (args.size) {
            1 -> listOf("check", "set", "setstat", "reset", "reload", "give").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "check", "set", "setstat", "reset", "give" -> Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "set" -> SkillType.entries.map { it.displayName }
                    .filter { it.lowercase().startsWith(args[2].lowercase()) }
                "setstat" -> listOf("STR", "DEX", "INT")
                    .filter { it.lowercase().startsWith(args[2].lowercase()) }
                "give" -> listOf("spellbook", "runebook", "rune", "reagents", "scroll")
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
