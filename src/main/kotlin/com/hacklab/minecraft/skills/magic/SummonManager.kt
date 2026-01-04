package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages summoned creatures for the SUMMON_CREATURE spell
 * Summoned creatures are tracked by player UUID and auto-despawn after duration
 */
class SummonManager(private val plugin: Skills) {

    // PDC keys for marking summoned creatures
    private val summonedKey = NamespacedKey(plugin, "summoned")
    private val summonerKey = NamespacedKey(plugin, "summoner")
    private val summonTimeKey = NamespacedKey(plugin, "summon_time")

    // Track active summons per player
    private val activeSummons: MutableMap<UUID, MutableSet<UUID>> = ConcurrentHashMap()

    // Summon tiers based on Magery skill
    data class SummonTier(
        val entityType: EntityType,
        val minSkill: Int,
        val displayName: String
    )

    private val summonTiers = listOf(
        SummonTier(EntityType.WOLF, 0, "Wolf"),
        SummonTier(EntityType.IRON_GOLEM, 40, "Iron Golem"),
        SummonTier(EntityType.VEX, 60, "Vex"),
        SummonTier(EntityType.BLAZE, 80, "Blaze")
    )

    /**
     * Get max number of summons allowed for a player
     * Based on Magery skill: skill / 20 (max 5 at skill 100)
     */
    fun getMaxSummons(player: Player): Int {
        val data = plugin.playerDataManager.getPlayerData(player)
        val magerySkill = data.getSkillValue(SkillType.MAGERY)
        return (magerySkill / 20).toInt().coerceIn(1, 5)
    }

    /**
     * Get current summon count for a player
     */
    fun getSummonCount(player: Player): Int {
        return activeSummons[player.uniqueId]?.size ?: 0
    }

    /**
     * Check if player can summon more creatures
     */
    fun canSummonMore(player: Player): Boolean {
        return getSummonCount(player) < getMaxSummons(player)
    }

    /**
     * Get summon duration in ticks based on Magery skill
     * Base 60 seconds + 1 second per skill point
     */
    fun getSummonDuration(player: Player): Int {
        val data = plugin.playerDataManager.getPlayerData(player)
        val magerySkill = data.getSkillValue(SkillType.MAGERY)
        return ((60 + magerySkill) * 20).toInt() // Convert to ticks
    }

    /**
     * Get the entity type for summon based on player's Magery skill
     */
    fun getSummonType(player: Player): SummonTier {
        val data = plugin.playerDataManager.getPlayerData(player)
        val magerySkill = data.getSkillValue(SkillType.MAGERY)

        // Find highest tier the player qualifies for
        return summonTiers.filter { it.minSkill <= magerySkill }
            .maxByOrNull { it.minSkill } ?: summonTiers.first()
    }

    /**
     * Summon a creature for a player
     * Returns the summoned entity or null if failed
     */
    fun summonCreature(player: Player): LivingEntity? {
        if (!canSummonMore(player)) {
            plugin.messageSender.send(
                player, MessageKey.MAGIC_SUMMON_LIMIT,
                "max" to getMaxSummons(player)
            )
            return null
        }

        val tier = getSummonType(player)
        val location = player.location.add(player.location.direction.multiply(2))

        // Spawn the entity
        val entity = player.world.spawnEntity(location, tier.entityType) as? LivingEntity ?: return null

        // Mark as summoned using PDC
        entity.persistentDataContainer.set(summonedKey, PersistentDataType.BYTE, 1)
        entity.persistentDataContainer.set(summonerKey, PersistentDataType.STRING, player.uniqueId.toString())
        entity.persistentDataContainer.set(summonTimeKey, PersistentDataType.LONG, System.currentTimeMillis())

        // Set custom name
        entity.customName = "[${player.name}'s ${tier.displayName}]"
        entity.isCustomNameVisible = true

        // Make it follow the player if tameable
        if (entity is Tameable) {
            entity.isTamed = true
            entity.owner = player
        }

        // Track the summon
        val summonSet = activeSummons.getOrPut(player.uniqueId) { ConcurrentHashMap.newKeySet() }
        summonSet.add(entity.uniqueId)

        // Schedule despawn
        val duration = getSummonDuration(player)
        object : BukkitRunnable() {
            override fun run() {
                if (entity.isValid) {
                    dispelSummon(entity)
                    plugin.messageSender.send(player, MessageKey.MAGIC_SUMMON_DESPAWNED)
                }
            }
        }.runTaskLater(plugin, duration.toLong())

        plugin.messageSender.send(
            player, MessageKey.MAGIC_SUMMON_CREATURE_CAST,
            "creature" to tier.displayName
        )

        return entity
    }

    /**
     * Check if an entity is a summoned creature
     */
    fun isSummoned(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(summonedKey, PersistentDataType.BYTE)
    }

    /**
     * Get the summoner (player UUID) of a summoned entity
     */
    fun getSummoner(entity: Entity): UUID? {
        val uuidStr = entity.persistentDataContainer.get(summonerKey, PersistentDataType.STRING)
        return uuidStr?.let { UUID.fromString(it) }
    }

    /**
     * Dispel a single summoned creature
     */
    fun dispelSummon(entity: Entity) {
        if (!isSummoned(entity)) return

        val summonerUuid = getSummoner(entity)
        if (summonerUuid != null) {
            activeSummons[summonerUuid]?.remove(entity.uniqueId)
        }

        // Remove entity with effect
        entity.world.spawnParticle(
            org.bukkit.Particle.SMOKE,
            entity.location.add(0.0, 1.0, 0.0),
            20, 0.5, 0.5, 0.5, 0.05
        )
        entity.world.playSound(entity.location, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f)
        entity.remove()
    }

    /**
     * Dispel all summoned creatures for a player
     */
    fun dispelAllSummons(player: Player) {
        val summons = activeSummons[player.uniqueId]?.toSet() ?: return

        for (entityUuid in summons) {
            val entity = player.world.entities.find { it.uniqueId == entityUuid }
            if (entity != null && isSummoned(entity)) {
                dispelSummon(entity)
            }
        }

        activeSummons.remove(player.uniqueId)
    }

    /**
     * Dispel summoned creatures in a radius (for Mass Dispel)
     * Returns count of dispelled creatures
     */
    fun dispelSummonsInArea(center: org.bukkit.Location, radius: Double): Int {
        val radiusSquared = radius * radius
        var count = 0

        val nearbyEntities = center.world?.getNearbyEntities(center, radius, radius, radius) ?: return 0

        for (entity in nearbyEntities) {
            if (isSummoned(entity) && entity.location.distanceSquared(center) <= radiusSquared) {
                dispelSummon(entity)
                count++
            }
        }

        return count
    }

    /**
     * Clean up when player quits
     */
    fun onPlayerQuit(player: Player) {
        dispelAllSummons(player)
    }

    /**
     * Cleanup task - remove references to dead entities
     */
    fun cleanup() {
        for ((playerUuid, summons) in activeSummons) {
            val player = plugin.server.getPlayer(playerUuid) ?: continue
            summons.removeIf { entityUuid ->
                val entity = player.world.entities.find { it.uniqueId == entityUuid }
                entity == null || !entity.isValid
            }
        }
    }
}
