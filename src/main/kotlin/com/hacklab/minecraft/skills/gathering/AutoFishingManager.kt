package com.hacklab.minecraft.skills.gathering

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.nms.NmsManager
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.Material
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Manages auto-fishing system based on Fishing skill.
 *
 * When a fish bites, there's a skill-based probability to:
 * 1. Automatically reel in the fish
 * 2. Automatically recast the fishing rod
 *
 * Skill 0-19: No auto-fishing (manual only)
 * Skill 20-99: Skill% chance to auto-fish
 * Skill 100 (GM): 100% auto-fishing (AFK possible)
 *
 * Uses runtime reflection to simulate right-click actions without
 * compile-time NMS dependencies.
 */
class AutoFishingManager(private val plugin: Skills) {

    // Track players who are in auto-fishing mode
    private val autoFishingPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    // Track active fish hooks for recast
    private val activeHooks: MutableMap<UUID, FishHook> = ConcurrentHashMap()

    // Track which hand has the fishing rod for each player
    private val rodHand: MutableMap<UUID, EquipmentSlot> = ConcurrentHashMap()

    // Minimum skill level for auto-fishing
    private val MIN_AUTO_FISHING_SKILL = 20.0

    // Whether reflection is available
    private var reflectionAvailable = false

    /**
     * Initialize the auto-fishing system.
     * NmsManager must be initialized before calling this.
     * Call this on plugin enable.
     */
    fun initialize() {
        reflectionAvailable = NmsManager.isFishingNmsAvailable()
        if (reflectionAvailable) {
            plugin.logger.info("Auto-fishing initialized successfully (using NmsManager)")
        } else {
            plugin.logger.warning("Auto-fishing NMS not available - falling back to manual fishing only")
        }
    }

    /**
     * Get the auto-fishing probability for a player.
     * @return probability as a value between 0.0 and 1.0
     */
    fun getAutoFishingProbability(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)

        // No auto-fishing below skill 20
        if (fishingSkill < MIN_AUTO_FISHING_SKILL) {
            return 0.0
        }

        // GM (skill 100) always succeeds
        if (fishingSkill >= 100.0) {
            return 1.0
        }

