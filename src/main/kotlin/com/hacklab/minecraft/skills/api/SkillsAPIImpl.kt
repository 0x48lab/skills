package com.hacklab.minecraft.skills.api

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatType
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of the SkillsAPI.
 */
class SkillsAPIImpl(private val plugin: Skills) : SkillsAPI {

    // ===== Skill Operations =====

    override fun getSkill(player: Player, skillName: String): Double? {
        val skillType = resolveSkillType(skillName) ?: return null
        return plugin.skillManager.getSkillValue(player, skillType)
    }

    override fun getSkill(uuid: UUID, skillName: String): Double? {
        val skillType = resolveSkillType(skillName) ?: return null
        val data = plugin.playerDataManager.getPlayerData(uuid) ?: return null
        return data.getSkillValue(skillType)
    }

    override fun setSkill(player: Player, skillName: String, value: Double): Boolean {
        val skillType = resolveSkillType(skillName) ?: return false
        val clampedValue = max(0.0, min(100.0, value))
        plugin.skillManager.setSkill(player, skillType, clampedValue)
        return true
    }

    override fun addSkill(player: Player, skillName: String, amount: Double): Double? {
        val skillType = resolveSkillType(skillName) ?: return null
        val currentValue = plugin.skillManager.getSkillValue(player, skillType)
        val newValue = max(0.0, min(100.0, currentValue + amount))
        plugin.skillManager.setSkill(player, skillType, newValue)
        return newValue
    }

    override fun hasSkillLevel(player: Player, skillName: String, minLevel: Double): Boolean {
        val skillValue = getSkill(player, skillName) ?: return false
        return skillValue >= minLevel
    }

    override fun getAllSkills(player: Player): Map<String, Double> {
        val data = plugin.playerDataManager.getPlayerData(player)
        return SkillType.entries.associate { skill ->
            skill.displayName to data.getSkillValue(skill)
        }
    }

    override fun getTotalSkillPoints(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.getTotalSkillPoints()
    }

    // ===== Stat Operations =====

    override fun getStat(player: Player, statName: String): Int? {
        val data = plugin.playerDataManager.getPlayerData(player)
        return when (statName.uppercase()) {
            "STR", "STRENGTH" -> data.str
            "DEX", "DEXTERITY" -> data.dex
            "INT", "INTELLIGENCE" -> data.int
            else -> null
        }
    }

    override fun getAllStats(player: Player): Map<String, Int> {
        val data = plugin.playerDataManager.getPlayerData(player)
        return mapOf(
            "STR" to data.str,
            "DEX" to data.dex,
            "INT" to data.int
        )
    }

    // ===== HP/Mana/Stamina Operations =====

    override fun getCurrentHp(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.internalHp
    }

    override fun getMaxHp(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.maxInternalHp
    }

    override fun getCurrentMana(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.mana
    }

    override fun getMaxMana(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.maxMana
    }

    override fun getCurrentStamina(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.stamina
    }

    override fun getMaxStamina(player: Player): Double {
        val data = plugin.playerDataManager.getPlayerData(player)
        return data.maxStamina
    }

    override fun restoreMana(player: Player, amount: Double) {
        plugin.manaManager.restoreMana(player, amount)
    }

    override fun restoreStamina(player: Player, amount: Double) {
        val data = plugin.playerDataManager.getPlayerData(player)
        data.restoreStamina(amount)
    }

    // ===== Utility =====

    override fun getAvailableSkillNames(): List<String> {
        return SkillType.entries.map { it.displayName }
    }

    override fun isValidSkillName(skillName: String): Boolean {
        return resolveSkillType(skillName) != null
    }

    override fun getTitle(player: Player): String {
        val data = plugin.playerDataManager.getPlayerData(player)
        return plugin.skillTitleManager.getTitleFromData(data)
    }

    // ===== Private Helpers =====

    /**
     * Resolve skill name to SkillType.
     * Accepts display name (e.g., "Swordsmanship") or enum name (e.g., "SWORDSMANSHIP").
     */
    private fun resolveSkillType(skillName: String): SkillType? {
        // Try display name first
        SkillType.fromDisplayName(skillName)?.let { return it }

        // Try enum name (with underscores replaced by spaces)
        val normalizedName = skillName.replace("_", " ")
        SkillType.fromDisplayName(normalizedName)?.let { return it }

        // Try exact enum name match
        return try {
            SkillType.valueOf(skillName.uppercase().replace(" ", "_"))
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
