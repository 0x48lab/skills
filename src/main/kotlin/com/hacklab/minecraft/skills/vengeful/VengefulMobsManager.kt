package com.hacklab.minecraft.skills.vengeful

import com.hacklab.minecraft.skills.Skills
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * VengefulMobs機能を管理
 * 受動的Mobに反撃能力を付与
 */
class VengefulMobsManager(private val plugin: Skills) {

    val config = VengefulMobsConfig(plugin)

    // 怒り状態のMob: MobのUUID → (攻撃者UUID, 怒り終了時刻)
    private val angryMobs = ConcurrentHashMap<UUID, AngryState>()

    // RETALIATE_ONCEモード用: 1回反撃したMobを追跡
    private val retaliatedOnce = ConcurrentHashMap.newKeySet<UUID>()

    // タスク実行中かどうか
    private var taskRunning = false

    data class AngryState(
        val targetId: UUID,
        val expireTime: Long,
        val mode: AggressionMode,
        val damage: Double
    )

    /**
     * 設定を再読み込み
     */
    fun reload() {
        config.reload()
        angryMobs.clear()
        retaliatedOnce.clear()
    }

    /**
     * 攻撃追跡タスクを開始
     */
    fun startAggressionTask() {
        if (taskRunning) return
        taskRunning = true

        object : BukkitRunnable() {
            override fun run() {
                if (!config.enabled) return
                processAngryMobs()
            }
        }.runTaskTimer(plugin, 1L, 1L) // 毎tick実行
    }

    /**
     * 怒り状態のMobを処理
     */
    private fun processAngryMobs() {
        val now = System.currentTimeMillis()

        val iterator = angryMobs.entries.iterator()
        while (iterator.hasNext()) {
            val (mobId, state) = iterator.next()

            // 期限切れチェック
            if (now > state.expireTime) {
                iterator.remove()
                continue
            }

            val mob = plugin.server.getEntity(mobId) as? Mob
            if (mob == null || mob.isDead) {
                iterator.remove()
                continue
            }

            val target = plugin.server.getEntity(state.targetId) as? LivingEntity
            if (target == null || target.isDead) {
                iterator.remove()
                continue
            }

            // 距離チェック（give_up_distance以内）
            val distance = mob.location.distance(target.location)
            if (!mob.world.equals(target.world) || distance > config.giveUpDistance) {
                iterator.remove()
                continue
            }

            // 追跡中なら怒り状態を延長（逃げるまで追い続ける）
            if (config.extendAngerWhileChasing && distance <= config.giveUpDistance) {
                val newExpireTime = now + config.angerDuration
                if (newExpireTime > state.expireTime) {
                    angryMobs[mobId] = state.copy(expireTime = newExpireTime)
                }
            }

            // ターゲットに向かって移動
            moveTowardTarget(mob, target)

            // 近ければ攻撃（攻撃範囲内）
            val attackRange = config.getConfig(mob.type).attackRange
            if (mob.location.distance(target.location) <= attackRange) {
                attackTarget(mob, target, state)
            }
        }
    }

    /**
     * ターゲットに向かって移動
     */
    private fun moveTowardTarget(mob: Mob, target: LivingEntity) {
        val mobLoc = mob.location
        val targetLoc = target.location

        // Creatureの場合はPathfinderを使用
        if (mob is Creature) {
            mob.target = target
            // パスファインダーで移動
            mob.pathfinder.moveTo(targetLoc)
        } else {
            // Creatureでない場合は速度ベクトルで移動
            val direction = targetLoc.toVector().subtract(mobLoc.toVector()).normalize()
            val speed = config.getConfig(mob.type).speed * 0.2
            mob.velocity = direction.multiply(speed)

            // 向きを変更
            val yaw = Math.toDegrees(
                Math.atan2(-(targetLoc.x - mobLoc.x), targetLoc.z - mobLoc.z)
            ).toFloat()
            mobLoc.yaw = yaw
            mob.teleport(mobLoc)
        }
    }

    /**
     * ターゲットを攻撃
     */
    private fun attackTarget(mob: Mob, target: LivingEntity, state: AngryState) {
        // RETALIATE_ONCEの場合、1回だけ攻撃
        if (state.mode == AggressionMode.RETALIATE_ONCE) {
            if (mob.uniqueId in retaliatedOnce) {
                angryMobs.remove(mob.uniqueId)
                return
            }
            retaliatedOnce.add(mob.uniqueId)
        }

        // 攻撃クールダウン（0.5秒ごと）
        val lastAttackKey = "vengeful_last_attack"
        val lastAttack = mob.getPersistentDataContainer().getOrDefault(
            org.bukkit.NamespacedKey(plugin, lastAttackKey),
            org.bukkit.persistence.PersistentDataType.LONG,
            0L
        )

        val now = System.currentTimeMillis()
        if (now - lastAttack < 500) return

        mob.getPersistentDataContainer().set(
            org.bukkit.NamespacedKey(plugin, lastAttackKey),
            org.bukkit.persistence.PersistentDataType.LONG,
            now
        )

        // ダメージを与える
        target.damage(state.damage, mob)

        // 攻撃アニメーション（腕を振る）
        mob.swingMainHand()

        if (plugin.skillsConfig.debugMode) {
            plugin.logger.info("VengefulMobs: ${mob.type} attacked ${target.name} for ${state.damage} damage")
        }
    }

