package com.hacklab.minecraft.skills.i18n

enum class MessageKey(val path: String) {
    // System
    SYSTEM_PLUGIN_ENABLED("system.plugin_enabled"),
    SYSTEM_PLUGIN_DISABLED("system.plugin_disabled"),
    SYSTEM_PLAYER_DATA_LOADED("system.player_data_loaded"),
    SYSTEM_PLAYER_DATA_SAVED("system.player_data_saved"),
    SYSTEM_NO_PERMISSION("system.no_permission"),
    SYSTEM_PLAYER_ONLY("system.player_only"),
    SYSTEM_PLAYER_NOT_FOUND("system.player_not_found"),
    SYSTEM_INVALID_AMOUNT("system.invalid_amount"),
    SYSTEM_RELOAD_SUCCESS("system.reload_success"),

    // Skill
    SKILL_GAIN("skill.gain"),
    SKILL_DECREASE("skill.decrease"),
    SKILL_CAP_REACHED("skill.cap_reached"),
    SKILL_LIST_HEADER("skill.list_header"),
    SKILL_LIST_ENTRY("skill.list_entry"),
    SKILL_SET("skill.set"),
    SKILL_RESET("skill.reset"),

    // Stats
    STATS_HEADER("stats.header"),
    STATS_HP("stats.hp"),
    STATS_MANA("stats.mana"),
    STATS_STR("stats.str"),
    STATS_DEX("stats.dex"),
    STATS_INT("stats.int"),
    STATS_TOTAL_SKILLS("stats.total_skills"),
    STAT_GAIN("stats.gain"),
    STAT_DECREASE("stats.decrease"),
    STAT_LOCK_CHANGED("stats.lock_changed"),

    // Magic - General
    MAGIC_CAST_START("magic.cast_start"),
    MAGIC_TARGETING("magic.targeting"),
    MAGIC_CAST_SUCCESS("magic.cast_success"),
    MAGIC_CAST_FAILED("magic.cast_failed"),
    MAGIC_CANCELLED("magic.cancelled"),
    MAGIC_TARGET_TIMEOUT("magic.target_timeout"),
    MAGIC_SELECT_TARGET("magic.select_target"),
    MAGIC_INVALID_TARGET("magic.invalid_target"),
    MAGIC_NO_SPELLBOOK("magic.no_spellbook"),
    MAGIC_SPELL_NOT_IN_BOOK("magic.spell_not_in_book"),
    MAGIC_NO_REAGENTS("magic.no_reagents"),
    MAGIC_NO_MANA("magic.no_mana"),
    MAGIC_OUT_OF_RANGE("magic.out_of_range"),
    MAGIC_INTERRUPTED("magic.interrupted"),

    // Heal spells
    HEAL_TARGET("heal.target"),
    HEAL_SELF("heal.self"),
    HEAL_AMOUNT("heal.amount"),

    // Teleport spells
    TELEPORT_MARK_SUCCESS("teleport.mark_success"),
    TELEPORT_RECALL_SUCCESS("teleport.recall_success"),
    TELEPORT_GATE_OPEN("teleport.gate_open"),
    TELEPORT_NO_RUNE("teleport.no_rune"),
    TELEPORT_RUNE_NOT_MARKED("teleport.rune_not_marked"),

    // Buff spells
    MAGIC_AGILITY_CAST("magic.agility_cast"),
    MAGIC_STRENGTH_CAST("magic.strength_cast"),
    MAGIC_CUNNING_CAST("magic.cunning_cast"),

    // Debuff spells
    MAGIC_CLUMSY_CAST("magic.clumsy_cast"),
    MAGIC_WEAKEN_CAST("magic.weaken_cast"),
    MAGIC_CURSE_CAST("magic.curse_cast"),
    MAGIC_MASS_CURSE_CAST("magic.mass_curse_cast"),
    MAGIC_POISON_CAST("magic.poison_cast"),

    // Utility spells
    MAGIC_MANA_DRAIN_CAST("magic.mana_drain_cast"),
    MAGIC_MANA_VAMPIRE_CAST("magic.mana_vampire_cast"),
    MAGIC_DISPEL_CAST("magic.dispel_cast"),
    MAGIC_MASS_DISPEL_CAST("magic.mass_dispel_cast"),
    MAGIC_REVEAL_CAST("magic.reveal_cast"),
    MAGIC_REVEAL_FOUND("magic.reveal_found"),
    MAGIC_REVEAL_NONE("magic.reveal_none"),
    MAGIC_ARCH_CURE_CAST("magic.arch_cure_cast"),

