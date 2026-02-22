package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.integration.NotorietyIntegration
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class StealingManager(private val plugin: Skills) {

    /**
     * Attempt to steal an item from target
     * @param isEquipment true if stealing from equipment slots (adds penalty)
     */
    fun trySteal(thief: Player, target: Player, slot: Int, item: ItemStack, isEquipment: Boolean = false): StealResult {
        val thiefData = plugin.playerDataManager.getPlayerData(thief)
        val targetData = plugin.playerDataManager.getPlayerData(target)

        val stealingSkill = thiefData.getSkillValue(SkillType.STEALING)
        val targetDetectingHidden = targetData.getSkillValue(SkillType.DETECTING_HIDDEN)

        // Calculate difficulty based on item weight/value
        var itemDifficulty = calculateItemDifficulty(item)
        if (isEquipment) {
            itemDifficulty += plugin.skillsConfig.equipmentStealPenalty
        }

        // Success chance: StealingSkill - (TargetDetectingHidden/2) - ItemDifficulty + 10
        val successChance = (stealingSkill - targetDetectingHidden / 2 - itemDifficulty + 10).coerceIn(5.0, 90.0)

        // Try skill gain
        plugin.skillManager.tryGainSkill(thief, SkillType.STEALING, itemDifficulty + targetDetectingHidden.toInt() / 2)

        if (Random.nextDouble() * 100 > successChance) {
            // Failed - notify target
            plugin.messageSender.send(target, MessageKey.THIEF_STEAL_NOTICED,
                "player" to thief.name,
                "item" to item.type.name.lowercase().replace("_", " "))
            plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_FAILED)

            // Report crime to notoriety system
            reportCrime(thief, target, item, false)

            return StealResult.FAILED
        }

        if (isEquipment) {
            // Equipment steal - remove from equipment slot
            val equipmentItem = when (slot) {
                45 -> target.inventory.helmet
                46 -> target.inventory.chestplate
                47 -> target.inventory.leggings
                48 -> target.inventory.boots
                49 -> target.inventory.itemInOffHand.takeIf { !it.type.isAir }
                else -> null
            }
            if (equipmentItem == null || equipmentItem.type != item.type) {
                return StealResult.ITEM_MOVED
            }

            val stolen = equipmentItem.clone()
            when (slot) {
                45 -> target.inventory.helmet = null
                46 -> target.inventory.chestplate = null
                47 -> target.inventory.leggings = null
                48 -> target.inventory.boots = null
                49 -> target.inventory.setItemInOffHand(ItemStack(org.bukkit.Material.AIR))
            }

            // Give to thief
            val remaining = thief.inventory.addItem(stolen)
            if (remaining.isNotEmpty()) {
                thief.world.dropItem(thief.location, remaining.values.first())
            }

            plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_SUCCESS,
                "item" to stolen.type.name.lowercase().replace("_", " "))

            reportCrime(thief, target, stolen, true)
            return StealResult.SUCCESS
        }

        // Normal inventory steal - transfer item
        val actualItem = target.inventory.getItem(slot)
        if (actualItem == null || actualItem.type != item.type) {
            return StealResult.ITEM_MOVED
        }

        val stolen = if (actualItem.amount > 1) {
            actualItem.amount -= 1
            val stolenItem = actualItem.clone()
            stolenItem.amount = 1
            stolenItem
        } else {
            target.inventory.setItem(slot, null)
            actualItem
        }

        // Give to thief
        val remaining = thief.inventory.addItem(stolen)
        if (remaining.isNotEmpty()) {
            thief.world.dropItem(thief.location, remaining.values.first())
        }

        plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_SUCCESS,
            "item" to stolen.type.name.lowercase().replace("_", " "))

        reportCrime(thief, target, stolen, true)

        return StealResult.SUCCESS
    }

    /**
     * Calculate item difficulty for stealing
     */
    private fun calculateItemDifficulty(item: ItemStack): Int {
        // Based on item rarity/value
        return when {
            item.type.name.contains("NETHERITE") -> 50
            item.type.name.contains("DIAMOND") -> 40
            item.type.name.contains("GOLD") || item.type.name.contains("GOLDEN") -> 25
            item.type.name.contains("IRON") -> 20
            item.type.name.contains("ENCHANTED") -> 35
            item.type == org.bukkit.Material.TOTEM_OF_UNDYING -> 60
            item.type == org.bukkit.Material.ELYTRA -> 60
            else -> 10
        }
    }

    /**
     * Report theft to notoriety system
     */
    private fun reportCrime(thief: Player, victim: Player, item: ItemStack, success: Boolean) {
        if (plugin.notorietyIntegration.isAvailable()) {
            val penalty = if (success) NotorietyIntegration.STEAL_PENALTY else NotorietyIntegration.STEAL_FAILED_PENALTY
            plugin.notorietyIntegration.addAlignment(thief.uniqueId, penalty)
        }
    }
}

enum class StealResult {
    SUCCESS,
    FAILED,
    NO_SESSION,
    TARGET_OFFLINE,
    NO_ITEM,
    ITEM_MOVED
}
