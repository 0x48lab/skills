package com.hacklab.minecraft.skills.loot

import org.bukkit.entity.EntityType

/**
 * Magic-related mobs that can drop spell scrolls
 */
enum class MagicMob(
    val entityType: EntityType,
    val minCircle: Int,
    val maxCircle: Int,
    val dropRate: Double  // 0.002 = 0.2%
) {
    WITCH(EntityType.WITCH, 1, 3, 0.002),           // 0.2%
    BLAZE(EntityType.BLAZE, 2, 4, 0.002),           // 0.2%
    PHANTOM(EntityType.PHANTOM, 1, 2, 0.0015),      // 0.15%
    SHULKER(EntityType.SHULKER, 3, 5, 0.0025),      // 0.25%
    EVOKER(EntityType.EVOKER, 4, 6, 0.003),         // 0.3%
    ILLUSIONER(EntityType.ILLUSIONER, 5, 6, 0.003); // 0.3%

    companion object {
        private val entityTypeMap = entries.associateBy { it.entityType }

        fun fromEntityType(type: EntityType): MagicMob? = entityTypeMap[type]

        fun isMagicMob(type: EntityType): Boolean = entityTypeMap.containsKey(type)
    }
}
