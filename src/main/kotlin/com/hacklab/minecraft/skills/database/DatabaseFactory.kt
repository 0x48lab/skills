package com.hacklab.minecraft.skills.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.util.logging.Logger

/**
 * Builds pooled [JdbcDatabase] instances for any supported backend.
 *
 * Kept Bukkit-free: callers pass plain [ConnectionSettings] so the factory can be
 * exercised in unit tests. [Skills] supplies a thin convenience wrapper.
 */
object DatabaseFactory {

    data class ConnectionSettings(
        val type: DatabaseType,
        /** Path to the SQLite file (ignored for MySQL/PostgreSQL). */
        val sqliteFile: File? = null,
        val host: String = "localhost",
        val port: Int = 0,
        val databaseName: String = "skills",
        val user: String = "root",
        val password: String = "",
        val poolMaxSize: Int = 10,
        val poolMinIdle: Int = 2,
        val connectionTimeoutMs: Long = 10_000
    )

    /** Build a connection pool for the given settings. */
    fun createDataSource(settings: ConnectionSettings): HikariDataSource {
        val dialect = settings.type.dialect
        val config = HikariConfig().apply {
            poolName = "Skills-${settings.type.name}"
            driverClassName = dialect.driverClassName
            connectionTimeout = settings.connectionTimeoutMs
        }

        when (settings.type) {
            DatabaseType.SQLITE -> {
                val file = settings.sqliteFile
                    ?: throw IllegalArgumentException("SQLite backend requires a sqliteFile")
                file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                // WAL + busy_timeout greatly reduce "database is locked" under concurrency.
                // foreign_keys=on enables ON DELETE CASCADE.
                config.jdbcUrl = "jdbc:sqlite:${file.absolutePath}?" +
                    "foreign_keys=on&journal_mode=WAL&busy_timeout=5000"
                // SQLite is a single-writer engine; one pooled connection avoids lock contention
                // and the multiple-in-memory-DB pitfall in tests.
                config.maximumPoolSize = 1
            }
            DatabaseType.MYSQL -> {
                val port = if (settings.port > 0) settings.port else DatabaseType.MYSQL.defaultPort
                // createDatabaseIfNotExist=true makes the driver create the schema on first
                // connect (requires CREATE privilege). Table charset is pinned to utf8mb4 in
                // the DDL, so a non-utf8mb4 server default does not affect stored data.
                config.jdbcUrl = "jdbc:mysql://${settings.host}:$port/${settings.databaseName}" +
                    "?characterEncoding=utf8&autoReconnect=true&createDatabaseIfNotExist=true"
                config.username = settings.user
                config.password = settings.password
                applyPool(config, settings)
            }
            DatabaseType.POSTGRESQL -> {
                val port = if (settings.port > 0) settings.port else DatabaseType.POSTGRESQL.defaultPort
                config.jdbcUrl = "jdbc:postgresql://${settings.host}:$port/${settings.databaseName}"
                config.username = settings.user
                config.password = settings.password
                applyPool(config, settings)
            }
        }

        return HikariDataSource(config)
    }

    private fun applyPool(config: HikariConfig, settings: ConnectionSettings) {
        config.maximumPoolSize = settings.poolMaxSize.coerceAtLeast(1)
        config.minimumIdle = settings.poolMinIdle.coerceIn(0, settings.poolMaxSize)
    }

    /** Build a fully wired [JdbcDatabase] (does not call connect()). */
    fun createDatabase(settings: ConnectionSettings, logger: Logger): JdbcDatabase =
        JdbcDatabase(createDataSource(settings), settings.type, logger)
}
