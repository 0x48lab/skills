package com.hacklab.minecraft.skills.magic

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.StatCalculator
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class SpellManager(private val plugin: Skills) {

    /**
     * Attempt to cast a spell
     * @param targetPlayer For PLAYER_OR_SELF spells, the pre-selected target player (from command argument)
     */
    fun castSpell(player: Player, spell: SpellType, useScroll: Boolean = false, targetPlayer: Player? = null): CastResult {
        val data = plugin.playerDataManager.getPlayerData(player)

        // Check if using scroll
        if (useScroll) {
            // Scrolls bypass spellbook and reagent requirements
            // But still need mana (reduced for scrolls)
            val scrollManaCost = spell.baseMana / 2.0
            if (data.mana < scrollManaCost) {
                plugin.messageSender.send(player, MessageKey.MAGIC_NO_MANA)
                return CastResult.NO_MANA
            }
        } else {
            // Check spellbook
            if (!plugin.spellbookManager.hasSpell(player, spell)) {
                plugin.messageSender.send(player, MessageKey.MAGIC_SPELL_NOT_IN_BOOK)
                return CastResult.NO_SPELLBOOK
            }

            // Check reagents
            if (!plugin.reagentManager.hasReagents(player, spell)) {
                plugin.messageSender.send(player, MessageKey.MAGIC_NO_REAGENTS)
                return CastResult.NO_REAGENTS
            }

            // Check mana
            if (!plugin.manaManager.hasEnoughMana(player, spell)) {
                plugin.messageSender.send(player, MessageKey.MAGIC_NO_MANA)
                return CastResult.NO_MANA
            }
        }

        // Start casting
        plugin.messageSender.send(player, MessageKey.MAGIC_CAST_START, "spell" to spell.displayName)

        // Calculate casting time based on circle
        val baseCastTime = plugin.skillsConfig.castingTimeBase
        val castTime = baseCastTime + (spell.circle.number * 500L)

        // Start cast sequence
        startCastSequence(player, spell, castTime, useScroll, targetPlayer)

        return CastResult.CASTING
    }

    private fun startCastSequence(player: Player, spell: SpellType, castTime: Long, useScroll: Boolean, targetPlayer: Player? = null) {
        // Use the new CastingManager for casting with BossBar display
        plugin.castingManager.startCasting(player, spell, useScroll, targetPlayer)
    }

    /**
     * Raycast to find an entity in the player's line of sight
     */
    fun raycastForEntity(player: Player, maxDistance: Double): RaycastResult {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction

        // First check for entities along the ray
        val rayTraceResult = player.world.rayTraceEntities(
            eyeLocation,
            direction,
            maxDistance,
            1.2  // Ray size for more forgiving target selection
        ) { entity -> entity != player && entity is LivingEntity }

        if (rayTraceResult != null && rayTraceResult.hitEntity != null) {
            return RaycastResult(
                rayTraceResult.hitEntity as? LivingEntity,
                rayTraceResult.hitPosition.toLocation(player.world)
            )
        }

        // If no entity hit, check for block
        val blockResult = player.world.rayTraceBlocks(
            eyeLocation,
            direction,
            maxDistance
        )

        val hitLocation = blockResult?.hitPosition?.toLocation(player.world)
            ?: eyeLocation.add(direction.multiply(maxDistance))

        return RaycastResult(null, hitLocation)
    }

    /**
     * Raycast to find a location (block or max range)
     */
    fun raycastForLocation(player: Player, maxDistance: Double): RaycastResult {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction

        // Check for entities first (for area spells that can hit entities)
        val entityResult = player.world.rayTraceEntities(
            eyeLocation,
            direction,
            maxDistance,
            1.2  // Ray size for more forgiving target selection
        ) { entity -> entity != player && entity is LivingEntity }

        if (entityResult != null && entityResult.hitEntity != null) {
            return RaycastResult(
                entityResult.hitEntity as? LivingEntity,
                entityResult.hitPosition.toLocation(player.world)
            )
        }

        // Check for block
        val blockResult = player.world.rayTraceBlocks(
            eyeLocation,
            direction,
            maxDistance
        )

        val hitLocation = blockResult?.hitPosition?.toLocation(player.world)
            ?: eyeLocation.add(direction.multiply(maxDistance))

        return RaycastResult(null, hitLocation)
    }

    data class RaycastResult(
        val entity: LivingEntity?,
        val location: Location
    )

    /**
     * Finalize spell cast after targeting (if applicable)
     */
    fun finalizeCast(
        caster: Player,
        spell: SpellType,
        entityTarget: LivingEntity?,
        locationTarget: Location?,
        useScroll: Boolean
    ): Boolean {
        val data = plugin.playerDataManager.getPlayerData(caster)

        // Consume scroll first (consumed on both success and failure, but not on cancel)
        if (useScroll) {
            plugin.scrollManager.consumeScroll(caster, spell)
        }

        // Calculate success chance
        val magerySkill = data.getSkillValue(SkillType.MAGERY)
        val intBonus = data.int / 5.0  // +20% at INT 100
        val baseSuccess = 50.0 + magerySkill - (spell.circle.number * 10) + intBonus
        val successChance = baseSuccess.coerceIn(5.0, 95.0)

        // Roll for success
        if (Random.nextDouble() * 100 > successChance) {
            plugin.messageSender.send(caster, MessageKey.MAGIC_CAST_FAILED, "spell" to spell.displayName)
            // Play fizzle sound (fart-like "puff" sound)
            caster.world.playSound(caster.location, Sound.ENTITY_PLAYER_BURP, 0.5f, 0.5f)
            // Still consume resources on failure (reagents only if not using scroll)
            if (!useScroll) {
                plugin.reagentManager.consumeReagents(caster, spell)
            }
            plugin.manaManager.consumeMana(caster, spell)
            return false
        }

        // Consume resources
        if (!useScroll) {
            plugin.reagentManager.consumeReagents(caster, spell)
        }
        plugin.manaManager.consumeMana(caster, spell)

        // Try skill gain (only when casting from spellbook, not scrolls)
        if (!useScroll) {
            plugin.skillManager.tryGainSkill(caster, SkillType.MAGERY, spell.difficulty)
        }

        // Execute spell effect
        executeSpellEffect(caster, spell, entityTarget, locationTarget)

        plugin.messageSender.send(caster, MessageKey.MAGIC_CAST_SUCCESS, "spell" to spell.displayName)
        return true
    }

    private fun executeSpellEffect(
        caster: Player,
        spell: SpellType,
        entityTarget: LivingEntity?,
        locationTarget: Location?
    ) {
        val casterData = plugin.playerDataManager.getPlayerData(caster)
        val evalIntSkill = casterData.getSkillValue(SkillType.EVALUATING_INTELLIGENCE)
        val magerySkill = casterData.getSkillValue(SkillType.MAGERY)

        when (spell) {
            // === Attack Spells ===
            SpellType.MAGIC_ARROW -> {
                // Fire a magic projectile towards the target location
                val targetLoc = locationTarget ?: return
                val eyeLoc = caster.eyeLocation
                val world = eyeLoc.world ?: return

                // Calculate direction from caster to target
                val direction = targetLoc.clone().subtract(eyeLoc).toVector().normalize()
                val maxDistance = 30.0
                val speed = 1.0  // blocks per tick
                val calculatedDamage = StatCalculator.calculateMagicDamage(spell.baseDamage, magerySkill, evalIntSkill)

                // Play launch sound
                world.playSound(eyeLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f)

                // Spawn initial particle at caster's position
                world.spawnParticle(Particle.WITCH, eyeLoc, 10, 0.2, 0.2, 0.2, 0.05)

                // Create projectile task with captured values
                val startX = eyeLoc.x
                val startY = eyeLoc.y
                val startZ = eyeLoc.z
                val dirX = direction.x
                val dirY = direction.y
                val dirZ = direction.z
                val casterRef = caster

                object : BukkitRunnable() {
                    var traveled = 0.0

                    override fun run() {
                        traveled += speed

                        // Calculate current position
                        val currentX = startX + dirX * traveled
                        val currentY = startY + dirY * traveled
                        val currentZ = startZ + dirZ * traveled
                        val currentLoc = Location(world, currentX, currentY, currentZ)

                        // Spawn trail particles (more visible)
                        world.spawnParticle(Particle.WITCH, currentLoc, 5, 0.1, 0.1, 0.1, 0.02)
                        world.spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0.0)

                        // Check for entity collision
                        val nearbyEntities = world.getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)
                            .filterIsInstance<LivingEntity>()
                            .filter { it.uniqueId != casterRef.uniqueId }

                        if (nearbyEntities.isNotEmpty()) {
                            val hitTarget = nearbyEntities.first()
                            applyMagicDamage(casterRef, hitTarget, calculatedDamage)
                            // Impact effect
                            world.spawnParticle(Particle.WITCH, hitTarget.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.4, 0.4, 0.1)
                            world.playSound(hitTarget.location, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f)
                            cancel()
                            return
                        }

                        // Check for block collision
                        if (currentLoc.block.type.isSolid) {
                            // Hit a block - impact effect
                            world.spawnParticle(Particle.WITCH, currentLoc, 15, 0.3, 0.3, 0.3, 0.1)
                            world.playSound(currentLoc, Sound.BLOCK_STONE_HIT, 0.5f, 1.5f)
                            cancel()
                            return
                        }

                        // Check max distance
                        if (traveled >= maxDistance) {
                            cancel()
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L)  // Start after 1 tick delay
            }

            SpellType.HARM -> {
                entityTarget?.let { target ->
                    val damage = StatCalculator.calculateMagicDamage(spell.baseDamage, magerySkill, evalIntSkill)
                    applyMagicDamage(caster, target, damage)
                    // Visual: Dark damage effect
                    target.world.spawnParticle(Particle.DAMAGE_INDICATOR, target.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.5, 0.3, 0.1)
                    target.world.spawnParticle(Particle.SMOKE, target.location.add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.05)
                    target.world.playSound(target.location, Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f)
                }
            }

            SpellType.ENERGY_BOLT -> {
                entityTarget?.let { target ->
                    val damage = StatCalculator.calculateMagicDamage(spell.baseDamage, magerySkill, evalIntSkill)
                    applyMagicDamage(caster, target, damage)
                    // Visual: Purple energy bolt effect
                    val world = target.world
                    val targetLoc = target.location.add(0.0, 1.0, 0.0)
                    world.spawnParticle(Particle.DRAGON_BREATH, targetLoc, 30, 0.3, 0.5, 0.3, 0.1)
                    world.spawnParticle(Particle.END_ROD, targetLoc, 20, 0.4, 0.6, 0.4, 0.05)
                    world.spawnParticle(Particle.PORTAL, targetLoc, 25, 0.3, 0.5, 0.3, 0.2)
                    world.playSound(target.location, Sound.ENTITY_ENDERMAN_HURT, 1.0f, 1.5f)
                    world.playSound(target.location, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f)
                }
            }

            SpellType.FIREBALL -> {
                // Fire a fireball projectile towards the target location
                val targetLoc = locationTarget ?: return
                val eyeLoc = caster.eyeLocation
                val world = eyeLoc.world ?: return

                // Calculate direction from caster to target
                val direction = targetLoc.clone().subtract(eyeLoc).toVector().normalize()
                val maxDistance = 40.0
                val speed = 0.8  // blocks per tick (slower than magic arrow)
                val calculatedDamage = StatCalculator.calculateMagicDamage(spell.baseDamage, magerySkill, evalIntSkill)

                // Play launch sound
                world.playSound(eyeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f)

                // Spawn initial fire burst at caster
                world.spawnParticle(Particle.FLAME, eyeLoc, 15, 0.3, 0.3, 0.3, 0.05)

                // Create projectile task with captured values
                val startX = eyeLoc.x
                val startY = eyeLoc.y
                val startZ = eyeLoc.z
                val dirX = direction.x
                val dirY = direction.y
                val dirZ = direction.z
                val casterRef = caster

                object : BukkitRunnable() {
                    var traveled = 0.0

                    override fun run() {
                        traveled += speed

                        // Calculate current position
                        val currentX = startX + dirX * traveled
                        val currentY = startY + dirY * traveled
                        val currentZ = startZ + dirZ * traveled
                        val currentLoc = Location(world, currentX, currentY, currentZ)

                        // Spawn fire trail particles
                        world.spawnParticle(Particle.FLAME, currentLoc, 8, 0.2, 0.2, 0.2, 0.03)
                        world.spawnParticle(Particle.SMOKE, currentLoc, 3, 0.1, 0.1, 0.1, 0.01)
                        world.spawnParticle(Particle.LAVA, currentLoc, 1, 0.1, 0.1, 0.1, 0.0)

                        // Check for entity collision
                        val nearbyEntities = world.getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)
                            .filterIsInstance<LivingEntity>()
                            .filter { it.uniqueId != casterRef.uniqueId }

                        if (nearbyEntities.isNotEmpty()) {
                            val hitTarget = nearbyEntities.first()
                            applyMagicDamage(casterRef, hitTarget, calculatedDamage)
                            hitTarget.fireTicks = 60  // 3 seconds fire
                            // Impact explosion effect
                            world.spawnParticle(Particle.FLAME, hitTarget.location.add(0.0, 1.0, 0.0), 40, 0.6, 0.6, 0.6, 0.1)
                            world.spawnParticle(Particle.LAVA, hitTarget.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.4, 0.4, 0.0)
                            world.playSound(hitTarget.location, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f)
                            cancel()
                            return
                        }

                        // Check for block collision
                        if (currentLoc.block.type.isSolid) {
                            // Hit a block - explosion effect
                            world.spawnParticle(Particle.FLAME, currentLoc, 30, 0.5, 0.5, 0.5, 0.1)
                            world.spawnParticle(Particle.LAVA, currentLoc, 15, 0.4, 0.4, 0.4, 0.0)
                            world.playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f)
                            cancel()
                            return
                        }

                        // Check max distance
                        if (traveled >= maxDistance) {
                            cancel()
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L)  // Start after 1 tick delay
            }

            SpellType.LIGHTNING -> {
                // Strike lightning at target location or entity
                val targetLoc = entityTarget?.location ?: locationTarget ?: return
                val world = targetLoc.world ?: return
                val damage = StatCalculator.calculateMagicDamage(spell.baseDamage, magerySkill, evalIntSkill)

                // Strike lightning effect
                world.strikeLightningEffect(targetLoc)

                // Damage nearby entities
                world.getNearbyEntities(targetLoc, 2.0, 2.0, 2.0).forEach { entity ->
                    if (entity is LivingEntity && entity != caster) {
                        applyMagicDamage(caster, entity, damage)
                        entity.fireTicks = 20  // Brief fire
                    }
                }
            }

            SpellType.FIRE_WALL -> {
                val targetLoc = locationTarget ?: return
                val world = targetLoc.world ?: return

                // Use player's yaw to calculate perpendicular direction for wall
                val yawRadians = Math.toRadians(caster.location.yaw.toDouble())
                // Wall is perpendicular to player's facing direction
                val perpX = kotlin.math.cos(yawRadians)
                val perpZ = kotlin.math.sin(yawRadians)

                val wallLength = 4  // 4 blocks wide
                val durationSeconds = 10 + (magerySkill / 10).toInt()  // 10-20 seconds
                val durationTicks = durationSeconds * 20
                // Fire Wall does damage per tick, so use lower per-tick damage
                val perTickDamage = StatCalculator.calculateMagicDamage(spell.baseDamage / 10.0, magerySkill, evalIntSkill)

                // Calculate wall positions (capture as primitive values)
                val wallPositions = mutableListOf<Triple<Double, Double, Double>>()

                for (i in -wallLength / 2..wallLength / 2) {
                    val x = targetLoc.x + perpX * i
                    val z = targetLoc.z + perpZ * i
                    // Use target's Y position (ground level where player clicked)
                    val y = targetLoc.y
                    wallPositions.add(Triple(x, y, z))
                }

                // Play initial sound
                world.playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f)
                world.playSound(targetLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f)

                // Capture references for runnable
                val casterRef = caster

                // Create fire wall effect task
                object : BukkitRunnable() {
                    var ticksRemaining = durationTicks

                    override fun run() {
                        if (ticksRemaining <= 0) {
                            // Final extinguish effect
                            wallPositions.forEach { (x, y, z) ->
                                world.spawnParticle(Particle.SMOKE, Location(world, x, y + 1.0, z), 10, 0.3, 0.5, 0.3, 0.02)
                            }
                            world.playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f)
                            cancel()
                            return
                        }

                        // Spawn fire particles at wall positions (tall flames)
                        wallPositions.forEach { (x, y, z) ->
                            // Ground level flames
                            world.spawnParticle(Particle.FLAME, Location(world, x, y + 0.5, z), 3, 0.1, 0.2, 0.1, 0.02)
                            // Mid-height flames
                            world.spawnParticle(Particle.FLAME, Location(world, x, y + 1.0, z), 4, 0.15, 0.3, 0.15, 0.03)
                            // Top flames
                            world.spawnParticle(Particle.FLAME, Location(world, x, y + 1.5, z), 2, 0.1, 0.2, 0.1, 0.02)
                            // Smoke rising
                            world.spawnParticle(Particle.SMOKE, Location(world, x, y + 2.0, z), 1, 0.1, 0.1, 0.1, 0.01)
                        }

                        // Play fire sound occasionally
                        if (ticksRemaining % 20 == 0) {
                            world.playSound(targetLoc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f)
                        }

                        // Apply damage every 10 ticks (0.5 seconds)
                        if (ticksRemaining % 10 == 0) {
                            wallPositions.forEach { (x, y, z) ->
                                val pos = Location(world, x, y + 1.0, z)
                                world.getNearbyEntities(pos, 1.0, 2.0, 1.0).forEach { entity ->
                                    if (entity is LivingEntity) {
                                        if (entity is Player) {
                                            applyMagicDamage(casterRef, entity, perTickDamage)
                                        } else {
                                            entity.damage(perTickDamage, casterRef)
                                        }
                                        entity.fireTicks = 60  // Set on fire
                                    }
                                }
                            }
                        }

                        ticksRemaining--
                    }
                }.runTaskTimer(plugin, 0L, 1L)
            }

            SpellType.MIND_BLAST -> {
                entityTarget?.let { target ->
                    if (target is Player) {
                        // Drain hunger (mana equivalent)
                        val currentFood = target.foodLevel
                        target.foodLevel = (currentFood - 3).coerceAtLeast(0)
                        plugin.skillManager.tryGainSkill(caster, SkillType.EVALUATING_INTELLIGENCE, 50)
                        // Visual: Mind effect
                        target.world.spawnParticle(Particle.ENCHANT, target.location.add(0.0, 2.0, 0.0), 30, 0.3, 0.3, 0.3, 0.5)
                        target.world.playSound(target.location, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.5f)
                    }
                }
            }

            SpellType.EXPLOSION -> {
                val targetLoc = locationTarget ?: entityTarget?.location ?: return
                val radius = 5.0
                targetLoc.world?.getNearbyEntities(targetLoc, radius, radius, radius)?.forEach { entity ->
                    if (entity is LivingEntity && entity != caster) {
                        val distance = entity.location.distance(targetLoc)
                        val falloff = 1.0 - (distance / radius)
                        val damage = StatCalculator.calculateMagicDamage(spell.baseDamage * falloff, magerySkill, evalIntSkill)
                        applyMagicDamage(caster, entity, damage)
                    }
                }
                // Visual effect
                targetLoc.world?.createExplosion(targetLoc, 0f, false, false)
            }

            SpellType.METEOR_SWARM -> {
                val targetLoc = locationTarget ?: entityTarget?.location ?: return
                val radius = 8.0
                targetLoc.world?.getNearbyEntities(targetLoc, radius, radius, radius)?.forEach { entity ->
                    if (entity is LivingEntity && entity != caster) {
                        val distance = entity.location.distance(targetLoc)
                        val falloff = 1.0 - (distance / radius)
                        val damage = StatCalculator.calculateMagicDamage(spell.baseDamage * falloff, magerySkill, evalIntSkill)
                        applyMagicDamage(caster, entity, damage)
                        entity.fireTicks = 100
                    }
                }
                // Visual effects
                targetLoc.world?.createExplosion(targetLoc, 0f, false, false)
            }

            // === Healing Spells (can target self or other players) ===
            SpellType.HEAL -> {
                val target = (entityTarget as? Player) ?: caster
                val healAmount = 2.0 + (magerySkill / 20.0)  // 2-7 HP
                plugin.combatManager.healPlayer(target, healAmount * 10)
                if (target == caster) {
                    plugin.messageSender.send(caster, MessageKey.HEAL_AMOUNT, "amount" to healAmount.toInt())
                } else {
                    plugin.messageSender.send(caster, MessageKey.HEAL_TARGET, "target" to target.name, "amount" to healAmount.toInt())
                    plugin.messageSender.send(target, MessageKey.HEAL_SELF, "caster" to caster.name, "amount" to healAmount.toInt())
                }
                // Visual: Green hearts on target
                target.world.spawnParticle(Particle.HEART, target.location.add(0.0, 1.5, 0.0), 8, 0.3, 0.3, 0.3, 0.0)
                target.world.spawnParticle(Particle.HAPPY_VILLAGER, target.location.add(0.0, 1.0, 0.0), 15, 0.4, 0.5, 0.4, 0.0)
                target.world.playSound(target.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f)
            }

            SpellType.CURE -> {
                val target = (entityTarget as? Player) ?: caster
                target.removePotionEffect(PotionEffectType.POISON)
                target.removePotionEffect(PotionEffectType.WITHER)
                target.removePotionEffect(PotionEffectType.WEAKNESS)
                target.removePotionEffect(PotionEffectType.SLOWNESS)
                // Visual: Cleansing effect on target
                target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.6, 0.4, 0.05)
                target.world.playSound(target.location, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f)
            }

            SpellType.GREATER_HEAL -> {
                val target = (entityTarget as? Player) ?: caster
                val healAmount = 6.0 + (magerySkill / 10.0)  // 6-16 HP
                plugin.combatManager.healPlayer(target, healAmount * 10)
                if (target == caster) {
                    plugin.messageSender.send(caster, MessageKey.HEAL_AMOUNT, "amount" to healAmount.toInt())
                } else {
                    plugin.messageSender.send(caster, MessageKey.HEAL_TARGET, "target" to target.name, "amount" to healAmount.toInt())
                    plugin.messageSender.send(target, MessageKey.HEAL_SELF, "caster" to caster.name, "amount" to healAmount.toInt())
                }
                // Visual: Intense healing on target
                target.world.spawnParticle(Particle.HEART, target.location.add(0.0, 1.5, 0.0), 15, 0.5, 0.5, 0.5, 0.0)
                target.world.spawnParticle(Particle.HAPPY_VILLAGER, target.location.add(0.0, 1.0, 0.0), 30, 0.5, 0.8, 0.5, 0.0)
                target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 0.5, 0.0), 20, 0.3, 1.0, 0.3, 0.02)
                target.world.playSound(target.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
            }

            // === Buff Spells (can target self or other players) ===
            SpellType.BLESS -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 1200 + (magerySkill * 12).toInt()  // 1-3 minutes
                target.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, duration, 0))
                // Visual: Golden blessing on target
                target.world.spawnParticle(Particle.ENCHANT, target.location.add(0.0, 1.5, 0.0), 40, 0.5, 0.8, 0.5, 0.5)
                target.world.spawnParticle(Particle.INSTANT_EFFECT, target.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.5, 0.3, 0.0)
                target.world.playSound(target.location, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f)
            }

            SpellType.PROTECTION -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 1200 + (magerySkill * 12).toInt()  // 1-3 minutes
                target.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, duration, 0))
                // Visual: Shield effect on target
                target.world.spawnParticle(Particle.ENCHANTED_HIT, target.location.add(0.0, 1.0, 0.0), 30, 0.5, 0.8, 0.5, 0.3)
                target.world.spawnParticle(Particle.SOUL_FIRE_FLAME, target.location.add(0.0, 0.5, 0.0), 15, 0.6, 0.1, 0.6, 0.0)
                target.world.playSound(target.location, Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 0.8f)
            }

            SpellType.INVISIBILITY -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 600 + (magerySkill * 6).toInt()  // 30s-1.5 minutes
                target.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, duration, 0))
                // Visual: Fade effect on target
                target.world.spawnParticle(Particle.SNEEZE, target.location.add(0.0, 1.0, 0.0), 20, 0.4, 0.6, 0.4, 0.02)
                target.world.playSound(target.location, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f)
            }

            // === Utility Spells ===
            SpellType.CREATE_FOOD -> {
                // Create cooked beef (steak) in player's inventory
                val steakAmount = 1 + (magerySkill / 25).toInt()  // 1-5 steaks based on skill
                val steak = org.bukkit.inventory.ItemStack(Material.COOKED_BEEF, steakAmount)
                val leftover = caster.inventory.addItem(steak)

                // If inventory full, drop on ground
                if (leftover.isNotEmpty()) {
                    leftover.values.forEach { item ->
                        caster.world.dropItemNaturally(caster.location, item)
                    }
                }

                // Visual: Food appearing effect
                caster.world.spawnParticle(Particle.HAPPY_VILLAGER, caster.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3, 0.0)
                caster.world.playSound(caster.location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
            }

            SpellType.NIGHT_SIGHT -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 6000 + (magerySkill * 60).toInt()  // 5-10 minutes
                target.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0))
                // Visual: Eye glow
                target.world.spawnParticle(Particle.END_ROD, target.eyeLocation, 10, 0.1, 0.1, 0.1, 0.02)
                target.world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f)
            }

            SpellType.FEATHER_FALL -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 1200 + (magerySkill * 12).toInt()  // 60-120 seconds
                target.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0))
                // Visual: Feather effect
                target.world.spawnParticle(Particle.CLOUD, target.location.add(0.0, 0.5, 0.0), 20, 0.4, 0.2, 0.4, 0.02)
                target.world.spawnParticle(Particle.END_ROD, target.location.add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.01)
                target.world.playSound(target.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f)
            }

            SpellType.WATER_BREATHING -> {
                val target = (entityTarget as? Player) ?: caster
                val duration = 3600 + (magerySkill * 36).toInt()  // 3-6 minutes
                target.addPotionEffect(PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0))
                // Visual: Bubble effect
                target.world.spawnParticle(Particle.BUBBLE_POP, target.location.add(0.0, 1.0, 0.0), 25, 0.3, 0.5, 0.3, 0.02)
                target.world.spawnParticle(Particle.DRIPPING_WATER, target.location.add(0.0, 1.5, 0.0), 15, 0.4, 0.3, 0.4, 0.0)
                target.world.playSound(target.location, Sound.ENTITY_DOLPHIN_SPLASH, 0.8f, 1.2f)
            }

            SpellType.TELEPORT -> {
                locationTarget?.let { loc ->
                    val maxRange = 15.0
                    if (loc.distance(caster.location) <= maxRange) {
                        // Visual: Departure effect
                        caster.world.spawnParticle(Particle.PORTAL, caster.location.add(0.0, 1.0, 0.0), 50, 0.3, 0.5, 0.3, 0.5)
                        caster.world.playSound(caster.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
                        // Find safe location (not inside blocks)
                        val safeLoc = loc.clone()
                        safeLoc.y = loc.world?.getHighestBlockYAt(loc)?.toDouble()?.plus(1) ?: loc.y
                        caster.teleport(safeLoc)
                        // Visual: Arrival effect
                        caster.world.spawnParticle(Particle.PORTAL, safeLoc.add(0.0, 1.0, 0.0), 50, 0.3, 0.5, 0.3, 0.5)
                        caster.world.playSound(safeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f)
                    }
                }
            }

            SpellType.MARK -> {
                // Mark the held rune with current location
                val rune = plugin.runeManager.getHeldRune(caster)
                if (rune != null) {
                    val loc = caster.location
                    val locationName = "${loc.world?.name ?: "World"} (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})"
                    plugin.runeManager.markRune(rune, loc, locationName)
                    // Visual: Rune marking
                    caster.world.spawnParticle(Particle.ENCHANT, loc.add(0.0, 0.5, 0.0), 40, 0.5, 0.2, 0.5, 0.3)
                    caster.world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
                }
            }

            SpellType.RECALL -> {
                // Check for runebook pending location first
                var targetLoc = plugin.runebookListener.getPendingRecallLocation(caster)

                // Fall back to held rune if no runebook location
                if (targetLoc == null) {
                    val rune = plugin.runeManager.getMarkedRune(caster)
                    if (rune != null) {
                        targetLoc = plugin.runeManager.getMarkedLocation(rune)
                    }
                }

                if (targetLoc != null) {
                    // Visual: Departure
                    caster.world.spawnParticle(Particle.REVERSE_PORTAL, caster.location.add(0.0, 1.0, 0.0), 60, 0.3, 0.8, 0.3, 0.1)
                    caster.world.playSound(caster.location, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f)
                    caster.teleport(targetLoc)
                    // Visual: Arrival
                    caster.world.spawnParticle(Particle.REVERSE_PORTAL, targetLoc.clone().add(0.0, 1.0, 0.0), 60, 0.3, 0.8, 0.3, 0.1)
                }
            }

            SpellType.PARALYZE -> {
                entityTarget?.let { target ->
                    if (target is LivingEntity) {
                        val duration = 40 + (magerySkill / 2).toInt()  // 2-4.5 seconds
                        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, duration, 100))
                        target.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128))
                        // Visual: Freeze effect
                        target.world.spawnParticle(Particle.SNOWFLAKE, target.location.add(0.0, 1.0, 0.0), 30, 0.4, 0.6, 0.4, 0.02)
                        target.world.spawnParticle(Particle.ELECTRIC_SPARK, target.location.add(0.0, 1.0, 0.0), 15, 0.3, 0.5, 0.3, 0.05)
                        target.world.playSound(target.location, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f)
                    }
                }
            }

            SpellType.GATE_TRAVEL -> {
                // Check for runebook pending location first
                var targetLoc = plugin.runebookListener.getPendingGateLocation(caster)

                // Fall back to held rune if no runebook location
                if (targetLoc == null) {
                    val rune = plugin.runeManager.getMarkedRune(caster)
                    if (rune != null) {
                        targetLoc = plugin.runeManager.getMarkedLocation(rune)
                    }
                }

                if (targetLoc != null) {
                    // Close any existing gate from this player first
                    plugin.gateManager.closeGatesForPlayer(caster.uniqueId)

                    // Gate duration is fixed at 30 seconds (UO-style)
                    val durationSeconds = 30
                    val durationTicks = durationSeconds * 20

                    // Spawn gate 2 blocks in front of caster so they don't immediately enter
                    val casterLoc = caster.location.clone()
                    val direction = casterLoc.direction.setY(0).normalize()
                    val gateLocation = casterLoc.add(direction.multiply(2.0))

                    // Create bidirectional gate
                    plugin.gateManager.createGate(
                        caster = caster,
                        locationA = gateLocation,
                        locationB = targetLoc.clone(),
                        durationTicks = durationTicks
                    )

                    plugin.messageSender.send(caster, MessageKey.GATE_CREATED,
                        "duration" to durationSeconds.toString())
                }
            }
        }
    }

    /**
     * Spawn particles in a line between two locations
     */
    private fun spawnParticleLine(from: Location, to: Location, particle: Particle, count: Int) {
        val world = from.world ?: return
        val direction = to.clone().subtract(from).toVector()
        val distance = direction.length()
        direction.normalize()

        val step = distance / count
        for (i in 0 until count) {
            val point = from.clone().add(direction.clone().multiply(step * i))
            world.spawnParticle(particle, point, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun applyMagicDamage(caster: Player, target: LivingEntity, damage: Double) {
        if (target is Player) {
            // Process through combat manager for player targets
            val defenseResult = plugin.combatManager.processPlayerDefense(
                target, caster, damage, isMagicDamage = true
            )
            plugin.combatManager.applyInternalDamage(target, defenseResult.damage)

            // Try eval int skill gain for caster
            plugin.skillManager.tryGainSkill(caster, SkillType.EVALUATING_INTELLIGENCE, 50)

            // Try resisting spells skill gain for target (when taking magic damage)
            plugin.skillManager.tryGainSkill(target, SkillType.RESISTING_SPELLS, 50)
        } else {
            // Direct damage to mobs
            target.damage(damage, caster)
        }
    }
}

enum class CastResult {
    SUCCESS,
    CASTING,
    NO_SPELLBOOK,
    NO_REAGENTS,
    NO_MANA,
    FAILED,
    INTERRUPTED
}
