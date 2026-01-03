package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.util.WeaponUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class PoisoningManager(private val plugin: Skills) {
    private val poisonChargesKey = NamespacedKey(plugin, "poison_charges")
    private val poisonLevelKey = NamespacedKey(plugin, "poison_level")

    // Track poisoned entities for custom damage
    private val poisonedEntities: MutableMap<UUID, PoisonEffect> = ConcurrentHashMap()

    data class PoisonEffect(
        val level: PoisonLevel,
        var ticksRemaining: Int,
        val attacker: UUID
    )

    init {
        // Start poison damage task
        startPoisonDamageTask()
    }

    /**
     * Apply poison to a held weapon
     */
    fun applyPoison(player: Player): Boolean {
        val weapon = player.inventory.itemInMainHand

        // Must be a weapon
        if (!WeaponUtil.isWeapon(weapon.type)) {
            plugin.messageSender.send(player, MessageKey.THIEF_POISON_NO_WEAPON)
            return false
        }

        // Find the best poison potion in inventory
        val (poisonSlot, poisonLevel) = findBestPoisonPotion(player)
        if (poisonSlot == -1 || poisonLevel == null) {
            plugin.messageSender.send(player, MessageKey.THIEF_POISON_NO_POTION)
            return false
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val poisoningSkill = data.getSkillValue(SkillType.POISONING)

        // Calculate charges: skill / 20 (max 5)
        val charges = (poisoningSkill / 20.0).toInt().coerceIn(1, 5)

        // Success chance based on skill vs poison difficulty
        // Higher level poisons are harder to apply
        val difficultyDelta = poisoningSkill - poisonLevel.applyDifficulty
        val successChance = (50.0 + difficultyDelta).coerceIn(10.0, 95.0)

        // Try skill gain with appropriate difficulty
        plugin.skillManager.tryGainSkill(player, SkillType.POISONING, poisonLevel.applyDifficulty)

        if (Random.nextDouble() * 100 > successChance) {
            plugin.messageSender.send(player, MessageKey.THIEF_POISON_FAILED)
            // Do NOT consume potion on failure (per spec)
            return false
        }

        // Apply poison to weapon
        setPoisonData(weapon, charges, poisonLevel)

        // Consume potion
        consumePoisonPotion(player, poisonSlot)

        // Update weapon lore
        val isJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE
        updatePoisonLore(weapon, charges, poisonLevel, isJapanese)

        val levelName = if (isJapanese) poisonLevel.displayNameJa else poisonLevel.displayName
        plugin.messageSender.send(
            player, MessageKey.THIEF_POISON_APPLIED,
            "charges" to charges,
            "level" to levelName
        )
        return true
    }

    /**
     * Process a poison hit
     */
    fun processPoisonHit(attacker: Player, target: LivingEntity, weapon: ItemStack): Boolean {
        val charges = getPoisonCharges(weapon)
        if (charges <= 0) return false

        val poisonLevel = getPoisonLevel(weapon) ?: PoisonLevel.LESSER
        val data = plugin.playerDataManager.getPlayerData(attacker)
        val poisoningSkill = data.getSkillValue(SkillType.POISONING)

        // Duration based on skill: 3-8 seconds (60-160 ticks)
        val durationTicks = (60 + (poisoningSkill * 1.0)).toInt()

        // Apply custom poison effect
        applyPoisonEffect(target, poisonLevel, durationTicks, attacker.uniqueId)

        // Reduce charges
        val newCharges = charges - 1
        if (newCharges <= 0) {
            removePoisonData(weapon)
        } else {
            setPoisonData(weapon, newCharges, poisonLevel)
            val isJapanese = plugin.localeManager.getLanguage(attacker) == Language.JAPANESE
            updatePoisonLore(weapon, newCharges, poisonLevel, isJapanese)
        }

        // Try skill gain with appropriate difficulty
        plugin.skillManager.tryGainSkill(attacker, SkillType.POISONING, poisonLevel.applyDifficulty)

        plugin.messageSender.sendActionBar(attacker, MessageKey.THIEF_POISON_HIT)
        return true
    }

    /**
     * Apply custom poison effect to target
     */
    private fun applyPoisonEffect(target: LivingEntity, level: PoisonLevel, durationTicks: Int, attacker: UUID) {
        // Override existing poison with new one (UO behavior - last poison wins)
        poisonedEntities[target.uniqueId] = PoisonEffect(level, durationTicks, attacker)

        // Visual indicator - greenish particles
        target.world.spawnParticle(
            org.bukkit.Particle.HAPPY_VILLAGER,
            target.location.add(0.0, 1.0, 0.0),
            10, 0.3, 0.5, 0.3, 0.0
        )
    }

    /**
     * Poison damage task - runs every second (20 ticks)
     */
    private fun startPoisonDamageTask() {
        object : BukkitRunnable() {
            override fun run() {
                val iterator = poisonedEntities.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val entityId = entry.key
                    val effect = entry.value

                    // Find entity
                    val entity = plugin.server.getEntity(entityId) as? LivingEntity
                    if (entity == null || entity.isDead) {
                        iterator.remove()
                        continue
                    }

                    // Apply damage
                    val damage = effect.level.damagePerTick

                    // Don't kill with poison (leave at 1 HP minimum) - UO style
                    if (entity.health > damage + 1) {
                        entity.damage(damage)

                        // Poison particles
                        entity.world.spawnParticle(
                            org.bukkit.Particle.DUST,
                            entity.location.add(0.0, 1.0, 0.0),
                            5,
                            0.2, 0.3, 0.2,
                            org.bukkit.Particle.DustOptions(
                                org.bukkit.Color.fromRGB(0, 100, 0),
                                1.0f
                            )
                        )
                    }

                    // Reduce duration
                    effect.ticksRemaining -= 20
                    if (effect.ticksRemaining <= 0) {
                        iterator.remove()
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L)  // Every 1 second
    }

    /**
     * Find the best (highest level) poison potion in player's inventory
     * Returns pair of (slot, level) or (-1, null) if not found
     */
    private fun findBestPoisonPotion(player: Player): Pair<Int, PoisonLevel?> {
        var bestSlot = -1
        var bestLevel: PoisonLevel? = null

        for (i in 0 until player.inventory.size) {
            val item = player.inventory.getItem(i) ?: continue
            val level = plugin.poisonItemManager.getPoisonLevel(item)
            if (level != null) {
                if (bestLevel == null || level.ordinal > bestLevel.ordinal) {
                    bestSlot = i
                    bestLevel = level
                }
            }
        }

        return Pair(bestSlot, bestLevel)
    }

    /**
     * Get poison charges on a weapon
     */
    fun getPoisonCharges(weapon: ItemStack?): Int {
        if (weapon == null) return 0
        return weapon.itemMeta?.persistentDataContainer
            ?.get(poisonChargesKey, PersistentDataType.INTEGER) ?: 0
    }

    /**
     * Get poison level on a weapon
     */
    fun getPoisonLevel(weapon: ItemStack?): PoisonLevel? {
        if (weapon == null) return null
        val levelName = weapon.itemMeta?.persistentDataContainer
            ?.get(poisonLevelKey, PersistentDataType.STRING) ?: return null
        return try {
            PoisonLevel.valueOf(levelName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Check if weapon is poisoned
     */
    fun isPoisoned(weapon: ItemStack?): Boolean {
        return getPoisonCharges(weapon) > 0
    }

    private fun setPoisonData(weapon: ItemStack, charges: Int, level: PoisonLevel) {
        val meta = weapon.itemMeta ?: return
        meta.persistentDataContainer.set(poisonChargesKey, PersistentDataType.INTEGER, charges)
        meta.persistentDataContainer.set(poisonLevelKey, PersistentDataType.STRING, level.name)
        weapon.itemMeta = meta
    }

    private fun removePoisonData(weapon: ItemStack) {
        val meta = weapon.itemMeta ?: return
        meta.persistentDataContainer.remove(poisonChargesKey)
        meta.persistentDataContainer.remove(poisonLevelKey)

        // Remove poison lore
        val lore = meta.lore()?.toMutableList() ?: return
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component)
            plain.contains("Poison") || plain.contains("毒")
        }
        meta.lore(lore)
        weapon.itemMeta = meta
    }

    private fun updatePoisonLore(weapon: ItemStack, charges: Int, level: PoisonLevel, useJapanese: Boolean) {
        val meta = weapon.itemMeta ?: return
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        // Remove existing poison lore
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component)
            plain.contains("Poison") || plain.contains("毒")
        }

        // Add new poison lore
        val displayName = if (useJapanese) level.displayNameJa else level.displayName
        val chargeText = if (useJapanese) "回" else " charges"
        lore.add(
            Component.text("☠ $displayName ($charges$chargeText)")
                .color(level.color)
        )

        meta.lore(lore)
        weapon.itemMeta = meta
    }

    private fun consumePoisonPotion(player: Player, slot: Int) {
        val item = player.inventory.getItem(slot) ?: return
        if (item.amount > 1) {
            item.amount -= 1
        } else {
            player.inventory.setItem(slot, ItemStack(Material.GLASS_BOTTLE))
        }
    }

    /**
     * Check if entity is poisoned
     */
    fun isPoisoned(entity: LivingEntity): Boolean {
        return poisonedEntities.containsKey(entity.uniqueId)
    }

    /**
     * Clear poison from entity
     */
    fun clearPoison(entity: LivingEntity) {
        poisonedEntities.remove(entity.uniqueId)
    }
}
