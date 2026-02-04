package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.TimeSkipEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Listener for sleep-related events
 * Handles cleanup of virtually sleeping players when they:
 * - Leave a bed
 * - Disconnect
 * - Change worlds
 * - When night is skipped
 *
 * Also handles Bed Rest Recovery feature:
 * - Players who sleep in a bed get HP and hunger restored when they wake up
 */
class SleepListener(private val plugin: Skills) : Listener {

    /**
     * Track players who entered a bed (for bed rest recovery)
     * Only players in this set are eligible for recovery when leaving bed
     */
    private val playersInBed: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /**
     * When a player enters a bed, notify other players with a clickable message
     * and track them for bed rest recovery
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

        // Track player for bed rest recovery
        playersInBed.add(sleeper.uniqueId)

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
     * and apply bed rest recovery if enabled
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        val player = event.player

        plugin.sleepCommand.removeVirtualSleeper(player)

        // Check if player was actually in bed (not "sleep along" participant)
        if (playersInBed.remove(player.uniqueId)) {
            applyBedRestRecovery(player)
        }
    }

    /**
     * Apply bed rest recovery to a player who slept in a bed
     * Restores internal HP to full and hunger to full
     */
    private fun applyBedRestRecovery(player: org.bukkit.entity.Player) {
        // Check if feature is enabled
        if (!plugin.skillsConfig.bedRestRecoveryEnabled) {
            return
        }

        // Restore internal HP to full
        plugin.healthManager.fullHeal(player)

        // Restore hunger to full (20 = max food level)
        player.foodLevel = 20
        player.saturation = 5.0f

        // Play recovery sound
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)

        // Send recovery message
        val lang = plugin.localeManager.getLanguage(player)
        val message = plugin.messageManager.get(MessageKey.BED_REST_RECOVERY, lang, "player" to player.name)
        player.sendMessage(message)
    }

    /**
     * When a player disconnects, remove them from virtually sleeping list
     * and clean up bed tracking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.sleepCommand.removeVirtualSleeper(event.player)
        playersInBed.remove(event.player.uniqueId)
    }

    /**
     * When a player changes worlds, remove them from virtually sleeping list
     * and clean up bed tracking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        plugin.sleepCommand.removeVirtualSleeper(event.player)
        playersInBed.remove(event.player.uniqueId)
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
