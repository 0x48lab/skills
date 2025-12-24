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
     * Calculate DEX-based effects
     */
    fun getDexEffects(dex: Int): DexEffects {
        return DexEffects(
            attackSpeedBonus = dex / 10.0,    // +10% at DEX 100
            movementSpeedBonus = dex / 10.0   // +10% at DEX 100
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
    private const val MODIFIER_NAME = "skills.dex_bonus"

    /**
     * Apply stat-based attribute modifiers to a player
     */
    fun applyAttributeModifiers(player: Player, data: PlayerData) {
        val dex = data.getDex()
        val dexEffects = getDexEffects(dex)

        // Apply movement speed modifier (DEX bonus)
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { attr ->
            // Remove existing modifier first
            attr.modifiers.filter { it.name == MODIFIER_NAME }.forEach { attr.removeModifier(it) }

            // Add new modifier (percentage-based)
            val speedBonus = dexEffects.movementSpeedBonus / 100.0  // Convert to decimal
            if (speedBonus > 0) {
                val modifier = AttributeModifier(
                    MOVEMENT_SPEED_MODIFIER_UUID,
                    MODIFIER_NAME,
                    speedBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                )
                attr.addModifier(modifier)
            }
        }

        // Apply attack speed modifier (DEX bonus)
        player.getAttribute(Attribute.ATTACK_SPEED)?.let { attr ->
            // Remove existing modifier first
            attr.modifiers.filter { it.name == MODIFIER_NAME }.forEach { attr.removeModifier(it) }

            // Add new modifier
            val attackSpeedBonus = dexEffects.attackSpeedBonus / 100.0
            if (attackSpeedBonus > 0) {
                val modifier = AttributeModifier(
                    ATTACK_SPEED_MODIFIER_UUID,
                    MODIFIER_NAME,
                    attackSpeedBonus,
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
     */
    fun syncManaToVanilla(player: Player, data: PlayerData) {
        val manaPercent = data.getManaPercentage()
        player.foodLevel = (20 * manaPercent).toInt().coerceIn(0, 20)
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
     * Calculate magic damage with skill bonuses
     */
    fun calculateMagicDamage(
        baseDamage: Double,
        magerySkill: Double,
        evalIntSkill: Double,
        targetResistSkill: Double = 0.0
    ): Double {
        // Magery affects base damage slightly
        val mageryModifier = 1.0 + (magerySkill / 400.0)  // +25% at skill 100

        // Eval Int is the main damage modifier
        val evalIntBonus = evalIntSkill / 2.0  // +50% at skill 100

        // Target resistance reduces damage
        val resistReduction = targetResistSkill / 2.0  // -50% at skill 100

        return baseDamage *
                mageryModifier *
                (1.0 + evalIntBonus / 100.0) *
                (1.0 - resistReduction / 100.0)
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
