package com.hacklab.minecraft.skills.database

/**
 * Supported database backends.
 *
 * SQLite is the default (file-based, no external server). MySQL and PostgreSQL
 * are available for multi-server / large-scale setups; switching is done via
 * config.yml (`database.type`) and migrating data with `/skilladmin migrate`.
 */
enum class DatabaseType(val dialect: SqlDialect, val defaultPort: Int) {
    SQLITE(SqliteDialect, 0),
    MYSQL(MysqlDialect, 3306),
    POSTGRESQL(PostgresDialect, 5432);

    companion object {
        fun fromConfig(value: String): DatabaseType = when (value.trim().lowercase()) {
            "sqlite" -> SQLITE
            "mysql", "mariadb" -> MYSQL
            "postgresql", "postgres", "pgsql" -> POSTGRESQL
            else -> SQLITE
        }
    }
}
