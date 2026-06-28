package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatLockMode
import com.hacklab.minecraft.skills.skill.StatType
import java.sql.Connection
import java.util.*
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Unified JDBC-backed [Database] implementation that works for SQLite, MySQL and
 * PostgreSQL. Backend-specific SQL is delegated to a [SqlDialect]; everything
 * else (reads, parameter binding, transactions) is shared.
 *
 * Deliberately free of Bukkit/Skills dependencies so it can be unit-tested with a
 * plain [DataSource] (e.g. an in-memory / temp-file SQLite pool).
 */
class JdbcDatabase(
    private val dataSource: DataSource,
    private val type: DatabaseType,
    private val logger: Logger
) : Database {

    private val dialect: SqlDialect = type.dialect

    override fun connect() {
        // Validate connectivity early so misconfiguration fails fast at startup.
        dataSource.connection.use { /* no-op: borrowing proves the pool works */ }
        logger.info("Connected to ${type.name} database")
    }

    override fun disconnect() {
        (dataSource as? AutoCloseable)?.close()
        logger.info("Disconnected from ${type.name} database")
    }

    override fun createTables() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(dialect.createPlayersTableSql())
                stmt.executeUpdate(dialect.createSkillsTableSql())
                dialect.createSkillsIndexSql()?.let { stmt.executeUpdate(it) }
            }
            // Legacy SQLite databases may predate some columns; add them idempotently.
            if (type == DatabaseType.SQLITE) {
                migrateLegacySqliteColumns(conn)
            }
        }
        logger.info("Database tables created/verified")
    }

    /**
     * Idempotently add columns that were introduced after the original SQLite
     * schema shipped. Each ALTER is wrapped because SQLite has no
     * "ADD COLUMN IF NOT EXISTS" and throws when the column already exists.
     */
    private fun migrateLegacySqliteColumns(conn: Connection) {
        val playerAlters = listOf(
            "ALTER TABLE players ADD COLUMN str INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "ALTER TABLE players ADD COLUMN dex INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "ALTER TABLE players ADD COLUMN int INTEGER DEFAULT ${StatType.DEFAULT_STAT_VALUE}",
            "ALTER TABLE players ADD COLUMN str_lock TEXT DEFAULT 'UP'",
            "ALTER TABLE players ADD COLUMN dex_lock TEXT DEFAULT 'UP'",
            "ALTER TABLE players ADD COLUMN int_lock TEXT DEFAULT 'UP'",
            "ALTER TABLE players ADD COLUMN scoreboard_visible INTEGER DEFAULT 1",
            "ALTER TABLE players ADD COLUMN scoreboard_sections INTEGER DEFAULT 31",
            "ALTER TABLE skills ADD COLUMN lock_mode TEXT DEFAULT 'UP'"
        )
        for (sql in playerAlters) {
            try {
                conn.createStatement().use { it.executeUpdate(sql) }
            } catch (_: Exception) {
                // Column already exists — expected, ignore.
            }
        }
    }

    override fun loadPlayerData(uuid: UUID): PlayerData? {
        dataSource.connection.use { conn ->
            val playerData = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    PlayerData(
                        uuid = uuid,
                        playerName = rs.getString("player_name"),
                        internalHp = rs.getDouble("internal_hp"),
                        maxInternalHp = rs.getDouble("max_internal_hp"),
                        mana = rs.getDouble("mana"),
                        maxMana = rs.getDouble("max_mana"),
                        language = rs.getString("language")?.let { Language.fromCode(it) },
                        str = rs.getInt("str").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
                        dex = rs.getInt("dex").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
                        int = rs.getInt("int").takeIf { it > 0 } ?: StatType.DEFAULT_STAT_VALUE,
                        strLock = rs.getString("str_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP,
                        dexLock = rs.getString("dex_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP,
                        intLock = rs.getString("int_lock")?.let { StatLockMode.valueOf(it) } ?: StatLockMode.UP,
                        scoreboardVisible = rs.getInt("scoreboard_visible") != 0,
                        scoreboardSections = try { rs.getInt("scoreboard_sections") } catch (e: Exception) { 0x1F }
                    )
                }
            }

            conn.prepareStatement("SELECT * FROM skills WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val skillType = try {
                            SkillType.valueOf(rs.getString("skill_type"))
                        } catch (e: IllegalArgumentException) {
                            continue // Skip unknown skill types
                        }
                        val skillData = playerData.getSkill(skillType)
                        skillData.value = rs.getDouble("value")
                        skillData.lastUsed = rs.getLong("last_used")
                        skillData.lockMode = try {
                            rs.getString("lock_mode")?.let { SkillLockMode.valueOf(it) } ?: SkillLockMode.UP
                        } catch (e: Exception) {
                            SkillLockMode.UP
                        }
                    }
                }
            }

            playerData.updateMaxStats()
            return playerData
        }
    }

    override fun savePlayerData(data: PlayerData) {
        dataSource.connection.use { conn ->
            val previousAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                conn.prepareStatement(dialect.upsertPlayerSql()).use { stmt ->
                    stmt.setString(1, data.uuid.toString())
                    stmt.setString(2, data.playerName)
                    stmt.setDouble(3, data.internalHp)
                    stmt.setDouble(4, data.maxInternalHp)
                    stmt.setDouble(5, data.mana)
                    stmt.setDouble(6, data.maxMana)
                    stmt.setString(7, data.language?.code)
                    stmt.setLong(8, System.currentTimeMillis())
                    stmt.setInt(9, data.str)
                    stmt.setInt(10, data.dex)
                    stmt.setInt(11, data.int)
                    stmt.setString(12, data.strLock.name)
                    stmt.setString(13, data.dexLock.name)
                    stmt.setString(14, data.intLock.name)
                    stmt.setInt(15, if (data.scoreboardVisible) 1 else 0)
                    stmt.setInt(16, data.scoreboardSections)
                    stmt.executeUpdate()
                }

                conn.prepareStatement(dialect.upsertSkillSql()).use { stmt ->
                    data.getAllSkills().forEach { (skillType, skillData) ->
                        stmt.setString(1, data.uuid.toString())
                        stmt.setString(2, skillType.name)
                        stmt.setDouble(3, skillData.value)
                        stmt.setLong(4, skillData.lastUsed)
                        stmt.setString(5, skillData.lockMode.name)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                conn.commit()
            } catch (e: Exception) {
                try { conn.rollback() } catch (_: Exception) {}
                throw e
            } finally {
                conn.autoCommit = previousAutoCommit
            }
        }
    }

    override fun deletePlayerData(uuid: UUID) {
        dataSource.connection.use { conn ->
            // Skills are removed via ON DELETE CASCADE.
            conn.prepareStatement("DELETE FROM players WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    override fun playerExists(uuid: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT 1 FROM players WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs -> return rs.next() }
            }
        }
    }

    override fun getAllPlayerUuids(): List<UUID> {
        val uuids = mutableListOf<UUID>()
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT uuid FROM players").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        try {
                            uuids.add(UUID.fromString(rs.getString("uuid")))
                        } catch (e: IllegalArgumentException) {
                            // Skip malformed UUID rows.
                        }
                    }
                }
            }
        }
        return uuids
    }
}
