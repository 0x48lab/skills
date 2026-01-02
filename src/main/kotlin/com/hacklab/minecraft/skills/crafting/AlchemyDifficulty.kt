package com.hacklab.minecraft.skills.crafting

import org.bukkit.Material
import org.bukkit.potion.PotionType

/**
 * Alchemy difficulty table for all potions
 * Based on CLAUDE.md specification
 *
 * Difficulty tiers:
 * - Base potions (Awkward, Mundane, Thick): 15-20
 * - Basic effect potions (Level I): 30
 * - Extended potions (+duration): 45
 * - Enhanced potions (Level II): 50
 * - Complex potions (multiple effects): 55-60
 * - Splash modifier: +10
 * - Lingering modifier: +20
 */
object AlchemyDifficulty {

    data class PotionInfo(
        val baseDifficulty: Int,
        val canExtend: Boolean = true,
        val canEnhance: Boolean = true,
        val baseSeconds: Int = 180  // Default 3 minutes
    )

    // Base difficulties for each potion type
    private val potionInfoMap: Map<PotionType, PotionInfo> = buildMap {
        // ========================================
        // Base potions (difficulty 15-20)
        // ========================================
        put(PotionType.WATER, PotionInfo(0, canExtend = false, canEnhance = false, baseSeconds = 0))
        put(PotionType.MUNDANE, PotionInfo(15, canExtend = false, canEnhance = false, baseSeconds = 0))
        put(PotionType.THICK, PotionInfo(15, canExtend = false, canEnhance = false, baseSeconds = 0))
        put(PotionType.AWKWARD, PotionInfo(20, canExtend = false, canEnhance = false, baseSeconds = 0))

        // ========================================
        // Basic effect potions (difficulty 30)
        // ========================================

        // Healing - Instant, no duration
        put(PotionType.HEALING, PotionInfo(30, canExtend = false, canEnhance = true, baseSeconds = 0))

        // Harming - Instant, no duration
        put(PotionType.HARMING, PotionInfo(35, canExtend = false, canEnhance = true, baseSeconds = 0))

        // Fire Resistance - Duration only, no enhance
        put(PotionType.FIRE_RESISTANCE, PotionInfo(30, canExtend = true, canEnhance = false, baseSeconds = 180))

        // Night Vision - Duration only, no enhance
        put(PotionType.NIGHT_VISION, PotionInfo(25, canExtend = true, canEnhance = false, baseSeconds = 180))

        // Water Breathing - Duration only, no enhance
        put(PotionType.WATER_BREATHING, PotionInfo(30, canExtend = true, canEnhance = false, baseSeconds = 180))

        // Invisibility - Duration only, no enhance
        put(PotionType.INVISIBILITY, PotionInfo(40, canExtend = true, canEnhance = false, baseSeconds = 180))

        // Swiftness - Can extend and enhance
        put(PotionType.SWIFTNESS, PotionInfo(30, canExtend = true, canEnhance = true, baseSeconds = 180))

        // Slowness - Can extend and enhance
        put(PotionType.SLOWNESS, PotionInfo(30, canExtend = true, canEnhance = true, baseSeconds = 90))

        // Leaping - Can extend and enhance
        put(PotionType.LEAPING, PotionInfo(30, canExtend = true, canEnhance = true, baseSeconds = 180))

        // Strength - Can extend and enhance
        put(PotionType.STRENGTH, PotionInfo(35, canExtend = true, canEnhance = true, baseSeconds = 180))

        // Regeneration - Can extend and enhance
        put(PotionType.REGENERATION, PotionInfo(40, canExtend = true, canEnhance = true, baseSeconds = 45))

        // Poison - Can extend and enhance
        put(PotionType.POISON, PotionInfo(35, canExtend = true, canEnhance = true, baseSeconds = 45))

        // Weakness - Duration only, no enhance
        put(PotionType.WEAKNESS, PotionInfo(30, canExtend = true, canEnhance = false, baseSeconds = 90))

        // ========================================
        // Advanced potions (difficulty 45-55)
        // ========================================

        // Slow Falling - Duration only
        put(PotionType.SLOW_FALLING, PotionInfo(45, canExtend = true, canEnhance = false, baseSeconds = 90))

        // Turtle Master - Complex effect (slowness + resistance)
        put(PotionType.TURTLE_MASTER, PotionInfo(55, canExtend = true, canEnhance = true, baseSeconds = 20))

        // Luck - Rare effect
        put(PotionType.LUCK, PotionInfo(50, canExtend = false, canEnhance = false, baseSeconds = 300))

        // ========================================
        // 1.21+ New potions (difficulty 50-60)
        // ========================================

        // Wind Charged - Combat utility
        put(PotionType.WIND_CHARGED, PotionInfo(50, canExtend = false, canEnhance = false, baseSeconds = 180))

        // Weaving - Combat utility
        put(PotionType.WEAVING, PotionInfo(50, canExtend = false, canEnhance = false, baseSeconds = 180))

        // Oozing - Combat utility
        put(PotionType.OOZING, PotionInfo(50, canExtend = false, canEnhance = false, baseSeconds = 180))

        // Infested - Combat utility
        put(PotionType.INFESTED, PotionInfo(50, canExtend = false, canEnhance = false, baseSeconds = 180))
    }

