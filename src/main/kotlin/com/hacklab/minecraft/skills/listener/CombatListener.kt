package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.combat.MobDifficulty
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent

class CombatListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        // Get the attacker (handle projectiles)
        val attacker = when (val damager = event.damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }

        val target = event.entity

        // Player attacking
        if (attacker != null && target is LivingEntity) {
            // Break hiding on attack
            if (plugin.hidingManager.isHidden(attacker.uniqueId)) {
                plugin.hidingManager.breakHiding(attacker, "attack")
            }

            // Process attack
            val weapon = attacker.inventory.itemInMainHand
            val result = plugin.combatManager.processPlayerAttack(
                attacker, target, weapon, event.damage
            )

            // Check for poison hit
            if (plugin.poisoningManager.isPoisoned(weapon)) {
                plugin.poisoningManager.processPoisonHit(attacker, target, weapon)
            }

            // Modify damage based on calculation
            event.damage = result.damage / plugin.skillsConfig.baseDamageMultiplier
        }

        // Player being attacked
        if (target is Player) {
            val isMagic = event.cause == EntityDamageEvent.DamageCause.MAGIC ||
                    event.cause == EntityDamageEvent.DamageCause.DRAGON_BREATH

            val defenseResult = plugin.combatManager.processPlayerDefense(
                target, attacker, event.damage, isMagic
            )

            event.damage = defenseResult.damage / plugin.skillsConfig.baseDamageMultiplier
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        // Sync internal HP after damage
        if (entity is Player) {
            val data = plugin.playerDataManager.getPlayerData(entity)
            val internalDamage = event.finalDamage * plugin.skillsConfig.baseDamageMultiplier
            data.damage(internalDamage)

            // Break hiding on taking damage
            if (plugin.hidingManager.isHidden(entity.uniqueId)) {
                plugin.hidingManager.breakHiding(entity, "damage")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer

        // Process kill for skill gains
        if (killer != null) {
            plugin.combatManager.processKill(killer, entity)
        }
    }
}
