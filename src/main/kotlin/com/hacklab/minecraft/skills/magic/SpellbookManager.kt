package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class SpellbookManager(private val plugin: Skills) {
    private val spellbookKey = NamespacedKey(plugin, "spellbook")
    private val spellsKey = NamespacedKey(plugin, "spells")
    private val languageKey = NamespacedKey(plugin, "spellbook_lang")

    private val localization: MutableMap<Language, YamlConfiguration> = ConcurrentHashMap()

    fun loadLocalization() {
        val folder = File(plugin.dataFolder, "spellbook")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        // Save default files from resources
        Language.entries.forEach { lang ->
            val resourcePath = "spellbook/${lang.code}.yml"
            val file = File(folder, "${lang.code}.yml")
            if (!file.exists()) {
                try {
                    plugin.saveResource(resourcePath, false)
                } catch (e: Exception) {
                    plugin.logger.warning("Could not save spellbook localization: $resourcePath")
                }
            }
        }

        // Load all localization files
        Language.entries.forEach { lang ->
            val file = File(folder, "${lang.code}.yml")
            if (file.exists()) {
                localization[lang] = YamlConfiguration.loadConfiguration(file)
                plugin.logger.info("Loaded spellbook localization: ${lang.code}.yml")
            }
        }
    }

    private fun getLocalized(lang: Language, key: String, default: String = key): String {
        val config = localization[lang] ?: localization[Language.ENGLISH]
        val value = config?.getString(key)
        return if (value.isNullOrBlank()) default else value
    }

    private fun getCircleName(lang: Language, circle: SpellCircle): String {
        val circleKey = circle.name.lowercase()
        return getLocalized(lang, "circles.$circleKey", "${circle.name.lowercase().replaceFirstChar { it.uppercase() }} Circle")
    }

    private fun getSpellDescription(lang: Language, spell: SpellType): String {
        return getLocalized(lang, "spells.${spell.name}", spell.displayName)
    }

    private fun getMaterialName(lang: Language, material: org.bukkit.Material): String {
        return getLocalized(lang, "materials.${material.name}", formatMaterialName(material))
    }

    private fun formatMaterialName(material: org.bukkit.Material): String {
        return material.name.lowercase().replace("_", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    /**
     * Check if an item is a spellbook
     */
    fun isSpellbook(item: ItemStack?): Boolean {
        if (item == null) return false
        if (item.type != Material.WRITTEN_BOOK && item.type != Material.ENCHANTED_BOOK) {
            return false
        }
        return item.itemMeta?.persistentDataContainer?.has(spellbookKey, PersistentDataType.BYTE) == true
    }

    /**
     * Create a new empty spellbook with player's language
     */
    fun createSpellbook(player: Player): ItemStack {
        val lang = plugin.localeManager.getLanguage(player)
        return createSpellbookWithLanguage(lang)
    }

    /**
     * Create a new empty spellbook (uses Japanese as default for NPC trades)
     */
    fun createSpellbook(): ItemStack {
        return createSpellbookWithLanguage(Language.JAPANESE)
    }

    /**
     * Create a new empty spellbook with specific language
     */
    fun createSpellbookWithLanguage(lang: Language): ItemStack {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as? BookMeta ?: return book

        val name = getLocalized(lang, "name", "Spellbook")
        val author = getLocalized(lang, "author", "Arcane Library")

        meta.title(Component.text(name).color(NamedTextColor.DARK_BLUE))
        meta.author(Component.text(author))

        // Add empty page
        val emptyPage = Component.text()
            .append(Component.text("=== $name ===\n\n").color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.BOLD))
            .append(Component.text(getLocalized(lang, "empty_message", "No spells recorded.\n\nUse scrolls to learn spells.")).color(NamedTextColor.BLACK))
            .build()
        meta.addPages(emptyPage)

        meta.persistentDataContainer.set(spellbookKey, PersistentDataType.BYTE, 1)
        meta.persistentDataContainer.set(spellsKey, PersistentDataType.STRING, "")
        meta.persistentDataContainer.set(languageKey, PersistentDataType.STRING, lang.code)

        book.itemMeta = meta
        return book
    }

    /**
     * Get the language of a spellbook
     */
    private fun getSpellbookLanguage(spellbook: ItemStack): Language {
        val meta = spellbook.itemMeta ?: return Language.ENGLISH
        val langCode = meta.persistentDataContainer.get(languageKey, PersistentDataType.STRING) ?: "en"
        return Language.fromCode(langCode)
    }

    /**
     * Get the spellbook from player's inventory (or hand)
     */
    fun getSpellbook(player: Player): ItemStack? {
        // Check main hand first
        val mainHand = player.inventory.itemInMainHand
        if (isSpellbook(mainHand)) return mainHand

        // Check off hand
        val offHand = player.inventory.itemInOffHand
        if (isSpellbook(offHand)) return offHand

        // Search inventory
        return player.inventory.contents.firstOrNull { isSpellbook(it) }
    }

    /**
     * Check if player has a spellbook
     */
    fun hasSpellbook(player: Player): Boolean {
        return getSpellbook(player) != null
    }

    /**
     * Get spells in a spellbook
     */
    fun getSpells(spellbook: ItemStack): Set<SpellType> {
        if (!isSpellbook(spellbook)) return emptySet()

        val meta = spellbook.itemMeta ?: return emptySet()
        val spellsString = meta.persistentDataContainer.get(spellsKey, PersistentDataType.STRING) ?: ""

        if (spellsString.isEmpty()) return emptySet()

        return spellsString.split(",")
            .mapNotNull { name ->
                try { SpellType.valueOf(name) } catch (e: Exception) { null }
            }
            .toSet()
    }

    /**
     * Check if spellbook contains a specific spell
     */
    fun hasSpell(spellbook: ItemStack, spell: SpellType): Boolean {
        return getSpells(spellbook).contains(spell)
    }

    /**
     * Check if player's spellbook contains a spell
     */
    fun hasSpell(player: Player, spell: SpellType): Boolean {
        val spellbook = getSpellbook(player) ?: return false
        return hasSpell(spellbook, spell)
    }

    /**
     * Add a spell to a spellbook
     */
    fun addSpell(spellbook: ItemStack, spell: SpellType): Boolean {
        if (!isSpellbook(spellbook)) return false

        val currentSpells = getSpells(spellbook).toMutableSet()
        if (currentSpells.contains(spell)) return false

        currentSpells.add(spell)
        updateSpellbook(spellbook, currentSpells)
        return true
    }

    /**
     * Remove a spell from a spellbook
     */
    fun removeSpell(spellbook: ItemStack, spell: SpellType): Boolean {
        if (!isSpellbook(spellbook)) return false

        val currentSpells = getSpells(spellbook).toMutableSet()
        if (!currentSpells.contains(spell)) return false

        currentSpells.remove(spell)
        updateSpellbook(spellbook, currentSpells)
        return true
    }

    /**
     * Update spellbook with new spell list - creates interactive book pages
     */
    private fun updateSpellbook(spellbook: ItemStack, spells: Set<SpellType>) {
        val meta = spellbook.itemMeta as? BookMeta ?: return
        val lang = getSpellbookLanguage(spellbook)

        // Update persistent data
        val spellsString = spells.joinToString(",") { it.name }
        meta.persistentDataContainer.set(spellsKey, PersistentDataType.STRING, spellsString)

        // Don't try to clear pages - just create a new BookMeta instead
        // (The old while loop was an infinite loop because page() doesn't remove pages)

        // Create new book with pages
        val newMeta = (ItemStack(Material.WRITTEN_BOOK).itemMeta as BookMeta)
        newMeta.title(meta.title())
        newMeta.author(meta.author())

        // Copy persistent data
        newMeta.persistentDataContainer.set(spellbookKey, PersistentDataType.BYTE, 1)
        newMeta.persistentDataContainer.set(spellsKey, PersistentDataType.STRING, spellsString)
        newMeta.persistentDataContainer.set(languageKey, PersistentDataType.STRING, lang.code)

        if (spells.isEmpty()) {
            val emptyPage = Component.text()
                .append(Component.text("=== ${getLocalized(lang, "name", "Spellbook")} ===\n\n").color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.BOLD))
                .append(Component.text(getLocalized(lang, "empty_message", "No spells recorded.")).color(NamedTextColor.BLACK))
                .build()
            newMeta.addPages(emptyPage)
        } else {
            // Create table of contents page
            val tocPage = createTableOfContents(spells, lang)
            newMeta.addPages(tocPage)

            // Create pages for each circle (max 2 spells per page to avoid overflow)
            SpellCircle.entries.forEach { circle ->
                val circleSpells = spells.filter { it.circle == circle }.sortedBy { it.displayName }
                if (circleSpells.isNotEmpty()) {
                    val circlePages = createCirclePages(circle, circleSpells, lang)
                    circlePages.forEach { page -> newMeta.addPages(page) }
                }
            }
        }

        spellbook.itemMeta = newMeta
    }

    /**
     * Create table of contents page
     */
    private fun createTableOfContents(spells: Set<SpellType>, lang: Language): Component {
        val builder = Component.text()
            .append(Component.text("=== ${getLocalized(lang, "name", "Spellbook")} ===\n").color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.BOLD))
            .append(Component.text("${getLocalized(lang, "spells_count", "Spells: {count}").replace("{count}", spells.size.toString())}\n\n").color(NamedTextColor.DARK_BLUE))

        // List circles with spell count
        SpellCircle.entries.forEach { circle ->
            val count = spells.count { it.circle == circle }
            if (count > 0) {
                val circleName = getCircleName(lang, circle)
                builder.append(
                    Component.text("$circleName ($count)\n")
                        .color(NamedTextColor.BLACK)
                )
            }
        }

        builder.append(Component.text("\n"))
        builder.append(Component.text(getLocalized(lang, "click_hint", "Click spell to cast")).color(NamedTextColor.DARK_RED).decorate(TextDecoration.ITALIC))

        return builder.build()
    }

    /**
     * Create pages for a spell circle with clickable spells (max 2 spells per page)
     */
    private fun createCirclePages(circle: SpellCircle, spells: List<SpellType>, lang: Language): List<Component> {
        val pages = mutableListOf<Component>()
        val spellsPerPage = 2

        spells.chunked(spellsPerPage).forEachIndexed { index, pageSpells ->
            val builder = Component.text()
                .append(Component.text("${getCircleName(lang, circle)}").color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.BOLD))

            // Show page number if multiple pages
            if (spells.size > spellsPerPage) {
                builder.append(Component.text(" (${index + 1})").color(NamedTextColor.GRAY))
            }
            builder.append(Component.text("\n"))
            builder.append(Component.text("Mana: ${circle.baseMana}\n\n").color(NamedTextColor.DARK_BLUE))

            pageSpells.forEach { spell ->
                // Spell name (clickable)
                val spellComponent = Component.text("[${spell.displayName}]")
                    .color(NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/cast ${spell.displayName}"))
                    .hoverEvent(HoverEvent.showText(
                        Component.text()
                            .append(Component.text("${getLocalized(lang, "click_to_cast", "Click to cast")}\n").color(NamedTextColor.DARK_GREEN))
                            .append(Component.text("/cast ${spell.displayName}").color(NamedTextColor.BLACK))
                            .build()
                    ))

                builder.append(spellComponent)
                builder.append(Component.text("\n"))

                // Spell description
                val description = getSpellDescription(lang, spell)
                builder.append(Component.text("  $description\n").color(NamedTextColor.BLACK))

                // Power Words (UO-style incantation)
                val powerWordsLabel = getLocalized(lang, "power_words_label", "Incantation")
                builder.append(
                    Component.text("  $powerWordsLabel: ")
                        .color(NamedTextColor.DARK_PURPLE)
                        .append(
                            Component.text(spell.powerWords)
                                .color(NamedTextColor.LIGHT_PURPLE)
                                .decorate(TextDecoration.ITALIC)
                        )
                        .append(Component.text("\n"))
                )

                // Reagents (group and count duplicates)
                val reagentCounts = spell.reagents.groupingBy { it }.eachCount()
                val reagentsText = reagentCounts.entries.joinToString(", ") { (material, count) ->
                    val name = getMaterialName(lang, material)
                    if (count > 1) "$name x$count" else name
                }
                builder.append(Component.text("  ${getLocalized(lang, "reagents_label", "Reagents")}: $reagentsText\n\n").color(NamedTextColor.DARK_BLUE))
            }

            pages.add(builder.build())
        }

        return pages
    }

    /**
     * Create a spellbook with specific spells for a player
     */
    fun createSpellbookWith(player: Player, spells: Set<SpellType>): ItemStack {
        val book = createSpellbook(player)
        spells.forEach { addSpell(book, it) }
        return book
    }

    /**
     * Create a spellbook with specific spells (default language)
     */
    fun createSpellbookWith(spells: Set<SpellType>): ItemStack {
        val book = createSpellbook()
        spells.forEach { addSpell(book, it) }
        return book
    }

    /**
     * Create a full spellbook (all spells)
     */
    fun createFullSpellbook(): ItemStack {
        return createSpellbookWith(SpellType.entries.toSet())
    }

    /**
     * Create a full spellbook for a player
     */
    fun createFullSpellbook(player: Player): ItemStack {
        return createSpellbookWith(player, SpellType.entries.toSet())
    }

    /**
     * Reload localization files
     */
    fun reload() {
        localization.clear()
        loadLocalization()
    }
}
