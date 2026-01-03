# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.9] - 2026-01-03

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
