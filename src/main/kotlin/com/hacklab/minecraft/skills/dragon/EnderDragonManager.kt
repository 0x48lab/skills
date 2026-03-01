package com.hacklab.minecraft.skills.dragon

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EnderDragon
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.min

class EnderDragonManager(private val plugin: Skills) {

    private var dragonData: DragonData = DragonData()
    private var respawnTask: BukkitRunnable? = null
    private val preAnnounceTasks = mutableListOf<BukkitRunnable>()

    lateinit var skillExecutor: DragonSkillExecutor
        private set
    lateinit var dropGenerator: DragonDropGenerator
        private set

    val killCount: Int get() = dragonData.killCount
    val isDragonAlive: Boolean get() = dragonData.dragonAlive

    fun initialize() {
        dragonData = DragonData.loadFromDisk(plugin)
        dropGenerator = DragonDropGenerator(plugin)
        skillExecutor = DragonSkillExecutor(plugin, this)

        // Restore timer if needed
        val nextRespawn = dragonData.nextRespawnTime
        if (nextRespawn != null && !dragonData.dragonAlive) {
            val now = System.currentTimeMillis()
            if (nextRespawn <= now) {
                // Past due - respawn immediately (delayed by 1 second for world loading)
                object : BukkitRunnable() {
                    override fun run() {
                        performRespawn()
                    }
                }.runTaskLater(plugin, 20L)
            } else {
                scheduleRespawnAt(nextRespawn)
            }
        }

        // If dragon was alive when server stopped, find it and start skills
        if (dragonData.dragonAlive) {
            object : BukkitRunnable() {
                override fun run() {
                    val endWorld = getEndWorld() ?: return
                    val dragon = endWorld.getEntitiesByClass(EnderDragon::class.java).firstOrNull()
                    if (dragon != null) {
                        applyDragonStats(dragon)
                        skillExecutor.startSkills(dragon, dragonData.killCount)
                    } else {
                        // Dragon entity not found, mark as dead
                        dragonData.dragonAlive = false
                        saveData()
                    }
                }
            }.runTaskLater(plugin, 40L) // Wait for world to fully load
        }

        plugin.logger.info("EnderDragonManager initialized (killCount=${dragonData.killCount})")
    }

    // === Scaling Calculations ===

    fun getScaledMaxHp(): Double {
        val hpPerKill = plugin.skillsConfig.enderDragonHpPerKill
        val maxHp = plugin.skillsConfig.enderDragonMaxHp
        return min(200.0 + dragonData.killCount * hpPerKill, maxHp.toDouble())
    }

    fun getDamageScale(): Double {
        val scalePerKill = plugin.skillsConfig.enderDragonDamageScalePerKill
        val maxScale = plugin.skillsConfig.enderDragonMaxDamageScale
        return min(1.0 + dragonData.killCount * scalePerKill, maxScale)
    }

    fun getScaledXp(): Int = dropGenerator.getScaledXp(dragonData.killCount)

    fun getDragonLevel(): Int = dragonData.killCount + 1

    fun getActiveSkills(): List<DragonSkillType> = DragonSkillType.getActiveSkills(dragonData.killCount)

    fun hasSkill(skill: DragonSkillType): Boolean = dragonData.killCount >= skill.requiredKills

    fun getNextRespawnTime(): Long? = dragonData.nextRespawnTime

    // === Respawn Processing ===

    private fun scheduleRespawn() {
        val hours = plugin.skillsConfig.enderDragonRespawnIntervalHours
        val respawnTime = System.currentTimeMillis() + hours * 3600_000L
        dragonData.nextRespawnTime = respawnTime
        saveData()

        scheduleRespawnAt(respawnTime)
    }

    private fun scheduleRespawnAt(respawnTime: Long) {
        cancelRespawnTasks()

        val delayMs = respawnTime - System.currentTimeMillis()
        if (delayMs <= 0) {
            performRespawn()
            return
        }

        val delayTicks = delayMs / 50 // Convert ms to ticks

        respawnTask = object : BukkitRunnable() {
            override fun run() {
                performRespawn()
            }
        }
        respawnTask!!.runTaskLater(plugin, delayTicks)

        schedulePreAnnouncements(respawnTime)
    }

