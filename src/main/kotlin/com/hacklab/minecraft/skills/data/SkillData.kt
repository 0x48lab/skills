package com.hacklab.minecraft.skills.data

import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType

data class SkillData(
    val type: SkillType,
    var value: Double = 0.0,
    var lastUsed: Long = System.currentTimeMillis(),
    var lockMode: SkillLockMode = SkillLockMode.UP
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

    fun canIncrease(): Boolean = lockMode == SkillLockMode.UP && value < SkillType.MAX_SKILL_VALUE

    fun canDecrease(): Boolean = lockMode != SkillLockMode.LOCKED && value > 0

    fun copy(): SkillData = SkillData(type, value, lastUsed, lockMode)
}