    // Field spells
    MAGIC_WALL_OF_STONE_CAST("magic.wall_of_stone_cast"),
    MAGIC_ENERGY_FIELD_CAST("magic.energy_field_cast"),
    MAGIC_PARALYZE_FIELD_CAST("magic.paralyze_field_cast"),

    // Attack spells
    MAGIC_FLAMESTRIKE_CAST("magic.flamestrike_cast"),
    MAGIC_CHAIN_LIGHTNING_CAST("magic.chain_lightning_cast"),
    MAGIC_EARTHQUAKE_CAST("magic.earthquake_cast"),

    // Summon spells
    MAGIC_SUMMON_CREATURE_CAST("magic.summon_creature_cast"),
    MAGIC_SUMMON_DESPAWNED("magic.summon_despawned"),
    MAGIC_SUMMON_LIMIT("magic.summon_limit"),

    // 8th Circle special
    MAGIC_WORD_OF_DEATH_CAST("magic.word_of_death_cast"),
    MAGIC_WORD_OF_DEATH_FAILED("magic.word_of_death_failed"),
    MAGIC_WORD_OF_DEATH_IMMUNE("magic.word_of_death_immune"),
    MAGIC_MASS_SLEEP_CAST("magic.mass_sleep_cast"),

    // Thief skills
    THIEF_HIDE_SUCCESS("thief.hide_success"),
    THIEF_HIDE_FAILED("thief.hide_failed"),
    THIEF_HIDE_BROKEN("thief.hide_broken"),
    THIEF_STEALTH_DISTANCE("thief.stealth_distance"),
    THIEF_STEALTH_ENDED("thief.stealth_ended"),
    THIEF_DETECT_FOUND("thief.detect_found"),
    THIEF_DETECT_NONE("thief.detect_none"),
    THIEF_SNOOP_SUCCESS("thief.snoop_success"),
    THIEF_SNOOP_FAILED("thief.snoop_failed"),
    THIEF_SNOOP_NOTICED("thief.snoop_noticed"),
    THIEF_STEAL_SUCCESS("thief.steal_success"),
    THIEF_STEAL_FAILED("thief.steal_failed"),
    THIEF_STEAL_NOTICED("thief.steal_noticed"),
    THIEF_STEAL_ITEM_MOVED("thief.steal_item_moved"),
    THIEF_STEAL_TARGET_OFFLINE("thief.steal_target_offline"),
    THIEF_POISON_APPLIED("thief.poison_applied"),
    THIEF_POISON_FAILED("thief.poison_failed"),
    THIEF_POISON_HIT("thief.poison_hit"),
    THIEF_POISON_NO_WEAPON("thief.poison_no_weapon"),
    THIEF_POISON_NO_POTION("thief.poison_no_potion"),

    // Taming
    TAMING_START("taming.start"),
    TAMING_SUCCESS("taming.success"),
    TAMING_FAILED("taming.failed"),
    TAMING_ALREADY_TAMED("taming.already_tamed"),
    TAMING_CANNOT_TAME("taming.cannot_tame"),
    TAMING_TOO_DIFFICULT("taming.too_difficult"),

    // Animal Lore
    LORE_HEADER("lore.header"),
    LORE_OWNER("lore.owner"),
    LORE_HEALTH("lore.health"),
    LORE_TAMEABLE("lore.tameable"),
    LORE_DIFFICULTY("lore.difficulty"),

    // Veterinary
    VETERINARY_HEAL("veterinary.heal"),
    VETERINARY_WRONG_FOOD("veterinary.wrong_food"),
    VETERINARY_NOT_OWNER("veterinary.not_owner"),

    // Evaluate
    EVALUATE_RESULT("evaluate.result"),
    EVALUATE_SELF("evaluate.self"),

    // Crafting
    CRAFTING_SUCCESS("crafting.success"),
    CRAFTING_QUALITY("crafting.quality"),
    CRAFTING_QUALITY_HQ("crafting.quality_hq"),
    CRAFTING_QUALITY_EX("crafting.quality_ex"),
    CRAFTING_QUALITY_SUMMARY("crafting.quality_summary"),

    // Repair
    REPAIR_SUCCESS("repair.success"),

