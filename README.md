# GearBalance

A Fabric mod for Minecraft Java 26.3 that lets you shape gear and progression the way you want: the values of every vanilla tool, weapon and armor piece, level requirements, armor weight, set bonuses, loot and recipes. On top of that comes a whole smithing system with upgrades, sockets and gems, new shields, tailoring with padded armor and bags, a new endgame material, a glow for fully socketed gear and a set of comfort and view features. It's part of a bigger project of mine, a set of mods that let every player adjust progression and experience to their own taste.

A fresh install changes no gameplay. Every value starts at vanilla, every feature starts switched off, and only what you change is touched. Items you already own use the new values right away, and removing the mod brings vanilla back. If you don't want to set everything up yourself, three ready-made presets and five HM+ profiles give you a rounded game with one click.

## Features

### Item values

82 items: all tools, weapons and armor from wood to netherite including copper and the seven spears, plus mace, trident, bow, crossbow, shield, shears, fishing rod, brush and wolf armor. With titanium switched on, the titanium gear joins them. Gear from add-ons is added to the same tabs.

| Item group | Values |
| --- | --- |
| Tools (pickaxe, axe, shovel, hoe) | Durability, attack damage, attack speed, mining bonus |
| Weapons (sword, spear, mace, trident) | Durability, attack damage, attack speed |
| Armor and wolf armor | Durability, armor, toughness, knockback resistance |
| Shield | Durability, how long an axe disables it, how much damage it blocks |
| Bow, crossbow and the rest | Durability |

Damage and attack speed are the totals you see in the tooltip. The mining bonus is added to the mining speed, a bit like Efficiency. Durability goes up to 10000.

This is mostly about the things in vanilla that never really made sense. Gold breaks after a few blocks, netherite is only a small step up from diamond, copper is easy to skip. With a few numbers every material gets a real place.

### Materials

Some values are set per material instead of per item.

| Value | What it does |
| --- | --- |
| Level | Until a player has reached this level once, armor of the material can't be worn, tools mine like the bare hand and weapons deal no damage to mobs (spears neither with the jab nor with the charge). Spending levels or dying doesn't lock anything again. Creative and spectator are not affected, and nothing is locked while "Mod active" is off. 0 = none |
| Repair | How much one ingot, diamond or plank repairs at the anvil, in percent. Vanilla 25 |
| Weight | How much slower a full set makes you, 0 to 90 % in steps of 0.1. Each worn piece carries a quarter. Leather has no weight. The view doesn't zoom in like it does with slowness. Vanilla 0 |

Level requirements make the early and mid game last longer: finding diamonds isn't enough, you also have to earn them. Weight gives every armor a character of its own, heavy iron against quick copper, with netherite as the lightest of the metals.

### Set bonuses

Optional, with a main switch in the options. For every armor material you pick a bonus and its strength. Two worn pieces give half the bonus, all four the full one. Pieces of different materials count for their own sets, so two half sets work at the same time.

18 bonus kinds: movement speed, less fall damage, sneaking speed, jump strength, armor, toughness, knockback resistance, blast knockback resistance, hearts, attack damage, attack speed, hit knockback, reach, mining speed, breathing, swimming speed, shorter burning and luck.

The armor tooltip shows the set, how many pieces you wear and the bonus for 2 and for 4 pieces: green when it's active, grey when it isn't. The speed bonus doesn't zoom the view either.

### Smithing: upgrades, sockets and gems

Each part has its own switch, all off by default. Everything happens at the smithing table.

- **Compressed materials and upgrades:** 8 ingots, leather or iron chains in a ring make a compressed material (diamonds and netherite scraps with a blaze rod in the middle). A forge hammer and a compressed material raise a piece to +1, +2 and +3, each level a few percent better. The level shows as a prefix in the name. Mace (breeze rod), trident (prismarine crystals), bow and crossbow (compressed iron) need a diamond or netherite hammer. Bow and crossbow get more arrow damage and draw faster, the trident also hits harder when thrown.
- **Sockets:** a socket chisel and amethyst shards open a socket. How many sockets a material can have and what they cost is adjustable. Titanium gets 4, the 4th socket costs 25 amethyst shards.
- **Gems:** ruby, sapphire, topaz, emerald, aquamarine and onyx in 53 sorts and three cuts (chipped, cut, flawless). Ruby, sapphire and topaz drop from redstone, lapis and gold ore and lie in structure chests, aquamarine comes from diamond ore and onyx from ancient debris. The stonecutter cuts them (you pick the sort at the first cut), and three of one cut make the next. Every sort has a fixed bonus and fits either armor and shields or weapons and tools. The same sort and cut of aquamarine or onyx can only be set once per piece. The optional recut turns a gem 1:1 into another sort of the same gem.
- **Titanite:** a very rare stone from titanium ore, in three cuts. In a socket it makes every other gem of the piece 8, 16 or 25 % stronger (adjustable), one per piece. It only drops when the ore is mined without Silk Touch.
- **Gem lever:** takes the last gem out of a piece whole. At the grindstone you choose the gems to remove; they are lost, but with an optional chance one cut lower comes back.
- **Master smithing plan:** an optional extra ingredient for the diamond and better smithing tools, found in chests of hard places and dropped by wither skeletons and piglin brutes, copied at the crafting table.
- **Netherite repair kit:** fully repairs a piece without anvil cost, used in the inventory like a bundle.
- **Smithing table storage:** items stay in the smithing table when it is closed.
- **Break protection:** valuable pieces (enchanted, upgraded or with gems) stop at 1 durability instead of breaking. A damaged piece is shown grey and has no effect until it is repaired at the smithing table with a forge hammer and the compressed material (elytra: compressed phantom membrane) or with the repair kit. Wooden and stone tools, wooden shields, shears, flint and steel and the fishing rod still break as usual. Gems can be taken out of a damaged piece.

