package com.hacklab.minecraft.skills.vengeful

import org.bukkit.entity.EntityType

/**
 * 個別Mobの反撃設定
 */
data class MobConfig(
    val entityType: EntityType,
    val damage: Double,
    val mode: AggressionMode,
    val speed: Double = 1.0,
    val attackRange: Double = 2.0
) {
    companion object {
        fun default(entityType: EntityType): MobConfig {
            return MobConfig(
                entityType = entityType,
                damage = 2.0,
                mode = AggressionMode.RETALIATE,
                speed = 1.0,
                attackRange = 2.0
            )
        }
    }
}
