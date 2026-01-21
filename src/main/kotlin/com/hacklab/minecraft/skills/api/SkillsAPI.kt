package com.hacklab.minecraft.skills.api

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Public API for the Skills plugin.
 * Other plugins can access this API via Bukkit's ServicesManager:
 *
 * ```kotlin
 * val api = Bukkit.getServicesManager().getRegistration(SkillsAPI::class.java)?.provider
 * ```
 */
interface SkillsAPI {

    // ===== Skill Operations =====

    /**
     * Get the skill value for a player.
     *
     * @param player The player
     * @param skillName Skill name (e.g., "Swordsmanship", "Mining", "SWORDSMANSHIP")
     * @return Skill value (0.0 - 100.0), or null if skill not found
     */
    fun getSkill(player: Player, skillName: String): Double?

    /**
     * Get the skill value for a player by UUID.
     *
     * @param uuid The player's UUID
     * @param skillName Skill name
     * @return Skill value (0.0 - 100.0), or null if skill not found or player not loaded
     */
    fun getSkill(uuid: UUID, skillName: String): Double?

    /**
     * Set the skill value for a player.
     *
     * @param player The player
     * @param skillName Skill name
     * @param value New skill value (will be clamped to 0.0 - 100.0)
     * @return true if successful, false if skill not found
     */
    fun setSkill(player: Player, skillName: String, value: Double): Boolean

    /**
     * Add to a skill value (respects cap).
     *
     * @param player The player
     * @param skillName Skill name
     * @param amount Amount to add (can be negative)
     * @return New skill value, or null if skill not found
     */
    fun addSkill(player: Player, skillName: String, amount: Double): Double?

    /**
     * Check if player has at least the specified skill level.
     *
     * @param player The player
     * @param skillName Skill name
     * @param minLevel Minimum required level
     * @return true if player's skill >= minLevel
     */
    fun hasSkillLevel(player: Player, skillName: String, minLevel: Double): Boolean

    /**
     * Get all skill values for a player.
     *
     * @param player The player
     * @return Map of skill display name to value
     */
    fun getAllSkills(player: Player): Map<String, Double>

    /**
     * Get the total skill points for a player.
     *
     * @param player The player
     * @return Total skill points
     */
    fun getTotalSkillPoints(player: Player): Double

    // ===== Stat Operations =====

    /**
     * Get a stat value (STR, DEX, INT) for a player.
     *
     * @param player The player
     * @param statName "STR", "DEX", or "INT"
     * @return Stat value, or null if stat not found
     */
    fun getStat(player: Player, statName: String): Int?

    /**
     * Get all stats for a player.
     *
     * @param player The player
     * @return Map of stat name to value
     */
    fun getAllStats(player: Player): Map<String, Int>

    // ===== HP/Mana/Stamina Operations =====

    /**
     * Get current HP for a player.
     *
     * @param player The player
     * @return Current HP
     */
    fun getCurrentHp(player: Player): Double

    /**
     * Get max HP for a player.
     *
     * @param player The player
     * @return Max HP
     */
    fun getMaxHp(player: Player): Double

    /**
     * Get current mana for a player.
     *
     * @param player The player
     * @return Current mana
     */
    fun getCurrentMana(player: Player): Double

    /**
     * Get max mana for a player.
     *
     * @param player The player
     * @return Max mana
     */
    fun getMaxMana(player: Player): Double

    /**
     * Get current stamina for a player.
     *
     * @param player The player
     * @return Current stamina
     */
    fun getCurrentStamina(player: Player): Double

    /**
     * Get max stamina for a player.
     *
     * @param player The player
     * @return Max stamina
     */
    fun getMaxStamina(player: Player): Double

    /**
     * Restore mana for a player.
     *
     * @param player The player
     * @param amount Amount to restore
     */
    fun restoreMana(player: Player, amount: Double)

    /**
     * Restore stamina for a player.
     *
     * @param player The player
     * @param amount Amount to restore
     */
    fun restoreStamina(player: Player, amount: Double)

    // ===== Utility =====

    /**
     * Get all available skill names.
     *
     * @return List of skill display names
     */
    fun getAvailableSkillNames(): List<String>

    /**
     * Check if a skill name is valid.
     *
     * @param skillName Skill name to check
     * @return true if valid
     */
    fun isValidSkillName(skillName: String): Boolean

    /**
     * Get the player's title based on their skills.
     *
     * @param player The player
     * @return Title string
     */
    fun getTitle(player: Player): String
}
