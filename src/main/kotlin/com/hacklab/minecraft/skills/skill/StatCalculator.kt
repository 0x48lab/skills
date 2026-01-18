package com.hacklab.minecraft.skills.skill

import com.hacklab.minecraft.skills.data.PlayerData
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.UUID

object StatCalculator {

    /**
     * Calculate STR-based effects
     */
    fun getStrEffects(str: Int): StrEffects {
        return StrEffects(
            bonusHp = str,                    // +1 HP per STR
            miningSpeedBonus = str / 10.0,    // +10% at STR 100
            lumberSpeedBonus = str / 10.0     // +10% at STR 100
        )
    }

    /**
     * Calculate DEX-based effects (UO-style)
     * Attack speed: UO had significant impact, DEX 100 could nearly double attack speed
     * Movement speed: Minecraft-specific, but made noticeable
     */
    fun getDexEffects(dex: Int): DexEffects {
        return DexEffects(
            attackSpeedBonus = dex / 2.0,     // +50% at DEX 100 (UO-like significant impact)
            movementSpeedBonus = dex / 10.0   // +10% at DEX 100 (noticeable speed boost)
        )
    }

    /**
     * Calculate INT-based effects
     */
    fun getIntEffects(int: Int): IntEffects {
        return IntEffects(
            manaReduction = int / 2.0,        // -50% mana cost at INT 100
            castSuccessBonus = int / 5.0      // +20% cast success at INT 100
        )
    }

    // Unique UUIDs for skill-based attribute modifiers
    private val MOVEMENT_SPEED_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    private val ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-5b6c-9d0e-1f2a3b4c5d6e")
    private val BLOCK_BREAK_SPEED_MODIFIER_UUID = UUID.fromString("c3d4e5f6-a7b8-6c7d-0e1f-2a3b4c5d6e7f")
    private const val DEX_MODIFIER_NAME = "skills.dex_bonus"
    private const val STR_MODIFIER_NAME = "skills.str_bonus"

    /**
     * Apply stat-based attribute modifiers to a player
     * @param player The player to apply modifiers to
     * @param data The player's skill data
     * @param armorDexPenalty DEX penalty from equipped armor (default 0)
     */
    fun applyAttributeModifiers(player: Player, data: PlayerData, armorDexPenalty: Int = 0) {
        // Apply armor DEX penalty to effective DEX
        val effectiveDex = (data.dex - armorDexPenalty).coerceAtLeast(0)
        val str = data.str
        val dexEffects = getDexEffects(effectiveDex)
        val strEffects = getStrEffects(str)

        // Apply movement speed modifier (DEX bonus)
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { attr ->
            // Remove existing modifier by UUID first
            attr.getModifier(MOVEMENT_SPEED_MODIFIER_UUID)?.let { attr.removeModifier(it) }

            // Add new modifier (percentage-based)
            val speedBonus = dexEffects.movementSpeedBonus / 100.0  // Convert to decimal
            if (speedBonus > 0) {
                val modifier = AttributeModifier(
                    MOVEMENT_SPEED_MODIFIER_UUID,
                    DEX_MODIFIER_NAME,
                    speedBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                )
                attr.addModifier(modifier)
            }
        }

        // Apply attack speed modifier (DEX bonus)
        player.getAttribute(Attribute.ATTACK_SPEED)?.let { attr ->
            // Remove existing modifier by UUID first
            attr.getModifier(ATTACK_SPEED_MODIFIER_UUID)?.let { attr.removeModifier(it) }

            // Add new modifier
            val attackSpeedBonus = dexEffects.attackSpeedBonus / 100.0
            if (attackSpeedBonus > 0) {
                val modifier = AttributeModifier(
                    ATTACK_SPEED_MODIFIER_UUID,
                    DEX_MODIFIER_NAME,
                    attackSpeedBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                )
                attr.addModifier(modifier)
            }
        }

        // Apply block break speed modifier (STR bonus)
        player.getAttribute(Attribute.BLOCK_BREAK_SPEED)?.let { attr ->
            // Remove existing modifier by UUID first
            attr.getModifier(BLOCK_BREAK_SPEED_MODIFIER_UUID)?.let { attr.removeModifier(it) }

            // Add new modifier
            val miningSpeedBonus = strEffects.miningSpeedBonus / 100.0
            if (miningSpeedBonus > 0) {
                val modifier = AttributeModifier(
                    BLOCK_BREAK_SPEED_MODIFIER_UUID,
                    STR_MODIFIER_NAME,
                    miningSpeedBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                )
                attr.addModifier(modifier)
            }
        }
    }

