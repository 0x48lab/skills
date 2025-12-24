package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.StatCalculator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StatsCommand(private val plugin: Skills) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        val data = plugin.playerDataManager.getPlayerData(sender)

        // Header
        sender.sendMessage(Component.text("═══ Your Stats ═══").color(NamedTextColor.GOLD))

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
        val manaPercent = (data.mana / data.maxMana * 100).toInt()
        sender.sendMessage(
            Component.text("🍖 Mana: ${data.mana.toInt()} / ${data.maxMana.toInt()}")
                .color(NamedTextColor.BLUE)
        )

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY))

        // STR
        val str = data.getStr()
        val strEffects = StatCalculator.getStrEffects(str)
        sender.sendMessage(
            Component.text("STR: $str")
                .color(NamedTextColor.RED)
                .append(Component.text("  (+${strEffects.bonusHp} HP, +${strEffects.miningSpeedBonus.toInt()}% mining)")
                    .color(NamedTextColor.GRAY))
        )

        // DEX
        val dex = data.getDex()
        val dexEffects = StatCalculator.getDexEffects(dex)
        sender.sendMessage(
            Component.text("DEX: $dex")
                .color(NamedTextColor.GREEN)
                .append(Component.text("  (+${dexEffects.attackSpeedBonus.toInt()}% attack speed)")
                    .color(NamedTextColor.GRAY))
        )

        // INT
        val int = data.getInt()
        val intEffects = StatCalculator.getIntEffects(int)
        sender.sendMessage(
            Component.text("INT: $int")
                .color(NamedTextColor.BLUE)
                .append(Component.text("  (-${intEffects.manaReduction.toInt()}% mana cost, +${intEffects.castSuccessBonus.toInt()}% cast)")
                    .color(NamedTextColor.GRAY))
        )

        // Total skills
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━").color(NamedTextColor.GRAY))
        sender.sendMessage(
            Component.text("Total Skills: ${String.format("%.1f", data.getTotalSkillPoints())} / ${com.hacklab.minecraft.skills.skill.SkillType.TOTAL_SKILL_CAP}")
                .color(NamedTextColor.AQUA)
        )

        return true
    }
}
