package com.hacklab.minecraft.skills.skill

enum class StatType(val displayName: String) {
    STR("Strength"),
    DEX("Dexterity"),
    INT("Intelligence");

    companion object {
        const val MAX_STAT_VALUE = 100
        const val MIN_STAT_VALUE = 10
        const val TOTAL_STAT_CAP = 225
        const val DEFAULT_STAT_VALUE = 25  // Starting value for each stat (25*3 = 75 total)
    }
}

/**
 * Stat lock mode (UO-style)
 * - UP: Stat can increase, will decrease when others need room
 * - DOWN: Stat can decrease, cannot increase
 * - LOCKED: Stat cannot change at all
 */
enum class StatLockMode {
    UP,      // Can go up (default)
    DOWN,    // Can go down only
    LOCKED   // Cannot change
}
