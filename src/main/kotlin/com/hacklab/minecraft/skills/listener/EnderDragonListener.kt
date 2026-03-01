package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import org.bukkit.Material
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.EnderDragon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*

class EnderDragonListener(private val plugin: Skills) : Listener {

    private val dragonManager get() = plugin.enderDragonManager

    /**
     * Handle dragon death - XP scaling, drops, egg control, kill tracking
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity !is EnderDragon) return

        // Scale XP
        event.droppedExp = dragonManager.getScaledXp()

        // Add equipment drop
        event.drops.add(dragonManager.dropGenerator.generateEquipmentDrop())

        // Add scroll drop
        event.drops.add(dragonManager.dropGenerator.generateScrollDrop())

        // Remove dragon egg from drops after first kill
        if (!dragonManager.shouldDropEgg()) {
            event.drops.removeIf { it.type == Material.DRAGON_EGG }
        }

        // Track the kill
        dragonManager.onDragonKilled()
    }

    /**
     * Handle explosion immunity and heal aura damage tracking
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is EnderDragon) return

        // Explosion immunity skill
        if (dragonManager.hasSkill(com.hacklab.minecraft.skills.dragon.DragonSkillType.EXPLOSION_IMMUNITY)) {
            if (event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
            ) {
                event.isCancelled = true
                return
            }
        }

        // Record damage for heal aura
        if (dragonManager.hasSkill(com.hacklab.minecraft.skills.dragon.DragonSkillType.HEAL_AURA)) {
            dragonManager.skillExecutor.recordDragonDamage()
        }
    }

    /**
     * Apply damage scale when dragon attacks entities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is EnderDragon) return

        val scale = dragonManager.getDamageScale()
        if (scale > 1.0) {
            event.damage = event.damage * scale
        }
    }

    /**
     * Detect dragon spawn - apply stats and start skills
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val entity = event.entity
        if (entity !is EnderDragon) return

        // Delay slightly to ensure dragon is fully initialized
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (entity.isValid) {
                dragonManager.onDragonSpawned(entity)
            }
        }, 5L)
    }

    /**
     * Handle crystal destruction for crystal regeneration skill
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        if (entity !is EnderCrystal) return

        if (dragonManager.hasSkill(com.hacklab.minecraft.skills.dragon.DragonSkillType.CRYSTAL_REGENERATION)) {
            dragonManager.skillExecutor.onCrystalDestroyed(event.location)
        }
    }

    /**
     * Extend dragon breath duration based on kill count
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity = event.entity
        if (entity !is AreaEffectCloud) return

        val source = entity.source
        if (source !is EnderDragon) return

        val killCount = dragonManager.killCount
        if (killCount > 0) {
            val baseDuration = entity.duration
            entity.duration = baseDuration + killCount * 10
        }
    }
}
