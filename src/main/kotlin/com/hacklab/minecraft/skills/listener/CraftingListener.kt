package com.hacklab.minecraft.skills.listener

import com.hacklab.minecraft.skills.Skills
import com.hacklab.minecraft.skills.i18n.MessageKey
import com.hacklab.minecraft.skills.skill.SkillType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.Material

class CraftingListener(private val plugin: Skills) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val result = event.recipe.result

        // Check if this is a skill-craftable item
        if (plugin.craftingManager.isSkillCraftable(result.type)) {
            val processedResult = plugin.craftingManager.processCraft(player, result.clone())
            event.inventory.result = processedResult
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFurnaceExtract(event: FurnaceExtractEvent) {
        val player = event.player
        val material = event.itemType

        // Check if it's a food item (Cooking skill)
        if (material.isEdible || material.name.startsWith("COOKED_")) {
            val item = org.bukkit.inventory.ItemStack(material, event.itemAmount)
            plugin.craftingManager.processCooking(player, item)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onAnvilPrepare(event: PrepareAnvilEvent) {
        val player = event.viewers.firstOrNull() as? Player ?: return
        val result = event.result ?: return

        // Check if this is a repair (not just renaming)
        val anvil = event.inventory
        val firstItem = anvil.getItem(0)
        val secondItem = anvil.getItem(1)

        // If repairing with material or combining items
        if (firstItem != null && plugin.durabilityManager.isRepairable(firstItem)) {
            // Apply max durability cap to the result
            val customMax = plugin.durabilityManager.getCustomMaxDurability(firstItem)
            if (customMax != null) {
                // Copy custom max to result and cap durability
                plugin.durabilityManager.setCustomMaxDurability(result, customMax)
                plugin.durabilityManager.capDurabilityToMax(result)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnvilTakeResult(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if this is an anvil
        if (event.inventory.type != InventoryType.ANVIL) return

        // Check if clicking on result slot (slot 2)
        if (event.rawSlot != 2) return

        val result = event.currentItem
        if (result == null || result.type == Material.AIR) return

        val anvil = event.inventory as? AnvilInventory ?: return
        val firstItem = anvil.getItem(0) ?: return

        // Check if this is a repair operation (item has durability)
        if (!plugin.durabilityManager.isRepairable(firstItem)) return

        // Get player's Blacksmithy skill
        val data = plugin.playerDataManager.getPlayerData(player)
        val blacksmithySkill = data.getSkillValue(SkillType.BLACKSMITHY)

        // Get repair difficulty
        val difficulty = plugin.durabilityManager.getRepairDifficulty(firstItem)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.BLACKSMITHY, difficulty)

        // Process repair - reduce max durability (UO-style)
        val repairResult = plugin.durabilityManager.processRepair(result, blacksmithySkill)

        if (repairResult.success) {
            // Send message about durability reduction
            plugin.messageSender.send(
                player, MessageKey.REPAIR_SUCCESS,
                "reduction" to repairResult.reduction,
                "max" to repairResult.newMax
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Handle brewing stand extraction
        if (event.inventory.type == InventoryType.BREWING) {
            // Slot 0-2 are output slots
            if (event.slot in 0..2 && event.currentItem != null) {
                val potion = event.currentItem!!
                if (potion.type.name.contains("POTION")) {
                    plugin.craftingManager.processBrewing(player, potion)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onScrollToSpellbook(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Get cursor item (what player is holding on cursor)
        val cursor = event.view.cursor
        if (cursor == null || cursor.type == Material.AIR) return

        // Get clicked item
        val clicked = event.currentItem
        if (clicked == null || clicked.type == Material.AIR) return

        // Cursor must be a scroll, clicked must be a spellbook
        if (!plugin.scrollManager.isScroll(cursor)) {
            return
        }
        if (!plugin.spellbookManager.isSpellbook(clicked)) {
            return
        }

        // Get the spell from the scroll
        val spell = plugin.scrollManager.getSpell(cursor) ?: return

        // Try to add the spell to the spellbook
        if (plugin.spellbookManager.addSpell(clicked, spell)) {
            // Spell learned successfully
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.SCROLL_LEARNED, "spell" to spell.displayName)

            // Consume the scroll - must update through view
            if (cursor.amount > 1) {
                val newCursor = cursor.clone()
                newCursor.amount = cursor.amount - 1
                event.view.setCursor(newCursor)
            } else {
                event.view.setCursor(null)
            }

            // Cancel the event to prevent item swap
            event.isCancelled = true
        } else {
            // Spell already known
            player.sendMessage("§7You already know this spell.")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onCreateRune(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Get cursor item (what player is holding on cursor)
        val cursor = event.view.cursor
        if (cursor == null || cursor.type == Material.AIR) return

        // Get clicked item
        val clicked = event.currentItem
        if (clicked == null || clicked.type == Material.AIR) return

        // Check for Gate Travel scroll + Amethyst Shard combination
        val isScrollOnCursor = plugin.scrollManager.isScroll(cursor)
        val isAmethystClicked = clicked.type == Material.AMETHYST_SHARD && !plugin.runeManager.isRune(clicked)
        val isAmethystOnCursor = cursor.type == Material.AMETHYST_SHARD && !plugin.runeManager.isRune(cursor)
        val isScrollClicked = plugin.scrollManager.isScroll(clicked)

        // Either: scroll on cursor + amethyst clicked, or amethyst on cursor + scroll clicked
        val gateScrollOnCursor = isScrollOnCursor &&
            plugin.scrollManager.getSpell(cursor) == com.hacklab.minecraft.skills.magic.SpellType.GATE_TRAVEL
        val gateScrollClicked = isScrollClicked &&
            plugin.scrollManager.getSpell(clicked) == com.hacklab.minecraft.skills.magic.SpellType.GATE_TRAVEL

        if ((gateScrollOnCursor && isAmethystClicked) || (isAmethystOnCursor && gateScrollClicked)) {
            // Create a rune
            val rune = plugin.runeManager.createRune()

            // Determine which item is the scroll and which is the amethyst
            if (gateScrollOnCursor && isAmethystClicked) {
                // Consume the scroll from cursor
                if (cursor.amount > 1) {
                    val newCursor = cursor.clone()
                    newCursor.amount = cursor.amount - 1
                    event.view.setCursor(newCursor)
                } else {
                    event.view.setCursor(null)
                }

                // Replace the amethyst with the rune
                event.currentItem = rune
            } else {
                // Consume the amethyst from cursor
                if (cursor.amount > 1) {
                    val newCursor = cursor.clone()
                    newCursor.amount = cursor.amount - 1
                    event.view.setCursor(newCursor)
                } else {
                    event.view.setCursor(null)
                }

                // Replace the scroll with the rune
                event.currentItem = rune
            }

            // Send message
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.RUNE_CREATED)

            // Play sound
            player.world.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f)

            event.isCancelled = true
        }
    }
}
