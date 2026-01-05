package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.loot.EndChestLoot
import com.hacklab.minecraft.skills.loot.MagicMob
import com.hacklab.minecraft.skills.loot.ScrollDropManager
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent

/**
 * Listener for scroll drops from mobs and End city chests
 */
class ScrollLootListener(private val plugin: Skills) : Listener {

    private val scrollDropManager = ScrollDropManager(plugin)
    private val endChestLoot = EndChestLoot(plugin)

    /**
     * Handle mob death - drop scrolls from magic mobs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDeath(event: EntityDeathEvent) {
        // Only drop if killed by player
        val killer = event.entity.killer ?: return

        // Check if mob drops are enabled
        if (!scrollDropManager.isMobDropEnabled()) return

        // Check if this is a magic mob
        if (!MagicMob.isMagicMob(event.entity.type)) return

        // Try to get a scroll drop
        val scroll = scrollDropManager.tryGetScrollDrop(event.entity.type)
        if (scroll != null) {
            event.drops.add(scroll)

            if (plugin.skillsConfig.debugMode) {
                plugin.logger.info("${killer.name} received scroll drop from ${event.entity.type}")
            }
        }
    }

    /**
     * Handle loot generation - add scrolls to End city treasure chests and Trial Chamber vaults
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onLootGenerate(event: LootGenerateEvent) {
        val lootTableKey = event.lootTable.key.key

        // Handle End city treasure chests
        if (lootTableKey == "chests/end_city_treasure") {
            handleEndCityLoot(event)
            return
        }

        // Handle Trial Chamber vault rewards
        if (lootTableKey == "chests/trial_chambers/reward" ||
            lootTableKey == "chests/trial_chambers/reward_ominous") {
            handleTrialChamberLoot(event)
            return
        }
    }

    /**
     * Add scrolls to End city treasure chest loot
     */
    private fun handleEndCityLoot(event: LootGenerateEvent) {
        if (!endChestLoot.isEndChestEnabled()) return

        val world = event.lootContext.location.world ?: return
        if (world.environment != World.Environment.THE_END) return

        val scroll = endChestLoot.tryGetEndChestScroll()
        if (scroll != null) {
            event.loot.add(scroll)

            if (plugin.skillsConfig.debugMode) {
                plugin.logger.info("Added scroll to End city chest loot")
            }
        }
    }

    /**
     * Add scrolls to Trial Chamber vault loot
     */
    private fun handleTrialChamberLoot(event: LootGenerateEvent) {
        if (!endChestLoot.isTrialChamberEnabled()) return

        val scroll = endChestLoot.tryGetTrialChamberScroll()
        if (scroll != null) {
            event.loot.add(scroll)

            if (plugin.skillsConfig.debugMode) {
                plugin.logger.info("Added scroll to Trial Chamber vault loot: ${scroll.type}")
            }
        }
    }
}
