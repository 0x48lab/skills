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
    // ========== 1st Circle ==========
    // Attack
    MAGIC_ARROW("Magic Arrow", SpellCircle.FIRST, SpellTargetType.TARGET_LOCATION,
        listOf(Material.STRING), "In Por Ylem", 20.0),
    // Healing
    HEAL("Heal", SpellCircle.FIRST, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT), "In Mani"),
    // Utility
    CREATE_FOOD("Create Food", SpellCircle.FIRST, SpellTargetType.SELF,
        listOf(Material.WHEAT), "In Mani Ylem"),
    NIGHT_SIGHT("Night Sight", SpellCircle.FIRST, SpellTargetType.PLAYER_OR_SELF,
        listOf(Material.GLOWSTONE_DUST), "In Lor"),
    // Debuff (NEW)
    CLUMSY("Clumsy", SpellCircle.FIRST, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE), "Uus Jux"),
    WEAKEN("Weaken", SpellCircle.FIRST, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE), "Des Mani"),

    // ========== 2nd Circle ==========
    // Attack
    HARM("Harm", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE), "An Mani", 35.0),
    // Healing
    CURE("Cure", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.POISONOUS_POTATO), "An Nox"),
    // Utility
    FEATHER_FALL("Feather Fall", SpellCircle.SECOND, SpellTargetType.TARGET_ENTITY,
        listOf(Material.FEATHER), "Rel Des Por"),
    // Buff (NEW)
    AGILITY("Agility", SpellCircle.SECOND, SpellTargetType.PLAYER_OR_SELF,
        listOf(Material.NETHER_WART), "Ex Uus"),
    STRENGTH("Strength", SpellCircle.SECOND, SpellTargetType.PLAYER_OR_SELF,
        listOf(Material.SPIDER_EYE), "Uus Mani"),
    CUNNING("Cunning", SpellCircle.SECOND, SpellTargetType.PLAYER_OR_SELF,
        listOf(Material.SPIDER_EYE), "Uus Wis"),

    // ========== 3rd Circle ==========
    // Attack
    FIREBALL("Fireball", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.SPIDER_EYE), "Vas Flam", 50.0),
    // Buff
    BLESS("Bless", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART), "Rel Sanct"),
    // Utility
    WATER_BREATHING("Water Breathing", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.PUFFERFISH), "Vas An Ort"),
    TELEPORT("Teleport", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.ENDER_PEARL), "Rel Por"),
    // Debuff (NEW)
    POISON("Poison", SpellCircle.THIRD, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE), "In Nox"),
    // Field (NEW)
    WALL_OF_STONE("Wall of Stone", SpellCircle.THIRD, SpellTargetType.TARGET_LOCATION,
        listOf(Material.NETHER_WART, Material.POISONOUS_POTATO), "In Sanct Ylem"),

    // ========== 4th Circle ==========
    // Attack
    LIGHTNING("Lightning", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER), "Por Ort Grav", 65.0),
    FIRE_WALL("Fire Wall", SpellCircle.FOURTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.SPIDER_EYE), "Kal Vas Flam", 40.0),
    // Healing
    GREATER_HEAL("Greater Heal", SpellCircle.FOURTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GOLDEN_CARROT, Material.GOLDEN_CARROT), "In Vas Mani"),
    // Utility (NEW)
    ARCH_CURE("Arch Cure", SpellCircle.FOURTH, SpellTargetType.AREA,
        listOf(Material.GOLDEN_CARROT, Material.POISONOUS_POTATO), "Vas An Nox"),
    // Debuff (NEW)
    CURSE("Curse", SpellCircle.FOURTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.POISONOUS_POTATO), "Des Sanct"),
    MANA_DRAIN("Mana Drain", SpellCircle.FOURTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.BLAZE_POWDER, Material.ENDER_PEARL), "Ort Rel"),

    // ========== 5th Circle ==========
    // Attack
    ENERGY_BOLT("Energy Bolt", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.BLAZE_POWDER), "Corp Por", 80.0),
    MIND_BLAST("Mind Blast", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.NETHER_WART), "Por Corp Wis", 0.0),
    // Buff
    PROTECTION("Protection", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.STRING), "Uus Sanct"),
    // Utility
    MARK("Mark", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.BLAZE_POWDER), "Kal Por Ylem"),
    RECALL("Recall", SpellCircle.FIFTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL), "Kal Ort Por"),
    PARALYZE("Paralyze", SpellCircle.FIFTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.STRING), "An Ex Por"),
    // Summon (NEW)
    SUMMON_CREATURE("Summon Creature", SpellCircle.FIFTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.NETHER_WART, Material.BLAZE_POWDER), "Kal Xen"),

    // ========== 6th Circle ==========
    // Attack
    EXPLOSION("Explosion", SpellCircle.SIXTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER), "Vas Ort Flam", 90.0),
    // Buff
    INVISIBILITY("Invisibility", SpellCircle.SIXTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.NETHER_WART, Material.BLAZE_POWDER), "An Lor Xen"),
    // Utility (NEW)
    DISPEL("Dispel", SpellCircle.SIXTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.POISONOUS_POTATO, Material.GUNPOWDER), "An Ort"),
    REVEAL("Reveal", SpellCircle.SIXTH, SpellTargetType.AREA,
        listOf(Material.NETHER_WART, Material.GUNPOWDER), "Wis Quas"),
    // Debuff (NEW)
    MASS_CURSE("Mass Curse", SpellCircle.SIXTH, SpellTargetType.AREA,
        listOf(Material.SPIDER_EYE, Material.BLAZE_POWDER), "Vas Des Sanct"),
    // Field (NEW)
    PARALYZE_FIELD("Paralyze Field", SpellCircle.SIXTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.ENDER_PEARL, Material.GOLDEN_CARROT), "In Ex Grav"),

    // ========== 7th Circle ==========
    // Utility
    GATE_TRAVEL("Gate Travel", SpellCircle.SEVENTH, SpellTargetType.TARGET_ITEM,
        listOf(Material.ENDER_PEARL, Material.ENDER_PEARL, Material.BLAZE_POWDER), "Vas Rel Por"),
    // Attack (NEW)
    CHAIN_LIGHTNING("Chain Lightning", SpellCircle.SEVENTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.ENDER_PEARL, Material.GUNPOWDER, Material.BLAZE_POWDER), "Vas Ort Grav", 90.0),
    FLAMESTRIKE("Flamestrike", SpellCircle.SEVENTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.GUNPOWDER, Material.STRING), "Kal Vas Flam", 85.0),
    // Field (NEW)
    ENERGY_FIELD("Energy Field", SpellCircle.SEVENTH, SpellTargetType.TARGET_LOCATION,
        listOf(Material.ENDER_PEARL, Material.BLAZE_POWDER, Material.STRING), "In Sanct Grav"),
    // Utility (NEW)
    MANA_VAMPIRE("Mana Vampire", SpellCircle.SEVENTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.ENDER_PEARL, Material.NETHER_WART, Material.BLAZE_POWDER), "Ort Sanct"),
    MASS_DISPEL("Mass Dispel", SpellCircle.SEVENTH, SpellTargetType.AREA,
        listOf(Material.ENDER_PEARL, Material.POISONOUS_POTATO, Material.BLAZE_POWDER), "Vas An Ort"),

    // ========== 8th Circle ==========
    // Attack
    METEOR_SWARM("Meteor Swarm", SpellCircle.EIGHTH, SpellTargetType.AREA,
        listOf(Material.GUNPOWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER, Material.BLAZE_POWDER),
        "Kal Des Flam Ylem", 130.0),
    EARTHQUAKE("Earthquake", SpellCircle.EIGHTH, SpellTargetType.AREA,
        listOf(Material.NETHER_WART, Material.GOLDEN_CARROT, Material.BLAZE_POWDER, Material.GUNPOWDER),
        "In Vas Por", 100.0),
    // Special (NEW)
    WORD_OF_DEATH("Word of Death", SpellCircle.EIGHTH, SpellTargetType.TARGET_ENTITY,
        listOf(Material.SPIDER_EYE, Material.GUNPOWDER, Material.BLAZE_POWDER, Material.NETHER_WART),
        "Kal Vas An Mani"),
    MASS_SLEEP("Mass Sleep", SpellCircle.EIGHTH, SpellTargetType.AREA,
        listOf(Material.NETHER_WART, Material.SPIDER_EYE, Material.STRING, Material.BLAZE_POWDER),
        "Vas Zu");

    val baseMana: Int get() = circle.baseMana
    val difficulty: Int get() = circle.number * 10

    companion object {
        // Cache for Power Words lookup (lowercase key -> SpellType, lower circle priority)
        private val powerWordsMap: Map<String, SpellType> by lazy {
            val map = mutableMapOf<String, SpellType>()
            // Sort by circle number to ensure lower circle spells have priority
            entries.sortedBy { it.circle.number }.forEach { spell ->
                val key = spell.powerWords.lowercase()
                // Only add if not already present (lower circle wins)
                if (!map.containsKey(key)) {
                    map[key] = spell
                }
            }
            map
        }

        fun fromDisplayName(name: String): SpellType? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }

        fun getByCircle(circle: SpellCircle): List<SpellType> =
            entries.filter { it.circle == circle }

        /**
         * Find spell by Power Words (case-insensitive)
         * When duplicates exist (e.g., "Kal Vas Flam"), lower circle spell has priority
         */
        fun fromPowerWords(words: String): SpellType? =
            powerWordsMap[words.lowercase()]

        /**
         * Get all Power Words for tab completion
         */
        fun getAllPowerWords(): List<String> =
            entries.map { it.powerWords }
    }
}
