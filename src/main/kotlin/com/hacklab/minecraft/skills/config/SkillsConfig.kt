package com.hacklab.minecraft.skills.config

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.scoreboard.ConflictMode
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
    val reagentSearchShulkerBoxes: Boolean get() = config.getBoolean("magic.reagent_search.search_shulker_boxes", true)
    val reagentSearchBundles: Boolean get() = config.getBoolean("magic.reagent_search.search_bundles", true)

    // Thief settings
    val hideMovementBreaks: Boolean get() = config.getBoolean("thief.hide_movement_breaks", false)
    val stealthMaxDistance: Double get() = config.getDouble("thief.stealth_max_distance", 10.0)
    val detectRange: Double get() = config.getDouble("thief.detect_range", 10.0)
    val stealthDistanceDivisor: Double get() = config.getDouble("thief.stealth_distance_divisor", 5.0)
    val hideTimeoutBase: Int get() = config.getInt("thief.hide_timeout_base", 30)
    val hideTimeoutMax: Int get() = config.getInt("thief.hide_timeout_max", 300)
    val hideTimeoutWarning: Int get() = config.getInt("thief.hide_timeout_warning", 10)
    val equipmentStealPenalty: Int get() = config.getInt("thief.equipment_steal_penalty", 20)

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

    // Sleep settings
    val sleepEnabled: Boolean get() = config.getBoolean("sleep.enabled", true)

    // Bed Rest Recovery settings
    val bedRestRecoveryEnabled: Boolean get() = config.getBoolean("bed_rest_recovery.enabled", true)

    // Economy settings
    val economyEnabled: Boolean get() = config.getBoolean("economy.enabled", true)
    val economyChunkLimitEnabled: Boolean get() = config.getBoolean("economy.chunk_limit.enabled", true)
    val economyChunkRadius: Int get() = config.getInt("economy.chunk_limit.chunk_radius", 5)
    val economyTimeWindowMinutes: Int get() = config.getInt("economy.chunk_limit.time_window_minutes", 60)
    val economyShowOnScoreboard: Boolean get() = config.getBoolean("economy.show_on_scoreboard", true)

    // Chunk Mob Limit settings
    val chunkMobLimitEnabled: Boolean get() = config.getBoolean("chunk_mob_limit.enabled", true)
    val chunkMobLimitPassive: Int get() = config.getInt("chunk_mob_limit.limits.passive", 24)
    val chunkMobLimitHostile: Int get() = config.getInt("chunk_mob_limit.limits.hostile", 32)
    val chunkMobLimitAmbient: Int get() = config.getInt("chunk_mob_limit.limits.ambient", 8)
    val chunkMobLimitWaterCreature: Int get() = config.getInt("chunk_mob_limit.limits.water_creature", 8)
    val chunkMobLimitWaterAmbient: Int get() = config.getInt("chunk_mob_limit.limits.water_ambient", 16)
    val chunkMobLimitCheckInterval: Long get() = config.getLong("chunk_mob_limit.check_interval_ticks", 100)
    val chunkMobLimitNotify: Boolean get() = config.getBoolean("chunk_mob_limit.notify_on_limit", true)

    // Ender Dragon Scaling
    val enderDragonScalingEnabled: Boolean get() = config.getBoolean("ender_dragon_scaling.enabled", true)
    val enderDragonRespawnIntervalHours: Int get() = config.getInt("ender_dragon_scaling.respawn_interval_hours", 24)
    val enderDragonHpPerKill: Int get() = config.getInt("ender_dragon_scaling.hp_per_kill", 50)
    val enderDragonMaxHp: Int get() = config.getInt("ender_dragon_scaling.max_hp", 1000)
    val enderDragonDamageScalePerKill: Double get() = config.getDouble("ender_dragon_scaling.damage_scale_per_kill", 0.15)
    val enderDragonMaxDamageScale: Double get() = config.getDouble("ender_dragon_scaling.max_damage_scale", 4.0)
    val enderDragonXpScalePerKill: Double get() = config.getDouble("ender_dragon_scaling.xp_scale_per_kill", 0.25)
    val enderDragonPreRespawnMinutes: List<Int> get() = config.getIntegerList("ender_dragon_scaling.announce_pre_respawn_minutes").ifEmpty { listOf(5, 1) }

    // Scoreboard settings
    val scoreboardEnabled: Boolean get() = config.getBoolean("scoreboard.enabled", true)
    val scoreboardUpdateInterval: Long get() = config.getLong("scoreboard.update_interval_ticks", 20)
    val scoreboardConflictMode: ConflictMode get() =
        ConflictMode.fromString(config.getString("scoreboard.conflict_mode", "RESPECT") ?: "RESPECT")
    val scoreboardShowByDefault: Boolean get() = config.getBoolean("scoreboard.show_by_default", true)
    val scoreboardAllowToggle: Boolean get() = config.getBoolean("scoreboard.allow_player_toggle", true)

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
