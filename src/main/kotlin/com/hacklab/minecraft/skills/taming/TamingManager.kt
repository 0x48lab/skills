package com.hacklab.minecraft.skills.taming

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class TamingManager(private val plugin: Skills) {
    // Taming difficulties for different entity types
    private val tamingDifficulties = mapOf(
        EntityType.WOLF to 30,
        EntityType.CAT to 25,
        EntityType.PARROT to 35,
        EntityType.HORSE to 40,
        EntityType.DONKEY to 35,
        EntityType.MULE to 45,
        EntityType.LLAMA to 40,
        EntityType.FOX to 50,
        EntityType.AXOLOTL to 45,
        EntityType.ALLAY to 60,
        EntityType.CAMEL to 50,
        EntityType.SNIFFER to 55
    )

    // Cooldown tracking
    private val tamingCooldowns: MutableMap<UUID, Long> = ConcurrentHashMap()

    /**
     * Attempt to tame an entity
     */
    fun tryTame(player: Player, entity: LivingEntity): TameResult {
        // Check if entity is tameable
        if (!isTameable(entity)) {
            plugin.messageSender.send(player, MessageKey.TAMING_CANNOT_TAME)
            return TameResult.NOT_TAMEABLE
        }

        // Check if already tamed
        if (isAlreadyTamed(entity)) {
            plugin.messageSender.send(player, MessageKey.TAMING_ALREADY_TAMED)
            return TameResult.ALREADY_TAMED
        }

        // Check cooldown
        val lastAttempt = tamingCooldowns[player.uniqueId] ?: 0
        val cooldown = plugin.skillsConfig.tameAttemptCooldown
        if (System.currentTimeMillis() - lastAttempt < cooldown) {
            return TameResult.ON_COOLDOWN
        }
        tamingCooldowns[player.uniqueId] = System.currentTimeMillis()

        val data = plugin.playerDataManager.getPlayerData(player)
        val tamingSkill = data.getSkillValue(SkillType.ANIMAL_TAMING)
        val difficulty = getTamingDifficulty(entity.type)

        // Check if skill is high enough
        if (tamingSkill < difficulty - 30) {
            plugin.messageSender.send(player, MessageKey.TAMING_TOO_DIFFICULT)
            return TameResult.TOO_DIFFICULT
        }

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.ANIMAL_TAMING, difficulty)

        // Calculate success chance
        val successChance = (tamingSkill - difficulty + 50).coerceIn(10.0, 90.0)

        plugin.messageSender.send(player, MessageKey.TAMING_START)

        if (Random.nextDouble() * 100 > successChance) {
            plugin.messageSender.send(player, MessageKey.TAMING_FAILED)
            return TameResult.FAILED
        }

        // Success! Tame the entity
        when (entity) {
            is Tameable -> {
                entity.isTamed = true
                entity.owner = player
            }
            is Horse -> {
                entity.isTamed = true
                entity.owner = player
            }
            is Llama -> {
                entity.isTamed = true
                entity.owner = player
            }
            // For entities that don't have native taming, we can use custom system
            else -> {
                // Store taming data in entity's persistent data container
                setCustomOwner(entity, player.uniqueId)
            }
        }

        plugin.messageSender.send(player, MessageKey.TAMING_SUCCESS,
            "entity" to entity.type.name.lowercase().replace("_", " "))

        return TameResult.SUCCESS
    }

    /**
     * Check if entity type is tameable
     */
    fun isTameable(entity: LivingEntity): Boolean {
        return tamingDifficulties.containsKey(entity.type) ||
                entity is Tameable
    }

    /**
     * Get taming difficulty for entity type
     */
    fun getTamingDifficulty(entityType: EntityType): Int {
        return tamingDifficulties[entityType] ?: 50
    }

    /**
     * Check if entity is already tamed
     */
    fun isAlreadyTamed(entity: LivingEntity): Boolean {
        return when (entity) {
            is Tameable -> entity.isTamed
            is Horse -> entity.isTamed
            is Llama -> entity.isTamed
            else -> hasCustomOwner(entity)
        }
    }

    /**
     * Get owner of tamed entity
     */
    fun getOwner(entity: LivingEntity): UUID? {
        return when (entity) {
            is Tameable -> entity.owner?.uniqueId
            is Horse -> entity.owner?.uniqueId
            is Llama -> entity.owner?.uniqueId
            else -> getCustomOwner(entity)
        }
    }

    /**
     * Check if player owns the entity
     */
    fun isOwner(player: Player, entity: LivingEntity): Boolean {
        return getOwner(entity) == player.uniqueId
    }

    // Custom owner storage for non-native tameable entities
    private val customOwnerKey = org.bukkit.NamespacedKey(plugin, "tamed_owner")

    private fun setCustomOwner(entity: LivingEntity, owner: UUID) {
        entity.persistentDataContainer.set(
            customOwnerKey,
            org.bukkit.persistence.PersistentDataType.STRING,
            owner.toString()
        )
    }

    private fun getCustomOwner(entity: LivingEntity): UUID? {
        val ownerString = entity.persistentDataContainer.get(
            customOwnerKey,
            org.bukkit.persistence.PersistentDataType.STRING
        ) ?: return null
        return try {
            UUID.fromString(ownerString)
        } catch (e: Exception) {
            null
        }
    }

    private fun hasCustomOwner(entity: LivingEntity): Boolean {
        return entity.persistentDataContainer.has(
            customOwnerKey,
            org.bukkit.persistence.PersistentDataType.STRING
        )
    }

    /**
     * Remove player cooldown (for cleanup)
     */
    fun removeCooldown(playerId: UUID) {
        tamingCooldowns.remove(playerId)
    }
}

enum class TameResult {
    SUCCESS,
    FAILED,
    NOT_TAMEABLE,
    ALREADY_TAMED,
    TOO_DIFFICULT,
    ON_COOLDOWN
}
