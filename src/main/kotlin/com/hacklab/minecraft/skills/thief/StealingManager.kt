package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class StealingManager(private val plugin: Skills) {

    /**
     * Attempt to steal an item from target
     */
    fun trySteal(thief: Player, target: Player, slot: Int, item: ItemStack): StealResult {
        val thiefData = plugin.playerDataManager.getPlayerData(thief)
        val targetData = plugin.playerDataManager.getPlayerData(target)

        val stealingSkill = thiefData.getSkillValue(SkillType.STEALING)
        val targetDex = targetData.dex

        // Calculate difficulty based on item weight/value
        val itemDifficulty = calculateItemDifficulty(item)

        // Success chance: StealingSkill - (TargetDEX/2) - ItemDifficulty + 50
        val successChance = (stealingSkill - targetDex / 2 - itemDifficulty + 50).coerceIn(5.0, 80.0)

        // Try skill gain
        plugin.skillManager.tryGainSkill(thief, SkillType.STEALING, itemDifficulty + targetDex / 2)

        if (Random.nextDouble() * 100 > successChance) {
            // Failed - notify target
            plugin.messageSender.send(target, MessageKey.THIEF_STEAL_NOTICED,
                "player" to thief.name,
                "item" to item.type.name.lowercase().replace("_", " "))
            plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_FAILED)

            // Report crime to notoriety system if available
            reportCrime(thief, target, item, false)

            return StealResult.FAILED
        }

        // Success - transfer item
        val actualItem = target.inventory.getItem(slot)
        if (actualItem == null || actualItem.type != item.type) {
            // Item was moved
            return StealResult.ITEM_MOVED
        }

        // Take one item (or the whole stack for small stacks)
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
            // Drop on ground if inventory full
            thief.world.dropItem(thief.location, remaining.values.first())
        }

        plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_SUCCESS,
            "item" to stolen.type.name.lowercase().replace("_", " "))

        // Report crime
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
        // Integration with notoriety plugin would go here
        // For now, just log
        if (success) {
            plugin.logger.info("${thief.name} stole ${item.type} from ${victim.name}")
        } else {
            plugin.logger.info("${thief.name} failed to steal from ${victim.name}")
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
