package com.hacklab.minecraft.skills.data

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.database.Database
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerDataManager(
    private val plugin: Skills,
    private val database: Database
) {
    private val playerDataCache: MutableMap<UUID, PlayerData> = ConcurrentHashMap()

    /**
     * Load player data and return a pair of (data, isNewPlayer)
     */
    fun loadPlayer(player: Player): PlayerData {
        return loadPlayerWithStatus(player).first
    }

    /**
     * Load player data and return whether it's a new player
     */
    fun loadPlayerWithStatus(player: Player): Pair<PlayerData, Boolean> {
        val uuid = player.uniqueId

        // Check cache first
        playerDataCache[uuid]?.let { return it to false }

        // Load from database or create new
        val existingData = database.loadPlayerData(uuid)
        val isNew = existingData == null
        val data = existingData ?: PlayerData(
            uuid = uuid,
            playerName = player.name
        )

        data.updateMaxStats()
        playerDataCache[uuid] = data
        return data to isNew
    }

    fun getPlayerData(uuid: UUID): PlayerData? = playerDataCache[uuid]

    fun getPlayerData(player: Player): PlayerData = playerDataCache[player.uniqueId]
        ?: loadPlayer(player)

    fun savePlayer(uuid: UUID) {
        playerDataCache[uuid]?.let { data ->
            if (data.dirty) {
                database.savePlayerData(data)
                data.markClean()
            }
        }
    }

    fun savePlayer(player: Player) = savePlayer(player.uniqueId)

    fun unloadPlayer(uuid: UUID) {
        savePlayer(uuid)
        playerDataCache.remove(uuid)
    }

    fun unloadPlayer(player: Player) = unloadPlayer(player.uniqueId)

    fun saveAllPlayers() {
        playerDataCache.forEach { (uuid, _) ->
            savePlayer(uuid)
        }
    }

    fun getOnlinePlayerData(): Collection<PlayerData> = playerDataCache.values

    fun isPlayerLoaded(uuid: UUID): Boolean = playerDataCache.containsKey(uuid)
}
