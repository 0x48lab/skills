package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.gathering.GatheringDifficulty
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerFishEvent

class GatheringListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val drops = event.block.getDrops(player.inventory.itemInMainHand).toMutableList()

        // Check if it's an ore (Mining)
        if (GatheringDifficulty.isOre(block.type)) {
            plugin.gatheringManager.processMining(player, block, drops)

            // Apply modified drops
            event.isDropItems = false
            drops.forEach { drop ->
                block.world.dropItemNaturally(block.location, drop)
            }
        }
        // Check if it's a log (Lumberjacking)
        else if (GatheringDifficulty.isLog(block.type)) {
            plugin.gatheringManager.processLumberjacking(player, block, drops)

            // Apply modified drops
            event.isDropItems = false
            drops.forEach { drop ->
                block.world.dropItemNaturally(block.location, drop)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player

        // Only process when actually catching something
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH &&
            event.state != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return
        }

        val caught = event.caught as? Item ?: return

        // Process fishing
        plugin.gatheringManager.processFishing(player, caught)
    }
}
