# GearBalance

A Fabric mod for Minecraft Java 26.3 that lets you shape gear and progression the way you want: the values of every vanilla tool, weapon and armor piece, level requirements, armor weight, set bonuses, the shield, endgame loot and a few recipes. It's part of a bigger project of mine, a set of mods that let every player adjust progression and experience to their own taste.

A fresh install changes nothing. Every value starts at vanilla, and only what you change is touched. Items you already own use the new values right away, and removing the mod brings vanilla back. If you don't want to set everything up yourself, three ready-made presets give you a rounded game with one click.

## Features

### Item values

75 items: all tools, weapons and armor from wood to netherite including copper, plus mace, trident, bow, crossbow, shield, shears, fishing rod, brush and wolf armor.

| Item group | Values |
| --- | --- |
| Tools (pickaxe, axe, shovel, hoe) | Durability, attack damage, attack speed, mining bonus |
| Weapons (sword, mace, trident) | Durability, attack damage, attack speed |
| Armor and wolf armor | Durability, armor, toughness, knockback resistance |
| Shield | Durability, how long an axe disables it, how much damage it blocks |
| Bow, crossbow and the rest | Durability |

Damage and attack speed are the totals you see in the tooltip. The mining bonus is added to the mining speed, a bit like Efficiency. Durability goes up to 10000.

This is mostly about the things in vanilla that never really made sense. Gold breaks after a few blocks, netherite is only a small step up from diamond, copper is easy to skip. With a few numbers every material gets a real place.

### Materials

Some values are set per material instead of per item.

| Value | What it does |
| --- | --- |
| Level | Until a player has reached this level once, armor of the material can't be worn, tools mine like the bare hand and weapons deal no damage to mobs. Spending levels or dying doesn't lock anything again. Creative and spectator are not affected. 0 = none |
| Repair | How much one ingot, diamond or plank repairs at the anvil, in percent. Vanilla 25 |
| Weight | How much slower a full set makes you, 0 to 90 % in steps of 0.1. Each worn piece carries a quarter. Leather has no weight. The view doesn't zoom in like it does with slowness. Vanilla 0 |

Level requirements make the early and mid game last longer: finding diamonds isn't enough, you also have to earn them. Weight gives every armor a character of its own, heavy iron against quick copper, with netherite as the lightest of the metals.

### Set bonuses

Optional, with a main switch in the options (off by default). For every armor material you pick a bonus and its strength. Two worn pieces give half the bonus, all four the full one. Pieces of different materials count for their own sets, so two half sets work at the same time.

18 bonus kinds: movement speed, less fall damage, sneaking speed, jump strength, armor, toughness, knockback resistance, blast knockback resistance, hearts, attack damage, attack speed, hit knockback, reach, mining speed, breathing, swimming speed, shorter burning and luck.

The armor tooltip shows the set, how many pieces you wear and the bonus for 2 and for 4 pieces: green when it's active, grey when it isn't. The speed bonus doesn't zoom the view either.

### Endgame loot

Optional, three switches for the chests in end cities and end ships, bastions, ancient cities and the trial chamber vaults. The supply chests of trial chambers stay as they are.

| Switch | What it does |
| --- | --- |
| More smithing templates | Extra chance for netherite upgrade and snout in bastions, spire in end cities, ward and silence in ancient cities, bolt and flow in vaults |
| More valuable materials | One or two extra valuable stacks per chest, fitting the place |
| Replace cheap loot | Bones, string, coal, arrows, bread, chains, nuggets and similar filler become valuable loot. In the End, iron is replaced too |

What counts as valuable depends on the place:

| Place | Valuable loot |
| --- | --- |
| Bastion | Gold ingots, golden carrots and apples, gold blocks, rarely netherite scrap, ancient debris or Pigstep |
| End city and end ship | Gold, emeralds, shulker shells, diamonds, ender pearls, potions of slow falling, healing, regeneration and strength, rarely netherite scrap |
| Ancient city | Echo shards, amethyst, lapis, experience bottles, rarely diamonds |
| Trial vaults | Emeralds, gold, golden carrots, experience; ominous vaults also netherite scrap and enchanted golden apples |

