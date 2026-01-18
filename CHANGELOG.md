# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.9] - 2026-01-18

### Changed
- **Auto-Fishing Improvement**
  - Skill 0-19: No auto-fishing (manual only)
  - Skill 20-99: Roll probability on fish bite
    - Success: Auto-reel + auto-recast (fishing continues)
    - Failure: Message displayed, fish escapes, player must manually reel in
  - Skill 100 (GM): Always auto-reel + auto-recast (true AFK fishing)
  - This makes the skill progression more meaningful - GM is the only truly AFK-capable level
  - Added failure message: "You lost your focus... the fish got away!"

### Added
- **NMS Ecosystem (Centralized NMS Code)**
  - Created `/nms/` package to consolidate all NMS-related code
  - `NmsVersion.kt`: Server version detection with Paper 1.20.5+ unversioned package support
  - `ReflectionCache.kt`: Centralized caching for reflection lookups (classes, methods, fields)
  - `FishingNms.kt`: Fishing-specific NMS operations (moved from FishingReflectionHelper)
  - `NmsManager.kt`: Central manager for all NMS features
  - Future NMS code should follow this pattern for easier version upgrades

### Removed
- Deleted `FishingReflectionHelper.kt` (migrated to NMS ecosystem)

## [0.4.8] - 2026-01-17

### Fixed
- **Pick Block (Middle-Click) Not Working**
  - Fixed pick block functionality not finding items in inventory
  - Root cause: `StackBonusManager` modifies `maxStackSize` from 64 to 99 for stackable items
  - This caused `ItemStack.isSimilar()` to fail when comparing inventory items to world blocks
  - Solution: Added `PickBlockListener` that intercepts `PlayerPickBlockEvent` when vanilla search fails
  - Custom search ignores `maxStackSize` differences and matches by material type only
  - Handles special block-to-item mappings (ores → drops, wall items → regular items)

## [0.4.7] - 2026-01-11

### Added
- **DEX Requirement for Armor (UO-Style)**
  - Heavy armor now requires minimum DEX to equip (like Ultima Online)
  - Players with low DEX (e.g., STR/INT builds) cannot wear plate armor
  - DEX requirements by armor tier:
    - Netherite: DEX 50+ (full set), 35-50 per piece
    - Diamond: DEX 45+ (full set), 30-45 per piece
    - Iron: DEX 35+ (full set), 20-35 per piece
    - Chainmail: DEX 25+ (full set), 10-25 per piece
    - Leather/Gold: No DEX requirement
  - Error messages shown when trying to equip with insufficient DEX
  - Armor automatically removed when DEX drops below requirement

### Changed
- **End City Scroll Drop Rates (Rebalanced)**
  - End City chests now drop C5-C8 scrolls only (high-circle focus)
  - New drop rates for endgame-appropriate rewards:
    - C5-C6: 10% (was: none)
    - C7: 5% (was: 0.5%)
    - C8: 2% (was: 0.2%)
  - Total ~17% per chest, ~4 scrolls per hour (25 chests)
  - C7-C8 scrolls: ~1-2 per hour
  - Removed C1-C4 from End City (available in Trial Chambers instead)

### Fixed
- Fixed Bless spell particle effect (INSTANT_EFFECT → ENCHANT)

## [0.4.6] - 2026-01-09

### Changed
- **Production Skill Consolidation** (Breaking Change)
  - Merged 4 production skills into 2:
    - **CRAFTING**: Equipment, tools, weapons, armor (merged Blacksmithy + Craftsmanship)
      - STR weight: 60%, DEX weight: 40%
      - Covers: Metal items, wooden items, leather items, bows, tools, shields
    - **COOKING**: Food and potions (merged Cooking + Alchemy)
      - STR weight: 50%, INT weight: 50%
      - Covers: Furnace cooking, crafted food, potions, brewing
  - Stack bonus now uses 3 skills (max 300) instead of 4 skills (max 400)
    - Formula: `stackSize = 64 + (CRAFTING + COOKING + INSCRIPTION) / 300 * 35`
  - Mending enchantment now uses CRAFTING skill for all craftable equipment
  - Skill titles updated: removed Blacksmith/Alchemist, kept Artisan/Chef
  - Quality system unchanged (difficulty-relative HQ/EX calculation)
  - Food/Potion bonus effects unchanged (recovery bonus, duration extension)

