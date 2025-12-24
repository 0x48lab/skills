package com.hacklab.minecraft.skills.data

import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class PlayerData(
    val uuid: UUID,
    var playerName: String,
    private val skills: MutableMap<SkillType, SkillData> = ConcurrentHashMap(),
    var internalHp: Double = 100.0,
    var maxInternalHp: Double = 100.0,
    var mana: Double = 20.0,
    var maxMana: Double = 20.0,
    var language: Language = Language.ENGLISH,
    var dirty: Boolean = false
) {
    init {
        // Initialize all skills with default values if not present
        SkillType.entries.forEach { skillType ->
            skills.putIfAbsent(skillType, SkillData(skillType))
        }
    }

    // Skill operations
    fun getSkill(skillType: SkillType): SkillData =
        skills.getOrPut(skillType) { SkillData(skillType) }

    fun getSkillValue(skillType: SkillType): Double = getSkill(skillType).value

    fun setSkillValue(skillType: SkillType, value: Double) {
        getSkill(skillType).value = value.coerceIn(0.0, SkillType.MAX_SKILL_VALUE)
        dirty = true
    }

    fun getTotalSkillPoints(): Double = skills.values.sumOf { it.value }

    fun getAllSkills(): Map<SkillType, SkillData> = skills.toMap()

    // Find the least recently used skill (for skill decrease)
    fun getLeastRecentlyUsedSkill(exclude: SkillType? = null): SkillType? {
        return skills.entries
            .filter { it.key != exclude && it.value.value > 0 }
            .minByOrNull { it.value.lastUsed }
            ?.key
    }

    // Stats calculation
    fun calculateStat(statType: StatType): Int {
        var totalWeight = 0.0
        var weightedSum = 0.0

        SkillType.entries.forEach { skill ->
            val weight = when (statType) {
                StatType.STR -> skill.strWeight
                StatType.DEX -> skill.dexWeight
                StatType.INT -> skill.intWeight
            }
            if (weight > 0) {
                weightedSum += getSkillValue(skill) * weight
                totalWeight += weight
            }
        }

        return if (totalWeight > 0) {
            (weightedSum / totalWeight).toInt().coerceIn(0, StatType.MAX_STAT_VALUE)
        } else 0
    }

    fun getStr(): Int = calculateStat(StatType.STR)
    fun getDex(): Int = calculateStat(StatType.DEX)
    fun getInt(): Int = calculateStat(StatType.INT)

    // HP/Mana operations
    fun calculateMaxHp(): Double = 100.0 + getStr()
    fun calculateMaxMana(): Double = 20.0

    fun updateMaxStats() {
        maxInternalHp = calculateMaxHp()
        maxMana = calculateMaxMana()
        // Clamp current values
        internalHp = internalHp.coerceIn(0.0, maxInternalHp)
        mana = mana.coerceIn(0.0, maxMana)
    }

    fun damage(amount: Double): Double {
        val actualDamage = amount.coerceAtMost(internalHp)
        internalHp = (internalHp - actualDamage).coerceAtLeast(0.0)
        dirty = true
        return actualDamage
    }

    fun heal(amount: Double): Double {
        val actualHeal = amount.coerceAtMost(maxInternalHp - internalHp)
        internalHp = (internalHp + actualHeal).coerceAtMost(maxInternalHp)
        dirty = true
        return actualHeal
    }

    fun useMana(amount: Double): Boolean {
        val intReduction = getInt() / 200.0  // INT 100 = 50% reduction
        val actualCost = amount * (1 - intReduction)
        if (mana >= actualCost) {
            mana -= actualCost
            dirty = true
            return true
        }
        return false
    }

    fun restoreMana(amount: Double): Double {
        val actualRestore = amount.coerceAtMost(maxMana - mana)
        mana = (mana + actualRestore).coerceAtMost(maxMana)
        dirty = true
        return actualRestore
    }

    fun getHpPercentage(): Double = if (maxInternalHp > 0) internalHp / maxInternalHp else 0.0
    fun getManaPercentage(): Double = if (maxMana > 0) mana / maxMana else 0.0

    fun markClean() {
        dirty = false
    }
}