### Tailoring

Each part has its own switch. Everything needs the sewing needle (iron nugget above string).

- **Padded armor:** the needle joins a copper, iron, gold, diamond or netherite armor piece with the matching leather piece. Padded armor protects against freezing.
- **Bags:** from leather to titanium, worn in their own slot next to the player. A worn bag adds rows above the inventory (rows per bag adjustable). Left click on the worn bag opens or closes its rows, right click takes it off, the tooltip shows how full it is. Bags are upgraded at the smithing table and keep their content. On death the bag drops with everything in it.
- **Thread:** 1 wool makes 4 thread.
- **Leather patching:** a damaged leather piece, leather, thread and the needle give back 25 % durability.
- **Animal armor padding:** horse or wolf armor, wool, thread and the needle make padded armor. A horse or wolf wearing it takes only half the fall damage.

### Shields

Optional shields of wood, copper, iron, gold, diamond, netherite and titanium. The wooden shield replaces the vanilla recipe, vanilla shields you already have keep working. Better material means more durability, blocking sooner, a shorter lock after an axe hit, a wider explosion protection angle and a stronger parry. Every value can be set per shield. The tooltip shows parry window, blocked damage, blocking angle and durability.

**Shield speed:** better shields let you walk faster while blocking: wood 20 % of normal speed as in vanilla, copper 25 %, gold 30 %, iron 35 %, diamond 40 %, netherite 45 %, titanium 50 %. Own switch, every value can be set per shield, applies after loading the world again.

**Parry:** blocking a melee hit right when the shield is ready pushes the attacker away, and your next hit within 2 seconds deals 30 % more damage. Better shields have a longer window (wood 0.15 s to netherite 0.35 s).

### Titanium

Optional new material after netherite. The ore is rare in deepslate below Y 0 and only a netherite pickaxe mines it. The blast furnace makes fragments, 4 fragments and a netherite ingot make an ingot, and a titanium smithing template from end city chests turns netherite gear into titanium gear: about 15 % better than netherite, fire resistant, and every worn armor piece lowers fall damage (10 % per piece, adjustable). There are also a titanium shield, bag, forge hammer, socket chisel and gem lever, and titanium armor has 4 sockets. Switching titanium off keeps the world safe: items, blocks and ore that already exist stay, only new ore, the recipes, the template and the fall damage bonus stop.

### Glow of full sockets

When every socket of a weapon, tool or shield holds a gem, the piece glows in your hand: a 1 px outline with colored side faces and a soft glow around it. Spears get the outline only, shields a colored frame and a soft glow around the plate. The glow is only shown to you.

The "Glow" tab sets it up:

