# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.2] - 2026-11-22

Hotfix for 1.1.1.

### Fixed

- Creative inventory: the search found nothing. Some tooltips (hints of titanite and the titanium template, and the compare line while wearing gear) could only be built on the render thread, and the search builds its index on another thread. The search works again.
- Bags: opening or closing the worn bag moved the whole inventory window. It now keeps its place.
- Hotbar hiding: the HD level number stayed on screen while the experience bar slid away. It now moves with it.

### Added

- Tooltip hints: thread and the compressed materials (including the compressed phantom membrane) now say how they are made and what they are for, like the gems.

## [1.1.1] - 2026-11-22

### Fixed

- Titanium spear: the missing item models caused a missing texture (purple-black) in the inventory and in the hand.
- Gear sets: a set saved the off hand too, so it could hold two helmets. It now holds only the four worn armor pieces; old sets drop the off hand.
- Gear sets: equal pieces in the inventory got the set number too. A saved piece now gets its own id, so only the worn piece belongs to the set. Sets saved in 1.1.0 need to be saved once more.
- Gear sets: the message was hidden behind the inventory window and is now shown above it.
- Bags: a left click with an empty hand on the worn bag took it off instead of closing its rows.
- HD HUD: the lines of the experience bar were see-through and are now solid.

### Added

- Gear sets: Ctrl + click on a set button deletes the set. The set numbers have their own colors (1 yellow, 2 green, 3 red) on the buttons and on the items.

## [1.1.0] - 2026-11-21

**Updating from 1.0.1:** your settings, profiles and exported files are kept. Every new feature starts switched off. The new preset values only apply once you load a preset again, and loading one replaces your current values, so save them in a profile of your own first.

### Added

- Smithing system (each part with its own switch): compressed materials, upgrades +1 to +3 with forge hammers (shown as a prefix in the name), sockets with socket chisels, 6 gem types in 53 sorts and three cuts, gem recut, gem lever, master smithing plan, netherite repair kit and smithing table storage.
- Titanite: a very rare stone from titanium ore in three cuts that makes all other gems of a piece 8, 16 or 25 % stronger (adjustable), one per piece.
- The same sort and cut of aquamarine or onyx can only be set once per piece. Gem resistances show as their own tooltip lines.
- Break protection: valuable tools, weapons and armor stop at 1 durability, are grey and without effect, and are repaired at the smithing table with a forge hammer and the compressed material or with the repair kit. Wooden and stone tools, the wooden shield, shears, flint and steel and the fishing rod still break. Gems can be removed from worn-out pieces.
- Shields of wood, copper, iron, gold, diamond, netherite and titanium with their own textures, parry, explosion protection angle and adjustable values.
- Shield speed: better shields let you walk faster while blocking, from 20 % of normal speed (wood, as in vanilla) to 50 % (titanium). Adjustable per shield, applies after loading the world again.
- Tailoring with the sewing needle: padded armor (protects against freezing), bags from leather to titanium in their own slot, thread, leather patching and padded animal armor (half fall damage).
- Titanium: ore, fragments, ingots, smithing template from end city chests, tools, weapons, armor (4 sockets, less fall damage), shield, bag, forge hammer, socket chisel and gem lever. Values in all presets and HM+ profiles.
- Glow of full sockets: weapons, tools and shields with every socket filled glow in your hand with a 1 px outline, colored side faces and a soft glow. Spears get the outline only, shields a colored frame and a soft glow. Only shown to you.
- Flames or shadow instead of the glow on diamond, netherite and titanium tools and swords.
- New "Glow" tab with sliders for color, brightness, outline saturation, pulse, shield frame and shield glow, and a switch to hide the vanilla enchantment glint on glowing pieces.
- Comfort: sprint fix, tool refill, gear sets (also from the worn bag), a trash bin slot in the inventory and own length of day and night.
- View: tooltip frames in the color of the material, name color by upgrade level, compare with the worn piece by holding Shift, pickup display next to the hotbar, sparkle on new items, lower shield and fire in first person, hand sway, durability warning, hotbar hiding, own inventory size, stats panel and durability of every item. Socket symbols have a slight shadow.
- Loot reduction (overworld): no diamonds and golden apples in easy overworld chests.
- Horse armor recipes for copper, iron, gold and diamond.
- Multiplayer: the server sends all settings when a player joins, operators can change them in game.
- Death (each part with its own switch, decided by the server): drops and experience in one pile at the death point that never despawns or after 1 to 30 minutes, lava and void protection, an adjustable share of all experience points (default 80 %), 
  drops safe from explosions, keeping armor, hotbar and off hand with a small durability loss (default 10 %, never breaks), and a chat message with the coordinates.
- Spawners: mining one in survival gives iron ingots (default 2) and a chance of a chipped gem (default 25 %, with sockets on). With a netherite or titanium pickaxe and Silk Touch the spawner drops with its mob and spawns it again when placed.
- Armor bar: every armor point in the color of the piece it comes from, cracks at two adjustable durability limits, darker with wear, red blinking when low and a grey symbol for worn-out or just broken pieces.
- HD symbols, each with its own switch: armor bar, hearts (every kind, optional silver ring for hearts from gear), food bar (optional golden drumsticks for saturation), hotbar, selection frame, off hand slot, experience bar and level number in 4x resolution. With the HD hotbar the items are 25 % smaller.
- Hotbar animations: pop of the selected item, red pulsing slot at low durability, item name animation and an animated experience bar with sparks and a sound on level up.
- Smooth scrolling for lists and chat, and a gliding hotbar selection frame.
- Item tags of GearBalance have names, for example in recipe viewers.
- JEI support for the smithing recipes.
- Shield wear (optional, off by default): below a set durability a shield blocks less damage, falling evenly down to a set minimum just before it breaks. Works for every shield and applies at once. Settings on their own page in the options.
- HM+ profiles: a new "HM+ Profiles" button in the profiles screen with five ready-made profiles (Relaxed, Tougher, Veteran, Nightmare, Realism) that match the profiles of HardmodePlus. Level requirements are 10 % to 50 % above Progression, 
  while more monsters mean more durability, faster mobs lighter armor and armored undead stronger swords and spears (+0.5 up to +2 damage). Only one can be active at a time, the active one is shown in green.
