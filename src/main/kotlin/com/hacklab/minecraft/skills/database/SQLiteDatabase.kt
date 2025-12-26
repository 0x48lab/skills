package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.data.SkillData
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatLockMode
import com.hacklab.minecraft.skills.skill.StatType
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class SQLiteDatabase(private val plugin: Skills) : Database {
    private var connection: Connection? = null

    override fun connect() {
        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val dbFile = File(dataFolder, "skills.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"

        connection = DriverManager.getConnection(url)
        plugin.logger.info("Connected to SQLite database")
    }

    override fun disconnect() {
        connection?.close()
        connection = null
        plugin.logger.info("Disconnected from SQLite database")
    }

    override fun createTables() {
        val conn = connection ?: return

        conn.createStatement().use { stmt ->
            // Players table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    internal_hp REAL DEFAULT 100.0,
                    max_internal_hp REAL DEFAULT 100.0,
                    mana REAL DEFAULT 20.0,
                    max_mana REAL DEFAULT 20.0,
                    language TEXT DEFAULT 'en',
                    last_login INTEGER DEFAULT 0,
                    str INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE},
                    dex INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE},
                    int INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE},
                    str_lock TEXT DEFAULT 'UP',
                    dex_lock TEXT DEFAULT 'UP',
                    int_lock TEXT DEFAULT 'UP'
                )
            """.trimIndent())

            // Skills table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS skills (
                    uuid TEXT NOT NULL,
                    skill_type TEXT NOT NULL,
                    value REAL DEFAULT 0.0,
                    last_used INTEGER DEFAULT 0,
                    PRIMARY KEY (uuid, skill_type),
                    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
            """.trimIndent())

            // Create index for faster lookups
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_skills_uuid ON skills(uuid)
            """.trimIndent())
        }

        // Migration: Add stat columns if they don't exist (for existing databases)
        migrateStatColumns()

        plugin.logger.info("Database tables created/verified")
    }

    private fun migrateStatColumns() {
        val conn = connection ?: return

        // Check if columns exist and add them if not
        val columnsToAdd = listOf(
            "str" to "INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "dex" to "INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "int" to "INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "str_lock" to "TEXT DEFAULT 'UP'",
            "dex_lock" to "TEXT DEFAULT 'UP'",
            "int_lock" to "TEXT DEFAULT 'UP'"
        )

        columnsToAdd.forEach { (columnName, columnDef) ->
            try {
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate("ALTER TABLE players ADD COLUMN $columnName $columnDef")
                }
                plugin.logger.info("Added column $columnName to players table")
            } catch (e: Exception) {
                // Column already exists, ignore
            }
        }
    }

    override fun loadPlayerData(uuid: UUID): PlayerData? {
        val conn = connection ?: return null

        // Load player base data
        val playerStmt = conn.prepareStatement(
            "SELECT * FROM players WHERE uuid = ?"
        )
        playerStmt.setString(1, uuid.toString())

        val playerResult = playerStmt.executeQuery()
        if (!playerResult.next()) {
            playerStmt.close()
            return null
        }

        val playerData = PlayerData(
            uuid = uuid,
            playerName = playerResult.getString("player_name"),
            internalHp = playerResult.getDouble("internal_hp"),
            maxInternalHp = playerResult.getDouble("max_internal_hp"),
            mana = playerResult.getDouble("mana"),
            maxMana = playerResult.getDouble("max_mana"),
            language = playerResult.getString("language")?.let { Language.fromCode(it) },
            str = playerResult.getInt("str").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
            dex = playerResult.getInt("dex").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
            int = playerResult.getInt("int").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
            strLock = playerResult.getString("str_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP,
            dexLock = playerResult.getString("dex_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP,
            intLock = playerResult.getString("int_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP
        )
        playerStmt.close()

        // Load skills
        val skillStmt = conn.prepareStatement(
            "SELECT * FROM skills WHERE uuid = ?"
        )
        skillStmt.setString(1, uuid.toString())

        val skillResult = skillStmt.executeQuery()
        while (skillResult.next()) {
            val skillTypeName = skillResult.getString("skill_type")
            val skillType = try {
                SkillType.valueOf(skillTypeName)
            } catch (e: IllegalArgumentException) {
                continue // Skip unknown skill types
            }

            val skillData = playerData.getSkill(skillType)
            skillData.value = skillResult.getDouble("value")
            skillData.lastUsed = skillResult.getLong("last_used")
        }
        skillStmt.close()

        playerData.updateMaxStats()
        return playerData
    }

    override fun savePlayerData(data: PlayerData) {
        val conn = connection ?: return

        // Save/update player base data
        val playerStmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO players (uuid, player_name, internal_hp, max_internal_hp, mana, max_mana, language, last_login, str, dex, int, str_lock, dex_lock, int_lock)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent())

        playerStmt.setString(1, data.uuid.toString())
        playerStmt.setString(2, data.playerName)
        playerStmt.setDouble(3, data.internalHp)
        playerStmt.setDouble(4, data.maxInternalHp)
        playerStmt.setDouble(5, data.mana)
        playerStmt.setDouble(6, data.maxMana)
        playerStmt.setString(7, data.language?.code)
        playerStmt.setLong(8, System.currentTimeMillis())
        playerStmt.setInt(9, data.str)
        playerStmt.setInt(10, data.dex)
        playerStmt.setInt(11, data.int)
        playerStmt.setString(12, data.strLock.name)
        playerStmt.setString(13, data.dexLock.name)
        playerStmt.setString(14, data.intLock.name)
        playerStmt.executeUpdate()
        playerStmt.close()

        // Save skills
        val skillStmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO skills (uuid, skill_type, value, last_used)
            VALUES (?, ?, ?, ?)
        """.trimIndent())

        data.getAllSkills().forEach { (skillType, skillData) ->
            skillStmt.setString(1, data.uuid.toString())
            skillStmt.setString(2, skillType.name)
            skillStmt.setDouble(3, skillData.value)
            skillStmt.setLong(4, skillData.lastUsed)
            skillStmt.addBatch()
        }

        skillStmt.executeBatch()
        skillStmt.close()
    }

    override fun deletePlayerData(uuid: UUID) {
        val conn = connection ?: return

        // Skills will be deleted by CASCADE
        val stmt = conn.prepareStatement("DELETE FROM players WHERE uuid = ?")
        stmt.setString(1, uuid.toString())
        stmt.executeUpdate()
        stmt.close()
    }

    override fun playerExists(uuid: UUID): Boolean {
        val conn = connection ?: return false

        val stmt = conn.prepareStatement("SELECT 1 FROM players WHERE uuid = ?")
        stmt.setString(1, uuid.toString())
        val result = stmt.executeQuery()
        val exists = result.next()
        stmt.close()
        return exists
    }
}