### Removed
- SkillType.BLACKSMITHY (merged into CRAFTING)
- SkillType.CRAFTSMANSHIP (merged into CRAFTING)
- SkillType.ALCHEMY (merged into COOKING)

## [0.4.5] - 2026-01-06

### Fixed
- **Trial Chamber Vault Key Issue (CRITICAL)**
  - Fixed Trial Keys obtained from Trial Spawners not working with Vault blocks
  - Root cause: `StackBonusManager` was modifying Trial Key's ItemMeta when picked up
  - Trial Keys stack to 64, so they passed the stackability check and got modified
  - This corrupted the key's NBT data, making Vault blocks reject them
  - Creative-obtained keys worked because they bypassed the pickup event
  - Solution: Added `TRIAL_KEY` and `OMINOUS_TRIAL_KEY` to exclusion list in `StackBonusManager`

- **Rune Crafting ArrayIndexOutOfBoundsException**
  - Fixed crash when using 2x2 inventory crafting grid for rune crafting
  - Root cause: `onPrepareRuneCraft` was using 3x3 grid indices (0-8) for all grids
  - 2x2 grid only has indices 0-3, causing ArrayIndexOutOfBoundsException at index 4
  - Solution: Detect grid size and use appropriate vertical pairs
    - 2x2 grid: (0,2), (1,3)
    - 3x3 grid: (0,3), (1,4), (2,5), (3,6), (4,7), (5,8)

## [0.4.4] - 2026-01-06

### Added
- **`/rune` Command for Power Words Casting**
  - New command dedicated to UO-style Power Words incantations
  - Example: `/rune In Mani` (Heal), `/rune Vas Flam` (Fireball)
  - Aliases: `/powerwords`, `/pw`
  - Word-by-word tab completion for multi-word incantations
  - `/cast` command now only accepts English spell names

- **Spellbook Target Type Display**
  - Each spell page now shows target type (Self, Entity, Location, Area, etc.)
  - Localized: English and Japanese translations

- **Spellbook Layout Improvements**
  - One spell per page for stable display (reagents always visible)
  - Removed "Incantation:" label, Power Words shown directly
  - Removed redundant "Mana: N" line
  - Reagents displayed on separate lines with bullet points

### Changed
- **Command Separation**
  - `/cast <spell name>` - Cast by English name (e.g., `/cast fireball`)
  - `/rune <Power Words>` - Cast by Power Words (e.g., `/rune Vas Flam`)
  - Tab completion logic shared via SpellbookManager helper methods

### Fixed
- **Tab Completion for Power Words**
  - Fixed multi-word completion causing text duplication
  - Now completes word-by-word: `/rune Kal` → Tab → `Vas` → `/rune Kal Vas`

## [0.4.3] - 2026-01-05

### Added
- **Power Words Casting Support**
  - Cast spells using UO-style incantations (e.g., `/cast Vas Flam` for Fireball)
  - Tab completion shows both spell display names and Power Words
  - Duplicate Power Words (e.g., "Kal Vas Flam") resolve to lower circle spell
  - All 47 spells have unique Power Words

- **Enchantment-Style Casting Particles**
  - Magical glyphs (like enchantment table) spiral around caster during chanting
  - Particles rise and pulse around the player
  - Intensity scales with spell circle:
    - C1-C2: Basic ENCHANT particles (3-4 glyphs)
    - C3-C5: Enhanced effect with foot particles (5-7 glyphs)
    - C6: Adds END_ROD particles
    - C7-C8: Adds WITCH particles for dramatic effect
  - Effects persist until casting completes or is cancelled

