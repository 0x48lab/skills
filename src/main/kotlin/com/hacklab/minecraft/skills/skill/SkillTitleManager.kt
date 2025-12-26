package com.hacklab.minecraft.skills.skill

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.Player

/**
 * UO-style skill title system
 *
 * Ranks based on skill level:
 * - Neophyte: 0-29.9
 * - Novice: 30-39.9
 * - Apprentice: 40-49.9
 * - Journeyman: 50-59.9
 * - Expert: 60-69.9
 * - Adept: 70-79.9
 * - Master: 80-89.9
 * - Grandmaster: 90-100
 */
class SkillTitleManager(private val plugin: Skills) {

    /**
     * Skill rank based on skill level
     */
    enum class SkillRank(val minSkill: Double, val displayName: String, val displayNameJa: String) {
        GRANDMASTER(90.0, "Grandmaster", "伝説の"),
        MASTER(80.0, "Master", "達人"),
        ADEPT(70.0, "Adept", "熟練"),
        EXPERT(60.0, "Expert", "上級"),
        JOURNEYMAN(50.0, "Journeyman", "一人前の"),
        APPRENTICE(40.0, "Apprentice", "見習い"),
        NOVICE(30.0, "Novice", "新米"),
        NEOPHYTE(0.0, "Neophyte", "初心者");

        companion object {
            fun fromSkillValue(value: Double): SkillRank {
                return entries.first { value >= it.minSkill }
            }
        }
    }

    /**
     * Title suffix for each skill type
     */
    private val skillTitles: Map<SkillType, Pair<String, String>> = mapOf(
        // Combat - Weapons
        SkillType.SWORDSMANSHIP to Pair("Swordsman", "剣士"),
        SkillType.AXE to Pair("Axeman", "斧使い"),
        SkillType.MACE_FIGHTING to Pair("Mace Fighter", "鈍器使い"),
        SkillType.ARCHERY to Pair("Archer", "弓手"),
        SkillType.THROWING to Pair("Lancer", "槍兵"),
        SkillType.WRESTLING to Pair("Wrestler", "格闘家"),

        // Combat - Support
        SkillType.TACTICS to Pair("Tactician", "戦術家"),
        SkillType.ANATOMY to Pair("Healer", "治療師"),
        SkillType.PARRYING to Pair("Defender", "防御の達人"),
        SkillType.FOCUS to Pair("Warrior", "武人"),

        // Magic
        SkillType.MAGERY to Pair("Mage", "魔術師"),
        SkillType.EVALUATING_INTELLIGENCE to Pair("Scholar", "学者"),
        SkillType.MEDITATION to Pair("Mystic", "瞑想者"),
        SkillType.RESISTING_SPELLS to Pair("Spellbreaker", "呪耐者"),

        // Crafting
        SkillType.ALCHEMY to Pair("Alchemist", "錬金術師"),
        SkillType.BLACKSMITHY to Pair("Blacksmith", "鍛冶師"),
        SkillType.BOWCRAFT to Pair("Bowyer", "弓職人"),
        SkillType.CRAFTSMANSHIP to Pair("Artisan", "職人"),
        SkillType.COOKING to Pair("Chef", "料理人"),
        SkillType.INSCRIPTION to Pair("Scribe", "書写師"),
        SkillType.TINKERING to Pair("Tinker", "細工師"),

        // Gathering
        SkillType.MINING to Pair("Miner", "採掘者"),
        SkillType.LUMBERJACKING to Pair("Lumberjack", "木こり"),
        SkillType.FISHING to Pair("Fisherman", "漁師"),

        // Thief
        SkillType.HIDING to Pair("Shadow", "影"),
        SkillType.STEALTH to Pair("Rogue", "盗賊"),
        SkillType.DETECTING_HIDDEN to Pair("Scout", "斥候"),
        SkillType.SNOOPING to Pair("Spy", "密偵"),
        SkillType.STEALING to Pair("Thief", "泥棒"),
        SkillType.POISONING to Pair("Assassin", "暗殺者"),

        // Taming
        SkillType.ANIMAL_TAMING to Pair("Tamer", "調教師"),
        SkillType.ANIMAL_LORE to Pair("Ranger", "レンジャー"),
        SkillType.VETERINARY to Pair("Veterinarian", "獣医"),

        // Survival
        SkillType.ATHLETICS to Pair("Athlete", "冒険者"),
        SkillType.SWIMMING to Pair("Swimmer", "泳者"),
        SkillType.HEAT_RESISTANCE to Pair("Fire Walker", "炎の旅人"),
        SkillType.COLD_RESISTANCE to Pair("Frost Walker", "氷の旅人"),
        SkillType.ENDURANCE to Pair("Survivor", "生存者"),

        // Other
        SkillType.ARMS_LORE to Pair("Appraiser", "鑑定士")
    )