    // Scroll
    SCROLL_CREATED("scroll.created"),
    SCROLL_FAILED("scroll.failed"),
    SCROLL_USED("scroll.used"),
    SCROLL_LEARNED("scroll.learned"),
    SCROLL_ALREADY_KNOWN("scroll.already_known"),

    // Rune
    RUNE_CREATED("rune.created"),
    RUNE_MARKED("rune.marked"),

    // Runebook
    RUNEBOOK_RUNE_ADDED("runebook.rune_added"),
    RUNEBOOK_RUNE_REMOVED("runebook.rune_removed"),
    RUNEBOOK_FULL("runebook.full"),
    RUNEBOOK_NOT_A_RUNE("runebook.not_a_rune"),
    RUNEBOOK_RUNE_NOT_MARKED("runebook.rune_not_marked"),
    RUNEBOOK_WORLD_NOT_FOUND("runebook.world_not_found"),

    // Gate
    GATE_CREATED("gate.created"),
    GATE_TRAVEL_USED("gate.travel_used"),

    // Combat
    COMBAT_PARRY("combat.parry"),
    COMBAT_CRITICAL("combat.critical"),
    COMBAT_RESIST("combat.resist"),
    COMBAT_MISS("combat.miss"),
    COMBAT_STUN("combat.stun"),

    // Armor
    ARMOR_CANNOT_EQUIP_STR("armor.cannot_equip_str"),
    ARMOR_CANNOT_EQUIP_DEX("armor.cannot_equip_dex"),
    ARMOR_REMOVED_STR("armor.removed_str"),
    ARMOR_REMOVED_DEX("armor.removed_dex"),
    ARMOR_DEX_PENALTY_WARNING("armor.dex_penalty_warning"),

    // Gathering
    GATHERING_BONUS_DROP("gathering.bonus_drop"),
    GATHERING_RARE_FIND("gathering.rare_find"),
    GATHERING_CHAIN_CHOP("gathering.chain_chop"),
    GATHERING_AUTO_REPLANT("gathering.auto_replant"),
    GATHERING_AUTO_REPLANT_MATURE("gathering.auto_replant_mature"),
    GATHERING_AUTO_FISHING_RECAST("gathering.auto_fishing_recast"),
    GATHERING_AUTO_FISHING_STOPPED("gathering.auto_fishing_stopped"),
    GATHERING_AUTO_FISHING_FAILED("gathering.auto_fishing_failed"),

    // Arms Lore
    ARMS_LORE_HEADER("arms_lore.header"),
    ARMS_LORE_QUALITY("arms_lore.quality"),
    ARMS_LORE_DAMAGE("arms_lore.damage"),
    ARMS_LORE_DURABILITY("arms_lore.durability"),

    // Admin
    ADMIN_SKILL_SET("admin.skill_set"),
    ADMIN_STATS_RESET("admin.stats_reset"),
    ADMIN_DATA_RELOADED("admin.data_reloaded"),

    // Language
    LANGUAGE_CHANGED("language.changed"),
    LANGUAGE_CURRENT("language.current"),
    LANGUAGE_AVAILABLE("language.available"),
    LANGUAGE_AVAILABLE_LIST("language.available_list"),
    LANGUAGE_DISABLED("language.disabled"),
    LANGUAGE_RESET("language.reset"),
    LANGUAGE_USING_CLIENT("language.using_client"),

    // Guidebook
    GUIDEBOOK_RECEIVED("guidebook.received"),
    GUIDEBOOK_DROPPED("guidebook.dropped"),

    // Survival
    SURVIVAL_ATHLETICS_ROLL("survival.athletics_roll"),
    SURVIVAL_SWIMMING_BREATH("survival.swimming_breath"),
    SURVIVAL_HEAT_RESIST("survival.heat_resist"),
    SURVIVAL_EXTINGUISHED("survival.extinguished"),
    SURVIVAL_WARMED_UP("survival.warmed_up"),
    SURVIVAL_ENDURED("survival.endured"),

    // Cooldown
    COOLDOWN_ACTIVE("cooldown.active"),

    // Economy
    ECONOMY_MOB_REWARD("economy.mob_reward"),
    ECONOMY_CHUNK_LIMIT_REACHED("economy.chunk_limit_reached"),

