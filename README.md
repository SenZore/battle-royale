# Paper 1.21.x Mini-Game Plugin Full Prompt for ChatGPT Codex

## Overview
Create a Minecraft Paper 1.21.x plugin using Maven with the following features:

- Single server instance that auto-generates a new small world each game round.
- Worlds have borders smaller than Hoplite's, shrinking over time to limit chunk loading and save CPU.
- Maximum overworld game duration is 20 minutes.
- Nether shares the shrinking border logic; The End is disabled.
- Minimum players required to start is configurable via `config.yml` (default 3-5).
- Players joining before game start are auto-ready and wait.
- Once players exceed minimum (e.g., 5+), a new world is generated and a countdown begins.
- Players joining *while game is starting or in-progress* become spectators.
- No dedicated PvP arena; PvP is free with kits.
- Every 1-5 minutes (configurable interval), each player receives 1-3 random items individually selected from a loot pool made of multiple PvP kits.
- Kits include swords, maces, UHC gear, axe & shield, sniper kit, fairy (elytra), and special mace + elytra kits.
- Items stack enchantments randomly from a predefined pool (no Blast Protection).
- On death, players switch to spectator mode; no stats or data are saved after rounds.
- Player kills broadcast a global custom death message and summon a lightning bolt (cosmetic, no damage).
- When a round ends, all players teleport to new world spawn (X=0, Y=0, Z=0) and old world is deleted.
- Scoreboard displays player count, time remaining, border size, loot drop countdown, kills, and other status info.
- Optimize performance through efficient chunk management and Paper API usages.

## Configuration (`config.yml`)
- Minimum players to start (`minPlayers`)
- Game time limit in seconds (`maxGameTime`)
- Border initial size and shrinking speed (`borderSize`, `borderShrinkSpeed`)
- Loot drop interval seconds minimum and maximum (`minLootIntervalSeconds`, `maxLootIntervalSeconds`)
- Number of items per drop (`minItemsPerDrop`, `maxItemsPerDrop`)
- Enable/disable kits in loot pool (e.g. `enableSwordKit`, `enableMaceKit`, etc.)
- Toggle random enchantments (`randomEnchantments`)
- Rarity weights for loot (`rarityWeights`) to balance common vs rare drops

## Detailed Loot Pool and Enchantments
- Each loot drop gives random items picked individually from the following pool:

### Sword Kit
- Diamond/Netherite Sword
- Enchants: Sharpness III-IV, Smite III-IV, Unbreaking III, Fire Aspect II, Looting III, Mending
- Armor (Diamond/Netherite) with Protection III-IV (no Blast Protection), Feather Falling IV (boots), Unbreaking III, Mending
- Items: Golden Apples, Strength, and Swiftness Potions

### Mace Kit
- Diamond/Netherite Mace (custom item)
- Enchants: Sharpness IV, Unbreaking III, Knockback I-II, Fire Aspect I, Mending
- Custom: Density V, Wind Burst III
- Armor: Similar to Sword kit, max Protection III
- Consumables: Ender Pearls, Healing Splash Potions

### UHC Kit
- Diamond Sword with Sharpness II-III, Unbreaking III
- Full Diamond armor Protection III
- Bow with Power II, Infinity, Flame; few arrows
- Items: Lava & Water Buckets, Regeneration II & Healing Potions, Golden Apples

### Axe & Shield Kit
- Diamond/Netherite Axe with Sharpness III, Unbreaking III, Sweeping Edge III
- Shield with Unbreaking III
- Consumables: Bread, Strength Potions, Fire Charges

### Sniper Kit
- Bow with Power IV, Infinity, Flame, Unbreaking III
- Sword with Sharpness III, Unbreaking III
- Leather or Diamond armor Protection III
- Consumables: Arrows, Golden Apples

### Fairy Kit
- Elytra with Unbreaking III, Mending
- Fireworks Rocket (Duration 3)
- Diamond Sword Sharpness III, Fire Aspect I, Unbreaking III
- Diamond Armor Protection III
- Potions of Swiftness

### Special Mace + Elytra Kit
- Elytra Unbreaking III, Mending
- Fireworks Rocket (Duration 3)
- Mace enchant Density V, Wind Burst III
- Diamond/Netherite Armor Protection III
- Potion of Strength

## Loot Drop Mechanics (Configurable)
- Loot drop intervals random between `minLootIntervalSeconds` and `maxLootIntervalSeconds`
- Each player receives `minItemsPerDrop` to `maxItemsPerDrop` items per drop cycle
- Kits and enchantments can be toggled on/off via config
- Rarity weighting controls chance of rare vs common item drops

## Events & Visuals
- Countdown, game start and PvP toggle via Minecraft titles and music
- Lightning bolt effect on player kills (cosmetic, no damage)
- Global kill announcements in chat with killer & victim names

## Deliverables
- Full Maven project structure for Paper 1.21.x plugin
- Well-commented example Java classes showcasing:
  - World generation/deletion with border shrinking
  - Random timed individualized loot drops
  - Spectator mode with late join handling
  - Scoreboard display and updates
  - Event schedule and audio-visual feedback
- Practices optimized for server performance on Paper 1.21.x

---

Use this prompt as a thorough guide for ChatGPT Codex to generate the entire plugin codebase and configuration needed to build this mini-game plugin with random loot drops and dynamic game management.
