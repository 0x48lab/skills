package com.hacklab.minecraft.skills.vengeful

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.EntityType

/**
 * VengefulMobs設定管理
 * config.ymlからvengeful_mobs設定を読み込む
 */
class VengefulMobsConfig(private val plugin: Skills) {

    // 機能の有効/無効
    val enabled: Boolean
        get() = plugin.config.getBoolean("vengeful_mobs.enabled", false)

    // テイム済みMobを除外するか
    val excludeTamed: Boolean
        get() = plugin.config.getBoolean("vengeful_mobs.exclude_tamed", true)

    // 怒り状態の持続時間（ミリ秒）- 攻撃を受けるたびにリセット
    val angerDuration: Long
        get() = plugin.config.getLong("vengeful_mobs.anger_duration", 30000)

    // 追跡をやめる距離（ブロック）- この距離離れると諦める
    val giveUpDistance: Double
        get() = plugin.config.getDouble("vengeful_mobs.give_up_distance", 32.0)

    // 追跡中に期限を延長するかどうか
    val extendAngerWhileChasing: Boolean
        get() = plugin.config.getBoolean("vengeful_mobs.extend_anger_while_chasing", true)

    // 有効なMobタイプのセット
    private var enabledMobs: Set<EntityType> = emptySet()

    // Mob別のオーバーライド設定
    private var mobOverrides: Map<EntityType, MobConfig> = emptyMap()

    // デフォルト設定
    private var defaultConfig: MobConfig = MobConfig(
        entityType = EntityType.PIG, // dummy
        damage = 2.0,
        mode = AggressionMode.RETALIATE,
        speed = 1.0,
        attackRange = 2.0
    )

    init {
        reload()
    }

    /**
     * 設定を再読み込み
     */
    fun reload() {
        loadEnabledMobs()
        loadDefaultConfig()
        loadOverrides()
    }

    private fun loadEnabledMobs() {
        val list = plugin.config.getStringList("vengeful_mobs.enabled_mobs")
        enabledMobs = list.mapNotNull { name ->
            try {
                EntityType.valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid entity type in vengeful_mobs.enabled_mobs: $name")
                null
            }
        }.toSet()
    }

    private fun loadDefaultConfig() {
        val section = plugin.config.getConfigurationSection("vengeful_mobs.default") ?: return

        val damage = section.getDouble("damage", 2.0)
        val modeStr = section.getString("mode", "RETALIATE") ?: "RETALIATE"
        val mode = AggressionMode.fromString(modeStr) ?: AggressionMode.RETALIATE
        val speed = section.getDouble("speed", 1.0)
        val attackRange = section.getDouble("attack_range", 2.0)

        defaultConfig = MobConfig(
            entityType = EntityType.PIG, // dummy
            damage = damage,
            mode = mode,
            speed = speed,
            attackRange = attackRange
        )
    }

    private fun loadOverrides() {
        val section = plugin.config.getConfigurationSection("vengeful_mobs.overrides") ?: return

        val overrides = mutableMapOf<EntityType, MobConfig>()

        for (key in section.getKeys(false)) {
            val entityType = try {
                EntityType.valueOf(key.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid entity type in vengeful_mobs.overrides: $key")
                continue
            }

            val mobSection = section.getConfigurationSection(key) ?: continue

            val damage = mobSection.getDouble("damage", defaultConfig.damage)
            val modeStr = mobSection.getString("mode", defaultConfig.mode.name) ?: defaultConfig.mode.name
            val mode = AggressionMode.fromString(modeStr) ?: defaultConfig.mode
            val speed = mobSection.getDouble("speed", defaultConfig.speed)
            val attackRange = mobSection.getDouble("attack_range", defaultConfig.attackRange)

            overrides[entityType] = MobConfig(
                entityType = entityType,
                damage = damage,
                mode = mode,
                speed = speed,
                attackRange = attackRange
            )
        }

        mobOverrides = overrides
    }

    /**
     * 指定したMobタイプが有効かどうか
     */
    fun isEnabled(entityType: EntityType): Boolean {
        return enabled && entityType in enabledMobs
    }

    /**
     * 指定したMobタイプの設定を取得
     */
    fun getConfig(entityType: EntityType): MobConfig {
        return mobOverrides[entityType] ?: defaultConfig.copy(entityType = entityType)
    }

    /**
     * 有効なMob一覧を取得
     */
    fun getEnabledMobs(): Set<EntityType> = enabledMobs
}
