# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