- **Glow & effects:** switch, brightness, and flames or shadow instead of the glow on diamond, netherite and titanium tools and swords (all other pieces keep the glow). "Hide enchantment glint" removes the vanilla glint from these pieces so the glow shows clearly.
- **Outline:** saturation of the outline and its side faces.
- **Color:** red, green and blue as sliders, one color for everything.
- **Pulse:** on or off, strength and duration.
- **Shield:** brightness of the frame and strength of the glow (shields don't pulse).

All textures of the glow and the effects are made for this mod.

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

### Comfort

- **Sprint fix:** letting go of the sprint key stops sprinting at once (sprint mode "Hold").
- **Tool refill:** a broken tool or weapon is replaced from the inventory.
- **Gear sets:** three buttons left of the inventory save and put on armor and off hand, also from the worn bag.
- **Trash bin:** a slot in the top right of the inventory. The last item put in stays there and can be taken out again, putting in a new one deletes the old one.
- **Day and night length:** own length of day and night in real minutes (1 to 120 each), for all players on the server.
- **Death:** drops and experience land in one pile at the death point (no despawn, or after 1 to 30 minutes), dying in lava or the void puts the pile on the last safe block, an adjustable share of all experience points drops (default 80 %), explosions don't destroy the drops, armor, hotbar and off hand can be kept with a small durability loss (default 10 %, never breaks), and a chat message tells where the pile lies. Every part has its own switch, the server decides.
- **Spawners:** mining a spawner in survival gives iron ingots (default 2) and a chance of a chipped gem (default 25 %, with sockets on). With a netherite or titanium pickaxe and Silk Touch the spawner itself drops with its mob and spawns it again when placed.

### View

All of these are only for you. Only lowering shield and fire is set by the server in multiplayer.

- **Tooltips:** frames in the color of the material, name color by upgrade level (gold with all sockets filled), comparison with the worn piece by holding Shift, durability of every item and the uses left of the smithing tools.
- **Pickups:** picked up items appear as tiles next to the hotbar, new items sparkle in the inventory until you hover them.
- **Screen:** lower shield and fire in first person, a slight hand sway while standing, and a durability warning sound.
- **Hotbar hiding:** the hotbar slides away after a few seconds without changing the slot and comes back on damage or when you are hungry. Optionally armor, experience or everything go with it.
- **Inventory size:** own GUI size while the inventory is open, so the bag rows and the stats panel fit.
- **Stats panel** with all bonuses of the worn gear.
- **Armor bar:** every armor point in the color of the piece it comes from, cracks at two adjustable durability limits, darker with wear, red blinking when low and a grey symbol for worn-out or just broken pieces.
- **HD symbols:** armor bar, hearts (optional silver ring for hearts from gear), food bar (optional golden drumsticks for saturation), hotbar, experience bar and level number in 4x resolution, each with its own switch.
- **Hotbar animations:** pop of the selected item, red pulsing slot at low durability, item name animation and an animated experience bar with sparks and a sound on level up.
- **Smooth scrolling** for lists and chat, and a gliding hotbar selection frame.

### Other options

All off by default:

- **Anvil without "Too Expensive!"**: the cost is capped instead, 1 to 120 levels, default 39.
- **Trident recipe**: prismarine crystals, a nautilus shell and sticks.
- **Chainmail recipes**: chainmail armor from iron chains.
- **Leather to rabbit hides**: 1 leather makes 4 rabbit hides.
- **Leather armor from rabbit hide**: leather armor is crafted from rabbit hides instead of leather.
- **Horse armor recipes**: copper, iron, gold and diamond horse armor at the crafting table, shaped like leather horse armor.
- **Loot reduction (overworld)**: no diamonds and golden apples in the chests of mineshafts, temples, dungeons, strongholds, villages, shipwrecks and the other easy overworld places. The Nether, the End, ancient cities and trial chambers keep them.
- **Zombies drop leather**: zombies, husks, drowned and zombie villagers can drop 1 leather, chance 1 to 100 %, default 33 %.
- **Shield wear**: a worn shield blocks less. Below a set durability (default 50 %) the blocked damage falls evenly down to a set minimum just before it breaks (default 60 %). With the defaults a shield at 25 % durability still blocks 80 %. Works for every shield, also from other mods.

### Presets and profiles

Three presets set up everything at once: values, materials, set bonuses and switches. Every preset switches on the comfort functions, and from Progression on also smithing, shields, bags, the overworld loot reduction and the horse armor recipes. Titanium and the view settings stay as you set them.

| Preset | Idea |
| --- | --- |
| VanillaPlus | Vanilla without its weak spots. The sword is the main weapon and gets stronger and faster with every material (2 to 12 damage, speed up to 1.8). Spears hit slowly but hard at about two thirds of the sword's damage per second (1.5 to 16.5 damage) and keep their vanilla speed and reach, axes keep their vanilla damage and speed. Gold tools last six times as long and gold armor three times, leather twice. Netherite gets more durability, +1 toughness and a mining bonus. More knockback resistance for chainmail and better armor, toughness for iron armor, a longer lasting bow and shield, and a shield that axes disable for only 1 second. Trident, chainmail and rabbit hide recipes, no "Too Expensive!", zombie leather and all loot switches. No level requirements, weight or set bonuses |
| Progression | VanillaPlus plus level requirements (copper 5, chainmail 10, iron 15, gold 20, diamond 25, netherite 35), light armor weight and set bonuses for every material |
| Challenge | Progression with a quarter less durability, anvil repairs only 15 %, level requirements up to 50, more weight, set bonuses half as strong, a vanilla shield that is disabled for 7 seconds by axes and blocks only 80 %, and GearBalance shields with a quarter less durability that axes disable twice as long |

**HM+ profiles:** the "HM+ Profiles" button in the profiles screen opens five profiles with the same names as the profiles of HardmodePlus. Level requirements are always above Progression and rise with the profile. In return the gear helps against the harder mobs. Only one can be active at a time, the active one is shown in green.

| HM+ profile | Idea |
| --- | --- |
| Relaxed | Same as VanillaPlus |
| Tougher | Progression with 10 % more durability, level requirements up to 39 and +0.5 damage for swords and spears |
| Veteran | 25 % more durability, level requirements up to 42, lighter armor, +1 damage for swords and spears, the leather set gives 12 % speed |
| Nightmare | 40 % more durability, level requirements up to 46, even lighter armor, +1.5 damage for swords and spears, anvil repairs at least 30 %, stronger copper and iron sets |
| Realism | 50 % more durability, level requirements up to 53, heavy armor, +2 damage for swords and spears and shield wear |

Below them are three profiles of your own to save and load complete setups, including all switches. Loading a preset or a profile asks first, because anything you haven't saved is replaced.

**Export and import:** "Export" saves the current setup as a file in `config/gearbalance/profiles/`. Share it with friends or put it into a modpack. "Import..." lists the files in that folder, so a file you got from someone else just needs to be dropped in there. From there you can also delete files and open the folder in your file explorer.

Everything that would lose data asks first: loading, overwriting or clearing a profile, deleting a file and "Reset everything".

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) you get a settings screen. You can also edit `config/gearbalance.json` yourself, it only contains what you changed.

