package com.hacklab.minecraft.skills.crafting

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.thief.PoisonLevel
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

        // Check if this is a poison potion - convert to custom poison based on Alchemy skill
        if (isPoisonPotion(potionType)) {
            return createCustomPoisonPotion(item, potionType, alchemySkill, alchemistName)
        }

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

    /**
     * Check if a potion type is a poison potion
     */
    private fun isPoisonPotion(potionType: PotionType): Boolean {
        val name = potionType.name
        return name == "POISON" || name == "STRONG_POISON" || name == "LONG_POISON"
    }

    /**
     * Create a custom poison potion based on Alchemy skill
     *
     * Poison levels are determined by Alchemy skill:
     * - Lesser (0+): From vanilla Poison or Long Poison
     * - Regular (30+): Upgrade chance from vanilla, guaranteed from Strong Poison
     * - Greater (60+): Can create from Strong Poison
     * - Deadly (90+): Can create from Strong Poison
     *
     * @param item The original poison potion
     * @param potionType The vanilla potion type
     * @param alchemySkill The player's Alchemy skill
     * @param alchemistName The player who brewed it
     * @return Custom poison potion or the original item
     */
    private fun createCustomPoisonPotion(
        item: ItemStack,
        potionType: PotionType,
        alchemySkill: Double,
        alchemistName: String
    ): ItemStack {
        // Determine base level from vanilla potion
        val isStrongPoison = potionType.name == "STRONG_POISON"

        // Calculate max achievable poison level based on alchemy skill
        val maxLevel = when {
            alchemySkill >= PoisonLevel.DEADLY.alchemyRequired -> PoisonLevel.DEADLY
            alchemySkill >= PoisonLevel.GREATER.alchemyRequired -> PoisonLevel.GREATER
            alchemySkill >= PoisonLevel.REGULAR.alchemyRequired -> PoisonLevel.REGULAR
            else -> PoisonLevel.LESSER
        }

        // Base level from vanilla potion type
        val baseLevel = if (isStrongPoison) PoisonLevel.REGULAR else PoisonLevel.LESSER

        // Final level is the higher of base and max achievable
        val finalLevel = if (baseLevel.ordinal >= maxLevel.ordinal) {
            // Can't upgrade beyond max skill allows
            maxLevel
        } else {
            // Upgrade to max level skill allows
            maxLevel
        }

        // Create custom poison potion using PoisonItemManager
        val useJapanese = false // Default to English; player locale checked elsewhere
        val customPoison = plugin.poisonItemManager.createPoisonPotion(finalLevel, useJapanese)

        // Preserve splash/lingering type
        when (item.type) {
            Material.SPLASH_POTION -> customPoison.type = Material.SPLASH_POTION
            Material.LINGERING_POTION -> customPoison.type = Material.LINGERING_POTION
            else -> { /* Keep as regular potion */ }
        }

        // Add alchemist info to lore
        val meta = customPoison.itemMeta as? PotionMeta
        if (meta != null) {
            meta.persistentDataContainer.set(alchemistKey, PersistentDataType.STRING, alchemistName)

            // Add alchemist to lore for exceptional quality (high skill)
            if (alchemySkill >= 90) {
                val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                lore.add(Component.text("Brewed by: $alchemistName")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false))
                meta.lore(lore)
            }

            customPoison.itemMeta = meta
        }

        return customPoison
    }
}
