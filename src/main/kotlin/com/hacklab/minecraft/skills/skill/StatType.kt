package com.hacklab.minecraft.skills.skill

enum class StatType(val displayName: String) {
    STR("Strength"),
    DEX("Dexterity"),
    INT("Intelligence");

    companion object {
        const val MAX_STAT_VALUE = 100
    }
}
