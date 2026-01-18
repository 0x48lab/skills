package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class CombatListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val target = event.entity

        // Get the attacker (handle projectiles)
        val attacker = when (damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }

        // Determine if this is a ranged attack
        val isRangedAttack = damager is Projectile

        // Calculate distance for ranged attacks
        val distance = if (isRangedAttack && attacker != null) {
            attacker.location.distance(target.location)
        } else {
            0.0
        }

        // Player attacking
        if (attacker != null && target is LivingEntity) {
            // Break hiding/invisibility on attack
            if (plugin.hidingManager.isHidden(attacker.uniqueId)) {
                plugin.hidingManager.breakHiding(attacker, "attack")
            } else if (attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                // Magic invisibility - remove it on attack
                attacker.removePotionEffect(PotionEffectType.INVISIBILITY)
            }

            val weapon = attacker.inventory.itemInMainHand

            val result = if (isRangedAttack) {
                // Ranged attack - hit is already determined by vanilla
                // Pass distance for falloff calculation and movement penalty
                val wasMoving = plugin.combatManager.wasShooterMoving(damager.uniqueId)
                plugin.combatManager.processRangedAttack(
                    attacker, target, weapon, event.damage, distance, wasMoving
                )
            } else {
                // Melee attack - use UO hit chance system
                plugin.combatManager.processPlayerAttack(
                    attacker, target, weapon, event.damage
                )
            }

            // Check if attack hit
            if (!result.isHit) {
                // Miss - cancel damage entirely
                event.isCancelled = true
                return
            }

            // Check for poison hit (only on successful hit)
            if (plugin.poisoningManager.isPoisoned(weapon)) {
                plugin.poisoningManager.processPoisonHit(attacker, target, weapon)
            }

            // Modify damage based on calculation
            // Apply calculated damage directly to mob (already in vanilla scale)
            event.damage = result.damage
        }

        // Player being attacked - apply to internal HP
        if (target is Player) {

            val isMagic = event.cause == EntityDamageEvent.DamageCause.MAGIC ||
                    event.cause == EntityDamageEvent.DamageCause.DRAGON_BREATH

            // Check if this is a projectile attack (arrows can be blocked with shield)
            val defenseResult = plugin.combatManager.processPlayerDefense(
                target, attacker, event.damage, isMagic, isRangedAttack
            )

            // Apply damage to internal HP
            val data = plugin.playerDataManager.getPlayerData(target)
            val internalDamage = defenseResult.damage * plugin.skillsConfig.baseDamageMultiplier
            data.damage(internalDamage)

            // Cancel vanilla damage - we handle HP ourselves
            event.damage = 0.0

            // Sync internal HP to vanilla (this will show the correct health)
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (target.isOnline) {
                    StatCalculator.syncHealthToVanilla(target, data)
                }
            })

            // Break hiding on taking damage
            if (plugin.hidingManager.isHidden(target.uniqueId)) {
                plugin.hidingManager.breakHiding(target, "damage")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity

        // Skip if already handled by onEntityDamageByEntity
        if (event is EntityDamageByEntityEvent) {
            return
        }

        // Handle environmental damage to players (fall, fire, drowning, etc.)
        if (entity is Player) {
            val data = plugin.playerDataManager.getPlayerData(entity)
            val internalDamage = event.damage * plugin.skillsConfig.baseDamageMultiplier
            data.damage(internalDamage)

            // Cancel vanilla damage - we handle HP ourselves
            event.damage = 0.0

            // Sync internal HP to vanilla
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (entity.isOnline) {
                    StatCalculator.syncHealthToVanilla(entity, data)
                }
            })

            // Break hiding on taking damage
            if (plugin.hidingManager.isHidden(entity.uniqueId)) {
                plugin.hidingManager.breakHiding(entity, "damage")
            }
        }
    }

    /**
     * Handle vanilla health regeneration.
     * All regeneration sources are synced to internal HP system.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity

        if (entity is Player) {
            // Apply all healing to internal HP
            val data = plugin.playerDataManager.getPlayerData(entity)
            val internalHeal = event.amount * plugin.skillsConfig.baseDamageMultiplier
            data.heal(internalHeal)

            // Cancel vanilla healing and sync internal HP to vanilla
            // This ensures internal HP is the source of truth
            event.isCancelled = true
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (entity.isOnline) {
                    StatCalculator.syncHealthToVanilla(entity, data)
                }
            })
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val killer = entity.killer

        // Process kill for skill gains (Anatomy)
        if (killer != null) {
            plugin.combatManager.processKill(killer, entity)
        }
    }

    /**
     * Track player movement state when shooting bow/crossbow
     * Used to apply movement penalty to ranged attacks
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityShootBow(event: EntityShootBowEvent) {
        val shooter = event.entity
        val projectile = event.projectile

        if (shooter is Player) {
            val wasMoving = plugin.combatManager.isPlayerMoving(shooter)
            plugin.combatManager.recordProjectileShot(projectile.uniqueId, wasMoving)
        }
    }

    /**
     * Handle spell fireball hits
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity

        // Check if this is a spell fireball
        if (projectile !is SmallFireball) return

        val pdc = projectile.persistentDataContainer
        val casterUuidStr = pdc.get(plugin.spellManager.spellCasterKey, PersistentDataType.STRING) ?: return
        val damage = pdc.get(plugin.spellManager.spellDamageKey, PersistentDataType.DOUBLE) ?: return

        val caster = plugin.server.getPlayer(UUID.fromString(casterUuidStr))
        val hitEntity = event.hitEntity
        val hitBlock = event.hitBlock
        val world = projectile.world

        if (hitEntity != null && hitEntity is LivingEntity && hitEntity.uniqueId.toString() != casterUuidStr) {
            // Hit an entity - apply magic damage
            plugin.spellManager.applyMagicDamage(caster, hitEntity, damage)

            // Apply fire effect
            hitEntity.fireTicks = 60  // 3 seconds

            // Impact particles and sound
            val loc = hitEntity.location.add(0.0, 1.0, 0.0)
            world.spawnParticle(Particle.FLAME, loc, 40, 0.6, 0.6, 0.6, 0.1)
            world.spawnParticle(Particle.LAVA, loc, 20, 0.4, 0.4, 0.4, 0.0)
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f)
        } else if (hitBlock != null) {
            // Hit a block - just explosion effect
            val loc = hitBlock.location.add(0.5, 0.5, 0.5)
            world.spawnParticle(Particle.FLAME, loc, 30, 0.5, 0.5, 0.5, 0.1)
            world.spawnParticle(Particle.LAVA, loc, 15, 0.4, 0.4, 0.4, 0.0)
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f)
        }
    }
}
