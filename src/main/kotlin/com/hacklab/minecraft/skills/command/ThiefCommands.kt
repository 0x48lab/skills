package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.TargetingAction
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HideCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.HIDE)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.HIDE)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        if (plugin.hidingManager.isHidden(sender.uniqueId)) {
            plugin.hidingManager.breakHiding(sender, "manual")
        } else {
            plugin.hidingManager.tryHide(sender)
            // Set cooldown after attempt (success or fail)
            plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.HIDE)
        }

        return true
    }
}

class DetectCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.DETECT)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.DETECT)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        plugin.detectingManager.tryDetect(sender)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.DETECT)
        return true
    }
}

class SnoopCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.SNOOP)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.SNOOP)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        // Enter targeting mode for snooping
        plugin.targetManager.startTargeting(sender, TargetingAction.Snoop)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.SNOOP)
        return true
    }
}

class PoisonCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.POISON)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.POISON)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        plugin.poisoningManager.applyPoison(sender)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.POISON)
        return true
    }
}
