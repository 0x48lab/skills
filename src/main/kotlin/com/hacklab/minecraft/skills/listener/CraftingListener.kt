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
        if (!plugin.craftingManager.isSkillCraftable(result.type)) {
            return
        }

        val isShiftClick = event.click.isShiftClick

        // For non-stackable items with shift-click, handle manually to apply individual quality
        if (isShiftClick && result.maxStackSize == 1) {
            // Calculate how many items can be crafted
            val craftCount = calculateCraftCount(event)
            if (craftCount <= 0) return

            // Cancel the event - we'll handle crafting manually
            event.isCancelled = true

            // Consume materials
            consumeMaterials(event, craftCount)

            // Create each item with individual quality and add to inventory
            repeat(craftCount) {
                val processedResult = plugin.craftingManager.processCraft(player, result.clone(), true)
                val leftover = player.inventory.addItem(processedResult)

                // Drop any items that don't fit
                leftover.values.forEach { item ->
                    player.world.dropItemNaturally(player.location, item)
                }
            }
        } else {
            // Normal click or stackable items - use default behavior with quality
            val craftCount = if (isShiftClick) calculateCraftCount(event) else 1
            val processedResult = plugin.craftingManager.processCraft(player, result.clone(), isShiftClick, craftCount)
            event.inventory.result = processedResult
        }
    }

    /**
     * Calculate how many times an item can be crafted based on materials and inventory space
     */
    private fun calculateCraftCount(event: CraftItemEvent): Int {
        // Normal click = 1 craft
        if (!event.click.isShiftClick) {
            return 1
        }

        // Shift-click: calculate max possible crafts from materials
        val matrix = event.inventory.matrix
        val resultAmount = event.recipe.result.amount
        val resultType = event.recipe.result.type

        // Find the minimum stack size in the crafting grid (excluding empty slots)
        var minMaterialCount = Int.MAX_VALUE
        for (item in matrix) {
            if (item != null && item.type != Material.AIR) {
                minMaterialCount = minOf(minMaterialCount, item.amount)
            }
        }

        if (minMaterialCount == Int.MAX_VALUE) {
            return 1
        }

        // Calculate how many times we can craft
        val maxCrafts = minMaterialCount

        // Check available inventory space
        val inventory = event.whoClicked.inventory
        var availableSpace = 0

        if (resultType.maxStackSize == 1) {
            // Non-stackable: count empty slots
            for (slot in inventory.storageContents) {
                if (slot == null || slot.type == Material.AIR) {
                    availableSpace++
                }
            }
        } else {
            // Stackable: count available space
            for (slot in inventory.storageContents) {
                if (slot == null || slot.type == Material.AIR) {
                    availableSpace += resultType.maxStackSize
                } else if (slot.type == resultType && slot.amount < slot.maxStackSize) {
                    availableSpace += slot.maxStackSize - slot.amount
                }
            }
            availableSpace /= resultAmount
        }

        return minOf(maxCrafts, availableSpace).coerceAtLeast(1)
    }

    /**
     * Consume materials from the crafting grid
     */
    private fun consumeMaterials(event: CraftItemEvent, craftCount: Int) {
        val matrix = event.inventory.matrix
        for (i in matrix.indices) {
            val item = matrix[i]
            if (item != null && item.type != Material.AIR) {
                val newAmount = item.amount - craftCount
                if (newAmount <= 0) {
                    event.inventory.setItem(i + 1, null) // +1 because slot 0 is result
                } else {
                    item.amount = newAmount
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFurnaceClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Check if this is a furnace/smoker/blast furnace
        val invType = event.inventory.type
        if (invType != InventoryType.FURNACE &&
            invType != InventoryType.SMOKER &&
            invType != InventoryType.BLAST_FURNACE) return

        // Check if clicking on result slot (slot 2)
        if (event.rawSlot != 2) return

        val item = event.currentItem
        if (item == null || item.type == Material.AIR) return

        // Check if it's a food item (Cooking skill)
        if (item.type.isEdible || item.type.name.startsWith("COOKED_")) {
            // Get amount being taken (for shift-click, it's the full stack)
            val amount = item.amount

            // Process the food with cooking skill (skill gain for each item)
            val processedItem = plugin.craftingManager.processCooking(player, item.clone(), amount)

            // Replace the item in the slot
            event.currentItem = processedItem
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

        // Get player's Crafting skill
        val data = plugin.playerDataManager.getPlayerData(player)
        val craftingSkill = data.getSkillValue(SkillType.CRAFTING)

        // Get repair difficulty
        val difficulty = plugin.durabilityManager.getRepairDifficulty(firstItem)

        // Try skill gain
        plugin.skillManager.tryGainSkill(player, SkillType.CRAFTING, difficulty)

        // Process repair - reduce max durability (UO-style)
        val repairResult = plugin.durabilityManager.processRepair(result, craftingSkill)

        if (repairResult.success) {
            // Send message about durability reduction
            plugin.messageSender.send(
                player, MessageKey.REPAIR_SUCCESS,
                "reduction" to repairResult.reduction,
                "max" to repairResult.newMax
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBrewingClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Handle brewing stand extraction
        if (event.inventory.type == InventoryType.BREWING) {
            // Slot 0-2 are output slots
            if (event.rawSlot in 0..2 && event.currentItem != null) {
                val potion = event.currentItem!!
                if (potion.type.name.contains("POTION")) {
                    // Process the potion with alchemy skill
                    val processedPotion = plugin.craftingManager.processBrewing(player, potion.clone())

                    // Replace the item in the slot
                    event.currentItem = processedPotion
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onScrollToSpellbook(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Get cursor item (what player is holding on cursor)
        val cursor = event.cursor
        if (cursor.type == Material.AIR) return

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
            plugin.messageSender.send(player, MessageKey.SCROLL_ALREADY_KNOWN)
            event.isCancelled = true
        }
    }

    /**
     * Handle rune crafting via PrepareItemCraftEvent
     * Recipe: Teleport Scroll (top) + Amethyst Shard (bottom) = Blank Rune
     * Vertical pattern only
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareRuneCraft(event: org.bukkit.event.inventory.PrepareItemCraftEvent) {
        val matrix = event.inventory.matrix
        val matrixSize = matrix.size

        // Count non-empty items - must be exactly 2
        var itemCount = 0
        for (item in matrix) {
            if (item != null && item.type != Material.AIR) itemCount++
        }
        if (itemCount != 2) return

        // Check for vertical pattern: Scroll on top, Amethyst below
        // Valid vertical pairs depend on grid size:
        // 2x2 grid (size 4): (0,2), (1,3)
        // 3x3 grid (size 9): (0,3), (1,4), (2,5), (3,6), (4,7), (5,8)
        val verticalPairs = if (matrixSize == 4) {
            // 2x2 player inventory crafting grid
            listOf(Pair(0, 2), Pair(1, 3))
        } else {
            // 3x3 crafting table grid
            listOf(
                Pair(0, 3), Pair(1, 4), Pair(2, 5),
                Pair(3, 6), Pair(4, 7), Pair(5, 8)
            )
        }

        for ((topIndex, bottomIndex) in verticalPairs) {
            // Safety check for bounds
            if (topIndex >= matrixSize || bottomIndex >= matrixSize) continue

            val topItem = matrix[topIndex]
            val bottomItem = matrix[bottomIndex]

            if (topItem == null || bottomItem == null) continue
            if (topItem.type == Material.AIR || bottomItem.type == Material.AIR) continue

            // Check: Top = Teleport Scroll, Bottom = Amethyst Shard
            val isTopTeleportScroll = plugin.scrollManager.isScroll(topItem) &&
                plugin.scrollManager.getSpell(topItem) == com.hacklab.minecraft.skills.magic.SpellType.TELEPORT
            val isBottomAmethyst = bottomItem.type == Material.AMETHYST_SHARD &&
                !plugin.runeManager.isRune(bottomItem)

            if (isTopTeleportScroll && isBottomAmethyst) {
                event.inventory.result = plugin.runeManager.createRune()
                return
            }
        }
    }

    /**
     * Handle taking the crafted rune - play sound and send message
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCraftRune(event: CraftItemEvent) {
        val result = event.recipe.result
        if (plugin.runeManager.isRune(result)) {
            val player = event.whoClicked as? Player ?: return
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.RUNE_CREATED)
            player.world.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f)
        }
    }
}
