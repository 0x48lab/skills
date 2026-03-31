package com.hacklab.minecraft.skills.integration

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Integration with the Notoriety plugin for alignment/fame tracking.
 * Uses Java reflection to avoid hard dependency.
 */
class NotorietyIntegration(private val plugin: Skills) {

    // Stores original alignment before crime-gray was applied: playerId -> original alignment
    private val crimeGrayOriginal: MutableMap<UUID, Int> = ConcurrentHashMap()

    private var notorietyPlugin: Plugin? = null
    private var apiInstance: Any? = null
    private var getAlignmentMethod: java.lang.reflect.Method? = null
    private var setAlignmentMethod: java.lang.reflect.Method? = null
    private var addAlignmentMethod: java.lang.reflect.Method? = null
    private var recordCrimeMethod: java.lang.reflect.Method? = null
    private var crimeTypeClass: Class<*>? = null

    /**
     * Initialize the integration by finding the Notoriety plugin
     */
    fun initialize(): Boolean {
        notorietyPlugin = plugin.server.pluginManager.getPlugin("Notoriety")
        if (notorietyPlugin == null || !notorietyPlugin!!.isEnabled) {
            plugin.logger.info("Notoriety plugin not found or not enabled. Integration disabled.")
            return false
        }

        try {
            // Get the 'api' property from Notoriety plugin using Java reflection
            val apiGetter = notorietyPlugin!!.javaClass.getMethod("getApi")
            apiInstance = apiGetter.invoke(notorietyPlugin)

            // Get alignment methods
            getAlignmentMethod = apiInstance?.javaClass?.getMethod(
                "getAlignment",
                UUID::class.java
            )
            setAlignmentMethod = apiInstance?.javaClass?.getMethod(
                "setAlignment",
                UUID::class.java,
                Int::class.javaPrimitiveType
            )
            addAlignmentMethod = apiInstance?.javaClass?.getMethod(
                "addAlignment",
                UUID::class.java,
                Int::class.javaPrimitiveType
            )

            // Get the recordCrime method
            // recordCrime(criminal: UUID, crimeType: CrimeType, victim: UUID?, location: Location?, detail: String?)
            try {
                crimeTypeClass = Class.forName("com.hacklab.minecraft.notoriety.crime.CrimeType")
                recordCrimeMethod = apiInstance?.javaClass?.getMethod(
                    "recordCrime",
                    UUID::class.java,
                    crimeTypeClass,
                    UUID::class.java,
                    Location::class.java,
                    String::class.java
                )
            } catch (e: Exception) {
                plugin.logger.warning("recordCrime method not found (older Notoriety version?): ${e.message}")
            }

            plugin.logger.info("Notoriety integration enabled.")
            return true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize Notoriety integration: ${e.message}")
        }

        return false
    }

    /**
     * Check if integration is available
     */
    fun isAvailable(): Boolean = apiInstance != null && addAlignmentMethod != null

    /**
     * Get a player's current alignment value
     */
    fun getAlignment(playerId: UUID): Int? {
        if (!isAvailable() || getAlignmentMethod == null) return null

        return try {
            getAlignmentMethod?.invoke(apiInstance, playerId) as? Int
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get alignment: ${e.message}")
            null
        }
    }

    /**
     * Set a player's alignment to an exact value
     */
    fun setAlignment(playerId: UUID, value: Int) {
        if (!isAvailable() || setAlignmentMethod == null) return

        try {
            setAlignmentMethod?.invoke(apiInstance, playerId, value)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to set alignment: ${e.message}")
        }
    }

    /**
     * Add alignment to a player (negative for penalty)
     */
    fun addAlignment(playerId: UUID, amount: Int) {
        if (!isAvailable()) return

        try {
            addAlignmentMethod?.invoke(apiInstance, playerId, amount)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to add alignment: ${e.message}")
        }
    }

    /**
     * Ensure a player becomes gray (alignment < 0) after a direct crime against another player.
     * If alignment is still >= 0 after normal penalty, force it to [minAlignment].
     */
    fun ensureGray(playerId: UUID, minAlignment: Int = GRAY_ALIGNMENT_ON_CRIME) {
        val current = getAlignment(playerId) ?: return
        if (current >= 0) {
            setAlignment(playerId, minAlignment)
        }
    }

    /**
     * Record a crime in the Notoriety crime history.
     * @param crimeTypeName CrimeType enum name (e.g., "THEFT", "ATTACK")
     */
    fun recordCrime(criminal: UUID, crimeTypeName: String, victim: UUID?, location: Location?, detail: String?) {
        if (recordCrimeMethod == null || crimeTypeClass == null) return

        try {
            val crimeType = java.lang.Enum.valueOf(crimeTypeClass as Class<out Enum<*>>, crimeTypeName)
            recordCrimeMethod?.invoke(apiInstance, criminal, crimeType, victim, location, detail)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to record crime: ${e.message}")
        }
    }

    companion object {
        // Note: recordCrime already applies CrimeType.THEFT.defaultPenalty (-5) internally.
        // Only use addAlignment for EXTRA penalties beyond the default.

        // Extra penalty for successful theft (on top of recordCrime's -5, total = -10)
        const val STEAL_SUCCESS_EXTRA_PENALTY = -5

        // Alignment value forced on direct player crimes (stealing, snooping)
        // Ensures the criminal immediately becomes gray regardless of prior alignment
        const val GRAY_ALIGNMENT_ON_CRIME = -10

        // Crime type names (must match Notoriety's CrimeType enum)
        const val CRIME_THEFT = "THEFT"
    }
}
