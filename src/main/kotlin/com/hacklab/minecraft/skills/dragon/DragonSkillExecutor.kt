package com.hacklab.minecraft.skills.dragon

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.EnderDragon
import org.bukkit.entity.Enderman
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DragonSkillExecutor(private val plugin: Skills, private val manager: EnderDragonManager) {

    private val summonedEndermen = ConcurrentHashMap.newKeySet<UUID>()
    private val crystalRegenTasks = ConcurrentHashMap<Location, BukkitRunnable>()
    private val skillTasks = ConcurrentHashMap<DragonSkillType, BukkitRunnable>()
    @Volatile
    var lastDragonDamageTime: Long = 0L
        private set

    val summonKey = NamespacedKey(plugin, "dragon_summon")

    private val excludedBlocks = setOf(
        Material.OBSIDIAN,
        Material.END_STONE,
        Material.END_STONE_BRICKS,
        Material.END_STONE_BRICK_SLAB,
        Material.END_STONE_BRICK_STAIRS,
        Material.END_STONE_BRICK_WALL,
        Material.BEDROCK,
        Material.END_PORTAL,
        Material.END_PORTAL_FRAME,
        Material.END_GATEWAY,
        Material.AIR,
        Material.CAVE_AIR,
        Material.VOID_AIR
    )

    fun hasSkill(skill: DragonSkillType): Boolean = manager.hasSkill(skill)

    fun recordDragonDamage() {
        lastDragonDamageTime = System.currentTimeMillis()
    }

    // === Skill Lifecycle ===

    fun startSkills(dragon: EnderDragon, killCount: Int) {
        val activeSkills = DragonSkillType.getActiveSkills(killCount)
        lastDragonDamageTime = System.currentTimeMillis()

        for (skill in activeSkills) {
            when (skill) {
                DragonSkillType.EXPLOSION_IMMUNITY -> {} // Handled in listener
                DragonSkillType.STRUCTURE_DESTRUCTION -> startStructureDestruction(dragon)
                DragonSkillType.HEAL_AURA -> startHealAura(dragon)
                DragonSkillType.ENDERMAN_SUMMON -> startEndermanSummon(dragon)
                DragonSkillType.CRYSTAL_REGENERATION -> {} // Handled reactively via onCrystalDestroyed
                DragonSkillType.PLAYER_PULL -> startPlayerPull(dragon)
            }
        }
    }

    fun stopAllSkills() {
        skillTasks.values.forEach {
            if (!it.isCancelled) it.cancel()
        }
        skillTasks.clear()
        cleanupSummonedEndermen()
        cancelAllCrystalRegen()
    }

    // === Structure Destruction (killCount >= 2) ===

    private fun startStructureDestruction(dragon: EnderDragon) {
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!dragon.isValid || dragon.isDead) {
                    cancel()
                    return
                }
                destroyNearbyStructures(dragon)
            }
        }
        task.runTaskTimer(plugin, 100L, 100L) // Every 5 seconds
        skillTasks[DragonSkillType.STRUCTURE_DESTRUCTION] = task
    }

    private fun destroyNearbyStructures(dragon: EnderDragon) {
        val center = dragon.location
        val world = center.world ?: return
        val radius = 15
        val blocksToDestroy = mutableListOf<Location>()

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    if (x * x + y * y + z * z > radius * radius) continue
                    val loc = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    val block = world.getBlockAt(loc)
                    if (!excludedBlocks.contains(block.type)) {
                        blocksToDestroy.add(loc)
                    }
                }
            }
        }

        if (blocksToDestroy.isEmpty()) return

        // Distributed destruction: 5 blocks per tick (ChainChoppingManager pattern)
        var index = 0
        object : BukkitRunnable() {
            override fun run() {
                var count = 0
                while (index < blocksToDestroy.size && count < 5) {
                    val loc = blocksToDestroy[index++]
                    val block = world.getBlockAt(loc)
                    if (!excludedBlocks.contains(block.type) && block.type != Material.AIR) {
                        world.spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 5, block.blockData)
                        block.type = Material.AIR
                    }
                    count++
                }
                if (index >= blocksToDestroy.size) {
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    // === Heal Aura (killCount >= 3) ===

    private fun startHealAura(dragon: EnderDragon) {
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!dragon.isValid || dragon.isDead) {
                    cancel()
                    return
                }
                processHealAura(dragon)
            }
        }
        task.runTaskTimer(plugin, 20L, 20L) // Every 1 second
        skillTasks[DragonSkillType.HEAL_AURA] = task
    }

    private fun processHealAura(dragon: EnderDragon) {
        val now = System.currentTimeMillis()
        if (now - lastDragonDamageTime < 30_000) return // Need 30s without damage

        val maxHp = manager.getScaledMaxHp()
        if (dragon.health >= maxHp) return

        val healAmount = maxHp * 0.05 // 5% per second
        dragon.health = (dragon.health + healAmount).coerceAtMost(maxHp)

        // Effects
        val loc = dragon.location
        loc.world?.spawnParticle(Particle.HEART, loc.clone().add(0.0, 2.0, 0.0), 5, 2.0, 1.0, 2.0)
        loc.world?.playSound(loc, Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.8f)
    }

    // === Enderman Summon (killCount >= 4) ===

    private fun startEndermanSummon(dragon: EnderDragon) {
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!dragon.isValid || dragon.isDead) {
                    cancel()
                    return
                }
                summonEndermen(dragon)
            }
        }
        task.runTaskTimer(plugin, 1200L, 1200L) // Every 60 seconds
        skillTasks[DragonSkillType.ENDERMAN_SUMMON] = task
    }

    private fun summonEndermen(dragon: EnderDragon) {
        // Clean up dead/invalid endermen from tracking
        summonedEndermen.removeIf { uuid ->
            val entity = plugin.server.getEntity(uuid)
            entity == null || !entity.isValid || entity.isDead
        }

        if (summonedEndermen.size >= 9) return // Max 9 endermen

        val world = dragon.world
        val center = dragon.location
        val toSpawn = 3.coerceAtMost(9 - summonedEndermen.size)

        for (i in 0 until toSpawn) {
            val spawnLoc = findSafeSpawnLocation(world, center, 10.0, 20.0) ?: continue
            val enderman = world.spawn(spawnLoc, Enderman::class.java)
            enderman.persistentDataContainer.set(summonKey, PersistentDataType.BYTE, 1)
            summonedEndermen.add(enderman.uniqueId)
        }
    }

    private fun cleanupSummonedEndermen() {
        for (uuid in summonedEndermen) {
            val entity = plugin.server.getEntity(uuid)
            if (entity != null && entity.isValid) {
                entity.remove()
            }
        }
        summonedEndermen.clear()
    }

    // === Crystal Regeneration (killCount >= 5) ===

    fun onCrystalDestroyed(location: Location) {
        // Cancel existing regen task for this location if any
        crystalRegenTasks.remove(location)?.let {
            if (!it.isCancelled) it.cancel()
        }

        val task = object : BukkitRunnable() {
            override fun run() {
                crystalRegenTasks.remove(location)
                val world = location.world ?: return
                // Check if dragon is still alive
                if (!manager.isDragonAlive) return

                world.spawn(location, EnderCrystal::class.java) { crystal ->
                    crystal.isShowingBottom = true
                }
                world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.0f)
                world.spawnParticle(Particle.END_ROD, location, 30, 1.0, 1.0, 1.0, 0.05)
            }
        }
        task.runTaskLater(plugin, 2400L) // 120 seconds
        crystalRegenTasks[location] = task
    }

    private fun cancelAllCrystalRegen() {
        crystalRegenTasks.values.forEach {
            if (!it.isCancelled) it.cancel()
        }
        crystalRegenTasks.clear()
    }

    // === Player Pull (killCount >= 6) ===

    private fun startPlayerPull(dragon: EnderDragon) {
        val task = object : BukkitRunnable() {
            override fun run() {
                if (!dragon.isValid || dragon.isDead) {
                    cancel()
                    return
                }
                pullDistantPlayers(dragon)
            }
        }
        task.runTaskTimer(plugin, 200L, 200L) // Every 10 seconds
        skillTasks[DragonSkillType.PLAYER_PULL] = task
    }

    private fun pullDistantPlayers(dragon: EnderDragon) {
        val dragonLoc = dragon.location
        val world = dragonLoc.world ?: return

        for (player in world.players) {
            val distance = player.location.distance(dragonLoc)
            if (distance < 50) continue // Only pull players 50+ blocks away

            val safeLoc = findSafeSpawnLocation(world, dragonLoc, 5.0, 20.0)
            if (safeLoc != null) {
                // Warning effects
                player.world.spawnParticle(Particle.PORTAL, player.location, 30, 0.5, 1.0, 0.5)
                plugin.messageSender.send(player, MessageKey.DRAGON_PULL_WARNING)

                // Teleport after short delay for visual effect
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    if (player.isOnline && player.world == world) {
                        player.teleport(safeLoc)
                        player.world.spawnParticle(Particle.PORTAL, safeLoc, 30, 0.5, 1.0, 0.5)
                    }
                }, 10L) // 0.5 second delay
            }
        }
    }

    // === Utility ===

    private fun findSafeSpawnLocation(world: World, center: Location, minDist: Double, maxDist: Double): Location? {
        repeat(10) { // Try up to 10 times
            val angle = Math.random() * Math.PI * 2
            val dist = minDist + Math.random() * (maxDist - minDist)
            val x = center.x + Math.cos(angle) * dist
            val z = center.z + Math.sin(angle) * dist

            val loc = Location(world, x, center.y, z)
            // Find the highest solid block
            val highestY = world.getHighestBlockYAt(loc.blockX, loc.blockZ)
            if (highestY < world.minHeight) return@repeat

            val safeLoc = Location(world, x, highestY + 1.0, z)
            // Check 2 blocks of air above
            val block1 = world.getBlockAt(safeLoc.blockX, safeLoc.blockY.toInt(), safeLoc.blockZ)
            val block2 = world.getBlockAt(safeLoc.blockX, safeLoc.blockY.toInt() + 1, safeLoc.blockZ)
            if (block1.type.isAir && block2.type.isAir) {
                return safeLoc
            }
        }
        return null
    }
}