- **Spellbook Power Words Display**
  - Each spell entry now shows its Power Words/incantation
  - Localized labels: "Incantation" (English) / "詠唱" (Japanese)
  - Helps players learn the UO-style casting phrases

### Technical
- FloatingTextManager: Particle-based effect system replacing TextDisplay
- SpellType.fromPowerWords(): Lookup spell by Power Words with duplicate handling
- CastCommand: Enhanced parsing to accept spell names, enum names, or Power Words

## [0.4.2] - 2026-01-05

### Added
- **Chunk-Based Mob Limit System**
  - Prevents server lag from automatic breeding farms and mob spawners
  - Limits mob count per chunk by category:
    - Passive (animals): 24 mobs
    - Hostile (monsters): 32 mobs
    - Ambient (bats): 8 mobs
    - Water Creature (squid, dolphin): 8 mobs
    - Water Ambient (fish): 16 mobs
  - Protected entities exempt from limits:
    - Boss mobs: Ender Dragon, Wither, Elder Guardian
    - System mobs: Villager, Iron Golem, Snow Golem, Wandering Trader, Trader Llama
  - Protected spawn reasons exempt from limits:
    - Commands, custom spawns, golem/wither construction
    - Mob conversions (zombie drowning, piglin zombification, etc.)
  - Breeding blocked notification sent to players when limit reached
  - Toggleable via `chunk_mob_limit.enabled` in config.yml
  - All limits configurable in config.yml
  - Bilingual support (English/Japanese)

### Technical
- MobLimitCategory enum mapping Minecraft SpawnCategory to custom limits
- ProtectedEntityTypes object for exempt entity types and spawn reasons
- MobLimitManager with ConcurrentHashMap-based chunk caching
- MobLimitListener handles spawn, breed, death, and chunk load/unload events
- Cache automatically initialized on chunk load and cleared on chunk unload

## [0.4.1] - 2026-01-05

### Fixed
- **Trial Chamber Vault Compatibility**
  - Fixed StackBonusListener interfering with vanilla Vault behavior
  - Trial Keys now properly consumed when opening vaults
  - Vault loot now generates correctly
  - Added `isVaultInventory()` check to exclude Vault blocks from stack bonus processing

### Added
- **Trial Chamber Scroll Drops**
  - Scrolls can now drop from Trial Chamber vault rewards
  - Supports both normal and ominous trial rewards
  - Drop rates by circle:
    - C1-C3: 8% (common spells)
    - C4-C5: 4% (mid-tier spells)
    - C6: 2% (high-tier spells)
    - C7: 0.8% (rare spells)
    - C8: 0.3% (legendary spells)
  - Configurable via `scroll.trial_chamber_enabled` in config.yml

## [0.3.1] - 2026-01-04

### Added
- **Sleep Skip Command**
  - `/sleep` command allows players to be counted as sleeping without being in a bed
  - Requirements: night time, in Overworld, at least one player already sleeping in a bed
  - When enough players are sleeping (including /sleep users), night will be skipped
  - Respects `playersSleepingPercentage` game rule
  - Auto-cancels on world change, logout, or when night is skipped
  - Bilingual support (English/Japanese)

- **Sleep Notification System**
  - When a player enters a bed, awake players receive a clickable notification
  - Notification includes a clickable button that runs `/sleep` command
  - Hover text explains the action
  - Messages localized in English and Japanese

- **Sleep Feature Configuration**
  - Added `sleep.enabled` config option to enable/disable the sleep skip feature
  - Enabled by default
  - When disabled, `/sleep` command shows disabled message and bed notifications are not sent

## [0.3.0] - 2026-01-04

### Added
- **Health Boost for High Quality Food**
  - HQ food grants Health Boost I (+2 hearts) for 30 seconds when eaten
  - EX food grants Health Boost I (+2 hearts) for 1 minute when eaten
  - Balanced to not overshadow golden apples (which give Absorption + Regeneration)
  - Food quality is now stored in PersistentDataContainer

