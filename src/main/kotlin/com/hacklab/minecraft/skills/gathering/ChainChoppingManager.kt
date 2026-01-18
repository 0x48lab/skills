package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.LinkedList

/**
 * Manages chain chopping for Lumberjacking skill.
 * Higher skill = more connected logs chopped in chain.
 * GM (100) can chop entire tree from base.
 *
 * Algorithm:
 * - First block (player-broken): only search upward (prevent going into ground)
 * - Subsequent blocks: search all 26 directions (capture entire branches)
 */
class ChainChoppingManager(private val plugin: Skills) {

    // 9 directions for upward propagation (used for first block only)
    private val upwardDirections = listOf(
        Triple(0, 1, 0),    // directly up
        Triple(1, 1, 0),    // diagonal up
        Triple(-1, 1, 0),
        Triple(0, 1, 1),
        Triple(0, 1, -1),
        Triple(1, 1, 1),
        Triple(1, 1, -1),
        Triple(-1, 1, 1),
        Triple(-1, 1, -1)
    )

    // All 26 directions (used for subsequent blocks to capture branches)
    private val allDirections: List<Triple<Int, Int, Int>> = buildList {
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        add(Triple(dx, dy, dz))
                    }
                }
            }
        }
    }

    // Blocks processed per tick to reduce server load
    private val blocksPerTick = 5

    /**
     * Calculate max chain count based on Lumberjacking skill.
     * @return max blocks to chain (unlimited = Int.MAX_VALUE for GM)
     */
    fun getMaxChainCount(player: Player): Int {
        val data = plugin.playerDataManager.getPlayerData(player)
        val skill = data.getSkillValue(SkillType.LUMBERJACKING)

        // GM (skill 100) = unlimited chain
        if (skill >= 100.0) {
            return Int.MAX_VALUE
        }

        // floor(skill / 10)
        // Skill 0-9: 0 (no chain), Skill 10-19: 1, ..., Skill 50-59: 5, Skill 90-99: 9
        return (skill / 10).toInt()
    }

    /**
     * Find connected logs of the same type.
     * Uses BFS algorithm:
     * - First block (player-broken): only search upward (prevent going into ground)
     * - Subsequent blocks: search all 26 directions (capture entire branches)
     *
     * @param startBlock The first broken block
     * @param logType The type of log to match
     * @param maxCount Maximum number of logs to find
     * @return List of blocks to break (excluding the already broken start block)
     */
    fun findConnectedLogs(startBlock: Block, logType: Material, maxCount: Int): List<Block> {
        if (maxCount <= 0) return emptyList()

        val result = mutableListOf<Block>()
        val visited = mutableSetOf<Location>()
        val queue = LinkedList<Block>()

        // Start from the origin block's neighbors (origin is already broken)
        visited.add(startBlock.location)

        // First block: only search UPWARD neighbors (prevent going into ground)
        for ((dx, dy, dz) in upwardDirections) {
            val neighbor = startBlock.getRelative(dx, dy, dz)
            if (isSameLogType(neighbor.type, logType) && !visited.contains(neighbor.location)) {
                queue.add(neighbor)
                visited.add(neighbor.location)
            }
        }

        // BFS: subsequent blocks use ALL directions to capture branches
        while (queue.isNotEmpty() && result.size < maxCount) {
            val current = queue.poll()

            // Add to result
            result.add(current)

            // If we've reached the limit, stop searching
            if (result.size >= maxCount) {
                break
            }

            // Find ALL neighbors (26 directions) for branch detection
            for ((dx, dy, dz) in allDirections) {
                val neighbor = current.getRelative(dx, dy, dz)
                if (isSameLogType(neighbor.type, logType) && !visited.contains(neighbor.location)) {
                    queue.add(neighbor)
                    visited.add(neighbor.location)
                }
            }
        }

        return result
    }

    /**
     * Check if two log types are the same wood type.
     * e.g., OAK_LOG matches OAK_LOG, but not BIRCH_LOG.
     */
    private fun isSameLogType(blockType: Material, targetType: Material): Boolean {
        // Must be a log/stem
        if (!GatheringDifficulty.isLog(blockType)) {
            return false
        }

        // Extract wood type prefix (e.g., "OAK" from "OAK_LOG")
        val blockPrefix = getWoodPrefix(blockType)
        val targetPrefix = getWoodPrefix(targetType)

        return blockPrefix == targetPrefix
    }

    /**
     * Get the wood type prefix from a log material.
     */
    private fun getWoodPrefix(material: Material): String {
        val name = material.name
        return when {
            name.endsWith("_LOG") -> name.removeSuffix("_LOG")
            name.endsWith("_STEM") -> name.removeSuffix("_STEM")
            name.endsWith("_WOOD") -> name.removeSuffix("_WOOD")
            name.endsWith("_HYPHAE") -> name.removeSuffix("_HYPHAE")
            else -> name
        }
    }

    /**
     * Execute chain chopping with distributed processing.
     * Breaks logs over multiple ticks to reduce server load.
     * @param player The player who initiated the chopping
     * @param blocks The list of blocks to break
     */
    fun executeChainChopping(player: Player, blocks: List<Block>) {
        if (blocks.isEmpty()) return

        // Create a copy of blocks to process
        val blocksToProcess = blocks.toMutableList()

        // Schedule distributed breaking
        object : BukkitRunnable() {
            override fun run() {
                // Process up to blocksPerTick blocks
                val toProcess = minOf(blocksPerTick, blocksToProcess.size)

                for (i in 0 until toProcess) {
                    val block = blocksToProcess.removeAt(0)

                    // Check if block is still the same type (wasn't broken by something else)
                    if (GatheringDifficulty.isLog(block.type)) {
                        // Get drops before breaking
                        val drops = block.getDrops(player.inventory.itemInMainHand)

                        // Break the block (set to air)
                        val blockLocation = block.location

                        block.type = Material.AIR

                        // Drop items at the block's location (they will fall naturally)
                        drops.forEach { drop ->
                            block.world.dropItemNaturally(blockLocation.add(0.5, 0.5, 0.5), drop)
                        }
                    }
                }

                // If no more blocks, cancel the task
                if (blocksToProcess.isEmpty()) {
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L) // Start after 1 tick, repeat every tick
    }

    /**
     * Process chain chopping when a log is broken.
     * @param player The player breaking the log
     * @param brokenBlock The block that was broken
     * @param logType The type of log that was broken
     * @return The number of additional blocks that will be chain-chopped
     */
    fun processChainChopping(player: Player, brokenBlock: Block, logType: Material): Int {
        val maxChain = getMaxChainCount(player)

        // Find connected logs (upward only)
        val connectedLogs = findConnectedLogs(brokenBlock, logType, maxChain)

        // Execute chain chopping if there are logs to break
        if (connectedLogs.isNotEmpty()) {
            executeChainChopping(player, connectedLogs)

            // Send message to player
            plugin.messageSender.send(player, MessageKey.GATHERING_CHAIN_CHOP, "count" to connectedLogs.size)
        }

        return connectedLogs.size
    }
}
