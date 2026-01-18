package com.hacklab.minecraft.skills.i18n

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration
import java.util.logging.Level

class MessageSender(private val plugin: Skills) {

    private val messageManager: MessageManager get() = plugin.messageManager
    private val localeManager: PlayerLocaleManager get() = plugin.localeManager

    /**
     * Send a chat message to a player in their language
     */
    fun send(player: Player, key: MessageKey, vararg placeholders: Pair<String, Any>) {
        val lang = localeManager.getLanguage(player)
        val component = messageManager.getComponent(key, lang, *placeholders)
        player.sendMessage(component)
    }

    /**
     * Send a chat message to a command sender (player or console)
     * For players, uses their language setting. For console, uses English.
     */
    fun send(sender: org.bukkit.command.CommandSender, key: MessageKey, vararg placeholders: Pair<String, Any>) {
        val lang = if (sender is Player) localeManager.getLanguage(sender) else Language.ENGLISH
        val component = messageManager.getComponent(key, lang, *placeholders)
        sender.sendMessage(component)
    }

    /**
     * Send a raw string message to a player
     */
    fun sendRaw(player: Player, message: String) {
        player.sendMessage(Component.text(message))
    }

    /**
     * Send a raw string message to a command sender (player or console)
     */
    fun sendRaw(sender: org.bukkit.command.CommandSender, message: String) {
        sender.sendMessage(Component.text(message))
    }

    /**
     * Send an action bar message to a player
     */
    fun sendActionBar(player: Player, key: MessageKey, vararg placeholders: Pair<String, Any>) {
        val lang = localeManager.getLanguage(player)
        val component = messageManager.getComponent(key, lang, *placeholders)
        player.sendActionBar(component)
    }

    /**
     * Send a title to a player
     */
    fun sendTitle(
        player: Player,
        titleKey: MessageKey?,
        subtitleKey: MessageKey?,
        fadeIn: Duration = Duration.ofMillis(500),
        stay: Duration = Duration.ofMillis(3500),
        fadeOut: Duration = Duration.ofMillis(1000),
        vararg placeholders: Pair<String, Any>
    ) {
        val lang = localeManager.getLanguage(player)
        val title = titleKey?.let { messageManager.getComponent(it, lang, *placeholders) } ?: Component.empty()
        val subtitle = subtitleKey?.let { messageManager.getComponent(it, lang, *placeholders) } ?: Component.empty()

        val times = Title.Times.times(fadeIn, stay, fadeOut)
        player.showTitle(Title.title(title, subtitle, times))
    }

    /**
     * Broadcast a message to all online players (each in their language)
     */
    fun broadcast(key: MessageKey, vararg placeholders: Pair<String, Any>) {
        Bukkit.getOnlinePlayers().forEach { player ->
            send(player, key, *placeholders)
        }
    }

    /**
     * Broadcast a message to players with a specific permission
     */
    fun broadcastWithPermission(permission: String, key: MessageKey, vararg placeholders: Pair<String, Any>) {
        Bukkit.getOnlinePlayers()
            .filter { it.hasPermission(permission) }
            .forEach { player ->
                send(player, key, *placeholders)
            }
    }

    /**
     * Log a message to console (uses default language)
     */
    fun log(key: MessageKey, vararg placeholders: Pair<String, Any>) {
        val message = messageManager.get(key, Language.ENGLISH, *placeholders)
        plugin.logger.info(message)
    }

    /**
     * Log a message with a specific level
     */
    fun log(level: Level, key: MessageKey, vararg placeholders: Pair<String, Any>) {
        val message = messageManager.get(key, Language.ENGLISH, *placeholders)
        plugin.logger.log(level, message)
    }

    /**
     * Get a formatted message string for a player
     */
    fun format(player: Player, key: MessageKey, vararg placeholders: Pair<String, Any>): String {
        val lang = localeManager.getLanguage(player)
        return messageManager.get(key, lang, *placeholders)
    }

    /**
     * Get a component for a player
     */
    fun formatComponent(player: Player, key: MessageKey, vararg placeholders: Pair<String, Any>): Component {
        val lang = localeManager.getLanguage(player)
        return messageManager.getComponent(key, lang, *placeholders)
    }
}