        // Skill value as percentage
        return fishingSkill / 100.0
    }

    /**
     * Check if auto-fishing should trigger.
     * @return true if auto-fishing should proceed
     */
    fun shouldAutoFish(player: Player): Boolean {
        val probability = getAutoFishingProbability(player)
        if (probability <= 0.0) {
            return false
        }
        if (probability >= 1.0) {
            return true
        }
        return Random.nextDouble() < probability
    }

    /**
     * Check if player has auto-fishing capability (skill >= 20).
     */
    fun hasAutoFishingCapability(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)
        return fishingSkill >= MIN_AUTO_FISHING_SKILL
    }

    /**
     * Check if player is a Grandmaster (skill 100).
     * GM players always succeed at auto-fishing.
     */
    fun isGrandmaster(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val fishingSkill = data.getSkillValue(SkillType.FISHING)
        return fishingSkill >= 100.0
    }

    /**
     * Process a fish bite event for auto-fishing.
     * Called when PlayerFishEvent.State.BITE occurs.
     *
     * Behavior:
     * - Skill 0-19: No auto-fishing (returns false, manual only)
     * - Skill 20-99: Roll probability. Success = auto-reel + recast. Failure = message + do nothing.
     * - Skill 100 (GM): Always auto-reel + recast (true AFK fishing)
     *
     * @param player The player fishing
     * @param hook The fish hook entity
     * @return true if auto-fishing was triggered, false if player should reel manually
     */
    fun processFishBite(player: Player, hook: FishHook): Boolean {
        // Check if NMS is available
        if (!reflectionAvailable) {
            return false
        }

        // Check if player has auto-fishing capability (skill >= 20)
        if (!hasAutoFishingCapability(player)) {
            return false
        }

        // GM (skill 100) always succeeds
        val isGM = isGrandmaster(player)

        // For non-GM, roll probability
        if (!isGM) {
            val probability = getAutoFishingProbability(player)
            val roll = Random.nextDouble()

            if (roll >= probability) {
                // Failed - send message, let fish escape, player must manually reel
                plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_FISHING_FAILED)
                return false
            }
        }

        // Success (or GM) - proceed with auto-fishing
        // Determine which hand has the fishing rod
        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand

        val slot = when {
            mainHand.type == Material.FISHING_ROD -> EquipmentSlot.HAND
            offHand.type == Material.FISHING_ROD -> EquipmentSlot.OFF_HAND
            else -> return false
        }

        // Store the hook and hand info
        activeHooks[player.uniqueId] = hook
        rodHand[player.uniqueId] = slot
        autoFishingPlayers.add(player.uniqueId)

        // Schedule the auto-reel (simulate right-click to reel in)
        // We need a small delay to let the fish attach properly
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            performAutoReel(player)
        }, 2L) // 2 ticks = 0.1 seconds delay

        return true
    }

    /**
     * Perform the auto-reel action.
     * This simulates the player right-clicking to reel in the fish.
     */
    private fun performAutoReel(player: Player) {
        // Check if player is still online
        if (!player.isOnline) {
            cleanup(player.uniqueId)
            return
        }

        val slot = rodHand[player.uniqueId] ?: EquipmentSlot.HAND
        val rodItem = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> null
        }

        // Check if player still has fishing rod in hand
        if (rodItem == null || rodItem.type != Material.FISHING_ROD) {
            cleanup(player.uniqueId)
            return
        }

        // Try to use NMS to simulate rod use (reel in)
        val reflectionSuccess = NmsManager.simulateFishingRodUse(player, slot)

        if (!reflectionSuccess) {
            // Fallback: Try to retract the hook directly
            val hook = activeHooks[player.uniqueId]
            if (hook != null && hook.isValid) {
                // Pull any hooked entity and remove the hook
                hook.pullHookedEntity()
                hook.remove()
            }
        }

        // Since hook.remove() doesn't trigger CAUGHT_FISH event,
        // we need to manually schedule the recast here
        // Wait a bit for the catch to complete, then recast
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            // Only recast if player is still in auto-fishing mode
            if (autoFishingPlayers.contains(player.uniqueId)) {
                performAutoRecast(player)
            }
        }, 15L) // 15 ticks = 0.75 seconds delay
    }

    /**
     * Process after a successful fish catch for auto-recast.
     * Called when PlayerFishEvent.State.CAUGHT_FISH occurs.
     *
     * Note: Recast is now handled in performAutoReel to ensure it works
     * even when CAUGHT_FISH event doesn't fire (e.g., when using hook.remove()).
     * This method now just logs the catch for debugging purposes.
     *
     * @param player The player who caught fish
     */
    fun processAfterCatch(player: Player) {
        // Only process if this was an auto-fishing session
        if (!autoFishingPlayers.contains(player.uniqueId)) {
            return
        }

        // CAUGHT_FISH event fired - this means either:
        // 1. Reflection worked and player "used" the rod
        // 2. Player manually reeled in
        // Recast is already scheduled in performAutoReel, so we don't need to do anything here
        if (plugin.skillsConfig.debugMode) {
            plugin.logger.info("[AutoFishing] CAUGHT_FISH event for ${player.name} - recast already scheduled")
        }
    }

    /**
     * Perform auto-recast of the fishing rod.
     */
    private fun performAutoRecast(player: Player) {
        // Check if player is still online
        if (!player.isOnline) {
            cleanup(player.uniqueId)
            return
        }

        val slot = rodHand[player.uniqueId] ?: EquipmentSlot.HAND
        val fishingRod = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> null
        }

        // Check if player still has fishing rod
        if (fishingRod == null || fishingRod.type != Material.FISHING_ROD) {
            cleanup(player.uniqueId)
            return
        }

        // Check if fishing rod has durability left
        val meta = fishingRod.itemMeta
        if (meta != null && meta is org.bukkit.inventory.meta.Damageable) {
            val maxDurability = fishingRod.type.maxDurability.toInt()
            val currentDamage = meta.damage
            if (currentDamage >= maxDurability - 1) {
                // Rod is about to break, stop auto-fishing
                cleanup(player.uniqueId)
                plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_FISHING_STOPPED)
                return
            }
        }

        // Try to use NMS to simulate rod use (cast)
        if (NmsManager.simulateFishingRodUse(player, slot)) {
            // Success - the rod was used and a new hook should be cast
            plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_FISHING_RECAST)
        } else {
            // Fallback: Try to launch projectile directly (may not work perfectly)
            try {
                val hook = player.launchProjectile(FishHook::class.java)
                activeHooks[player.uniqueId] = hook
                plugin.messageSender.send(player, MessageKey.GATHERING_AUTO_FISHING_RECAST)
            } catch (e: Exception) {
                // Failed to cast, stop auto-fishing
                cleanup(player.uniqueId)
                plugin.logger.warning("[AutoFishing] Failed to recast for ${player.name}: ${e.message}")
            }
        }
    }

    /**
     * Cancel auto-fishing for a player.
     * Called when player manually interacts or moves significantly.
     */
    fun cancelAutoFishing(player: Player) {
        if (autoFishingPlayers.remove(player.uniqueId)) {
            activeHooks.remove(player.uniqueId)
            rodHand.remove(player.uniqueId)
        }
    }

    /**
     * Check if a player is currently in auto-fishing mode.
     */
    fun isAutoFishing(player: Player): Boolean {
        return autoFishingPlayers.contains(player.uniqueId)
    }

    /**
     * Cleanup when player disconnects or stops fishing.
     */
    fun cleanup(uuid: UUID) {
        autoFishingPlayers.remove(uuid)
        activeHooks.remove(uuid)
        rodHand.remove(uuid)
    }

    /**
     * Cleanup all data (called on plugin disable).
     */
    fun cleanupAll() {
        autoFishingPlayers.clear()
        activeHooks.clear()
        rodHand.clear()
    }

    /**
     * Check if reflection is available for auto-fishing.
     */
    fun isReflectionAvailable(): Boolean = reflectionAvailable

    /**
     * Get diagnostic information about auto-fishing state.
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("AutoFishingManager Status:")
            appendLine("  NMS Available: $reflectionAvailable")
            appendLine("  Active Auto-Fishers: ${autoFishingPlayers.size}")
            appendLine()
            append(NmsManager.getDiagnostics())
        }
    }
}
