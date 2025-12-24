package com.hacklab.minecraft.skills.data

import com.hacklab.minecraft.skills.skill.SkillType

data class SkillData(
    val type: SkillType,
    var value: Double = 0.0,
    var lastUsed: Long = System.currentTimeMillis()
) {
    init {
        require(value in 0.0..SkillType.MAX_SKILL_VALUE) {
            "Skill value must be between 0 and ${SkillType.MAX_SKILL_VALUE}"
        }
    }

    fun addValue(amount: Double): Boolean {
        val newValue = (value + amount).coerceIn(0.0, SkillType.MAX_SKILL_VALUE)
        if (newValue != value) {
            value = newValue
            return true
        }
        return false
    }

    fun updateLastUsed() {
        lastUsed = System.currentTimeMillis()
    }

    fun copy(): SkillData = SkillData(type, value, lastUsed)
}
