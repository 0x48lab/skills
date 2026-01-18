package com.hacklab.minecraft.skills.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.util.logging.Logger

/**
 * Central manager for all NMS (Net Minecraft Server) operations.
 *
 * This class provides a unified interface for NMS-related functionality,
 * abstracting away version-specific reflection details.
 *
 * ## Architecture
 * - All NMS code is centralized in the `nms` package
 * - Version detection is handled by [NmsVersion]
 * - Reflection caching is handled by [ReflectionCache]
 * - Feature-specific NMS code is organized in sub-modules (e.g., [FishingNms])
 *
 * ## Adding New NMS Features
 * 1. Create a new object in the `nms` package (e.g., `CombatNms`)
 * 2. Use [ReflectionCache] for all reflection operations
 * 3. Add initialization call in [initialize]
 * 4. Add accessor methods in this manager
 *
 * ## Version Compatibility
 * - Designed for Paper 1.21.x
 * - Uses unversioned CraftBukkit packages (Paper 1.20.5+)
 * - Fallback to versioned packages for older servers
 */
object NmsManager {

    private var logger: Logger? = null
    private var initialized = false
    private var version: NmsVersion? = null

    // Feature modules
    private var fishingNmsAvailable = false

    /**
     * Initialize the NMS manager.
     * Call this once on plugin enable.
     *
     * @param logger Logger for diagnostic output
     * @return true if initialization was successful
     */
    fun initialize(logger: Logger): Boolean {
        if (initialized) return true

        this.logger = logger

        // Detect version
        version = NmsVersion.detect()
        logger.info("[NMS] Detected server version: $version")

        // Initialize reflection cache
        ReflectionCache.initialize(logger)

        // Initialize feature modules
        fishingNmsAvailable = FishingNms.initialize(logger)

        initialized = true

        // Log summary
        logger.info("[NMS] Initialization complete:")
        logger.info("[NMS]   Version: $version")
        logger.info("[NMS]   Fishing NMS: ${if (fishingNmsAvailable) "available" else "unavailable"}")

        return true
    }

    /**
     * Get the detected NMS version.
     */
    fun getVersion(): NmsVersion {
        return version ?: NmsVersion.detect()
    }

    /**
     * Check if the NMS manager is initialized.
     */
    fun isInitialized(): Boolean = initialized

    // ============================================================
    // Fishing NMS
    // ============================================================

    /**
     * Check if fishing NMS features are available.
     */
    fun isFishingNmsAvailable(): Boolean = fishingNmsAvailable

    /**
     * Simulate using a fishing rod (right-click action).
     * This triggers the same action as the player right-clicking.
     *
     * @param player The player
     * @param slot The hand containing the fishing rod
     * @return true if successful
     */
    fun simulateFishingRodUse(player: Player, slot: EquipmentSlot): Boolean {
        if (!fishingNmsAvailable) {
            logger?.warning("[NMS] Fishing NMS not available, cannot simulate rod use")
            return false
        }
        return FishingNms.simulateRodUse(player, slot)
    }

    // ============================================================
    // Future NMS Features (add here as needed)
    // ============================================================

    // Example:
    // fun isCombatNmsAvailable(): Boolean = combatNmsAvailable
    // fun getEntityDamage(entity: Entity): Double { ... }

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Cleanup and reset (for plugin reload).
     */
    fun cleanup() {
        ReflectionCache.clearCaches()
        FishingNms.cleanup()
        initialized = false
        version = null
        fishingNmsAvailable = false
    }

    /**
     * Get diagnostic information about NMS state.
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("=== NMS Manager Diagnostics ===")
            appendLine("Initialized: $initialized")
            appendLine("Version: $version")
            appendLine()
            appendLine("Feature Availability:")
            appendLine("  Fishing: $fishingNmsAvailable")
            appendLine()
            append(ReflectionCache.getStats())
            appendLine()
            append(FishingNms.getDiagnostics())
        }
    }
}
