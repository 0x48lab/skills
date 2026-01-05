package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.Language
import com.hacklab.minecraft.skills.magic.CastResult
import com.hacklab.minecraft.skills.magic.RunebookManager
import com.hacklab.minecraft.skills.magic.SpellType
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Runebook = チェストのようなシンプルなインベントリ
 * 違いは右クリック/Shift+右クリックでRecall/Gate Travelが発動すること
 */
class RunebookListener(private val plugin: Skills) : Listener {

    // 開いているルーンブック (プレイヤーUUID -> ルーンブックItemStack)
    private val openRunebooks = ConcurrentHashMap<UUID, ItemStack>()

    // 魔法発動待ち
    private val pendingRecall = ConcurrentHashMap<UUID, Location>()
    private val pendingGate = ConcurrentHashMap<UUID, Location>()

    /**
     * ルーンブックを右クリックで開く
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (!plugin.runebookManager.isRunebook(item)) return

        event.isCancelled = true

        val player = event.player
        plugin.runebookManager.openGUI(player, item)
        openRunebooks[player.uniqueId] = item
        player.world.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.0f)
    }

    /**
     * クリック処理 - 右クリック系のみ特別処理、それ以外は通常のチェスト動作
     * ただし、登録済みルーン以外のアイテムは入れられない
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val runebook = openRunebooks[player.uniqueId] ?: return

        // ルーンブックGUIかどうか確認
        if (!isRunebookGUI(event.view.title())) return

        // ルーンブックに入れようとしているアイテムをチェック
        // 1. カーソルにアイテムを持ってルーンブック内をクリック
        if (event.rawSlot in 0 until RunebookManager.GUI_SIZE) {
            val cursorItem = event.cursor
            if (!cursorItem.type.isAir) {
                // カーソルのアイテムが登録済みルーンでなければキャンセル
                if (!plugin.runebookManager.isValidMarkedRune(cursorItem)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // 2. Shift+クリックでプレイヤーインベントリからルーンブックへ
        if (event.click.isShiftClick && event.rawSlot >= RunebookManager.GUI_SIZE) {
            val clickedItem = event.currentItem
            if (clickedItem != null && !clickedItem.type.isAir) {
                // クリックしたアイテムが登録済みルーンでなければキャンセル
                if (!plugin.runebookManager.isValidMarkedRune(clickedItem)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // 上部インベントリ（ルーンブック）内のクリックのみ魔法発動処理
        if (event.rawSlot !in 0 until RunebookManager.GUI_SIZE) return

        val clickedItem = event.currentItem ?: return
        val runeEntry = plugin.runebookManager.extractRuneEntry(clickedItem) ?: return

        // 右クリック系のみ特別処理
        when {
            event.click == ClickType.SHIFT_RIGHT -> {
                event.isCancelled = true
                player.closeInventory()
                castGateTravel(player, runeEntry)
            }
            event.click.isRightClick -> {
                event.isCancelled = true
                player.closeInventory()
                castRecall(player, runeEntry)
            }
            // それ以外（左クリック等）は通常のチェスト動作 - 何もしない
        }
    }

    /**
     * インベントリを閉じた時にルーンブックのデータを保存
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val runebook = openRunebooks.remove(player.uniqueId) ?: return

        // インベントリの内容をルーンブックに保存
        val useJapanese = plugin.localeManager.getLanguage(player) == Language.JAPANESE
        plugin.runebookManager.saveFromInventory(runebook, event.inventory, useJapanese)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        openRunebooks.remove(uuid)
        pendingRecall.remove(uuid)
        pendingGate.remove(uuid)
    }

    // ==================== 魔法発動 ====================

    private fun castRecall(player: Player, entry: RunebookManager.RuneEntry) {
        val location = plugin.runebookManager.getLocation(entry)
        if (location == null) {
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.RUNEBOOK_WORLD_NOT_FOUND)
            return
        }

        pendingRecall[player.uniqueId] = location
        val result = plugin.spellManager.castSpell(player, SpellType.RECALL, false)
        if (result != CastResult.CASTING) {
            pendingRecall.remove(player.uniqueId)
        }
    }

    private fun castGateTravel(player: Player, entry: RunebookManager.RuneEntry) {
        val location = plugin.runebookManager.getLocation(entry)
        if (location == null) {
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.RUNEBOOK_WORLD_NOT_FOUND)
            return
        }

        pendingGate[player.uniqueId] = location
        val result = plugin.spellManager.castSpell(player, SpellType.GATE_TRAVEL, false)
        if (result != CastResult.CASTING) {
            pendingGate.remove(player.uniqueId)
        }
    }

    // ==================== Public API ====================

    fun getPendingRecallLocation(player: Player): Location? {
        return pendingRecall.remove(player.uniqueId)
    }

    fun getPendingGateLocation(player: Player): Location? {
        return pendingGate.remove(player.uniqueId)
    }

    fun hasPendingRunebookSpell(player: Player): Boolean {
        return pendingRecall.containsKey(player.uniqueId) || pendingGate.containsKey(player.uniqueId)
    }

    fun clearPending(player: Player) {
        pendingRecall.remove(player.uniqueId)
        pendingGate.remove(player.uniqueId)
    }

    private fun isRunebookGUI(title: Component): Boolean {
        val titleStr = title.toString()
        return titleStr.contains("Runebook") || titleStr.contains("ルーンの書")
    }
}
