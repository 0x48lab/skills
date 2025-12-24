package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.magic.TargetResult
import com.hacklab.minecraft.skills.magic.TargetingAction
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class TargetingListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val target = event.rightClicked

        // Check if player is in targeting mode
        if (!plugin.targetManager.isTargeting(player.uniqueId)) {
            // Handle normal pet healing (Veterinary)
            if (target is LivingEntity && plugin.tamingManager.isOwner(player, target)) {
                val food = player.inventory.itemInMainHand
                if (plugin.veterinaryManager.isHealingFood(food.type)) {
                    plugin.veterinaryManager.tryHeal(player, target, food)
                    event.isCancelled = true
                }
            }
            return
        }

        // Process targeting
        event.isCancelled = true

        val result = plugin.targetManager.processEntityTarget(player, target)
        if (result is TargetResult.EntityTarget) {
            val state = plugin.targetManager.getTargetingState(player.uniqueId)
                ?: return // State was already cleared

            handleTargetAction(player, state.action, result.entity as? LivingEntity, null)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        // Only process main hand events to prevent double-firing
        if (event.hand != org.bukkit.inventory.EquipmentSlot.HAND) {
            return
        }

        // Check if player is in spell casting targeting phase
        val castingState = plugin.castingManager.getCastingState(player.uniqueId)
        if (castingState != null && castingState.phase == com.hacklab.minecraft.skills.magic.CastingManager.CastPhase.TARGETING) {
            // Left-click fires the spell
            if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
                plugin.castingManager.processTargetClick(player)
                return
            }
            // Right-click cancels
            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                event.isCancelled = true
                plugin.castingManager.cancelCasting(player.uniqueId, silent = false)
                plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.MAGIC_CANCELLED)
                return
            }
        }

        // Check if player is in targeting mode (old system)
        if (!plugin.targetManager.isTargeting(player.uniqueId)) {
            // Handle scroll usage
            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                val item = player.inventory.itemInMainHand
                if (plugin.scrollManager.isScroll(item)) {
                    // Prevent double-firing with cooldown
                    if (plugin.cooldownManager.isOnCooldown(player.uniqueId, com.hacklab.minecraft.skills.util.CooldownAction.USE_SCROLL)) {
                        event.isCancelled = true
                        return
                    }
                    plugin.cooldownManager.setCooldown(player.uniqueId, com.hacklab.minecraft.skills.util.CooldownAction.USE_SCROLL)
                    plugin.scrollManager.useScroll(player, item)
                    event.isCancelled = true
                }
            }
            return
        }

        val state = plugin.targetManager.getTargetingState(player.uniqueId)
            ?: return

        // Right-click cancels targeting
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            plugin.targetManager.cancelTargeting(player.uniqueId)
            event.isCancelled = true
            return
        }

        // Left-click for targeting (entity or location)
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            event.isCancelled = true

            // Use raycast to find entity or location target
            val raycastResult = plugin.spellManager.raycastForEntity(player, 10.0)

            if (raycastResult.entity != null) {
                // Found an entity
                val result = plugin.targetManager.processEntityTarget(player, raycastResult.entity)
                if (result is TargetResult.EntityTarget) {
                    handleTargetAction(player, state.action, result.entity as? LivingEntity, null)
                }
            } else if (event.clickedBlock != null) {
                // No entity, use block location
                val result = plugin.targetManager.processLocationTarget(player, event.clickedBlock!!.location)
                if (result is TargetResult.LocationTarget) {
                    handleTargetAction(player, state.action, null, result.location)
                }
            }
        }
    }

    private fun handleTargetAction(
        player: Player,
        action: TargetingAction,
        entityTarget: LivingEntity?,
        locationTarget: org.bukkit.Location?
    ) {
        when (action) {
            is TargetingAction.CastSpell -> {
                if (entityTarget != null || locationTarget != null) {
                    plugin.spellManager.finalizeCast(
                        player, action.spell, entityTarget, locationTarget, action.useScroll
                    )
                }
            }
            is TargetingAction.Evaluate -> {
                if (entityTarget is Player) {
                    val targetData = plugin.playerDataManager.getPlayerData(entityTarget)
                    val accuracy = plugin.playerDataManager.getPlayerData(player)
                        .getSkillValue(com.hacklab.minecraft.skills.skill.SkillType.EVALUATING_INTELLIGENCE)

                    // Add some inaccuracy based on skill
                    val manaDisplay = if (accuracy >= 90) {
                        "${targetData.mana.toInt()} / ${targetData.maxMana.toInt()}"
                    } else {
                        val variance = ((100 - accuracy) / 100.0 * targetData.mana * 0.2).toInt()
                        val displayMana = (targetData.mana + (-variance..variance).random()).toInt()
                        "~$displayMana / ${targetData.maxMana.toInt()}"
                    }

                    plugin.messageSender.send(player, MessageKey.EVALUATE_RESULT,
                        "player" to entityTarget.name,
                        "mana" to manaDisplay)

                    plugin.skillManager.tryGainSkill(
                        player,
                        com.hacklab.minecraft.skills.skill.SkillType.EVALUATING_INTELLIGENCE,
                        50
                    )
                }
            }
            is TargetingAction.Lore -> {
                if (entityTarget != null) {
                    plugin.animalLoreManager.showLore(player, entityTarget)
                }
            }
            is TargetingAction.Tame -> {
                if (entityTarget != null) {
                    plugin.tamingManager.tryTame(player, entityTarget)
                }
            }
            is TargetingAction.Snoop -> {
                if (entityTarget is Player) {
                    plugin.snoopingManager.trySnoop(player, entityTarget)
                }
            }
            is TargetingAction.Detect -> {
                // Detect doesn't use targeting result, it's area-based
                plugin.detectingManager.tryDetect(player)
            }
        }
    }
}