- **GM (Skill 100) Complete Immunity**
  - All survival skills at level 100 now grant complete damage immunity
  - Uses `event.isCancelled = true` to prevent damage AND screen shake effect
  - Applies to: Athletics, Swimming, Heat Resistance, Cold Resistance, Endurance
  - Heat Resistance GM: Also immediately extinguishes fire (`fireTicks = 0`)
  - Cold Resistance GM: Also immediately clears freeze (`freezeTicks = 0`)

### Fixed
- **Item Stacking Bug**
  - Fixed items with same name/quality not stacking when picked up
  - Root cause: `setMaxStackSize()` created different ItemMeta preventing stacking
  - Solution: Synchronize MaxStackSize across all items of same type using highest value
  - Applies when picking up items, taking from containers, and on login

- **Food Item Stacking Issue (Floating Point Precision)**
  - Fixed same-quality food items not stacking due to floating point precision differences
  - Root cause: Cooking skill increases during cooking, causing slightly different bonus values
  - Solution: Cooking bonus now rounded to 1% precision (matching Lore display)
  - Both PDC value AND Lore are now updated consistently using the same rounded value
  - Existing items are automatically normalized when picking up food items
  - Old and new items can now stack together after picking up any food item

- **Survival Skills Damage Rebalancing** (Breaking Change)
  - All survival skills now compensate for internal HP system (x10 damage multiplier)
  - New formula: 90% base reduction + up to 90% skill-based reduction
  - Skill 0: ~vanilla damage (10% of internal damage)
  - Skill 100: 1% damage (effectively immune, but with screen shake)
  - Affected skills: Athletics, Swimming, Heat Resistance, Cold Resistance, Endurance
  - Contact damage (cactus/berry bush) now uses same formula via Endurance skill

- **Admin Command Skill Name Parsing**
  - Fixed `/skilladmin set <player> <skill> <value>` failing for skills with spaces
  - Example: `/skilladmin set player Heat Resistance 100` now works correctly
  - Skill name is parsed by joining all arguments between player and value

### Changed
- **Endurance Skill Expanded**
  - Now covers both suffocation damage and contact damage (cactus, berry bush, etc.)
  - Skill gains from both suffocation (difficulty 50) and contact damage (difficulty 15)

## [0.2.9] - 2026-01-04

### Added
- **UO-Style Runebook System**
  - Runebook item that stores up to 16 runes
  - Purchase from level 3 Librarian villagers (40-60 emeralds)
  - GUI interface with 27 slots (3 rows)
    - Row 1-2: 16 rune slots (8 per row)
    - Row 3: Drop zone for adding runes
  - Click on registered rune to cast Recall (left-click) or Gate Travel (shift+left-click)
  - Drag & drop marked runes to register them
  - Right-click on registered rune to remove it (returns as item)
  - Full spell casting requirements apply (reagents, mana, casting time)
  - Bilingual support (English/Japanese)

### Technical
- RunebookManager for item creation and rune data management
- RunebookListener for GUI handling and spell integration
- JSON storage using Gson (PDC-based persistence)
- Integration with existing SpellManager for Recall/Gate Travel

### Fixed
- Clear pending runebook spell locations when casting is cancelled
- Clean up runebook data on player quit to prevent memory leaks
- Gate Travel duration now fixed at 30 seconds (UO-style)
- Japanese runebook name changed from "ルーンブック" to "ルーンの書"

## [0.2.8] - 2026-01-03

