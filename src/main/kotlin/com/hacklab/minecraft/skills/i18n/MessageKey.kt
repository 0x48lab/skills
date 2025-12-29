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
    THIEF_POISON_APPLIED("thief.poison_applied"),
    THIEF_POISON_FAILED("thief.poison_failed"),
    THIEF_POISON_HIT("thief.poison_hit"),

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

    // Repair
    REPAIR_SUCCESS("repair.success"),

    // Scroll
    SCROLL_CREATED("scroll.created"),
    SCROLL_FAILED("scroll.failed"),
    SCROLL_USED("scroll.used"),
    SCROLL_LEARNED("scroll.learned"),

    // Rune
    RUNE_CREATED("rune.created"),
    RUNE_MARKED("rune.marked"),

    // Gate
    GATE_CREATED("gate.created"),
    GATE_TRAVEL_USED("gate.travel_used"),

    // Combat
    COMBAT_PARRY("combat.parry"),
    COMBAT_CRITICAL("combat.critical"),
    COMBAT_RESIST("combat.resist"),
    COMBAT_MISS("combat.miss"),

    // Armor
    ARMOR_CANNOT_EQUIP_STR("armor.cannot_equip_str"),
    ARMOR_REMOVED_STR("armor.removed_str"),
    ARMOR_DEX_PENALTY_WARNING("armor.dex_penalty_warning"),

    // Gathering
    GATHERING_BONUS_DROP("gathering.bonus_drop"),
    GATHERING_RARE_FIND("gathering.rare_find"),

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

    // Common
    COMMON_YES("common.yes"),
    COMMON_NO("common.no"),
    COMMON_NONE("common.none"),
    COMMON_UNKNOWN("common.unknown"),
    COMMON_ENABLED("common.enabled"),
    COMMON_DISABLED("common.disabled");

    companion object {
        fun fromPath(path: String): MessageKey? =
            entries.find { it.path == path }
    }
}
