package com.hacklab.minecraft.skills.vengeful

import com.hacklab.minecraft.skills.Skills
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent

/**
 * VengefulMobs機能のイベントリスナー
 */
class VengefulMobsListener(private val plugin: Skills) : Listener {

    private val manager: VengefulMobsManager
        get() = plugin.vengefulMobsManager

    /**
     * Mobがダメージを受けた時
     * プレイヤーからの攻撃なら反撃を開始
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val damager = event.damager

        // MobでないならスキップA
        if (entity !is Mob) return

        // 攻撃者がLivingEntityでないならスキップ
        val attacker = when (damager) {
            is Player -> damager
            is LivingEntity -> damager
            is org.bukkit.entity.Projectile -> {
                // 矢やトライデントの場合は射手を取得
                (damager.shooter as? LivingEntity)
            }
            else -> null
        } ?: return

        // 反撃を開始
        manager.onMobDamaged(entity, attacker)
    }

    /**
     * Mobがスポーンした時
     * HOSTILEモードのMobを設定
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        val entity = event.entity

        if (entity !is Mob) return

        // HOSTILEモードのMobを設定
        manager.onMobAdded(entity)
    }

    /**
     * Mobが死亡した時
     * クリーンアップ
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        if (entity is Mob) {
            manager.onMobRemoved(entity.uniqueId)
        }
    }
}
