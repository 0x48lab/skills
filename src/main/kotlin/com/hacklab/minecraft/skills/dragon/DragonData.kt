package com.hacklab.minecraft.skills.dragon

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

data class DragonData(
    var killCount: Int = 0,
    var nextRespawnTime: Long? = null,
    var lastKillTime: Long? = null,
    var dragonAlive: Boolean = false
) {
    companion object {
        private const val FILE_NAME = "dragon_data.yml"

        fun loadFromDisk(plugin: JavaPlugin): DragonData {
            val file = File(plugin.dataFolder, FILE_NAME)
            if (!file.exists()) return DragonData()

            val config = YamlConfiguration.loadConfiguration(file)
            return DragonData(
                killCount = config.getInt("kill_count", 0),
                nextRespawnTime = if (config.contains("next_respawn_time")) config.getLong("next_respawn_time") else null,
                lastKillTime = if (config.contains("last_kill_time")) config.getLong("last_kill_time") else null,
                dragonAlive = config.getBoolean("dragon_alive", false)
            )
        }

        fun saveToDisk(plugin: JavaPlugin, data: DragonData) {
            val file = File(plugin.dataFolder, FILE_NAME)
            val config = YamlConfiguration()

            config.set("kill_count", data.killCount)
            if (data.nextRespawnTime != null) {
                config.set("next_respawn_time", data.nextRespawnTime)
            }
            if (data.lastKillTime != null) {
                config.set("last_kill_time", data.lastKillTime)
            }
            config.set("dragon_alive", data.dragonAlive)

            config.save(file)
        }
    }
}
