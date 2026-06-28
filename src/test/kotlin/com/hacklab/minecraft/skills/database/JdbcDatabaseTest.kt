package com.hacklab.minecraft.skills.database

import com.hacklab.minecraft.skills.data.PlayerData
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatLockMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID
import java.util.logging.Logger

/**
 * Exercises the JDBC database layer against a temp-file SQLite backend
 * (the same engine used in production), with no Bukkit dependency.
 */
class JdbcDatabaseTest {

    private val logger: Logger = Logger.getLogger("JdbcDatabaseTest")

    private fun openDb(file: File): JdbcDatabase {
        val db = DatabaseFactory.createDatabase(
            DatabaseFactory.ConnectionSettings(type = DatabaseType.SQLITE, sqliteFile = file),
            logger
        )
        db.connect()
        db.createTables()
        return db
    }

    private fun sampleData(uuid: UUID = UUID.randomUUID()): PlayerData {
        // str=40 -> maxHp computed as 140 on load; keep internalHp within range.
        val data = PlayerData(
            uuid = uuid,
            playerName = "Tester",
            internalHp = 120.0,
            mana = 15.0,
            language = Language.JAPANESE,
            str = 40,
            dex = 30,
            int = 20,
            strLock = StatLockMode.DOWN,
            dexLock = StatLockMode.LOCKED,
            intLock = StatLockMode.UP,
            scoreboardVisible = false,
            scoreboardSections = 0x0A
        )
        data.getSkill(SkillType.WRESTLING).apply {
            value = 55.5; lockMode = SkillLockMode.DOWN; lastUsed = 12345L
        }
        data.getSkill(SkillType.MINING).apply {
            value = 10.0; lockMode = SkillLockMode.LOCKED
        }
        return data
    }

    @Test
    fun `save then load round-trips all fields`(@TempDir tmp: Path) {
        val file = tmp.resolve("rt.db").toFile()
        val uuid = UUID.randomUUID()

        openDb(file).use { db ->
            db.savePlayerData(sampleData(uuid))
        }

        // Reopen a fresh instance to prove data persisted to the file.
        openDb(file).use { db ->
            val loaded = assertNotNull(db.loadPlayerData(uuid))
            assertEquals("Tester", loaded.playerName)
            assertEquals(40, loaded.str)
            assertEquals(30, loaded.dex)
            assertEquals(20, loaded.int)
            assertEquals(StatLockMode.DOWN, loaded.strLock)
            assertEquals(StatLockMode.LOCKED, loaded.dexLock)
            assertEquals(StatLockMode.UP, loaded.intLock)
            assertEquals(Language.JAPANESE, loaded.language)
            assertFalse(loaded.scoreboardVisible)
            assertEquals(0x0A, loaded.scoreboardSections)
            assertEquals(120.0, loaded.internalHp)
            assertEquals(15.0, loaded.mana)
            // maxInternalHp is recomputed as 100 + str on load.
            assertEquals(140.0, loaded.maxInternalHp)

            val wrestling = loaded.getSkill(SkillType.WRESTLING)
            assertEquals(55.5, wrestling.value)
            assertEquals(SkillLockMode.DOWN, wrestling.lockMode)
            assertEquals(12345L, wrestling.lastUsed)

            val mining = loaded.getSkill(SkillType.MINING)
            assertEquals(10.0, mining.value)
            assertEquals(SkillLockMode.LOCKED, mining.lockMode)

            // A skill never touched keeps defaults.
            val archery = loaded.getSkill(SkillType.ARCHERY)
            assertEquals(0.0, archery.value)
            assertEquals(SkillLockMode.UP, archery.lockMode)
        }
    }

    @Test
    fun `repeated save upserts without duplicating rows`(@TempDir tmp: Path) {
        val file = tmp.resolve("upsert.db").toFile()
        val uuid = UUID.randomUUID()

        openDb(file).use { db ->
            val data = sampleData(uuid)
            db.savePlayerData(data)
            // Mutate and save again — must update, not insert a second row.
            data.getSkill(SkillType.WRESTLING).value = 77.0
            db.savePlayerData(data)

            assertEquals(listOf(uuid), db.getAllPlayerUuids())
            val reloaded = assertNotNull(db.loadPlayerData(uuid))
            assertEquals(77.0, reloaded.getSkill(SkillType.WRESTLING).value)
        }
    }

    @Test
    fun `delete removes player and skills`(@TempDir tmp: Path) {
        val file = tmp.resolve("del.db").toFile()
        val uuid = UUID.randomUUID()

        openDb(file).use { db ->
            db.savePlayerData(sampleData(uuid))
            assertTrue(db.playerExists(uuid))

            db.deletePlayerData(uuid)
            assertFalse(db.playerExists(uuid))
            assertNull(db.loadPlayerData(uuid))
            assertTrue(db.getAllPlayerUuids().isEmpty())
        }
    }

    @Test
    fun `getAllPlayerUuids returns every saved player`(@TempDir tmp: Path) {
        val file = tmp.resolve("all.db").toFile()
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()

        openDb(file).use { db ->
            db.savePlayerData(sampleData(a))
            db.savePlayerData(sampleData(b))
            val all = db.getAllPlayerUuids().toSet()
            assertEquals(setOf(a, b), all)
        }
    }

    @Test
    fun `migration copies all data from source to target`(@TempDir tmp: Path) {
        val sourceFile = tmp.resolve("source.db").toFile()
        val targetFile = tmp.resolve("target.db").toFile()
        val uuid = UUID.randomUUID()

        // Seed the source.
        openDb(sourceFile).use { src -> src.savePlayerData(sampleData(uuid)) }

        // Mimic MigrationManager's copy loop against a separate target backend.
        val source = openDb(sourceFile)
        val target = openDb(targetFile)
        try {
            var count = 0
            for (id in source.getAllPlayerUuids()) {
                val data = source.loadPlayerData(id) ?: continue
                target.savePlayerData(data)
                count++
            }
            assertEquals(1, count)

            val migrated = assertNotNull(target.loadPlayerData(uuid))
            assertEquals("Tester", migrated.playerName)
            assertEquals(40, migrated.str)
            assertEquals(55.5, migrated.getSkill(SkillType.WRESTLING).value)
            assertEquals(SkillLockMode.DOWN, migrated.getSkill(SkillType.WRESTLING).lockMode)
        } finally {
            source.disconnect()
            target.disconnect()
        }
    }
}

/** Allow JdbcDatabase to be used in Kotlin's `use {}` for test cleanup. */
private inline fun <R> JdbcDatabase.use(block: (JdbcDatabase) -> R): R =
    try { block(this) } finally { this.disconnect() }
