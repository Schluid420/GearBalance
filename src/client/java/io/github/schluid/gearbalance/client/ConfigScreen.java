package io.github.schluid.gearbalance.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.config.GearConfig;

import io.github.schluid.gearbalance.config.VanillaValues;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.GemSorts;
import io.github.schluid.gearbalance.socket.GemValues;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.GearItems.Kind;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The GearBalance settings screen. Main tabs: Functions (On/Off switches), Gear (values), Options.
 * Below: sub tabs and a table with a reset button per row.
 * White = default, yellow = changed, green = saved, red = not allowed.
 * "Done" saves to config/gearbalance.json, then asks for a restart (gear values) or world reload (recipes).
 */
public class ConfigScreen extends TipScreen {
	// ---------------------------------------------------------------- tabs

	private enum Main {
		FUNCTIONS, GEAR, OPTIONS;

		String key() {
			return "gearbalance.menu." + name().toLowerCase(Locale.ROOT);
		}
	}

	private enum Sub {
		SMITHING(Main.FUNCTIONS, "gearbalance.menu.smithing", null),
		TAILORING(Main.FUNCTIONS, "gearbalance.menu.tailoring", null),
		SOCKETS(Main.FUNCTIONS, "gearbalance.menu.sockets", null),
		GEMS(Main.FUNCTIONS, "gearbalance.menu.gems", null),
		SHIELDS(Main.FUNCTIONS, "gearbalance.menu.shields", null),
		SET_BONUSES(Main.FUNCTIONS, "gearbalance.menu.set_bonuses", null),
		LOOT(Main.FUNCTIONS, "gearbalance.menu.loot", null),
		RECIPES(Main.FUNCTIONS, "gearbalance.menu.recipes", null),
		TOOL(Main.GEAR, "gearbalance.tab.tool", Kind.TOOL),
		WEAPON(Main.GEAR, "gearbalance.tab.weapon", Kind.WEAPON),
		ARMOR(Main.GEAR, "gearbalance.tab.armor", Kind.ARMOR),
		OTHER(Main.GEAR, "gearbalance.tab.other", Kind.OTHER),
		MATERIALS(Main.GEAR, "gearbalance.tab.materials", null),
		GENERAL(Main.OPTIONS, "gearbalance.menu.general", null),
		VIEW(Main.OPTIONS, "gearbalance.menu.view", null),
		GLOW(Main.OPTIONS, "gearbalance.menu.glow", null),
		COMFORT(Main.OPTIONS, "gearbalance.menu.comfort", null),
		TITANIUM(Main.OPTIONS, "gearbalance.menu.titanium", null);

		final Main main;
		final String key;
		final Kind kind;

		Sub(Main main, String key, Kind kind) {
			this.main = main;
			this.key = key;
			this.kind = kind;
		}

		/** Gear tables start in the first value column, the switch tables leave room for long names. */
		int firstColumn() {
			return main == Main.GEAR ? 0 : FIRST_COLUMN;
		}

		static List<Sub> of(Main main) {
			List<Sub> result = new ArrayList<>();
			for (Sub sub : values()) {
				if (sub.main == main) {
					result.add(sub);
				}
			}
			return result;
		}
	}

	// ---------------------------------------------------------------- layout

	private static final int MAIN_TAB_Y = 22;
	private static final int SUB_TAB_Y = 44;
	private static final int HEADER_Y = 72;
	private static final int TABLE_TOP = 84;
	private static final int ROW_HEIGHT = 22;
	private static final int BOTTOM_SPACE = 50;
	private static final int ICON_WIDTH = 20;
	private static final int NAME_WIDTH = 112;
	private static final int COLUMN_WIDTH = 54;
	private static final int BOX_WIDTH = 48;
	private static final int MAX_COLUMNS = 5;
	/** Sliders of the glow tab span three value columns. */
	private static final int SLIDER_WIDTH = 3 * COLUMN_WIDTH - 6;
	private static final int FIRST_COLUMN = 2;
	private static final int RESET_WIDTH = 26;
	private static final int TABLE_WIDTH = ICON_WIDTH + NAME_WIDTH + MAX_COLUMNS * COLUMN_WIDTH + RESET_WIDTH + 4;
	private static final int INDENT = 12;
	private static final int BAR_LEFT = 3;
	private static final int BAR_WIDTH = 4;
	private static final int BAR_GRAB = 10;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int DARK_GREY = 0xFF606060;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFE066;
	private static final int GREEN = 0xFF66E066;
	private static final int ROW_DARK = 0x30000000;
	private static final int ROW_LIGHT = 0x18FFFFFF;
	private static final int HEADING = 0x50000000;
	private static final int SHADOW = 0xFF3F3F3F;
	private static final int LOCKED = 0xFF707070;

	/** The arrow glyph of the font is tiny, so the reset icon is drawn by hand. */
	private static final String[] RESET_ICON = {
			"..###.#",
			".#...##",
			"#...###",
			"#......",
			"#.....#",
			".#...#.",
			"..###.."};
	private static final int RESET_PIXEL = 2;

	// ---------------------------------------------------------------- table model

	/** A number box. "gear" boxes show the vanilla value, "changed" only with an own value; step 0 = any, 1 = whole, 0.1 = one decimal. */
	private record Cell(String id, DoubleSupplier current, double standard, double min, double max, double step,
			DoubleConsumer setter, BooleanSupplier changed, String unit, String headerKey, Component hint, boolean gear,
			boolean slider) {
		void reset() {
			setter.accept(standard);
		}
	}

	/** An On/Off switch; nameKey is the translation key of its name, with ".tip" its explanation. */
	private record Toggle(String nameKey, BooleanSupplier current, BooleanConsumer setter, boolean standard) {
		boolean changed() {
			return current.getAsBoolean() != standard;
		}

		void reset() {
			setter.accept(standard);
		}
	}

	/**
	 * A table row. A heading with children opens/closes on click. "needs" = switch the row depends on (greyed out when off),
	 * "label" = grey word in a switchless heading, "reset" replaces the normal reset, "setMaterial" = set bonus row.
	 */
	private record Row(String icon, String icon2, Component name, Component hint, List<Cell> cells, Toggle toggle,
			String section, int indent, Toggle needs, List<Row> children, Component label, Runnable reset,
			Material setMaterial) {
		boolean heading() {
			return section != null;
		}

		boolean collapsible() {
			return !children.isEmpty();
		}
	}

	// ---------------------------------------------------------------- state

	/** Kept while the game runs, so the screen opens where it was left. Sections start closed. */
	private static Main main = Main.FUNCTIONS;
	private static final Map<Main, Sub> SUB_OF = new EnumMap<>(Main.class);
	private static final Map<Sub, Integer> SCROLL = new EnumMap<>(Sub.class);
	private static final Set<String> OPEN = new HashSet<>();

	static {
		SUB_OF.put(Main.FUNCTIONS, Sub.SMITHING);
		SUB_OF.put(Main.GEAR, Sub.TOOL);
		SUB_OF.put(Main.OPTIONS, Sub.GENERAL);
	}

