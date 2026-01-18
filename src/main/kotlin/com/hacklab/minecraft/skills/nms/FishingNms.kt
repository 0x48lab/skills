package com.hacklab.minecraft.skills.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.logging.Logger

/**
 * NMS operations for fishing-related functionality.
 *
 * Handles:
 * - Simulating fishing rod use (right-click action)
 * - Converting between Bukkit and NMS ItemStacks
 *
 * Uses [ReflectionCache] for all reflection operations.
 */
object FishingNms {

    private var logger: Logger? = null
    private var initialized = false
    private var available = false

    // Cached reflection objects
    private var craftPlayerClass: Class<*>? = null
    private var getHandleMethod: Method? = null
    private var serverPlayerClass: Class<*>? = null
    private var interactionHandClass: Class<*>? = null
    private var interactionHandMainHand: Any? = null
    private var interactionHandOffHand: Any? = null

    // GameMode approach
    private var gameModeField: Field? = null
    private var useItemMethod: Method? = null
    private var levelMethod: Method? = null

    // CraftItemStack for conversion
    private var craftItemStackClass: Class<*>? = null
    private var asNmsCopyMethod: Method? = null

    /**
     * Initialize fishing NMS components.
     *
     * @param logger Logger for diagnostic output
     * @return true if initialization was successful
     */
    fun initialize(logger: Logger): Boolean {
        if (initialized) return available

        this.logger = logger

        try {
            // Find CraftPlayer class
            craftPlayerClass = ReflectionCache.findCraftBukkitClass(
                "FishingNms.CraftPlayer",
                "entity.CraftPlayer"
            )
            if (craftPlayerClass == null) {
                logger.warning("[FishingNms] Could not find CraftPlayer class")
                initialized = true
                available = false
                return false
            }
            logger.fine("[FishingNms] Found CraftPlayer: ${craftPlayerClass!!.name}")

            // Get the getHandle() method
            getHandleMethod = ReflectionCache.findMethod(
                "FishingNms.getHandle",
                craftPlayerClass!!,
                "getHandle"
            )
            if (getHandleMethod == null) {
                logger.warning("[FishingNms] Could not find getHandle method")
                initialized = true
                available = false
                return false
            }

            // Get ServerPlayer class from the return type
            serverPlayerClass = getHandleMethod!!.returnType
            logger.fine("[FishingNms] Found ServerPlayer: ${serverPlayerClass!!.name}")

            // Find InteractionHand enum
            interactionHandClass = ReflectionCache.findClass(
                "FishingNms.InteractionHand",
                "net.minecraft.world.InteractionHand"
            )
            if (interactionHandClass != null) {
                interactionHandMainHand = ReflectionCache.getEnumConstant(interactionHandClass!!, 0)
                interactionHandOffHand = ReflectionCache.getEnumConstant(interactionHandClass!!, 1)
                logger.fine("[FishingNms] Found InteractionHand: ${interactionHandClass!!.name}")
            }

            // Find gameMode field in ServerPlayer
            gameModeField = ReflectionCache.findFieldByTypeName(
                "FishingNms.gameMode",
                serverPlayerClass!!,
                "GameMode"
            )
            if (gameModeField != null) {
                gameModeField!!.isAccessible = true
                logger.fine("[FishingNms] Found gameMode field: ${gameModeField!!.name}")

                // Find useItem method in the gameMode class
                useItemMethod = findUseItemMethod(gameModeField!!.type)
                if (useItemMethod != null) {
                    useItemMethod!!.isAccessible = true
                    logger.fine("[FishingNms] Found useItem method: ${useItemMethod!!.name}")
                }
            }

            // Find level() method in ServerPlayer
            levelMethod = findLevelMethod()
            if (levelMethod != null) {
                logger.fine("[FishingNms] Found level method: ${levelMethod!!.name}")
            }

            // Find CraftItemStack for conversion
            craftItemStackClass = ReflectionCache.findCraftBukkitClass(
                "FishingNms.CraftItemStack",
                "inventory.CraftItemStack"
            )
            if (craftItemStackClass != null) {
                asNmsCopyMethod = ReflectionCache.findMethod(
                    "FishingNms.asNMSCopy",
                    craftItemStackClass!!,
                    "asNMSCopy",
                    ItemStack::class.java
                )
            }

            // Check if we have everything needed
            val canUseGameMode = gameModeField != null &&
                    useItemMethod != null &&
                    levelMethod != null &&
                    interactionHandClass != null &&
                    craftItemStackClass != null &&
                    asNmsCopyMethod != null

            initialized = true
            available = canUseGameMode

            logger.info("[FishingNms] Initialization complete. Available: $available")
            return available

        } catch (e: Exception) {
            logger.warning("[FishingNms] Failed to initialize: ${e.message}")
            e.printStackTrace()
            initialized = true
            available = false
            return false
        }
    }

    /**
     * Find the useItem method in the gameMode class.
     * Looks for method with 4 parameters including InteractionHand.
     */
    private fun findUseItemMethod(gameModeClass: Class<*>): Method? {
        // Try known method names first
        val methodNames = listOf("useItem", "a")
        for (name in methodNames) {
            for (method in gameModeClass.declaredMethods) {
                if (method.name == name && method.parameterCount == 4) {
                    return method
                }
            }
        }

        // Fallback: find any method with InteractionHand parameter
        if (interactionHandClass != null) {
            for (method in gameModeClass.declaredMethods) {
                if (method.parameterCount == 4 &&
                    method.parameterTypes.any { it == interactionHandClass }) {
                    return method
                }
            }
        }

        return null
    }