- Main tabs for functions, gear and settings, with sub tabs (for example general, view and glow). Single switches come first, sections that open with an arrow after them.
- While the settings are open, they use their own GUI size that takes about the same share of the screen on every resolution, from the main menu and in game alike.
- White = vanilla, yellow = changed but not saved yet, green = changed and saved, red = invalid input.
- Hover a field for the default and the allowed range, hover a column title or a tab for an explanation. Tooltips always stay inside the window.
- Scroll with the mouse wheel or drag the scrollbar.
- The arrow at the end of a row resets it. "Reset everything" resets everything after asking, your profiles are kept.
- "Mod active" switches everything back to vanilla without losing your settings.

Item values, armor weight, "Mod active" and some shield values apply after a restart. When you close the screen after such a change, the game offers to quit, then you just start it again, or you choose "Later". Recipes apply when the world is loaded again (the game offers to do that), everything else applies at once.

If the config file ever gets broken, the mod starts with vanilla values and keeps the old file as `gearbalance.json.broken`, so nothing is lost.

On the very first start a small note in the corner says that the mod is free and where the official download is. Mod Menu also shows buttons to the official pages.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings screen

The mod must be installed on the client and on the server.

## Multiplayer

The server sends all its settings to every player when they join. The client only keeps them in memory, its own `config/gearbalance.json` stays untouched, and the settings screen shows them greyed out. Operators can change them from the settings screen. In singleplayer and as LAN host nothing is locked. The view settings and the glow are always your own, except lowering shield and fire: they improve the view, so the server decides them.

## Other mods

Only vanilla items and vanilla chests are changed, gear and loot from other mods stay as they are. With JEI installed, the smithing recipes show up there and JEI keeps out of the way of the stats panel and the gear set buttons. With EnchantPlus installed, its books keep appearing in the endgame chests, and the extra lapis and experience bottles help with its enchanting costs. GearBalance also works well with HardmodePlus: the hotbar hiding moves its bars along, and the HM+ profiles match its profiles. It needs no other mod.

**Add-ons:** other mods can bring their own materials and gear. They show up in every tab like the vanilla ones, with level, repair, weight and set bonus, and the presets and HM+ profiles set their values too. Without add-ons nothing changes. If you remove an add-on for a while, its saved values stay in the config and in your profiles.

For mod authors: add-ons register materials, items and preset values with `io.github.schluid.gearbalance.api.GearBalanceApi` in a `gearbalance` entrypoint (`GearBalanceAddon`), which runs before GearBalance reads its config. The interface also tells whether GearBalance is active and which level a material needs, and lets add-ons raise the level an item needs. Tool, weapon and armor values need no interface: GearBalance writes them into the items at start, so they can be read from the item itself.

## Translations

Currently English and German. You can add another language with your own resource pack (`assets/gearbalance/lang/<code>.json`, based on `en_us.json`).

If you would like your translation to be part of the mod, open an issue on GitHub, and I will add it.

## Modpacks

You may use GearBalance in modpacks. Please link to the official download (CurseForge or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## Inspiration

Some ideas for the hotbar animations come from *Immersive Hotbar* by derpderpling, and some ideas for the death features from *Simple Death Improvements* by pajic. No code was taken from either mod.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE).
