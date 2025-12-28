package com.hacklab.minecraft.skills.config

import com.hacklab.minecraft.skills.Skills
import org.bukkit.configuration.file.FileConfiguration

class SkillsConfig(private val plugin: Skills) {
    private val config: FileConfiguration get() = plugin.config

    // Database
    val databaseType: String get() = config.getString("database.type", "sqlite") ?: "sqlite"
    val databaseHost: String get() = config.getString("database.host", "localhost") ?: "localhost"
    val databasePort: Int get() = config.getInt("database.port", 3306)
    val databaseName: String get() = config.getString("database.name", "skills") ?: "skills"
    val databaseUser: String get() = config.getString("database.user", "root") ?: "root"
    val databasePassword: String get() = config.getString("database.password", "") ?: ""

    // Skill settings
    val skillCap: Double get() = config.getDouble("skills.total_cap", 600.0)
    val skillMax: Double get() = config.getDouble("skills.individual_max", 100.0)
    val skillGainAmount: Double get() = config.getDouble("skills.gain_amount", 0.1)

    // Combat settings
    val baseDamageMultiplier: Double get() = config.getDouble("combat.damage_multiplier", 10.0)
    val parryChanceMax: Double get() = config.getDouble("combat.parry_chance_max", 50.0)
    val criticalChanceMax: Double get() = config.getDouble("combat.critical_chance_max", 50.0)

    // Magic settings
    val castingTimeBase: Long get() = config.getLong("magic.casting_time_base", 2000)
    val targetingTimeout: Long get() = config.getLong("magic.targeting_timeout", 10000)
    val spellRange: Double get() = config.getDouble("magic.spell_range", 12.0)

    // Thief settings
    val hideMovementBreaks: Boolean get() = config.getBoolean("thief.hide_movement_breaks", false)
    val stealthMaxDistance: Double get() = config.getDouble("thief.stealth_max_distance", 10.0)
    val detectRange: Double get() = config.getDouble("thief.detect_range", 10.0)

    // Taming settings
    val tameAttemptCooldown: Long get() = config.getLong("taming.attempt_cooldown", 5000)

    // Language settings
    val defaultLanguage: String get() = config.getString("language.default", "en") ?: "en"
    val useClientLocale: Boolean get() = config.getBoolean("language.use_client_locale", true)
    val allowPlayerChangeLanguage: Boolean get() = config.getBoolean("language.allow_player_change", true)

    // Auto-save settings
    val autoSaveInterval: Long get() = config.getLong("auto_save.interval", 300) // seconds
    val autoSaveEnabled: Boolean get() = config.getBoolean("auto_save.enabled", true)

    // Integration
    val notorietyEnabled: Boolean get() = config.getBoolean("integration.notoriety", true)

    // Debug
    val debugMode: Boolean get() = config.getBoolean("debug", false)

    // Economy settings
    val economyEnabled: Boolean get() = config.getBoolean("economy.enabled", true)
    val economyChunkLimitEnabled: Boolean get() = config.getBoolean("economy.chunk_limit.enabled", true)
    val economyChunkRadius: Int get() = config.getInt("economy.chunk_limit.chunk_radius", 5)
    val economyTimeWindowMinutes: Int get() = config.getInt("economy.chunk_limit.time_window_minutes", 60)
    val economyShowOnScoreboard: Boolean get() = config.getBoolean("economy.show_on_scoreboard", true)

    /**
     * Get reward limit for a specific world
     */
    fun getWorldRewardLimit(worldName: String): Double {
        val worldLimits = config.getConfigurationSection("economy.chunk_limit.world_limits")
        return worldLimits?.getDouble(worldName)
            ?: worldLimits?.getDouble("default")
            ?: 500.0
    }

    fun reload() {
        plugin.reloadConfig()
    }
}
