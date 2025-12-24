package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GateManager(private val plugin: Skills) {

    // Active gates: gatePairId -> GatePair
    private val activeGates = ConcurrentHashMap<UUID, GatePair>()

    // Teleport cooldown to prevent infinite loop
    private val teleportCooldown = ConcurrentHashMap<UUID, Long>()

    data class GatePair(
        val id: UUID,
        val gateA: Gate,
        val gateB: Gate,
        val caster: UUID,
        val expiresAt: Long
    )

    data class Gate(
        val location: Location,
        val world: org.bukkit.World
    )

    /**
     * Create a bidirectional gate between two locations
     * @param caster The player who cast the spell
     * @param locationA First gate location (usually caster's location)
     * @param locationB Second gate location (rune's marked location)
     * @param durationTicks How long the gates last
     */
    fun createGate(
        caster: Player,
        locationA: Location,
        locationB: Location,
        durationTicks: Int
    ): UUID {
        val pairId = UUID.randomUUID()

        val gateA = Gate(locationA.clone(), locationA.world!!)
        val gateB = Gate(locationB.clone(), locationB.world!!)

        val gatePair = GatePair(
            id = pairId,
            gateA = gateA,
            gateB = gateB,
            caster = caster.uniqueId,
            expiresAt = System.currentTimeMillis() + (durationTicks * 50L)
        )

        activeGates[pairId] = gatePair

        // Play creation sound at both locations
        locationA.world?.playSound(locationA, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f)
        locationB.world?.playSound(locationB, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f)

        // Capture primitive values for the runnable
        val locAX = locationA.x
        val locAY = locationA.y
        val locAZ = locationA.z
        val worldA = locationA.world!!

        val locBX = locationB.x
        val locBY = locationB.y
        val locBZ = locationB.z
        val worldB = locationB.world!!

        // Start gate effect and teleportation task
        object : BukkitRunnable() {
            var ticksRemaining = durationTicks

            override fun run() {
                // Check if gate was removed
                if (!activeGates.containsKey(pairId)) {
                    cancel()
                    return
                }

                if (ticksRemaining <= 0) {
                    // Gate expires
                    closeGate(pairId)
                    cancel()
                    return
                }

                // Spawn portal particles at both gates (every tick for smooth effect)
                spawnGateParticles(worldA, locAX, locAY, locAZ)
                spawnGateParticles(worldB, locBX, locBY, locBZ)

                // Play ambient sound every 40 ticks
                if (ticksRemaining % 40 == 0) {
                    worldA.playSound(Location(worldA, locAX, locAY, locAZ), Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 1.0f)
                    worldB.playSound(Location(worldB, locBX, locBY, locBZ), Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 1.0f)
                }

                // Check for entities every 10 ticks (0.5 seconds)
                if (ticksRemaining % 10 == 0) {
                    checkAndTeleport(pairId, worldA, locAX, locAY, locAZ, worldB, locBX, locBY, locBZ)
                }

                ticksRemaining--
            }
        }.runTaskTimer(plugin, 0L, 1L)

        return pairId
    }

    private fun spawnGateParticles(world: org.bukkit.World, x: Double, y: Double, z: Double) {
        val loc = Location(world, x, y, z)

        // Create a vertical ring of particles (portal shape)
        for (angle in 0 until 360 step 30) {
            val radians = Math.toRadians(angle.toDouble())
            val offsetX = kotlin.math.cos(radians) * 0.8
            val offsetZ = kotlin.math.sin(radians) * 0.8

            // Bottom ring
            world.spawnParticle(Particle.PORTAL, loc.clone().add(offsetX, 0.3, offsetZ), 1, 0.0, 0.0, 0.0, 0.0)
            // Middle ring
            world.spawnParticle(Particle.PORTAL, loc.clone().add(offsetX * 0.9, 1.0, offsetZ * 0.9), 2, 0.0, 0.0, 0.0, 0.0)
            // Top ring
            world.spawnParticle(Particle.PORTAL, loc.clone().add(offsetX * 0.7, 1.7, offsetZ * 0.7), 1, 0.0, 0.0, 0.0, 0.0)
        }

        // Center particles (portal inside)
        world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0.0, 1.0, 0.0), 5, 0.3, 0.5, 0.3, 0.02)
        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0.0, 1.0, 0.0), 3, 0.2, 0.3, 0.2, 0.5)
    }

    private fun checkAndTeleport(
        pairId: UUID,
        worldA: org.bukkit.World,
        aX: Double, aY: Double, aZ: Double,
        worldB: org.bukkit.World,
        bX: Double, bY: Double, bZ: Double
    ) {
        val gatePair = activeGates[pairId] ?: return

        val locA = Location(worldA, aX, aY, aZ)
        val locB = Location(worldB, bX, bY, bZ)

        // Track entities teleported in this check to avoid immediate return trip
        val teleportedThisCheck = mutableSetOf<UUID>()

        // Check entities near gate A
        worldA.getNearbyEntities(locA, 1.5, 2.0, 1.5).forEach { entity ->
            if (entity is LivingEntity && canTeleport(entity)) {
                teleportThroughGate(entity, locB)
                teleportedThisCheck.add(entity.uniqueId)
            }
        }

        // Check entities near gate B (skip if just teleported from A)
        worldB.getNearbyEntities(locB, 1.5, 2.0, 1.5).forEach { entity ->
            if (entity is LivingEntity &&
                !teleportedThisCheck.contains(entity.uniqueId) &&
                canTeleport(entity)) {
                teleportThroughGate(entity, locA)
            }
        }
    }

    private fun canTeleport(entity: LivingEntity): Boolean {
        val lastTeleport = teleportCooldown[entity.uniqueId] ?: 0L
        return System.currentTimeMillis() - lastTeleport > 3000 // 3 second cooldown
    }

    private fun teleportThroughGate(entity: LivingEntity, destination: Location) {
        // Set cooldown
        teleportCooldown[entity.uniqueId] = System.currentTimeMillis()

        // Effect at departure
        entity.world.spawnParticle(Particle.PORTAL, entity.location.add(0.0, 1.0, 0.0), 30, 0.3, 0.5, 0.3, 0.5)
        entity.world.playSound(entity.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f)

        // Teleport
        val safeDest = destination.clone()
        safeDest.yaw = entity.location.yaw
        safeDest.pitch = entity.location.pitch
        entity.teleport(safeDest)

        // Effect at arrival
        entity.world.spawnParticle(Particle.PORTAL, entity.location.add(0.0, 1.0, 0.0), 30, 0.3, 0.5, 0.3, 0.5)
        entity.world.playSound(entity.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f)

        // Notify if player
        if (entity is Player) {
            plugin.messageSender.send(entity, com.hacklab.minecraft.skills.i18n.MessageKey.GATE_TRAVEL_USED)
        }
    }

    /**
     * Close a specific gate pair
     */
    fun closeGate(pairId: UUID) {
        val gatePair = activeGates.remove(pairId) ?: return

        // Closing effect at both locations
        gatePair.gateA.world.spawnParticle(
            Particle.SMOKE,
            gatePair.gateA.location.clone().add(0.0, 1.0, 0.0),
            30, 0.5, 0.8, 0.5, 0.05
        )
        gatePair.gateA.world.playSound(gatePair.gateA.location, Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 0.5f)

        gatePair.gateB.world.spawnParticle(
            Particle.SMOKE,
            gatePair.gateB.location.clone().add(0.0, 1.0, 0.0),
            30, 0.5, 0.8, 0.5, 0.05
        )
        gatePair.gateB.world.playSound(gatePair.gateB.location, Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 0.5f)
    }

    /**
     * Close all gates created by a specific player
     */
    fun closeGatesForPlayer(playerId: UUID) {
        activeGates.entries.removeIf { (id, pair) ->
            if (pair.caster == playerId) {
                closeGate(id)
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if a player has an active gate
     */
    fun hasActiveGate(playerId: UUID): Boolean {
        return activeGates.values.any { it.caster == playerId }
    }

    /**
     * Cleanup expired cooldowns
     */
    fun cleanupCooldowns() {
        val now = System.currentTimeMillis()
        teleportCooldown.entries.removeIf { (_, time) -> now - time > 5000 }
    }
}
