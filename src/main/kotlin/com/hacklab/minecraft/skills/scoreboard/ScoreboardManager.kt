package com.hacklab.minecraft.skills.scoreboard

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatLockMode
import com.hacklab.minecraft.skills.skill.StatType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ScoreboardManager(private val plugin: Skills) {

    private val playerScoreboards: MutableMap<UUID, Scoreboard> = ConcurrentHashMap()
    private var updateTask: BukkitRunnable? = null

    /**
     * Start the scoreboard update task
     */
    fun startUpdateTask() {
        updateTask = object : BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { player ->
                    updateScoreboard(player)
                }
            }
        }
        // Update every second (20 ticks)
        updateTask?.runTaskTimer(plugin, 20L, 20L)
    }

    /**
     * Stop the scoreboard update task
     */
    fun stopUpdateTask() {
        updateTask?.cancel()
        updateTask = null
    }

    /**
     * Create or update scoreboard for a player
     */
    fun updateScoreboard(player: Player) {
        val data = plugin.playerDataManager.getPlayerData(player)
        val scoreboard = getOrCreateScoreboard(player)

        // mainScoreboardのチーム情報を同期（他プラグインの更新を反映）
        copyTeamsFromMainScoreboard(scoreboard)

        // Get or create objective
        var objective = scoreboard.getObjective("skills_stats")
        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                "skills_stats",
                Criteria.DUMMY,
                Component.text("Stats").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            )
            objective.displaySlot = DisplaySlot.SIDEBAR
        }

        // Clear old scores
        scoreboard.entries.forEach { entry ->
            scoreboard.resetScores(entry)
        }

        // Add stats (scores are displayed in descending order, higher = top)
        var score = 14

        // Player title (UO-style, always English, split into 2 lines)
        val titleParts = plugin.skillTitleManager.getPlayerTitleParts(player, useJapanese = false)
        if (titleParts != null) {
            setScore(objective, titleParts.first, score--)   // e.g., "Grandmaster"
            setScore(objective, titleParts.second, score--)  // e.g., "Swordsman"
        } else {
            setScore(objective, "Adventurer", score--)
            score-- // Skip a line
        }
        setScore(objective, "", score--) // Empty line separator

        // H/M/S/G (Health, Mana, Stamina, Gold)
        val hp = data.internalHp.toInt()
        val maxHp = data.maxInternalHp.toInt()
        setScore(objective, "H: $hp/$maxHp", score--)

        val mana = data.mana.toInt()
        val maxMana = data.maxMana.toInt()
        setScore(objective, "M: $mana/$maxMana", score--)

        val stamina = data.stamina.toInt()
        val maxStamina = data.maxStamina.toInt()
        setScore(objective, "S: $stamina/$maxStamina", score--)

        // Show balance if economy is enabled
        if (plugin.skillsConfig.economyShowOnScoreboard && plugin.vaultHook.isEnabled()) {
            val balance = plugin.vaultHook.getBalance(player)
            val formatted = plugin.vaultHook.format(balance)
            setScore(objective, "G: $formatted", score--)
        }

        setScore(objective, " ", score--) // Empty line separator (with space to be unique)

        // Stat total
        val totalStats = data.getTotalStats()
        setScore(objective, "Stats: $totalStats/${StatType.TOTAL_STAT_CAP}", score--)

        // STR with lock icon
        val str = data.str
        val strLock = getLockIcon(data.strLock)
        setScore(objective, "STR: $str $strLock", score--)

        // DEX with lock icon
        val dex = data.dex
        val dexLock = getLockIcon(data.dexLock)
        setScore(objective, "DEX: $dex $dexLock", score--)

        // INT with lock icon
        val intVal = data.int
        val intLock = getLockIcon(data.intLock)
        setScore(objective, "INT: $intVal $intLock", score--)

        // Apply scoreboard to player
        player.scoreboard = scoreboard
    }

    private fun setScore(objective: Objective, text: String, score: Int) {
        objective.getScore(text).score = score
    }

    private fun getLockIcon(mode: StatLockMode): String {
        return when (mode) {
            StatLockMode.UP -> "+"
            StatLockMode.DOWN -> "-"
            StatLockMode.LOCKED -> "X"
        }
    }

    private fun getOrCreateScoreboard(player: Player): Scoreboard {
        return playerScoreboards.getOrPut(player.uniqueId) {
            val newBoard = Bukkit.getScoreboardManager().newScoreboard
            // mainScoreboardのチーム情報をコピー（他プラグインとの互換性のため）
            copyTeamsFromMainScoreboard(newBoard)
            newBoard
        }
    }

    /**
     * mainScoreboardのチーム情報とBELOW_NAME Objectiveを新しいスコアボードにコピー
     */
    private fun copyTeamsFromMainScoreboard(targetBoard: Scoreboard) {
        val mainBoard = Bukkit.getScoreboardManager().mainScoreboard

        // チーム情報をコピー
        mainBoard.teams.forEach { mainTeam ->
            val team = targetBoard.getTeam(mainTeam.name) ?: targetBoard.registerNewTeam(mainTeam.name)
            (mainTeam.color() as? NamedTextColor)?.let { team.color(it) }
            team.prefix(mainTeam.prefix())
            team.suffix(mainTeam.suffix())
            mainTeam.entries.forEach { entry ->
                if (!team.hasEntry(entry)) {
                    team.addEntry(entry)
                }
            }
        }

        // BELOW_NAME Objectiveをコピー（他プラグインの称号表示用）
        mainBoard.getObjective(DisplaySlot.BELOW_NAME)?.let { mainObjective ->
            val objective = targetBoard.getObjective(mainObjective.name)
                ?: targetBoard.registerNewObjective(
                    mainObjective.name,
                    mainObjective.trackedCriteria,
                    mainObjective.displayName()
                )
            objective.displaySlot = DisplaySlot.BELOW_NAME

            // スコアとnumberFormatをコピー
            mainObjective.scoreboard?.entries?.forEach { entry ->
                val mainScore = mainObjective.getScore(entry)
                if (mainScore.isScoreSet) {
                    val score = objective.getScore(entry)
                    score.score = mainScore.score
                    mainScore.numberFormat()?.let { score.numberFormat(it) }
                }
            }
        }
    }

    /**
     * 全プレイヤーのスコアボードにチーム情報を再同期
     */
    fun syncTeamsToAllPlayers() {
        playerScoreboards.values.forEach { board ->
            copyTeamsFromMainScoreboard(board)
        }
    }

    /**
     * Get the scoreboard for a player
     */
    fun getScoreboard(player: Player): Scoreboard? {
        return playerScoreboards[player.uniqueId]
    }

    /**
     * Remove scoreboard for a player
     */
    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    /**
     * Toggle scoreboard visibility for a player
     */
    fun toggleScoreboard(player: Player): Boolean {
        val scoreboard = playerScoreboards[player.uniqueId]
        return if (scoreboard != null && player.scoreboard == scoreboard) {
            // Hide scoreboard
            player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
            false
        } else {
            // Show scoreboard
            updateScoreboard(player)
            true
        }
    }

    /**
     * Check if player has scoreboard visible
     */
    fun isScoreboardVisible(player: Player): Boolean {
        val scoreboard = playerScoreboards[player.uniqueId]
        return scoreboard != null && player.scoreboard == scoreboard
    }

    /**
     * Cleanup when player leaves
     */
    fun cleanup(playerId: UUID) {
        playerScoreboards.remove(playerId)
    }
}
