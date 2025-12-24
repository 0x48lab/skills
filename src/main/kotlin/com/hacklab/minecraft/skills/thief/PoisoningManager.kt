package com.hacklab.minecraft.skills.thief

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.util.WeaponUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

class PoisoningManager(private val plugin: Skills) {
    private val poisonChargesKey = NamespacedKey(plugin, "poison_charges")

    /**
     * Apply poison to a held weapon
     */
    fun applyPoison(player: Player): Boolean {
        val weapon = player.inventory.itemInMainHand

        // Must be a weapon
        if (!WeaponUtil.isWeapon(weapon.type)) {
            return false
        }

        // Must have poison
        if (!player.inventory.contains(Material.POTION)) {
            // Check for poison potions specifically
            val hasPoisonPotion = player.inventory.contents.any { item ->
                item?.type == Material.POTION &&
                        item.itemMeta?.let { meta ->
                            (meta as? org.bukkit.inventory.meta.PotionMeta)
                                ?.basePotionType?.name?.contains("POISON") == true
                        } ?: false
            }
            if (!hasPoisonPotion) return false
        }

        val data = plugin.playerDataManager.getPlayerData(player)
        val poisoningSkill = data.getSkillValue(SkillType.POISONING)

        // Calculate charges: skill / 20 (max 5)
        val charges = (poisoningSkill / 20.0).toInt().coerceIn(1, 5)

        // Success chance based on skill
        val successChance = poisoningSkill.coerceIn(20.0, 95.0)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.POISONING, 50)

        if (Random.nextDouble() * 100 > successChance) {
            plugin.messageSender.send(player, MessageKey.THIEF_POISON_FAILED)
            // Still consume potion
            consumePoisonPotion(player)
            return false
        }

        // Apply poison to weapon
        setPoisonCharges(weapon, charges)

        // Consume potion
        consumePoisonPotion(player)

        // Update weapon lore
        updatePoisonLore(weapon, charges)

        plugin.messageSender.send(player, MessageKey.THIEF_POISON_APPLIED, "charges" to charges)
        return true
    }

    /**
     * Process a poison hit
     */
    fun processPoisonHit(attacker: Player, target: LivingEntity, weapon: ItemStack): Boolean {
        val charges = getPoisonCharges(weapon)
        if (charges <= 0) return false

        // Apply poison effect
        val data = plugin.playerDataManager.getPlayerData(attacker)
        val poisoningSkill = data.getSkillValue(SkillType.POISONING)

        // Duration based on skill: 2-10 seconds
        val duration = (40 + (poisoningSkill * 1.6)).toInt()  // In ticks

        target.addPotionEffect(
            PotionEffect(
                PotionEffectType.POISON,
                duration,
                1  // Level 2 poison
            )
        )

        // Reduce charges
        val newCharges = charges - 1
        if (newCharges <= 0) {
            removePoisonCharges(weapon)
        } else {
            setPoisonCharges(weapon, newCharges)
            updatePoisonLore(weapon, newCharges)
        }

        // Try skill gain
        plugin.skillManager.tryGainSkill(attacker, SkillType.POISONING, 50)

        plugin.messageSender.sendActionBar(attacker, MessageKey.THIEF_POISON_HIT)
        return true
    }

    /**
     * Get poison charges on a weapon
     */
    fun getPoisonCharges(weapon: ItemStack?): Int {
        if (weapon == null) return 0
        return weapon.itemMeta?.persistentDataContainer
            ?.get(poisonChargesKey, PersistentDataType.INTEGER) ?: 0
    }

    /**
     * Check if weapon is poisoned
     */
    fun isPoisoned(weapon: ItemStack?): Boolean {
        return getPoisonCharges(weapon) > 0
    }

    private fun setPoisonCharges(weapon: ItemStack, charges: Int) {
        val meta = weapon.itemMeta ?: return
        meta.persistentDataContainer.set(poisonChargesKey, PersistentDataType.INTEGER, charges)
        weapon.itemMeta = meta
    }

    private fun removePoisonCharges(weapon: ItemStack) {
        val meta = weapon.itemMeta ?: return
        meta.persistentDataContainer.remove(poisonChargesKey)

        // Remove poison lore
        val lore = meta.lore()?.toMutableList() ?: return
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component)
            plain.contains("Poison")
        }
        meta.lore(lore)
        weapon.itemMeta = meta
    }

    private fun updatePoisonLore(weapon: ItemStack, charges: Int) {
        val meta = weapon.itemMeta ?: return
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        // Remove existing poison lore
        lore.removeIf { component ->
            val plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component)
            plain.contains("Poison")
        }

        // Add new poison lore
        lore.add(Component.text("Poisoned ($charges charges)").color(NamedTextColor.DARK_GREEN))

        meta.lore(lore)
        weapon.itemMeta = meta
    }

    private fun consumePoisonPotion(player: Player) {
        for (i in 0 until player.inventory.size) {
            val item = player.inventory.getItem(i) ?: continue
            if (item.type != Material.POTION) continue

            val potionMeta = item.itemMeta as? org.bukkit.inventory.meta.PotionMeta ?: continue
            if (potionMeta.basePotionType?.name?.contains("POISON") == true) {
                if (item.amount > 1) {
                    item.amount -= 1
                } else {
                    player.inventory.setItem(i, ItemStack(Material.GLASS_BOTTLE))
                }
                return
            }
        }
    }
}
