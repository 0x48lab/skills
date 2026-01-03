package com.hacklab.minecraft.skills.data

import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.skill.SkillLockMode
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatLockMode
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
    var stamina: Double = 100.0,
    var maxStamina: Double = 100.0,
    var language: Language? = null,
    var dirty: Boolean = false,
    // UO-style independent stats
    var str: Int = StatType.DEFAULT_STAT_VALUE,
    var dex: Int = StatType.DEFAULT_STAT_VALUE,
    var int: Int = StatType.DEFAULT_STAT_VALUE,
    // Stat lock modes
    var strLock: StatLockMode = StatLockMode.UP,
    var dexLock: StatLockMode = StatLockMode.UP,
    var intLock: StatLockMode = StatLockMode.UP
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

    // Find the skill to decrease (for skill seesaw)
    // Priority: DOWN mode skills first, then UP mode, never LOCKED
    fun getSkillToDecrease(exclude: SkillType? = null): SkillType? {
        // First try skills set to DOWN mode (least recently used)
        val downModeSkill = skills.entries
            .filter { it.key != exclude && it.value.value > 0 && it.value.lockMode == SkillLockMode.DOWN }
            .minByOrNull { it.value.lastUsed }
            ?.key
        if (downModeSkill != null) return downModeSkill

        // Then try skills set to UP mode (least recently used)
        return skills.entries
            .filter { it.key != exclude && it.value.value > 0 && it.value.lockMode == SkillLockMode.UP }
            .minByOrNull { it.value.lastUsed }
            ?.key
        // LOCKED skills are never decreased
    }

    // Skill lock operations
    fun getSkillLock(skillType: SkillType): SkillLockMode = getSkill(skillType).lockMode

    fun setSkillLock(skillType: SkillType, mode: SkillLockMode) {
        getSkill(skillType).lockMode = mode
        dirty = true
    }

    fun toggleSkillLock(skillType: SkillType): SkillLockMode {
        val skill = getSkill(skillType)
        skill.lockMode = skill.lockMode.next()
        dirty = true
        return skill.lockMode
    }

    fun canSkillIncrease(skillType: SkillType): Boolean = getSkill(skillType).canIncrease()

    fun canSkillDecrease(skillType: SkillType): Boolean = getSkill(skillType).canDecrease()

    // Stats - UO style (independent values)
    // Properties str, dex, int are accessed directly (Kotlin generates getters automatically)

    fun getStat(statType: StatType): Int = when (statType) {
        StatType.STR -> str
        StatType.DEX -> dex
        StatType.INT -> int
    }

    fun setStat(statType: StatType, value: Int) {
        val clamped = value.coerceIn(StatType.MIN_STAT_VALUE, StatType.MAX_STAT_VALUE)
        when (statType) {
            StatType.STR -> str = clamped
            StatType.DEX -> dex = clamped
            StatType.INT -> int = clamped
        }
        dirty = true
    }

    fun getStatLock(statType: StatType): StatLockMode = when (statType) {
        StatType.STR -> strLock
        StatType.DEX -> dexLock
        StatType.INT -> intLock
    }

    fun setStatLock(statType: StatType, mode: StatLockMode) {
        when (statType) {
            StatType.STR -> strLock = mode
            StatType.DEX -> dexLock = mode
            StatType.INT -> intLock = mode
        }
        dirty = true
    }

    fun getTotalStats(): Int = str + dex + int

    /**
     * Try to gain a stat point (UO-style with seesaw)
     * @return true if stat was gained
     */
    fun tryGainStat(statType: StatType): Boolean {
        val lock = getStatLock(statType)
        val currentValue = getStat(statType)

        // Cannot gain if locked or set to DOWN
        if (lock != StatLockMode.UP) return false

        // Cannot exceed max
        if (currentValue >= StatType.MAX_STAT_VALUE) return false

        val totalStats = getTotalStats()

        // If at cap, need to decrease another stat
        if (totalStats >= StatType.TOTAL_STAT_CAP) {
            val decreased = decreaseOtherStat(statType)
            if (!decreased) return false
        }

        // Increase the stat
        setStat(statType, currentValue + 1)
        return true
    }

    /**
     * Decrease another stat to make room (seesaw effect)
     * Prioritizes stats set to DOWN mode, then UP mode
     */
    private fun decreaseOtherStat(exclude: StatType): Boolean {
        // First try stats set to DOWN
        for (stat in StatType.entries) {
            if (stat == exclude) continue
            if (getStatLock(stat) == StatLockMode.DOWN && getStat(stat) > StatType.MIN_STAT_VALUE) {
                setStat(stat, getStat(stat) - 1)
                return true
            }
        }

        // Then try stats set to UP (not locked)
        for (stat in StatType.entries) {
            if (stat == exclude) continue
            if (getStatLock(stat) == StatLockMode.UP && getStat(stat) > StatType.MIN_STAT_VALUE) {
                setStat(stat, getStat(stat) - 1)
                return true
            }
        }

        // All other stats are locked or at minimum
        return false
    }

    // HP/Mana/Stamina operations
    fun calculateMaxHp(): Double = 100.0 + str
    fun calculateMaxMana(): Double = 20.0
    fun calculateMaxStamina(): Double {
        val focusSkill = getSkillValue(SkillType.FOCUS)
        return 100.0 + dex + (focusSkill / 2.0)
    }

    fun updateMaxStats() {
        maxInternalHp = calculateMaxHp()
        maxMana = calculateMaxMana()
        maxStamina = calculateMaxStamina()
        // Clamp current values
        internalHp = internalHp.coerceIn(0.0, maxInternalHp)
        mana = mana.coerceIn(0.0, maxMana)
        stamina = stamina.coerceIn(0.0, maxStamina)
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
        val intReduction = int / 200.0  // INT 100 = 50% reduction
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

    /**
     * Consume stamina (for sprinting)
     * @param amount Base stamina cost
     * @return true if stamina was consumed, false if not enough
     */
    fun consumeStamina(amount: Double): Boolean {
        val focusSkill = getSkillValue(SkillType.FOCUS)
        val reduction = focusSkill / 200.0  // Focus 100 = 50% reduction
        val actualCost = amount * (1 - reduction)
        if (stamina >= actualCost) {
            stamina -= actualCost
            dirty = true
            return true
        }
        return false
    }

    /**
     * Restore stamina (when not sprinting)
     * @param amount Base restore amount
     * @return actual amount restored
     */
    fun restoreStamina(amount: Double): Double {
        val focusSkill = getSkillValue(SkillType.FOCUS)
        val bonus = 1.0 + (focusSkill / 100.0)  // Focus 100 = +100% regen
        val boostedAmount = amount * bonus
        val actualRestore = boostedAmount.coerceAtMost(maxStamina - stamina)
        stamina = (stamina + actualRestore).coerceAtMost(maxStamina)
        dirty = true
        return actualRestore
    }

    fun hasStamina(): Boolean = stamina > 0

    fun getHpPercentage(): Double = if (maxInternalHp > 0) internalHp / maxInternalHp else 0.0
    fun getManaPercentage(): Double = if (maxMana > 0) mana / maxMana else 0.0
    fun getStaminaPercentage(): Double = if (maxStamina > 0) stamina / maxStamina else 0.0

    fun markClean() {
        dirty = false
    }
}
