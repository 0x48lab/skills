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
            // Calculate how many times we're crafting (for shift-click)
            val craftCount = calculateCraftCount(event)

            // Process craft with the count for skill gain
            val processedResult = plugin.craftingManager.processCraft(player, result.clone(), craftCount)
            event.inventory.result = processedResult
        }
    }

    /**
     * Calculate how many times an item will be crafted
     * For shift-click, calculates max possible crafts from available materials
     */
    private fun calculateCraftCount(event: CraftItemEvent): Int {
        // Normal click = 1 craft
        if (!event.click.isShiftClick) {
            return 1
        }

        // Shift-click: calculate max possible crafts from materials
        val matrix = event.inventory.matrix
        val resultAmount = event.recipe.result.amount

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
        // Also limit by how many can fit in inventory
        val maxCrafts = minMaterialCount
        val resultPerCraft = resultAmount

        // Check available inventory space
        val inventory = event.whoClicked.inventory
        var availableSpace = 0
        for (slot in inventory.storageContents) {
            if (slot == null || slot.type == Material.AIR) {
                availableSpace += event.recipe.result.maxStackSize
            } else if (slot.type == event.recipe.result.type && slot.amount < slot.maxStackSize) {
                availableSpace += slot.maxStackSize - slot.amount
            }
        }

        val maxBySpace = availableSpace / resultPerCraft
        return minOf(maxCrafts, maxBySpace).coerceAtLeast(1)
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
     * Create a blank rune by combining Teleport Scroll + Amethyst Shard in inventory
     * Either: scroll on cursor + amethyst clicked, or amethyst on cursor + scroll clicked
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onCreateRune(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        // Get cursor item (what player is holding on cursor)
        val cursor = event.cursor
        if (cursor.type == Material.AIR) return

        // Get clicked item
        val clicked = event.currentItem
        if (clicked == null || clicked.type == Material.AIR) return

        // Check for Teleport Scroll + Amethyst Shard combination
        val isScrollOnCursor = plugin.scrollManager.isScroll(cursor)
        val isScrollClicked = plugin.scrollManager.isScroll(clicked)
        val isAmethystClicked = clicked.type == Material.AMETHYST_SHARD && !plugin.runeManager.isRune(clicked)
        val isAmethystOnCursor = cursor.type == Material.AMETHYST_SHARD && !plugin.runeManager.isRune(cursor)

        // Debug logging
        if ((isScrollOnCursor && isAmethystClicked) || (isAmethystOnCursor && isScrollClicked)) {
            val cursorSpell = if (isScrollOnCursor) plugin.scrollManager.getSpell(cursor) else null
            val clickedSpell = if (isScrollClicked) plugin.scrollManager.getSpell(clicked) else null
            plugin.logger.info("[Rune Debug] isScrollOnCursor=$isScrollOnCursor, isAmethystClicked=$isAmethystClicked")
            plugin.logger.info("[Rune Debug] isScrollClicked=$isScrollClicked, isAmethystOnCursor=$isAmethystOnCursor")
            plugin.logger.info("[Rune Debug] cursorSpell=$cursorSpell, clickedSpell=$clickedSpell")
        }

        // Check if the scroll is a Teleport scroll
        val teleportScrollOnCursor = isScrollOnCursor &&
            plugin.scrollManager.getSpell(cursor) == com.hacklab.minecraft.skills.magic.SpellType.TELEPORT
        val teleportScrollClicked = isScrollClicked &&
            plugin.scrollManager.getSpell(clicked) == com.hacklab.minecraft.skills.magic.SpellType.TELEPORT

        // Either: teleport scroll on cursor + amethyst clicked, or amethyst on cursor + teleport scroll clicked
        if ((teleportScrollOnCursor && isAmethystClicked) || (isAmethystOnCursor && teleportScrollClicked)) {
            // Cancel the event first to prevent default behavior
            event.isCancelled = true

            // Create a rune
            val rune = plugin.runeManager.createRune()

            // Consume both items (one from cursor, one from clicked)
            if (teleportScrollOnCursor && isAmethystClicked) {
                // Consume scroll from cursor
                if (cursor.amount > 1) {
                    val newCursor = cursor.clone()
                    newCursor.amount = cursor.amount - 1
                    player.setItemOnCursor(newCursor)
                } else {
                    player.setItemOnCursor(null)
                }

                // Consume one amethyst from clicked slot
                if (clicked.amount > 1) {
                    val newClicked = clicked.clone()
                    newClicked.amount = clicked.amount - 1
                    event.clickedInventory?.setItem(event.slot, newClicked)
                } else {
                    event.clickedInventory?.setItem(event.slot, null)
                }
                // Give rune to player
                val leftover = player.inventory.addItem(rune)
                if (leftover.isNotEmpty()) {
                    leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                }
            } else {
                // Consume amethyst from cursor
                if (cursor.amount > 1) {
                    val newCursor = cursor.clone()
                    newCursor.amount = cursor.amount - 1
                    player.setItemOnCursor(newCursor)
                } else {
                    player.setItemOnCursor(null)
                }

                // Consume one scroll from clicked slot
                if (clicked.amount > 1) {
                    val newClicked = clicked.clone()
                    newClicked.amount = clicked.amount - 1
                    event.clickedInventory?.setItem(event.slot, newClicked)
                } else {
                    event.clickedInventory?.setItem(event.slot, null)
                }
                // Give rune to player
                val leftover = player.inventory.addItem(rune)
                if (leftover.isNotEmpty()) {
                    leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                }
            }

            // Send message
            plugin.messageSender.send(player, com.hacklab.minecraft.skills.i18n.MessageKey.RUNE_CREATED)

            // Play sound
            player.world.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f)
        }
    }
}