- New base for every preset: the sword is the main weapon and gets stronger and faster with every material (2 to 12 damage, speed up to 1.8). Axes keep their vanilla damage and speed.
- Spears: all seven spears from wood to netherite are now managed in the weapons tab, with durability, damage and speed, level requirements by material (a locked spear deals no damage, neither with the jab nor with the charge) and values in every preset. They hit slowly but hard at about two thirds of the sword's damage per second (1.5 to 16.5 damage) 
  and keep their vanilla speed and reach. Gold and netherite spears last as long as the other gold and netherite tools. The HM+ profiles give spears the same extra damage as swords. The charge is not changed by the damage value.
- The base also gives netherite tools a mining bonus, chainmail and better armor knockback resistance, iron armor toughness, a longer lasting bow and shield, a shield that is disabled for only 1 second after an axe hit, stronger set bonuses (the copper set now gives attack speed) and the rabbit hide recipes. 
  Every material needs a higher level than the one before, with gold between iron and diamond.
- Interface for add-ons (`GearBalanceApi`): add-ons can add their own materials and gear, which then appear in every tab with level, repair, weight and set bonus, set their values for the presets and HM+ profiles, and raise the level an item needs. Without add-ons nothing changes. 
  Values saved for an add-on stay in the config and in profiles when it is removed for a while.

### Changed

- New menu layout: Functions, Gear and Options, single switches first. Set bonuses without a sub menu, shields in their own tab, titanium, comfort and glow under Options.
- The settings screen uses its own GUI size that takes about the same share of the screen on every resolution, from the main menu and in game alike.
- The gem tab shows each gem in its color with its symbol, and the cut columns with their shapes.
- Item icons in the settings also show from the main menu.
- The name of the held item is drawn a bit higher, so it doesn't cover the armor bar, and moves up with extra heart rows.
- Lowering shield and fire in first person is decided by the server in multiplayer, since it improves the view.
- Tooltips have more space at the bottom, and the settings screen is smaller.
- "Explosion angle" is now called "explosion protection angle".
- Knockback resistance can be set per piece.
- Presets and HM+ profiles also set the new functions: comfort in every preset, smithing, shields, bags, loot reduction and horse armor from Progression on. Challenge also weakens the GearBalance shields. Titanium, the view settings and the glow are left alone.
- Tooltips are wrapped into a compact block and always stay inside the window.
- Names and texts that are too long are drawn smaller instead of being cut off.
- The scrollbar of the settings screen can be dragged with the mouse.
- Every tab explains itself in its tooltip, and the reset button is as high as the row.
- The title of the settings screen shows when the mod is not active.
- Hovering a preset shows its full description.
- A profile slot that only has a name can be cleared.
- The mod description in Mod Menu is translated.
- Needs Fabric Loader 0.20.1 or newer.
- Release files are named with an underscore and the Minecraft version, for example `gearbalance_1.1.0-26.3.jar`.
- The README describes the new preset values and the HM+ profiles.

### Fixed

- "NaN" and "Infinity" were accepted as values in the settings screen.
- Level requirements still applied while "Mod active" was off.

## [1.0.1] - 2026-10-31

### Changed

- Official download pages are now CurseForge and GitHub: start note, Mod Menu homepage link and license updated.

## [1.0.0] - 2026-10-30

First release.

### Added

- Settings for durability, attack damage, attack speed, mining bonus, armor, toughness and knockback resistance of 75 vanilla items, copper gear included.
- Settings screen via Mod Menu with tabs, vanilla values and allowed ranges in the tooltips, and a reset per item.
- Level requirements per material for wearing armor and using tools and weapons.
- Armor weight and anvil repair amount per material, without the view zoom vanilla shows for speed changes.
- Optional set bonuses per armor material with 18 bonus kinds, half with 2 pieces and full with 4, shown in the armor tooltip.
- Shield settings for the axe disable time and the blocked damage.
- Optional anvil setting that replaces "Too Expensive!" with a cost cap.
- Optional recipes for the trident, chainmail armor, rabbit hides from leather and leather armor from rabbit hides.
- Optional leather drop for zombies, husks, drowned and zombie villagers with an adjustable chance.
- Optional endgame loot switches: more smithing templates, more valuable materials and cheap filler replaced by fitting loot in end cities, bastions, ancient cities and trial chamber vaults.
- Three presets (VanillaPlus, Progression, Challenge) that set up values, materials, set bonuses and switches in one click.
- Three profiles of your own to save and load complete setups.
- Export and import of setups as files in `config/gearbalance/profiles/`.
- Prompt to quit the game after changing values that need a restart.
- A short note on the very first start that the mod is free, and links to the official pages in Mod Menu.
- Broken config files are kept as `gearbalance.json.broken`.
- English and German translations.
