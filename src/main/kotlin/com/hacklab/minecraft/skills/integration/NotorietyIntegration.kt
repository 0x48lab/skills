package com.hacklab.minecraft.skills.integration

import com.hacklab.minecraft.skills.Skills
import org.bukkit.plugin.Plugin
import java.util.*

/**
 * Integration with the Notoriety plugin for alignment/fame tracking.
 * Uses Java reflection to avoid hard dependency.
 */
class NotorietyIntegration(private val plugin: Skills) {

    private var notorietyPlugin: Plugin? = null
    private var apiInstance: Any? = null
    private var addAlignmentMethod: java.lang.reflect.Method? = null

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

            // Get the addAlignment method
            addAlignmentMethod = apiInstance?.javaClass?.getMethod(
                "addAlignment",
                UUID::class.java,
                Int::class.javaPrimitiveType
            )

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

    companion object {
        // Standard penalties
        const val SNOOP_PENALTY = -5
        const val STEAL_PENALTY = -10
        const val STEAL_FAILED_PENALTY = -5
    }
}
