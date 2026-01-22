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
     * Effect: Fall damage reduced by skill% - linear from 0% to 100%
     * Skill gain: When taking fall damage
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

        // GM (skill 100) - completely immune, no screen shake
        if (athleticsSkill >= 100.0) {
            event.isCancelled = true
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = athleticsSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
        event.damage = event.damage * damageMultiplier
    }

    /**
     * Swimming skill - extends underwater breathing time
     * Effect: Air consumption reduced by (skill / 2)% chance
     * Also provides slight speed boost underwater
     * Note: Skill gain is handled by onDrowningDamage, not here
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
     * Swimming skill gain - when taking drowning damage
     * Skill gain: When taking drowning damage (consistent with other survival skills)
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onDrowningDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.DROWNING) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val swimmingSkill = data.getSkillValue(SkillType.SWIMMING)

        // Calculate difficulty based on depth
        val depth = player.location.block.y
        val difficulty = when {
            depth < 0 -> 50    // Deep ocean / underwater caves
            depth < 40 -> 35   // Underwater
            else -> 20         // Near surface
        }

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.SWIMMING, difficulty)

        // GM (skill 100) - completely immune, no screen shake
        if (swimmingSkill >= 100.0) {
            event.isCancelled = true
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = swimmingSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
        event.damage = event.damage * damageMultiplier
    }

    /**
     * Heat Resistance skill - reduces fire and lava damage
     * Effect: Fire/lava damage reduced by skill% - linear from 0% to 100%
     * Also reduces burn time
     * Skill gain: When taking fire/lava damage
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

        // GM (skill 100) - completely immune, no screen shake, extinguish fire
        if (heatResistSkill >= 100.0) {
            event.isCancelled = true
            player.fireTicks = 0  // Extinguish any fire
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = heatResistSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
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
     * Effect: Freeze damage reduced by skill% - linear from 0% to 100%
     * Also reduces freeze buildup rate
     * Skill gain: When taking freeze damage
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onFreezeDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.FREEZE) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val coldResistSkill = data.getSkillValue(SkillType.COLD_RESISTANCE)

        // Try to gain skill (difficulty 40 for freeze damage)
        plugin.skillManager.tryGainSkill(player, SkillType.COLD_RESISTANCE, 40)

        // GM (skill 100) - completely immune, no screen shake, clear freeze
        if (coldResistSkill >= 100.0) {
            event.isCancelled = true
            player.freezeTicks = 0  // Clear freeze
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = coldResistSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
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
     * Effect: Suffocation damage reduced by skill% - linear from 0% to 100%
     * Skill gain: When taking suffocation damage
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     * Note: Drowning is handled by Swimming skill (onDrowningDamage)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSuffocationDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.SUFFOCATION) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val enduranceSkill = data.getSkillValue(SkillType.ENDURANCE)

        // Difficulty for suffocation (being crushed/buried)
        val difficulty = 50

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.ENDURANCE, difficulty)

        // GM (skill 100) - completely immune, no screen shake
        if (enduranceSkill >= 100.0) {
            event.isCancelled = true
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = enduranceSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
        event.damage = event.damage * damageMultiplier
    }

    /**
     * Endurance skill - reduces contact damage (cactus, berry bush, etc.)
     * Effect: Contact damage reduced by skill% - linear from 0% to 100%
     * Skill gain: When taking contact damage
     * Note: Runs at NORMAL priority so CombatListener (HIGH) can apply to internal HP
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onContactDamage(event: EntityDamageEvent) {
        if (event.cause != DamageCause.CONTACT) return
        val player = event.entity as? Player ?: return

        val data = plugin.playerDataManager.getPlayerData(player)
        val enduranceSkill = data.getSkillValue(SkillType.ENDURANCE)

        // Difficulty for contact damage (cactus is easy to avoid)
        val difficulty = 15

        // Try to gain skill
        plugin.skillManager.tryGainSkill(player, SkillType.ENDURANCE, difficulty)

        // GM (skill 100) - completely immune, no screen shake
        if (enduranceSkill >= 100.0) {
            event.isCancelled = true
            return
        }

        // Compensate for internal HP system (x10 multiplier)
        // Internal HP max = 100 (STR 0), Vanilla HP max = 20
        // To match vanilla: internal damage = vanilla damage * 5 (100/20)
        // Since CombatListener multiplies by 10, we use 0.5 here (0.5 * 10 = 5)
        // Skill 0 = 0% reduction (vanilla), Skill 100 = 100% reduction (immune)
        val baseMultiplier = 0.5  // Converts to 5x total (matches vanilla percentage)
        val skillReductionPercent = enduranceSkill.coerceAtMost(100.0)
        val damageMultiplier = baseMultiplier * (1.0 - (skillReductionPercent / 100.0))
        event.damage = event.damage * damageMultiplier
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
     * Cleanup when player leaves
     */
    fun removePlayer(playerId: UUID) {
        lastAirLevel.remove(playerId)
    }
}