    /**
     * Get player's title based on their highest skill
     * @param player The player
     * @param useJapanese Whether to use Japanese titles
     * @return The formatted title (e.g., "Master Swordsman" or "達人 剣士")
     */
    fun getPlayerTitle(player: Player, useJapanese: Boolean = false): String {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Find highest skill
        var highestSkill: SkillType? = null
        var highestValue = 0.0

        SkillType.entries.forEach { skill ->
            val value = data.getSkillValue(skill)
            if (value > highestValue) {
                highestValue = value
                highestSkill = skill
            }
        }

        // If no skills, return default
        if (highestSkill == null || highestValue < 30) {
            return if (useJapanese) "冒険者" else "Adventurer"
        }

        val rank = SkillRank.fromSkillValue(highestValue)
        val titlePair = skillTitles[highestSkill] ?: Pair("Adventurer", "冒険者")

        return if (useJapanese) {
            "${rank.displayNameJa}${titlePair.second}"
        } else {
            "${rank.displayName} ${titlePair.first}"
        }
    }

    /**
     * Get player's title using their language preference
     */
    fun getPlayerTitle(player: Player): String {
        val lang = plugin.localeManager.getLanguage(player)
        return getPlayerTitle(player, lang.code == "ja")
    }

    /**
     * Get title for a specific skill at a given level
     */
    fun getTitleForSkill(skill: SkillType, value: Double, useJapanese: Boolean = false): String {
        if (value < 30) {
            return if (useJapanese) "冒険者" else "Adventurer"
        }

        val rank = SkillRank.fromSkillValue(value)
        val titlePair = skillTitles[skill] ?: Pair("Adventurer", "冒険者")

        return if (useJapanese) {
            "${rank.displayNameJa}${titlePair.second}"
        } else {
            "${rank.displayName} ${titlePair.first}"
        }
    }

    /**
     * Get the skill that gives the player their current title
     */
    fun getHighestSkill(player: Player): Pair<SkillType, Double>? {
        val data = plugin.playerDataManager.getPlayerData(player)

        var highestSkill: SkillType? = null
        var highestValue = 0.0

        SkillType.entries.forEach { skill ->
            val value = data.getSkillValue(skill)
            if (value > highestValue) {
                highestValue = value
                highestSkill = skill
            }
        }

        return highestSkill?.let { Pair(it, highestValue) }
    }

    /**
     * Get player's rank and job title separately
     * @return Pair of (rank, jobTitle) or null if no significant skill
     */
    fun getPlayerTitleParts(player: Player, useJapanese: Boolean = false): Pair<String, String>? {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Find highest skill
        var highestSkill: SkillType? = null
        var highestValue = 0.0

        SkillType.entries.forEach { skill ->
            val value = data.getSkillValue(skill)
            if (value > highestValue) {
                highestValue = value
                highestSkill = skill
            }
        }

        // If no skills above threshold, return null
        if (highestSkill == null || highestValue < 30) {
            return null
        }

        val rank = SkillRank.fromSkillValue(highestValue)
        val titlePair = skillTitles[highestSkill] ?: Pair("Adventurer", "冒険者")

        return if (useJapanese) {
            Pair(rank.displayNameJa, titlePair.second)
        } else {
            Pair(rank.displayName, titlePair.first)
        }
    }
}
