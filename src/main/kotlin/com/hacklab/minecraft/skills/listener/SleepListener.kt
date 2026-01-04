package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.TimeSkipEvent

/**
 * Listener for sleep-related events
 * Handles cleanup of virtually sleeping players when they:
 * - Leave a bed
 * - Disconnect
 * - Change worlds
 * - When night is skipped
 */
class SleepListener(private val plugin: Skills) : Listener {

    /**
     * When a player enters a bed, notify other players with a clickable message
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        // Check if sleep feature is enabled
        if (!plugin.skillsConfig.sleepEnabled) {
            return
        }

        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) {
            return
        }

        val sleeper = event.player
        val world = sleeper.world

        // Only in overworld
        if (world.environment != World.Environment.NORMAL) {
            return
        }

        // Delay slightly to ensure player is actually in bed
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            // Get all awake players in the same world
            val awakePlayers = world.players.filter { player ->
                player != sleeper &&
                !player.isSleeping &&
                !plugin.sleepCommand.isVirtuallySleeping(player)
            }

            if (awakePlayers.isEmpty()) {
                return@Runnable
            }

            // Send clickable message to each awake player
            awakePlayers.forEach { player ->
                val lang = plugin.localeManager.getLanguage(player)

                // Get localized messages
                val notifyText = plugin.messageManager.get(MessageKey.SLEEP_NOTIFY, lang, "player" to sleeper.name)
                val clickText = plugin.messageManager.get(MessageKey.SLEEP_CLICK_TO_SLEEP, lang)
                val hoverText = plugin.messageManager.get(MessageKey.SLEEP_CLICK_HOVER, lang)

                // Build clickable message
                val message = Component.text(notifyText)
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(" "))
                    .append(
                        Component.text("[$clickText]")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/sleep"))
                            .hoverEvent(HoverEvent.showText(Component.text(hoverText).color(NamedTextColor.GRAY)))
                    )

                player.sendMessage(message)
            }
        }, 5L) // 5 ticks delay
    }

    /**
     * When a player leaves a bed, remove them from virtually sleeping list
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        plugin.sleepCommand.removeVirtualSleeper(event.player)
    }

    /**
     * When a player disconnects, remove them from virtually sleeping list
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.sleepCommand.removeVirtualSleeper(event.player)
    }

    /**
     * When a player changes worlds, remove them from virtually sleeping list
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        plugin.sleepCommand.removeVirtualSleeper(event.player)
    }

    /**
     * When night is skipped (by sleeping), clear all virtually sleeping players in that world
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onTimeSkip(event: TimeSkipEvent) {
        if (event.skipReason == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            plugin.sleepCommand.onDayStart(event.world)
        }
    }
}
