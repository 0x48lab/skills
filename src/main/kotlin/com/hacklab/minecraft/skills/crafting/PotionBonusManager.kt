package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionType

/**
 * Manages potion bonuses from Alchemy skill
 *
 * Potions can have bonuses that affect:
 * - Duration (extended based on skill)
 * - Amplifier (upgraded based on quality for HQ/EX)
 *
 * The bonus is applied directly to the potion's effects
 */
class PotionBonusManager(private val plugin: Skills) {

    private val alchemistKey = NamespacedKey(plugin, "alchemist")
    private val qualityKey = NamespacedKey(plugin, "potion_quality")

    /**
     * Apply alchemy bonus to a potion
     *
     * @param item The potion item
     * @param alchemySkill The player's Alchemy skill value
     * @param alchemistName The name of the player who brewed it
     * @return The modified potion with bonus applied
     */
    fun applyPotionBonus(
        item: ItemStack,
        alchemySkill: Double,
        alchemistName: String
    ): ItemStack {
        val meta = item.itemMeta as? PotionMeta ?: return item
        val potionType = meta.basePotionType ?: return item

        // Calculate difficulty
        val isExtended = potionType.name.contains("LONG") || isExtendedPotion(meta)
        val isUpgraded = potionType.name.contains("STRONG") || isUpgradedPotion(meta)
        val difficulty = AlchemyDifficulty.calculateDifficulty(
            getPotionBaseType(potionType),
            isExtended,
            isUpgraded,
            item.type
        )

        // Calculate quality based on skill and difficulty
        val quality = QualityType.calculateQuality(alchemySkill, difficulty)

        // Calculate duration bonus: skill 100 = +50% duration
        val durationBonus = AlchemyDifficulty.calculateDurationBonus(alchemySkill)

        // Store metadata
        meta.persistentDataContainer.set(alchemistKey, PersistentDataType.STRING, alchemistName)
        meta.persistentDataContainer.set(qualityKey, PersistentDataType.STRING, quality.name)

        // Apply duration bonus to all effects
        val customEffects = mutableListOf<PotionEffect>()

        // Get base effects from potion type
        potionType.potionEffects.forEach { effect ->
            if (effect.duration > 0) {
                val newDuration = (effect.duration * durationBonus).toInt()

                // For HQ/EX, also boost amplifier for certain potions
                val newAmplifier = if (quality == QualityType.EXCEPTIONAL && canUpgradeAmplifier(effect.type)) {
                    effect.amplifier + 1
                } else {
                    effect.amplifier
                }

                customEffects.add(PotionEffect(
                    effect.type,
                    newDuration,
                    newAmplifier,
                    effect.isAmbient,
                    effect.hasParticles(),
                    effect.hasIcon()
                ))
            }
        }

        // Apply custom effects (this overrides base effects)
        customEffects.forEach { effect ->
            meta.addCustomEffect(effect, true)
        }

        // Update lore
        updatePotionLore(meta, quality, durationBonus, alchemistName)

        item.itemMeta = meta
        return item
    }

    /**
     * Check if a potion effect type can have its amplifier upgraded
     */
    private fun canUpgradeAmplifier(effectType: org.bukkit.potion.PotionEffectType): Boolean {
        return effectType in listOf(
            org.bukkit.potion.PotionEffectType.STRENGTH,
            org.bukkit.potion.PotionEffectType.SPEED,
            org.bukkit.potion.PotionEffectType.REGENERATION,
            org.bukkit.potion.PotionEffectType.POISON,
            org.bukkit.potion.PotionEffectType.INSTANT_HEALTH,
            org.bukkit.potion.PotionEffectType.INSTANT_DAMAGE,
            org.bukkit.potion.PotionEffectType.JUMP_BOOST,
            org.bukkit.potion.PotionEffectType.RESISTANCE
        )
    }

    /**
     * Get base potion type without modifiers
     */
    private fun getPotionBaseType(potionType: PotionType): PotionType {
        // Remove LONG_ or STRONG_ prefix if present
        val name = potionType.name
            .replace("LONG_", "")
            .replace("STRONG_", "")

        return try {
            PotionType.valueOf(name)
        } catch (e: Exception) {
            potionType
        }
    }

    /**
     * Check if potion is extended duration
     */
    private fun isExtendedPotion(meta: PotionMeta): Boolean {
        val baseName = meta.basePotionType?.name ?: return false
        return baseName.startsWith("LONG_")
    }

    /**
     * Check if potion is upgraded (level II)
     */
    private fun isUpgradedPotion(meta: PotionMeta): Boolean {
        val baseName = meta.basePotionType?.name ?: return false
        return baseName.startsWith("STRONG_")
    }

    /**
     * Update potion lore to show bonus information
     */
    private fun updatePotionLore(
        meta: PotionMeta,
        quality: QualityType,
        durationBonus: Double,
        alchemistName: String
    ) {
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        // Remove existing alchemy lore
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
            plain.contains("Duration:") || plain.contains("Brewed by:") || plain.contains("Quality:")
        }

        // Add quality indicator
        if (quality != QualityType.NORMAL_QUALITY) {
            lore.add(0, Component.text("Quality: ${quality.displayName}")
                .color(quality.color)
                .decoration(TextDecoration.ITALIC, false))
        }

        // Add duration bonus if significant
        val bonusPercent = ((durationBonus - 1.0) * 100).toInt()
        if (bonusPercent > 0) {
            lore.add(Component.text("Duration: +${bonusPercent}%")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false))
        }

        // Add alchemist name for exceptional quality
        if (quality == QualityType.EXCEPTIONAL) {
            lore.add(Component.text("Brewed by: $alchemistName")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false))
        }

        meta.lore(lore)
    }

    /**
     * Get the quality of a potion
     */
    fun getPotionQuality(item: ItemStack?): QualityType {
        if (item == null) return QualityType.NORMAL_QUALITY
        val meta = item.itemMeta ?: return QualityType.NORMAL_QUALITY

        val qualityName = meta.persistentDataContainer
            .get(qualityKey, PersistentDataType.STRING)

        return qualityName?.let {
            try { QualityType.valueOf(it) } catch (e: Exception) { QualityType.NORMAL_QUALITY }
        } ?: QualityType.NORMAL_QUALITY
    }

    /**
     * Get the alchemist name from a potion
     */
    fun getAlchemistName(item: ItemStack?): String? {
        if (item == null) return null
        return item.itemMeta?.persistentDataContainer
            ?.get(alchemistKey, PersistentDataType.STRING)
    }

    /**
     * Check if potion has alchemy bonus
     */
    fun hasPotionBonus(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.itemMeta?.persistentDataContainer?.has(alchemistKey, PersistentDataType.STRING) == true
    }
}
