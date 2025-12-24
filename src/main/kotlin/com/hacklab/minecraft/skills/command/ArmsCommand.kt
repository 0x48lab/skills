package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.util.CooldownAction
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class ArmsCommand(private val plugin: Skills) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command is for players only.")
            return true
        }

        // Check cooldown
        if (plugin.cooldownManager.isOnCooldown(sender.uniqueId, CooldownAction.ARMS_LORE)) {
            val remaining = plugin.cooldownManager.getRemainingCooldown(sender.uniqueId, CooldownAction.ARMS_LORE)
            plugin.messageSender.send(sender, MessageKey.COOLDOWN_ACTIVE, "seconds" to remaining.toString())
            return true
        }

        // Get item in main hand
        val item = sender.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            sender.sendMessage("Hold an item to evaluate it with Arms Lore.")
            return true
        }

        // Check if it's a weapon or armor
        if (!isWeaponOrArmor(item)) {
            sender.sendMessage("This item is not a weapon or armor.")
            return true
        }

        // Evaluate the item
        evaluateItem(sender, item)

        // Set cooldown and try skill gain
        plugin.cooldownManager.setCooldown(sender.uniqueId, CooldownAction.ARMS_LORE)
        plugin.skillManager.tryGainSkill(sender, SkillType.ARMS_LORE, getDifficulty(item))

        return true
    }

    private fun isWeaponOrArmor(item: ItemStack): Boolean {
        val type = item.type
        return isWeapon(type) || isArmor(type) || isShield(type)
    }

    private fun isWeapon(material: Material): Boolean {
        val name = material.name
        return name.endsWith("_SWORD") ||
                name.endsWith("_AXE") ||
                name.endsWith("_PICKAXE") ||
                name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE") ||
                material == Material.BOW ||
                material == Material.CROSSBOW ||
                material == Material.TRIDENT ||
                material == Material.MACE
    }

    private fun isArmor(material: Material): Boolean {
        val name = material.name
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS") ||
                name.endsWith("_CAP") ||
                name.endsWith("_TUNIC") ||
                name.endsWith("_PANTS")
    }

    private fun isShield(material: Material): Boolean {
        return material == Material.SHIELD
    }

    private fun evaluateItem(player: Player, item: ItemStack) {
        val data = plugin.playerDataManager.getPlayerData(player)
        val armsLoreSkill = data.getSkillValue(SkillType.ARMS_LORE)

        plugin.messageSender.send(player, MessageKey.ARMS_LORE_HEADER)

        // Quality (always shown with enough skill)
        val quality = plugin.qualityManager.getQuality(item)
        if (armsLoreSkill >= 10.0) {
            plugin.messageSender.send(player, MessageKey.ARMS_LORE_QUALITY, "quality" to quality.displayName)
        }

        // Damage info for weapons
        if (isWeapon(item.type) && armsLoreSkill >= 30.0) {
            val damage = getWeaponDamage(item)
            val qualityMod = quality.modifier
            val effectiveDamage = String.format("%.1f", damage * qualityMod)
            plugin.messageSender.send(player, MessageKey.ARMS_LORE_DAMAGE, "damage" to effectiveDamage)
        }

        // Durability info
        if (armsLoreSkill >= 20.0) {
            val meta = item.itemMeta
            if (meta is Damageable) {
                val maxDurability = item.type.maxDurability.toInt()
                val currentDurability = maxDurability - meta.damage
                plugin.messageSender.send(
                    player,
                    MessageKey.ARMS_LORE_DURABILITY,
                    "current" to currentDurability.toString(),
                    "max" to maxDurability.toString()
                )
            }
        }
    }

    private fun getWeaponDamage(item: ItemStack): Double {
        // Base damage values for different weapon types
        return when {
            item.type.name.contains("NETHERITE") -> 8.0
            item.type.name.contains("DIAMOND") -> 7.0
            item.type.name.contains("IRON") -> 6.0
            item.type.name.contains("GOLD") -> 4.0
            item.type.name.contains("STONE") -> 5.0
            item.type.name.contains("WOOD") -> 4.0
            item.type == Material.TRIDENT -> 9.0
            item.type == Material.MACE -> 7.0
            item.type == Material.BOW || item.type == Material.CROSSBOW -> 6.0
            else -> 4.0
        }
    }

    private fun getDifficulty(item: ItemStack): Int {
        // Higher tier items are more difficult to evaluate
        return when {
            item.type.name.contains("NETHERITE") -> 80
            item.type.name.contains("DIAMOND") -> 60
            item.type.name.contains("IRON") -> 40
            item.type.name.contains("GOLD") -> 30
            else -> 20
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        return emptyList()
    }
}
