package com.hacklab.minecraft.skills.util

import com.hacklab.minecraft.skills.skill.SkillType
import com.hacklab.minecraft.skills.skill.WeaponType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object WeaponUtil {

    fun getWeaponType(item: ItemStack?): WeaponType {
        if (item == null || item.type == Material.AIR) {
            return WeaponType.FIST
        }

        return when (item.type) {
            // Swords
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD -> WeaponType.SWORD

            // Axes
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE -> WeaponType.AXE

            // Mace
            Material.MACE -> WeaponType.MACE

            // Spears
            Material.WOODEN_SPEAR,
            Material.STONE_SPEAR,
            Material.COPPER_SPEAR,
            Material.IRON_SPEAR,
            Material.GOLDEN_SPEAR,
            Material.DIAMOND_SPEAR,
            Material.NETHERITE_SPEAR -> WeaponType.SPEAR

            // Bows
            Material.BOW -> WeaponType.BOW
            Material.CROSSBOW -> WeaponType.CROSSBOW

            // Trident
            Material.TRIDENT -> WeaponType.TRIDENT

            else -> WeaponType.FIST
        }
    }

    fun getWeaponSkillType(item: ItemStack?): SkillType {
        return when (getWeaponType(item)) {
            WeaponType.SWORD -> SkillType.SWORDSMANSHIP
            WeaponType.AXE -> SkillType.AXE
            WeaponType.MACE -> SkillType.MACE_FIGHTING
            WeaponType.SPEAR -> SkillType.SPEAR
            WeaponType.BOW, WeaponType.CROSSBOW -> SkillType.ARCHERY
            WeaponType.TRIDENT -> SkillType.THROWING
            WeaponType.FIST -> SkillType.WRESTLING
        }
    }

    fun isWeapon(material: Material): Boolean {
        return material.name.endsWith("_SWORD") ||
                material.name.endsWith("_AXE") ||
                material.name.endsWith("_SPEAR") ||
                material == Material.MACE ||
                material == Material.BOW ||
                material == Material.CROSSBOW ||
                material == Material.TRIDENT
    }

    fun isArmor(material: Material): Boolean {
        return material.name.endsWith("_HELMET") ||
                material.name.endsWith("_CHESTPLATE") ||
                material.name.endsWith("_LEGGINGS") ||
                material.name.endsWith("_BOOTS") ||
                material == Material.SHIELD
    }

    fun isTool(material: Material): Boolean {
        return material.name.endsWith("_PICKAXE") ||
                material.name.endsWith("_SHOVEL") ||
                material.name.endsWith("_HOE") ||
                material == Material.SHEARS ||
                material == Material.FISHING_ROD ||
                material == Material.FLINT_AND_STEEL
    }
}
