package com.hacklab.minecraft.skills.skill

enum class SkillType(
    val displayName: String,
    val category: SkillCategory,
    val strWeight: Double = 0.0,
    val dexWeight: Double = 0.0,
    val intWeight: Double = 0.0
) {
    // Combat - Weapons
    SWORDSMANSHIP("Swordsmanship", SkillCategory.COMBAT, strWeight = 1.0),
    AXE("Axe", SkillCategory.COMBAT, strWeight = 1.0),
    MACE_FIGHTING("Mace Fighting", SkillCategory.COMBAT, strWeight = 1.0),
    ARCHERY("Archery", SkillCategory.COMBAT, dexWeight = 1.0),
    THROWING("Throwing", SkillCategory.COMBAT, dexWeight = 1.0),
    WRESTLING("Wrestling", SkillCategory.COMBAT, strWeight = 1.0),

    // Combat - Support
    TACTICS("Tactics", SkillCategory.COMBAT, strWeight = 1.0),
    ANATOMY("Anatomy", SkillCategory.COMBAT, strWeight = 1.0),
    PARRYING("Parrying", SkillCategory.COMBAT, dexWeight = 1.0),
    FOCUS("Focus", SkillCategory.COMBAT, dexWeight = 1.0),

    // Magic
    MAGERY("Magery", SkillCategory.MAGIC, intWeight = 1.0),
    EVALUATING_INTELLIGENCE("Evaluating Intelligence", SkillCategory.MAGIC, intWeight = 1.0),
    MEDITATION("Meditation", SkillCategory.MAGIC, intWeight = 1.0),
    RESISTING_SPELLS("Resisting Spells", SkillCategory.MAGIC, intWeight = 1.0),

    // Crafting
    ALCHEMY("Alchemy", SkillCategory.CRAFTING, intWeight = 1.0),
    BLACKSMITHY("Blacksmithy", SkillCategory.CRAFTING, strWeight = 1.0),
    BOWCRAFT("Bowcraft", SkillCategory.CRAFTING, intWeight = 1.0),
    CRAFTSMANSHIP("Craftsmanship", SkillCategory.CRAFTING, strWeight = 1.0),
    COOKING("Cooking", SkillCategory.CRAFTING, strWeight = 1.0),
    INSCRIPTION("Inscription", SkillCategory.CRAFTING, intWeight = 1.0),
    TINKERING("Tinkering", SkillCategory.CRAFTING, dexWeight = 1.0),

    // Gathering
    MINING("Mining", SkillCategory.GATHERING, strWeight = 1.0),
    LUMBERJACKING("Lumberjacking", SkillCategory.GATHERING, strWeight = 1.0),
    FISHING("Fishing", SkillCategory.GATHERING, dexWeight = 1.0),
    FARMING("Farming", SkillCategory.GATHERING, dexWeight = 1.0),

    // Thief
    HIDING("Hiding", SkillCategory.THIEF, dexWeight = 1.0),
    STEALTH("Stealth", SkillCategory.THIEF, dexWeight = 1.0),
    DETECTING_HIDDEN("Detecting Hidden", SkillCategory.THIEF, dexWeight = 0.5, intWeight = 0.5),
    SNOOPING("Snooping", SkillCategory.THIEF, dexWeight = 1.0),
    STEALING("Stealing", SkillCategory.THIEF, dexWeight = 1.0),
    POISONING("Poisoning", SkillCategory.THIEF, dexWeight = 1.0),

    // Taming
    ANIMAL_TAMING("Animal Taming", SkillCategory.TAMING, strWeight = 0.5, dexWeight = 0.5),
    ANIMAL_LORE("Animal Lore", SkillCategory.TAMING, intWeight = 1.0),
    VETERINARY("Veterinary", SkillCategory.TAMING, strWeight = 0.5, intWeight = 0.5),

    // Survival
    ATHLETICS("Athletics", SkillCategory.SURVIVAL, dexWeight = 1.0),
    SWIMMING("Swimming", SkillCategory.SURVIVAL, dexWeight = 1.0),
    HEAT_RESISTANCE("Heat Resistance", SkillCategory.SURVIVAL, strWeight = 1.0),
    COLD_RESISTANCE("Cold Resistance", SkillCategory.SURVIVAL, strWeight = 1.0),
    ENDURANCE("Endurance", SkillCategory.SURVIVAL, strWeight = 1.0),

    // Other
    ARMS_LORE("Arms Lore", SkillCategory.OTHER, strWeight = 0.33, dexWeight = 0.33, intWeight = 0.33);

    companion object {
        const val MAX_SKILL_VALUE = 100.0
        const val TOTAL_SKILL_CAP = 700.0
        const val SKILL_GAIN_AMOUNT = 0.1

        fun fromDisplayName(name: String): SkillType? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}

enum class SkillCategory(val displayName: String) {
    COMBAT("Combat"),
    MAGIC("Magic"),
    CRAFTING("Crafting"),
    GATHERING("Gathering"),
    THIEF("Thief"),
    TAMING("Taming"),
    SURVIVAL("Survival"),
    OTHER("Other")
}
