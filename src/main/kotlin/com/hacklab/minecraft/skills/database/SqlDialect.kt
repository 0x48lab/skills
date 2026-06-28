package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.skill.StatType

/**
 * Encapsulates the SQL differences between database backends.
 *
 * Only two tables exist (`players`, `skills`), so the dialect surface is tiny:
 * the DDL types and the UPSERT syntax. Read queries (`SELECT *`) are identical
 * across all backends and live in [JdbcDatabase].
 *
 * Reserved-word columns `int` (players) and `value` (skills) are quoted per
 * backend. SQLite tolerates them unquoted, matching the legacy schema.
 */
interface SqlDialect {
    /** Fully-qualified JDBC driver class name. */
    val driverClassName: String

    /** DDL for the players table (CREATE TABLE IF NOT EXISTS). */
    fun createPlayersTableSql(): String

    /** DDL for the skills table (CREATE TABLE IF NOT EXISTS). */
    fun createSkillsTableSql(): String

    /** Optional secondary index DDL, or null if not needed for this backend. */
    fun createSkillsIndexSql(): String?

    /** UPSERT for a players row (16 positional params). */
    fun upsertPlayerSql(): String

    /** UPSERT for a skills row (5 positional params). */
    fun upsertSkillSql(): String
}

private const val DEFAULT_STAT = StatType.DEFAULT_STAT_VALUE

private val PLAYER_COLS_SQLITE =
    "uuid, player_name, internal_hp, max_internal_hp, mana, max_mana, language, last_login, " +
        "str, dex, int, str_lock, dex_lock, int_lock, scoreboard_visible, scoreboard_sections"

private val PLAYER_COLS_MYSQL =
    "uuid, player_name, internal_hp, max_internal_hp, mana, max_mana, language, last_login, " +
        "str, dex, `int`, str_lock, dex_lock, int_lock, scoreboard_visible, scoreboard_sections"

private val PLAYER_COLS_PG =
    "uuid, player_name, internal_hp, max_internal_hp, mana, max_mana, language, last_login, " +
        "str, dex, \"int\", str_lock, dex_lock, int_lock, scoreboard_visible, scoreboard_sections"

private const val PLAYER_PLACEHOLDERS = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
private const val SKILL_PLACEHOLDERS = "?, ?, ?, ?, ?"

object SqliteDialect : SqlDialect {
    override val driverClassName = "org.sqlite.JDBC"

    override fun createPlayersTableSql(): String = """
        CREATE TABLE IF NOT EXISTS players (
            uuid TEXT PRIMARY KEY,
            player_name TEXT NOT NULL,
            internal_hp REAL DEFAULT 100.0,
            max_internal_hp REAL DEFAULT 100.0,
            mana REAL DEFAULT 20.0,
            max_mana REAL DEFAULT 20.0,
            language TEXT DEFAULT 'en',
            last_login INTEGER DEFAULT 0,
            str INTEGER DEFAULT $DEFAULT_STAT,
            dex INTEGER DEFAULT $DEFAULT_STAT,
            int INTEGER DEFAULT $DEFAULT_STAT,
            str_lock TEXT DEFAULT 'UP',
            dex_lock TEXT DEFAULT 'UP',
            int_lock TEXT DEFAULT 'UP',
            scoreboard_visible INTEGER DEFAULT 1,
            scoreboard_sections INTEGER DEFAULT 31
        )
    """.trimIndent()

    override fun createSkillsTableSql(): String = """
        CREATE TABLE IF NOT EXISTS skills (
            uuid TEXT NOT NULL,
            skill_type TEXT NOT NULL,
            value REAL DEFAULT 0.0,
            last_used INTEGER DEFAULT 0,
            lock_mode TEXT DEFAULT 'UP',
            PRIMARY KEY (uuid, skill_type),
            FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
        )
    """.trimIndent()

    override fun createSkillsIndexSql(): String =
        "CREATE INDEX IF NOT EXISTS idx_skills_uuid ON skills(uuid)"

    override fun upsertPlayerSql(): String =
        "INSERT OR REPLACE INTO players ($PLAYER_COLS_SQLITE) VALUES ($PLAYER_PLACEHOLDERS)"

    override fun upsertSkillSql(): String =
        "INSERT OR REPLACE INTO skills (uuid, skill_type, value, last_used, lock_mode) " +
            "VALUES ($SKILL_PLACEHOLDERS)"
}

object MysqlDialect : SqlDialect {
    override val driverClassName = "com.mysql.cj.jdbc.Driver"

    override fun createPlayersTableSql(): String = """
        CREATE TABLE IF NOT EXISTS players (
            uuid VARCHAR(36) PRIMARY KEY,
            player_name VARCHAR(32) NOT NULL,
            internal_hp DOUBLE DEFAULT 100.0,
            max_internal_hp DOUBLE DEFAULT 100.0,
            mana DOUBLE DEFAULT 20.0,
            max_mana DOUBLE DEFAULT 20.0,
            language VARCHAR(8) DEFAULT 'en',
            last_login BIGINT DEFAULT 0,
            str INT DEFAULT $DEFAULT_STAT,
            dex INT DEFAULT $DEFAULT_STAT,
            `int` INT DEFAULT $DEFAULT_STAT,
            str_lock VARCHAR(8) DEFAULT 'UP',
            dex_lock VARCHAR(8) DEFAULT 'UP',
            int_lock VARCHAR(8) DEFAULT 'UP',
            scoreboard_visible INT DEFAULT 1,
            scoreboard_sections INT DEFAULT 31
        ) DEFAULT CHARSET=utf8mb4
    """.trimIndent()

