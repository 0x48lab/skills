package com.hacklab.minecraft.skills.dragon

import com.hacklab.minecraft.skills.i18n.MessageKey

enum class DragonSkillType(
    val requiredKills: Int,
    val messageKey: MessageKey
) {
    EXPLOSION_IMMUNITY(1, MessageKey.DRAGON_SKILL_EXPLOSION_IMMUNITY),
    STRUCTURE_DESTRUCTION(2, MessageKey.DRAGON_SKILL_STRUCTURE_DESTRUCTION),
    HEAL_AURA(3, MessageKey.DRAGON_SKILL_HEAL_AURA),
    ENDERMAN_SUMMON(4, MessageKey.DRAGON_SKILL_ENDERMAN_SUMMON),
    CRYSTAL_REGENERATION(5, MessageKey.DRAGON_SKILL_CRYSTAL_REGENERATION),
    PLAYER_PULL(6, MessageKey.DRAGON_SKILL_PLAYER_PULL);

    companion object {
        fun getActiveSkills(killCount: Int): List<DragonSkillType> =
            entries.filter { killCount >= it.requiredKills }
    }
}