    // Sleep
    SLEEP_DISABLED("sleep.disabled"),
    SLEEP_NOT_NIGHT("sleep.not_night"),
    SLEEP_WRONG_DIMENSION("sleep.wrong_dimension"),
    SLEEP_NO_SLEEPING_PLAYERS("sleep.no_sleeping_players"),
    SLEEP_ALREADY_SLEEPING("sleep.already_sleeping"),
    SLEEP_STARTED("sleep.started"),
    SLEEP_CANCELLED("sleep.cancelled"),
    SLEEP_PERCENTAGE("sleep.percentage"),
    SLEEP_NIGHT_SKIPPED("sleep.night_skipped"),
    SLEEP_NOTIFY("sleep.notify"),
    SLEEP_CLICK_TO_SLEEP("sleep.click_to_sleep"),
    SLEEP_CLICK_HOVER("sleep.click_hover"),

    // Chunk Mob Limit
    CHUNK_MOB_LIMIT_BREEDING("chunk_mob_limit.breeding"),
    CHUNK_MOB_LIMIT_INFO("chunk_mob_limit.info"),

    // Scoreboard
    SCOREBOARD_DISABLED("scoreboard.disabled"),
    SCOREBOARD_SHOWN("scoreboard.shown"),
    SCOREBOARD_HIDDEN("scoreboard.hidden"),
    SCOREBOARD_TOGGLE_NOT_ALLOWED("scoreboard.toggle_not_allowed"),

    // Common
    COMMON_YES("common.yes"),
    COMMON_NO("common.no"),
    COMMON_NONE("common.none"),
    COMMON_UNKNOWN("common.unknown"),
    COMMON_ENABLED("common.enabled"),
    COMMON_DISABLED("common.disabled"),

    // Admin help
    ADMIN_HELP_HEADER("admin.help.header"),
    ADMIN_HELP_CHECK("admin.help.check"),
    ADMIN_HELP_SET("admin.help.set"),
    ADMIN_HELP_SETSTAT("admin.help.setstat"),
    ADMIN_HELP_RESET("admin.help.reset"),
    ADMIN_HELP_RELOAD("admin.help.reload"),
    ADMIN_HELP_GIVE_SPELLBOOK("admin.help.give_spellbook"),
    ADMIN_HELP_GIVE_RUNEBOOK("admin.help.give_runebook"),
    ADMIN_HELP_GIVE_RUNE("admin.help.give_rune"),
    ADMIN_HELP_GIVE_REAGENTS("admin.help.give_reagents"),
    ADMIN_HELP_GIVE_SCROLL("admin.help.give_scroll"),

    // Admin usage
    ADMIN_USAGE_CHECK("admin.usage.check"),
    ADMIN_USAGE_SET("admin.usage.set"),
    ADMIN_USAGE_SET_NOTE("admin.usage.set_note"),
    ADMIN_USAGE_SETSTAT("admin.usage.setstat"),
    ADMIN_USAGE_RESET("admin.usage.reset"),
    ADMIN_USAGE_GIVE("admin.usage.give"),
    ADMIN_USAGE_SCROLL("admin.usage.scroll"),

    // Admin messages
    ADMIN_PLAYER_NOT_FOUND_OR_NEVER_JOINED("admin.player_not_found_or_never_joined"),
    ADMIN_PLAYER_SKILLS_HEADER("admin.player_skills_header"),
    ADMIN_PLAYER_TITLE("admin.player_title"),
    ADMIN_PLAYER_STATS("admin.player_stats"),
    ADMIN_PLAYER_TOTAL("admin.player_total"),
    ADMIN_UNKNOWN_SKILL("admin.unknown_skill"),
    ADMIN_UNKNOWN_SKILL_LIST("admin.unknown_skill_list"),
    ADMIN_INVALID_VALUE("admin.invalid_value"),
    ADMIN_UNKNOWN_STAT("admin.unknown_stat"),
    ADMIN_SKILL_SET_SUCCESS("admin.skill_set_success"),
    ADMIN_STAT_SET_SUCCESS("admin.stat_set_success"),
    ADMIN_SKILLS_RESET("admin.skills_reset"),
    ADMIN_CONFIG_RELOADED("admin.config_reloaded"),
    ADMIN_GAVE_SPELLBOOK("admin.gave_spellbook"),
    ADMIN_GAVE_BLANK_SPELLBOOK("admin.gave_blank_spellbook"),
    ADMIN_GAVE_RUNEBOOK("admin.gave_runebook"),
    ADMIN_GAVE_RUNE("admin.gave_rune"),
    ADMIN_GAVE_REAGENTS("admin.gave_reagents"),
    ADMIN_GAVE_SCROLL("admin.gave_scroll"),
    ADMIN_GAVE_ALL_SCROLLS("admin.gave_all_scrolls"),
    ADMIN_UNKNOWN_SPELL("admin.unknown_spell"),
    ADMIN_UNKNOWN_SPELL_LIST("admin.unknown_spell_list"),
    ADMIN_UNKNOWN_ITEM("admin.unknown_item"),

