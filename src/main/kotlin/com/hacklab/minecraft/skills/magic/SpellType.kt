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
    PLAYER_OR_SELF, // Target player or self (click player = target, click block/air = self)
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
    val reagents: List<Material>,
    val powerWords: String,      // UO-style incantation
    val baseDamage: Double = 0.0 // Base damage for attack spells
) {
    // Attack spells - higher damage to compensate for reagent cost
    MAGIC_ARROW("Magic Arrow", SpellCircle.FIRST, SpellTargetType.TARGET_LOCATION,
        listOf(Material.STRING), "In Por Ylem", 20.0),
    HARM("Harm", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE), "An Mani", 35.0),
    FIREBALL("Fireball", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.SPIDER_EYE), "Vas Flam", 50.0),
    LIGHTNING("Lightning", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER), "Por Ort Grav", 65.0),
    FIRE_WALL("Fire Wall", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.SPIDER_EYE), "Kal Vas Flam", 40.0),
    ENERGY_BOLT("Energy Bolt", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.BLAZE_POWDER), "Corp Por", 80.0),
    MIND_BLAST("Mind Blast", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.NETHER_WART), "Por Corp Wis", 0.0), // Mana damage, not HP
    EXPLOSION("Explosion", SpellCircle.SIXTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER), "Vas Ort Flam", 90.0),
    METEOR_SWARM("Meteor Swarm", SpellCircle.EIGHTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER),
        "Kal Des Flam Ylem", 130.0),

    // Healing spells (can target self or others)
    HEAL("Heal", SpellCircle.FIRST, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT), "In Mani"),
    CURE("Cure", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.POISONOUS_POTATO), "An Nox"),
    GREATER_HEAL("Greater Heal", SpellCircle.FOURTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.GOLDEN_CARROT), "In Vas Mani"),

    // Buff spells (can target self or others)
    BLESS("Bless", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART), "Rel Sanct"),
    PROTECTION("Protection", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.STRING), "Uus Sanct"),
    INVISIBILITY("Invisibility", SpellCircle.SIXTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.BLAZE_POWDER), "An Lor Xen"),

    // Utility spells
    CREATE_FOOD("Create Food", SpellCircle.FIRST, SpellTargetType.SELF,
        listOf(Material.WHEAT), "In Mani Ylem"),
    NIGHT_SIGHT("Night Sight", SpellCircle.FIRST, SpellTargetType.PLAYER_OR_SELF,
        listOf(Material.GLOWSTONE_DUST), "In Lor"),
    FEATHER_FALL("Feather Fall", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.FEATHER), "Rel Des Por"),
    WATER_BREATHING("Water Breathing", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.PUFFERFISH), "Vas An Ort"),
    TELEPORT("Teleport", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.ENDER_PEARL), "Rel Por"),
    MARK("Mark", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.BLAZE_POWDER), "Kal Por Ylem"),
    RECALL("Recall", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL), "Kal Ort Por"),
    PARALYZE("Paralyze", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.STRING), "An Ex Por"),
    GATE_TRAVEL("Gate Travel", SpellCircle.SEVENTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.ENDER_PEARL, Material.BLAZE_POWDER), "Vas Rel Por");

    val baseMana: Int get() = circle.baseMana
    val difficulty: Int get() = circle.number * 10

    companion object {
        fun fromDisplayName(name: String): SpellType? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }

        fun getByCircle(circle: SpellCircle): List<SpellType> =
            entries.filter { it.circle == circle }
    }
}