    /**
     * Calculate total difficulty for a potion
     *
     * @param potionType The base potion type
     * @param isExtended Whether the potion has extended duration
     * @param isUpgraded Whether the potion is level II
     * @param containerType The container type (POTION, SPLASH_POTION, LINGERING_POTION)
     */
    fun calculateDifficulty(
        potionType: PotionType,
        isExtended: Boolean,
        isUpgraded: Boolean,
        containerType: Material
    ): Int {
        val info = potionInfoMap[potionType] ?: return 30

        var difficulty = info.baseDifficulty

        // Extended duration adds +15
        if (isExtended && info.canExtend) {
            difficulty += 15
        }

        // Upgraded (Level II) adds +20
        if (isUpgraded && info.canEnhance) {
            difficulty += 20
        }

        // Container modifiers
        difficulty += when (containerType) {
            Material.SPLASH_POTION -> 10
            Material.LINGERING_POTION -> 20
            else -> 0
        }

        return difficulty
    }

    /**
     * Get base potion info
     */
    fun getPotionInfo(potionType: PotionType): PotionInfo? = potionInfoMap[potionType]

    /**
     * Get base difficulty without modifiers
     */
    fun getBaseDifficulty(potionType: PotionType): Int = potionInfoMap[potionType]?.baseDifficulty ?: 30

    /**
     * Calculate duration bonus based on Alchemy skill
     * Skill 100 = +50% duration
     */
    fun calculateDurationBonus(alchemySkill: Double): Double {
        return 1.0 + (alchemySkill / 200.0)  // 1.0 to 1.5
    }

    /**
     * Calculate new duration with skill bonus
     *
     * @param baseDurationTicks Base duration in ticks (20 ticks = 1 second)
     * @param alchemySkill The player's Alchemy skill value
     * @return New duration in ticks
     */
    fun calculateBonusDuration(baseDurationTicks: Int, alchemySkill: Double): Int {
        val bonus = calculateDurationBonus(alchemySkill)
        return (baseDurationTicks * bonus).toInt()
    }

    /**
     * Difficulty table summary for reference
     *
     * | Potion Type       | Base | Extended | Upgraded | Splash | Lingering |
     * |-------------------|------|----------|----------|--------|-----------|
     * | Healing           | 30   | -        | 50       | 40/60  | 50/70     |
     * | Swiftness         | 30   | 45       | 50       | 40-60  | 50-70     |
     * | Strength          | 35   | 50       | 55       | 45-65  | 55-75     |
     * | Regeneration      | 40   | 55       | 60       | 50-70  | 60-80     |
     * | Invisibility      | 40   | 55       | -        | 50/65  | 60/75     |
     * | Turtle Master     | 55   | 70       | 75       | 65-85  | 75-95     |
     */
}
