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
        // Block stealing from party members
        if (plugin.partyManager.isInSameParty(thief.uniqueId, target.uniqueId)) {
            plugin.messageSender.send(thief, MessageKey.PARTY_STEAL_BLOCKED)
            return StealResult.FAILED
        }

        val thiefData = plugin.playerDataManager.getPlayerData(thief)
        val targetData = plugin.playerDataManager.getPlayerData(target)

        val stealingSkill = thiefData.getSkillValue(SkillType.STEALING)
        val targetDetectingHidden = targetData.getSkillValue(SkillType.DETECTING_HIDDEN)

        // Equipment steal requires Stealing 80+
        if (isEquipment && stealingSkill < 80.0) {
            plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_EQUIP_SKILL_TOO_LOW,
                "required" to 80)
            return StealResult.SKILL_TOO_LOW
        }

        // Item tier restriction based on Stealing skill
        val requiredSkill = getRequiredSkillForItem(item)
        if (stealingSkill < requiredSkill) {
            plugin.messageSender.send(thief, MessageKey.THIEF_STEAL_SKILL_TOO_LOW,
                "required" to requiredSkill)
            return StealResult.SKILL_TOO_LOW
        }

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
     * Get the minimum Stealing skill required to steal this item.
     * Tier 0 (skill 0+):  Common items (food, blocks, basic materials)
     * Tier 1 (skill 30+): Iron/gold items
     * Tier 2 (skill 50+): Diamond items, enchanted items
     * Tier 3 (skill 70+): Netherite, totem, elytra
     * Equipment steal requires skill 80+ (checked separately)
     */
    private fun getRequiredSkillForItem(item: ItemStack): Int {
        // Tier 3: Legendary items (skill 70+)
        if (item.type.name.contains("NETHERITE")) return 70
        if (item.type == org.bukkit.Material.TOTEM_OF_UNDYING) return 70
        if (item.type == org.bukkit.Material.ELYTRA) return 70

        // Tier 2: High-value items (skill 50+)
        if (item.type.name.contains("DIAMOND")) return 50
        if (item.itemMeta?.hasEnchants() == true) return 50

        // Tier 1: Mid-value items (skill 30+)
        if (item.type.name.contains("IRON")) return 30
        if (item.type.name.contains("GOLD") || item.type.name.contains("GOLDEN")) return 30

        // Tier 0: Common items (skill 0+)
        return 0
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
            item.type == org.bukkit.Material.TOTEM_OF_UNDYING -> 60
            item.type == org.bukkit.Material.ELYTRA -> 60
            item.itemMeta?.hasEnchants() == true -> 35
            else -> 10
        }
    }

    /**
     * Report theft to notoriety system.
     * recordCrime applies alignment penalty internally via CrimeType.defaultPenalty (-5).
     * For successful theft, apply additional penalty to total -10.
     */
    private fun reportCrime(thief: Player, victim: Player, item: ItemStack, success: Boolean) {
        if (plugin.notorietyIntegration.isAvailable()) {
            val status = if (success) "Success" else "Failed"
            val itemName = item.type.name.lowercase().replace("_", " ")

            // recordCrime applies CrimeType.THEFT defaultPenalty (-5) internally
            plugin.notorietyIntegration.recordCrime(
                criminal = thief.uniqueId,
                crimeTypeName = NotorietyIntegration.CRIME_THEFT,
                victim = victim.uniqueId,
                location = thief.location,
                detail = "Stealing ($status): $itemName from ${victim.name}"
            )

            // Successful theft gets additional penalty (-5 more, total -10)
            if (success) {
                plugin.notorietyIntegration.addAlignment(thief.uniqueId, NotorietyIntegration.STEAL_SUCCESS_EXTRA_PENALTY)
            }

            // Direct crime against a player: ensure thief becomes gray immediately
            plugin.notorietyIntegration.ensureGray(thief.uniqueId)
        }
    }
}

enum class StealResult {
    SUCCESS,
    FAILED,
    NO_SESSION,
    TARGET_OFFLINE,
    NO_ITEM,
    ITEM_MOVED,
    OUT_OF_RANGE,
    SKILL_TOO_LOW
}
