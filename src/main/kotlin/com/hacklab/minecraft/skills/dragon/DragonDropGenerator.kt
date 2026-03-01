package com.hacklab.minecraft.skills.dragon

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.crafting.QualityType
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random

class DragonDropGenerator(private val plugin: Skills) {

    private val legendaryBlacksmiths = listOf("Zardoz", "Volund", "Mondain")
    private val crafterKey = NamespacedKey(plugin, "crafter")

    private val materialTable = listOf(
        70 to MaterialSet.IRON,
        25 to MaterialSet.DIAMOND,
        5 to MaterialSet.NETHERITE
    )

    private val qualityTable = listOf(
        94 to QualityType.NORMAL_QUALITY,
        5 to QualityType.HIGH_QUALITY,
        1 to QualityType.EXCEPTIONAL
    )

    private val scrollCircleTable = listOf(
        40 to SpellCircle.FOURTH,
        25 to SpellCircle.FIFTH,
        18 to SpellCircle.SIXTH,
        12 to SpellCircle.SEVENTH,
        5 to SpellCircle.EIGHTH
    )

    fun generateEquipmentDrop(): ItemStack {
        val materialSet = weightedRandom(materialTable)
        val equipType = EquipmentType.entries.random()
        val material = materialSet.getMaterial(equipType)
        val quality = weightedRandom(qualityTable)

        val item = ItemStack(material)
        plugin.qualityManager.setQuality(item, quality)

        if (quality == QualityType.EXCEPTIONAL) {
            val blacksmith = legendaryBlacksmiths.random()
            val meta = item.itemMeta ?: return item
            meta.persistentDataContainer.set(crafterKey, PersistentDataType.STRING, blacksmith)

            val lore = meta.lore()?.toMutableList() ?: mutableListOf()
            lore.add(
                Component.text("Crafter: $blacksmith")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(lore)
            item.itemMeta = meta
        }

        return item
    }

    fun generateScrollDrop(): ItemStack {
        val circle = weightedRandom(scrollCircleTable)
        val spellsInCircle = SpellType.entries.filter { it.circle == circle }
        val spell = spellsInCircle.random()
        return plugin.scrollManager.createScroll(spell)
    }

    fun getScaledXp(killCount: Int): Int {
        val xpScale = plugin.skillsConfig.enderDragonXpScalePerKill
        return (12000 * (1 + killCount * xpScale)).toInt()
    }

    private fun <T> weightedRandom(table: List<Pair<Int, T>>): T {
        val totalWeight = table.sumOf { it.first }
        var roll = Random.nextInt(totalWeight)
        for ((weight, value) in table) {
            roll -= weight
            if (roll < 0) return value
        }
        return table.last().second
    }

    private enum class MaterialSet {
        IRON, DIAMOND, NETHERITE;

        fun getMaterial(type: EquipmentType): Material = when (this) {
            IRON -> type.iron
            DIAMOND -> type.diamond
            NETHERITE -> type.netherite
        }
    }

    private enum class EquipmentType(val iron: Material, val diamond: Material, val netherite: Material) {
        SWORD(Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD),
        AXE(Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE),
        HELMET(Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET),
        CHESTPLATE(Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE),
        LEGGINGS(Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS),
        BOOTS(Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS)
    }
}