    private fun schedulePreAnnouncements(respawnTime: Long) {
        val announceMinutes = plugin.skillsConfig.enderDragonPreRespawnMinutes
        val now = System.currentTimeMillis()

        for (minutes in announceMinutes) {
            val announceTime = respawnTime - minutes * 60_000L
            val delayMs = announceTime - now
            if (delayMs <= 0) continue

            val task = object : BukkitRunnable() {
                override fun run() {
                    plugin.messageSender.broadcast(
                        MessageKey.DRAGON_RESPAWN_SOON,
                        "minutes" to minutes
                    )
                }
            }
            task.runTaskLater(plugin, delayMs / 50)
            preAnnounceTasks.add(task)
        }
    }

    private fun performRespawn() {
        val endWorld = getEndWorld()
        if (endWorld == null) {
            plugin.logger.warning("End world not found, cannot respawn dragon")
            return
        }

        val battle = endWorld.enderDragonBattle
        if (battle == null) {
            plugin.logger.warning("No DragonBattle found in End world")
            return
        }

        // Initiate respawn via DragonBattle API
        battle.initiateRespawn()

        dragonData.nextRespawnTime = null
        saveData()
    }

    // === Dragon Event Handlers ===

    fun onDragonSpawned(dragon: EnderDragon) {
        applyDragonStats(dragon)

        dragonData.dragonAlive = true
        cancelRespawnTasks()

        skillExecutor.startSkills(dragon, dragonData.killCount)

        // Broadcast respawn message
        plugin.messageSender.broadcast(
            MessageKey.DRAGON_RESPAWNED,
            "level" to getDragonLevel(),
            "hp" to getScaledMaxHp().toInt()
        )

        saveData()
    }

    fun onDragonKilled() {
        val previousKillCount = dragonData.killCount
        dragonData.killCount++
        dragonData.dragonAlive = false
        dragonData.lastKillTime = System.currentTimeMillis()

        skillExecutor.stopAllSkills()

        // Broadcast defeat message
        val hours = plugin.skillsConfig.enderDragonRespawnIntervalHours
        plugin.messageSender.broadcast(
            MessageKey.DRAGON_DEFEATED,
            "level" to (previousKillCount + 1),
            "hours" to hours
        )

        scheduleRespawn()
        saveData()
    }

    /**
     * Whether dragon egg should drop (only on first kill)
     */
    fun shouldDropEgg(): Boolean = dragonData.killCount == 0

    // === Stats Application ===

    private fun applyDragonStats(dragon: EnderDragon) {
        val scaledMaxHp = getScaledMaxHp()
        val healthAttr = dragon.getAttribute(Attribute.MAX_HEALTH)
        if (healthAttr != null) {
            healthAttr.baseValue = scaledMaxHp
            dragon.health = scaledMaxHp
        }

        // Set custom name with level
        val level = getDragonLevel()
        if (level > 1) {
            dragon.customName(
                net.kyori.adventure.text.Component.text("Ender Dragon ")
                    .append(net.kyori.adventure.text.Component.text("Lv.$level")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED))
            )
        }
    }

    // === Data Persistence ===

    fun saveData() {
        DragonData.saveToDisk(plugin, dragonData)
    }

    fun cleanup() {
        cancelRespawnTasks()
        if (::skillExecutor.isInitialized) {
            skillExecutor.stopAllSkills()
        }
        saveData()
    }

    private fun cancelRespawnTasks() {
        respawnTask?.let {
            if (!it.isCancelled) it.cancel()
        }
        respawnTask = null

        preAnnounceTasks.forEach {
            if (!it.isCancelled) it.cancel()
        }
        preAnnounceTasks.clear()
    }

    private fun getEndWorld(): World? {
        return plugin.server.worlds.firstOrNull { it.environment == World.Environment.THE_END }
    }
}
