package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
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
        val targetSnoopingSkill = targetData.getSkillValue(SkillType.SNOOPING) // Awareness

        // Success chance: SnoopingSkill - TargetSkill/2 + 50
        val successChance = (snoopingSkill - targetSnoopingSkill / 2 + 50).coerceIn(10.0, 90.0)

        // Try skill gain
        plugin.skillManager.tryGainSkill(snooper, SkillType.SNOOPING, targetSnoopingSkill.toInt())

        // Apply alignment penalty for snooping (criminal act)
        plugin.notorietyIntegration.addAlignment(snooper.uniqueId, SNOOP_ALIGNMENT_PENALTY)

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
     * Create a read-only view of target's inventory
     */
    private fun createSnoopInventory(target: Player): Inventory {
        val inventory = Bukkit.createInventory(null, 36, "${target.name}'s Inventory")

        // Copy main inventory items
        for (i in 0 until 36) {
            val item = target.inventory.getItem(i)
            if (item != null) {
                inventory.setItem(i, item.clone())
            }
        }

        return inventory
    }

    /**
     * Handle click in snooping inventory (stealing attempt)
     */
    fun handleSnoopClick(snooper: Player, slot: Int): StealResult {
        val targetUuid = snoopingSessions[snooper.uniqueId] ?: return StealResult.NO_SESSION
        val target = plugin.server.getPlayer(targetUuid) ?: return StealResult.TARGET_OFFLINE

        // Close the inventory first
        snooper.closeInventory()
        snoopingSessions.remove(snooper.uniqueId)

        // Get the item at that slot
        val item = target.inventory.getItem(slot) ?: return StealResult.NO_ITEM

        // Attempt stealing
        return plugin.stealingManager.trySteal(snooper, target, slot, item)
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