    /**
     * Sync internal HP to vanilla health
     */
    fun syncHealthToVanilla(player: Player, data: PlayerData) {
        val hpPercent = data.getHpPercentage()
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        player.health = (maxHealth * hpPercent).coerceIn(0.0, maxHealth)
    }

    /**
     * Sync internal Mana to vanilla food level
     * Note: Food level is capped at 19 (not 20) so player can always eat.
     * This allows eating to restore HP via overflow system even when mana is full.
     */
    fun syncManaToVanilla(player: Player, data: PlayerData) {
        val manaPercent = data.getManaPercentage()
        // Cap at 19 to allow eating even when mana is full
        player.foodLevel = (20 * manaPercent).toInt().coerceIn(0, 19)
        player.saturation = (5f * manaPercent.toFloat()).coerceIn(0f, 5f)
    }

    /**
     * Calculate damage with skill bonuses
     */
    fun calculateDamage(
        baseDamage: Double,
        weaponSkill: Double,
        tacticsSkill: Double,
        anatomySkill: Double,
        qualityModifier: Double = 1.0
    ): DamageResult {
        // Weapon skill affects base damage
        val weaponModifier = 1.0 + (weaponSkill / 200.0)  // +50% at skill 100

        // Tactics adds flat percentage
        val tacticsBonus = tacticsSkill / 5.0  // +20% at skill 100

        // Anatomy affects critical chance
        val criticalChance = anatomySkill / 2.0  // 50% at skill 100
        val isCritical = Math.random() * 100 < criticalChance
        val criticalMultiplier = if (isCritical) 1.5 else 1.0

        val finalDamage = baseDamage *
                weaponModifier *
                (1.0 + tacticsBonus / 100.0) *
                qualityModifier *
                criticalMultiplier

        return DamageResult(
            damage = finalDamage,
            isCritical = isCritical,
            criticalChance = criticalChance
        )
    }

    /**
     * Calculate magic damage with skill bonuses (UO-style)
     *
     * Magery and Evaluating Intelligence are ESSENTIAL skills for magic damage:
     * - Magery 10, EvalInt 10: ~6% of max damage
     * - Magery 100, EvalInt 100: 150% of base damage
     *
     * This matches the combat system where Tactics and Anatomy are essential.
     */
    fun calculateMagicDamage(
        baseDamage: Double,
        magerySkill: Double,
        evalIntSkill: Double,
        targetResistSkill: Double = 0.0
    ): Double {
        // Magery modifier: Magery / 100 → main damage multiplier (0.1 to 1.0)
        // Low Magery = very low damage, High Magery = full damage potential
        val mageryModifier = (magerySkill / 100.0).coerceIn(0.1, 1.0)

        // Eval Int modifier: 0.5 + (EvalInt / 100) → 0.6 to 1.5
        // Low EvalInt reduces damage, High EvalInt boosts damage
        val evalIntModifier = 0.5 + (evalIntSkill / 100.0)

        // Target resistance reduces damage: 0-70% reduction at skill 100
        val resistReduction = (targetResistSkill * 0.7 / 100.0).coerceIn(0.0, 0.7)

        return baseDamage * mageryModifier * evalIntModifier * (1.0 - resistReduction)
    }
}

data class StrEffects(
    val bonusHp: Int,
    val miningSpeedBonus: Double,
    val lumberSpeedBonus: Double
)

data class DexEffects(
    val attackSpeedBonus: Double,
    val movementSpeedBonus: Double
)

data class IntEffects(
    val manaReduction: Double,
    val castSuccessBonus: Double
)

data class DamageResult(
    val damage: Double,
    val isCritical: Boolean,
    val criticalChance: Double
)
