package com.hacklab.minecraft.skills.taming

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class AnimalLoreManager(private val plugin: Skills) {

    /**
     * Display animal lore for a target entity
     */
    fun showLore(player: Player, entity: LivingEntity) {
        val data = plugin.playerDataManager.getPlayerData(player)
        val animalLoreSkill = data.getSkillValue(SkillType.ANIMAL_LORE)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.ANIMAL_LORE, 30)

        // Build lore display
        val messages = mutableListOf<Component>()

        // Header
        messages.add(Component.text("═══ Animal Lore ═══").color(NamedTextColor.GOLD))
        messages.add(Component.text("Entity: ${entity.type.name.lowercase().replace("_", " ")}")
            .color(NamedTextColor.WHITE))

        // Health (always shown)
        val healthPercent = (entity.health / entity.maxHealth * 100).toInt()
        val healthColor = when {
            healthPercent > 70 -> NamedTextColor.GREEN
            healthPercent > 30 -> NamedTextColor.YELLOW
            else -> NamedTextColor.RED
        }
        messages.add(Component.text("Health: ${entity.health.toInt()}/${entity.maxHealth.toInt()} ($healthPercent%)")
            .color(healthColor))

        // Owner (if skill >= 30)
        if (animalLoreSkill >= 30) {
            val owner = plugin.tamingManager.getOwner(entity)
            val ownerName = owner?.let { plugin.server.getOfflinePlayer(it).name } ?: "Wild"
            messages.add(Component.text("Owner: $ownerName").color(NamedTextColor.AQUA))
        }

        // Tameable status (if skill >= 50)
        if (animalLoreSkill >= 50) {
            val isTameable = plugin.tamingManager.isTameable(entity)
            val isTamed = plugin.tamingManager.isAlreadyTamed(entity)
            val status = when {
                isTamed -> "Tamed"
                isTameable -> "Can be tamed"
                else -> "Cannot be tamed"
            }
            messages.add(Component.text("Status: $status").color(NamedTextColor.GRAY))
        }

        // Taming difficulty (if skill >= 70)
        if (animalLoreSkill >= 70 && plugin.tamingManager.isTameable(entity)) {
            val difficulty = plugin.tamingManager.getTamingDifficulty(entity.type)
            val difficultyText = when {
                difficulty <= 30 -> "Easy"
                difficulty <= 50 -> "Medium"
                difficulty <= 70 -> "Hard"
                else -> "Very Hard"
            }
            messages.add(Component.text("Taming Difficulty: $difficultyText ($difficulty)")
                .color(NamedTextColor.YELLOW))
        }

        // Additional info at high skill
        if (animalLoreSkill >= 90) {
            // Could show attributes, effects, etc.
            entity.activePotionEffects.forEach { effect ->
                messages.add(Component.text("Effect: ${effect.type.name} (${effect.duration / 20}s)")
                    .color(NamedTextColor.LIGHT_PURPLE))
            }
        }

        // Send all messages
        messages.forEach { player.sendMessage(it) }
    }

    /**
     * Get a brief lore summary
     */
    fun getBriefLore(player: Player, entity: LivingEntity): String {
        val healthPercent = (entity.health / entity.maxHealth * 100).toInt()
        return "${entity.type.name}: $healthPercent% HP"
    }
}
