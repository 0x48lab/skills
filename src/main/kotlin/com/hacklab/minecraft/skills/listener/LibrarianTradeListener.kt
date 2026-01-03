package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.magic.SpellCircle
import com.hacklab.minecraft.skills.magic.SpellType
import org.bukkit.Material
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.VillagerAcquireTradeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import kotlin.random.Random

/**
 * Adds magic-related trades to Librarian villagers.
 * - Level 1-2: C1/C2 scrolls
 * - Level 3: Empty spellbook
 * - Level 4: Additional C2 scrolls
 */
class LibrarianTradeListener(private val plugin: Skills) : Listener {

    // C1 spells for scroll trades
    private val circle1Spells = SpellType.getByCircle(SpellCircle.FIRST)

    // C2 spells for scroll trades
    private val circle2Spells = SpellType.getByCircle(SpellCircle.SECOND)

    @EventHandler
    fun onVillagerAcquireTrade(event: VillagerAcquireTradeEvent) {
        val villager = event.entity as? Villager ?: return
        if (villager.profession != Villager.Profession.LIBRARIAN) return

        // Add custom trades based on villager level
        val level = villager.villagerLevel

        // We add trades alongside vanilla trades, not replace them
        // This event fires for each trade acquired, so we check if it's a good time to add ours

        when (level) {
            1 -> maybeAddCircle1ScrollTrade(villager)
            2 -> maybeAddCircle1Or2ScrollTrade(villager)
            3 -> {
                maybeAddSpellbookTrade(villager)
                maybeAddRunebookTrade(villager)
            }
            4 -> maybeAddCircle2ScrollTrade(villager)
        }
    }

    /**
     * Add a C1 scroll trade (level 1 librarian)
     * Price: 5-8 emeralds
     */
    private fun maybeAddCircle1ScrollTrade(villager: Villager) {
        if (hasScrollTrade(villager)) return

        val spell = circle1Spells.randomOrNull() ?: return
        val scroll = plugin.scrollManager.createScroll(spell)
        val price = Random.nextInt(5, 9) // 5-8 emeralds

        addTrade(villager, scroll, price, 3, 2) // maxUses=3, villagerXp=2
    }

    /**
     * Add a C1 or C2 scroll trade (level 2 librarian)
     * Price: 8-12 emeralds
     */
    private fun maybeAddCircle1Or2ScrollTrade(villager: Villager) {
        if (countScrollTrades(villager) >= 2) return

        // 50% chance C1, 50% chance C2
        val spells = if (Random.nextBoolean()) circle1Spells else circle2Spells
        val spell = spells.randomOrNull() ?: return
        val scroll = plugin.scrollManager.createScroll(spell)
        val price = Random.nextInt(8, 13) // 8-12 emeralds

        addTrade(villager, scroll, price, 3, 5) // maxUses=3, villagerXp=5
    }

    /**
     * Add an empty spellbook trade (level 3 librarian)
     * Price: 30-50 emeralds
     */
    private fun maybeAddSpellbookTrade(villager: Villager) {
        if (hasSpellbookTrade(villager)) return

        val spellbook = plugin.spellbookManager.createSpellbook()
        val price = Random.nextInt(30, 51) // 30-50 emeralds

        addTrade(villager, spellbook, price, 1, 10) // maxUses=1, villagerXp=10
    }

    /**
     * Add an empty runebook trade (level 3 librarian)
     * Price: 40-60 emeralds
     */
    private fun maybeAddRunebookTrade(villager: Villager) {
        if (hasRunebookTrade(villager)) return

        val runebook = plugin.runebookManager.createRunebook(false)
        val price = Random.nextInt(40, 61) // 40-60 emeralds

        addTrade(villager, runebook, price, 1, 10) // maxUses=1, villagerXp=10
    }

    /**
     * Add a C2 scroll trade (level 4 librarian)
     * Price: 12-15 emeralds
     */
    private fun maybeAddCircle2ScrollTrade(villager: Villager) {
        if (countScrollTrades(villager) >= 3) return

        val spell = circle2Spells.randomOrNull() ?: return
        val scroll = plugin.scrollManager.createScroll(spell)
        val price = Random.nextInt(12, 16) // 12-15 emeralds

        addTrade(villager, scroll, price, 3, 10) // maxUses=3, villagerXp=10
    }

    /**
     * Add a trade to the villager
     */
    private fun addTrade(villager: Villager, result: ItemStack, emeraldPrice: Int, maxUses: Int, villagerXp: Int) {
        val recipe = MerchantRecipe(result, maxUses)
        recipe.addIngredient(ItemStack(Material.EMERALD, emeraldPrice))
        recipe.villagerExperience = villagerXp

        val recipes = villager.recipes.toMutableList()
        recipes.add(recipe)
        villager.recipes = recipes
    }

    /**
     * Check if villager already has a scroll trade
     */
    private fun hasScrollTrade(villager: Villager): Boolean {
        return villager.recipes.any { recipe ->
            plugin.scrollManager.isScroll(recipe.result)
        }
    }

    /**
     * Count how many scroll trades the villager has
     */
    private fun countScrollTrades(villager: Villager): Int {
        return villager.recipes.count { recipe ->
            plugin.scrollManager.isScroll(recipe.result)
        }
    }

    /**
     * Check if villager already has a spellbook trade
     */
    private fun hasSpellbookTrade(villager: Villager): Boolean {
        return villager.recipes.any { recipe ->
            plugin.spellbookManager.isSpellbook(recipe.result)
        }
    }

    /**
     * Check if villager already has a runebook trade
     */
    private fun hasRunebookTrade(villager: Villager): Boolean {
        return villager.recipes.any { recipe ->
            plugin.runebookManager.isRunebook(recipe.result)
        }
    }
}
