package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.TargetingAction
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TameCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.TAME)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.TAME)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        // Enter targeting mode for taming
        plugin.targetManager.startTargeting(sender, TargetingAction.Tame)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.TAME)
        return true
    }
}

class LoreCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.ANIMAL_LORE)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.ANIMAL_LORE)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        // Enter targeting mode for animal lore
        plugin.targetManager.startTargeting(sender, TargetingAction.Lore)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.ANIMAL_LORE)
        return true
    }
}

class EvaluateCommand(private val plugin: Skills) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.EVALUATE)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.EVALUATE)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        // Enter targeting mode for evaluate intelligence
        plugin.targetManager.startTargeting(sender, TargetingAction.Evaluate)
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.EVALUATE)
        return true
    }
}
