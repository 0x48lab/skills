package com.hacklab.minecraft.skills.command

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * /sleep command - allows players to be counted as sleeping without being in a bed
 *
 * Conditions:
 * - Must be night time in the overworld
 * - At least one player must already be sleeping in a bed
 * - The executing player must not already be sleeping
 *
 * Effect:
 * - Marks the player as "sleeping" for the sleep percentage calculation
 * - When enough players are sleeping (including /sleep users), night will skip
 */
class SleepCommand(private val plugin: Skills) : CommandExecutor {

    // Track players who are virtually sleeping via /sleep
    private val virtuallySleeping = mutableSetOf<Player>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            plugin.messageSender.sendRaw(sender, "This command is for players only.")
            return true
        }

        // Check if sleep feature is enabled
        if (!plugin.skillsConfig.sleepEnabled) {
            plugin.messageSender.send(sender, MessageKey.SLEEP_DISABLED)
            return true
        }

        val world = sender.world

        // Check if it's overworld
        if (world.environment != World.Environment.NORMAL) {
            plugin.messageSender.send(sender, MessageKey.SLEEP_WRONG_DIMENSION)
            return true
        }

        // Check if it's night time (time >= 12541 and < 23460 for sleeping to work)
        val time = world.time
        if (time < 12541 || time >= 23460) {
            plugin.messageSender.send(sender, MessageKey.SLEEP_NOT_NIGHT)
            return true
        }

        // Check if someone is already sleeping in a bed
        val sleepingInBed = world.players.any { it.isSleeping }
        if (!sleepingInBed) {
            plugin.messageSender.send(sender, MessageKey.SLEEP_NO_SLEEPING_PLAYERS)
            return true
        }

        // Check if already sleeping (either in bed or virtually)
        if (sender.isSleeping || virtuallySleeping.contains(sender)) {
            plugin.messageSender.send(sender, MessageKey.SLEEP_ALREADY_SLEEPING)
            return true
        }

        // Mark as virtually sleeping
        virtuallySleeping.add(sender)
        plugin.messageSender.send(sender, MessageKey.SLEEP_STARTED)

        // Calculate and broadcast sleeping percentage
        val totalPlayers = world.players.size
        val sleepingCount = world.players.count { it.isSleeping } + virtuallySleeping.count { it.world == world && !it.isSleeping }
        val percentage = (sleepingCount * 100) / totalPlayers

        // Broadcast to world
        world.players.forEach { player ->
            plugin.messageSender.send(player, MessageKey.SLEEP_PERCENTAGE,
                "count" to sleepingCount,
                "total" to totalPlayers,
                "percentage" to percentage
            )
        }

        // Check if enough players are sleeping to skip night
        // Minecraft requires a certain percentage (gamerule playersSleepingPercentage, default 100)
        checkAndSkipNight(world)

        return true
    }

    /**
     * Check if enough players are sleeping and skip the night if so
     */
    private fun checkAndSkipNight(world: World) {
        val totalPlayers = world.players.size
        if (totalPlayers == 0) return

        val sleepingInBed = world.players.count { it.isSleeping }
        val virtuallySleepingInWorld = virtuallySleeping.count { it.world == world && !it.isSleeping }
        val totalSleeping = sleepingInBed + virtuallySleepingInWorld

        // Get the required percentage (default is 100%)
        val requiredPercentage = try {
            world.getGameRuleValue(org.bukkit.GameRule.PLAYERS_SLEEPING_PERCENTAGE) ?: 100
        } catch (e: Exception) {
            100
        }

        val currentPercentage = (totalSleeping * 100) / totalPlayers

        if (currentPercentage >= requiredPercentage) {
            // Skip the night
            world.time = 0
            world.setStorm(false)
            world.isThundering = false

            // Clear virtually sleeping players and notify
            val sleepersInWorld = virtuallySleeping.filter { it.world == world }
            sleepersInWorld.forEach { player ->
                plugin.messageSender.send(player, MessageKey.SLEEP_NIGHT_SKIPPED)
            }
            virtuallySleeping.removeAll(sleepersInWorld.toSet())

            // Notify all players in the world
            world.players.forEach { player ->
                if (!sleepersInWorld.contains(player)) {
                    plugin.messageSender.send(player, MessageKey.SLEEP_NIGHT_SKIPPED)
                }
            }
        }
    }

    /**
     * Called when a player leaves a bed or disconnects
     * Remove them from virtually sleeping list
     */
    fun removeVirtualSleeper(player: Player) {
        if (virtuallySleeping.remove(player)) {
            plugin.messageSender.send(player, MessageKey.SLEEP_CANCELLED)
        }
    }

    /**
     * Called when the world time changes to day
     * Clear all virtually sleeping players in that world
     */
    fun onDayStart(world: World) {
        virtuallySleeping.removeAll { it.world == world }
    }

    /**
     * Check if a player is virtually sleeping
     */
    fun isVirtuallySleeping(player: Player): Boolean {
        return virtuallySleeping.contains(player)
    }

    /**
     * Get count of virtually sleeping players in a world
     */
    fun getVirtuallySleeperCount(world: World): Int {
        return virtuallySleeping.count { it.world == world && !it.isSleeping }
    }
}
