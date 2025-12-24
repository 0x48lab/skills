package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.data.PlayerData
import java.util.*

interface Database {
    fun connect()
    fun disconnect()
    fun createTables()

    fun loadPlayerData(uuid: UUID): PlayerData?
    fun savePlayerData(data: PlayerData)
    fun deletePlayerData(uuid: UUID)

    fun playerExists(uuid: UUID): Boolean
}
