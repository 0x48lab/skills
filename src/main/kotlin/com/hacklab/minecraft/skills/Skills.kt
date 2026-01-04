package com.hacklab.minecraft.skills

import com.hacklab.minecraft.skills.combat.CombatManager
import com.hacklab.minecraft.skills.combat.InternalHealthManager
import com.hacklab.minecraft.skills.command.*
import com.hacklab.minecraft.skills.armor.ArmorManager
import com.hacklab.minecraft.skills.config.ArmorConfig
import com.hacklab.minecraft.skills.config.CombatConfig
import com.hacklab.minecraft.skills.config.SkillsConfig
import com.hacklab.minecraft.skills.economy.ChunkLimitManager
import com.hacklab.minecraft.skills.economy.MobRewardListener
import com.hacklab.minecraft.skills.economy.MobRewardManager
import com.hacklab.minecraft.skills.economy.VaultHook
import com.hacklab.minecraft.skills.crafting.CraftingManager
import com.hacklab.minecraft.skills.crafting.DurabilityManager
import com.hacklab.minecraft.skills.crafting.QualityManager
import com.hacklab.minecraft.skills.crafting.StackBonusManager
import com.hacklab.minecraft.skills.data.PlayerDataManager
import com.hacklab.minecraft.skills.database.Database
import com.hacklab.minecraft.skills.database.SQLiteDatabase
import com.hacklab.minecraft.skills.gathering.GatheringManager
import com.hacklab.minecraft.skills.guide.GuideManager
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.i18n.MessageManager
import com.hacklab.minecraft.skills.i18n.MessageSender
import com.hacklab.minecraft.skills.i18n.PlayerLocaleManager
import com.hacklab.minecraft.skills.listener.*
import com.hacklab.minecraft.skills.magic.*
import com.hacklab.minecraft.skills.skill.SkillManager
import com.hacklab.minecraft.skills.skill.SkillTitleManager
import com.hacklab.minecraft.skills.taming.AnimalLoreManager
import com.hacklab.minecraft.skills.taming.TamingManager
import com.hacklab.minecraft.skills.taming.VeterinaryManager
import com.hacklab.minecraft.skills.thief.*
import com.hacklab.minecraft.skills.scoreboard.ScoreboardManager
import com.hacklab.minecraft.skills.stamina.StaminaManager
import com.hacklab.minecraft.skills.util.CooldownManager
import com.hacklab.minecraft.skills.vengeful.VengefulMobsListener
import com.hacklab.minecraft.skills.vengeful.VengefulMobsManager
import com.hacklab.minecraft.skills.integration.NotorietyIntegration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class Skills : JavaPlugin() {

    // Config
    lateinit var skillsConfig: SkillsConfig
        private set
    lateinit var combatConfig: CombatConfig
        private set
    lateinit var armorConfig: ArmorConfig
        private set

    // Database
    lateinit var database: Database
        private set

    // i18n
    lateinit var messageManager: MessageManager
        private set
    lateinit var localeManager: PlayerLocaleManager
        private set
    lateinit var messageSender: MessageSender
        private set

    // Data
    lateinit var playerDataManager: PlayerDataManager
        private set

    // Skills
    lateinit var skillManager: SkillManager
        private set
    lateinit var skillTitleManager: SkillTitleManager
        private set

    // Combat
    lateinit var combatManager: CombatManager
        private set
    lateinit var healthManager: InternalHealthManager
        private set
    lateinit var armorManager: ArmorManager
        private set

    // Magic
    lateinit var spellManager: SpellManager
        private set
    lateinit var castingManager: CastingManager
        private set
    lateinit var targetManager: TargetManager
        private set
    lateinit var manaManager: ManaManager
        private set
    lateinit var reagentManager: ReagentManager
        private set
    lateinit var spellbookManager: SpellbookManager
        private set
    lateinit var scrollManager: ScrollManager
        private set
    lateinit var runeManager: RuneManager
        private set
    lateinit var gateManager: GateManager
        private set
    lateinit var runebookManager: RunebookManager
        private set

    // Crafting
    lateinit var craftingManager: CraftingManager
        private set
    lateinit var qualityManager: QualityManager
        private set
    lateinit var durabilityManager: DurabilityManager
        private set
    lateinit var stackBonusManager: StackBonusManager
        private set

    // Gathering
    lateinit var gatheringManager: GatheringManager
        private set

    // Thief
    lateinit var hidingManager: HidingManager
        private set
    lateinit var detectingManager: DetectingManager
        private set
    lateinit var snoopingManager: SnoopingManager
        private set
    lateinit var stealingManager: StealingManager
        private set
    lateinit var poisonItemManager: PoisonItemManager
        private set
    lateinit var poisoningManager: PoisoningManager
        private set

    // Taming
    lateinit var tamingManager: TamingManager
        private set
    lateinit var animalLoreManager: AnimalLoreManager
        private set
    lateinit var veterinaryManager: VeterinaryManager
        private set

    // Utility
    lateinit var cooldownManager: CooldownManager
        private set

    // Guide
    lateinit var guideManager: GuideManager
        private set

    // VengefulMobs
    lateinit var vengefulMobsManager: VengefulMobsManager
        private set

    // Scoreboard
    lateinit var scoreboardManager: ScoreboardManager
        private set

    // Stamina
    lateinit var staminaManager: StaminaManager
        private set

    // Economy
    lateinit var vaultHook: VaultHook
        private set
    lateinit var chunkLimitManager: ChunkLimitManager
        private set
    lateinit var mobRewardManager: MobRewardManager
        private set

    // Integration
    lateinit var notorietyIntegration: NotorietyIntegration
        private set

    // Listeners
    private lateinit var meditationListener: MeditationListener
    lateinit var survivalListener: SurvivalListener
        private set
    lateinit var runebookListener: RunebookListener
        private set

    // Sleep
    lateinit var sleepCommand: SleepCommand
        private set

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()

        // Initialize config
        skillsConfig = SkillsConfig(this)
        combatConfig = CombatConfig(this)
        armorConfig = ArmorConfig(this)

        // Initialize i18n
        messageManager = MessageManager(this)
        messageManager.loadLanguages()
        localeManager = PlayerLocaleManager(this)
        messageSender = MessageSender(this)

        // Initialize database
        database = SQLiteDatabase(this)
        database.connect()
        database.createTables()

        // Initialize data manager
        playerDataManager = PlayerDataManager(this, database)

        // Initialize managers
        initializeManagers()

        // Register listeners
        registerListeners()

        // Register commands
        registerCommands()

        // Start scheduled tasks
        startScheduledTasks()

        // Load online players (for reload)
        server.onlinePlayers.forEach { player ->
            playerDataManager.loadPlayer(player)
        }

        messageSender.log(MessageKey.SYSTEM_PLUGIN_ENABLED)
        logger.info("Skills plugin enabled!")
    }

    override fun onDisable() {
        // Save all player data
        playerDataManager.saveAllPlayers()

        // Cleanup listeners
        if (::meditationListener.isInitialized) {
            meditationListener.cleanup()
        }

        // Cleanup scoreboard
        if (::scoreboardManager.isInitialized) {
            scoreboardManager.stopUpdateTask()
        }

        // Cleanup stamina
        if (::staminaManager.isInitialized) {
            staminaManager.stopUpdateTask()
        }

        // Disconnect database
        database.disconnect()

        messageSender.log(MessageKey.SYSTEM_PLUGIN_DISABLED)
        logger.info("Skills plugin disabled!")
    }

    private fun initializeManagers() {
        // Utility
        cooldownManager = CooldownManager(this)

        // Skills
        skillManager = SkillManager(this)
        skillTitleManager = SkillTitleManager(this)

        // Combat
        combatManager = CombatManager(this)
        healthManager = InternalHealthManager(this)
        armorManager = ArmorManager(this)

        // Magic
        targetManager = TargetManager(this)
        manaManager = ManaManager(this)
        reagentManager = ReagentManager(this)
        spellbookManager = SpellbookManager(this)
        spellbookManager.loadLocalization()
        scrollManager = ScrollManager(this)
        runeManager = RuneManager(this)
        gateManager = GateManager(this)
        runebookManager = RunebookManager(this)
        castingManager = CastingManager(this)
        spellManager = SpellManager(this)

        // Crafting
        qualityManager = QualityManager(this)
        craftingManager = CraftingManager(this)
        durabilityManager = DurabilityManager(this)
        stackBonusManager = StackBonusManager(this)

        // Gathering
        gatheringManager = GatheringManager(this)

        // Thief
        hidingManager = HidingManager(this)
        detectingManager = DetectingManager(this)
        snoopingManager = SnoopingManager(this)
        stealingManager = StealingManager(this)
        poisonItemManager = PoisonItemManager(this)
        poisoningManager = PoisoningManager(this)

        // Taming
        tamingManager = TamingManager(this)
        animalLoreManager = AnimalLoreManager(this)
        veterinaryManager = VeterinaryManager(this)

        // Guide
        guideManager = GuideManager(this)
        guideManager.loadGuides()

        // VengefulMobs
        vengefulMobsManager = VengefulMobsManager(this)

        // Scoreboard
        scoreboardManager = ScoreboardManager(this)

        // Stamina
        staminaManager = StaminaManager(this)

        // Economy (Vault integration)
        vaultHook = VaultHook(this)
        if (vaultHook.setup()) {
            logger.info("Vault economy integration enabled")
        }
        chunkLimitManager = ChunkLimitManager(this)
        mobRewardManager = MobRewardManager(this)

        // Integration (Notoriety plugin)
        notorietyIntegration = NotorietyIntegration(this)
        notorietyIntegration.initialize()
    }

    private fun registerListeners() {
        val pm = server.pluginManager

        pm.registerEvents(PlayerListener(this), this)
        pm.registerEvents(CombatListener(this), this)
        pm.registerEvents(ArmorListener(this), this)
        pm.registerEvents(CraftingListener(this), this)
        pm.registerEvents(GatheringListener(this), this)
        pm.registerEvents(TargetingListener(this), this)
        pm.registerEvents(ThiefListener(this), this)

        meditationListener = MeditationListener(this)
        pm.registerEvents(meditationListener, this)

        survivalListener = SurvivalListener(this)
        pm.registerEvents(survivalListener, this)

        // VengefulMobs
        pm.registerEvents(VengefulMobsListener(this), this)

        // Scroll loot (mob drops and End chest)
        pm.registerEvents(ScrollLootListener(this), this)

        // Librarian trades (spellbook and scrolls)
        pm.registerEvents(LibrarianTradeListener(this), this)

        // Mob reward (economy)
        pm.registerEvents(MobRewardListener(this), this)

        // Mending integration with Blacksmithy
        pm.registerEvents(MendingListener(this), this)

        // Food consumption (Cooking skill bonus)
        pm.registerEvents(FoodListener(this), this)

        // Stack size bonus based on production skills
        pm.registerEvents(StackBonusListener(this), this)

        // Runebook GUI
        runebookListener = RunebookListener(this)
        pm.registerEvents(runebookListener, this)

        // Sleep
        pm.registerEvents(SleepListener(this), this)
    }

    private fun registerCommands() {
        // Player commands
        getCommand("skills")?.setExecutor(SkillsCommand(this))
        val statsCmd = StatsCommand(this)
        getCommand("stats")?.setExecutor(statsCmd)
        getCommand("stats")?.tabCompleter = statsCmd
        getCommand("cast")?.setExecutor(CastCommand(this))
        getCommand("language")?.setExecutor(LanguageCommand(this))

        // Thief commands
        getCommand("hide")?.setExecutor(HideCommand(this))
        getCommand("detect")?.setExecutor(DetectCommand(this))
        getCommand("snoop")?.setExecutor(SnoopCommand(this))
        getCommand("poison")?.setExecutor(PoisonCommand(this))

        // Taming commands
        getCommand("tame")?.setExecutor(TameCommand(this))
        getCommand("lore")?.setExecutor(LoreCommand(this))
        getCommand("evaluate")?.setExecutor(EvaluateCommand(this))

        // Admin commands
        val adminCmd = SkillAdminCommand(this)
        getCommand("skilladmin")?.setExecutor(adminCmd)
        getCommand("skilladmin")?.tabCompleter = adminCmd

        // Crafting commands
        getCommand("arms")?.setExecutor(ArmsCommand(this))
        getCommand("scribe")?.setExecutor(ScribeCommand(this))

        // Sleep command
        sleepCommand = SleepCommand(this)
        getCommand("sleep")?.setExecutor(sleepCommand)
    }

    private fun startScheduledTasks() {
        // Auto-save task
        if (skillsConfig.autoSaveEnabled) {
            object : BukkitRunnable() {
                override fun run() {
                    playerDataManager.saveAllPlayers()
                    if (skillsConfig.debugMode) {
                        logger.info("Auto-saved player data")
                    }
                }
            }.runTaskTimerAsynchronously(
                this,
                skillsConfig.autoSaveInterval * 20L,
                skillsConfig.autoSaveInterval * 20L
            )
        }

        // Targeting timeout cleanup
        object : BukkitRunnable() {
            override fun run() {
                targetManager.cleanupExpired()
            }
        }.runTaskTimer(this, 20L, 20L) // Every second

        // Cooldown cleanup (every minute)
        object : BukkitRunnable() {
            override fun run() {
                cooldownManager.cleanupExpiredCooldowns()
            }
        }.runTaskTimerAsynchronously(this, 1200L, 1200L) // Every minute

        // VengefulMobs aggression task
        vengefulMobsManager.startAggressionTask()

        // Scoreboard update task
        scoreboardManager.startUpdateTask()

        // Stamina update task
        staminaManager.startUpdateTask()

        // Chunk limit cleanup (every 5 minutes)
        object : BukkitRunnable() {
            override fun run() {
                chunkLimitManager.cleanupExpired()
            }
        }.runTaskTimerAsynchronously(this, 6000L, 6000L) // Every 5 minutes
    }
}
