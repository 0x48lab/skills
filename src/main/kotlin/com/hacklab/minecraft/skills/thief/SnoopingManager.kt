package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SnoopingManager(private val plugin: Skills) {
    companion object {
        const val SNOOP_ALIGNMENT_PENALTY = -5
    }

    // Track active snooping sessions: snooper UUID -> target UUID
    private val snoopingSessions: MutableMap<UUID, UUID> = ConcurrentHashMap()

    /**
     * Attempt to snoop on a target player
     */
    fun trySnoop(snooper: Player, target: Player): SnoopResult {
        if (snooper == target) {
            return SnoopResult.INVALID_TARGET
        }

        val snooperData = plugin.playerDataManager.getPlayerData(snooper)
        val targetData = plugin.playerDataManager.getPlayerData(target)

        val snoopingSkill = snooperData.getSkillValue(SkillType.SNOOPING)
        val targetDetectingHidden = targetData.getSkillValue(SkillType.DETECTING_HIDDEN)

        // Success chance: SnoopingSkill - TargetDetectingHidden/2 + 20
        val successChance = (snoopingSkill - targetDetectingHidden / 2 + 20).coerceIn(5.0, 95.0)

        // Try skill gain (difficulty = target's average skill level)
        val snoopDifficulty = (targetData.getTotalSkillPoints() / SkillType.entries.size).toInt()
        plugin.skillManager.tryGainSkill(snooper, SkillType.SNOOPING, snoopDifficulty)

        // Apply alignment penalty for snooping (criminal act)
        if (plugin.notorietyIntegration.isAvailable()) {
            plugin.notorietyIntegration.addAlignment(snooper.uniqueId, SNOOP_ALIGNMENT_PENALTY)
        }

        if (Random.nextDouble() * 100 > successChance) {
            // Failed - notify target
            plugin.messageSender.send(target, MessageKey.THIEF_SNOOP_NOTICED,
                "player" to snooper.name)
            plugin.messageSender.send(snooper, MessageKey.THIEF_SNOOP_FAILED)
            return SnoopResult.FAILED
        }

        // Success - open inventory view
        val snoopInventory = createSnoopInventory(target)
        snooper.openInventory(snoopInventory)

        // Track session for stealing
        snoopingSessions[snooper.uniqueId] = target.uniqueId

        plugin.messageSender.send(snooper, MessageKey.THIEF_SNOOP_SUCCESS,
            "player" to target.name)

        return SnoopResult.SUCCESS
    }

    /**
     * Create a read-only view of target's inventory (54 slots = 6 rows)
     * Rows 1-4: Main inventory (slots 0-35)
     * Row 5: Separator (gray glass panes, slots 36-44)
     * Row 6: Equipment (slots 45-48: helmet/chest/legs/boots, slot 49: offhand, 50-53: empty)
     */
    private fun createSnoopInventory(target: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 54, "${target.name}'s Inventory")

        // Copy main inventory items (slots 0-35)
        for (i in 0 until 36) {
            val item = target.inventory.getItem(i)
            if (item != null) {
                inventory.setItem(i, item.clone())
            }
        }

        // Separator row (slots 36-44): gray glass panes
        val separator = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.also { it.setDisplayName(" ") }
        }
        for (i in 36..44) {
            inventory.setItem(i, separator)
        }

        // Equipment slots (slots 45-49)
        // 45: Helmet, 46: Chestplate, 47: Leggings, 48: Boots, 49: Offhand
        target.inventory.helmet?.let { inventory.setItem(45, it.clone()) }
        target.inventory.chestplate?.let { inventory.setItem(46, it.clone()) }
        target.inventory.leggings?.let { inventory.setItem(47, it.clone()) }
        target.inventory.boots?.let { inventory.setItem(48, it.clone()) }
        target.inventory.itemInOffHand.let {
            if (!it.type.isAir) inventory.setItem(49, it.clone())
        }

        return inventory
    }

    /**
     * Handle click in snooping inventory (stealing attempt)
     * Slot ranges: 0-35 = main inventory, 36-44 = separator (ignore), 45-49 = equipment, 50-53 = ignore
     */
    fun handleSnoopClick(snooper: Player, slot: Int): StealResult {
        val targetUuid = snoopingSessions[snooper.uniqueId] ?: return StealResult.NO_SESSION
        val target = plugin.server.getPlayer(targetUuid) ?: return StealResult.TARGET_OFFLINE

        // Ignore separator and empty slots
        if (slot in 36..44 || slot in 50..53) {
            return StealResult.NO_ITEM
        }

        // Close the inventory first
        snooper.closeInventory()
        snoopingSessions.remove(snooper.uniqueId)

        if (slot in 0..35) {
            // Main inventory steal
            val item = target.inventory.getItem(slot) ?: return StealResult.NO_ITEM
            return plugin.stealingManager.trySteal(snooper, target, slot, item)
        }

        if (slot in 45..49) {
            // Equipment steal
            val equipmentItem = when (slot) {
                45 -> target.inventory.helmet
                46 -> target.inventory.chestplate
                47 -> target.inventory.leggings
                48 -> target.inventory.boots
                49 -> target.inventory.itemInOffHand.takeIf { !it.type.isAir }
                else -> null
            } ?: return StealResult.NO_ITEM
            return plugin.stealingManager.trySteal(snooper, target, slot, equipmentItem, isEquipment = true)
        }

        return StealResult.NO_ITEM
    }

    /**
     * Clean up session when inventory is closed
     */
    fun endSession(playerId: UUID) {
        snoopingSessions.remove(playerId)
    }

    /**
     * Check if player is currently snooping
     */
    fun isSnooping(playerId: UUID): Boolean {
        return snoopingSessions.containsKey(playerId)
    }

    /**
     * Get the target being snooped
     */
    fun getSnoopTarget(snooperId: UUID): UUID? {
        return snoopingSessions[snooperId]
    }
}

enum class SnoopResult {
    SUCCESS,
    FAILED,
    INVALID_TARGET
}