    /**
     * Find the level() or getLevel() method in ServerPlayer.
     */
    private fun findLevelMethod(): Method? {
        if (serverPlayerClass == null) return null

        // Try known method names
        val methodNames = listOf("level", "getLevel", "serverLevel", "getServerLevel")
        for (name in methodNames) {
            val method = ReflectionCache.findMethodByNames(
                "FishingNms.level",
                serverPlayerClass!!,
                listOf(name)
            )
            if (method != null) {
                val returnTypeName = method.returnType.simpleName
                if (returnTypeName.contains("Level") || returnTypeName.contains("World")) {
                    return method
                }
            }
        }

        // Fallback: find any no-arg method returning a Level type
        for (method in serverPlayerClass!!.methods) {
            if (method.parameterCount == 0) {
                val returnTypeName = method.returnType.simpleName
                if (returnTypeName.contains("Level") || returnTypeName.contains("ServerLevel")) {
                    return method
                }
            }
        }

        return null
    }

    /**
     * Simulate using a fishing rod (right-click action).
     *
     * This calls the same internal method as a player right-click,
     * ensuring identical behavior for loot tables, enchantments, etc.
     *
     * @param player The player
     * @param slot The hand containing the fishing rod
     * @return true if successful
     */
    fun simulateRodUse(player: Player, slot: EquipmentSlot): Boolean {
        if (!available) {
            logger?.warning("[FishingNms] Not available, cannot simulate rod use")
            return false
        }

        try {
            // Get the NMS ServerPlayer
            val craftPlayer = craftPlayerClass!!.cast(player)
            val serverPlayer = getHandleMethod!!.invoke(craftPlayer)

            // Try the gameMode.useItem approach
            if (tryGameModeUseItem(serverPlayer, player, slot)) {
                return true
            }

            logger?.fine("[FishingNms] GameMode approach failed, trying direct hook manipulation")

            // Fallback: direct hook manipulation
            return tryDirectHookManipulation(player)

        } catch (e: Exception) {
            logger?.warning("[FishingNms] Failed to simulate rod use: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Try to use gameMode.useItem approach.
     * This is the preferred method as it triggers the exact same code path as manual right-click.
     */
    private fun tryGameModeUseItem(serverPlayer: Any, player: Player, slot: EquipmentSlot): Boolean {
        if (gameModeField == null || useItemMethod == null || levelMethod == null) {
            return false
        }

        try {
            val gameMode = gameModeField!!.get(serverPlayer)
            val level = levelMethod!!.invoke(serverPlayer)
            val hand = if (slot == EquipmentSlot.HAND) interactionHandMainHand else interactionHandOffHand

            if (hand == null) {
                logger?.warning("[FishingNms] InteractionHand is null")
                return false
            }

            // Get the ItemStack
            val bukkitItem = when (slot) {
                EquipmentSlot.HAND -> player.inventory.itemInMainHand
                EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
                else -> return false
            }

            // Convert to NMS ItemStack
            val nmsItemStack = getNmsItemStack(bukkitItem)
            if (nmsItemStack == null) {
                logger?.warning("[FishingNms] Could not convert ItemStack to NMS")
                return false
            }

            // Call useItem(serverPlayer, level, itemStack, hand)
            val result = useItemMethod!!.invoke(gameMode, serverPlayer, level, nmsItemStack, hand)
            logger?.fine("[FishingNms] useItem called, result: $result")
            return true

        } catch (e: Exception) {
            logger?.warning("[FishingNms] GameMode useItem failed: ${e.message}")
            return false
        }
    }

    /**
     * Convert Bukkit ItemStack to NMS ItemStack.
     */
    private fun getNmsItemStack(bukkitItem: ItemStack): Any? {
        if (craftItemStackClass == null || asNmsCopyMethod == null) {
            return null
        }

        return try {
            asNmsCopyMethod!!.invoke(null, bukkitItem)
        } catch (e: Exception) {
            logger?.warning("[FishingNms] Failed to convert ItemStack: ${e.message}")
            null
        }
    }

    /**
     * Fallback: Try direct hook manipulation through Bukkit API.
     * This is less ideal as it doesn't trigger the full vanilla code path.
     */
    private fun tryDirectHookManipulation(player: Player): Boolean {
        return try {
            val existingHook = player.world.entities
                .filterIsInstance<org.bukkit.entity.FishHook>()
                .find { it.shooter == player }

            if (existingHook != null && existingHook.isValid) {
                existingHook.pullHookedEntity()
                existingHook.remove()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger?.warning("[FishingNms] Direct hook manipulation failed: ${e.message}")
            false
        }
    }

    /**
     * Check if fishing NMS is available.
     */
    fun isAvailable(): Boolean = available

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        // Nothing to cleanup currently, but provided for consistency
    }

    /**
     * Get diagnostic information.
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("FishingNms Status:")
            appendLine("  Initialized: $initialized")
            appendLine("  Available: $available")
            appendLine("  CraftPlayer: ${craftPlayerClass?.name ?: "not found"}")
            appendLine("  ServerPlayer: ${serverPlayerClass?.name ?: "not found"}")
            appendLine("  InteractionHand: ${interactionHandClass?.name ?: "not found"}")
            appendLine("  GameMode Field: ${gameModeField?.name ?: "not found"}")
            appendLine("  UseItem Method: ${useItemMethod?.name ?: "not found"}")
            appendLine("  Level Method: ${levelMethod?.name ?: "not found"}")
            appendLine("  CraftItemStack: ${craftItemStackClass?.name ?: "not found"}")
            appendLine("  AsNMSCopy Method: ${asNmsCopyMethod?.name ?: "not found"}")
        }
    }
}
