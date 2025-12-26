package com.hacklab.minecraft.skills.i18n

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerLocaleManager(private val plugin: Skills) {
    private val playerLocales: MutableMap<UUID, Language> = ConcurrentHashMap()

    /**
     * Get the language for a player
     * Priority: 1. Explicitly saved setting (not null), 2. Client locale, 3. Default
     */
    fun getLanguage(player: Player): Language {
        // Check explicitly saved setting first (from PlayerData, not cache)
        val playerData = plugin.playerDataManager.getPlayerData(player)
        playerData.language?.let { return it }

        // Check if we should use client locale
        if (plugin.config.getBoolean("language.use_client_locale", true)) {
            val clientLocale = player.locale().toString()
            return Language.fromLocale(clientLocale)
        }

        // Fall back to default
        return plugin.messageManager.getDefaultLanguage()
    }

    /**
     * Get language for a UUID (cached only)
     */
    fun getLanguage(uuid: UUID): Language {
        return playerLocales[uuid] ?: plugin.messageManager.getDefaultLanguage()
    }

    /**
     * Set a player's language preference (explicitly)
     */
    fun setLanguage(player: Player, language: Language) {
        playerLocales[player.uniqueId] = language
        // Also update PlayerData
        plugin.playerDataManager.getPlayerData(player).language = language
    }

    /**
     * Reset a player's language preference to use client locale
     */
    fun resetLanguage(player: Player) {
        playerLocales.remove(player.uniqueId)
        plugin.playerDataManager.getPlayerData(player).language = null
    }

    /**
     * Load a player's language preference from PlayerData
     * Only caches if language is explicitly set (not null)
     */
    fun loadPlayerLocale(uuid: UUID, language: Language?) {
        if (language != null) {
            playerLocales[uuid] = language
        }
    }

    /**
     * Remove a player's cached language preference
     */
    fun removePlayer(uuid: UUID) {
        playerLocales.remove(uuid)
    }

    /**
     * Check if player language change is allowed
     */
    fun canPlayerChangeLanguage(): Boolean {
        return plugin.config.getBoolean("language.allow_player_change", true)
    }
}
