package com.hacklab.minecraft.skills.combat

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.entity.Player

class InternalHealthManager(private val plugin: Skills) {

    /**
     * Sync vanilla health change to internal HP
     */
    fun syncFromVanilla(player: Player, vanillaDamage: Double): Double {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Convert vanilla damage to internal
        val internalDamage = vanillaDamage * plugin.skillsConfig.baseDamageMultiplier

        // Apply damage
        data.damage(internalDamage)

        return internalDamage
    }

    /**
     * Sync internal HP to vanilla health
     */
    fun syncToVanilla(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        StatCalculator.syncHealthToVanilla(player, data)
    }

    /**
     * Get player's current internal HP
     */
    fun getInternalHp(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).internalHp
    }

    /**
     * Get player's max internal HP
     */
    fun getMaxInternalHp(player: Player): Double {
        return plugin.playerDataManager.getPlayerData(player).maxInternalHp
    }

    /**
     * Check if player is dead (internal HP <= 0)
     */
    fun isInternallyDead(player: Player): Boolean {
        return getInternalHp(player) <= 0
    }

    /**
     * Restore player to full internal HP
     */
    fun fullHeal(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.internalHp = data.maxInternalHp
        syncToVanilla(player)
    }

    /**
     * Handle player respawn - reset internal HP
     */
    fun handleRespawn(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.internalHp = data.maxInternalHp
        data.mana = data.maxMana
        syncToVanilla(player)
    }

    /**
     * Apply food healing to internal HP
     */
    fun applyFoodHealing(player: Player, healAmount: Int) {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Convert food healing to internal (food restores both HP and Mana in UO style)
        val internalHeal = healAmount.toDouble() * plugin.skillsConfig.baseDamageMultiplier
        data.heal(internalHeal)

        syncToVanilla(player)
    }
}
