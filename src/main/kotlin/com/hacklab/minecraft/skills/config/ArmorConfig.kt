package com.hacklab.minecraft.skills.config

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.QualityType
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File

/**
 * Armor configuration loaded from armor.yml
 * Handles AR (Armor Rating), STR requirements, DEX penalties
 */
class ArmorConfig(private val plugin: Skills) {

    // Armor stats by material
    private val armorStats = mutableMapOf<Material, ArmorStats>()

    // Enchantment AR bonuses
    private val enchantmentArBonus = mutableMapOf<Enchantment, Int>()

    // Quality modifiers
    private var qualityModifiers = mapOf(
        QualityType.LOW_QUALITY to 0.85,
        QualityType.NORMAL_QUALITY to 1.0,
        QualityType.HIGH_QUALITY to 1.15,
        QualityType.EXCEPTIONAL to 1.30
    )

    init {
        reload()
    }

    fun reload() {
        val file = File(plugin.dataFolder, "armor.yml")
        if (!file.exists()) {
            plugin.saveResource("armor.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)

        loadArmorStats(config)
        loadEnchantmentBonuses(config)
        loadQualityModifiers(config)

        plugin.logger.info("Loaded ${armorStats.size} armor configurations")
    }

    private fun loadArmorStats(config: YamlConfiguration) {
        val armorSection = config.getConfigurationSection("armor") ?: return
        armorStats.clear()

        for (key in armorSection.getKeys(false)) {
            try {
                val material = Material.valueOf(key)
                val section = armorSection.getConfigurationSection(key) ?: continue

                val specialSection = section.getConfigurationSection("special")
                val special = if (specialSection != null) {
                    ArmorSpecial(
                        knockbackResistance = specialSection.getDouble("knockback_resistance", 0.0),
                        waterBreathing = specialSection.getInt("water_breathing", 0),
                        piglinNeutral = specialSection.getBoolean("piglin_neutral", false),
                        blockBonus = specialSection.getInt("block_bonus", 0),
                        flight = specialSection.getBoolean("flight", false)
                    )
                } else null

                armorStats[material] = ArmorStats(
                    ar = section.getInt("ar", 0),
                    strRequired = section.getInt("str_required", 0),
                    dexPenalty = section.getInt("dex_penalty", 0),
                    special = special
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Unknown material in armor.yml: $key")
            }
        }
    }

    private fun loadEnchantmentBonuses(config: YamlConfiguration) {
        val enchSection = config.getConfigurationSection("enchantment_ar_bonus") ?: return
        enchantmentArBonus.clear()

        for (key in enchSection.getKeys(false)) {
            val enchantment = when (key) {
                "PROTECTION" -> Enchantment.PROTECTION
                "FIRE_PROTECTION" -> Enchantment.FIRE_PROTECTION
                "BLAST_PROTECTION" -> Enchantment.BLAST_PROTECTION
                "PROJECTILE_PROTECTION" -> Enchantment.PROJECTILE_PROTECTION
                else -> null
            }
            if (enchantment != null) {
                enchantmentArBonus[enchantment] = enchSection.getInt(key, 0)
            }
        }
    }

    private fun loadQualityModifiers(config: YamlConfiguration) {
        val qualitySection = config.getConfigurationSection("quality_modifiers") ?: return
        qualityModifiers = mapOf(
            QualityType.LOW_QUALITY to qualitySection.getDouble("LOW_QUALITY", 0.85),
            QualityType.NORMAL_QUALITY to qualitySection.getDouble("NORMAL_QUALITY", 1.0),
            QualityType.HIGH_QUALITY to qualitySection.getDouble("HIGH_QUALITY", 1.15),
            QualityType.EXCEPTIONAL to qualitySection.getDouble("EXCEPTIONAL", 1.30)
        )
    }

    // === Public API ===

    /**
     * Get armor stats for a material
     */
    fun getArmorStats(material: Material): ArmorStats? {
        return armorStats[material]
    }

    /**
     * Get armor stats for an item
     */
    fun getArmorStats(item: ItemStack?): ArmorStats? {
        if (item == null || item.type == Material.AIR) return null
        return armorStats[item.type]
    }

    /**
     * Check if a material is armor
     */
    fun isArmor(material: Material): Boolean {
        return armorStats.containsKey(material)
    }

    /**
     * Get base AR for an armor piece
     */
    fun getBaseAR(item: ItemStack?): Int {
        return getArmorStats(item)?.ar ?: 0
    }

    /**
     * Get STR requirement for an armor piece
     */
    fun getStrRequired(item: ItemStack?): Int {
        return getArmorStats(item)?.strRequired ?: 0
    }

    /**
     * Get DEX penalty for an armor piece
     */
    fun getDexPenalty(item: ItemStack?): Int {
        return getArmorStats(item)?.dexPenalty ?: 0
    }

    /**
     * Calculate total AR for an armor piece including enchantments and quality
     */
    fun calculateItemAR(item: ItemStack?): Double {
        if (item == null || item.type == Material.AIR) return 0.0

        val baseAR = getBaseAR(item)
        if (baseAR == 0) return 0.0

        // Add enchantment bonuses
        var enchantBonus = 0
        for ((enchantment, level) in item.enchantments) {
            val bonusPerLevel = enchantmentArBonus[enchantment] ?: 0
            enchantBonus += bonusPerLevel * level
        }

        // Apply quality modifier
        val quality = plugin.qualityManager.getQuality(item)
        val qualityMod = qualityModifiers[quality] ?: 1.0

        return (baseAR + enchantBonus) * qualityMod
    }

    /**
     * Calculate total AR for a player's equipped armor
     */
    fun calculateTotalAR(player: Player): Double {
        val equipment = player.inventory
        var totalAR = 0.0

        // Helmet
        totalAR += calculateItemAR(equipment.helmet)
        // Chestplate
        totalAR += calculateItemAR(equipment.chestplate)
        // Leggings
        totalAR += calculateItemAR(equipment.leggings)
        // Boots
        totalAR += calculateItemAR(equipment.boots)
        // Off-hand (shield)
        val offhand = equipment.itemInOffHand
        if (offhand.type == Material.SHIELD) {
            totalAR += calculateItemAR(offhand)
        }

        return totalAR
    }

    /**
     * Calculate total DEX penalty from equipped armor
     */
    fun calculateTotalDexPenalty(player: Player): Int {
        val equipment = player.inventory
        var totalPenalty = 0

        totalPenalty += getDexPenalty(equipment.helmet)
        totalPenalty += getDexPenalty(equipment.chestplate)
        totalPenalty += getDexPenalty(equipment.leggings)
        totalPenalty += getDexPenalty(equipment.boots)

        val offhand = equipment.itemInOffHand
        if (offhand.type == Material.SHIELD) {
            totalPenalty += getDexPenalty(offhand)
        }

        return totalPenalty
    }

    /**
     * Check if player can equip an armor piece based on STR
     */
    fun canEquip(player: Player, item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) return true

        val stats = getArmorStats(item) ?: return true
        val playerData = plugin.playerDataManager.getPlayerData(player)

        return playerData.str >= stats.strRequired
    }

    /**
     * Get the STR deficit for equipping an item (0 if can equip)
     */
    fun getStrDeficit(player: Player, item: ItemStack?): Int {
        if (item == null || item.type == Material.AIR) return 0

        val stats = getArmorStats(item) ?: return 0
        val playerData = plugin.playerDataManager.getPlayerData(player)

        return (stats.strRequired - playerData.str).coerceAtLeast(0)
    }

    /**
     * Check all equipped armor and return items that can no longer be worn
     */
    fun getUnequippableArmor(player: Player): List<ItemStack> {
        val equipment = player.inventory
        val unequippable = mutableListOf<ItemStack>()

        equipment.helmet?.let { if (!canEquip(player, it)) unequippable.add(it) }
        equipment.chestplate?.let { if (!canEquip(player, it)) unequippable.add(it) }
        equipment.leggings?.let { if (!canEquip(player, it)) unequippable.add(it) }
        equipment.boots?.let { if (!canEquip(player, it)) unequippable.add(it) }

        return unequippable
    }

    /**
     * Get shield block bonus from equipped shield
     */
    fun getShieldBlockBonus(player: Player): Int {
        val offhand = player.inventory.itemInOffHand
        if (offhand.type != Material.SHIELD) return 0

        return getArmorStats(offhand)?.special?.blockBonus ?: 0
    }
}

/**
 * Armor statistics
 */
data class ArmorStats(
    val ar: Int,
    val strRequired: Int,
    val dexPenalty: Int,
    val special: ArmorSpecial? = null
)

/**
 * Special armor properties (Minecraft-specific)
 */
data class ArmorSpecial(
    val knockbackResistance: Double = 0.0,
    val waterBreathing: Int = 0,  // seconds
    val piglinNeutral: Boolean = false,
    val blockBonus: Int = 0,  // % bonus to parrying with shield
    val flight: Boolean = false  // Elytra
)
