package com.hacklab.minecraft.skills.config

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.WeaponType
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import java.io.File

/**
 * Combat configuration loaded from mobs.yml, weapons.yml, enchantments.yml
 */
class CombatConfig(private val plugin: Skills) {

    // Mob stats
    private val mobStats = mutableMapOf<EntityType, MobStats>()
    private var defaultMobStats = MobStats(10, 10, 3)

    // Weapon stats
    private val weaponStats = mutableMapOf<Material, WeaponStats>()
    private var unarmedStats = WeaponStats(1, SkillType.WRESTLING, WeaponType.FIST)

    // Enchantment DI
    private val enchantmentDI = mutableMapOf<Enchantment, EnchantmentDI>()

    init {
        reload()
    }

    fun reload() {
        loadMobsConfig()
        loadWeaponsConfig()
        loadEnchantmentsConfig()
    }

    private fun loadMobsConfig() {
        val file = File(plugin.dataFolder, "mobs.yml")
        if (!file.exists()) {
            plugin.saveResource("mobs.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val mobsSection = config.getConfigurationSection("mobs") ?: return

        mobStats.clear()

        for (key in mobsSection.getKeys(false)) {
            if (key == "DEFAULT") {
                val section = mobsSection.getConfigurationSection(key)
                if (section != null) {
                    defaultMobStats = MobStats(
                        physicalDefense = section.getInt("physical_defense", 10),
                        magicDefense = section.getInt("magic_defense", 10),
                        attackPower = section.getInt("attack_power", 3),
                        rewardMin = section.getInt("reward_min", 1),
                        rewardMax = section.getInt("reward_max", 5),
                        rewardChance = section.getInt("reward_chance", 50)
                    )
                }
                continue
            }

            try {
                val entityType = EntityType.valueOf(key)
                val section = mobsSection.getConfigurationSection(key) ?: continue

                mobStats[entityType] = MobStats(
                    physicalDefense = section.getInt("physical_defense", 10),
                    magicDefense = section.getInt("magic_defense", 10),
                    attackPower = section.getInt("attack_power", 3),
                    rewardMin = section.getInt("reward_min", 0),
                    rewardMax = section.getInt("reward_max", 0),
                    rewardChance = section.getInt("reward_chance", 0)
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Unknown entity type in mobs.yml: $key")
            }
        }

        plugin.logger.info("Loaded ${mobStats.size} mob configurations")
    }

    private fun loadWeaponsConfig() {
        val file = File(plugin.dataFolder, "weapons.yml")
        if (!file.exists()) {
            plugin.saveResource("weapons.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val weaponsSection = config.getConfigurationSection("weapons") ?: return

        weaponStats.clear()

        for (key in weaponsSection.getKeys(false)) {
            try {
                val material = Material.valueOf(key)
                val section = weaponsSection.getConfigurationSection(key) ?: continue

                val skillName = section.getString("skill", "WRESTLING") ?: "WRESTLING"
                val weaponTypeName = section.getString("weapon_type", "FIST") ?: "FIST"

                weaponStats[material] = WeaponStats(
                    baseDamage = section.getInt("base_damage", 1),
                    skill = try { SkillType.valueOf(skillName) } catch (e: Exception) { SkillType.WRESTLING },
                    weaponType = try { WeaponType.valueOf(weaponTypeName) } catch (e: Exception) { WeaponType.FIST }
                )
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Unknown material in weapons.yml: $key")
            }
        }

        // Load unarmed stats
        config.getConfigurationSection("unarmed")?.let { section ->
            val skillName = section.getString("skill", "WRESTLING") ?: "WRESTLING"
            val weaponTypeName = section.getString("weapon_type", "FIST") ?: "FIST"

            unarmedStats = WeaponStats(
                baseDamage = section.getInt("base_damage", 1),
                skill = try { SkillType.valueOf(skillName) } catch (e: Exception) { SkillType.WRESTLING },
                weaponType = try { WeaponType.valueOf(weaponTypeName) } catch (e: Exception) { WeaponType.FIST }
            )
        }

        plugin.logger.info("Loaded ${weaponStats.size} weapon configurations")
    }

    private fun loadEnchantmentsConfig() {
        val file = File(plugin.dataFolder, "enchantments.yml")
        if (!file.exists()) {
            plugin.saveResource("enchantments.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val enchSection = config.getConfigurationSection("enchantments") ?: return

        enchantmentDI.clear()

        for (key in enchSection.getKeys(false)) {
            try {
                val enchantment = when (key) {
                    "SHARPNESS" -> Enchantment.SHARPNESS
                    "SMITE" -> Enchantment.SMITE
                    "BANE_OF_ARTHROPODS" -> Enchantment.BANE_OF_ARTHROPODS
                    "POWER" -> Enchantment.POWER
                    "IMPALING" -> Enchantment.IMPALING
                    "DENSITY" -> Enchantment.DENSITY
                    "BREACH" -> Enchantment.BREACH
                    else -> null
                }

                if (enchantment == null) {
                    plugin.logger.warning("Unknown enchantment in enchantments.yml: $key")
                    continue
                }

                val section = enchSection.getConfigurationSection(key) ?: continue
                val targetTypeNames = section.getStringList("target_types")
                val targetTypes = targetTypeNames.mapNotNull { name ->
                    try { EntityType.valueOf(name) } catch (e: Exception) { null }
                }.toSet()

                enchantmentDI[enchantment] = EnchantmentDI(
                    diPerLevel = section.getInt("di_per_level", 10),
                    targetTypes = if (targetTypes.isEmpty()) null else targetTypes
                )
            } catch (e: Exception) {
                plugin.logger.warning("Error loading enchantment $key: ${e.message}")
            }
        }

        plugin.logger.info("Loaded ${enchantmentDI.size} enchantment configurations")
    }

    // === Public API ===

    /**
     * Get mob stats for an entity type
     */
    fun getMobStats(entityType: EntityType): MobStats {
        return mobStats[entityType] ?: defaultMobStats
    }

    /**
     * Get physical defense for hit chance calculation
     */
    fun getPhysicalDefense(entityType: EntityType): Int {
        return getMobStats(entityType).physicalDefense
    }

    /**
     * Get magic defense for magic damage reduction
     */
    fun getMagicDefense(entityType: EntityType): Int {
        return getMobStats(entityType).magicDefense
    }

    /**
     * Get attack power (mob's damage)
     */
    fun getAttackPower(entityType: EntityType): Int {
        return getMobStats(entityType).attackPower
    }

    /**
     * Get difficulty for skill gain calculation (physical_defense + attack_power)
     */
    fun getDifficulty(entityType: EntityType): Int {
        val stats = getMobStats(entityType)
        return stats.physicalDefense + stats.attackPower
    }

    /**
     * Get weapon stats
     */
    fun getWeaponStats(item: ItemStack?): WeaponStats {
        if (item == null || item.type == Material.AIR) {
            return unarmedStats
        }
        return weaponStats[item.type] ?: unarmedStats
    }

    /**
     * Get base damage for a weapon
     */
    fun getBaseDamage(item: ItemStack?): Int {
        return getWeaponStats(item).baseDamage
    }

    /**
     * Get skill type for a weapon
     */
    fun getWeaponSkill(item: ItemStack?): SkillType {
        return getWeaponStats(item).skill
    }

    /**
     * Get weapon type for a weapon
     */
    fun getWeaponType(item: ItemStack?): WeaponType {
        return getWeaponStats(item).weaponType
    }

    /**
     * Check if item is a weapon (not unarmed)
     */
    fun isWeapon(item: ItemStack?): Boolean {
        if (item == null || item.type == Material.AIR) {
            return false
        }
        return weaponStats.containsKey(item.type)
    }

    /**
     * Calculate total DI from enchantments on a weapon
     * @param item The weapon item
     * @param targetType The target entity type (for conditional enchantments like Smite)
     * @return Total DI percentage (e.g., 50 for +50%)
     */
    fun calculateTotalDI(item: ItemStack?, targetType: EntityType?): Int {
        if (item == null) return 0

        var totalDI = 0
        val weaponType = getWeaponStats(item).weaponType

        for ((enchantment, level) in item.enchantments) {
            val di = enchantmentDI[enchantment] ?: continue

            // Check if this enchantment applies to the target type
            if (di.targetTypes != null && targetType != null) {
                if (targetType !in di.targetTypes) continue
            }

            totalDI += di.diPerLevel * level
        }

        return totalDI
    }
}

/**
 * Mob combat statistics
 */
data class MobStats(
    val physicalDefense: Int,
    val magicDefense: Int,
    val attackPower: Int,
    val rewardMin: Int = 0,
    val rewardMax: Int = 0,
    val rewardChance: Int = 0
)

/**
 * Weapon statistics
 */
data class WeaponStats(
    val baseDamage: Int,
    val skill: SkillType,
    val weaponType: WeaponType
)

/**
 * Enchantment DI (Damage Increase) configuration
 */
data class EnchantmentDI(
    val diPerLevel: Int,
    val targetTypes: Set<EntityType>? // null means applies to all
)
