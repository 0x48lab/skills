package com.hacklab.minecraft.skills.armor

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Manages armor equipment restrictions and effects
 */
class ArmorManager(private val plugin: Skills) {

    /**
     * Check if player can equip an armor piece
     * @return true if can equip, false otherwise
     */
    fun canEquip(player: Player, item: ItemStack?): Boolean {
        return plugin.armorConfig.canEquip(player, item)
    }

    /**
     * Try to equip armor, sending message if failed
     * @return true if can equip, false if blocked
     */
    fun tryEquip(player: Player, item: ItemStack?): Boolean {
        if (item == null) return true

        val playerData = plugin.playerDataManager.getPlayerData(player)

        // Check STR requirement
        val strDeficit = plugin.armorConfig.getStrDeficit(player, item)
        if (strDeficit > 0) {
            val required = plugin.armorConfig.getStrRequired(item)

            plugin.messageSender.send(
                player,
                MessageKey.ARMOR_CANNOT_EQUIP_STR,
                "required" to required,
                "current" to playerData.str
            )
            return false
        }

        // Check DEX requirement (UO-style heavy armor restriction)
        val dexDeficit = plugin.armorConfig.getDexDeficit(player, item)
        if (dexDeficit > 0) {
            val required = plugin.armorConfig.getDexRequired(item)

            plugin.messageSender.send(
                player,
                MessageKey.ARMOR_CANNOT_EQUIP_DEX,
                "required" to required,
                "current" to playerData.dex
            )
            return false
        }

        // Show DEX penalty warning if significant
        val dexPenalty = plugin.armorConfig.getDexPenalty(item)
        if (dexPenalty >= 5) {
            plugin.messageSender.send(
                player,
                MessageKey.ARMOR_DEX_PENALTY_WARNING,
                "penalty" to dexPenalty
            )
        }

        return true
    }

    /**
     * Check player's equipment and remove any items they can no longer wear
     * Called when STR or DEX decreases
     */
    fun validateEquipment(player: Player) {
        val unequippable = plugin.armorConfig.getUnequippableArmor(player)
        val playerData = plugin.playerDataManager.getPlayerData(player)

        for (item in unequippable) {
            removeArmorPiece(player, item)

            // Determine which stat caused the removal
            val strDeficit = plugin.armorConfig.getStrDeficit(player, item)
            val dexDeficit = plugin.armorConfig.getDexDeficit(player, item)

            when {
                strDeficit > 0 -> {
                    plugin.messageSender.send(
                        player,
                        MessageKey.ARMOR_REMOVED_STR,
                        "item" to getArmorDisplayName(item)
                    )
                }
                dexDeficit > 0 -> {
                    plugin.messageSender.send(
                        player,
                        MessageKey.ARMOR_REMOVED_DEX,
                        "item" to getArmorDisplayName(item)
                    )
                }
            }
        }
    }

    /**
     * Remove an armor piece from player and drop it or add to inventory
     */
    private fun removeArmorPiece(player: Player, item: ItemStack) {
        val equipment = player.inventory

        // Clone the item BEFORE removing from slot to ensure we have a valid copy
        // This is important because setting slot to null may invalidate the original reference
        val itemToGive = item.clone()

        val removed = when (item.type) {
            equipment.helmet?.type -> {
                equipment.helmet = null
                true
            }
            equipment.chestplate?.type -> {
                equipment.chestplate = null
                true
            }
            equipment.leggings?.type -> {
                equipment.leggings = null
                true
            }
            equipment.boots?.type -> {
                equipment.boots = null
                true
            }
            else -> false
        }

        if (removed) {
            giveOrDropItem(player, itemToGive)
        }
    }

    /**
     * Give item to player or drop if inventory full
     */
    private fun giveOrDropItem(player: Player, item: ItemStack) {
        val leftover = player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            // Drop at player's feet if inventory is full
            player.world.dropItemNaturally(player.location, item)
        }
    }

    /**
     * Get display name for armor item
     * Uses PlainTextComponentSerializer to avoid legacy formatting codes in MiniMessage
     */
    private fun getArmorDisplayName(item: ItemStack): String {
        return if (item.hasItemMeta() && item.itemMeta?.hasDisplayName() == true) {
            item.itemMeta?.displayName()?.let { component ->
                PlainTextComponentSerializer.plainText().serialize(component)
            } ?: item.type.name
        } else {
            // Convert DIAMOND_CHESTPLATE to Diamond Chestplate
            item.type.name.lowercase().split("_").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
        }
    }

    /**
     * Get player's total AR
     */
    fun getTotalAR(player: Player): Double {
        return plugin.armorConfig.calculateTotalAR(player)
    }

    /**
     * Get player's total DEX penalty from armor
     */
    fun getTotalDexPenalty(player: Player): Int {
        return plugin.armorConfig.calculateTotalDexPenalty(player)
    }

    /**
     * Get effective DEX after armor penalty
     */
    fun getEffectiveDex(player: Player): Int {
        val playerData = plugin.playerDataManager.getPlayerData(player)
        val baseDex = playerData.dex
        val penalty = getTotalDexPenalty(player)
        return (baseDex - penalty).coerceAtLeast(0)
    }

    /**
     * Get shield block bonus if shield is equipped
     */
    fun getShieldBlockBonus(player: Player): Int {
        return plugin.armorConfig.getShieldBlockBonus(player)
    }
}