Books are never added, removed or swapped, so mods that control where enchanted books come from keep working.

### Other options

All off by default:

- **Anvil without "Too Expensive!"**: the cost is capped instead, 1 to 120 levels, default 39.
- **Trident recipe**: prismarine crystals, a nautilus shell and sticks.
- **Chainmail recipes**: chainmail armor from iron chains.
- **Leather to rabbit hides**: 1 leather makes 4 rabbit hides.
- **Leather armor from rabbit hide**: leather armor is crafted from rabbit hides instead of leather.
- **Zombies drop leather**: zombies, husks, drowned and zombie villagers can drop 1 leather, chance 1 to 100 %, default 33 %.

### Presets and profiles

Three presets set up everything at once: values, materials, set bonuses and switches.

| Preset | Idea |
| --- | --- |
| VanillaPlus | Vanilla without its weak spots. Gold tools last six times as long and gold armor three times, leather twice, netherite gets more durability, +1 damage and +1 toughness. Trident and chainmail recipes, no "Too Expensive!", zombie leather and all loot switches. No level requirements |
| Progression | VanillaPlus plus level requirements (copper 5, chainmail and gold 10, iron 15, diamond 25, netherite 35), light armor weight and set bonuses for every material |
| Challenge | A quarter less durability, anvil repairs only 15 %, level requirements up to 50, more weight, weaker set bonuses, and a shield that is disabled for 7 seconds by axes and blocks only 80 % |

Below them are three profiles of your own to save and load complete setups, including all switches. Loading a preset or a profile asks first, because anything you haven't saved is replaced.

**Export and import:** "Export" saves the current setup as a file in `config/gearbalance/profiles/`. Share it with friends or put it into a modpack. "Import..." lists the files in that folder, so a file you got from someone else just needs to be dropped in there. From there you can also delete files and open the folder in your file explorer.

Everything that would lose data asks first: loading, overwriting or clearing a profile, deleting a file and "Reset Vanilla".

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) you get a settings screen. You can also edit `config/gearbalance.json` yourself, it only contains what you changed.

- Tabs for tools, weapons, armor, other items, materials and set bonuses, with icons for every item and material.
- White = vanilla, yellow = changed but not saved yet, green = changed and saved, red = invalid input.
- Hover a field for the vanilla value and the allowed range, hover a column title for an explanation.
- The arrow at the end of a row resets it. "Reset Vanilla" resets everything after asking, your profiles are kept.
- The options hold "Mod active" (back to vanilla without losing your settings), the set bonus switch, the loot settings, the recipes and the anvil.

Item values, armor weight and the shield apply after a restart. When you close the screen after such a change, the game offers to quit, then you just start it again. Everything else applies at once, recipes after `/reload` or when a world is loaded.

If the config file ever gets broken, the mod starts with vanilla values and keeps the old file as `gearbalance.json.broken`, so nothing is lost.

On the very first start a small note in the corner says that the mod is free and where the official download is. Mod Menu also shows buttons to the official pages.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings screen

The mod must be installed on the client and on the server.

## Multiplayer

Use the same `config/gearbalance.json` on the server and on every client. Damage, durability, mining, level requirements, set bonuses and loot are decided by the server, so a different client file only leads to wrong tooltips and durability bars on that client.

## Other mods

Only vanilla items and vanilla chests are changed, gear and loot from other mods stay as they are. With EnchantPlus installed, its books keep appearing in the endgame chests, and the extra lapis and experience bottles help with its enchanting costs. GearBalance also works well with the upcoming HardmodePlus, but needs nothing else.

## Translations

Currently English and German. You can add another language with your own resource pack (`assets/gearbalance/lang/<code>.json`, based on `en_us.json`).

If you would like your translation to be part of the mod, open an issue or a pull request on GitHub, and I will add it.

## Modpacks

You may use GearBalance in modpacks. Please link to the official download (Modrinth or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE)
