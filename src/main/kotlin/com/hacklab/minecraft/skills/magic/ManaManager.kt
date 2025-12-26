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

        // Sync to vanilla food level
        StatCalculator.syncManaToVanilla(player, data)

        return true
    }

    /**
     * Restore mana to player
     */
    fun restoreMana(player: Player, amount: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.restoreMana(amount)
        StatCalculator.syncManaToVanilla(player, data)
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
        StatCalculator.syncManaToVanilla(player, data)

        // Try skill gain
        plugin.skillManager.tryGainSkill(
            player,
            com.hacklab.minecraft.skills.skill.SkillType.MEDITATION,
            50
        )

        return true
    }

    /**
     * Handle vanilla food level change
     * Keep mana synced when food level changes naturally
     */
    fun syncFromVanilla(player: Player, newFoodLevel: Int) {
        val data = plugin.playerDataManager.getPlayerData(player)
        // Convert food level (0-20) to mana percentage
        val manaPercent = newFoodLevel / 20.0
        data.mana = data.maxMana * manaPercent
        data.dirty = true
    }
}