    // Skills command
    SKILLS_HEADER("skills.header"),
    SKILLS_CATEGORY_HEADER("skills.category_header"),
    SKILLS_SKILL_ENTRY("skills.skill_entry"),
    SKILLS_TOTAL("skills.total"),
    SKILLS_USAGE_LOCK("skills.usage_lock"),
    SKILLS_USAGE_CATEGORY("skills.usage_category"),
    SKILLS_UNKNOWN_CATEGORY("skills.unknown_category"),
    SKILLS_UNKNOWN_SKILL("skills.unknown_skill"),
    SKILLS_SKILL_DETAIL_HEADER("skills.skill_detail_header"),
    SKILLS_SKILL_DETAIL_VALUE("skills.skill_detail_value"),
    SKILLS_SKILL_DETAIL_CATEGORY("skills.skill_detail_category"),
    SKILLS_SKILL_DETAIL_AFFECTS("skills.skill_detail_affects"),
    SKILLS_LOCK_CHANGED("skills.lock_changed"),

    // Stats command
    STATS_HEADER_DISPLAY("stats.header_display"),
    STATS_HP_DISPLAY("stats.hp_display"),
    STATS_MANA_DISPLAY("stats.mana_display"),
    STATS_STAT_TOTAL("stats.stat_total"),
    STATS_STR_DISPLAY("stats.str_display"),
    STATS_DEX_DISPLAY("stats.dex_display"),
    STATS_INT_DISPLAY("stats.int_display"),
    STATS_TOTAL_SKILLS_DISPLAY("stats.total_skills_display"),
    STATS_LOCK_HELP("stats.lock_help"),
    STATS_LOCK_DESCRIPTION("stats.lock_description"),
    STATS_USAGE_LOCK("stats.usage_lock"),
    STATS_INVALID_STAT("stats.invalid_stat"),
    STATS_INVALID_MODE("stats.invalid_mode"),

    // Cast/Rune command
    CAST_USAGE("cast.usage"),
    CAST_EXAMPLE("cast.example"),
    CAST_POWER_WORDS_HINT("cast.power_words_hint"),
    CAST_UNKNOWN_SPELL("cast.unknown_spell"),
    CAST_SPELL_LIST_HINT("cast.spell_list_hint"),
    RUNE_USAGE("rune.usage"),
    RUNE_EXAMPLE1("rune.example1"),
    RUNE_EXAMPLE2("rune.example2"),
    RUNE_UNKNOWN_POWER_WORDS("rune.unknown_power_words"),
    RUNE_POWER_WORDS_HINT("rune.power_words_hint"),

    // Scribe command
    SCRIBE_HELP_HEADER("scribe.help_header"),
    SCRIBE_HELP_LIST("scribe.help_list"),
    SCRIBE_HELP_SPELL("scribe.help_spell"),
    SCRIBE_HELP_REQUIREMENTS("scribe.help_requirements"),
    SCRIBE_SPELLBOOK_EMPTY("scribe.spellbook_empty"),
    SCRIBE_AVAILABLE_HEADER("scribe.available_header"),
    SCRIBE_UNKNOWN_SPELL("scribe.unknown_spell"),
    SCRIBE_UNKNOWN_SPELL_HINT("scribe.unknown_spell_hint"),

    // Arms command
    ARMS_HOLD_ITEM("arms.hold_item"),
    ARMS_NOT_WEAPON_ARMOR("arms.not_weapon_armor"),

    // Food/Mana
    FOOD_MANA_RESTORE("food.mana_restore"),

    // UI separators
    UI_SEPARATOR("ui.separator"),
    UI_SEPARATOR_THIN("ui.separator_thin");

    companion object {
        fun fromPath(path: String): MessageKey? =
            entries.find { it.path == path }
    }
}