    /**
     * MobがダメージをReceiveした時に呼び出し
     * 反撃を開始する
     */
    fun onMobDamaged(mob: Mob, attacker: LivingEntity) {
        if (!config.enabled) return
        if (!config.isEnabled(mob.type)) return

        // テイム済みを除外
        if (config.excludeTamed && mob is Tameable && mob.isTamed) return

        val mobConfig = config.getConfig(mob.type)

        // RETALIATE_ONCEで既に反撃済みの場合はスキップ
        if (mobConfig.mode == AggressionMode.RETALIATE_ONCE && mob.uniqueId in retaliatedOnce) {
            return
        }

        // 怒り状態に設定（設定された持続時間）
        val expireTime = System.currentTimeMillis() + config.angerDuration
        angryMobs[mob.uniqueId] = AngryState(
            targetId = attacker.uniqueId,
            expireTime = expireTime,
            mode = mobConfig.mode,
            damage = mobConfig.damage
        )

        // RETALIATE_WITH_SUPPORTの場合、周囲のMobも怒らせる
        if (mobConfig.mode == AggressionMode.RETALIATE_WITH_SUPPORT) {
            alertNearbyMobs(mob, attacker)
        }

        if (plugin.skillsConfig.debugMode) {
            plugin.logger.info("VengefulMobs: ${mob.type} is now angry at ${attacker.name}")
        }
    }

    /**
     * HOSTILEモードのMobを設定（ワールドに追加時）
     */
    fun onMobAdded(mob: Mob) {
        if (!config.enabled) return
        if (!config.isEnabled(mob.type)) return

        // テイム済みを除外
        if (config.excludeTamed && mob is Tameable && mob.isTamed) return

        val mobConfig = config.getConfig(mob.type)

        // HOSTILEまたはMURDERモードの場合のみ
        when (mobConfig.mode) {
            AggressionMode.HOSTILE, AggressionMode.MURDER_ALL, AggressionMode.MURDER_OTHERS -> {
                // 攻撃力を設定
                mob.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = mobConfig.damage

                // 近くのプレイヤーを自動的にターゲット
                findAndTargetNearestPlayer(mob, mobConfig)
            }
            else -> {
                // RETALIATEモードは攻撃されてから反応
            }
        }
    }

    /**
     * 最も近いプレイヤーを探してターゲット
     */
    private fun findAndTargetNearestPlayer(mob: Mob, mobConfig: MobConfig) {
        val nearestPlayer = mob.getNearbyEntities(16.0, 16.0, 16.0)
            .filterIsInstance<Player>()
            .filter { !it.isDead && it.gameMode == org.bukkit.GameMode.SURVIVAL }
            .minByOrNull { it.location.distance(mob.location) }

        if (nearestPlayer != null) {
            val expireTime = System.currentTimeMillis() + 30_000 // 30秒間
            angryMobs[mob.uniqueId] = AngryState(
                targetId = nearestPlayer.uniqueId,
                expireTime = expireTime,
                mode = mobConfig.mode,
                damage = mobConfig.damage
            )
        }
    }

    /**
     * 周囲のMobに攻撃者を通知（RETALIATE_WITH_SUPPORT用）
     */
    private fun alertNearbyMobs(attacked: Mob, attacker: LivingEntity, radius: Double = 10.0) {
        val nearbyMobs = attacked.getNearbyEntities(radius, radius, radius)
            .filterIsInstance<Mob>()
            .filter { it.type == attacked.type }
            .filter { config.isEnabled(it.type) }
            .filter { !(it is Tameable && it.isTamed) }
            .filter { !angryMobs.containsKey(it.uniqueId) }

        val mobConfig = config.getConfig(attacked.type)
        val expireTime = System.currentTimeMillis() + config.angerDuration

        for (mob in nearbyMobs) {
            angryMobs[mob.uniqueId] = AngryState(
                targetId = attacker.uniqueId,
                expireTime = expireTime,
                mode = mobConfig.mode,
                damage = mobConfig.damage
            )
        }

        if (nearbyMobs.isNotEmpty() && plugin.skillsConfig.debugMode) {
            plugin.logger.info("VengefulMobs: Alerted ${nearbyMobs.size} nearby ${attacked.type}s")
        }
    }

    /**
     * Mobが削除された時のクリーンアップ
     */
    fun onMobRemoved(mobId: UUID) {
        angryMobs.remove(mobId)
        retaliatedOnce.remove(mobId)
    }

    /**
     * 怒り状態のMob数を取得（デバッグ用）
     */
    fun getAngryMobCount(): Int = angryMobs.size
}
