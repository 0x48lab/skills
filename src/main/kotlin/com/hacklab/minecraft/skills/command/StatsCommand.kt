package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.StatCalculator
import com.hacklab.minecraft.skills.skill.StatLockMode
import com.hacklab.minecraft.skills.skill.StatType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class StatsCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        // Handle lock subcommand
        if (args.isNotEmpty() && args[0].equals("lock", ignoreCase = true)) {
            return handleLockCommand(sender, args)
        }

        val data = plugin.playerDataManager.getPlayerData(sender)

        // Header
        plugin.messageSender.send(sender, MessageKey.STATS_HEADER_DISPLAY)

        // HP
        val hpPercent = (data.internalHp / data.maxInternalHp * 100).toInt()
        val hpColor = when {
            hpPercent > 70 -> NamedTextColor.GREEN
            hpPercent > 30 -> NamedTextColor.YELLOW
            else -> NamedTextColor.RED
        }
        sender.sendMessage(
            Component.text("❤ HP: ${data.internalHp.toInt()} / ${data.maxInternalHp.toInt()}")
                .color(hpColor)
        )

        // Mana
        plugin.messageSender.send(sender, MessageKey.STATS_MANA_DISPLAY,
            "current" to data.mana.toInt().toString(),
            "max" to data.maxMana.toInt().toString()
        )

        plugin.messageSender.send(sender, MessageKey.UI_SEPARATOR)

        // Total stat points info
        val totalStats = data.getTotalStats()
        plugin.messageSender.send(sender, MessageKey.STATS_STAT_TOTAL,
            "current" to totalStats.toString(),
            "max" to StatType.TOTAL_STAT_CAP.toString()
        )

        // STR with lock
        val str = data.str
        val strLock = data.strLock
        val strEffects = StatCalculator.getStrEffects(str)
        sender.sendMessage(
            Component.text("STR: $str ")
                .color(NamedTextColor.RED)
                .append(getLockIcon(strLock))
                .append(Component.text("  (+${strEffects.bonusHp} HP, +${strEffects.miningSpeedBonus.toInt()}% mining, +${strEffects.lumberSpeedBonus.toInt()}% lumber)")
                    .color(NamedTextColor.GRAY))
        )

        // DEX with lock
        val dex = data.dex
        val dexLock = data.dexLock
        val dexEffects = StatCalculator.getDexEffects(dex)
        sender.sendMessage(
            Component.text("DEX: $dex ")
                .color(NamedTextColor.GREEN)
                .append(getLockIcon(dexLock))
                .append(Component.text("  (+${dexEffects.attackSpeedBonus.toInt()}% attack, +${dexEffects.movementSpeedBonus.toInt()}% move)")
                    .color(NamedTextColor.GRAY))
        )

        // INT with lock
        val intVal = data.int
        val intLock = data.intLock
        val intEffects = StatCalculator.getIntEffects(intVal)
        sender.sendMessage(
            Component.text("INT: $intVal ")
                .color(NamedTextColor.BLUE)
                .append(getLockIcon(intLock))
                .append(Component.text("  (-${intEffects.manaReduction.toInt()}% mana cost, +${intEffects.castSuccessBonus.toInt()}% cast)")
                    .color(NamedTextColor.GRAY))
        )

        // Total skills
        plugin.messageSender.send(sender, MessageKey.UI_SEPARATOR)
        plugin.messageSender.send(sender, MessageKey.STATS_TOTAL_SKILLS_DISPLAY,
            "current" to String.format("%.1f", data.getTotalSkillPoints()),
            "max" to com.hacklab.minecraft.skills.skill.SkillType.TOTAL_SKILL_CAP.toString()
        )

        // Lock help
        plugin.messageSender.send(sender, MessageKey.UI_SEPARATOR)
        plugin.messageSender.send(sender, MessageKey.STATS_LOCK_HELP)
        plugin.messageSender.send(sender, MessageKey.STATS_LOCK_DESCRIPTION)

        return true
    }

    private fun handleLockCommand(player: Player, args: Array<String>): Boolean {
        if (args.size < 2) {
            plugin.messageSender.send(player, MessageKey.STATS_USAGE_LOCK)
            return true
        }

        val statType = when (args[1].lowercase()) {
            "str", "strength" -> StatType.STR
            "dex", "dexterity" -> StatType.DEX
            "int", "intelligence" -> StatType.INT
            else -> {
                plugin.messageSender.send(player, MessageKey.STATS_INVALID_STAT, "stat" to args[1])
                return true
            }
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val currentLock = data.getStatLock(statType)

        // If no mode specified, cycle through modes
        val newMode = if (args.size < 3) {
            when (currentLock) {
                StatLockMode.UP -> StatLockMode.DOWN
                StatLockMode.DOWN -> StatLockMode.LOCKED
                StatLockMode.LOCKED -> StatLockMode.UP
            }
        } else {
            when (args[2].lowercase()) {
                "up" -> StatLockMode.UP
                "down" -> StatLockMode.DOWN
                "locked", "lock" -> StatLockMode.LOCKED
                else -> {
                    plugin.messageSender.send(player, MessageKey.STATS_INVALID_MODE, "mode" to args[2])
                    return true
                }
            }
        }

        data.setStatLock(statType, newMode)

        plugin.messageSender.send(
            player, MessageKey.STAT_LOCK_CHANGED,
            "stat" to statType.displayName,
            "mode" to newMode.name
        )

        return true
    }

    private fun getLockIcon(mode: StatLockMode): Component {
        return when (mode) {
            StatLockMode.UP -> Component.text("▲").color(NamedTextColor.GREEN)
            StatLockMode.DOWN -> Component.text("▼").color(NamedTextColor.RED)
            StatLockMode.LOCKED -> Component.text("🔒").color(NamedTextColor.YELLOW)
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String> {
        if (sender !is Player) return emptyList()

        return when {
            args.size == 1 -> listOf("lock").filter { it.startsWith(args[0], ignoreCase = true) }
            args.size == 2 && args[0].equals("lock", ignoreCase = true) ->
                listOf("str", "dex", "int").filter { it.startsWith(args[1], ignoreCase = true) }
            args.size == 3 && args[0].equals("lock", ignoreCase = true) ->
                listOf("up", "down", "locked").filter { it.startsWith(args[2], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