	/** Values as they are in the file, to show saved changes in green. Built again after every save. */
	private final Map<String, Double> saved = new HashMap<>();
	private int savedAt = -1;
	private final Screen parent;
	private final String gearAtOpen;
	private final String recipesAtOpen;
	/** The settings when the screen opened, to see whether an operator changed those of the server. */
	private final String settingsAtOpen;
	private final List<Row> rows = new ArrayList<>();
	private final List<int[]> resetButtons = new ArrayList<>();
	private int visibleRows;
	private boolean draggingBar;
	private int changes;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("gearbalance.config.title"));
		// Profiles and the import reopen this screen: keep the state of the first one
		ConfigScreen first = parent instanceof ConfigScreen config ? config : null;
		this.parent = first != null ? first.parent : parent;
		this.gearAtOpen = first != null ? first.gearAtOpen : GearConfig.restartSignature();
		this.recipesAtOpen = first != null ? first.recipesAtOpen : recipeState();
		this.settingsAtOpen = first != null ? first.settingsAtOpen : GearConfig.settingsToJson().toString();
	}

	private static Sub sub() {
		return SUB_OF.get(main);
	}

	private static String recipeState() {
		return GearConfig.recipeState();
	}

	// ---------------------------------------------------------------- switches

	private static Toggle option(String id, BooleanSupplier current, BooleanConsumer setter, boolean standard) {
		return new Toggle("gearbalance.options." + id, current, setter, standard);
	}

	/** One hand effect: switching it on replaces the others, off returns to the glow outline. */
	private static Toggle effect(String id, int value) {
		return option(id, () -> GearConfig.handEffect == value, v -> {
			if (v) {
				GearConfig.handEffect = value;
			} else if (GearConfig.handEffect == value) {
				GearConfig.handEffect = GearConfig.EFFECT_GLOW;
			}
		}, false);
	}

	private static Toggle gear(String key, BooleanSupplier current, BooleanConsumer setter) {
		return new Toggle("gearbalance.options." + key, current, setter, false);
	}

	/** "Mod active" switches the gear values and every function together. */
	private static final Toggle ENABLED = new Toggle("gearbalance.options.enabled", () -> GearConfig.enabled,
			v -> GearConfig.enabled = v, true);
	private static final Toggle SHOW_HINTS = option("show_hints", () -> GearConfig.showHints,
			v -> GearConfig.showHints = v, true);
	private static final Toggle PADDED_ARMOR = option("padded_armor", () -> GearConfig.paddedArmor,
			v -> GearConfig.paddedArmor = v, false);
	private static final Toggle LEATHER_PATCH = option("leather_patch", () -> GearConfig.leatherPatch,
			v -> GearConfig.leatherPatch = v, false);
	private static final Toggle HORSE_PADDING = option("horse_padding", () -> GearConfig.horsePadding,
			v -> GearConfig.horsePadding = v, false);
	private static final Toggle UPGRADES = option("upgrades", () -> GearConfig.upgrades,
			v -> GearConfig.upgrades = v, false);
	private static final Toggle SOCKETS = option("sockets", () -> GearConfig.sockets,
			v -> GearConfig.sockets = v, false);
	private static final Toggle GEM_RETURN = option("gem_return", () -> GearConfig.gemReturn,
			v -> GearConfig.gemReturn = v, false);
	private static final Toggle GEM_RECUT = option("gem_recut", () -> GearConfig.gemRecut,
			v -> GearConfig.gemRecut = v, false);
	private static final Toggle KEEP_TABLE_ITEMS = option("keep_table_items", () -> GearConfig.keepTableItems,
			v -> GearConfig.keepTableItems = v, false);
	private static final Toggle SHIELDS = option("shields", () -> GearConfig.shields,
			v -> GearConfig.shields = v, false);
	private static final Toggle SHIELD_LOWER = option("shield_lower", () -> GearConfig.shieldLower,
			v -> GearConfig.shieldLower = v, false);
	private static final Toggle FIRE_LOWER = option("fire_lower", () -> GearConfig.fireLower,
			v -> GearConfig.fireLower = v, false);
	private static final Toggle TOOLTIP_FRAMES = option("tooltip_frames", () -> GearConfig.tooltipFrames,
			v -> GearConfig.tooltipFrames = v, false);
	private static final Toggle PICKUP_DISPLAY = option("pickup_display", () -> GearConfig.pickupDisplay,
			v -> GearConfig.pickupDisplay = v, false);
	private static final Toggle PICKUP_MARKS = option("pickup_marks", () -> GearConfig.pickupMarks,
			v -> GearConfig.pickupMarks = v, false);
	private static final Toggle PICKUP_RIGHT = option("pickup_right", () -> GearConfig.pickupRight,
			v -> GearConfig.pickupRight = v, false);
	private static final Toggle OUTLINES = option("outlines", () -> GearConfig.outlines,
			v -> GearConfig.outlines = v, false);
	private static final Toggle EFFECT_FLAMES = effect("effect_flames", GearConfig.EFFECT_FLAMES);
	private static final Toggle EFFECT_SHADOW = effect("effect_shadow", GearConfig.EFFECT_SHADOW);
	private static final Toggle HIDE_GLINT = option("hide_glint", () -> GearConfig.hideGlint,
			v -> GearConfig.hideGlint = v, false);
	private static final Toggle GLOW_PULSE = option("glow_pulse", () -> GearConfig.glowPulse,
			v -> GearConfig.glowPulse = v, true);
	private static final Toggle HAND_IDLE = option("hand_idle", () -> GearConfig.handIdle,
			v -> GearConfig.handIdle = v, false);
	private static final Toggle DURABILITY_WARNING = option("durability_warning", () -> GearConfig.durabilityWarning,
			v -> GearConfig.durabilityWarning = v, false);
	private static final Toggle ARMOR_BAR = option("armor_bar", () -> GearConfig.armorBar, v -> GearConfig.armorBar = v,
			false);
	private static final Toggle HD_ARMOR = option("hd_armor", () -> GearConfig.hdArmor, v -> GearConfig.hdArmor = v,
			false);
	private static final Toggle ARMOR_COLORS = option("armor_colors", () -> GearConfig.armorColors,
			v -> GearConfig.armorColors = v, false);
	private static final Toggle ARMOR_CRACKS = option("armor_cracks", () -> GearConfig.armorCracks,
			v -> GearConfig.armorCracks = v, false);
	private static final Toggle ARMOR_DARKEN = option("armor_darken", () -> GearConfig.armorDarken,
			v -> GearConfig.armorDarken = v, false);
	private static final Toggle ARMOR_BLINK = option("armor_blink", () -> GearConfig.armorBlink,
			v -> GearConfig.armorBlink = v, false);
	private static final Toggle ARMOR_BROKEN = option("armor_broken", () -> GearConfig.armorBroken,
			v -> GearConfig.armorBroken = v, false);
	private static final Toggle HOTBAR_POP = option("hotbar_pop", () -> GearConfig.hotbarPop,
			v -> GearConfig.hotbarPop = v, false);
	private static final Toggle HOTBAR_LOW_GLOW = option("hotbar_low_glow", () -> GearConfig.hotbarLowGlow,
			v -> GearConfig.hotbarLowGlow = v, false);
	private static final Toggle ITEM_NAME_BOUNCE = option("item_name_bounce", () -> GearConfig.itemNameBounce,
			v -> GearConfig.itemNameBounce = v, false);
	private static final Toggle XP_ANIMATED = option("xp_animated", () -> GearConfig.xpAnimated,
			v -> GearConfig.xpAnimated = v, false);
	private static final Toggle HD_HEARTS = option("hd_hearts", () -> GearConfig.hdHearts,
			v -> GearConfig.hdHearts = v, false);
	private static final Toggle HEART_RING = option("heart_ring", () -> GearConfig.heartRing,
			v -> GearConfig.heartRing = v, false);
	private static final Toggle HD_FOOD = option("hd_food", () -> GearConfig.hdFood,
			v -> GearConfig.hdFood = v, false);
	private static final Toggle SATURATION_GOLD = option("saturation_gold", () -> GearConfig.saturationGold,
			v -> GearConfig.saturationGold = v, false);
	private static final Toggle HD_HUD = option("hd_hud", () -> GearConfig.hdHud, v -> GearConfig.hdHud = v, false);
	private static final Toggle SMOOTH_SCROLL = option("smooth_scroll", () -> GearConfig.smoothScroll,
			v -> GearConfig.smoothScroll = v, false);
	private static final Toggle HOTBAR_GLIDE = option("hotbar_glide", () -> GearConfig.hotbarGlide,
			v -> GearConfig.hotbarGlide = v, false);
	private static final Toggle DEATH_PILE = option("death_pile", () -> GearConfig.deathPile,
			v -> GearConfig.deathPile = v, false);
	private static final Toggle DEATH_SAFE = option("death_safe", () -> GearConfig.deathSafe,
			v -> GearConfig.deathSafe = v, false);
	private static final Toggle DEATH_XP = option("death_xp", () -> GearConfig.deathXp,
			v -> GearConfig.deathXp = v, false);
	private static final Toggle DEATH_EXPLOSION = option("death_explosion", () -> GearConfig.deathExplosionProof,
			v -> GearConfig.deathExplosionProof = v, false);
	private static final Toggle DEATH_KEEP_ARMOR = option("death_keep_armor", () -> GearConfig.deathKeepArmor,
			v -> GearConfig.deathKeepArmor = v, false);
	private static final Toggle DEATH_KEEP_HOTBAR = option("death_keep_hotbar", () -> GearConfig.deathKeepHotbar,
			v -> GearConfig.deathKeepHotbar = v, false);
	private static final Toggle DEATH_KEEP_OFFHAND = option("death_keep_offhand", () -> GearConfig.deathKeepOffhand,
			v -> GearConfig.deathKeepOffhand = v, false);
	private static final Toggle DEATH_POINT = option("death_point", () -> GearConfig.deathPoint,
			v -> GearConfig.deathPoint = v, false);
	private static final Toggle SPAWNER_DROPS = option("spawner_drops", () -> GearConfig.spawnerDrops,
			v -> GearConfig.spawnerDrops = v, false);
	private static final Toggle SPAWNER_WHOLE = option("spawner_whole", () -> GearConfig.spawnerWhole,
			v -> GearConfig.spawnerWhole = v, false);
	private static final Toggle COMPARE_TOOLTIP = option("compare_tooltip", () -> GearConfig.compareTooltip,
			v -> GearConfig.compareTooltip = v, false);
	private static final Toggle TOOL_DURABILITY = option("tool_durability", () -> GearConfig.toolDurability,
			v -> GearConfig.toolDurability = v, false);
	private static final Toggle DURABILITY_ALL = option("durability_all", () -> GearConfig.durabilityAll,
			v -> GearConfig.durabilityAll = v, false);
	private static final Toggle REPAIR_KIT = option("repair_kit", () -> GearConfig.repairKit,
			v -> GearConfig.repairKit = v, false);
	private static final Toggle GEM_LEVER = option("gem_lever", () -> GearConfig.gemLever,
			v -> GearConfig.gemLever = v, false);
	private static final Toggle PARRY = option("parry", () -> GearConfig.parry,
			v -> GearConfig.parry = v, false);
	private static final Toggle SHIELD_SPEED = option("shield_speed", () -> GearConfig.shieldSpeed,
			v -> GearConfig.shieldSpeed = v, false);
	private static final Toggle INVENTORY_SCALE = option("inventory_scale", () -> GearConfig.inventoryScale,
			v -> GearConfig.inventoryScale = v, false);
	private static final Toggle SPRINT_FIX = option("sprint_fix", () -> GearConfig.sprintFix,
			v -> GearConfig.sprintFix = v, false);
	private static final Toggle HOTBAR_HIDE = option("hotbar_hide", () -> GearConfig.hotbarHide,
			v -> GearConfig.hotbarHide = v, false);
	private static final Toggle HOTBAR_HIDE_ALL = option("hotbar_hide_all", () -> GearConfig.hotbarHideAll,
			v -> GearConfig.hotbarHideAll = v, false);
	private static final Toggle HOTBAR_HIDE_ARMOR = option("hotbar_hide_armor", () -> GearConfig.hotbarHideArmor,
			v -> GearConfig.hotbarHideArmor = v, false);
	private static final Toggle HOTBAR_HIDE_XP = option("hotbar_hide_xp", () -> GearConfig.hotbarHideXp,
			v -> GearConfig.hotbarHideXp = v, false);
	private static final Toggle NAME_COLORS = option("name_colors", () -> GearConfig.nameColors,
			v -> GearConfig.nameColors = v, true);
	private static final Toggle TOOL_REFILL = option("tool_refill", () -> GearConfig.toolRefill,
			v -> GearConfig.toolRefill = v, false);
	private static final Toggle BREAK_PROTECTION = option("break_protection", () -> GearConfig.breakProtection,
			v -> GearConfig.breakProtection = v, false);
	private static final Toggle TITANIUM_SWITCH = option("titanium", () -> GearConfig.titanium,
			v -> GearConfig.titanium = v, false);
	private static final Toggle LOADOUTS = option("loadouts", () -> GearConfig.loadouts,
			v -> GearConfig.loadouts = v, false);
	private static final Toggle TRASH = option("trash", () -> GearConfig.trash, v -> GearConfig.trash = v, false);
	private static final Toggle DAY_NIGHT = option("day_night", () -> GearConfig.dayNight, v -> GearConfig.dayNight = v, false);
	/** Options only this player's game uses: open to change on a server too. */
	private static final Set<Toggle> CLIENT_ONLY = Set.of(SPRINT_FIX, HOTBAR_HIDE, HOTBAR_HIDE_ALL,
			HOTBAR_HIDE_ARMOR, HOTBAR_HIDE_XP, NAME_COLORS, SHOW_HINTS, TOOLTIP_FRAMES, COMPARE_TOOLTIP,
			TOOL_DURABILITY, DURABILITY_ALL, PICKUP_DISPLAY, PICKUP_MARKS, PICKUP_RIGHT,
			HAND_IDLE, DURABILITY_WARNING, INVENTORY_SCALE, OUTLINES, EFFECT_FLAMES, EFFECT_SHADOW, HIDE_GLINT, GLOW_PULSE,
			ARMOR_BAR, HD_ARMOR, ARMOR_COLORS, ARMOR_CRACKS, ARMOR_DARKEN, ARMOR_BLINK, ARMOR_BROKEN, HOTBAR_POP, HOTBAR_LOW_GLOW,
			ITEM_NAME_BOUNCE, XP_ANIMATED, HD_HEARTS, HEART_RING, HD_FOOD,
			SATURATION_GOLD, HD_HUD, SMOOTH_SCROLL, HOTBAR_GLIDE);
	private static final Toggle BAGS = option("bags", () -> GearConfig.bags,
			v -> GearConfig.bags = v, false);
	private static final Toggle MASTER_PLAN = option("master_plan", () -> GearConfig.masterPlan,
			v -> GearConfig.masterPlan = v, false);

	private static final Toggle SET_BONUSES = gear("set_bonuses", () -> GearConfig.setBonuses, v -> GearConfig.setBonuses = v);
	private static final Toggle TRIDENT = gear("trident", () -> GearConfig.tridentRecipe, v -> GearConfig.tridentRecipe = v);
	private static final Toggle CHAINMAIL = gear("chainmail", () -> GearConfig.chainmailRecipes,
			v -> GearConfig.chainmailRecipes = v);
	private static final Toggle RABBIT_RECIPE = gear("rabbit_recipe", () -> GearConfig.rabbitHideRecipe,
			v -> GearConfig.rabbitHideRecipe = v);
	private static final Toggle HORSE_ARMOR = gear("horse_armor", () -> GearConfig.horseArmorRecipes,
			v -> GearConfig.horseArmorRecipes = v);
	private static final Toggle RABBIT_ARMOR = gear("rabbit_armor", () -> GearConfig.rabbitHideArmor,
			v -> GearConfig.rabbitHideArmor = v);
	private static final Toggle ANVIL = gear("anvil", () -> GearConfig.anvilNoLimit, v -> GearConfig.anvilNoLimit = v);
	private static final Toggle ZOMBIE_LEATHER = gear("zombie_leather", () -> GearConfig.zombieLeather,
			v -> GearConfig.zombieLeather = v);
	private static final Toggle LOOT_TEMPLATES = gear("loot_templates", () -> GearConfig.lootTemplates,
			v -> GearConfig.lootTemplates = v);
	private static final Toggle LOOT_MATERIALS = gear("loot_materials", () -> GearConfig.lootMaterials,
			v -> GearConfig.lootMaterials = v);
	private static final Toggle LOOT_NO_CHEAP = gear("loot_no_cheap", () -> GearConfig.lootNoCheap,
			v -> GearConfig.lootNoCheap = v);
	private static final Toggle LOOT_OVERWORLD = gear("loot_overworld", () -> GearConfig.lootOverworld,
			v -> GearConfig.lootOverworld = v);
	private static final Toggle SHIELD_WEAR = gear("shield_wear", () -> GearConfig.shieldWear, v -> GearConfig.shieldWear = v);

	// ---------------------------------------------------------------- row builders

	private static MutableComponent t(String key, Object... args) {
		return Component.translatable(key, args);
	}

	/** A feature: its switch in the heading, its settings below (opened with the arrow). */
	private static Row function(Toggle toggle, Toggle needs, List<Row> children) {
		return new Row(null, null, t(toggle.nameKey()), t(toggle.nameKey() + ".tip"), List.of(), toggle,
				toggle.nameKey(), 0, needs, children, null, null, null);
	}

	/** A group of settings without own switch, e.g. "Sockets per armor". */
	private static Row group(String section, Component name, Component hint, Toggle needs, List<Row> children,
			Component label) {
		return new Row(null, null, name, hint, List.of(), null, section, 0, needs, children, label, null, null);
	}

	/** A sub menu inside a function or group (e.g. "Uses of the forge hammers"): arrow and keyword, rows indented. */
	private static Row subgroup(String section, Component name, Component hint, Toggle needs, List<Row> children) {
		List<Row> inner = new ArrayList<>();
		for (Row child : children) {
			inner.add(new Row(child.icon(), child.icon2(), child.name(), child.hint(), child.cells(), child.toggle(),
					child.section(), 2, child.needs(), child.children(), child.label(), child.reset(), child.setMaterial()));
		}
		return new Row(null, null, name, hint, List.of(), null, section, 1, needs, inner, null, null, null);
	}

	private static Row switchRow(Toggle toggle, Toggle needs) {
		return new Row(null, null, t(toggle.nameKey()), t(toggle.nameKey() + ".tip"), List.of(), toggle, null, 1, needs,
				List.of(), null, null, null);
	}

	private static Row numberRow(String icon, Component name, Component hint, List<Cell> cells, Toggle needs) {
		return new Row(icon, null, name, hint, cells, null, null, 1, needs, List.of(), null, null, null);
	}

	private static Cell value(String id, DoubleSupplier current, double standard, double min, double max,
			DoubleConsumer setter, String unit, boolean whole) {
		return new Cell(id, current, standard, min, max, whole ? 1 : 0, setter,
				() -> Math.abs(current.getAsDouble() - standard) > 1.0E-6, unit, null, null, false, false);
	}

	/** A chance stored as 0..1, shown in percent. */
	private static Cell percent(String id, DoubleSupplier current, double standard, DoubleConsumer setter) {
		return value(id, () -> current.getAsDouble() * 100.0, standard * 100.0, 0, 100, v -> setter.accept(v / 100.0),
				"%", false);
	}

	private static Cell whole(String id, IntSupplier current, int standard, int min, int max, IntConsumer setter,
			String unit) {
		return value(id, current::getAsInt, standard, min, max, v -> setter.accept((int) Math.round(v)), unit, true);
	}

	/** A whole number of a gear switch (anvil, zombie leather, shield wear), named after its options key. */
	private static Row gearNumber(String key, IntSupplier current, int standard, int min, int max, IntConsumer setter,
			String unit, Toggle needs) {
		String full = "gearbalance.options." + key;
		return numberRow(null, t(full), t(full + ".tip", max),
				List.of(whole("gear." + key, current, standard, min, max, setter, unit)), needs);
	}

	/** Uses of a smithing tool (item path). */
	private static Row usesRow(String tool, Toggle needs) {
		String icon = GearBalance.MOD_ID + ":" + tool;
		return numberRow(icon, itemName(icon), t("gearbalance.config.uses.tip"),
				List.of(whole("uses." + tool, () -> GearConfig.TOOL_DURABILITY.get(tool),
						GearConfig.defaultDurability(tool), GearConfig.minDurability(tool),
						GearConfig.maxDurability(tool), v -> GearConfig.TOOL_DURABILITY.put(tool, v), "")),
				needs);
	}

	private static List<Row> tools(String suffix, Toggle needs) {
		List<Row> result = new ArrayList<>();
		for (String tool : GearConfig.TOOLS) {
			if (tool.endsWith(suffix)) {
				result.add(usesRow(tool, needs));
			}
		}
		return result;
	}

	private static Row cooldownRow(String key, IntSupplier current, IntConsumer setter, Toggle needs) {
		return numberRow(null, t("gearbalance.config.cooldown." + key),
				t("gearbalance.config.cooldown." + key + ".tip"),
				List.of(whole("cooldown." + key, current, 0, 0, GearConfig.MAX_COOLDOWN_HOURS, setter, " h")), needs);
	}

	/** A setting of the glow tab, shown as a slider (whole numbers). */
	private static Row glowRow(String key, IntSupplier current, int standard, int min, int max, IntConsumer setter,
			String unit, Toggle needs) {
		Cell cell = new Cell("glow." + key, current::getAsInt, standard, min, max, 1, v -> setter.accept((int) Math.round(v)),
				() -> current.getAsInt() != standard, unit, null, null, false, true);
		return numberRow(null, t("gearbalance.config.glow." + key), t("gearbalance.config.glow." + key + ".tip"),
				List.of(cell), needs);
	}

	/** A whole percent value of the view settings. */
	private static Row viewRow(String key, IntSupplier current, int standard, int max, IntConsumer setter, Toggle needs) {
		return numberRow(null, t("gearbalance.config.view." + key), t("gearbalance.config.view." + key + ".tip"),
				List.of(whole("view." + key, current, standard, 0, max, setter, "%")), needs);
	}

	/** A whole number of the hotbar hiding. */
	private static Row secondsRow(String key, IntSupplier current, int standard, int min, int max, IntConsumer setter,
			String unit) {
		return numberRow(null, t("gearbalance.config.view." + key), t("gearbalance.config.view." + key + ".tip"),
				List.of(whole("view." + key, current, standard, min, max, setter, unit)), HOTBAR_HIDE);
	}

	// ---------------------------------------------------------------- the tabs

	/** The headings of a tab; each holds its rows. */
	private static List<Row> sections(Sub of) {
		List<Row> result = new ArrayList<>();
		switch (of) {
			case SMITHING -> {
				List<Row> upgrades = new ArrayList<>();
				upgrades.add(cooldownRow("upgrade", () -> GearConfig.upgradeCooldownHours,
						v -> GearConfig.upgradeCooldownHours = v, UPGRADES));
				upgrades.add(subgroup("hammer_uses", t("gearbalance.config.sub.hammer_uses"),
						t("gearbalance.config.uses.tip"), UPGRADES, tools("_forge_hammer", UPGRADES)));
				upgrades.add(subgroup("weapon_upgrades", t("gearbalance.config.sub.weapon_upgrades"),
						t("gearbalance.config.weapon_upgrade.tip"), UPGRADES, weaponUpgradeRows()));
				result.add(function(UPGRADES, null, upgrades));
				result.add(function(MASTER_PLAN, null, List.of(
						numberRow(null, t("gearbalance.config.plan.chests"), t("gearbalance.config.plan.chests.tip"),
								List.of(percent("plan.chests", () -> GearConfig.planChestChance,
										GearConfig.DEFAULT_PLAN_CHEST_CHANCE, v -> GearConfig.planChestChance = v)),
								MASTER_PLAN),
						numberRow(null, t("gearbalance.config.plan.mobs"), t("gearbalance.config.plan.mobs.tip"),
								List.of(percent("plan.mobs", () -> GearConfig.planMobChance,
										GearConfig.DEFAULT_PLAN_MOB_CHANCE, v -> GearConfig.planMobChance = v)),
								MASTER_PLAN))));
				result.add(function(REPAIR_KIT, null, tools("_repair_kit", REPAIR_KIT)));
				result.add(function(KEEP_TABLE_ITEMS, null, List.of()));
				result.add(function(ANVIL, null, List.of(gearNumber("anvil_max", () -> GearConfig.anvilMaxCost, 39, 1,
						GearConfig.ANVIL_MAX_COST_LIMIT, v -> GearConfig.anvilMaxCost = v, "", ANVIL))));
			}
			case SOCKETS -> {
				List<Row> sockets = new ArrayList<>();
				sockets.add(cooldownRow("socket", () -> GearConfig.socketCooldownHours,
						v -> GearConfig.socketCooldownHours = v, SOCKETS));
				sockets.add(subgroup("chisel_uses", t("gearbalance.config.sub.chisel_uses"),
						t("gearbalance.config.uses.tip"), SOCKETS, tools("_socket_chisel", SOCKETS)));
				socketGroups(sockets);
				result.add(function(SOCKETS, null, sockets));
				result.add(function(GEM_RETURN, SOCKETS, List.of(numberRow("minecraft:grindstone",
						t("gearbalance.values.return"), t("gearbalance.values.return.tip"),
						List.of(percent("return", () -> GearConfig.grindstoneReturnChance,
								GearConfig.DEFAULT_RETURN_CHANCE, v -> GearConfig.grindstoneReturnChance = v)),
						GEM_RETURN))));
				result.add(function(GEM_RECUT, SOCKETS, List.of()));
				result.add(function(GEM_LEVER, SOCKETS, tools("_gem_lever", GEM_LEVER)));
			}
			case GEMS -> {
				for (String type : GearConfig.GEM_TYPES) {
					List<Row> children = new ArrayList<>();
					if (GearConfig.BONUS_SHARE.containsKey(type)) {
						// Share of the set values for each of the two bonuses of an aquamarine or onyx
						children.add(numberRow(null, t("gearbalance.config.gem.share"),
								t("gearbalance.config.gem.share.tip"),
								List.of(whole("gem." + type + ".share", () -> GearConfig.BONUS_SHARE.get(type),
										GearConfig.defaultShare(type), GearConfig.MIN_SHARE, GearConfig.MAX_SHARE,
										v -> GearConfig.setShare(type, v), "%")),
								SOCKETS));
					}
					children.addAll(gemRows(type));
					// Gem name in its color with the flawless symbol in front, like in the tooltips
					Component gemName = GearBalanceClient.symbolLine(GearBalanceClient.gemSymbol(type, 3),
							GearBalanceClient.gemColored(type, t("gearbalance.values.gem." + type)));
					result.add(group("gem." + type, gemName,
							t("gearbalance.values.gem_row.tip"), SOCKETS, children, null));
				}
			}
			case SHIELDS -> {
				List<Row> shields = new ArrayList<>();
				shieldGroups(shields);
				result.add(function(SHIELDS, null, shields));
				result.add(function(SHIELD_WEAR, null, List.of(
						gearNumber("shield_wear_start", () -> GearConfig.shieldWearStart, 50, 1, 100,
								v -> GearConfig.shieldWearStart = v, "%", SHIELD_WEAR),
						gearNumber("shield_wear_min", () -> GearConfig.shieldWearMin, 60, 0, 100,
								v -> GearConfig.shieldWearMin = v, "%", SHIELD_WEAR))));
				result.add(function(SHIELD_SPEED, SHIELDS, shieldSpeedRows()));
				result.add(function(PARRY, null, parryRows()));
			}
			case SET_BONUSES -> {
				// Switch on top, every material right below it (grayed out while the switch is off)
				result.add(function(SET_BONUSES, null, List.of()));
				result.addAll(setRows(0));
			}
			case LOOT -> {
				result.add(function(ZOMBIE_LEATHER, null, List.of(gearNumber("zombie_leather_chance",
						() -> GearConfig.zombieLeatherChance, 33, 1, 100, v -> GearConfig.zombieLeatherChance = v, "%",
						ZOMBIE_LEATHER))));
				result.add(function(LOOT_OVERWORLD, null, List.of()));
				result.add(group("endgame", t("gearbalance.menu.endgame"), t("gearbalance.menu.endgame.tip"), null,
						List.of(switchRow(LOOT_TEMPLATES, null), switchRow(LOOT_MATERIALS, null),
								switchRow(LOOT_NO_CHEAP, null)), null));
				gemLoot(result);
			}
			case TAILORING -> {
				// The needle serves every tailoring function, so it has its own group
				result.add(group("tailoring.needle", t("item.gearbalance.sewing_needle"),
						t("gearbalance.config.tailoring.needle.tip"), null, tools("sewing_needle", null), null));
				result.add(function(PADDED_ARMOR, null, List.of()));
				result.add(function(LEATHER_PATCH, null, List.of()));
				result.add(function(HORSE_PADDING, null, List.of()));
				List<Row> bags = new ArrayList<>();
				bags.add(numberRow(null, t("gearbalance.config.bag_columns"), t("gearbalance.config.bag_columns.tip"),
						List.of(whole("bag_columns", () -> GearConfig.bagColumns, GearConfig.MAX_BAG_COLUMNS,
								GearConfig.MIN_BAG_COLUMNS, GearConfig.MAX_BAG_COLUMNS,
								v -> GearConfig.bagColumns = GearConfig.bagColumns(v), "")),
						BAGS));
				for (String tier : GearConfig.BAG_TIERS) {
					String icon = GearBalance.MOD_ID + ":" + tier + "_bag";
					bags.add(numberRow(icon, itemName(icon), t("gearbalance.config.bag_rows.tip"),
							List.of(whole("bag_rows." + tier, () -> GearConfig.BAG_ROWS.get(tier),
									GearConfig.defaultBagRows(tier), 1, GearConfig.MAX_BAG_ROWS,
									v -> GearConfig.BAG_ROWS.put(tier, v), "")),
							BAGS));
				}
				result.add(function(BAGS, null, bags));
			}
			case RECIPES -> {
				result.add(function(TRIDENT, null, List.of()));
				result.add(function(CHAINMAIL, null, List.of()));
				result.add(function(RABBIT_RECIPE, null, List.of()));
				result.add(function(RABBIT_ARMOR, null, List.of()));
				result.add(function(HORSE_ARMOR, null, List.of()));
			}
			case TITANIUM -> result.add(function(TITANIUM_SWITCH, null, List.of(numberRow("gearbalance:titanium_boots",
					t("gearbalance.config.titanium_fall"), t("gearbalance.config.titanium_fall.tip"),
					List.of(whole("titanium_fall", () -> GearConfig.titaniumFallPercent, 10, 0, 25,
							v -> GearConfig.titaniumFallPercent = v, "%")),
					TITANIUM_SWITCH),
					numberRow("gearbalance:titanite", t("gearbalance.config.titanite_chance"),
							t("gearbalance.config.titanite_chance.tip"),
							List.of(value("titanite_chance", () -> GearConfig.titaniteChance, 1.0, 0, 100,
									v -> GearConfig.titaniteChance = v, "%", false)),
							TITANIUM_SWITCH),
					numberRow("gearbalance:flawless_titanite", t("gearbalance.config.titanite_boost"),
							t("gearbalance.config.titanite_boost.tip"),
							List.of(whole("titanite_boost_chipped", () -> GearConfig.titaniteBoostChipped, 8, 0, 100,
											v -> GearConfig.titaniteBoostChipped = v, "%"),
									whole("titanite_boost_cut", () -> GearConfig.titaniteBoostCut, 16, 0, 100,
											v -> GearConfig.titaniteBoostCut = v, "%"),
									whole("titanite_boost", () -> GearConfig.titaniteBoost, 25, 0, 100,
											v -> GearConfig.titaniteBoost = v, "%")),
							TITANIUM_SWITCH))));
			case COMFORT -> {
				result.add(function(SPRINT_FIX, null, List.of()));
				result.add(function(TOOL_REFILL, null, List.of()));
				result.add(function(BREAK_PROTECTION, null, List.of()));
				result.add(function(LOADOUTS, null, List.of()));
				result.add(function(TRASH, null, List.of()));
				result.add(function(DAY_NIGHT, null, List.of(numberRow(null,
						t("gearbalance.config.comfort.day_night"), t("gearbalance.config.comfort.day_night.tip"),
						List.of(whole("comfort.day_minutes", () -> GearConfig.dayMinutes, GearConfig.DEFAULT_DAY_MINUTES, 1,
										GearConfig.MAX_DAY_NIGHT_MINUTES, v -> GearConfig.dayMinutes = v, " min"),
								whole("comfort.night_minutes", () -> GearConfig.nightMinutes, GearConfig.DEFAULT_NIGHT_MINUTES, 1,
										GearConfig.MAX_DAY_NIGHT_MINUTES, v -> GearConfig.nightMinutes = v, " min")),
						DAY_NIGHT))));
				result.add(group("comfort.death", t("gearbalance.config.comfort.group.death"),
						t("gearbalance.config.comfort.group.death.tip"), null, List.of(
								switchRow(DEATH_PILE, null),
								numberRow(null, t("gearbalance.config.comfort.death_despawn"), t("gearbalance.config.comfort.death_despawn.tip"),
										List.of(whole("comfort.death_despawn", () -> GearConfig.deathDespawnMinutes, GearConfig.DEFAULT_DEATH_DESPAWN, 0,
												GearConfig.MAX_DEATH_DESPAWN, v -> GearConfig.deathDespawnMinutes = v, " min")), DEATH_PILE),
								switchRow(DEATH_SAFE, null),
								switchRow(DEATH_XP, null),
								numberRow(null, t("gearbalance.config.comfort.death_xp"), t("gearbalance.config.comfort.death_xp.tip"),
										List.of(whole("comfort.death_xp", () -> GearConfig.deathXpPercent, GearConfig.DEFAULT_DEATH_XP, 0,
												GearConfig.MAX_PERCENT_100, v -> GearConfig.deathXpPercent = v, "%")), DEATH_XP),
								switchRow(DEATH_EXPLOSION, null),
								switchRow(DEATH_KEEP_ARMOR, null),
								switchRow(DEATH_KEEP_HOTBAR, null),
								switchRow(DEATH_KEEP_OFFHAND, null),
								numberRow(null, t("gearbalance.config.comfort.death_keep_damage"), t("gearbalance.config.comfort.death_keep_damage.tip"),
										List.of(whole("comfort.death_keep_damage", () -> GearConfig.deathKeepDamage, GearConfig.DEFAULT_DEATH_KEEP_DAMAGE, 0,
												GearConfig.MAX_DEATH_KEEP_DAMAGE, v -> GearConfig.deathKeepDamage = v, "%")), null),
								switchRow(DEATH_POINT, null)), null));
				result.add(group("comfort.spawner", t("gearbalance.config.comfort.group.spawner"),
						t("gearbalance.config.comfort.group.spawner.tip"), null, List.of(
								switchRow(SPAWNER_DROPS, null),
								numberRow(null, t("gearbalance.config.comfort.spawner_iron"), t("gearbalance.config.comfort.spawner_iron.tip"),
										List.of(whole("comfort.spawner_iron", () -> GearConfig.spawnerIron, GearConfig.DEFAULT_SPAWNER_IRON, 0,
												GearConfig.MAX_SPAWNER_IRON, v -> GearConfig.spawnerIron = v, "")), SPAWNER_DROPS),
								numberRow(null, t("gearbalance.config.comfort.spawner_gem"), t("gearbalance.config.comfort.spawner_gem.tip"),
										List.of(whole("comfort.spawner_gem", () -> GearConfig.spawnerGemChance, GearConfig.DEFAULT_SPAWNER_GEM, 0,
												GearConfig.MAX_PERCENT_100, v -> GearConfig.spawnerGemChance = v, "%")), SPAWNER_DROPS),
								switchRow(SPAWNER_WHOLE, SPAWNER_DROPS)), null));
			}
			case GENERAL -> {
				result.add(function(ENABLED, null, List.of()));
			}
			case GLOW -> {
				// Everything about the glow of full sockets: switch and effects, outline, color, pulse, shields
				result.add(group("glow.effect", t("gearbalance.config.glow.group.effect"),
						t("gearbalance.config.glow.group.effect.tip"), null, List.of(
								switchRow(OUTLINES, null),
								switchRow(EFFECT_FLAMES, OUTLINES),
								switchRow(EFFECT_SHADOW, OUTLINES),
								glowRow("effect_brightness", () -> GearConfig.effectBrightness, GearConfig.DEFAULT_EFFECT_BRIGHTNESS,
										0, 100, v -> GearConfig.effectBrightness = v, "%", OUTLINES),
								switchRow(HIDE_GLINT, OUTLINES)), null));
				result.add(group("glow.outline", t("gearbalance.config.glow.group.outline"),
						t("gearbalance.config.glow.group.outline.tip"), null, List.of(
								glowRow("ring_saturation", () -> GearConfig.ringSaturation, GearConfig.DEFAULT_RING_SATURATION,
										0, 100, v -> GearConfig.ringSaturation = v, "%", OUTLINES)), null));
				result.add(group("glow.color", t("gearbalance.config.glow.group.color"),
						t("gearbalance.config.glow.group.color.tip"), null, List.of(
								glowRow("effect_red", () -> GearConfig.effectRed, GearConfig.DEFAULT_EFFECT_RED,
										0, 255, v -> GearConfig.effectRed = v, "", OUTLINES),
								glowRow("effect_green", () -> GearConfig.effectGreen, GearConfig.DEFAULT_EFFECT_GREEN,
										0, 255, v -> GearConfig.effectGreen = v, "", OUTLINES),
								glowRow("effect_blue", () -> GearConfig.effectBlue, GearConfig.DEFAULT_EFFECT_BLUE,
										0, 255, v -> GearConfig.effectBlue = v, "", OUTLINES)), null));
				result.add(group("glow.pulse", t("gearbalance.config.glow.group.pulse"),
						t("gearbalance.config.glow.group.pulse.tip"), null, List.of(
								switchRow(GLOW_PULSE, OUTLINES),
								glowRow("pulse_strength", () -> GearConfig.pulseStrength, GearConfig.DEFAULT_PULSE_STRENGTH,
										0, 100, v -> GearConfig.pulseStrength = v, "%", GLOW_PULSE),
								glowRow("pulse_seconds", () -> GearConfig.pulseSeconds, GearConfig.DEFAULT_PULSE_SECONDS,
										1, GearConfig.MAX_PULSE_SECONDS, v -> GearConfig.pulseSeconds = v, " s", GLOW_PULSE)), null));
				result.add(group("glow.shield", t("gearbalance.config.glow.group.shield"),
						t("gearbalance.config.glow.group.shield.tip"), null, List.of(
								glowRow("shield_frame", () -> GearConfig.shieldFrame, GearConfig.DEFAULT_SHIELD_FRAME,
										0, 100, v -> GearConfig.shieldFrame = v, "%", OUTLINES),
								glowRow("shield_glow", () -> GearConfig.shieldGlow, GearConfig.DEFAULT_SHIELD_GLOW,
										0, 100, v -> GearConfig.shieldGlow = v, "%", OUTLINES)), null));
			}
			case VIEW -> {
				// Grouped by where it shows: tooltips, picking up, the screen while playing, the inventory
				result.add(group("view.tooltips", t("gearbalance.config.view.group.tooltips"),
						t("gearbalance.config.view.group.tooltips.tip"), null, List.of(
								switchRow(TOOLTIP_FRAMES, null), switchRow(NAME_COLORS, null), switchRow(SHOW_HINTS, null),
								switchRow(COMPARE_TOOLTIP, null), switchRow(TOOL_DURABILITY, null),
								switchRow(DURABILITY_ALL, null)), null));
				result.add(group("view.pickup", t("gearbalance.config.view.group.pickup"),
						t("gearbalance.config.view.group.pickup.tip"), null, List.of(
								switchRow(PICKUP_DISPLAY, null),
								switchRow(PICKUP_RIGHT, PICKUP_DISPLAY),
								numberRow(null, t("gearbalance.config.view.pickup_seconds"), t("gearbalance.config.view.pickup_seconds.tip"),
										List.of(whole("view.pickup_seconds", () -> GearConfig.pickupSeconds, GearConfig.DEFAULT_PICKUP_SECONDS,
												1, GearConfig.MAX_PICKUP_SECONDS, v -> GearConfig.pickupSeconds = v, " s")), PICKUP_DISPLAY),
								numberRow(null, t("gearbalance.config.view.pickup_rows"), t("gearbalance.config.view.pickup_rows.tip"),
										List.of(whole("view.pickup_rows", () -> GearConfig.pickupRows, GearConfig.DEFAULT_PICKUP_ROWS,
												1, GearConfig.MAX_PICKUP_ROWS, v -> GearConfig.pickupRows = v, "")), PICKUP_DISPLAY),
								numberRow(null, t("gearbalance.config.view.pickup_scale"), t("gearbalance.config.view.pickup_scale.tip"),
										List.of(whole("view.pickup_scale", () -> GearConfig.pickupScale, GearConfig.DEFAULT_PICKUP_SCALE,
												GearConfig.MIN_PICKUP_SCALE, GearConfig.MAX_PICKUP_SCALE, v -> GearConfig.pickupScale = v, "%")),
										PICKUP_DISPLAY),
								switchRow(PICKUP_MARKS, null)), null));
				result.add(group("view.screen", t("gearbalance.config.view.group.screen"),
						t("gearbalance.config.view.group.screen.tip"), null, List.of(
								switchRow(SHIELD_LOWER, null),
								viewRow("shield_lower", () -> GearConfig.shieldLowerPercent, GearConfig.DEFAULT_SHIELD_LOWER,
										GearConfig.MAX_SHIELD_LOWER, v -> GearConfig.shieldLowerPercent = v, SHIELD_LOWER),
								switchRow(FIRE_LOWER, null),
								viewRow("fire_lower", () -> GearConfig.fireLowerPercent, GearConfig.DEFAULT_FIRE_LOWER,
										GearConfig.MAX_FIRE_LOWER, v -> GearConfig.fireLowerPercent = v, FIRE_LOWER),
								switchRow(HAND_IDLE, null),
								viewRow("hand_idle", () -> GearConfig.handIdlePercent, GearConfig.DEFAULT_HAND_IDLE,
										GearConfig.MAX_HAND_IDLE, v -> GearConfig.handIdlePercent = v, HAND_IDLE),
								switchRow(DURABILITY_WARNING, null),
								viewRow("durability_warning", () -> GearConfig.warnPercent, GearConfig.DEFAULT_WARN,
										GearConfig.MAX_WARN, v -> GearConfig.warnPercent = v, DURABILITY_WARNING)), null));
				result.add(function(ARMOR_BAR, null, List.of(
								switchRow(ARMOR_COLORS, ARMOR_BAR),
								switchRow(ARMOR_CRACKS, ARMOR_BAR),
								viewRow("armor_crack", () -> GearConfig.armorCrackPercent, GearConfig.DEFAULT_ARMOR_CRACK, 100,
										v -> GearConfig.armorCrackPercent = v, ARMOR_CRACKS),
								viewRow("armor_crack2", () -> GearConfig.armorCrack2Percent, GearConfig.DEFAULT_ARMOR_CRACK2, 100,
										v -> GearConfig.armorCrack2Percent = v, ARMOR_CRACKS),
								switchRow(ARMOR_DARKEN, ARMOR_BAR),
								viewRow("armor_darken", () -> GearConfig.armorDarkenPercent, GearConfig.DEFAULT_ARMOR_DARKEN,
										GearConfig.MAX_ARMOR_DARKEN, v -> GearConfig.armorDarkenPercent = v, ARMOR_DARKEN),
								switchRow(ARMOR_BLINK, ARMOR_BAR),
								viewRow("armor_blink", () -> GearConfig.armorBlinkPercent, GearConfig.DEFAULT_ARMOR_BLINK,
										GearConfig.MAX_WARN, v -> GearConfig.armorBlinkPercent = v, ARMOR_BLINK),
								switchRow(ARMOR_BROKEN, ARMOR_BAR))));
				result.add(group("view.hearts", t("gearbalance.config.view.group.hearts"),
						t("gearbalance.config.view.group.hearts.tip"), null, List.of(
								switchRow(HD_ARMOR, ARMOR_BAR),
								switchRow(HD_HEARTS, null),
								switchRow(HEART_RING, HD_HEARTS),
								switchRow(HD_FOOD, null),
								switchRow(SATURATION_GOLD, HD_FOOD),
								switchRow(HD_HUD, null)), null));
				result.add(group("view.scrolling", t("gearbalance.config.view.group.scrolling"),
						t("gearbalance.config.view.group.scrolling.tip"), null, List.of(
								switchRow(SMOOTH_SCROLL, null)), null));
				result.add(group("view.hotbar", t("gearbalance.config.view.group.hotbar"),
						t("gearbalance.config.view.group.hotbar.tip"), null, List.of(
								switchRow(HOTBAR_HIDE, null),
								secondsRow("hotbar_hide_seconds", () -> GearConfig.hotbarHideSeconds, 5, 1, 60,
										v -> GearConfig.hotbarHideSeconds = v, " s"),
								switchRow(HOTBAR_HIDE_ALL, HOTBAR_HIDE),
								switchRow(HOTBAR_HIDE_ARMOR, HOTBAR_HIDE),
								switchRow(HOTBAR_HIDE_XP, HOTBAR_HIDE),
								secondsRow("hotbar_damage_seconds", () -> GearConfig.hotbarDamageSeconds, 10, 0, 60,
										v -> GearConfig.hotbarDamageSeconds = v, " s"),
								secondsRow("hotbar_food", () -> GearConfig.hotbarFoodThreshold, 6, 0, 20,
										v -> GearConfig.hotbarFoodThreshold = v, ""),
								switchRow(HOTBAR_GLIDE, null),
								switchRow(HOTBAR_POP, null),
								viewRow("hotbar_pop", () -> GearConfig.hotbarPopPercent, GearConfig.DEFAULT_HOTBAR_POP,
										GearConfig.MAX_HOTBAR_POP, v -> GearConfig.hotbarPopPercent = v, HOTBAR_POP),
								switchRow(HOTBAR_LOW_GLOW, null),
								viewRow("hotbar_glow", () -> GearConfig.hotbarGlowPercent, GearConfig.DEFAULT_HOTBAR_GLOW,
										GearConfig.MAX_WARN, v -> GearConfig.hotbarGlowPercent = v, HOTBAR_LOW_GLOW),
								switchRow(ITEM_NAME_BOUNCE, null),
								switchRow(XP_ANIMATED, null)), null));
				result.add(group("view.inventory", t("gearbalance.config.view.group.inventory"),
						t("gearbalance.config.view.group.inventory.tip"), null, List.of(
								switchRow(INVENTORY_SCALE, null),
								numberRow(null, t("gearbalance.config.view.inventory_scale"), t("gearbalance.config.view.inventory_scale.tip"),
										List.of(whole("view.inventory_scale", () -> GearConfig.inventoryGuiScale,
												GearConfig.DEFAULT_INVENTORY_GUI_SCALE, 1, GearConfig.MAX_INVENTORY_GUI_SCALE,
												v -> GearConfig.inventoryGuiScale = v, "")),
										INVENTORY_SCALE)), null));
			}
			default -> gearRows(of, result);
		}
		return of.main == Main.GEAR ? result : singlesFirst(result);
	}

	/** Rows without a sub menu first, then the ones that open, in every sub menu; order otherwise kept. */
	private static List<Row> singlesFirst(List<Row> rows) {
		List<Row> singles = new ArrayList<>();
		List<Row> groups = new ArrayList<>();
		for (Row row : rows) {
			if (row.children().isEmpty()) {
				singles.add(row);
			} else {
				groups.add(new Row(row.icon(), row.icon2(), row.name(), row.hint(), row.cells(), row.toggle(),
						row.section(), row.indent(), row.needs(), singlesFirst(row.children()), row.label(), row.reset(),
						row.setMaterial()));
			}
		}
		singles.addAll(groups);
		return singles;
	}

	/** Upgrade values of mace, trident, bow and crossbow: percent per level. */
	private static List<Row> weaponUpgradeRows() {
		List<Row> result = new ArrayList<>();
		GearConfig.WEAPON_UPGRADE_VALUES.forEach((weapon, values) -> {
			String icon = "minecraft:" + weapon;
			for (String value : values) {
				MutableComponent name = itemName(icon).copy().append(" – ")
						.append(t("gearbalance.config.weapon_upgrade." + value));
				result.add(numberRow(icon, name, t("gearbalance.config.weapon_upgrade.tip"),
						List.of(value("weapon_upgrade." + weapon + "." + value, () -> GearConfig.weaponUpgrade(weapon, value),
								GearConfig.defaultWeaponUpgrade(value), 0, GearConfig.MAX_WEAPON_UPGRADE,
								v -> GearConfig.WEAPON_UPGRADES.get(weapon).put(value, v), "%", false)),
						UPGRADES));
			}
		});
		return result;
	}

	/** Item id of the shield of a material; "wood" is the wooden shield of GearBalance, or the vanilla one. */
	private static String shieldIcon(String material, boolean vanilla) {
		if (material.equals("wood")) {
			return vanilla ? "minecraft:shield" : GearBalance.MOD_ID + ":wooden_shield";
		}
		return GearBalance.MOD_ID + ":" + (material.equals("gold") ? "golden" : material) + "_shield";
	}

	/** Blocked damage, durability, block delay, axe time and explosion protection angle of the GearBalance shields. */
	private static void shieldGroups(List<Row> result) {
		List<Row> durability = new ArrayList<>();
		List<Row> delay = new ArrayList<>();
		List<Row> axe = new ArrayList<>();
		List<Row> angle = new ArrayList<>();
		List<Row> blocked = new ArrayList<>();
		for (String material : GearConfig.SHIELD_MATERIALS) {
			String icon = shieldIcon(material, false);
			durability.add(numberRow(icon, itemName(icon), t("gearbalance.config.section.shield_durability.tip"),
					List.of(whole("shield_durability." + material, () -> GearConfig.SHIELD_DURABILITY.get(material),
							GearConfig.defaultShieldDurability(material), 1, GearConfig.MAX_SHIELD_DURABILITY,
							v -> GearConfig.SHIELD_DURABILITY.put(material, v), "")),
					SHIELDS));
			delay.add(numberRow(icon, itemName(icon), t("gearbalance.config.section.shield_block_delay.tip"),
					List.of(value("shield_block_delay." + material, () -> GearConfig.SHIELD_BLOCK_DELAY.get(material),
							GearConfig.defaultShieldBlockDelay(material), 0, GearConfig.MAX_SHIELD_BLOCK_DELAY,
							v -> GearConfig.SHIELD_BLOCK_DELAY.put(material, v), " s", false)),
					SHIELDS));
			axe.add(numberRow(icon, itemName(icon), t("gearbalance.config.section.shield_axe.tip"),
					List.of(value("shield_axe." + material, () -> GearConfig.SHIELD_AXE_SECONDS.get(material),
							GearConfig.defaultShieldAxeSeconds(material), 0, GearConfig.MAX_SHIELD_AXE_SECONDS,
							v -> GearConfig.SHIELD_AXE_SECONDS.put(material, v), " s", false)),
					SHIELDS));
			angle.add(numberRow(icon, itemName(icon), t("gearbalance.config.section.shield_explosion_angle.tip"),
					List.of(whole("shield_explosion_angle." + material, () -> GearConfig.SHIELD_EXPLOSION_ANGLE.get(material),
							GearConfig.defaultShieldExplosionAngle(material), GearConfig.MIN_SHIELD_EXPLOSION_ANGLE,
							GearConfig.MAX_SHIELD_EXPLOSION_ANGLE, v -> GearConfig.SHIELD_EXPLOSION_ANGLE.put(material, v), "°")),
					SHIELDS));
			blocked.add(numberRow(icon, itemName(icon), t("gearbalance.config.section.shield_block.tip"),
					List.of(whole("shield_block." + material, () -> GearConfig.SHIELD_BLOCK.get(material),
							GearConfig.DEFAULT_SHIELD_BLOCK, 0, 100, v -> GearConfig.SHIELD_BLOCK.put(material, v), "%")),
					SHIELDS));
		}
		result.add(subgroup("shield_block", t("gearbalance.config.section.shield_block"),
				t("gearbalance.config.section.shield_block.tip"), SHIELDS, blocked));
		result.add(subgroup("shield_durability", t("gearbalance.config.section.shield_durability"),
				t("gearbalance.config.section.shield_durability.tip"), SHIELDS, durability));
		result.add(subgroup("shield_block_delay", t("gearbalance.config.section.shield_block_delay"),
				t("gearbalance.config.section.shield_block_delay.tip"), SHIELDS, delay));
		result.add(subgroup("shield_axe", t("gearbalance.config.section.shield_axe"),
				t("gearbalance.config.section.shield_axe.tip"), SHIELDS, axe));
		result.add(subgroup("shield_explosion_angle", t("gearbalance.config.section.shield_explosion_angle"),
				t("gearbalance.config.section.shield_explosion_angle.tip"), SHIELDS, angle));
	}

	/** Walking speed while blocking, per GearBalance shield. */
	private static List<Row> shieldSpeedRows() {
		List<Row> result = new ArrayList<>();
		for (String material : GearConfig.SHIELD_MATERIALS) {
			String icon = shieldIcon(material, false);
			result.add(numberRow(icon, itemName(icon), t("gearbalance.config.shield_speed.tip"),
					List.of(whole("shield_speed." + material, () -> GearConfig.SHIELD_SPEED.get(material),
							GearConfig.defaultShieldSpeed(material), GearConfig.MIN_SHIELD_SPEED, GearConfig.MAX_SHIELD_SPEED,
							v -> GearConfig.SHIELD_SPEED.put(material, v), "%")),
					SHIELD_SPEED));
		}
		return result;
	}

	/** How far a parry throws the attacker back, per shield material. */
	private static List<Row> parryRows() {
		List<Row> result = new ArrayList<>();
		for (String material : GearConfig.SHIELD_MATERIALS) {
			String icon = shieldIcon(material, true);
			result.add(numberRow(icon, itemName(icon), t("gearbalance.config.parry_knockback.tip"),
					List.of(value("parry_knockback." + material, () -> GearConfig.PARRY_KNOCKBACK.get(material),
							GearConfig.defaultParryKnockback(material), 0, GearConfig.MAX_PARRY_KNOCKBACK,
							v -> GearConfig.PARRY_KNOCKBACK.put(material, v), "", false)),
					PARRY));
		}
		return result;
	}

	/** Sockets per armor material, per shield and the amethyst cost of each socket. */
	private static void socketGroups(List<Row> result) {
		List<Row> counts = new ArrayList<>();
		String[] icons = {"minecraft:leather_chestplate", "minecraft:copper_chestplate", "minecraft:chainmail_chestplate",
				"minecraft:iron_chestplate", "minecraft:golden_chestplate", "minecraft:diamond_chestplate",
				"minecraft:netherite_chestplate", "gearbalance:titanium_chestplate"};
		for (int i = 0; i < GearConfig.SOCKET_MATERIALS.size(); i++) {
			String material = GearConfig.SOCKET_MATERIALS.get(i);
			counts.add(numberRow(icons[i], t("gearbalance.values.material." + material),
					t("gearbalance.values.max_sockets.tip"),
					List.of(whole("sockets." + material, () -> GearConfig.SOCKETS_PER_MATERIAL.get(material),
							GearConfig.defaultSockets(material), 0, GearConfig.MAX_SOCKETS,
							v -> GearConfig.SOCKETS_PER_MATERIAL.put(material, v), "")),
					SOCKETS));
		}
		result.add(subgroup("max_sockets", t("gearbalance.values.heading.max_sockets"),
				t("gearbalance.values.max_sockets.tip"), SOCKETS, counts));
		List<Row> shieldCounts = new ArrayList<>();
		for (String material : GearConfig.SHIELD_MATERIALS) {
			String icon = shieldIcon(material, true);
			shieldCounts.add(numberRow(icon, itemName(icon), t("gearbalance.values.max_sockets.tip"),
					List.of(whole("shield_sockets." + material, () -> GearConfig.SHIELD_SOCKETS.get(material),
							GearConfig.defaultShieldSockets(material), 0, GearConfig.MAX_SOCKETS,
							v -> GearConfig.SHIELD_SOCKETS.put(material, v), "")),
					SOCKETS));
		}
		result.add(subgroup("shield_sockets", t("gearbalance.config.section.shield_sockets"),
				t("gearbalance.values.max_sockets.tip"), SOCKETS, shieldCounts));
		List<Row> costs = new ArrayList<>();
		for (int i = 0; i < GearConfig.MAX_SOCKETS; i++) {
			int index = i;
			costs.add(numberRow("minecraft:amethyst_shard", t("gearbalance.values.socket_cost", i + 1),
					t("gearbalance.values.socket_cost.tip"),
					List.of(whole("cost." + i, () -> GearConfig.SOCKET_COST[index],
							GearConfig.defaultSocketCost(index), 1, 64, v -> GearConfig.SOCKET_COST[index] = v, "")),
					SOCKETS));
		}
		result.add(subgroup("socket_cost", t("gearbalance.values.heading.socket_cost"),
				t("gearbalance.values.socket_cost.tip"), SOCKETS, costs));
	}

	/** Gems from ores and chests (only while sockets are on). */
	private static void gemLoot(List<Row> result) {
		List<Row> ores = new ArrayList<>();
		for (String type : GearConfig.ROUGH_TYPES) {
			ores.add(numberRow(GearBalance.MOD_ID + ":" + type, t("gearbalance.values.ore." + type),
					t("gearbalance.values.ore.tip"),
					List.of(percent("ore." + type, () -> GearConfig.ORE_DROP_CHANCE.get(type),
							GearConfig.defaultOreChance(type), v -> GearConfig.ORE_DROP_CHANCE.put(type, v))),
					SOCKETS));
		}
		List<Row> oreRows = new ArrayList<>();
		oreRows.add(subgroup("ore_chances", t("gearbalance.config.sub.ore_chances"), t("gearbalance.values.ore.tip"),
				SOCKETS, ores));
		oreRows.add(numberRow("minecraft:redstone_ore", t("gearbalance.config.drop.ore"),
				t("gearbalance.config.drop.ore.tip"),
				List.of(whole("drop.ore", () -> GearConfig.oreDropMax, 1, 1, GearConfig.MAX_DROP,
						v -> GearConfig.oreDropMax = v, "")),
				SOCKETS));
		result.add(group("ores", t("gearbalance.values.heading.ores"), t("gearbalance.values.ore.tip"),
				SOCKETS, oreRows, null));
		List<Row> chests = new ArrayList<>();
		List<Row> stages = new ArrayList<>();
		String[] icons = {"minecraft:chest", "minecraft:ender_eye", "minecraft:netherite_scrap"};
		for (int i = 0; i < GearConfig.LOOT_TIERS.size(); i++) {
			String tier = GearConfig.LOOT_TIERS.get(i);
			chests.add(numberRow(icons[i], t("gearbalance.values.chest." + tier),
					t("gearbalance.values.chest." + tier + ".tip"),
					List.of(percent("chest." + tier, () -> GearConfig.CHEST_CHANCE.get(tier),
							GearConfig.defaultChestChance(tier), v -> GearConfig.CHEST_CHANCE.put(tier, v))),
					SOCKETS));
		}
		String[] stageIcons = {"gearbalance:ruby", "gearbalance:chipped_bold_ruby", "gearbalance:cut_bold_ruby",
				"gearbalance:flawless_bold_ruby"};
		for (int i = 0; i < GearConfig.DROP_STAGES.size(); i++) {
			String stage = GearConfig.DROP_STAGES.get(i);
			stages.add(numberRow(stageIcons[i], t("gearbalance.config.drop." + stage),
					t("gearbalance.config.drop.chest.tip"),
					List.of(whole("drop." + stage, () -> GearConfig.CHEST_DROP_MAX.get(stage), 1, 1,
							GearConfig.MAX_DROP, v -> GearConfig.CHEST_DROP_MAX.put(stage, v), "")),
					SOCKETS));
		}
		List<Row> chestRows = new ArrayList<>();
		chestRows.add(subgroup("chest_chances", t("gearbalance.config.sub.chest_chances"),
				t("gearbalance.config.section.chests.tip"), SOCKETS, chests));
		chestRows.add(subgroup("chest_most", t("gearbalance.config.sub.chest_most"),
				t("gearbalance.config.drop.chest.tip"), SOCKETS, stages));
		result.add(group("chests", t("gearbalance.values.heading.chests"),
				t("gearbalance.config.section.chests.tip"), SOCKETS, chestRows, null));
	}

	/** Hint of a two-bonus gem: values per cut with the share applied (gem color), empty line, where they are set. */
	private static Component pairHint(GemSorts.Sort sort) {
		MutableComponent hint = Component.empty();
		String[] cuts = {"chipped", "cut", "flawless"};
		List<GemValues.Bonus> bonuses = GemValues.of(sort);
		for (int cut = 1; cut <= 3; cut++) {
			MutableComponent line = t("gearbalance.values.column." + cuts[cut - 1]).append(": ");
			for (int i = 0; i < bonuses.size(); i++) {
				if (i > 0) {
					line.append(", ");
				}
				line.append(GemValues.line(bonuses.get(i).kind(), bonuses.get(i).amount(cut)));
			}
			hint.append(GearBalanceClient.gemColored(sort.type(), line)).append("\n");
		}
		return hint.append("\n").append(t("gearbalance.config.gem.pair.tip"));
	}

	/** The sorts of a gem type with their bonuses; single-bonus sorts hold the values per cut. */
	private static List<Row> gemRows(String type) {
		List<Row> result = new ArrayList<>();
		for (GemSorts.Sort sort : GemSorts.of(type)) {
			MutableComponent name = t("gearbalance.sort." + sort.key() + "_" + sort.type());
			MutableComponent bonuses = Component.empty();
			for (int i = 0; i < sort.bonuses().size(); i++) {
				GemBonuses.Bonus bonus = GemBonuses.get(sort.bonuses().get(i));
				if (i > 0) {
					bonuses.append(" + ");
				}
				bonuses.append(GemValues.name(bonus));
			}
			// The bonuses in the color of their gem
			name.append(GearBalanceClient.gemColored(sort.type(), Component.literal(" (").append(bonuses).append(")")));
			if (sort.pair()) {
				result.add(numberRow(null, name, pairHint(sort), List.of(), SOCKETS));
				continue;
			}
			GemBonuses.Bonus bonus = GemBonuses.get(sort.bonuses().get(0));
			List<Cell> cells = new ArrayList<>();
			for (int cut = 1; cut <= 3; cut++) {
				int index = cut - 1;
				cells.add(value("gem." + bonus.id() + "." + cut, () -> GearConfig.GEM_VALUES.get(bonus.id())[index],
						GearConfig.defaultGemValue(bonus.id(), cut), 0, bonus.max(),
						v -> GearConfig.GEM_VALUES.get(bonus.id())[index] = v, bonus.percent() ? "%" : "", false));
			}
			result.add(numberRow(null, name, GemValues.description(bonus), cells, SOCKETS));
		}
		return result;
	}

	/** Set bonus per armor material: type button and strength. */
	private static List<Row> setRows(int indent) {
		List<Row> result = new ArrayList<>();
		for (Material material : Material.values()) {
			if (!material.hasArmor()) {
				continue;
			}
			Cell strength = new Cell("set." + material.key, () -> GearConfig.getSetStrength(material), 0, 0,
					GearConfig.getSetType(material).max, 0,
					v -> GearConfig.setSet(material, GearConfig.getSetType(material), v),
					() -> GearConfig.getSetStrength(material) > 0, "", "gearbalance.config.column.strength",
					t("gearbalance.config.column.strength.hint"), false, false);
			List<Cell> cells = new ArrayList<>();
			cells.add(null);
			cells.add(null);
			cells.add(strength);
			result.add(new Row(material.armorIcon(), null, t(material.nameKey), t("gearbalance.tab.sets.tip"), cells, null,
					null, indent, SET_BONUSES, List.of(), null, () -> GearConfig.resetSet(material), material));
		}
		return result;
	}

	/** Values of the items of one kind, or the values per material. */
	private static void gearRows(Sub of, List<Row> result) {
		if (of == Sub.MATERIALS) {
			for (Material material : Material.values()) {
				List<Cell> cells = new ArrayList<>();
				for (MaterialStat stat : MaterialStat.values()) {
					cells.add(stat.appliesTo(material) ? new Cell(material.key + "." + stat.key,
							() -> GearConfig.getMaterial(stat, material), stat.vanilla, stat.min, stat.max, stat.step,
							v -> GearConfig.setMaterial(stat, material, v), () -> GearConfig.isChanged(stat, material),
							"", "gearbalance.config.column." + stat.key, null, true, false) : null);
				}
				// Chestplate first, then pickaxe. A material with only one of them shows it in front.
				String armor = material.armorIcon();
				result.add(new Row(armor != null ? armor : material.toolIcon(), armor != null ? material.toolIcon() : null,
						t(material.nameKey), null, cells, null, null, 0, null, List.of(), null,
						() -> GearConfig.resetMaterial(material), null));
			}
			return;
		}
		List<Stat> stats = Stat.forKind(of.kind);
		for (GearItems.Entry entry : GearItems.all()) {
			if (entry.kind() != of.kind) {
				continue;
			}
			List<Cell> cells = new ArrayList<>();
			for (Stat stat : stats) {
				if (!stat.appliesTo(entry)) {
					cells.add(null);
					continue;
				}
				cells.add(new Cell(entry.id() + "." + stat.key, () -> GearConfig.getEffective(entry, stat),
						stat.get(VanillaValues.get(entry)), stat.min, stat.max(entry), stat == Stat.DURABILITY ? 1 : 0,
						v -> GearConfig.setOverride(entry, stat, v), () -> GearConfig.getOverride(entry.id(), stat) != null,
						"", "gearbalance.stat." + stat.key, null, true, false));
			}
			result.add(new Row(entry.id(), null, itemName(entry.id()), null, cells, null, null, 0, null, List.of(), null,
					() -> GearConfig.resetItem(entry.id()), null));
		}
	}

	/** Column headers of a tab: translation key, the hover text uses the same key and ".tip". */
	private static List<String> headers(Sub of) {
		return switch (of) {
			case GEMS -> List.of("gearbalance.values.column.chipped", "gearbalance.values.column.cut",
					"gearbalance.values.column.flawless");
			case LOOT -> List.of("gearbalance.values.column.chance");
			case MATERIALS -> {
				List<String> keys = new ArrayList<>();
				for (MaterialStat stat : MaterialStat.values()) {
					keys.add("gearbalance.config.column." + stat.key);
				}
				yield keys;
			}
			case TOOL, WEAPON, ARMOR, OTHER -> {
				List<String> keys = new ArrayList<>();
				for (Stat stat : Stat.forKind(of.kind)) {
					keys.add("gearbalance.stat." + stat.key);
				}
				yield keys;
			}
			default -> List.of("gearbalance.values.column.value");
		};
	}

	/** Gear headers have a short form for the column and an explanation; the others are short already. */
	private static boolean gearHeaders(Sub of) {
		return of.main == Main.GEAR;
	}

	/** Every row of a tab with all sections open. */
	private static List<Row> allRows(Sub of) {
		List<Row> result = new ArrayList<>();
		for (Row row : sections(of)) {
			addAll(row, result);
		}
		return result;
	}

	private static void addAll(Row row, List<Row> into) {
		into.add(row);
		for (Row child : row.children()) {
			addAll(child, into);
		}
	}

	private static Component itemName(String id) {
		return Component.translatable(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).getDescriptionId());
	}

	// ---------------------------------------------------------------- state of the values

	/** Counts the changes and, after a save, remembers the values in the file. */
	private void refresh() {
		boolean takeSaved = savedAt != GearConfig.saveCount();
		if (takeSaved) {
			saved.clear();
			savedAt = GearConfig.saveCount();
		}
		int count = 0;
		for (Sub s : Sub.values()) {
			for (Row row : allRows(s)) {
				if (row.toggle() != null && row.toggle().changed()) {
					count++;
				}
				for (Cell cell : row.cells()) {
					if (cell == null) {
						continue;
					}
					if (takeSaved) {
						saved.put(cell.id(), cell.current().getAsDouble());
					}
					if (cell.changed().getAsBoolean()) {
						count++;
					}
				}
			}
		}
		changes = count;
	}

	private int color(Cell cell) {
		if (!cell.changed().getAsBoolean()) {
			return WHITE;
		}
		Double inFile = saved.get(cell.id());
		return inFile != null && Math.abs(inFile - cell.current().getAsDouble()) < 1.0E-6 ? GREEN : YELLOW;
	}

	/** Locked: the settings come from a server and the player is no operator, or the row needs a switch that is off. */
	private static boolean locked(Row row) {
		return serverLocked(row) || (row.needs() != null && !row.needs().current().getAsBoolean()) || vanillaShield(row);
	}

	/** The vanilla shield under Gear › Other while the new shields replace it. */
	private static boolean vanillaShield(Row row) {
		return "minecraft:shield".equals(row.icon()) && GearConfig.shieldsActive();
	}

	/** Locked server settings; own view options stay open. A switchless heading is locked only if all its rows are. */
	private static boolean serverLocked(Row row) {
		if (!ServerSettingsClient.locked()) {
			return false;
		}
		if (row.heading() && row.toggle() == null && !row.children().isEmpty()) {
			return row.children().stream().allMatch(ConfigScreen::serverLocked);
		}
		return !clientOnly(row.toggle()) && !clientOnly(row.needs());
	}

	private static boolean clientOnly(Toggle toggle) {
		return toggle != null && CLIENT_ONLY.contains(toggle);
	}

	private static Component lockedTip(Row row) {
		if (serverLocked(row)) {
			return t("gearbalance.server.locked.tip").withStyle(ChatFormatting.GOLD);
		}
		if (vanillaShield(row)) {
			return t("gearbalance.config.vanilla_shield_locked").withStyle(ChatFormatting.GOLD);
		}
		return t("gearbalance.config.locked", t(row.needs().nameKey())).withStyle(ChatFormatting.GOLD);
	}

	private void buildRows() {
		rows.clear();
		for (Row section : sections(sub())) {
			addOpen(section);
		}
	}

	/** A row and, if it is an open heading, its rows (sub menus inside the same way). */
	private void addOpen(Row row) {
		rows.add(row);
		if (row.heading() && OPEN.contains(row.section())) {
			for (Row child : row.children()) {
				addOpen(child);
			}
		}
	}

	// ---------------------------------------------------------------- layout

	private int tableLeft() {
		return this.width / 2 - TABLE_WIDTH / 2;
	}

	private int tableRight() {
		return tableLeft() + TABLE_WIDTH;
	}

	private int columnX(int column) {
		return tableLeft() + ICON_WIDTH + NAME_WIDTH + column * COLUMN_WIDTH;
	}

	/** x of a value column of this tab (0 = first value column). */
	private int valueX(int column) {
		return columnX(sub().firstColumn() + column);
	}

	/** Gear rows start after their icons; switch rows at the left edge, rows of a group one step in. */
	private int nameLeft(Row row) {
		if (sub().main == Main.GEAR) {
			return tableLeft() + ICON_WIDTH * (sub() == Sub.MATERIALS ? 2 : 1);
		}
		return tableLeft() + 4 + row.indent() * INDENT + (row.icon() != null ? ICON_WIDTH : 0);
	}

	private int scroll() {
		return SCROLL.getOrDefault(sub(), 0);
	}

	private void setScroll(int value) {
		SCROLL.put(sub(), Math.max(0, Math.min(maxScroll(), value)));
	}

	private int maxScroll() {
		return Math.max(0, rows.size() - visibleRows);
	}

	private int trackHeight() {
		return visibleRows * ROW_HEIGHT;
	}

	private int barHeight() {
		return Math.max(10, trackHeight() * visibleRows / Math.max(1, rows.size()));
	}

	// ---------------------------------------------------------------- widgets

	@Override
	protected void init() {
		clearTips();
		refresh();
		buildRows();
		visibleRows = Math.max(3, (this.height - TABLE_TOP - BOTTOM_SPACE) / ROW_HEIGHT);
		setScroll(scroll());

		// Main tabs
		Main[] mains = Main.values();
		int mainWidth = Math.min(120, (this.width - 20) / mains.length - 4);
		int mainLeft = this.width / 2 - (mains.length * (mainWidth + 4) - 4) / 2;
		for (int i = 0; i < mains.length; i++) {
			Main target = mains[i];
			Button button = Button.builder(t(target.key()), b -> {
				main = target;
				this.rebuildWidgets();
			}).bounds(mainLeft + i * (mainWidth + 4), MAIN_TAB_Y, mainWidth, 20).build();
			button.active = target != main;
			this.addRenderableWidget(tip(button, t(target.key()), t(target.key() + ".tip").withStyle(ChatFormatting.GRAY)));
		}

		// Sub tabs of the open main tab
		List<Sub> subs = Sub.of(main);
		int subWidth = Math.min(90, (this.width - 20) / subs.size() - 4);
		int subLeft = this.width / 2 - (subs.size() * (subWidth + 4) - 4) / 2;
		for (int i = 0; i < subs.size(); i++) {
			Sub target = subs.get(i);
			Button button = Button.builder(t(target.key), b -> {
				SUB_OF.put(main, target);
				this.rebuildWidgets();
			}).bounds(subLeft + i * (subWidth + 4), SUB_TAB_Y, subWidth, 20).build();
			button.active = target != sub();
			this.addRenderableWidget(tip(button, t(target.key), t(target.key + ".tip").withStyle(ChatFormatting.GRAY)));
		}

		// Rows: switches, set bonus types, number boxes, reset buttons
		resetButtons.clear();
		for (int r = 0; r < visibleRows; r++) {
			int index = scroll() + r;
			if (index >= rows.size()) {
				break;
			}
			Row row = rows.get(index);
			int y = TABLE_TOP + r * ROW_HEIGHT;
			boolean locked = locked(row);

			if (row.toggle() != null) {
				Button button = toggleButton(row.toggle(), valueX(0), y + 1, locked);
				Component hint = row.hint().copy().withStyle(ChatFormatting.GRAY);
				this.addRenderableWidget(locked ? tip(button, row.name(), hint, lockedTip(row)) : tip(button, row.name(), hint));
			}
			if (row.setMaterial() != null) {
				Material material = row.setMaterial();
				BonusType current = GearConfig.getSetType(material);
				CycleButton<BonusType> type = CycleButton.builder(
								(BonusType b) -> t("gearbalance.bonus." + b.key), current)
						.withValues(BonusType.values())
						.displayOnlyValue()
						.create(valueX(0), y + 1, 2 * COLUMN_WIDTH - 6, ROW_HEIGHT - 2,
								t("gearbalance.config.column.bonus"), (button, value) -> {
									GearConfig.setSet(material, value, GearConfig.getSetStrength(material));
									this.rebuildWidgets();
								});
				type.active = !locked;
				this.addRenderableWidget(tip(type, t("gearbalance.bonus." + current.key),
						t("gearbalance.bonus." + current.key + ".tip").withStyle(ChatFormatting.GRAY)));
			}
			for (int c = 0; c < row.cells().size(); c++) {
				Cell cell = row.cells().get(c);
				if (cell == null) {
					continue;
				}
				if (cell.slider()) {
					CellSlider slider = new CellSlider(valueX(c), y + 1, SLIDER_WIDTH, ROW_HEIGHT - 2, cell);
					slider.active = !locked;
					this.addRenderableWidget(tip(slider, boxTip(row, cell, locked).toArray(Component[]::new)));
					continue;
				}
				EditBox box = new EditBox(this.font, valueX(c), y + 2, BOX_WIDTH, ROW_HEIGHT - 4, row.name());
				box.setMaxLength(9);
				box.setValue(format(cell.current().getAsDouble()));
				box.setTextColor(color(cell));
				if (locked) {
					box.setEditable(false);
					box.setTextColorUneditable(LOCKED);
				}
				box.setResponder(text -> {
					Double parsed = parse(text);
					if (parsed == null || parsed < cell.min() || parsed > cell.max() || !fitsStep(parsed, cell.step())) {
						box.setTextColor(RED);
						return;
					}
					cell.setter().accept(parsed);
					box.setTextColor(color(cell));
					refresh();
				});
				this.addRenderableWidget(tip(box, boxTip(row, cell, locked).toArray(Component[]::new)));
			}

			boolean hasValues = row.toggle() != null || row.heading() || row.reset() != null
					|| row.cells().stream().anyMatch(cell -> cell != null);
			if (!hasValues) {
				continue;
			}
			Button reset = Button.builder(Component.empty(), b -> {
				resetRow(row);
				this.rebuildWidgets();
			}).bounds(columnX(MAX_COLUMNS) + 2, y, RESET_WIDTH, ROW_HEIGHT).build();
			reset.active = !locked;
			Component resetTip = t(sub().main == Main.GEAR ? "gearbalance.config.reset_row"
					: row.heading() ? "gearbalance.config.reset_section" : "gearbalance.values.reset");
			this.addRenderableWidget(locked ? tip(reset, resetTip, lockedTip(row)) : tip(reset, resetTip));
			resetButtons.add(new int[] {columnX(MAX_COLUMNS) + 2, y, locked ? 1 : 0});
		}

		// Bottom buttons
		int buttonWidth = 110;
		int bottomLeft = this.width / 2 - (3 * (buttonWidth + 4) - 4) / 2;
		int by = this.height - 28;
		this.addRenderableWidget(tip(Button.builder(t("gearbalance.menu.reset_all"), b -> {
			// Ask first: this removes every change at once
			BooleanConsumer answer = yes -> {
				if (yes) {
					GearConfig.resetAll();
					GearConfig.resetSwitches();
					GearConfig.resetValues();
				}
				this.minecraft.setScreenAndShow(this);
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer, t("gearbalance.menu.reset_all.title"),
					t("gearbalance.menu.reset_all.message"), t("gui.yes"), t("gui.no")));
		}).bounds(bottomLeft, by, buttonWidth, 20).build(),
				t("gearbalance.menu.reset_all"), t("gearbalance.menu.reset_all.tip").withStyle(ChatFormatting.GRAY)))
				.active = !ServerSettingsClient.locked();
		this.addRenderableWidget(tip(Button.builder(t("gearbalance.config.profiles"),
				b -> this.minecraft.setScreenAndShow(new ProfilesScreen(this)))
				.bounds(bottomLeft + buttonWidth + 4, by, buttonWidth, 20).build(),
				t("gearbalance.config.profiles"), t("gearbalance.menu.profiles.tip").withStyle(ChatFormatting.GRAY)));
		this.addRenderableWidget(Button.builder(t("gui.done"), b -> this.onClose())
				.bounds(bottomLeft + 2 * (buttonWidth + 4), by, buttonWidth, 20).build());
	}

	/** Explanation of a number box: name, default or vanilla value, allowed range. */
	private static List<Component> boxTip(Row row, Cell cell, boolean locked) {
		List<Component> lines = new ArrayList<>();
		lines.add(cell.headerKey() != null ? t(cell.headerKey()) : row.name());
		if (cell.hint() != null) {
			lines.add(cell.hint().copy().withStyle(ChatFormatting.GRAY));
		}
		if (cell.gear()) {
			lines.add(t("gearbalance.config.vanilla", format(cell.standard())));
			lines.add(t("gearbalance.config.range", format(cell.min()), format(cell.max())).withStyle(ChatFormatting.GRAY));
		} else {
			lines.add(t("gearbalance.values.default", format(cell.standard()) + cell.unit()));
			lines.add(t("gearbalance.values.range", format(cell.min()) + cell.unit(), format(cell.max()) + cell.unit())
					.withStyle(ChatFormatting.GRAY));
		}
		if (locked) {
			lines.add(lockedTip(row));
		}
		return lines;
	}

	private Button toggleButton(Toggle toggle, int x, int y, boolean locked) {
		boolean on = toggle.current().getAsBoolean();
		Component label = t(on ? "options.on" : "options.off")
				.withStyle(locked ? ChatFormatting.GRAY : on ? ChatFormatting.GREEN : ChatFormatting.RED);
		Button button = Button.builder(label, b -> {
			// Titanium off: ask first, its ore and recipes stop (the items stay)
			if (toggle == TITANIUM_SWITCH && toggle.current().getAsBoolean()) {
				BooleanConsumer answer = yes -> {
					if (yes) {
						toggle.setter().accept(false);
					}
					this.minecraft.setScreenAndShow(this);
				};
				this.minecraft.setScreenAndShow(new ConfirmScreen(answer, t("gearbalance.titanium.off.title"),
						t("gearbalance.titanium.off.message"), t("gui.yes"), t("gui.no")));
				return;
			}
			toggle.setter().accept(!toggle.current().getAsBoolean());
			this.rebuildWidgets();
		}).bounds(x, y, 2 * COLUMN_WIDTH - 6, ROW_HEIGHT - 2).build();
		button.active = !locked;
		return button;
	}

	/** A row with an own reset runs it; otherwise its switch, its boxes and all its rows go to the default. */
	private static void resetRow(Row row) {
		if (row.reset() != null) {
			row.reset().run();
		} else {
			if (row.toggle() != null) {
				row.toggle().reset();
			}
			for (Cell cell : row.cells()) {
				if (cell != null) {
					cell.reset();
				}
			}
		}
		for (Row child : row.children()) {
			resetRow(child);
		}
	}

	// ---------------------------------------------------------------- mouse

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && maxScroll() > 0) {
			int next = Math.max(0, Math.min(maxScroll(), scroll() + (scrollY > 0 ? -1 : 1)));
			if (next != scroll()) {
				setScroll(next);
				this.rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean overBar(double mouseX, double mouseY) {
		int left = tableRight() + 1;
		return maxScroll() > 0 && mouseX >= left && mouseX < left + BAR_GRAB
				&& mouseY >= TABLE_TOP && mouseY < TABLE_TOP + trackHeight();
	}

	/** Moves the scrollbar so that its middle sits under the mouse. */
	private void dragBarTo(double mouseY) {
		int free = trackHeight() - barHeight();
		if (free <= 0) {
			return;
		}
		double fraction = (mouseY - TABLE_TOP - barHeight() / 2.0) / free;
		int next = (int) Math.round(Math.max(0, Math.min(1, fraction)) * maxScroll());
		if (next != scroll()) {
			setScroll(next);
			this.rebuildWidgets();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overBar(event.x(), event.y())) {
			draggingBar = true;
			dragBarTo(event.y());
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			// A click on the name of a heading opens or closes its settings
			int r = (int) Math.floor((event.y() - TABLE_TOP) / ROW_HEIGHT);
			int index = scroll() + r;
			if (event.y() >= TABLE_TOP && r < visibleRows && index < rows.size()) {
				Row row = rows.get(index);
				if (row.heading() && row.collapsible() && event.x() >= tableLeft() && event.x() < valueX(0) - 4) {
					if (!OPEN.remove(row.section())) {
						OPEN.add(row.section());
					}
					this.rebuildWidgets();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingBar) {
			dragBarTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingBar && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			draggingBar = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// The vanilla "left button pressed" flag is not updated while a screen is open
		if (draggingBar) {
			dragBarTo(mouseY);
		}
		int left = tableLeft();
		int right = tableRight();

		// Row stripes behind the widgets
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			int y = TABLE_TOP + r * ROW_HEIGHT;
			boolean heading = rows.get(scroll() + r).heading();
			graphics.fill(left, y, right, y + ROW_HEIGHT, heading ? HEADING : r % 2 == 0 ? ROW_DARK : ROW_LIGHT);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		MutableComponent title = this.title.copy().append(t("gearbalance.config.changes", changes)
				.withStyle(ChatFormatting.GRAY));
		if (!GearConfig.enabled) {
			title.append(t("gearbalance.config.disabled").withStyle(ChatFormatting.RED));
		}
		if (GearConfig.serverMode()) {
			title.append(t(GearConfig.serverCanEdit() ? "gearbalance.server.editing" : "gearbalance.server.locked")
					.withStyle(ChatFormatting.GOLD));
		}
		drawFittedCentered(graphics, title, this.width / 2, 8, this.width - 20, WHITE);

		// Column headers, hover for an explanation
		List<Component> hovered = null;
		List<String> headers = headers(sub());
		// Plain "Setting / Value" says nothing: only tables with own columns get a header
		boolean plain = sub().main != Main.GEAR && headers.equals(List.of("gearbalance.values.column.value"));
		if (plain) {
			headers = List.of();
		} else {
			String first = sub() == Sub.MATERIALS ? "gearbalance.config.column.material"
					: sub().main == Main.GEAR ? "gearbalance.config.column.item" : "gearbalance.values.column.setting";
			graphics.text(this.font, t(first), sub().main == Main.GEAR ? nameLeft(null) : left + 4, HEADER_Y, GREY);
		}
		for (int c = 0; c < headers.size(); c++) {
			String key = headers.get(c);
			int x = valueX(c);
			Component header = t(gearHeaders(sub()) ? key + ".short" : key);
			if (sub() == Sub.GEMS) {
				// Cut columns get the shape of their cut: triangle, rhombus, round
				header = GearBalanceClient.symbolLine(GearBalanceClient.gemSymbol("sapphire", c + 1), header);
			}
			// Centered over the value box below it
			drawFittedCentered(graphics, header, x + BOX_WIDTH / 2, HEADER_Y, COLUMN_WIDTH - 4, GREY);
			if (gearHeaders(sub()) && mouseX >= x && mouseX < x + COLUMN_WIDTH && mouseY >= HEADER_Y - 2
					&& mouseY < HEADER_Y + 10) {
				hovered = List.of(t(key), t(key + ".tip").withStyle(ChatFormatting.GRAY));
			}
		}
		if (!plain) {
			graphics.horizontalLine(left, right - 1, TABLE_TOP - 2, DARK_GREY);
		}

		// Icons and names; hovering a name explains the row
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			Row row = rows.get(scroll() + r);
			int y = TABLE_TOP + r * ROW_HEIGHT;
			int nameY = y + (ROW_HEIGHT - 8) / 2;
			boolean locked = locked(row);
			int nameX = nameLeft(row);
			// Rows without values or buttons (e.g. aquamarine and onyx pairs) may use the whole width
			boolean wide = row.cells().isEmpty() && row.toggle() == null && row.label() == null
					&& row.setMaterial() == null;
			int nameWidth = (wide ? right : valueX(0)) - nameX - 4;
			if (sub().main == Main.GEAR) {
				drawItem(graphics, row.icon(), left + 2, y + 3);
				drawItem(graphics, row.icon2(), left + 2 + ICON_WIDTH, y + 3);
			} else if (row.icon() != null) {
				drawItem(graphics, row.icon(), nameX - ICON_WIDTH, y + 3);
			}
			Component shown = row.name();
			if (row.heading()) {
				String arrow = !row.collapsible() ? "  " : OPEN.contains(row.section()) ? "▼ " : "▶ ";
				// Gem names stay in the normal font so they read like the rows below them
				shown = sub() == Sub.GEMS ? Component.literal(arrow).append(row.name())
						: Component.literal(arrow).append(row.name()).withStyle(ChatFormatting.BOLD);
			}
			drawFitted(graphics, shown, nameX, nameY, nameWidth, locked ? LOCKED : WHITE);
			if (row.label() != null) {
				drawFitted(graphics, row.label(), valueX(0), nameY, 2 * COLUMN_WIDTH - 6, GREY);
			}
			for (int c = 0; c < row.cells().size(); c++) {
				if (row.cells().get(c) == null && row.setMaterial() == null) {
					graphics.text(this.font, Component.literal("–"), valueX(c) + BOX_WIDTH / 2 - 2, nameY, DARK_GREY);
				}
			}
			if (mouseX >= nameX && mouseX < nameX + nameWidth && mouseY >= y && mouseY < y + ROW_HEIGHT) {
				List<Component> lines = new ArrayList<>();
				lines.add(row.name());
				if (row.hint() != null) {
					lines.add(row.hint().copy().withStyle(ChatFormatting.GRAY));
				}
				if (locked) {
					lines.add(lockedTip(row));
				}
				hovered = lines;
			}
		}

		// Reset icons on top of the reset buttons, with a shadow like button text
		int iconSize = RESET_ICON.length * RESET_PIXEL;
		for (int[] pos : resetButtons) {
			int iconX = pos[0] + (RESET_WIDTH - iconSize) / 2;
			int iconY = pos[1] + (ROW_HEIGHT - iconSize) / 2;
			drawIcon(graphics, iconX + 1, iconY + 1, SHADOW);
			drawIcon(graphics, iconX, iconY, pos[2] == 1 ? LOCKED : WHITE);
		}

		// Scrollbar, can be dragged with the mouse
		if (maxScroll() > 0) {
			int barX = right + BAR_LEFT;
			int barY = TABLE_TOP + (trackHeight() - barHeight()) * scroll() / maxScroll();
			boolean hot = draggingBar || overBar(mouseX, mouseY);
			graphics.fill(barX, TABLE_TOP, barX + BAR_WIDTH, TABLE_TOP + trackHeight(), 0x40FFFFFF);
			graphics.fill(barX, barY, barX + BAR_WIDTH, barY + barHeight(), hot ? WHITE : 0xFFC0C0C0);
		}

		String note = switch (sub()) {
			case MATERIALS -> "gearbalance.config.note_materials";
			case TOOL, WEAPON, ARMOR, OTHER -> "gearbalance.config.note";
			case GENERAL, VIEW, GLOW, COMFORT -> "gearbalance.menu.note.options";
			default -> "gearbalance.menu.note.functions";
		};
		// Two lines in full size instead of one shrunk line; the table ends 50 px above the bottom
		List<FormattedCharSequence> noteLines = this.font.split(t(note), this.width - 40);
		if (noteLines.size() == 2) {
			for (int i = 0; i < 2; i++) {
				graphics.text(this.font, noteLines.get(i), (this.width - this.font.width(noteLines.get(i))) / 2,
						this.height - 49 + i * 10, GREY, true);
			}
		} else {
			drawFittedCentered(graphics, t(note), this.width / 2, this.height - 42, this.width - 20, GREY);
		}

		if (hovered != null) {
			showTip(graphics, hovered, mouseX, mouseY);
		} else {
			drawTips(graphics, mouseX, mouseY);
		}
	}

	private void drawItem(GuiGraphicsExtractor graphics, String id, int x, int y) {
		if (id == null) {
			return;
		}
		// Item components are not bound in the main menu, an ItemStack would crash there
		boolean bound = BuiltInRegistries.ITEM.get(Identifier.parse(id))
				.map(Holder.Reference::areComponentsBound).orElse(false);
		if (bound) {
			graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))), x, y);
			return;
		}
		// Main menu: draw the flat item picture instead (most tools, weapons and armor have one)
		Identifier texture = FLAT_ICONS.computeIfAbsent(id, ConfigScreen::flatIcon).orElse(null);
		if (texture != null) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16);
		}
	}

	/** Item pictures found for the main menu, empty when an item has none. */
	private static final Map<String, Optional<Identifier>> FLAT_ICONS = new HashMap<>();

	private static Optional<Identifier> flatIcon(String id) {
		Identifier item = Identifier.parse(id);
		for (String folder : List.of("item", "block")) {
			Identifier texture = Identifier.fromNamespaceAndPath(item.getNamespace(),
					"textures/" + folder + "/" + item.getPath() + ".png");
			if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
				return Optional.of(texture);
			}
		}
		return Optional.empty();
	}

	private static void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int row = 0; row < RESET_ICON.length; row++) {
			String line = RESET_ICON[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					int px = x + col * RESET_PIXEL;
					int py = y + row * RESET_PIXEL;
					graphics.fill(px, py, px + RESET_PIXEL, py + RESET_PIXEL, color);
				}
			}
		}
	}

	// ---------------------------------------------------------------- closing

	/** Saves everything, then: restart (gear values changed), load the world again (recipes changed) or back. */
	@Override
	public void onClose() {
		GearConfig.save();
		Screen back = this.parent;
		if (GearConfig.serverMode()) {
			// Settings of a server: an operator sends the changes, the server saves them and restarts are its matter
			if (!GearConfig.settingsToJson().toString().equals(settingsAtOpen)) {
				ServerSettingsClient.sendToServer();
			}
			this.minecraft.setScreenAndShow(back);
			return;
		}
		if (!GearConfig.restartSignature().equals(gearAtOpen)) {
			BooleanConsumer answer = quit -> {
				if (quit) {
					this.minecraft.stop();
				} else {
					this.minecraft.setScreenAndShow(back);
				}
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer, t("gearbalance.restart.title"),
					t("gearbalance.restart.message"), t("gearbalance.restart.quit"), t("gearbalance.restart.later")));
			return;
		}
		IntegratedServer server = this.minecraft.getSingleplayerServer();
		// Recipes are read when the world loads (in 26.3 they are a registry, /reload does not change them)
		if (server != null && !recipeState().equals(recipesAtOpen)) {
			String levelId = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
			BooleanConsumer answer = yes -> {
				if (yes) {
					this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
					this.minecraft.createWorldOpenFlows().openWorld(levelId,
							() -> this.minecraft.setScreenAndShow(new TitleScreen()));
				} else {
					this.minecraft.setScreenAndShow(back);
				}
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer, t("gearbalance.reload.title"),
					t("gearbalance.reload.message"), t("gui.yes"), t("gui.no")));
			return;
		}
		this.minecraft.setScreenAndShow(back);
	}

	// ---------------------------------------------------------------- numbers

	/** 0 = any value, otherwise only multiples of step (1 = whole numbers, 0.1 = one decimal). */
	private static boolean fitsStep(double value, double step) {
		if (step <= 0) {
			return true;
		}
		double units = value / step;
		return Math.abs(units - Math.rint(units)) < 1.0E-6;
	}

	/** Accepts "1.6" and "1,6". Returns null for invalid input, also for "NaN" and "Infinity". */
	static Double parse(String text) {
		try {
			double value = Double.parseDouble(text.trim().replace(',', '.'));
			return Double.isFinite(value) ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** 74 instead of 74.0, 1.6 instead of 1.6000000000000001. */
	/** A whole-number cell as a slider: drag, click or arrow keys; the text turns yellow/green like the boxes. */
	private final class CellSlider extends AbstractSliderButton {
		private final Cell cell;

		CellSlider(int x, int y, int width, int height, Cell cell) {
			super(x, y, width, height, Component.empty(),
					(cell.current().getAsDouble() - cell.min()) / (cell.max() - cell.min()));
			this.cell = cell;
			updateMessage();
		}

		private double number() {
			return Math.round(cell.min() + value * (cell.max() - cell.min()));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(format(number()) + cell.unit()).withColor(color(cell)));
		}

		@Override
		protected void applyValue() {
			cell.setter().accept(number());
			refresh();
			updateMessage();
		}
	}

	static String format(double value) {
		double rounded = Math.round(value * 1000.0) / 1000.0;
		if (rounded == Math.rint(rounded)) {
			return String.valueOf((long) rounded);
		}
		return String.format(Locale.ROOT, "%.3f", rounded).replaceAll("0+$", "");
	}
}
