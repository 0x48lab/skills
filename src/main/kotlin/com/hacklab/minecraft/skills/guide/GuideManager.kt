package com.hacklab.minecraft.skills.guide

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class GuideManager(private val plugin: Skills) {
    private val guideContents: MutableMap<Language, List<String>> = ConcurrentHashMap()
    private val PAGE_DELIMITER = "---PAGE---"

    fun loadGuides() {
        val guideFolder = File(plugin.dataFolder, "guide")
        if (!guideFolder.exists()) {
            guideFolder.mkdirs()
        }

        // Save default guide files from resources
        Language.entries.forEach { lang ->
            val resourcePath = "guide/${lang.code}.txt"
            val file = File(guideFolder, "${lang.code}.txt")
            if (!file.exists()) {
                try {
                    plugin.saveResource(resourcePath, false)
                } catch (e: Exception) {
                    plugin.logger.warning("Could not save default guide file: $resourcePath")
                }
            }
        }

        // Load all guide files
        Language.entries.forEach { lang ->
            val file = File(guideFolder, "${lang.code}.txt")
            if (file.exists()) {
                val content = file.readText()
                val pages = content.split(PAGE_DELIMITER).map { it.trim() }.filter { it.isNotEmpty() }
                guideContents[lang] = pages
                plugin.logger.info("Loaded guide file: ${lang.code}.txt (${pages.size} pages)")
            }
        }
    }

    /**
     * Create a guide book for the specified language
     */
    fun createGuideBook(language: Language): ItemStack {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as BookMeta

        // Set book metadata
        val title = when (language) {
            Language.JAPANESE -> "スキルガイド"
            else -> "Skills Guide"
        }
        meta.title(Component.text(title).color(NamedTextColor.DARK_BLUE))
        meta.author(Component.text("Skills System"))

        // Get pages for the language (fallback to English)
        val pages = guideContents[language] ?: guideContents[Language.ENGLISH] ?: listOf("No guide available.")

        // Add each page to the book
        pages.forEach { pageText ->
            val pageComponent = formatPage(pageText)
            meta.addPages(pageComponent)
        }

        book.itemMeta = meta
        return book
    }

    /**
     * Format a page of text into a Component
     */
    private fun formatPage(text: String): Component {
        val builder = Component.text()
        val lines = text.split("\n")

        lines.forEachIndexed { index, line ->
            val component = when {
                // Headers (=== Title ===)
                line.startsWith("===") && line.endsWith("===") -> {
                    val title = line.removePrefix("===").removeSuffix("===").trim()
                    Component.text(title + "\n")
                        .color(NamedTextColor.DARK_BLUE)
                        .decorate(TextDecoration.BOLD)
                }
                // Sub-headers (text:)
                line.endsWith(":") && !line.startsWith("-") && !line.startsWith(" ") -> {
                    Component.text(line + "\n").color(NamedTextColor.DARK_RED)
                }
                // Bullet points
                line.startsWith("- ") -> {
                    Component.text(line + "\n").color(NamedTextColor.BLACK)
                }
                // Indented text
                line.startsWith("  ") -> {
                    Component.text(line + "\n").color(NamedTextColor.DARK_BLUE)
                }
                // Empty line
                line.isBlank() -> {
                    Component.text("\n")
                }
                // Normal text
                else -> {
                    Component.text(line + "\n").color(NamedTextColor.BLACK)
                }
            }
            builder.append(component)
        }

        return builder.build()
    }

    /**
     * Give a guide book to a player in their preferred language
     */
    fun giveGuideBook(player: Player) {
        val language = plugin.localeManager.getLanguage(player.uniqueId)
        val book = createGuideBook(language)
        player.inventory.addItem(book)
    }

    /**
     * Check if guides are loaded
     */
    fun hasGuide(language: Language): Boolean = guideContents.containsKey(language)

    /**
     * Reload all guide files
     */
    fun reload() {
        guideContents.clear()
        loadGuides()
    }
}
