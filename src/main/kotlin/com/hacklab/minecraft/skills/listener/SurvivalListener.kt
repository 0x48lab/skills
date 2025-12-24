package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class SurvivalListener(private val plugin: Skills) : Listener {

    // Track air bubbles for swimming skill gain
    private val lastAirLevel = mutableMapOf<UUID, Int>()

    /**
     * Athletics skill - reduces fall damage
     * Effect: Fall damage reduced by (skill / 2)% - max 50% at skill 100
     * Skill gain: When taking fall damage
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFallDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.FALL) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val athleticsSkill = data.getSkillValue(SkillType.ATHLETICS)

        // Calculate fall height for difficulty (approximate from damage)
        // Minecraft: 1 damage per block fallen after 3 blocks
        val fallHeight = (event.damage + 3).toInt()
        val difficulty = calculateFallDifficulty(fallHeight)

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.ATHLETICS, difficulty)

        // Apply damage reduction: skill / 2 % (max 50%)
        val reductionPercent = (athleticsSkill / 2.0).coerceAtMost(50.0)
        val damageMultiplier = 1.0 - (reductionPercent / 100.0)
        event.damage = event.damage * damageMultiplier

        // At very high skill (90+), chance to completely negate small falls
        if (athleticsSkill >= 90.0 && fallHeight <= 6) {
            val rollChance = (athleticsSkill - 80.0) * 2  // 20-40% chance at 90-100 skill
            if (Math.random() * 100 < rollChance) {
                event.isCancelled = true
                plugin.messageSender.sendActionBar(player, MessageKey.SURVIVAL_ATHLETICS_ROLL)
            }
        }
    }

    /**
     * Swimming skill - extends underwater breathing time
     * Effect: Air consumption reduced by (skill / 2)% - max 50% at skill 100
     * Also provides slight speed boost underwater
     * Skill gain: When losing air bubbles underwater
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAirChange(event: EntityAirChangeEvent) {
        val player = event.entity as? Player ?: return

        val previousAir = lastAirLevel[player.uniqueId] ?: player.maximumAir
        val newAir = event.amount

        // Only process when losing air (going down)
        if (newAir < previousAir) {
            val data = plugin.playerDataManager.getPlayerData(player)
            val swimmingSkill = data.getSkillValue(SkillType.SWIMMING)

            // Try to gain skill when losing air
            val depth = player.location.block.y
            val difficulty = calculateSwimmingDifficulty(depth, previousAir - newAir)
            plugin.skillManager.tryGainSkill(player, SkillType.SWIMMING, difficulty)

            // Reduce air consumption: skill / 2 % chance to not lose air
            val saveChance = swimmingSkill / 2.0
            if (Math.random() * 100 < saveChance) {
                // Restore some air - effectively slower consumption
                event.amount = previousAir
            }

            // At high skill, apply water breathing effect periodically
            if (swimmingSkill >= 80.0 && !player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
                val duration = ((swimmingSkill - 70.0) * 2).toInt() // 20-60 ticks at 80-100 skill
                if (Math.random() * 100 < 10) { // 10% chance per air tick
                    player.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, true, false))
                }
            }
        }

        lastAirLevel[player.uniqueId] = newAir
    }

    /**
     * Heat Resistance skill - reduces fire and lava damage
     * Effect: Fire/lava damage reduced by (skill / 2)% - max 50% at skill 100
     * Also reduces burn time
     * Skill gain: When taking fire/lava damage
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFireDamage(event: EntityDamageEvent) {
        val validCauses = setOf(
            DamageCause.FIRE,
            DamageCause.FIRE_TICK,
            DamageCause.LAVA,
            DamageCause.HOT_FLOOR  // Magma blocks
        )

        if (event.cause !in validCauses) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val heatResistSkill = data.getSkillValue(SkillType.HEAT_RESISTANCE)

        // Calculate difficulty based on damage source
        val difficulty = when (event.cause) {
            DamageCause.LAVA -> 60       // Hardest - lava
            DamageCause.FIRE -> 30       // Medium - standing in fire
            DamageCause.FIRE_TICK -> 20  // Easy - burning
            DamageCause.HOT_FLOOR -> 25  // Medium-easy - magma blocks
            else -> 20
        }

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.HEAT_RESISTANCE, difficulty)

        // Apply damage reduction: skill / 2 % (max 50%)
        val reductionPercent = (heatResistSkill / 2.0).coerceAtMost(50.0)
        val damageMultiplier = 1.0 - (reductionPercent / 100.0)
        event.damage = event.damage * damageMultiplier

        // Reduce fire ticks (burn time) based on skill
        if (player.fireTicks > 0 && heatResistSkill > 0.0) {
            val tickReduction = (player.fireTicks * (heatResistSkill / 200.0)).toInt()
            player.fireTicks = (player.fireTicks - tickReduction).coerceAtLeast(0)
        }

        // At very high skill (95+), chance to extinguish completely
        if (heatResistSkill >= 95.0 && event.cause == DamageCause.FIRE_TICK) {
            val extinguishChance = (heatResistSkill - 90.0) * 4  // 20-40% at 95-100
            if (Math.random() * 100 < extinguishChance) {
                player.fireTicks = 0
                plugin.messageSender.sendActionBar(player, MessageKey.SURVIVAL_EXTINGUISHED)
            }
        }
    }

    /**
     * Cold Resistance skill - reduces freeze damage from powder snow
     * Effect: Freeze damage reduced by (skill / 2)% - max 50% at skill 100
     * Also reduces freeze buildup rate
     * Skill gain: When taking freeze damage
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFreezeDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.FREEZE) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val coldResistSkill = data.getSkillValue(SkillType.COLD_RESISTANCE)

        // Try to gain skill (difficulty 40 for freeze damage)
        plugin.skillManager.tryGainSkill(player, SkillType.COLD_RESISTANCE, 40)

        // Apply damage reduction: skill / 2 % (max 50%)
        val reductionPercent = (coldResistSkill / 2.0).coerceAtMost(50.0)
        val damageMultiplier = 1.0 - (reductionPercent / 100.0)
        event.damage = event.damage * damageMultiplier

        // Reduce freeze ticks based on skill
        if (player.freezeTicks > 0 && coldResistSkill > 0.0) {
            val tickReduction = (player.freezeTicks * (coldResistSkill / 200.0)).toInt()
            player.freezeTicks = (player.freezeTicks - tickReduction).coerceAtLeast(0)
        }

        // At very high skill (95+), chance to break free from freeze
        if (coldResistSkill >= 95.0) {
            val breakFreeChance = (coldResistSkill - 90.0) * 4  // 20-40% at 95-100
            if (Math.random() * 100 < breakFreeChance) {
                player.freezeTicks = 0
                plugin.messageSender.sendActionBar(player, MessageKey.SURVIVAL_WARMED_UP)
            }
        }
    }

    /**
     * Endurance skill - reduces suffocation damage (being buried in blocks)
     * Effect: Suffocation damage reduced by (skill / 2)% - max 50% at skill 100
     * Skill gain: When taking suffocation damage
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSuffocationDamage(event: EntityDamageEvent) {
        val validCauses = setOf(
            DamageCause.SUFFOCATION,  // Inside solid block
            DamageCause.DROWNING      // Also helps with drowning as backup
        )

        if (event.cause !in validCauses) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val enduranceSkill = data.getSkillValue(SkillType.ENDURANCE)

        // Calculate difficulty based on damage source
        val difficulty = when (event.cause) {
            DamageCause.SUFFOCATION -> 50  // Being crushed/buried
            DamageCause.DROWNING -> 30     // Drowning (backup for swimming)
            else -> 30
        }

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.ENDURANCE, difficulty)

        // Apply damage reduction: skill / 2 % (max 50%)
        val reductionPercent = (enduranceSkill / 2.0).coerceAtMost(50.0)
        val damageMultiplier = 1.0 - (reductionPercent / 100.0)
        event.damage = event.damage * damageMultiplier

        // At very high skill (90+), chance to resist damage completely for this tick
        if (enduranceSkill >= 90.0) {
            val resistChance = (enduranceSkill - 80.0) * 2  // 20-40% at 90-100
            if (Math.random() * 100 < resistChance) {
                event.isCancelled = true
                plugin.messageSender.sendActionBar(player, MessageKey.SURVIVAL_ENDURED)
            }
        }
    }

    /**
     * Calculate fall difficulty based on fall height
     */
    private fun calculateFallDifficulty(fallHeight: Int): Int {
        return when {
            fallHeight <= 5 -> 10    // Small fall
            fallHeight <= 10 -> 25   // Medium fall
            fallHeight <= 20 -> 40   // High fall
            fallHeight <= 30 -> 60   // Very high fall
            else -> 80               // Extreme fall
        }
    }

    /**
     * Calculate swimming difficulty based on depth and air loss rate
     */
    private fun calculateSwimmingDifficulty(yLevel: Int, airLost: Int): Int {
        // Deeper = harder, more air lost = harder
        val depthBonus = when {
            yLevel < 0 -> 20    // Deep ocean / caves
            yLevel < 40 -> 10   // Underwater
            else -> 0           // Surface
        }
        return (15 + depthBonus + airLost).coerceIn(10, 50)
    }

    /**
     * Cleanup when player leaves
     */
    fun removePlayer(playerId: UUID) {
        lastAirLevel.remove(playerId)
    }
}
