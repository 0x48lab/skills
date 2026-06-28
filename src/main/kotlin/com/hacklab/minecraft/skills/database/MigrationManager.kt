package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.Skills
import java.io.File

/**
 * One-shot data migration from the legacy SQLite file (`skills.db`) into the
 * currently active database (MySQL or PostgreSQL).
 *
 * Run via `/skilladmin migrate` after switching `database.type` and restarting.
 * The copy is non-destructive: the source `skills.db` is only renamed (never
 * deleted) once the migration succeeds, so a failed run leaves data intact.
 */
class MigrationManager(private val plugin: Skills) {

    data class Result(val success: Boolean, val migratedCount: Int, val message: String)

    fun migrateSqliteToActive(): Result {
        val activeType = DatabaseType.fromConfig(plugin.skillsConfig.databaseType)
        if (activeType == DatabaseType.SQLITE) {
            return Result(
                false, 0,
                "Active database is SQLite. Set database.type to mysql/postgresql and restart, then run migrate."
            )
        }

        val sqliteFile = File(plugin.dataFolder, "skills.db")
        if (!sqliteFile.exists()) {
            return Result(false, 0, "No skills.db found to migrate from (${sqliteFile.absolutePath}).")
        }

        val source = DatabaseFactory.createDatabase(
            DatabaseFactory.ConnectionSettings(type = DatabaseType.SQLITE, sqliteFile = sqliteFile),
            plugin.logger
        )

        return try {
            source.connect()
            source.createTables() // ensure legacy columns exist before reading
            val target = plugin.database

            val uuids = source.getAllPlayerUuids()
            var count = 0
            for (uuid in uuids) {
                val data = source.loadPlayerData(uuid) ?: continue
                target.savePlayerData(data)
                count++
            }
            source.disconnect()

            // Rename the source so a second run doesn't re-import stale data.
            val archived = File(plugin.dataFolder, "skills.db.migrated.${System.currentTimeMillis()}")
            val renamed = sqliteFile.renameTo(archived)
            val note = if (renamed) {
                " Source archived as ${archived.name}."
            } else {
                " (Warning: could not rename skills.db; archive it manually to avoid re-migrating.)"
            }

            Result(true, count, "Migrated $count player(s) to ${activeType.name}.$note")
        } catch (e: Exception) {
            try { source.disconnect() } catch (_: Exception) {}
            plugin.logger.severe("Migration failed: ${e.message}")
            Result(false, 0, "Migration failed: ${e.message}. Source skills.db left untouched.")
        }
    }
}
