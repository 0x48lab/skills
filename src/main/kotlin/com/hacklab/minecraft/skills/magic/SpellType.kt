package com.hacklab.minecraft.skills.magic

import org.bukkit.Material

enum class SpellCircle(val number: Int, val baseMana: Int) {
    FIRST(1, 1),
    SECOND(2, 2),
    THIRD(3, 3),
    FOURTH(4, 4),
    FIFTH(5, 5),
    SIXTH(6, 6),
    SEVENTH(7, 7),
    EIGHTH(8, 8)
}

enum class SpellTargetType {
    SELF,           // Self only (instant cast)
    TARGET_ENTITY,  // Target entity (player or mob)
    TARGET_LOCATION,// Target location
    TARGET_ITEM,    // Target item in hand
    AREA,           // Area effect around target location
    NONE            // No target needed
}

enum class SpellType(
    val displayName: String,
    val circle: SpellCircle,
    val targetType: SpellTargetType,
    val reagents: List<Material>
) {
    // Attack spells
    MAGIC_ARROW("Magic Arrow", SpellCircle.FIRST, SpellTargetType.TARGET_LOCATION,
        listOf(Material.STRING)),
    HARM("Harm", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE)),
    FIREBALL("Fireball", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.SPIDER_EYE)),
    LIGHTNING("Lightning", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER)),
    FIRE_WALL("Fire Wall", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.SPIDER_EYE)),
    MIND_BLAST("Mind Blast", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.BLAZE_POWDER)),
    EXPLOSION("Explosion", SpellCircle.SIXTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER)),
    METEOR_SWARM("Meteor Swarm", SpellCircle.EIGHTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER)),

    // Healing spells (can target self or others)
    HEAL("Heal", SpellCircle.FIRST, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT)),
    CURE("Cure", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.POISONOUS_POTATO)),
    GREATER_HEAL("Greater Heal", SpellCircle.FOURTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.GOLDEN_CARROT)),

    // Buff spells (can target self or others)
    BLESS("Bless", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART)),
    PROTECTION("Protection", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.STRING)),
    INVISIBILITY("Invisibility", SpellCircle.SIXTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.BLAZE_POWDER)),

    // Utility spells
    CREATE_FOOD("Create Food", SpellCircle.FIRST, SpellTargetType.SELF,
        listOf(Material.WHEAT)),
    NIGHT_SIGHT("Night Sight", SpellCircle.FIRST, SpellTargetType.SELF,
        listOf(Material.GLOWSTONE_DUST)),
    FEATHER_FALL("Feather Fall", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.FEATHER)),
    WATER_BREATHING("Water Breathing", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.PUFFERFISH)),
    TELEPORT("Teleport", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.ENDER_PEARL)),
    MARK("Mark", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.BLAZE_POWDER)),
    RECALL("Recall", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL)),
    PARALYZE("Paralyze", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.STRING)),
    GATE_TRAVEL("Gate Travel", SpellCircle.SEVENTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.ENDER_PEARL, Material.BLAZE_POWDER));

    val baseMana: Int get() = circle.baseMana
    val difficulty: Int get() = circle.number * 10

    companion object {
        fun fromDisplayName(name: String): SpellType? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }

        fun getByCircle(circle: SpellCircle): List<SpellType> =
            entries.filter { it.circle == circle }
    }
}
