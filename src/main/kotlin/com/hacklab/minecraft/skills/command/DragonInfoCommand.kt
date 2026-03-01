package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DragonInfoCommand(private val plugin: Skills) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.send(sender, MessageKey.SYSTEM_PLAYER_ONLY)
            return true
        }

        val manager = plugin.enderDragonManager

        // Header
        plugin.messageSender.send(sender, MessageKey.DRAGON_INFO_HEADER)

        // Kill count
        plugin.messageSender.send(sender, MessageKey.DRAGON_INFO_KILL_COUNT, "count" to manager.killCount)

        // Dragon alive status or next respawn
        if (manager.isDragonAlive) {
            plugin.messageSender.send(sender, MessageKey.DRAGON_INFO_ALIVE, "level" to manager.getDragonLevel())
        } else {
            val nextRespawn = manager.getNextRespawnTime()
            if (nextRespawn != null) {
                val remaining = nextRespawn - System.currentTimeMillis()
                plugin.messageSender.send(sender, MessageKey.DRAGON_INFO_NEXT_RESPAWN, "time" to formatRemainingTime(remaining))
            }
        }

        // Next dragon stats (what the next spawn will look like)
        plugin.messageSender.send(
            sender, MessageKey.DRAGON_INFO_STATUS,
            "hp" to manager.getScaledMaxHp().toInt(),
            "damage" to String.format("%.1f", manager.getDamageScale())
        )

        // Active skills
        val activeSkills = manager.getActiveSkills()
        if (activeSkills.isNotEmpty()) {
            val skillNames = activeSkills.map { skill ->
                plugin.messageSender.format(sender, skill.messageKey)
            }.joinToString(", ")
            plugin.messageSender.send(sender, MessageKey.DRAGON_INFO_SKILLS, "skills" to skillNames)
        }

        return true
    }

    private fun formatRemainingTime(remainingMs: Long): String {
        if (remainingMs <= 0) return "まもなくリスポーン"

        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}時間${minutes}分"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "まもなくリスポーン"
        }
    }
}
