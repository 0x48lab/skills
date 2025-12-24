package com.hacklab.minecraft.skills.i18n

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MessageManager(private val plugin: Skills) {
    private val messages: MutableMap<Language, YamlConfiguration> = ConcurrentHashMap()
    private var defaultLanguage: Language = Language.ENGLISH
    private val miniMessage = MiniMessage.miniMessage()

    fun loadLanguages() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        // Save default language files from resources
        Language.entries.forEach { lang ->
            val resourcePath = "lang/${lang.code}.yml"
            val file = File(langFolder, "${lang.code}.yml")
            if (!file.exists()) {
                try {
                    plugin.saveResource(resourcePath, false)
                } catch (e: Exception) {
                    plugin.logger.warning("Could not save default language file: $resourcePath")
                }
            }
        }

        // Load all language files
        Language.entries.forEach { lang ->
            val file = File(langFolder, "${lang.code}.yml")
            if (file.exists()) {
                messages[lang] = YamlConfiguration.loadConfiguration(file)
                plugin.logger.info("Loaded language file: ${lang.code}.yml")
            }
        }

        // Load default language from config
        val configLang = plugin.config.getString("language.default", "en") ?: "en"
        defaultLanguage = Language.fromCode(configLang)
    }

    fun setDefaultLanguage(language: Language) {
        defaultLanguage = language
    }

    fun getDefaultLanguage(): Language = defaultLanguage

    /**
     * Get a raw message string with placeholder replacement
     */
    fun get(key: MessageKey, lang: Language, vararg placeholders: Pair<String, Any>): String {
        val config = messages[lang] ?: messages[defaultLanguage]
        var message = config?.getString(key.path) ?: key.path

        placeholders.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value.toString())
        }

        return message
    }

    /**
     * Get a message as Adventure Component (supports MiniMessage format)
     */
    fun getComponent(key: MessageKey, lang: Language, vararg placeholders: Pair<String, Any>): Component {
        val message = get(key, lang, *placeholders)
        return miniMessage.deserialize(message)
    }

    /**
     * Get a raw message string
     */
    fun getRaw(key: MessageKey, lang: Language): String {
        val config = messages[lang] ?: messages[defaultLanguage]
        return config?.getString(key.path) ?: key.path
    }

    /**
     * Check if a message key exists
     */
    fun hasKey(key: MessageKey, lang: Language): Boolean {
        val config = messages[lang] ?: return false
        return config.contains(key.path)
    }

    /**
     * Reload all language files
     */
    fun reload() {
        messages.clear()
        loadLanguages()
    }
}