    override fun createSkillsTableSql(): String = """
        CREATE TABLE IF NOT EXISTS skills (
            uuid VARCHAR(36) NOT NULL,
            skill_type VARCHAR(48) NOT NULL,
            `value` DOUBLE DEFAULT 0.0,
            last_used BIGINT DEFAULT 0,
            lock_mode VARCHAR(8) DEFAULT 'UP',
            PRIMARY KEY (uuid, skill_type),
            FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
        ) DEFAULT CHARSET=utf8mb4
    """.trimIndent()

    // InnoDB auto-indexes the FK column, so no extra index is needed.
    override fun createSkillsIndexSql(): String? = null

    override fun upsertPlayerSql(): String = """
        INSERT INTO players ($PLAYER_COLS_MYSQL) VALUES ($PLAYER_PLACEHOLDERS)
        ON DUPLICATE KEY UPDATE
            player_name=VALUES(player_name), internal_hp=VALUES(internal_hp),
            max_internal_hp=VALUES(max_internal_hp), mana=VALUES(mana), max_mana=VALUES(max_mana),
            language=VALUES(language), last_login=VALUES(last_login),
            str=VALUES(str), dex=VALUES(dex), `int`=VALUES(`int`),
            str_lock=VALUES(str_lock), dex_lock=VALUES(dex_lock), int_lock=VALUES(int_lock),
            scoreboard_visible=VALUES(scoreboard_visible), scoreboard_sections=VALUES(scoreboard_sections)
    """.trimIndent()

    override fun upsertSkillSql(): String = """
        INSERT INTO skills (uuid, skill_type, `value`, last_used, lock_mode)
        VALUES ($SKILL_PLACEHOLDERS)
        ON DUPLICATE KEY UPDATE
            `value`=VALUES(`value`), last_used=VALUES(last_used), lock_mode=VALUES(lock_mode)
    """.trimIndent()
}

object PostgresDialect : SqlDialect {
    override val driverClassName = "org.postgresql.Driver"

    override fun createPlayersTableSql(): String = """
        CREATE TABLE IF NOT EXISTS players (
            uuid VARCHAR(36) PRIMARY KEY,
            player_name VARCHAR(32) NOT NULL,
            internal_hp DOUBLE PRECISION DEFAULT 100.0,
            max_internal_hp DOUBLE PRECISION DEFAULT 100.0,
            mana DOUBLE PRECISION DEFAULT 20.0,
            max_mana DOUBLE PRECISION DEFAULT 20.0,
            language VARCHAR(8) DEFAULT 'en',
            last_login BIGINT DEFAULT 0,
            str INTEGER DEFAULT $DEFAULT_STAT,
            dex INTEGER DEFAULT $DEFAULT_STAT,
            "int" INTEGER DEFAULT $DEFAULT_STAT,
            str_lock VARCHAR(8) DEFAULT 'UP',
            dex_lock VARCHAR(8) DEFAULT 'UP',
            int_lock VARCHAR(8) DEFAULT 'UP',
            scoreboard_visible INTEGER DEFAULT 1,
            scoreboard_sections INTEGER DEFAULT 31
        )
    """.trimIndent()

    override fun createSkillsTableSql(): String = """
        CREATE TABLE IF NOT EXISTS skills (
            uuid VARCHAR(36) NOT NULL,
            skill_type VARCHAR(48) NOT NULL,
            "value" DOUBLE PRECISION DEFAULT 0.0,
            last_used BIGINT DEFAULT 0,
            lock_mode VARCHAR(8) DEFAULT 'UP',
            PRIMARY KEY (uuid, skill_type),
            FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
        )
    """.trimIndent()

    override fun createSkillsIndexSql(): String =
        "CREATE INDEX IF NOT EXISTS idx_skills_uuid ON skills(uuid)"

    override fun upsertPlayerSql(): String = """
        INSERT INTO players ($PLAYER_COLS_PG) VALUES ($PLAYER_PLACEHOLDERS)
        ON CONFLICT (uuid) DO UPDATE SET
            player_name=EXCLUDED.player_name, internal_hp=EXCLUDED.internal_hp,
            max_internal_hp=EXCLUDED.max_internal_hp, mana=EXCLUDED.mana, max_mana=EXCLUDED.max_mana,
            language=EXCLUDED.language, last_login=EXCLUDED.last_login,
            str=EXCLUDED.str, dex=EXCLUDED.dex, "int"=EXCLUDED."int",
            str_lock=EXCLUDED.str_lock, dex_lock=EXCLUDED.dex_lock, int_lock=EXCLUDED.int_lock,
            scoreboard_visible=EXCLUDED.scoreboard_visible, scoreboard_sections=EXCLUDED.scoreboard_sections
    """.trimIndent()

    override fun upsertSkillSql(): String = """
        INSERT INTO skills (uuid, skill_type, "value", last_used, lock_mode)
        VALUES ($SKILL_PLACEHOLDERS)
        ON CONFLICT (uuid, skill_type) DO UPDATE SET
            "value"=EXCLUDED."value", last_used=EXCLUDED.last_used, lock_mode=EXCLUDED.lock_mode
    """.trimIndent()
}
