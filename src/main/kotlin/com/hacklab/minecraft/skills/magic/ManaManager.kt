package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.entity.Player

class ManaManager(private val plugin: Skills) {

    /**
     * Check if player has enough mana for a spell
     */
    fun hasEnoughMana(player: Player, spell: SpellType): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val actualCost = calculateManaCost(player, spell)
        return data.mana >= actualCost
    }

    /**
     * Calculate actual mana cost after INT reduction
     */
    fun calculateManaCost(player: Player, spell: SpellType): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        val baseCost = spell.baseMana.toDouble()
        val intReduction = data.int / 200.0  // INT 100 = 50% reduction
        return baseCost * (1 - intReduction)
    }

    /**
     * Consume mana for a spell
     * @return true if successful, false if not enough mana
     */
    fun consumeMana(player: Player, spell: SpellType): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)
        val actualCost = calculateManaCost(player, spell)

        if (data.mana < actualCost) {
            return false
        }

        data.mana -= actualCost
        data.dirty = true

        return true
    }

    /**
     * Restore mana to player
     */
    fun restoreMana(player: Player, amount: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.restoreMana(amount)
    }

    /**
     * Get current mana
     */
    fun getCurrentMana(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).mana
    }

    /**
     * Get max mana
     */
    fun getMaxMana(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).maxMana
    }

    /**
     * Process meditation mana regeneration
     * Called when player is sneaking and stationary
     */
    fun processMeditation(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Only regenerate if not full
        if (data.mana >= data.maxMana) {
            return false
        }

        // Calculate regen rate based on Meditation skill
        val meditationSkill = data.getSkillValue(com.hacklab.minecraft.skills.skill.SkillType.MEDITATION)
        val baseRegen = 0.1  // Base mana per tick (0.5 seconds)
        val skillBonus = meditationSkill / 100.0  // +100% at skill 100
        val actualRegen = baseRegen * (1 + skillBonus)

        data.restoreMana(actualRegen)

        // Try skill gain
        plugin.skillManager.tryGainSkill(
            player,
            com.hacklab.minecraft.skills.skill.SkillType.MEDITATION,
            50
        )

        return true
    }

    /**
     * Process natural mana regeneration (INT-based)
     * Called periodically for all online players
     * @return true if mana was regenerated
     */
    fun processNaturalRegeneration(player: Player): Boolean {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Only regenerate if not full
        if (data.mana >= data.maxMana) {
            return false
        }

        // Calculate regen rate based on INT
        // Base: 0.5 mana per 5 seconds
        // INT bonus: +INT% (INT 100 = +100% = 1.0 mana per 5 seconds)
        val baseRegen = 0.5
        val intBonus = data.int / 100.0
        val actualRegen = baseRegen * (1 + intBonus)

        data.restoreMana(actualRegen)

        return true
    }
}
