package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillCategory
import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SkillsCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        if (args.isEmpty()) {
            showAllSkills(sender)
            return true
        }

        when (args[0].lowercase()) {
            "list" -> showAllSkills(sender)
            "guide" -> giveGuideBook(sender)
            "lock" -> {
                if (args.size < 2) {
                    sender.sendMessage("Usage: /skills lock <skill>")
                    return true
                }
                toggleSkillLock(sender, args.drop(1).joinToString(" "))
            }
            "category" -> {
                if (args.size < 2) {
                    sender.sendMessage("Usage: /skills category <category>")
                    return true
                }
                showCategory(sender, args[1])
            }
            else -> {
                // Try to find skill by name
                val skillName = args.joinToString(" ")
                showSkill(sender, skillName)
            }
        }

        return true
    }

    private fun showAllSkills(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Show title
        val title = plugin.skillTitleManager.getPlayerTitle(player, useJapanese = false)
        player.sendMessage(Component.text("═══ $title ═══").color(NamedTextColor.GOLD))

        // Group by category
        SkillCategory.entries.forEach { category ->
            val categorySkills = SkillType.entries.filter { it.category == category }
            if (categorySkills.isNotEmpty()) {
                player.sendMessage(Component.text(""))
                player.sendMessage(Component.text("═══ ${category.displayName} ═══").color(NamedTextColor.GOLD))

                categorySkills.forEach { skill ->
                    val value = data.getSkillValue(skill)
                    val lockMode = data.getSkillLock(skill)
                    val color = when {
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
                    player.sendMessage(
                        Component.text("  ${skill.displayName}: ")
                            .color(NamedTextColor.WHITE)
                            .append(Component.text(String.format("%.1f", value)).color(color))
                            .append(Component.text(" ${lockMode.getSymbol()}").color(lockColor))
                    )
                }
            }
        }

        // Show total
        player.sendMessage(Component.text(""))
        player.sendMessage(
            Component.text("Total: ${String.format("%.1f", data.getTotalSkillPoints())} / ${SkillType.TOTAL_SKILL_CAP}")
                .color(NamedTextColor.AQUA)
        )
    }

    private fun showCategory(player: Player, categoryName: String) {
        val category = SkillCategory.entries.find {
            it.name.equals(categoryName, ignoreCase = true) ||
                    it.displayName.equals(categoryName, ignoreCase = true)
        }

        if (category == null) {
            player.sendMessage(Component.text("Unknown category: $categoryName").color(NamedTextColor.RED))
            return
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val skills = SkillType.entries.filter { it.category == category }

        player.sendMessage(Component.text("═══ ${category.displayName} Skills ═══").color(NamedTextColor.GOLD))

        skills.forEach { skill ->
            val value = data.getSkillValue(skill)
            player.sendMessage(
                Component.text("  ${skill.displayName}: ${String.format("%.1f", value)}")
                    .color(NamedTextColor.WHITE)
            )
        }
    }

    private fun showSkill(player: Player, skillName: String) {
        val skill = SkillType.fromDisplayName(skillName)
            ?: SkillType.entries.find { it.name.equals(skillName.replace(" ", "_"), ignoreCase = true) }

        if (skill == null) {
            player.sendMessage(Component.text("Unknown skill: $skillName").color(NamedTextColor.RED))
            return
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val skillData = data.getSkill(skill)

        player.sendMessage(Component.text("═══ ${skill.displayName} ═══").color(NamedTextColor.GOLD))
        player.sendMessage(Component.text("Value: ${String.format("%.1f", skillData.value)}").color(NamedTextColor.WHITE))
        player.sendMessage(Component.text("Category: ${skill.category.displayName}").color(NamedTextColor.GRAY))

        // Show stat contribution
        val statInfo = mutableListOf<String>()
        if (skill.strWeight > 0) statInfo.add("STR: ${(skill.strWeight * 100).toInt()}%")
        if (skill.dexWeight > 0) statInfo.add("DEX: ${(skill.dexWeight * 100).toInt()}%")
        if (skill.intWeight > 0) statInfo.add("INT: ${(skill.intWeight * 100).toInt()}%")
        if (statInfo.isNotEmpty()) {
            player.sendMessage(Component.text("Affects: ${statInfo.joinToString(", ")}").color(NamedTextColor.AQUA))
        }
    }

    private fun giveGuideBook(player: Player) {
        val guidebook = plugin.guideManager.createGuideBook(plugin.localeManager.getLanguage(player))

        val leftover = player.inventory.addItem(guidebook)
        if (leftover.isEmpty()) {
            plugin.messageSender.send(player, MessageKey.GUIDEBOOK_RECEIVED)
        } else {
            // Inventory full, drop at feet
            leftover.values.forEach { item ->
                player.world.dropItemNaturally(player.location, item)
            }
            plugin.messageSender.send(player, MessageKey.GUIDEBOOK_DROPPED)
        }
    }

    private fun toggleSkillLock(player: Player, skillName: String) {
        val skill = SkillType.fromDisplayName(skillName)
            ?: SkillType.entries.find { it.name.equals(skillName.replace(" ", "_"), ignoreCase = true) }

        if (skill == null) {
            player.sendMessage(Component.text("Unknown skill: $skillName").color(NamedTextColor.RED))
            return
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val newMode = data.toggleSkillLock(skill)

        val modeColor = when (newMode) {
            SkillLockMode.UP -> NamedTextColor.GREEN
            SkillLockMode.DOWN -> NamedTextColor.RED
            SkillLockMode.LOCKED -> NamedTextColor.YELLOW
        }
        val modeText = when (newMode) {
            SkillLockMode.UP -> "UP (can increase)"
            SkillLockMode.DOWN -> "DOWN (can decrease)"
            SkillLockMode.LOCKED -> "LOCKED (no change)"
        }

        player.sendMessage(
            Component.text("${skill.displayName} lock: ")
                .color(NamedTextColor.WHITE)
                .append(Component.text("${newMode.getSymbol()} $modeText").color(modeColor))
        )
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            val options = mutableListOf("list", "guide", "category", "lock")
            options.addAll(SkillType.entries.map { it.displayName })
            return options.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("category", ignoreCase = true)) {
            return SkillCategory.entries.map { it.displayName }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("lock", ignoreCase = true)) {
            return SkillType.entries.map { it.displayName }
                .filter { it.lowercase().startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }
}