### Added
- **UO-Style Deadly Poison System**
  - 4 poison levels: Lesser, Regular, Greater, Deadly
  - Each level has different damage, difficulty, and Alchemy requirements:
    - Lesser Poison (弱毒): 0.5 dmg/s, difficulty 20, Alchemy 0+
    - Regular Poison (毒): 1.0 dmg/s, difficulty 40, Alchemy 30+
    - Greater Poison (強毒): 1.5 dmg/s, difficulty 60, Alchemy 60+
    - Deadly Poison (猛毒): 2.0 dmg/s, difficulty 80, Alchemy 90+
  - Custom damage system independent of vanilla POISON effect
  - Poison damage reduces target to 1 HP minimum (UO-style, won't kill)
  - Variable difficulty allows Poisoning skill training to 100

- **Alchemy-Poison Integration**
  - Brewing poison potions now creates custom poison based on Alchemy skill
  - Vanilla Poison → Lesser/Regular/Greater/Deadly based on skill level
  - Vanilla Strong Poison → Regular/Greater/Deadly based on skill level
  - Custom poison potions show damage rate and Alchemy requirement in lore

- **Poison Level Display**
  - Weapon lore shows poison level and remaining charges
  - Color-coded by poison level (green → dark green → cyan → purple)
  - Messages display localized poison level names (English/Japanese)

### Changed
- **PoisoningManager Rewrite**
  - Complete rewrite to support poison levels
  - Uses BukkitRunnable for periodic poison damage application
  - Success chance now based on skill vs poison difficulty delta

## [0.2.7] - 2026-01-02

### Added
- **Shovel Digging System**
  - Digging soft blocks with shovels now gains Mining skill
  - Target blocks: dirt, grass, sand, gravel, clay, soul sand, snow, mud, concrete powder, etc.
  - Difficulty range: 5 (basic soil) to 25 (suspicious sand/gravel)
  - Durability reduction based on Mining skill (skill 100 = 100% reduction)
  - Haste speed bonus based on Mining skill (up to Haste II at skill 80+)

- **Gathering Speed Bonus System** (Haste Effect)
  - Mining ores, chopping logs, and digging soft blocks now apply Haste effect
  - Speed bonus calculated from skill level:
    - Mining: STR/10 + MiningSkill/10 (max 20%)
    - Lumberjacking: LumberjackingSkill/2 (max 50%)
    - Digging: MiningSkill/2 (max 50%)
  - Haste level applied:
    - 10-19% bonus: Haste I
    - 20-39% bonus: Haste I
    - 40%+ bonus: Haste II
  - Does not override stronger Haste effects (beacons, potions)

- **Cooking Skill Effects**
  - Food cooked gets recovery bonus based on Cooking skill
  - Formula: baseRecovery × (1 + skill/4/100 + qualityBonus)
  - Quality modifiers: LQ -15%, NQ +0%, HQ +15%, EX +25%
  - Difficulty range: 5 (dried kelp) to 100 (enchanted golden apple)

- **Alchemy Skill Effects**
  - Brewed potions get duration extension based on Alchemy skill
  - Formula: baseDuration × (1 + skill/200) - max +50% at skill 100
  - Quality affects potion strength
  - Difficulty range: 15 (mundane) to 55 (special potions)
  - Modifiers: Extended +15, Enhanced +20, Splash +10, Lingering +20

- **Tinkering Skill Effects**
  - Mending success rate for tinkering items depends on Tinkering skill
  - Formula: min(100, TinkeringSkill × 100/60)
  - Skill 0: 0%, Skill 30: 50%, Skill 60+: 100%
  - Target items: Fishing Rod, Shears, Flint and Steel, Clock, Compass, Spyglass

- **Spear Skill** (1.21.5+)
  - New combat skill for spear weapons
  - DEX-weighted skill (affects DEX stat calculation)
  - Supports all spear tiers: wooden, stone, iron, golden, diamond, netherite
  - Skill gain on dealing damage with spears

- **UO-Style Repair Degradation System**
  - Repairing items on anvil now reduces max durability (like Ultima Online)
  - Higher Blacksmithy skill = less max durability lost per repair
  - Formula: reduction = (110 - Blacksmithy skill) / 10
    - GM Blacksmith (skill 100): loses 1 max durability per repair
    - Skill 0: loses 11 max durability per repair
  - Custom max durability stored via PersistentDataContainer
  - Durability shown in item lore: "耐久度: current/max (percentage%)"
  - Color-coded percentage: green (>75%), yellow (>50%), orange (>25%), red (<=25%)
  - Blacksmithy skill gain on anvil repair (based on item difficulty)

- **Mending Enchantment + Crafting Skill Integration**
  - Mending enchantment success rate now depends on appropriate crafting skill
  - Item type determines required skill:
    - Metal items (iron/gold/diamond/netherite/chain/stone), trident, mace, spears → Blacksmithy
    - Bow, crossbow → Bowcraft
    - Leather armor, wooden items, shield → Craftsmanship
    - Fishing rod, shears, flint and steel → Tinkering
    - Elytra, turtle helmet → No skill required (always 100%)
  - Formula: success rate = min(100, skill × 100 / 60)
    - Skill 0: 0% success rate
    - Skill 30: 50% success rate
    - Skill 40: 67% success rate
    - Skill 60+: 100% success rate (full vanilla behavior)
  - When mending fails, XP goes to player instead of repairing the item
  - Encourages players to level appropriate crafting skills for equipment maintenance

- **Internal Stamina System (Monster Hunter Style)**
  - New internal stat: Stamina (100 + DEX + Focus/2)
  - Consumed while sprinting (20/sec base)
  - Regenerates when not sprinting (40/sec base, boosted by Focus)
  - When stamina depleted: exhausted state
    - Walk speed reduced to 50% (setWalkSpeed)
    - Jumping disabled (Jump Boost -200)
    - Panting effect: breath particles + wolf pant sound
    - Focus bonus ignored during exhaustion (fixed 40/sec recovery)
  - Must recover to 50 stamina before normal movement returns
  - Implementation based on SurvivalMethod plugin approach

- **Focus Skill Effects**
  - Max stamina bonus: +Focus/2
  - Stamina consumption reduction: -(Focus/2)% (max 50%)
  - Stamina regeneration bonus: +Focus% (max 100%) - only when not exhausted
  - Skill gain: Every 5 seconds of sprinting

- **Scoreboard HP/Mana/Stamina Display**
  - Sidebar now shows ❤ HP, 🍖 Mana, ⚡ Stamina
  - Real-time updates alongside STR/DEX/INT

- **Farming Skill**
  - New gathering skill for agriculture activities
  - Skill gains from: harvesting mature crops, tilling soil, planting seeds, using bone meal
  - Bonus harvest chance: skill/2 % (max 50%)
  - Extra seed drop chance: skill/4 % (max 25%)
  - Bone meal effectiveness bonus: +skill/5 % (max 20%)
  - Crop difficulty range: 10-45 (wheat=10, pitcher plant=45)
  - DEX-weighted skill (affects DEX stat calculation)

- **Bow/Crossbow Balance System**
  - Distance falloff: 100% at 0-15 blocks, linear to 50% at 15-30 blocks, 50% at 30+
  - Movement penalty: -30% damage when shooting while moving
  - Projectile shield parrying: Parrying × 0.6 (max 60%), 50% damage reduction
  - Crossbow base damage increased to 10 (from 9), bow stays at 6

- **Energy Bolt Spell** (5th Circle)
  - Single-target direct damage spell
  - Base damage: 80 (highest single-target spell)
  - Reagents: Spider Eye + Blaze Powder
  - Power Words: "Corp Por"

- **Power Words System**
  - UO-style incantations broadcast to nearby players when casting
  - Each spell has unique power words (e.g., "Vas Flam" for Fireball)

- **Librarian Villager Trades**
  - Librarians now sell spellbooks and scrolls
  - Level 1: C1 scroll (5-8 emeralds)
  - Level 2: C1/C2 scroll (8-12 emeralds)
  - Level 3: Empty spellbook (30-50 emeralds)
  - Level 4: C2 scroll (12-15 emeralds)

### Removed
- **Spellbook Crafting Recipe**
  - Removed Book + Echo Shard = Empty Spellbook recipe
  - Spellbooks are now obtained from Librarian villagers

### Fixed
- **MiniMessage Legacy Format Code Error**
  - Fixed `ArmorManager.getArmorDisplayName()` converting Adventure Component to legacy § codes
  - Fixed `DurabilityManager.updateDurabilityLore()` using legacy § color codes in item lore
  - Fixed `CraftingListener` and `ThiefListener` using legacy § codes in `sendMessage()`
  - All text now uses Adventure Components with `NamedTextColor` and `PlainTextComponentSerializer`

### Added
- **Armor System**
  - ArmorManager: Calculate total AR (Armor Rating) and DEX penalty
  - ArmorConfig: Load armor stats from `armor.yml`
  - ArmorListener: Apply DEX penalty when armor changes
  - `armor.yml`: Define armor stats for all armor types

- **Combat Configuration**
  - CombatConfig: Load weapon/mob/enchantment stats from YAML files
  - `weapons.yml`: Define base damage and skill type for each weapon
  - `enchantments.yml`: Define damage increase per enchantment level
  - `mobs.yml`: Define physical/magic defense and attack power for 56+ mob types

- **Scroll Loot System**
  - ScrollDropManager: Handle scroll drops from mobs and chests
  - ScrollLootListener: Listen for mob death and chest open events
  - MagicMob: Define which mobs drop scrolls and drop rates
  - EndChestLoot: Define end city chest scroll loot tables

- **UO-Style Skill Titles**
  - SkillTitleManager: Generate titles based on highest skill
  - Titles: Neophyte, Novice, Apprentice, Journeyman, Expert, Adept, Master, Grandmaster
  - Display on scoreboard (split into 2 lines)
  - Show in `/skills` command output

- **Admin Commands**
  - `/skilladmin check <player>`: View player's skills and stats
  - `/skilladmin setstat <player> <STR|DEX|INT> <value>`: Set player stats

- **Stat Lock System**
  - Lock STR/DEX/INT to UP, DOWN, or LOCKED mode
  - Display lock icons on scoreboard

- **VengefulMobs Feature**
  - Passive mobs (sheep, cow, pig, etc.) fight back when attacked
  - Configurable anger duration, chase distance, and aggression modes
  - Per-mob overrides for damage, speed, and behavior

- **Passive Animal Combat Stats**
  - Added defense values for: CHICKEN, RABBIT, PIG, SHEEP, COW, MOOSHROOM, HORSE, DONKEY, MULE, LLAMA, GOAT
  - Added BOGGED (1.21 swamp skeleton variant)

### Changed
- **Magic Spell Base Damage Rebalance**
  - Increased base damage for all attack spells to compensate for reagent costs
  - Magic Arrow: 2 → 20, Harm: 4 → 35, Fireball: 6 → 50
  - Lightning: 8 → 65, Fire Wall: 40, Energy Bolt: 80
  - Explosion: 10 → 90, Meteor Swarm: 15 → 130
  - Magic now stronger than bow/crossbow to justify reagent consumption

- **Targeting Time Reverted**
  - Targeting time back to 5 seconds (was briefly 2 seconds)
  - Since spells fire immediately on click, shorter timeout just made players rushed

- **CLAUDE.md Specification Updates**
  - Updated combat damage formula to match implementation
  - Updated armor table (STR requirements, DEX penalties) to match armor.yml
  - Added bow/crossbow balance section with penalties
  - Fixed outdated magic base damage table

- **Swimming Skill Balance**
  - Swimming skill now only gains on drowning damage (not every air tick)
  - Previously gained too fast due to air change events firing every tick
  - Now consistent with other survival skills (Athletics, Heat Resistance, etc.)
  - Drowning damage handling separated from Endurance skill

- **Combat Damage Formula** (Breaking Change)
  - Tactics now essential: `Tactics / 100` (0.1 to 1.0 multiplier)
  - Anatomy now affects damage: `0.5 + (Anatomy / 100)` (0.6 to 1.5 multiplier)
  - STR bonus reduced: `1 + (STR / 200)` (max +50%, was +100%)
  - Critical multiplier increased: 2.0x (was 1.5x)
  - Low skills (~10) deal ~6% damage, high skills (100) deal ~150% damage

- **Magic Damage Formula** (Breaking Change)
  - Magery now essential: `Magery / 100` (0.1 to 1.0 multiplier)
  - EvalInt now affects damage: `0.5 + (EvalInt / 100)` (0.6 to 1.5 multiplier)
  - Low skills (~10) deal ~6% damage, high skills (100) deal ~150% damage

### Fixed
- Fixed damage division bug where mobs took 10x less damage than intended
- Fixed Parrying skill gain only triggering on successful parry (UO-style behavior)

### Removed
- Deprecated `SkillbookCommand.kt`
- Deprecated `SpellbookCommand.kt`

## [0.2.2] - 2025-12-27

### Added
- **VengefulMobs: Per-mob attack cooldown**
  - Added `attack_cooldown` setting for each mob type
  - Default cooldown: 1000ms (same as vanilla mobs)
  - Chicken/Rabbit: 1500ms with reduced damage (1.0)
  - Goat: 800ms (faster, more aggressive)

### Fixed
- **Armor validation on STR change**
  - Now validates equipment when player joins (catches existing players after update)
  - Now validates equipment when STR decreases during gameplay
  - Players wearing armor they can't equip will have it removed automatically

- **Armor removal item loss in Creative mode**
  - Fixed items disappearing when armor is removed due to STR requirements
  - Now clones ItemStack before removing from slot

- **Vanilla critical hit precision**
  - Changed baseDamage from Int to Double to preserve vanilla jump-attack critical bonus (1.5x)
  - Previously truncated to integer, losing precision

- **VengefulMobs attack speed**
  - Fixed hardcoded 500ms attack cooldown that made all mobs attack too fast
  - Now uses configurable per-mob cooldown (default 1000ms)

## [1.0.0] - Initial Release

### Added
- **34 Skills** inspired by Ultima Online
  - Combat: Swordsmanship, Axe, Mace Fighting, Archery, Throwing, Wrestling, Tactics, Anatomy, Parrying, Focus
  - Magic: Magery, Evaluating Intelligence, Meditation, Resisting Spells, Inscription
  - Crafting: Alchemy, Blacksmithy, Bowcraft, Craftsmanship, Cooking, Tinkering
  - Gathering: Mining, Lumberjacking, Fishing
  - Thief: Hiding, Stealth, Detecting Hidden, Snooping, Stealing, Poisoning
  - Taming: Animal Taming, Animal Lore, Veterinary
  - Other: Arms Lore

- **Stat System**
  - STR, DEX, INT calculated from related skills
  - STR: +HP, mining/lumber speed bonus
  - DEX: Attack speed, movement speed bonus
  - INT: Mana cost reduction, cast success bonus

- **Internal HP/Mana System**
  - Internal HP: 100 + STR (synced with vanilla hearts)
  - Internal Mana: Uses hunger bar

- **Skill Cap System**
  - Total skill cap: 700 points
  - Individual skill cap: 100 points
  - Least-used skill decreases when cap reached

- **UO-Style Skill Gain**
  - Gain chance decreases as skill increases
  - Difficulty affects gain rate
  - 0.1 point gain per successful check

- **Magic System**
  - 20+ spells across 8 circles
  - Reagent system using Minecraft items
  - Spellbook and scroll crafting
  - Rune marking and recall/gate travel

- **Crafting Quality System**
  - LQ/NQ/HQ/EX quality based on skill
  - Quality affects damage/defense and durability

- **Thief System**
  - Hiding and stealth movement
  - Snooping and stealing from players
  - Weapon poisoning

- **Taming System**
  - Tame wolves, cats, horses, etc.
  - Animal Lore to check mob stats
  - Veterinary to heal tamed animals

- **Internationalization**
  - English and Japanese language support
  - Client locale detection
  - `/language` command to change language

- **Scoreboard Display**
  - Show STR/DEX/INT with lock icons
  - Show skill title
  - Compatible with other plugins' team/BELOW_NAME displays
