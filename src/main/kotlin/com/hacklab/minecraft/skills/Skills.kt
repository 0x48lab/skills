package com.hacklab.minecraft.skills

import com.hacklab.minecraft.skills.combat.CombatManager
import com.hacklab.minecraft.skills.combat.InternalHealthManager
import com.hacklab.minecraft.skills.command.*
import com.hacklab.minecraft.skills.config.SkillsConfig
import com.hacklab.minecraft.skills.crafting.CraftingManager
import com.hacklab.minecraft.skills.crafting.QualityManager
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
import com.hacklab.minecraft.skills.taming.AnimalLoreManager
import com.hacklab.minecraft.skills.taming.TamingManager
import com.hacklab.minecraft.skills.taming.VeterinaryManager
import com.hacklab.minecraft.skills.thief.*
import com.hacklab.minecraft.skills.util.CooldownManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class Skills : JavaPlugin() {

    // Config
    lateinit var skillsConfig: SkillsConfig
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

    // Combat
    lateinit var combatManager: CombatManager
        private set
    lateinit var healthManager: InternalHealthManager
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

    // Crafting
    lateinit var craftingManager: CraftingManager
        private set
    lateinit var qualityManager: QualityManager
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

    // Listeners
    private lateinit var meditationListener: MeditationListener
    lateinit var survivalListener: SurvivalListener
        private set

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()

        // Initialize config
        skillsConfig = SkillsConfig(this)

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

        // Combat
        combatManager = CombatManager(this)
        healthManager = InternalHealthManager(this)

        // Magic
        targetManager = TargetManager(this)
        manaManager = ManaManager(this)
        reagentManager = ReagentManager(this)
        spellbookManager = SpellbookManager(this)
        spellbookManager.loadLocalization()
        scrollManager = ScrollManager(this)
        runeManager = RuneManager(this)
        gateManager = GateManager(this)
        castingManager = CastingManager(this)
        spellManager = SpellManager(this)

        // Crafting
        qualityManager = QualityManager(this)
        craftingManager = CraftingManager(this)

        // Gathering
        gatheringManager = GatheringManager(this)

        // Thief
        hidingManager = HidingManager(this)
        detectingManager = DetectingManager(this)
        snoopingManager = SnoopingManager(this)
        stealingManager = StealingManager(this)
        poisoningManager = PoisoningManager(this)

        // Taming
        tamingManager = TamingManager(this)
        animalLoreManager = AnimalLoreManager(this)
        veterinaryManager = VeterinaryManager(this)

        // Guide
        guideManager = GuideManager(this)
        guideManager.loadGuides()
    }

    private fun registerListeners() {
        val pm = server.pluginManager

        pm.registerEvents(PlayerListener(this), this)
        pm.registerEvents(CombatListener(this), this)
        pm.registerEvents(CraftingListener(this), this)
        pm.registerEvents(GatheringListener(this), this)
        pm.registerEvents(TargetingListener(this), this)

        meditationListener = MeditationListener(this)
        pm.registerEvents(meditationListener, this)

        survivalListener = SurvivalListener(this)
        pm.registerEvents(survivalListener, this)
    }

    private fun registerCommands() {
        // Player commands
        getCommand("skills")?.setExecutor(SkillsCommand(this))
        getCommand("stats")?.setExecutor(StatsCommand(this))
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

        // Book commands
        val spellbookCmd = SpellbookCommand(this)
        getCommand("spellbook")?.setExecutor(spellbookCmd)
        getCommand("spellbook")?.tabCompleter = spellbookCmd
        getCommand("skillbook")?.setExecutor(SkillbookCommand(this))

        // Crafting commands
        getCommand("arms")?.setExecutor(ArmsCommand(this))
        getCommand("scribe")?.setExecutor(ScribeCommand(this))

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
    }
}
