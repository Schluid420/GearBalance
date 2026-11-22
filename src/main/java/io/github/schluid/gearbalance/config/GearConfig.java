package io.github.schluid.gearbalance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings, stored in config/gearbalance.json.
 *
 * Only changed values are written to the file, everything else stays vanilla:
 * "items": { "minecraft:diamond_sword": { "durability": 1951, "damage": 8 } }
 * "materials" holds the values per material: "iron": { "level": 10, "repair": 20, "weight": 2 }
 * "sets" holds the set bonus per material: "iron": { "bonus": "health", "strength": 2 }
 * Function switches (padded armor, upgrades, sockets, shields, bags, view ...) are written like other switches;
 * their numbers (gems, sockets, drop chances, tool uses ...) are in "values".
 */
public final class GearConfig {
	private GearConfig() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "gearbalance.json";

	public static final int PROFILE_COUNT = 3;
	public static final int ANVIL_MAX_COST_LIMIT = 120;
	private static final int VANILLA_ANVIL_MAX_COST = 39;
	private static final int DEFAULT_ZOMBIE_LEATHER_CHANCE = 33;
	private static final int DEFAULT_SHIELD_WEAR_START = 50;
	private static final int DEFAULT_SHIELD_WEAR_MIN = 60;

	public static boolean enabled = true;
	public static boolean tridentRecipe = false;
	public static boolean chainmailRecipes = false;
	/** 1 leather -> 4 rabbit hides. The other way round already exists in vanilla. */
	public static boolean rabbitHideRecipe = false;
	/** Copper, iron, gold and diamond horse armor at the crafting table. */
	public static boolean horseArmorRecipes = false;
	public static boolean rabbitHideArmor = false;
	/** No "Too Expensive!" at the anvil, the cost is capped at anvilMaxCost instead. */
	public static boolean anvilNoLimit = false;
	public static int anvilMaxCost = VANILLA_ANVIL_MAX_COST;
	/** Zombies, husks, drowned and zombie villagers drop leather, chance in percent. */
	public static boolean zombieLeather = false;
	public static int zombieLeatherChance = DEFAULT_ZOMBIE_LEATHER_CHANCE;
	/** Endgame chests: more smithing templates, more valuable materials, cheap filler replaced. */
	public static boolean lootTemplates = false;
	public static boolean lootMaterials = false;
	public static boolean lootNoCheap = false;
	/** No diamonds and golden apples in the chests of the easy overworld places. */
	public static boolean lootOverworld = false;
	/** Set once the note about the official download was shown. */
	public static boolean welcomeShown = false;
	/** Main switch for the set bonuses, the values per material stay saved while it is off. */
	public static boolean setBonuses = false;
	/** Shield wear: below shieldWearStart % durability it blocks less, down to shieldWearMin % at breaking. */
	public static boolean shieldWear = false;
	public static int shieldWearStart = DEFAULT_SHIELD_WEAR_START;
	public static int shieldWearMin = DEFAULT_SHIELD_WEAR_MIN;

	// Switches of the functions. Every function starts switched off, so a new installation plays like vanilla.

	/** Padded armor: sewing needle, padding at the smithing table, cold protection, grindstone. */
	public static boolean paddedArmor = false;
	/** Patching leather armor at the crafting table (leather, thread, sewing needle). */
	public static boolean leatherPatch = false;
	/** Padding horse armor at the crafting table (wool, thread, sewing needle): half fall damage for the horse. */
	public static boolean horsePadding = false;
	/** Upgrades +1 to +3: recipes of the compressed ingots and upgrading at the smithing table. */
	public static boolean upgrades = false;
	/** Sockets: socket chisels and cutting sockets at the smithing table. */
	public static boolean sockets = false;
	/** Gem return: a gem removed at the grindstone may come back one cut lower (needs sockets). */
	public static boolean gemReturn = false;
	/** Gem recut: a gem can be cut into another sort of the same kind and cut at the stonecutter (needs sockets). */
	public static boolean gemRecut = false;
	/** Master smithing plan: the diamond forge hammer and socket chisel need a plan found in the world. */
	public static boolean masterPlan = false;
	/** New shields: recipes of the copper, iron, gold, diamond and netherite shield. */
	public static boolean shields = false;
	/** First person view: a shield in hand is drawn lower. */
	public static boolean shieldLower = false;
	/** First person view: the flames on screen while burning are drawn lower. */
	public static boolean fireLower = false;
	/** Item tooltips with a frame in the material color, picture and name in the head. */
	public static boolean tooltipFrames = false;
	/** Picked up items next to the hotbar / a sparkle on new items in the inventory. Both start switched on. */
	public static boolean pickupDisplay = false;
	public static boolean pickupMarks = false;
	/** Pickup tiles right of the hotbar instead of left. */
	public static boolean pickupRight = false;
	/** Pulsing outline in the hand when every socket of a weapon, tool or shield holds a gem. Starts switched on. */
	public static boolean outlines = false;
	/** First person view: both hands move gently while standing still. */
	public static boolean handIdle = false;
	/** Netherite repair kit (smithing table, made with the master smithing plan). */
	public static boolean repairKit = false;
	/** Gem levers: take a gem out without losing it (needs sockets). */
	public static boolean gemLever = false;
	/** Parrying with a shield. */
	public static boolean parry = false;
	/** Better shields slow you down less while blocking. Applies after a restart. */
	public static boolean shieldSpeed = false;
	/** Bags: a bag slot in the inventory and extra rows above it. */
	public static boolean bags = false;
	/** A short sound when a worn or held item gets low on durability. */
	public static boolean durabilityWarning = false;
	/** Own armor bar (material colors, cracks, darker, blinking, broken); hdArmor: symbols in 4x resolution. */
	public static boolean armorBar = false;
	public static boolean hdArmor = false;
	public static boolean armorColors = false;
	public static boolean armorCracks = false;
	public static boolean armorDarken = false;
	public static boolean armorBlink = false;
	public static boolean armorBroken = false;
	/** Hotbar animations: pop of the selected item, low durability light, item name bounce, animated XP bar. */
	public static boolean hotbarPop = false;
	public static boolean hotbarLowGlow = false;
	public static boolean itemNameBounce = false;
	public static boolean xpAnimated = false;
	/** Hearts in the style of the armor bar; hearts from gear with a silver ring. */
	public static boolean hdHearts = false;
	public static boolean heartRing = false;
	/** Food bar in the style of the hearts; saturation as golden drumsticks. */
	public static boolean hdFood = false;
	public static boolean saturationGold = false;
	/** HD hotbar, selection frame, off hand slot, experience bar and level number. */
	public static boolean hdHud = false;
	/** Smooth scrolling of lists and chat; gliding hotbar selection frame. */
	public static boolean smoothScroll = false;
	public static boolean hotbarGlide = false;
	/** Death: one pile without despawn, lava/void safety, more XP, explosion proof, kept pieces, death point. */
	public static boolean deathPile = false;
	public static boolean deathSafe = false;
	public static boolean deathXp = false;
	public static boolean deathExplosionProof = false;
	public static boolean deathKeepArmor = false;
	public static boolean deathKeepHotbar = false;
	public static boolean deathKeepOffhand = false;
	public static boolean deathPoint = false;
	/** Spawners drop iron and maybe a gem; optionally the whole spawner with Silk Touch. */
	public static boolean spawnerDrops = false;
	public static boolean spawnerWhole = false;
	/** Item tooltip: with Shift, the difference to the worn or held item. */
	public static boolean compareTooltip = false;
	/** Tooltip: the uses left of the smithing tools (hammers, chisels, needle, repair kit, levers). */
	public static boolean toolDurability = false;
	/** Tooltip: durability left of every other damageable item (tools, weapons, armor) as well. */
	public static boolean durabilityAll = false;
	/** Smaller GUI size while the inventory is open, so bag rows and the stats panel fit. */
	public static boolean inventoryScale = false;
	/** Releasing the sprint key stops sprinting at once (sprint mode "Hold"); no double tap sprinting. */
	public static boolean sprintFix = false;
	/** The hotbar hides after some seconds without changing the slot. */
	public static boolean hotbarHide = false;
	/** Hides health, food, armor and experience with the hotbar (otherwise they move down into its place). */
	public static boolean hotbarHideAll = false;
	public static boolean hotbarHideArmor = false;
	public static boolean hotbarHideXp = false;
	/** Seconds without changing the slot until the hotbar hides. */
	public static int hotbarHideSeconds = 5;
	/** Seconds the hidden bars come back after taking damage (0 = not). */
	public static int hotbarDamageSeconds = 10;
	/** Food points (0 to 20) at or below which the bars stay visible (0 = never). */
	public static int hotbarFoodThreshold = 6;
	/** Item names in color by worth: +1 green, +2 blue, +3 purple, all sockets filled gold. Starts switched on. */
	public static boolean nameColors = true;
	/** A tool or weapon that breaks in the hand is replaced by the same item from the inventory. */
	public static boolean toolRefill = false;
	/** Valuable items (enchanted, upgraded or with gems) stop at 1 durability instead of breaking. */
	public static boolean breakProtection = false;
	/** Three armor sets per player, saved and put on with buttons next to the inventory. */
	public static boolean loadouts = false;
	/** Trash bin button in the inventory, keeps the last five deleted items. */
	public static boolean trash = false;
	/** Own length of day and night in the overworld, in real minutes. */
	public static boolean dayNight = false;
	/** Titanium: ore, recipes, the template in end city chests and the lower fall damage. */
	public static boolean titanium = false;
	/** Fall damage each worn titanium armor piece takes off, percent. */
	public static int titaniumFallPercent = 10;
	/** Titanite: chance in percent per mined titanium ore, and how much stronger each cut makes the other gems. */
	public static double titaniteChance = 1.0;
	public static int titaniteBoostChipped = 8;
	public static int titaniteBoostCut = 16;
	public static int titaniteBoost = 25;

	/** Boost of a titanite cut (1 chipped, 2 cut, 3 flawless) in percent. */
	public static int titaniteBoost(int cut) {
		return switch (cut) {
			case 1 -> titaniteBoostChipped;
			case 2 -> titaniteBoostCut;
			case 3 -> titaniteBoost;
			default -> 0;
		};
	}
	/** Items left in a smithing table stay there instead of going back to the player. Starts switched on. */
	public static boolean keepTableItems = false;
	/** Explanation lines in item tooltips ("cut it at the stonecutter" ...). Starts switched on. */
	public static boolean showHints = true;

	/** Padded armor is switched on and the mod is active. */
	public static boolean paddedArmorActive() {
		return enabled && paddedArmor;
	}

	/** Leather patching is switched on and the mod is active. */
	public static boolean leatherPatchActive() {
		return enabled && leatherPatch;
	}

	/** Horse padding is switched on and the mod is active. */
	public static boolean horsePaddingActive() {
		return enabled && horsePadding;
	}

	/** Upgrades are switched on and the mod is active. */
	public static boolean upgradesActive() {
		return enabled && upgrades;
	}

	/** Sockets are switched on and the mod is active. */
	public static boolean socketsActive() {
		return enabled && sockets;
	}

	/** The master smithing plan is needed: switch on, mod active, and an endgame-tool function on (upgrades, gem levers, repair kit). */
	public static boolean masterPlanActive() {
		return enabled && masterPlan && (upgrades || sockets || repairKit || gemLever);
	}

	public static boolean repairKitActive() {
		return enabled && repairKit;
	}

	public static boolean gemLeverActive() {
		return socketsActive() && gemLever;
	}

	public static boolean parryActive() {
		return enabled && parry;
	}

	/** Shield speed is built into the shields when the game starts, so it needs the shields too. */
	public static boolean shieldSpeedActive() {
		return enabled && shields && shieldSpeed;
	}

	public static boolean bagsActive() {
		return enabled && bags;
	}

	/** The new shields are switched on and the mod is active. */
	public static boolean shieldsActive() {
		return enabled && shields;
	}

	/** Items stay in the smithing table: its switch is on and the mod is active. */
	public static boolean keepTableItemsActive() {
		return enabled && keepTableItems;
	}

	/** Gem return is switched on, sockets are on and the mod is active. */
	public static boolean gemReturnActive() {
		return socketsActive() && gemReturn;
	}

	/** A broken tool or weapon in the hand is replaced from the inventory. */
	public static boolean toolRefillActive() {
		return enabled && toolRefill;
	}

	/** Valuable pieces stop at 1 durability instead of breaking. */
	public static boolean breakProtectionActive() {
		return enabled && breakProtection;
	}

	/** Titanium is switched on and the mod is active. */
	public static boolean titaniumActive() {
		return enabled && titanium;
	}

	/** The three gear sets in the inventory. */
	public static boolean loadoutsActive() {
		return enabled && loadouts;
	}

	/** Own length of day and night. */
	public static boolean dayNightActive() {
		return enabled && dayNight;
	}

	/** The trash bin in the inventory. */
	public static boolean trashActive() {
		return enabled && trash;
	}

	/** Gem recut is switched on, sockets are on and the mod is active. */
	public static boolean gemRecutActive() {
		return socketsActive() && gemRecut;
	}

	/** Last loaded HM+ profile (key like "veteran"), empty if none. Active only while the settings still match it. */
	private static String hmProfile = "";
	private static int hmSignature;

	private static Map<String, Map<Stat, Double>> overrides = new LinkedHashMap<>();
	private static Map<MaterialStat, Map<Material, Double>> materials = new EnumMap<>(MaterialStat.class);
	private static Map<Material, SetEntry> sets = new LinkedHashMap<>();
	/** Entries for items and materials of add-ons that are not installed right now, kept unchanged. */
	private static Kept kept = Kept.EMPTY;
	private static final Profile[] PROFILES = new Profile[PROFILE_COUNT];

	public static final class Profile {
		/** Empty = the screen shows "Profile 1", "Profile 2", ... */
		public String name = "";
		/** null while nothing is saved in this slot. */
		Map<String, Map<Stat, Double>> overrides;
		Map<MaterialStat, Map<Material, Double>> materials = new EnumMap<>(MaterialStat.class);
		Map<Material, SetEntry> sets = new LinkedHashMap<>();
		/** Options like loot and recipes, null in profiles saved before they were included. */
		JsonObject switches;
		/** Entries of add-ons that are not installed right now. */
		Kept kept = Kept.EMPTY;
		/** Numbers of the functions (gems, sockets, bags ...), null in profiles saved before they were included. */
		JsonObject values;

		public boolean isEmpty() {
			return overrides == null;
		}

		public int changeCount() {
			return overrides == null ? 0 : count(overrides) + countMaterials(materials) + sets.size()
					+ countSwitches(switches) + countValues(values);
		}
	}

	/** Set bonus of one material. Only stored while it differs from the default (strength 0). */
	public record SetEntry(BonusType type, double strength) {
	}

	/** Entries in "items", "materials" and "sets" of a currently missing add-on; written back unchanged. */
	record Kept(JsonObject items, JsonObject materials, JsonObject sets) {
		static final Kept EMPTY = new Kept(new JsonObject(), new JsonObject(), new JsonObject());

		static Kept from(JsonObject root) {
			return new Kept(unknown(root.get("items"), true), unknown(root.get("materials"), false),
					unknown(root.get("sets"), false));
		}

		private static JsonObject unknown(JsonElement element, boolean items) {
			JsonObject result = new JsonObject();
			if (element != null && element.isJsonObject()) {
				element.getAsJsonObject().entrySet().forEach(e -> {
					boolean known = items ? GearItems.get(e.getKey()) != null : Material.byKey(e.getKey()) != null;
					if (!known) {
						result.add(e.getKey(), e.getValue().deepCopy());
					}
				});
			}
			return result;
		}

		/** Adds the kept entries to the objects that are about to be written. */
		void addTo(JsonObject items, JsonObject materials, JsonObject sets) {
			copyMissing(this.items, items);
			copyMissing(this.materials, materials);
			copyMissing(this.sets, sets);
		}

		private static void copyMissing(JsonObject from, JsonObject to) {
			from.entrySet().forEach(e -> {
				if (!to.has(e.getKey())) {
					to.add(e.getKey(), e.getValue().deepCopy());
				}
			});
		}
	}

	static {
		for (int i = 0; i < PROFILE_COUNT; i++) {
			PROFILES[i] = new Profile();
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	// Item values

	/** The changed value, or null if it is still vanilla. */
	public static Double getOverride(String id, Stat stat) {
		Map<Stat, Double> item = overrides.get(id);
		return item == null ? null : item.get(stat);
	}

	public static boolean hasOverrides(String id) {
		Map<Stat, Double> item = overrides.get(id);
		return item != null && !item.isEmpty();
	}

	public static double getEffective(GearItems.Entry entry, Stat stat) {
		Double value = getOverride(entry.id(), stat);
		return value != null ? value : stat.get(VanillaValues.get(entry));
	}

	/** Setting a value back to vanilla removes it from the file. */
	public static void setOverride(GearItems.Entry entry, Stat stat, Double value) {
		Map<Stat, Double> item = overrides.computeIfAbsent(entry.id(), k -> new EnumMap<>(Stat.class));
		if (value == null || Math.abs(value - stat.get(VanillaValues.get(entry))) < 1.0E-6) {
			item.remove(stat);
		} else {
			item.put(stat, stat.clamp(value, entry));
		}
		if (item.isEmpty()) {
			overrides.remove(entry.id());
		}
	}

	public static void resetItem(String id) {
		overrides.remove(id);
	}

	public static void resetAll() {
		overrides.clear();
		materials.clear();
		sets.clear();
		kept = Kept.EMPTY;
	}

	/** Every feature switch and its numbers back to the default ("Mod active" and the welcome note stay). */
	public static void resetSwitches() {
		applySwitches(DEFAULT_SWITCHES);
	}

	/** The switches as they are after a new installation, to count and reset changes. */
	private static final JsonObject DEFAULT_SWITCHES = defaultSwitches();

	private static JsonObject defaultSwitches() {
		JsonObject o = new JsonObject();
		o.addProperty("setBonuses", false);
		o.addProperty("tridentRecipe", false);
		o.addProperty("chainmailRecipes", false);
		o.addProperty("rabbitHideRecipe", false);
		o.addProperty("horseArmorRecipes", false);
		o.addProperty("rabbitHideArmor", false);
		o.addProperty("anvilNoLimit", false);
		o.addProperty("anvilMaxCost", VANILLA_ANVIL_MAX_COST);
		o.addProperty("zombieLeather", false);
		o.addProperty("zombieLeatherChance", DEFAULT_ZOMBIE_LEATHER_CHANCE);
		o.addProperty("lootTemplates", false);
		o.addProperty("lootMaterials", false);
		o.addProperty("lootNoCheap", false);
		o.addProperty("lootOverworld", false);
		o.addProperty("shieldWear", false);
		o.addProperty("shieldWearStart", DEFAULT_SHIELD_WEAR_START);
		o.addProperty("shieldWearMin", DEFAULT_SHIELD_WEAR_MIN);
		o.addProperty("paddedArmor", false);
		o.addProperty("leatherPatch", false);
		o.addProperty("horsePadding", false);
		o.addProperty("upgrades", false);
		o.addProperty("sockets", false);
		o.addProperty("gemReturn", false);
		o.addProperty("gemRecut", false);
		o.addProperty("masterPlan", false);
		o.addProperty("showHints", true);
		o.addProperty("keepTableItems", false);
		o.addProperty("shields", false);
		o.addProperty("shieldLower", false);
		o.addProperty("fireLower", false);
		o.addProperty("tooltipFrames", false);
		o.addProperty("pickupDisplay", false);
		o.addProperty("pickupMarks", false);
		o.addProperty("pickupRight", false);
		o.addProperty("outlines", false);
		o.addProperty("handIdle", false);
		o.addProperty("durabilityWarning", false);
		o.addProperty("armorBar", false);
		o.addProperty("hdArmor", false);
		o.addProperty("armorColors", false);
		o.addProperty("armorCracks", false);
		o.addProperty("armorDarken", false);
		o.addProperty("armorBlink", false);
		o.addProperty("armorBroken", false);
		o.addProperty("hotbarPop", false);
		o.addProperty("hotbarLowGlow", false);
		o.addProperty("itemNameBounce", false);
		o.addProperty("xpAnimated", false);
		o.addProperty("hdHearts", false);
		o.addProperty("heartRing", false);
		o.addProperty("hdFood", false);
		o.addProperty("saturationGold", false);
		o.addProperty("hdHud", false);
		o.addProperty("smoothScroll", false);
		o.addProperty("hotbarGlide", false);
		o.addProperty("deathPile", false);
		o.addProperty("deathSafe", false);
		o.addProperty("deathXp", false);
		o.addProperty("deathExplosionProof", false);
		o.addProperty("deathKeepArmor", false);
		o.addProperty("deathKeepHotbar", false);
		o.addProperty("deathKeepOffhand", false);
		o.addProperty("deathPoint", false);
		o.addProperty("spawnerDrops", false);
		o.addProperty("spawnerWhole", false);
		o.addProperty("repairKit", false);
		o.addProperty("gemLever", false);
		o.addProperty("parry", false);
		o.addProperty("shieldSpeed", false);
		o.addProperty("bags", false);
		o.addProperty("compareTooltip", false);
		o.addProperty("toolDurability", false);
		o.addProperty("durabilityAll", false);
		o.addProperty("inventoryScale", false);
		o.addProperty("sprintFix", false);
		o.addProperty("hotbarHide", false);
		o.addProperty("hotbarHideAll", false);
		o.addProperty("hotbarHideArmor", false);
		o.addProperty("hotbarHideXp", false);
		o.addProperty("nameColors", true);
		o.addProperty("toolRefill", false);
		o.addProperty("breakProtection", false);
		o.addProperty("loadouts", false);
		o.addProperty("trash", false);
		o.addProperty("dayNight", false);
		o.addProperty("titanium", false);
		o.addProperty("titaniumFallPercent", 10);
		o.addProperty("titaniteChance", 1.0);
		o.addProperty("titaniteBoostChipped", 8);
		o.addProperty("titaniteBoostCut", 16);
		o.addProperty("titaniteBoost", 25);
		o.addProperty("hotbarHideSeconds", 5);
		o.addProperty("hotbarDamageSeconds", 10);
		o.addProperty("hotbarFoodThreshold", 6);
		return o;
	}

	/** Number of switches in the object that differ from a new installation. */
	private static int countSwitches(JsonObject o) {
		if (o == null) {
			return 0;
		}
		int count = 0;
		for (Map.Entry<String, JsonElement> entry : DEFAULT_SWITCHES.entrySet()) {
			JsonElement value = o.get(entry.getKey());
			if (value != null && !value.equals(entry.getValue())) {
				count++;
			}
		}
		return count;
	}

	/** Every change: gear values, materials, set bonuses, switches and the numbers of the functions. */
	public static int totalChangeCount() {
		return changeCount() + countSwitches(switchesToJson()) + countValues(valuesToJson());
	}

	/** The recipe switches; recipes are read when a world loads, so a change needs the world to load again. */
	public static String recipeState() {
		return enabled + "," + tridentRecipe + "," + chainmailRecipes + "," + rabbitHideRecipe + "," + rabbitHideArmor + ","
				+ paddedArmor + "," + leatherPatch + "," + horsePadding + "," + upgrades + "," + sockets + "," + gemRecut + "," + masterPlan + ","
				+ shields + "," + repairKit + "," + gemLever + "," + bags + "," + breakProtection + "," + titanium + "," + horseArmorRecipes;
	}

	/** Remembers the HM+ profile that was just loaded together with the settings it made. */
	public static void markHmProfile(String key) {
		hmProfile = key;
		hmSignature = signature();
	}

	/** Key of the active HM+ profile, empty once anything was changed by hand or something else was loaded. */
	public static String activeHmProfile() {
		return !hmProfile.isEmpty() && hmSignature == signature() ? hmProfile : "";
	}

	/** Fingerprint of everything a profile sets. Sorted, so the order of the items doesn't matter. */
	private static int signature() {
		return (itemsToJson(new java.util.TreeMap<>(overrides)) + "|" + materialsToJson(materials) + "|"
				+ setsToJson(sets) + "|" + gearSwitchesToJson()).hashCode();
	}

	public static int changeCount() {
		return count(overrides) + countMaterials(materials) + sets.size();
	}

	private static int countMaterials(Map<MaterialStat, Map<Material, Double>> map) {
		int total = 0;
		for (Map<Material, Double> values : map.values()) {
			total += values.size();
		}
		return total;
	}

	private static int count(Map<String, Map<Stat, Double>> map) {
		int total = 0;
		for (Map<Stat, Double> item : map.values()) {
			total += item.size();
		}
		return total;
	}

	/** Changes whenever something changes that only applies after a restart. */
	public static String restartSignature() {
		return enabled + "|" + itemsToJson(overrides) + "|" + materials.get(MaterialStat.WEIGHT) + "|" + shieldSignature();
	}

	// Values per material

	public static double getMaterial(MaterialStat stat, Material material) {
		Map<Material, Double> values = materials.get(stat);
		Double value = values == null ? null : values.get(material);
		return value != null ? value : stat.vanilla;
	}

	public static boolean isChanged(MaterialStat stat, Material material) {
		Map<Material, Double> values = materials.get(stat);
		return values != null && values.containsKey(material);
	}

	/** Setting a value back to vanilla removes it from the file. */
	public static void setMaterial(MaterialStat stat, Material material, double value) {
		double clamped = stat.clamp(value);
		Map<Material, Double> values = materials.computeIfAbsent(stat, k -> new LinkedHashMap<>());
		if (Math.abs(clamped - stat.vanilla) < 1.0E-6) {
			values.remove(material);
		} else {
			values.put(material, clamped);
		}
		if (values.isEmpty()) {
			materials.remove(stat);
		}
	}

	public static void resetMaterial(Material material) {
		for (MaterialStat stat : MaterialStat.values()) {
			setMaterial(stat, material, stat.vanilla);
		}
	}

	/** Value for the material of an item, vanilla for items without a material. */
	public static double forItem(MaterialStat stat, String itemId) {
		Material material = Material.of(itemId);
		return material == null ? stat.vanilla : getMaterial(stat, material);
	}

	// Set bonuses

	public static BonusType getSetType(Material material) {
		SetEntry set = sets.get(material);
		return set != null ? set.type() : BonusType.defaultFor(material);
	}

	public static double getSetStrength(Material material) {
		SetEntry set = sets.get(material);
		return set != null ? set.strength() : 0;
	}

	public static boolean isSetChanged(Material material) {
		return sets.containsKey(material);
	}

	/** A new type keeps the strength, cut down to what the new type allows. */
	public static void setSet(Material material, BonusType type, double strength) {
		double clamped = Math.max(0, Math.min(type.max, strength));
		if (clamped == 0 && type == BonusType.defaultFor(material)) {
			sets.remove(material);
		} else {
			sets.put(material, new SetEntry(type, clamped));
		}
	}

	public static void resetSet(Material material) {
		sets.remove(material);
	}

	/** Strength of the active bonus, 0 while the set bonuses or the mod are switched off. */
	public static double activeSetStrength(Material material) {
		return setBonuses && enabled ? getSetStrength(material) : 0;
	}

	/** Share of normal block a shield still has (1 = full); falls evenly from shieldWearStart % to shieldWearMin % at 0. */
	public static float shieldBlockFactor(int damage, int maxDamage) {
		if (!enabled || !shieldWear || maxDamage <= 0) {
			return 1.0F;
		}
		double left = 100.0 * (maxDamage - damage) / maxDamage;
		if (left >= shieldWearStart) {
			return 1.0F;
		}
		double fraction = Math.max(0, left) / shieldWearStart;
		return (float) ((shieldWearMin + (100 - shieldWearMin) * fraction) / 100.0);
	}

	public static int requiredLevel(String itemId) {
		return (int) forItem(MaterialStat.LEVEL, itemId);
	}

	// Profiles

	public static Profile profile(int index) {
		return PROFILES[index];
	}

	public static void saveToProfile(int index) {
		PROFILES[index].overrides = copy(overrides);
		PROFILES[index].materials = copyMaterials(materials);
		PROFILES[index].sets = new LinkedHashMap<>(sets);
		PROFILES[index].switches = switchesToJson();
		PROFILES[index].kept = kept;
		PROFILES[index].values = valuesToJson();
	}

	public static void loadProfile(int index) {
		if (!PROFILES[index].isEmpty()) {
			overrides = copy(PROFILES[index].overrides);
			materials = copyMaterials(PROFILES[index].materials);
			sets = new LinkedHashMap<>(PROFILES[index].sets);
			kept = PROFILES[index].kept;
			if (PROFILES[index].switches != null) {
				applySwitches(PROFILES[index].switches);
			}
			if (PROFILES[index].values != null) {
				valuesFromJson(PROFILES[index].values);
			}
		}
	}

	// Switches that belong to a setup. "Mod active" and the welcome note stay out of it.

	/** Every switch with its numbers: those of the gear and those of the functions. */
	private static JsonObject switchesToJson() {
		JsonObject o = gearSwitchesToJson();
		o.addProperty("paddedArmor", paddedArmor);
		o.addProperty("leatherPatch", leatherPatch);
		o.addProperty("horsePadding", horsePadding);
		o.addProperty("upgrades", upgrades);
		o.addProperty("sockets", sockets);
		o.addProperty("gemReturn", gemReturn);
		o.addProperty("gemRecut", gemRecut);
		o.addProperty("masterPlan", masterPlan);
		o.addProperty("showHints", showHints);
		o.addProperty("keepTableItems", keepTableItems);
		o.addProperty("shields", shields);
		o.addProperty("shieldLower", shieldLower);
		o.addProperty("fireLower", fireLower);
		o.addProperty("tooltipFrames", tooltipFrames);
		o.addProperty("pickupDisplay", pickupDisplay);
		o.addProperty("pickupMarks", pickupMarks);
		o.addProperty("pickupRight", pickupRight);
		o.addProperty("outlines", outlines);
		o.addProperty("handIdle", handIdle);
		o.addProperty("durabilityWarning", durabilityWarning);
		o.addProperty("armorBar", armorBar);
		o.addProperty("hdArmor", hdArmor);
		o.addProperty("armorColors", armorColors);
		o.addProperty("armorCracks", armorCracks);
		o.addProperty("armorDarken", armorDarken);
		o.addProperty("armorBlink", armorBlink);
		o.addProperty("armorBroken", armorBroken);
		o.addProperty("hotbarPop", hotbarPop);
		o.addProperty("hotbarLowGlow", hotbarLowGlow);
		o.addProperty("itemNameBounce", itemNameBounce);
		o.addProperty("xpAnimated", xpAnimated);
		o.addProperty("hdHearts", hdHearts);
		o.addProperty("heartRing", heartRing);
		o.addProperty("hdFood", hdFood);
		o.addProperty("saturationGold", saturationGold);
		o.addProperty("hdHud", hdHud);
		o.addProperty("smoothScroll", smoothScroll);
		o.addProperty("hotbarGlide", hotbarGlide);
		o.addProperty("deathPile", deathPile);
		o.addProperty("deathSafe", deathSafe);
		o.addProperty("deathXp", deathXp);
		o.addProperty("deathExplosionProof", deathExplosionProof);
		o.addProperty("deathKeepArmor", deathKeepArmor);
		o.addProperty("deathKeepHotbar", deathKeepHotbar);
		o.addProperty("deathKeepOffhand", deathKeepOffhand);
		o.addProperty("deathPoint", deathPoint);
		o.addProperty("spawnerDrops", spawnerDrops);
		o.addProperty("spawnerWhole", spawnerWhole);
		o.addProperty("repairKit", repairKit);
		o.addProperty("gemLever", gemLever);
		o.addProperty("parry", parry);
		o.addProperty("shieldSpeed", shieldSpeed);
		o.addProperty("bags", bags);
		o.addProperty("compareTooltip", compareTooltip);
		o.addProperty("toolDurability", toolDurability);
		o.addProperty("durabilityAll", durabilityAll);
		o.addProperty("inventoryScale", inventoryScale);
		o.addProperty("sprintFix", sprintFix);
		o.addProperty("hotbarHide", hotbarHide);
		o.addProperty("hotbarHideAll", hotbarHideAll);
		o.addProperty("hotbarHideArmor", hotbarHideArmor);
		o.addProperty("hotbarHideXp", hotbarHideXp);
		o.addProperty("nameColors", nameColors);
		o.addProperty("toolRefill", toolRefill);
		o.addProperty("breakProtection", breakProtection);
		o.addProperty("loadouts", loadouts);
		o.addProperty("trash", trash);
		o.addProperty("dayNight", dayNight);
		o.addProperty("titanium", titanium);
		o.addProperty("titaniumFallPercent", titaniumFallPercent);
		o.addProperty("titaniteChance", titaniteChance);
		o.addProperty("titaniteBoostChipped", titaniteBoostChipped);
		o.addProperty("titaniteBoostCut", titaniteBoostCut);
		o.addProperty("titaniteBoost", titaniteBoost);
		o.addProperty("hotbarHideSeconds", hotbarHideSeconds);
		o.addProperty("hotbarDamageSeconds", hotbarDamageSeconds);
		o.addProperty("hotbarFoodThreshold", hotbarFoodThreshold);
		return o;
	}

	/** The switches of the gear part only, for the fingerprint of the HM+ profiles. */
	private static JsonObject gearSwitchesToJson() {
		JsonObject o = new JsonObject();
		o.addProperty("setBonuses", setBonuses);
		o.addProperty("tridentRecipe", tridentRecipe);
		o.addProperty("chainmailRecipes", chainmailRecipes);
		o.addProperty("rabbitHideRecipe", rabbitHideRecipe);
		o.addProperty("horseArmorRecipes", horseArmorRecipes);
		o.addProperty("rabbitHideArmor", rabbitHideArmor);
		o.addProperty("anvilNoLimit", anvilNoLimit);
		o.addProperty("anvilMaxCost", anvilMaxCost);
		o.addProperty("zombieLeather", zombieLeather);
		o.addProperty("zombieLeatherChance", zombieLeatherChance);
		o.addProperty("lootTemplates", lootTemplates);
		o.addProperty("lootMaterials", lootMaterials);
		o.addProperty("lootNoCheap", lootNoCheap);
		o.addProperty("lootOverworld", lootOverworld);
		o.addProperty("shieldWear", shieldWear);
		o.addProperty("shieldWearStart", shieldWearStart);
		o.addProperty("shieldWearMin", shieldWearMin);
		return o;
	}

	/** Missing entries keep their current value. */
	private static void applySwitches(JsonObject o) {
		setBonuses = bool(o, "setBonuses", setBonuses);
		tridentRecipe = bool(o, "tridentRecipe", tridentRecipe);
		chainmailRecipes = bool(o, "chainmailRecipes", chainmailRecipes);
		rabbitHideRecipe = bool(o, "rabbitHideRecipe", rabbitHideRecipe);
		horseArmorRecipes = bool(o, "horseArmorRecipes", horseArmorRecipes);
		rabbitHideArmor = bool(o, "rabbitHideArmor", rabbitHideArmor);
		anvilNoLimit = bool(o, "anvilNoLimit", anvilNoLimit);
		anvilMaxCost = number(o, "anvilMaxCost", 1, ANVIL_MAX_COST_LIMIT, anvilMaxCost);
		zombieLeather = bool(o, "zombieLeather", zombieLeather);
		zombieLeatherChance = number(o, "zombieLeatherChance", 1, 100, zombieLeatherChance);
		lootTemplates = bool(o, "lootTemplates", lootTemplates);
		lootMaterials = bool(o, "lootMaterials", lootMaterials);
		lootNoCheap = bool(o, "lootNoCheap", lootNoCheap);
		lootOverworld = bool(o, "lootOverworld", lootOverworld);
		shieldWear = bool(o, "shieldWear", shieldWear);
		shieldWearStart = number(o, "shieldWearStart", 1, 100, shieldWearStart);
		shieldWearMin = number(o, "shieldWearMin", 0, 100, shieldWearMin);
		paddedArmor = bool(o, "paddedArmor", paddedArmor);
		leatherPatch = bool(o, "leatherPatch", leatherPatch);
		horsePadding = bool(o, "horsePadding", horsePadding);
		upgrades = bool(o, "upgrades", upgrades);
		sockets = bool(o, "sockets", sockets);
		gemReturn = bool(o, "gemReturn", gemReturn);
		gemRecut = bool(o, "gemRecut", gemRecut);
		masterPlan = bool(o, "masterPlan", masterPlan);
		showHints = bool(o, "showHints", showHints);
		keepTableItems = bool(o, "keepTableItems", keepTableItems);
		shields = bool(o, "shields", shields);
		shieldLower = bool(o, "shieldLower", shieldLower);
		fireLower = bool(o, "fireLower", fireLower);
		tooltipFrames = bool(o, "tooltipFrames", tooltipFrames);
		pickupDisplay = bool(o, "pickupDisplay", pickupDisplay);
		pickupMarks = bool(o, "pickupMarks", pickupMarks);
		pickupRight = bool(o, "pickupRight", pickupRight);
		outlines = bool(o, "outlines", outlines);
		handIdle = bool(o, "handIdle", handIdle);
		durabilityWarning = bool(o, "durabilityWarning", durabilityWarning);
		armorBar = bool(o, "armorBar", armorBar);
		hdArmor = bool(o, "hdArmor", hdArmor);
		armorColors = bool(o, "armorColors", armorColors);
		armorCracks = bool(o, "armorCracks", armorCracks);
		armorDarken = bool(o, "armorDarken", armorDarken);
		armorBlink = bool(o, "armorBlink", armorBlink);
		armorBroken = bool(o, "armorBroken", armorBroken);
		hotbarPop = bool(o, "hotbarPop", hotbarPop);
		hotbarLowGlow = bool(o, "hotbarLowGlow", hotbarLowGlow);
		itemNameBounce = bool(o, "itemNameBounce", itemNameBounce);
		xpAnimated = bool(o, "xpAnimated", xpAnimated);
		hdHearts = bool(o, "hdHearts", hdHearts);
		heartRing = bool(o, "heartRing", heartRing);
		hdFood = bool(o, "hdFood", hdFood);
		saturationGold = bool(o, "saturationGold", saturationGold);
		hdHud = bool(o, "hdHud", hdHud);
		smoothScroll = bool(o, "smoothScroll", smoothScroll);
		hotbarGlide = bool(o, "hotbarGlide", hotbarGlide);
		deathPile = bool(o, "deathPile", deathPile);
		deathSafe = bool(o, "deathSafe", deathSafe);
		deathXp = bool(o, "deathXp", deathXp);
		deathExplosionProof = bool(o, "deathExplosionProof", deathExplosionProof);
		deathKeepArmor = bool(o, "deathKeepArmor", deathKeepArmor);
		deathKeepHotbar = bool(o, "deathKeepHotbar", deathKeepHotbar);
		deathKeepOffhand = bool(o, "deathKeepOffhand", deathKeepOffhand);
		deathPoint = bool(o, "deathPoint", deathPoint);
		spawnerDrops = bool(o, "spawnerDrops", spawnerDrops);
		spawnerWhole = bool(o, "spawnerWhole", spawnerWhole);
		repairKit = bool(o, "repairKit", repairKit);
		gemLever = bool(o, "gemLever", gemLever);
		parry = bool(o, "parry", parry);
		shieldSpeed = bool(o, "shieldSpeed", shieldSpeed);
		bags = bool(o, "bags", bags);
		compareTooltip = bool(o, "compareTooltip", compareTooltip);
		toolDurability = bool(o, "toolDurability", toolDurability);
		durabilityAll = bool(o, "durabilityAll", durabilityAll);
		inventoryScale = bool(o, "inventoryScale", inventoryScale);
		sprintFix = bool(o, "sprintFix", sprintFix);
		hotbarHide = bool(o, "hotbarHide", hotbarHide);
		hotbarHideAll = bool(o, "hotbarHideAll", hotbarHideAll);
		hotbarHideArmor = bool(o, "hotbarHideArmor", hotbarHideArmor);
		hotbarHideXp = bool(o, "hotbarHideXp", hotbarHideXp);
		nameColors = bool(o, "nameColors", nameColors);
		toolRefill = bool(o, "toolRefill", toolRefill);
		breakProtection = bool(o, "breakProtection", breakProtection);
		loadouts = bool(o, "loadouts", loadouts);
		trash = bool(o, "trash", trash);
		dayNight = bool(o, "dayNight", dayNight);
		titanium = bool(o, "titanium", titanium);
		titaniumFallPercent = number(o, "titaniumFallPercent", 0, 25, titaniumFallPercent);
		titaniteChance = decimal(o, "titaniteChance", titaniteChance, 0, 100);
		titaniteBoostChipped = number(o, "titaniteBoostChipped", 0, 100, titaniteBoostChipped);
		titaniteBoostCut = number(o, "titaniteBoostCut", 0, 100, titaniteBoostCut);
		titaniteBoost = number(o, "titaniteBoost", 0, 100, titaniteBoost);
		hotbarHideSeconds = number(o, "hotbarHideSeconds", 1, 60, hotbarHideSeconds);
		hotbarDamageSeconds = number(o, "hotbarDamageSeconds", 0, 60, hotbarDamageSeconds);
		hotbarFoodThreshold = number(o, "hotbarFoodThreshold", 0, 20, hotbarFoodThreshold);
	}

	// Export and import of whole setups as files in config/gearbalance/profiles/

	private static final DateTimeFormatter EXPORT_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	public static Path exportFolder() {
		return FabricLoader.getInstance().getConfigDir().resolve("gearbalance").resolve("profiles");
	}

	/** Writes the current values, materials, set bonuses and switches. Returns the file name, null on failure. */
	public static String exportCurrent() {
		JsonObject root = new JsonObject();
		root.addProperty("gearbalanceExport", 1);
		root.add("items", itemsToJson(overrides));
		root.add("materials", materialsToJson(materials));
		root.add("sets", setsToJson(sets));
		addKept(root, kept);
		root.add("switches", switchesToJson());
		root.add("values", valuesToJson());
		String name = "gearbalance_" + LocalDateTime.now().format(EXPORT_NAME) + ".json";
		try {
			Files.createDirectories(exportFolder());
			try (Writer writer = Files.newBufferedWriter(exportFolder().resolve(name))) {
				GSON.toJson(root, writer);
			}
			return name;
		} catch (IOException e) {
			GearBalance.LOGGER.error("Could not export the settings", e);
			return null;
		}
	}

	/** The .json files in the export folder, newest first. */
	public static List<String> listExports() {
		Path folder = exportFolder();
		if (!Files.isDirectory(folder)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(folder)) {
			return files.filter(path -> path.getFileName().toString().endsWith(".json") && Files.isRegularFile(path))
					.sorted(Comparator.comparing(GearConfig::modified).reversed())
					.map(path -> path.getFileName().toString())
					.toList();
		} catch (IOException e) {
			GearBalance.LOGGER.error("Could not list the exported files", e);
			return List.of();
		}
	}

	private static long modified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	/** Deletes an exported file. Only names from listExports() are accepted, nothing outside the folder. */
	public static boolean deleteExport(String fileName) {
		if (!listExports().contains(fileName)) {
			return false;
		}
		try {
			return Files.deleteIfExists(exportFolder().resolve(fileName));
		} catch (IOException e) {
			GearBalance.LOGGER.error("Could not delete {}", fileName, e);
			return false;
		}
	}

	/** Replaces the current setup with the file. Returns false if it can't be read, nothing changes then. */
	public static boolean importFile(String fileName) {
		Path path = exportFolder().resolve(fileName);
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (!root.has("items") && !root.has("materials") && !root.has("sets")) {
				return false;
			}
			applyJson(root);
			return true;
		} catch (IOException | RuntimeException e) {
			GearBalance.LOGGER.error("Could not import {}", fileName, e);
			return false;
		}
	}

	/** Replaces values, materials, set bonuses and switches with a setup in export format (import, presets). */
	public static void applyJson(JsonObject root) {
		Map<String, Map<Stat, Double>> newItems = itemsFromJson(root.get("items"));
		Map<MaterialStat, Map<Material, Double>> newMaterials = materialsFromJson(root.get("materials"));
		Map<Material, SetEntry> newSets = setsFromJson(root.get("sets"));
		overrides = newItems;
		materials = newMaterials;
		sets = newSets;
		kept = Kept.from(root);
		JsonElement switches = root.get("switches");
		if (switches != null && switches.isJsonObject()) {
			applySwitches(switches.getAsJsonObject());
		}
		JsonElement values = root.get("values");
		if (values != null && values.isJsonObject()) {
			valuesFromJson(values.getAsJsonObject());
		}
	}

	// Settings of a server. On a server of someone else the client uses them in memory only; the own settings
	// stay in the file and come back when the player leaves.

	/** The own settings while those of a server are used, null while the own ones are used. */
	private static JsonObject ownSettings;
	private static boolean serverCanEdit;

	/** The settings of a server are used right now (and not the own ones). */
	public static boolean serverMode() {
		return ownSettings != null;
	}

	/** The player may change the settings of the server (operator). */
	public static boolean serverCanEdit() {
		return serverCanEdit;
	}

	/** Switches that only change what this player sees or how the own controls work; a server never sets them.
	 * Shield and fire lowering are left out on purpose: they improve the view, so the server decides them. */
	private static final List<String> CLIENT_OPTIONS = List.of("sprintFix", "hotbarHide", "hotbarHideAll",
			"hotbarHideSeconds", "hotbarDamageSeconds", "hotbarFoodThreshold", "nameColors", "hotbarHideArmor", "hotbarHideXp",
			"showHints", "tooltipFrames", "compareTooltip", "toolDurability", "durabilityAll", "pickupDisplay", "pickupMarks",
			"pickupRight", "handIdle", "durabilityWarning", "inventoryScale", "outlines", "armorBar", "hdArmor", "armorColors", "armorCracks",
			"armorDarken", "armorBlink", "armorBroken", "hotbarPop", "hotbarLowGlow", "itemNameBounce", "xpAnimated",
			"hdHearts", "heartRing", "hdFood", "saturationGold", "hdHud", "smoothScroll",
			"hotbarGlide");

	/** Numbers of the view and the glow; like CLIENT_OPTIONS they stay the player's own on a server. */
	private static final List<String> CLIENT_VALUES = List.of("effectRed",
			"effectGreen", "effectBlue", "handEffect", "effectBrightness", "ringSaturation", "glowPulse", "pulseStrength",
			"pulseSeconds", "shieldGlow", "shieldFrame", "shieldGlowShaders", "hideGlint", "pickupSeconds", "pickupRows", "pickupScale",
			"handIdlePercent", "durabilityWarningPercent", "inventoryGuiScale", "armorCrackPercent",
			"armorCrack2Percent", "armorDarkenPercent", "armorBlinkPercent", "hotbarPopPercent", "hotbarGlowPercent");

	private static JsonObject clientValues() {
		JsonObject all = valuesToJson();
		JsonObject result = new JsonObject();
		for (String key : CLIENT_VALUES) {
			if (all.has(key)) {
				result.add(key, all.get(key));
			}
		}
		return result;
	}

	private static JsonObject clientOptions() {
		JsonObject all = switchesToJson();
		JsonObject result = new JsonObject();
		for (String key : CLIENT_OPTIONS) {
			if (all.has(key)) {
				result.add(key, all.get(key));
			}
		}
		return result;
	}

	/** Everything that decides how the game plays: "Mod active", gear values, materials, sets, switches, numbers. */
	public static JsonObject settingsToJson() {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", enabled);
		root.add("items", itemsToJson(overrides));
		root.add("materials", materialsToJson(materials));
		root.add("sets", setsToJson(sets));
		JsonObject switches = switchesToJson();
		CLIENT_OPTIONS.forEach(switches::remove);
		root.add("switches", switches);
		JsonObject values = valuesToJson();
		CLIENT_VALUES.forEach(values::remove);
		root.add("values", values);
		return root;
	}

	/** Replaces everything that decides how the game plays; missing entries get their default. */
	public static void settingsFromJson(JsonObject root) {
		// The own view, glow and control options stay as they are
		JsonObject client = clientOptions();
		JsonObject clientValues = clientValues();
		enabled = bool(root, "enabled", true);
		overrides = itemsFromJson(root.get("items"));
		materials = materialsFromJson(root.get("materials"));
		sets = setsFromJson(root.get("sets"));
		applySwitches(DEFAULT_SWITCHES);
		JsonElement switches = root.get("switches");
		if (switches != null && switches.isJsonObject()) {
			applySwitches(switches.getAsJsonObject());
		}
		JsonElement values = root.get("values");
		if (values != null && values.isJsonObject()) {
			valuesFromJson(values.getAsJsonObject());
		} else {
			resetValues();
		}
		applySwitches(client);
		readValues(clientValues);
	}

	/** Uses the settings a server sent; the own ones are kept to come back later. */
	public static void useServerSettings(JsonObject settings, boolean canEdit) {
		if (ownSettings == null) {
			ownSettings = settingsToJson();
		}
		settingsFromJson(settings);
		serverCanEdit = canEdit;
	}

	/** Back to the own settings after leaving a server. */
	public static void useOwnSettings() {
		if (ownSettings != null) {
			JsonObject own = ownSettings;
			ownSettings = null;
			serverCanEdit = false;
			settingsFromJson(own);
		}
	}

	public static void clearProfile(int index) {
		PROFILES[index] = new Profile();
	}

	private static Map<String, Map<Stat, Double>> copy(Map<String, Map<Stat, Double>> source) {
		Map<String, Map<Stat, Double>> result = new LinkedHashMap<>();
		source.forEach((id, values) -> result.put(id, new EnumMap<>(values)));
		return result;
	}

	private static Map<MaterialStat, Map<Material, Double>> copyMaterials(Map<MaterialStat, Map<Material, Double>> source) {
		Map<MaterialStat, Map<Material, Double>> result = new EnumMap<>(MaterialStat.class);
		source.forEach((stat, values) -> result.put(stat, new LinkedHashMap<>(values)));
		return result;
	}

	// File

	public static void load() {
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				read(JsonParser.parseReader(reader).getAsJsonObject());
			} catch (IOException | RuntimeException e) {
				GearBalance.LOGGER.error("Could not read config/{}, using vanilla values", FILE_NAME, e);
				backupBrokenFile(path);
			}
		}
		save();
	}

	private static void read(JsonObject root) {
		enabled = bool(root, "enabled", true);
		tridentRecipe = bool(root, "tridentRecipe", false);
		chainmailRecipes = bool(root, "chainmailRecipes", false);
		rabbitHideRecipe = bool(root, "rabbitHideRecipe", false);
		horseArmorRecipes = bool(root, "horseArmorRecipes", false);
		rabbitHideArmor = bool(root, "rabbitHideArmor", false);
		anvilNoLimit = bool(root, "anvilNoLimit", false);
		anvilMaxCost = number(root, "anvilMaxCost", 1, ANVIL_MAX_COST_LIMIT, VANILLA_ANVIL_MAX_COST);
		zombieLeather = bool(root, "zombieLeather", false);
		zombieLeatherChance = number(root, "zombieLeatherChance", 1, 100, DEFAULT_ZOMBIE_LEATHER_CHANCE);
		overrides = itemsFromJson(root.get("items"));
		materials = materialsFromJson(root.get("materials"));
		sets = setsFromJson(root.get("sets"));
		kept = Kept.from(root);
		setBonuses = bool(root, "setBonuses", false);
		welcomeShown = bool(root, "welcomeShown", false);
		lootTemplates = bool(root, "lootTemplates", false);
		lootMaterials = bool(root, "lootMaterials", false);
		lootNoCheap = bool(root, "lootNoCheap", false);
		lootOverworld = bool(root, "lootOverworld", false);
		shieldWear = bool(root, "shieldWear", false);
		shieldWearStart = number(root, "shieldWearStart", 1, 100, DEFAULT_SHIELD_WEAR_START);
		shieldWearMin = number(root, "shieldWearMin", 0, 100, DEFAULT_SHIELD_WEAR_MIN);
		readFeatures(root);
		JsonElement hm = root.get("hmProfile");
		hmProfile = hm != null && hm.isJsonPrimitive() ? hm.getAsString() : "";
		JsonElement hmSig = root.get("hmSignature");
		hmSignature = hmSig != null && hmSig.isJsonPrimitive() ? hmSig.getAsInt() : 0;

		JsonElement profiles = root.get("profiles");
		if (profiles == null || !profiles.isJsonArray()) {
			return;
		}
		JsonArray array = profiles.getAsJsonArray();
		for (int i = 0; i < PROFILE_COUNT && i < array.size(); i++) {
			if (!array.get(i).isJsonObject()) {
				continue;
			}
			JsonObject p = array.get(i).getAsJsonObject();
			Profile profile = new Profile();
			JsonElement name = p.get("name");
			if (name != null && name.isJsonPrimitive()) {
				profile.name = name.getAsString();
			}
			JsonElement items = p.get("items");
			if (items != null && items.isJsonObject()) {
				profile.overrides = itemsFromJson(items);
				profile.materials = materialsFromJson(p.get("materials"));
				profile.sets = setsFromJson(p.get("sets"));
				profile.kept = Kept.from(p);
				JsonElement switches = p.get("switches");
				profile.switches = switches != null && switches.isJsonObject() ? switches.getAsJsonObject() : null;
				JsonElement values = p.get("values");
				profile.values = values != null && values.isJsonObject() ? values.getAsJsonObject() : null;
			}
			PROFILES[i] = profile;
		}
	}

	/** The switches and numbers of the functions from the root of the file; missing ones get their default. */
	private static void readFeatures(JsonObject root) {
		paddedArmor = bool(root, "paddedArmor", false);
		leatherPatch = bool(root, "leatherPatch", false);
		horsePadding = bool(root, "horsePadding", false);
		upgrades = bool(root, "upgrades", false);
		sockets = bool(root, "sockets", false);
		gemReturn = bool(root, "gemReturn", false);
		gemRecut = bool(root, "gemRecut", false);
		masterPlan = bool(root, "masterPlan", false);
		showHints = bool(root, "showHints", true);
		keepTableItems = bool(root, "keepTableItems", false);
		shields = bool(root, "shields", false);
		shieldLower = bool(root, "shieldLower", false);
		fireLower = bool(root, "fireLower", false);
		tooltipFrames = bool(root, "tooltipFrames", false);
		pickupDisplay = bool(root, "pickupDisplay", false);
		pickupMarks = bool(root, "pickupMarks", false);
		pickupRight = bool(root, "pickupRight", false);
		outlines = bool(root, "outlines", false);
		handIdle = bool(root, "handIdle", false);
		durabilityWarning = bool(root, "durabilityWarning", false);
		armorBar = bool(root, "armorBar", false);
		hdArmor = bool(root, "hdArmor", false);
		armorColors = bool(root, "armorColors", false);
		armorCracks = bool(root, "armorCracks", false);
		armorDarken = bool(root, "armorDarken", false);
		armorBlink = bool(root, "armorBlink", false);
		armorBroken = bool(root, "armorBroken", false);
		hotbarPop = bool(root, "hotbarPop", false);
		hotbarLowGlow = bool(root, "hotbarLowGlow", false);
		itemNameBounce = bool(root, "itemNameBounce", false);
		xpAnimated = bool(root, "xpAnimated", false);
		hdHearts = bool(root, "hdHearts", false);
		heartRing = bool(root, "heartRing", false);
		hdFood = bool(root, "hdFood", false);
		saturationGold = bool(root, "saturationGold", false);
		hdHud = bool(root, "hdHud", false);
		smoothScroll = bool(root, "smoothScroll", false);
		hotbarGlide = bool(root, "hotbarGlide", false);
		deathPile = bool(root, "deathPile", false);
		deathSafe = bool(root, "deathSafe", false);
		deathXp = bool(root, "deathXp", false);
		deathExplosionProof = bool(root, "deathExplosionProof", false);
		deathKeepArmor = bool(root, "deathKeepArmor", false);
		deathKeepHotbar = bool(root, "deathKeepHotbar", false);
		deathKeepOffhand = bool(root, "deathKeepOffhand", false);
		deathPoint = bool(root, "deathPoint", false);
		spawnerDrops = bool(root, "spawnerDrops", false);
		spawnerWhole = bool(root, "spawnerWhole", false);
		repairKit = bool(root, "repairKit", false);
		gemLever = bool(root, "gemLever", false);
		parry = bool(root, "parry", false);
		shieldSpeed = bool(root, "shieldSpeed", false);
		bags = bool(root, "bags", false);
		compareTooltip = bool(root, "compareTooltip", false);
		toolDurability = bool(root, "toolDurability", false);
		durabilityAll = bool(root, "durabilityAll", false);
		inventoryScale = bool(root, "inventoryScale", false);
		sprintFix = bool(root, "sprintFix", false);
		hotbarHide = bool(root, "hotbarHide", false);
		hotbarHideAll = bool(root, "hotbarHideAll", false);
		hotbarHideArmor = bool(root, "hotbarHideArmor", false);
		hotbarHideXp = bool(root, "hotbarHideXp", false);
		nameColors = bool(root, "nameColors", true);
		toolRefill = bool(root, "toolRefill", false);
		breakProtection = bool(root, "breakProtection", false);
		loadouts = bool(root, "loadouts", false);
		trash = bool(root, "trash", false);
		dayNight = bool(root, "dayNight", false);
		titanium = bool(root, "titanium", false);
		titaniumFallPercent = number(root, "titaniumFallPercent", 0, 25, 10);
		titaniteChance = decimal(root, "titaniteChance", 1.0, 0, 100);
		titaniteBoostChipped = number(root, "titaniteBoostChipped", 0, 100, 8);
		titaniteBoostCut = number(root, "titaniteBoostCut", 0, 100, 16);
		titaniteBoost = number(root, "titaniteBoost", 0, 100, 25);
		hotbarHideSeconds = number(root, "hotbarHideSeconds", 1, 60, 5);
		hotbarDamageSeconds = number(root, "hotbarDamageSeconds", 0, 60, 10);
		hotbarFoodThreshold = number(root, "hotbarFoodThreshold", 0, 20, 6);
		JsonElement values = root.get("values");
		if (values != null && values.isJsonObject()) {
			valuesFromJson(values.getAsJsonObject());
		} else {
			resetValues();
		}
	}

	/** Reads only the switches and numbers of the functions again (/reload). */
	public static void reloadFeatures() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (root.isJsonObject()) {
				readFeatures(root.getAsJsonObject());
			}
		} catch (IOException | RuntimeException e) {
			GearBalance.LOGGER.error("Could not read the functions in config/{}", FILE_NAME, e);
		}
	}

	/** Keeps an unreadable file as gearbalance.json.broken, so values and profiles are not lost. */
	private static void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling(FILE_NAME + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			GearBalance.LOGGER.warn("Your old config was saved as {}.broken", FILE_NAME);
		} catch (IOException e) {
			GearBalance.LOGGER.error("Could not back up the broken config file", e);
		}
	}

	private static int saves;

	/** Goes up with every save, so the settings screen knows which values are in the file. */
	public static int saveCount() {
		return saves;
	}

	public static void save() {
		if (ownSettings != null) {
			// On a server only the profiles change in the file; the own settings stay as they are
			JsonObject server = settingsToJson();
			settingsFromJson(ownSettings);
			try {
				write();
			} finally {
				settingsFromJson(server);
			}
			return;
		}
		write();
	}

	private static void write() {
		saves++;
		// Upgraded weapons work their modifiers out again with the new values
		Upgrades.clearCache();
		JsonObject root = new JsonObject();
		root.addProperty("enabled", enabled);
		root.addProperty("tridentRecipe", tridentRecipe);
		root.addProperty("chainmailRecipes", chainmailRecipes);
		root.addProperty("rabbitHideRecipe", rabbitHideRecipe);
		root.addProperty("horseArmorRecipes", horseArmorRecipes);
		root.addProperty("rabbitHideArmor", rabbitHideArmor);
		root.addProperty("anvilNoLimit", anvilNoLimit);
		root.addProperty("anvilMaxCost", anvilMaxCost);
		root.addProperty("zombieLeather", zombieLeather);
		root.addProperty("zombieLeatherChance", zombieLeatherChance);
		root.addProperty("setBonuses", setBonuses);
		root.addProperty("welcomeShown", welcomeShown);
		root.addProperty("lootTemplates", lootTemplates);
		root.addProperty("lootMaterials", lootMaterials);
		root.addProperty("lootNoCheap", lootNoCheap);
		root.addProperty("lootOverworld", lootOverworld);
		root.addProperty("shieldWear", shieldWear);
		root.addProperty("shieldWearStart", shieldWearStart);
		root.addProperty("shieldWearMin", shieldWearMin);
		root.addProperty("paddedArmor", paddedArmor);
		root.addProperty("leatherPatch", leatherPatch);
		root.addProperty("horsePadding", horsePadding);
		root.addProperty("upgrades", upgrades);
		root.addProperty("sockets", sockets);
		root.addProperty("gemReturn", gemReturn);
		root.addProperty("gemRecut", gemRecut);
		root.addProperty("masterPlan", masterPlan);
		root.addProperty("showHints", showHints);
		root.addProperty("keepTableItems", keepTableItems);
		root.addProperty("shields", shields);
		root.addProperty("shieldLower", shieldLower);
		root.addProperty("fireLower", fireLower);
		root.addProperty("tooltipFrames", tooltipFrames);
		root.addProperty("pickupDisplay", pickupDisplay);
		root.addProperty("pickupMarks", pickupMarks);
		root.addProperty("pickupRight", pickupRight);
		root.addProperty("outlines", outlines);
		root.addProperty("handIdle", handIdle);
		root.addProperty("durabilityWarning", durabilityWarning);
		root.addProperty("armorBar", armorBar);
		root.addProperty("hdArmor", hdArmor);
		root.addProperty("armorColors", armorColors);
		root.addProperty("armorCracks", armorCracks);
		root.addProperty("armorDarken", armorDarken);
		root.addProperty("armorBlink", armorBlink);
		root.addProperty("armorBroken", armorBroken);
		root.addProperty("hotbarPop", hotbarPop);
		root.addProperty("hotbarLowGlow", hotbarLowGlow);
		root.addProperty("itemNameBounce", itemNameBounce);
		root.addProperty("xpAnimated", xpAnimated);
		root.addProperty("hdHearts", hdHearts);
		root.addProperty("heartRing", heartRing);
		root.addProperty("hdFood", hdFood);
		root.addProperty("saturationGold", saturationGold);
		root.addProperty("hdHud", hdHud);
		root.addProperty("smoothScroll", smoothScroll);
		root.addProperty("hotbarGlide", hotbarGlide);
		root.addProperty("deathPile", deathPile);
		root.addProperty("deathSafe", deathSafe);
		root.addProperty("deathXp", deathXp);
		root.addProperty("deathExplosionProof", deathExplosionProof);
		root.addProperty("deathKeepArmor", deathKeepArmor);
		root.addProperty("deathKeepHotbar", deathKeepHotbar);
		root.addProperty("deathKeepOffhand", deathKeepOffhand);
		root.addProperty("deathPoint", deathPoint);
		root.addProperty("spawnerDrops", spawnerDrops);
		root.addProperty("spawnerWhole", spawnerWhole);
		root.addProperty("repairKit", repairKit);
		root.addProperty("gemLever", gemLever);
		root.addProperty("parry", parry);
		root.addProperty("shieldSpeed", shieldSpeed);
		root.addProperty("bags", bags);
		root.addProperty("compareTooltip", compareTooltip);
		root.addProperty("toolDurability", toolDurability);
		root.addProperty("durabilityAll", durabilityAll);
		root.addProperty("inventoryScale", inventoryScale);
		root.addProperty("sprintFix", sprintFix);
		root.addProperty("hotbarHide", hotbarHide);
		root.addProperty("hotbarHideAll", hotbarHideAll);
		root.addProperty("hotbarHideArmor", hotbarHideArmor);
		root.addProperty("hotbarHideXp", hotbarHideXp);
		root.addProperty("nameColors", nameColors);
		root.addProperty("toolRefill", toolRefill);
		root.addProperty("breakProtection", breakProtection);
		root.addProperty("loadouts", loadouts);
		root.addProperty("trash", trash);
		root.addProperty("dayNight", dayNight);
		root.addProperty("titanium", titanium);
		root.addProperty("titaniumFallPercent", titaniumFallPercent);
		root.addProperty("titaniteChance", titaniteChance);
		root.addProperty("titaniteBoostChipped", titaniteBoostChipped);
		root.addProperty("titaniteBoostCut", titaniteBoostCut);
		root.addProperty("titaniteBoost", titaniteBoost);
		root.addProperty("hotbarHideSeconds", hotbarHideSeconds);
		root.addProperty("hotbarDamageSeconds", hotbarDamageSeconds);
		root.addProperty("hotbarFoodThreshold", hotbarFoodThreshold);
		root.addProperty("hmProfile", hmProfile);
		root.addProperty("hmSignature", hmSignature);
		root.add("items", itemsToJson(overrides));
		root.add("materials", materialsToJson(materials));
		root.add("sets", setsToJson(sets));
		addKept(root, kept);
		root.add("values", valuesToJson());

		JsonArray profiles = new JsonArray();
		for (Profile profile : PROFILES) {
			JsonObject p = new JsonObject();
			p.addProperty("name", profile.name);
			if (!profile.isEmpty()) {
				p.add("items", itemsToJson(profile.overrides));
				p.add("materials", materialsToJson(profile.materials));
				p.add("sets", setsToJson(profile.sets));
				addKept(p, profile.kept);
				if (profile.switches != null) {
					p.add("switches", profile.switches);
				}
				if (profile.values != null) {
					p.add("values", profile.values);
				}
			}
			profiles.add(p);
		}
		root.add("profiles", profiles);

		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file())) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			GearBalance.LOGGER.error("Could not save config/{}", FILE_NAME, e);
		}
	}

	private static void addKept(JsonObject root, Kept entries) {
		entries.addTo(root.getAsJsonObject("items"), root.getAsJsonObject("materials"), root.getAsJsonObject("sets"));
	}

	private static boolean bool(JsonObject object, String key, boolean fallback) {
		JsonElement element = object.get(key);
		return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
	}

	private static int number(JsonObject object, String key, int min, int max, int fallback) {
		JsonElement element = object.get(key);
		return element != null && element.isJsonPrimitive()
				? Math.max(min, Math.min(max, element.getAsInt()))
				: fallback;
	}

	private static JsonObject setsToJson(Map<Material, SetEntry> map) {
		JsonObject result = new JsonObject();
		// Always in the order of the materials, so the HM+ fingerprint stays the same after a restart
		for (Material material : Material.values()) {
			SetEntry set = map.get(material);
			if (set == null) {
				continue;
			}
			JsonObject o = new JsonObject();
			o.addProperty("bonus", set.type().key);
			o.addProperty("strength", set.strength());
			result.add(material.key, o);
		}
		return result;
	}

	private static Map<Material, SetEntry> setsFromJson(JsonElement element) {
		Map<Material, SetEntry> result = new LinkedHashMap<>();
		if (element == null || !element.isJsonObject()) {
			return result;
		}
		JsonObject root = element.getAsJsonObject();
		for (Material material : Material.values()) {
			JsonElement entry = root.get(material.key);
			if (!material.hasArmor() || entry == null || !entry.isJsonObject()) {
				continue;
			}
			JsonObject o = entry.getAsJsonObject();
			JsonElement bonus = o.get("bonus");
			BonusType type = bonus != null && bonus.isJsonPrimitive() ? BonusType.byKey(bonus.getAsString()) : null;
			if (type == null) {
				type = BonusType.defaultFor(material);
			}
			JsonElement strength = o.get("strength");
			double value = strength != null && strength.isJsonPrimitive() ? strength.getAsDouble() : 0;
			double clamped = Math.max(0, Math.min(type.max, value));
			if (clamped != 0 || type != BonusType.defaultFor(material)) {
				result.put(material, new SetEntry(type, clamped));
			}
		}
		return result;
	}

	private static JsonObject materialsToJson(Map<MaterialStat, Map<Material, Double>> map) {
		JsonObject result = new JsonObject();
		for (Material material : Material.values()) {
			JsonObject o = new JsonObject();
			for (MaterialStat stat : MaterialStat.values()) {
				Map<Material, Double> values = map.get(stat);
				Double value = values == null ? null : values.get(material);
				if (value == null) {
					continue;
				}
				if (stat.whole()) {
					o.addProperty(stat.key, (int) Math.round(value));
				} else {
					o.addProperty(stat.key, value);
				}
			}
			if (!o.isEmpty()) {
				result.add(material.key, o);
			}
		}
		return result;
	}

	private static Map<MaterialStat, Map<Material, Double>> materialsFromJson(JsonElement element) {
		Map<MaterialStat, Map<Material, Double>> result = new EnumMap<>(MaterialStat.class);
		if (element == null || !element.isJsonObject()) {
			return result;
		}
		JsonObject root = element.getAsJsonObject();
		for (Material material : Material.values()) {
			JsonElement entry = root.get(material.key);
			if (entry == null || !entry.isJsonObject()) {
				continue;
			}
			for (MaterialStat stat : MaterialStat.values()) {
				JsonElement value = entry.getAsJsonObject().get(stat.key);
				if (value == null || !value.isJsonPrimitive() || !stat.appliesTo(material)) {
					continue;
				}
				double clamped = stat.clamp(value.getAsDouble());
				if (Math.abs(clamped - stat.vanilla) >= 1.0E-6) {
					result.computeIfAbsent(stat, k -> new LinkedHashMap<>()).put(material, clamped);
				}
			}
		}
		return result;
	}

	private static JsonObject itemsToJson(Map<String, Map<Stat, Double>> map) {
		JsonObject items = new JsonObject();
		// Same order as in the settings screen
		for (GearItems.Entry entry : GearItems.all()) {
			Map<Stat, Double> item = map.get(entry.id());
			if (item == null || item.isEmpty()) {
				continue;
			}
			JsonObject o = new JsonObject();
			item.forEach((stat, value) -> {
				if (stat == Stat.DURABILITY) {
					o.addProperty(stat.key, (int) Math.round(value));
				} else {
					o.addProperty(stat.key, value);
				}
			});
			items.add(entry.id(), o);
		}
		return items;
	}

	private static Map<String, Map<Stat, Double>> itemsFromJson(JsonElement element) {
		Map<String, Map<Stat, Double>> result = new LinkedHashMap<>();
		if (element == null || !element.isJsonObject()) {
			return result;
		}
		JsonObject items = element.getAsJsonObject();
		for (GearItems.Entry entry : GearItems.all()) {
			JsonElement itemElement = items.get(entry.id());
			if (itemElement == null || !itemElement.isJsonObject()) {
				continue;
			}
			JsonObject o = itemElement.getAsJsonObject();
			Map<Stat, Double> item = new EnumMap<>(Stat.class);
			for (Stat stat : Stat.values()) {
				JsonElement value = o.get(stat.key);
				if (value != null && value.isJsonPrimitive() && stat.appliesTo(entry)) {
					double clamped = stat.clamp(value.getAsDouble(), entry);
					// A value equal to vanilla is no change, same as when it is typed in by hand
					if (Math.abs(clamped - stat.get(VanillaValues.get(entry))) >= 1.0E-6) {
						item.put(stat, clamped);
					}
				}
			}
			if (!item.isEmpty()) {
				result.put(entry.id(), item);
			}
		}
		return result;
	}

	// Numbers of the functions ("values" in config/gearbalance.json): gem bonuses, ore drop chances, chest loot
	// chances, the grindstone return chance, sockets, bags, the uses of the smithing tools, cooldowns and the view.
	// Missing or broken entries keep their default. Read again on /reload.

	public static final List<String> GEM_TYPES = List.of("ruby", "sapphire", "topaz", "emerald", "aquamarine", "onyx");
	/** Gem types found in structure chests (the rare aquamarine and onyx only come from their ores). */
	public static final List<String> CHEST_TYPES = List.of("ruby", "sapphire", "topaz", "emerald");
	/** Gem types with an uncut form that drops from ore (the uncut emerald is the vanilla emerald). */
	public static final List<String> ROUGH_TYPES = List.of("ruby", "sapphire", "topaz", "aquamarine", "onyx");
	/** Stages of a gem found in a chest, for the most gems per chest. */
	public static final List<String> DROP_STAGES = List.of("rough", "chipped", "cut", "flawless");
	/** Most gems one find may give. */
	public static final int MAX_DROP = 5;
	public static final List<String> LOOT_TIERS = List.of("easy", "medium", "hard");
	public static final List<String> SOCKET_MATERIALS = List.of("leather", "copper", "chainmail", "iron", "gold", "diamond", "netherite", "titanium");
	/** Shield materials ("wood" = vanilla shield), for the sockets of shields. */
	public static final List<String> SHIELD_MATERIALS = List.of("wood", "copper", "iron", "gold", "diamond", "netherite", "titanium");
	/** Most sockets a piece may have: 0 to 3. */
	public static final int MAX_SOCKETS = 4;

	/** Gem bonus (GemBonuses id) -> its value per cut (chipped, cut, flawless), for every gem sort that gives it. */
	public static final Map<String, double[]> GEM_VALUES = new LinkedHashMap<>();
	/** Rough gem type -> chance per mined ore block (0.05 = 5 %). */
	public static final Map<String, Double> ORE_DROP_CHANCE = new LinkedHashMap<>();
	/** Most uncut gems per mined ore block; a find gives 1 to this many. */
	public static int oreDropMax;
	/** Stage (rough, chipped, cut, flawless) -> most gems per chest; a find gives 1 to this many. */
	public static final Map<String, Integer> CHEST_DROP_MAX = new LinkedHashMap<>();
	/** Gem type with two bonuses -> share of the values per bonus, percent (50 to 100). Before the static block. */
	public static final Map<String, Integer> BONUS_SHARE = new LinkedHashMap<>();
	/** Loot tier -> chance per chest for one gem. */
	public static final Map<String, Double> CHEST_CHANCE = new LinkedHashMap<>();
	/** Chance that a gem removed at the grindstone comes back one cut lower. */
	public static double grindstoneReturnChance;
	/** Material -> most sockets a piece of it may have (0 to 3). */
	public static final Map<String, Integer> SOCKETS_PER_MATERIAL = new LinkedHashMap<>();
	/** Shield material -> most sockets a shield of it may have (0 to 3). */
	public static final Map<String, Integer> SHIELD_SOCKETS = new LinkedHashMap<>();
	/** Bag tiers, from the leather bag up. */
	public static final List<String> BAG_TIERS = List.of("leather", "copper", "iron", "golden", "diamond", "netherite", "titanium");
	/** Bag tier -> rows it adds above the inventory (1 to 6). */
	public static final Map<String, Integer> BAG_ROWS = new LinkedHashMap<>();
	public static final int MAX_BAG_ROWS = 7;
	/** Slots per bag row: 9 (as wide as the inventory) to 15, in steps of 2 so it stays centered. */
	public static final int MIN_BAG_COLUMNS = 9, MAX_BAG_COLUMNS = 15;
	public static int bagColumns = MAX_BAG_COLUMNS;

	/** 9, 11, 13 or 15: other numbers are rounded down to the next allowed one. */
	public static int bagColumns(int value) {
		int clamped = Math.max(MIN_BAG_COLUMNS, Math.min(MAX_BAG_COLUMNS, value));
		return clamped - (clamped - MIN_BAG_COLUMNS) % 2;
	}
	/** Amethyst shards for the 1st, 2nd and 3rd socket (1 to 64). */
	public static final int[] SOCKET_COST = new int[MAX_SOCKETS];
	/** Smithing tools (item path) whose uses can be set, in the order of the settings screen. */
	public static final List<String> TOOLS = List.of(
			"iron_forge_hammer", "golden_forge_hammer", "diamond_forge_hammer", "netherite_forge_hammer", "titanium_forge_hammer",
			"iron_socket_chisel", "golden_socket_chisel", "diamond_socket_chisel", "netherite_socket_chisel",
			"titanium_socket_chisel",
			"sewing_needle", "netherite_repair_kit", "diamond_gem_lever", "netherite_gem_lever",
			"titanium_gem_lever");
	public static final int MIN_DURABILITY = 20;

	/** Most uses that can be set: 50 for the repair kit, 1000 for the other tools. */
	public static int maxDurability(String tool) {
		return tool.equals("netherite_repair_kit") ? 50 : MAX_DURABILITY;
	}

	/** Fewest uses that can be set: 1 for the repair kit and the gem levers, 20 for the other tools. */
	public static int minDurability(String tool) {
		return tool.equals("netherite_repair_kit") || tool.endsWith("_gem_lever") ? 1 : MIN_DURABILITY;
	}
	public static final int MAX_DURABILITY = 1000;
	/** Tool (item path) -> uses until it breaks. */
	public static final Map<String, Integer> TOOL_DURABILITY = new LinkedHashMap<>();

	/** Weapons without a material tier -> the upgrade values that can be set for them (percent per level). */
	public static final Map<String, List<String>> WEAPON_UPGRADE_VALUES = new LinkedHashMap<>();
	/** Weapon -> upgrade value -> percent per level. */
	public static final Map<String, Map<String, Double>> WEAPON_UPGRADES = new LinkedHashMap<>();
	public static final double MAX_WEAPON_UPGRADE = 50;
	/** Shield material -> durability of the GearBalance shield of it (the vanilla shield is set under Gear). */
	public static final Map<String, Integer> SHIELD_DURABILITY = new LinkedHashMap<>();
	/** Shield material -> seconds after raising the shield until it blocks. Applies after a restart. */
	public static final Map<String, Double> SHIELD_BLOCK_DELAY = new LinkedHashMap<>();
	/** Shield material -> seconds an axe hit disables the shield. Applies after a restart. */
	public static final Map<String, Double> SHIELD_AXE_SECONDS = new LinkedHashMap<>();
	/** Shield material -> angle from the front up to which explosions are blocked. Applies after a restart. */
	public static final Map<String, Integer> SHIELD_EXPLOSION_ANGLE = new LinkedHashMap<>();
	/** Shield material -> percent of the damage blocked. Applies after a restart. */
	public static final Map<String, Integer> SHIELD_BLOCK = new LinkedHashMap<>();
	/** Shield material -> walking speed while blocking, percent of normal (vanilla 20). Applies after a restart. */
	public static final Map<String, Integer> SHIELD_SPEED = new LinkedHashMap<>();
	public static final int MIN_SHIELD_SPEED = 5;
	public static final int MAX_SHIELD_SPEED = 100;
	/** Shield material ("wood" also for the vanilla shield) -> how far a parry throws the attacker back. */
	public static final Map<String, Double> PARRY_KNOCKBACK = new LinkedHashMap<>();
	public static final int MAX_SHIELD_DURABILITY = 10000;
	public static final double MAX_SHIELD_BLOCK_DELAY = 1;
	public static final double MAX_SHIELD_AXE_SECONDS = 20;
	public static final int MIN_SHIELD_EXPLOSION_ANGLE = 90;
	public static final int MAX_SHIELD_EXPLOSION_ANGLE = 180;
	public static final int DEFAULT_SHIELD_BLOCK = 100;
	public static final double MAX_PARRY_KNOCKBACK = 3;

	static {
		valueDefaults();
	}

	private static void valueDefaults() {
		for (String type : GEM_TYPES) {
			if (GemBonuses.slots(type) > 1) {
				BONUS_SHARE.put(type, defaultShare(type));
			}
		}
		for (GemBonuses.Bonus bonus : GemBonuses.all()) {
			GEM_VALUES.put(bonus.id(), bonus.defaults().clone());
		}
		for (String type : ROUGH_TYPES) {
			ORE_DROP_CHANCE.put(type, defaultOreChance(type));
		}
		oreDropMax = 1;
		for (String stage : DROP_STAGES) {
			CHEST_DROP_MAX.put(stage, 1);
		}
		CHEST_CHANCE.put("easy", 0.25);
		CHEST_CHANCE.put("medium", 0.35);
		CHEST_CHANCE.put("hard", 0.5);
		grindstoneReturnChance = 0.5;
		int[] sockets = {1, 1, 1, 2, 2, 3, 3, 4};
		for (int i = 0; i < SOCKET_MATERIALS.size(); i++) {
			SOCKETS_PER_MATERIAL.put(SOCKET_MATERIALS.get(i), sockets[i]);
		}
		for (String material : SHIELD_MATERIALS) {
			SHIELD_SOCKETS.put(material, defaultShieldSockets(material));
		}
		for (String tier : BAG_TIERS) {
			BAG_ROWS.put(tier, defaultBagRows(tier));
		}
		bagColumns = MAX_BAG_COLUMNS;
		SOCKET_COST[0] = 4;
		SOCKET_COST[1] = 8;
		SOCKET_COST[2] = 16;
		SOCKET_COST[3] = 25;
		for (String tool : TOOLS) {
			TOOL_DURABILITY.put(tool, defaultDurability(tool));
		}
		upgradeCooldownHours = 0;
		socketCooldownHours = 0;
		shieldLowerPercent = DEFAULT_SHIELD_LOWER;
		fireLowerPercent = DEFAULT_FIRE_LOWER;
		effectRed = DEFAULT_EFFECT_RED;
		effectGreen = DEFAULT_EFFECT_GREEN;
		effectBlue = DEFAULT_EFFECT_BLUE;
		handEffect = EFFECT_GLOW;
		effectBrightness = DEFAULT_EFFECT_BRIGHTNESS;
		ringSaturation = DEFAULT_RING_SATURATION;
		glowPulse = true;
		pulseStrength = DEFAULT_PULSE_STRENGTH;
		pulseSeconds = DEFAULT_PULSE_SECONDS;
		shieldGlow = DEFAULT_SHIELD_GLOW;
		shieldFrame = DEFAULT_SHIELD_FRAME;
		shieldGlowShaders = true;
		hideGlint = false;
		pickupSeconds = DEFAULT_PICKUP_SECONDS;
		pickupRows = DEFAULT_PICKUP_ROWS;
		pickupScale = DEFAULT_PICKUP_SCALE;
		dayMinutes = DEFAULT_DAY_MINUTES;
		nightMinutes = DEFAULT_NIGHT_MINUTES;
		handIdlePercent = DEFAULT_HAND_IDLE;
		warnPercent = DEFAULT_WARN;
		armorCrackPercent = DEFAULT_ARMOR_CRACK;
		armorCrack2Percent = DEFAULT_ARMOR_CRACK2;
		armorDarkenPercent = DEFAULT_ARMOR_DARKEN;
		armorBlinkPercent = DEFAULT_ARMOR_BLINK;
		hotbarPopPercent = DEFAULT_HOTBAR_POP;
		hotbarGlowPercent = DEFAULT_HOTBAR_GLOW;
		deathDespawnMinutes = DEFAULT_DEATH_DESPAWN;
		deathXpPercent = DEFAULT_DEATH_XP;
		deathKeepDamage = DEFAULT_DEATH_KEEP_DAMAGE;
		spawnerIron = DEFAULT_SPAWNER_IRON;
		spawnerGemChance = DEFAULT_SPAWNER_GEM;
		inventoryGuiScale = DEFAULT_INVENTORY_GUI_SCALE;
		planChestChance = DEFAULT_PLAN_CHEST_CHANCE;
		planMobChance = DEFAULT_PLAN_MOB_CHANCE;
		WEAPON_UPGRADE_VALUES.put("mace", List.of("damage", "durability"));
		WEAPON_UPGRADE_VALUES.put("trident", List.of("damage", "durability"));
		WEAPON_UPGRADE_VALUES.put("bow", List.of("arrow_damage", "draw_speed", "durability"));
		WEAPON_UPGRADE_VALUES.put("crossbow", List.of("arrow_damage", "draw_speed", "durability"));
		WEAPON_UPGRADES.clear();
		WEAPON_UPGRADE_VALUES.forEach((weapon, keys) -> {
			Map<String, Double> map = new LinkedHashMap<>();
			for (String value : keys) {
				map.put(value, defaultWeaponUpgrade(value));
			}
			WEAPON_UPGRADES.put(weapon, map);
		});
		for (String material : SHIELD_MATERIALS) {
			SHIELD_DURABILITY.put(material, defaultShieldDurability(material));
			SHIELD_BLOCK_DELAY.put(material, defaultShieldBlockDelay(material));
			SHIELD_AXE_SECONDS.put(material, defaultShieldAxeSeconds(material));
			SHIELD_EXPLOSION_ANGLE.put(material, defaultShieldExplosionAngle(material));
			SHIELD_BLOCK.put(material, DEFAULT_SHIELD_BLOCK);
			SHIELD_SPEED.put(material, defaultShieldSpeed(material));
			PARRY_KNOCKBACK.put(material, defaultParryKnockback(material));
		}
	}

	/** Default upgrade value of a weapon: +6 % damage, +8 % arrow damage and draw speed, +10 % durability per level. */
	public static double defaultWeaponUpgrade(String value) {
		return switch (value) {
			case "damage" -> 6;
			case "arrow_damage", "draw_speed" -> 8;
			default -> 10;
		};
	}

	/** Upgrade value of a weapon in percent per level, 0 if the weapon or value has none. */
	public static double weaponUpgrade(String weapon, String value) {
		Map<String, Double> values = WEAPON_UPGRADES.get(weapon);
		Double result = values == null ? null : values.get(value);
		return result == null ? 0 : result;
	}

	private static int shieldIndex(String material) {
		int index = SHIELD_MATERIALS.indexOf(material);
		return index < 0 ? 0 : index;
	}

	public static int defaultShieldDurability(String material) {
		return new int[] {150, 400, 500, 600, 1000, 1500, 2000}[shieldIndex(material)];
	}

	public static double defaultShieldBlockDelay(String material) {
		return new double[] {0.25, 0.22, 0.2, 0.18, 0.15, 0.12, 0.1}[shieldIndex(material)];
	}

	public static double defaultShieldAxeSeconds(String material) {
		return new double[] {4, 3.5, 3, 2.5, 2, 1.5, 1.0}[shieldIndex(material)];
	}

	public static int defaultShieldExplosionAngle(String material) {
		return new int[] {90, 95, 100, 105, 110, 120, 130}[shieldIndex(material)];
	}

	/** Walking speed while blocking: wood as vanilla (20 %), up to 50 % for titanium. */
	public static int defaultShieldSpeed(String material) {
		return new int[] {20, 25, 35, 30, 40, 45, 50}[shieldIndex(material)];
	}

	public static double defaultParryKnockback(String material) {
		return new double[] {0.48, 0.58, 0.69, 0.79, 0.9, 1.0, 1.1}[shieldIndex(material)];
	}

	/** The shield values that are built into the items when the game starts. */
	private static String shieldSignature() {
		return SHIELD_BLOCK_DELAY + "|" + SHIELD_AXE_SECONDS + "|" + SHIELD_EXPLOSION_ANGLE + "|" + SHIELD_BLOCK
				+ "|" + shieldSpeedActive() + "|" + SHIELD_SPEED;
	}

	/** Sets every number of the functions back to its default (settings screen, "Reset all" and profiles). */
	public static void resetValues() {
		valueDefaults();
	}

	/** Default uses of a smithing tool: rising with the tier (iron, gold, diamond, netherite). */
	public static int defaultDurability(String tool) {
		switch (tool) {
			case "sewing_needle" -> {
				return 25;
			}
			case "netherite_repair_kit" -> {
				return 5;
			}
			case "diamond_gem_lever" -> {
				return 10;
			}
			case "netherite_gem_lever" -> {
				return 25;
			}
			case "titanium_gem_lever" -> {
				return 50;
			}
			default -> {
			}
		}
		boolean chisel = tool.endsWith("_socket_chisel");
		if (tool.startsWith("golden_")) {
			return chisel ? 50 : 75;
		}
		if (tool.startsWith("diamond_")) {
			return 100;
		}
		if (tool.startsWith("netherite_")) {
			return 200;
		}
		if (tool.startsWith("titanium_")) {
			return 300;
		}
		return chisel ? 25 : 50;
	}

	/** Default of a socket count, for the reset buttons of the settings screen. */
	public static int defaultSockets(String material) {
		return switch (material) {
			case "iron", "gold" -> 2;
			case "diamond", "netherite" -> 3;
			case "titanium" -> 4;
			default -> 1;
		};
	}

	/** Default socket count of a shield: 2 for diamond and netherite, 1 for the others. */
	/** Rows of a bag tier: leather 1, copper 2 ... netherite 6. */
	public static int defaultBagRows(String tier) {
		int index = BAG_TIERS.indexOf(tier);
		return index < 0 ? 1 : Math.min(MAX_BAG_ROWS, index + 1);
	}

	public static int defaultShieldSockets(String material) {
		if (material.equals("titanium")) {
			return 3;
		}
		return material.equals("diamond") || material.equals("netherite") ? 2 : 1;
	}

	/** Default of the socket cost (index 0 to 2). */
	public static int defaultSocketCost(int index) {
		return new int[] {4, 8, 16, 25}[index];
	}

	/** Value of a gem bonus for cut level 1 chipped, 2 cut, 3 flawless; 0 for anything else. */
	public static double gemValue(String bonus, int cut) {
		double[] values = GEM_VALUES.get(bonus);
		return values == null || cut < 1 || cut > 3 ? 0 : values[cut - 1];
	}

	/** Default value of a gem bonus for cut level 1 to 3, for the reset buttons. */
	public static double defaultGemValue(String bonus, int cut) {
		GemBonuses.Bonus kind = GemBonuses.get(bonus);
		return kind == null || cut < 1 || cut > 3 ? 0 : kind.defaults()[cut - 1];
	}

	public static final int MIN_SHARE = 50;
	public static final int MAX_SHARE = 100;

	/** Default share: aquamarine 50 % (two small bonuses), onyx 100 % (two full bonuses, endgame). */
	public static int defaultShare(String type) {
		return type.equals("onyx") ? 100 : 50;
	}

	/** Sets the share (percent) each of the two bonuses of an aquamarine or onyx gets. */
	public static void setShare(String type, int share) {
		BONUS_SHARE.put(type, Math.max(MIN_SHARE, Math.min(MAX_SHARE, share)));
	}

	/** Default chance of an uncut gem per mined ore block. */
	public static double defaultOreChance(String type) {
		return switch (type) {
			case "aquamarine" -> 0.03;
			case "onyx" -> 0.1;
			default -> DEFAULT_ORE_CHANCE;
		};
	}

	/** Defaults of the chances, for the reset buttons of the settings screen. */
	public static final double DEFAULT_ORE_CHANCE = 0.05;
	public static final double DEFAULT_RETURN_CHANCE = 0.5;
	public static final double DEFAULT_PLAN_CHEST_CHANCE = 0.1;
	public static final double DEFAULT_PLAN_MOB_CHANCE = 0.05;
	public static final int MAX_COOLDOWN_HOURS = 240;

	/** First person: shield drawn lower, percent (0 to 30) / strength of the idle hand movement (0 to 200). */
	public static int shieldLowerPercent;
	public static int handIdlePercent;
	public static final int DEFAULT_SHIELD_LOWER = 10;
	public static final int MAX_SHIELD_LOWER = 30;
	public static final int DEFAULT_HAND_IDLE = 100;
	public static final int MAX_HAND_IDLE = 200;
	/** First person view: how much lower the flames are drawn while burning, percent (0 to 25). */
	public static int fireLowerPercent;
	public static final int DEFAULT_FIRE_LOWER = 15;
	public static final int MAX_FIRE_LOWER = 25;
	/** Color of the hand effects and the glow outline (red, green, blue 0 to 255), default orange. */
	public static int effectRed;
	public static int effectGreen;
	public static int effectBlue;
	public static final int DEFAULT_EFFECT_RED = 255;
	public static final int DEFAULT_EFFECT_GREEN = 110;
	public static final int DEFAULT_EFFECT_BLUE = 20;
	/** Effect in the hand with full sockets: 0 glow outline, 1 flames, 2 shadow (only one at a time). */
	public static int handEffect;
	public static final int EFFECT_GLOW = 0;
	public static final int EFFECT_FLAMES = 1;
	public static final int EFFECT_SHADOW = 2;
	/** Opacity of the glow and the effects, percent (0 to 100). */
	public static int effectBrightness;
	public static final int DEFAULT_EFFECT_BRIGHTNESS = 75;
	/** The 1 px outline ring with its side faces: saturation of the effect color, percent. */
	public static int ringSaturation;
	public static final int DEFAULT_RING_SATURATION = 70;
	/** Pulse of the glow: on/off, how much it dims (percent of the brightness) and one pulse in seconds. */
	public static boolean glowPulse;
	public static int pulseStrength;
	public static final int DEFAULT_PULSE_STRENGTH = 30;
	public static int pulseSeconds;
	public static final int DEFAULT_PULSE_SECONDS = 3;
	public static final int MAX_PULSE_SECONDS = 10;
	/** Soft glow around a shield plate with full sockets, percent opacity. */
	public static int shieldGlow;
	public static final int DEFAULT_SHIELD_GLOW = 50;
	/** Brightness of the frame around a shield plate with full sockets, percent of the outline color. */
	public static int shieldFrame;
	public static final int DEFAULT_SHIELD_FRAME = 70;
	/** The soft shield glow also while a shader pack (Iris) is in use; off leaves only the frame there. */
	public static boolean shieldGlowShaders;
	/** Hides the enchantment glint of pieces with full sockets while the glow is on. */
	public static boolean hideGlint;
	/** Picked up items: seconds a line stays, lines at most, size in percent. */
	public static int pickupSeconds;
	public static int pickupRows;
	public static int pickupScale;
	public static final int DEFAULT_PICKUP_SECONDS = 3;
	public static final int MAX_PICKUP_SECONDS = 30;
	public static final int DEFAULT_PICKUP_ROWS = 5;
	public static final int MAX_PICKUP_ROWS = 10;
	public static final int DEFAULT_PICKUP_SCALE = 100;
	public static final int MIN_PICKUP_SCALE = 50;
	public static final int MAX_PICKUP_SCALE = 200;
	/** Real minutes of the day (time 0 to 12000) and of the night with dusk and dawn (12000 to 24000); 10 = vanilla. */
	public static int dayMinutes;
	public static int nightMinutes;
	public static final int DEFAULT_DAY_MINUTES = 10;
	public static final int DEFAULT_NIGHT_MINUTES = 10;
	public static final int MAX_DAY_NIGHT_MINUTES = 120;
	/** Durability left (percent, 0 to 50) at which the warning sound plays; 0 = never. */
	public static int warnPercent;
	public static final int DEFAULT_WARN = 10;
	public static final int MAX_WARN = 50;
	/** Armor bar: durability left (percent) for the first and the second crack. */
	public static int armorCrackPercent;
	public static int armorCrack2Percent;
	public static final int DEFAULT_ARMOR_CRACK = 50;
	public static final int DEFAULT_ARMOR_CRACK2 = 25;
	/** Armor bar: how much darker a fully worn piece gets (percent). */
	public static int armorDarkenPercent;
	public static final int DEFAULT_ARMOR_DARKEN = 40;
	public static final int MAX_ARMOR_DARKEN = 80;
	/** Armor bar: durability left (percent) from which a piece blinks red. */
	public static int armorBlinkPercent;
	public static final int DEFAULT_ARMOR_BLINK = 10;
	/** Hotbar: how much bigger the selected item pops (percent; it rests at half of it). */
	public static int hotbarPopPercent;
	public static final int DEFAULT_HOTBAR_POP = 25;
	public static final int MAX_HOTBAR_POP = 50;
	/** Hotbar: durability left (percent) from which a slot glows red. */
	public static int hotbarGlowPercent;
	public static final int DEFAULT_HOTBAR_GLOW = 10;
	/** Death: minutes until death items despawn; 0 = never. */
	public static int deathDespawnMinutes;
	public static final int DEFAULT_DEATH_DESPAWN = 0;
	public static final int MAX_DEATH_DESPAWN = 30;
	/** Death: share of all experience points that drops (percent). */
	public static int deathXpPercent;
	public static final int DEFAULT_DEATH_XP = 80;
	public static final int MAX_PERCENT_100 = 100;
	/** Death: durability a kept piece loses (percent of its maximum). */
	public static int deathKeepDamage;
	public static final int DEFAULT_DEATH_KEEP_DAMAGE = 10;
	public static final int MAX_DEATH_KEEP_DAMAGE = 50;
	/** Spawner: iron ingots from the cage. */
	public static int spawnerIron;
	public static final int DEFAULT_SPAWNER_IRON = 2;
	public static final int MAX_SPAWNER_IRON = 9;
	/** Spawner: chance of a chipped gem (percent). */
	public static int spawnerGemChance;
	public static final int DEFAULT_SPAWNER_GEM = 25;
	/** GUI size while the inventory is open (1 to 8); used only when it is smaller than the normal size. */
	public static int inventoryGuiScale;
	public static final int DEFAULT_INVENTORY_GUI_SCALE = 6;
	public static final int MAX_INVENTORY_GUI_SCALE = 8;

	/** In-game hours (1000 ticks) a piece has to wait between two upgrades / two new sockets; 0 = no wait. */
	public static int upgradeCooldownHours;
	public static int socketCooldownHours;
	/** Chance of a master smithing plan in a chest of a hard place / from a wither skeleton or piglin brute. */
	public static double planChestChance;
	public static double planMobChance;

	public static double defaultChestChance(String tier) {
		return switch (tier) {
			case "easy" -> 0.25;
			case "medium" -> 0.35;
			default -> 0.5;
		};
	}

	private static void readValues(JsonObject o) {
		JsonObject gems = object(o, "gems");
		if (gems != null) {
			JsonObject share = object(gems, "bonusShare");
			if (share != null) {
				for (String type : BONUS_SHARE.keySet()) {
					BONUS_SHARE.put(type, whole(share, type, BONUS_SHARE.get(type), MIN_SHARE, MAX_SHARE));
				}
			}
			JsonObject values = object(gems, "values");
			if (values != null) {
				for (GemBonuses.Bonus bonus : GemBonuses.all()) {
					readCuts(values, bonus.id(), GEM_VALUES.get(bonus.id()), bonus.max());
				}
			}
		}
		JsonObject ore = object(o, "oreDropChance");
		if (ore != null) {
			for (String type : ROUGH_TYPES) {
				ORE_DROP_CHANCE.put(type, chance(ore, type, ORE_DROP_CHANCE.get(type)));
			}
		}
		oreDropMax = whole(o, "mostGemsPerOre", oreDropMax, 1, MAX_DROP);
		JsonObject dropMax = object(o, "mostGemsPerChest");
		if (dropMax != null) {
			for (String stage : DROP_STAGES) {
				CHEST_DROP_MAX.put(stage, whole(dropMax, stage, CHEST_DROP_MAX.get(stage), 1, MAX_DROP));
			}
		}
		JsonObject chests = object(o, "chestLootChance");
		if (chests != null) {
			for (String tier : LOOT_TIERS) {
				CHEST_CHANCE.put(tier, chance(chests, tier, CHEST_CHANCE.get(tier)));
			}
		}
		grindstoneReturnChance = chance(o, "grindstoneReturnChance", grindstoneReturnChance);
		planChestChance = chance(o, "planChestChance", planChestChance);
		planMobChance = chance(o, "planMobChance", planMobChance);
		upgradeCooldownHours = whole(o, "upgradeCooldownHours", upgradeCooldownHours, 0, MAX_COOLDOWN_HOURS);
		shieldLowerPercent = whole(o, "shieldLowerPercent", shieldLowerPercent, 0, MAX_SHIELD_LOWER);
		fireLowerPercent = whole(o, "fireLowerPercent", fireLowerPercent, 0, MAX_FIRE_LOWER);
		effectRed = whole(o, "effectRed", effectRed, 0, 255);
		effectGreen = whole(o, "effectGreen", effectGreen, 0, 255);
		effectBlue = whole(o, "effectBlue", effectBlue, 0, 255);
		handEffect = whole(o, "handEffect", handEffect, EFFECT_GLOW, 3);
		if (handEffect > EFFECT_SHADOW) {
			handEffect = EFFECT_GLOW; // old files may still hold a removed effect
		}
		effectBrightness = whole(o, "effectBrightness", effectBrightness, 0, 100);
		ringSaturation = whole(o, "ringSaturation", ringSaturation, 0, 100);
		glowPulse = bool(o, "glowPulse", glowPulse);
		pulseStrength = whole(o, "pulseStrength", pulseStrength, 0, 100);
		pulseSeconds = whole(o, "pulseSeconds", pulseSeconds, 1, MAX_PULSE_SECONDS);
		shieldGlow = whole(o, "shieldGlow", shieldGlow, 0, 100);
		shieldFrame = whole(o, "shieldFrame", shieldFrame, 0, 100);
		shieldGlowShaders = bool(o, "shieldGlowShaders", shieldGlowShaders);
		hideGlint = bool(o, "hideGlint", hideGlint);
		pickupSeconds = whole(o, "pickupSeconds", pickupSeconds, 1, MAX_PICKUP_SECONDS);
		pickupRows = whole(o, "pickupRows", pickupRows, 1, MAX_PICKUP_ROWS);
		pickupScale = whole(o, "pickupScale", pickupScale, MIN_PICKUP_SCALE, MAX_PICKUP_SCALE);
		dayMinutes = whole(o, "dayMinutes", dayMinutes, 1, MAX_DAY_NIGHT_MINUTES);
		nightMinutes = whole(o, "nightMinutes", nightMinutes, 1, MAX_DAY_NIGHT_MINUTES);
		handIdlePercent = whole(o, "handIdlePercent", handIdlePercent, 0, MAX_HAND_IDLE);
		warnPercent = whole(o, "durabilityWarningPercent", warnPercent, 0, MAX_WARN);
		armorCrackPercent = whole(o, "armorCrackPercent", armorCrackPercent, 0, 100);
		armorCrack2Percent = whole(o, "armorCrack2Percent", armorCrack2Percent, 0, 100);
		armorDarkenPercent = whole(o, "armorDarkenPercent", armorDarkenPercent, 0, MAX_ARMOR_DARKEN);
		armorBlinkPercent = whole(o, "armorBlinkPercent", armorBlinkPercent, 0, MAX_WARN);
		hotbarPopPercent = whole(o, "hotbarPopPercent", hotbarPopPercent, 0, MAX_HOTBAR_POP);
		hotbarGlowPercent = whole(o, "hotbarGlowPercent", hotbarGlowPercent, 0, MAX_WARN);
		deathDespawnMinutes = whole(o, "deathDespawnMinutes", deathDespawnMinutes, 0, MAX_DEATH_DESPAWN);
		deathXpPercent = whole(o, "deathXpPercent", deathXpPercent, 0, MAX_PERCENT_100);
		deathKeepDamage = whole(o, "deathKeepDamage", deathKeepDamage, 0, MAX_DEATH_KEEP_DAMAGE);
		spawnerIron = whole(o, "spawnerIron", spawnerIron, 0, MAX_SPAWNER_IRON);
		spawnerGemChance = whole(o, "spawnerGemChance", spawnerGemChance, 0, MAX_PERCENT_100);
		inventoryGuiScale = whole(o, "inventoryGuiScale", inventoryGuiScale, 1, MAX_INVENTORY_GUI_SCALE);
		socketCooldownHours = whole(o, "socketCooldownHours", socketCooldownHours, 0, MAX_COOLDOWN_HOURS);
		JsonObject sockets = object(o, "maxSockets");
		if (sockets != null) {
			for (String material : SOCKET_MATERIALS) {
				SOCKETS_PER_MATERIAL.put(material, whole(sockets, material, SOCKETS_PER_MATERIAL.get(material), 0, MAX_SOCKETS));
			}
		}
		JsonObject shieldSockets = object(o, "maxShieldSockets");
		if (shieldSockets != null) {
			for (String material : SHIELD_MATERIALS) {
				SHIELD_SOCKETS.put(material, whole(shieldSockets, material, SHIELD_SOCKETS.get(material), 0, MAX_SOCKETS));
			}
		}
		JsonObject bagRows = object(o, "bagRows");
		if (bagRows != null) {
			for (String tier : BAG_TIERS) {
				BAG_ROWS.put(tier, whole(bagRows, tier, BAG_ROWS.get(tier), 1, MAX_BAG_ROWS));
			}
		}
		bagColumns = bagColumns(whole(o, "bagColumns", bagColumns, MIN_BAG_COLUMNS, MAX_BAG_COLUMNS));
		JsonElement cost = o.get("socketCost");
		// Older files have 3 costs; the 4th then keeps its default
		if (cost != null && cost.isJsonArray()) {
			for (int i = 0; i < Math.min(MAX_SOCKETS, cost.getAsJsonArray().size()); i++) {
				JsonElement v = cost.getAsJsonArray().get(i);
				if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
					SOCKET_COST[i] = Math.max(1, Math.min(64, v.getAsInt()));
				}
			}
		}
		JsonObject durability = object(o, "toolDurability");
		if (durability != null) {
			for (String tool : TOOLS) {
				TOOL_DURABILITY.put(tool, whole(durability, tool, TOOL_DURABILITY.get(tool), minDurability(tool), maxDurability(tool)));
			}
		}
		JsonObject weapons = object(o, "weaponUpgrades");
		if (weapons != null) {
			WEAPON_UPGRADE_VALUES.forEach((weapon, keys) -> {
				JsonObject entry = object(weapons, weapon);
				if (entry != null) {
					for (String value : keys) {
						WEAPON_UPGRADES.get(weapon).put(value,
								decimal(entry, value, WEAPON_UPGRADES.get(weapon).get(value), 0, MAX_WEAPON_UPGRADE));
					}
				}
			});
		}
		JsonObject shieldDurability = object(o, "shieldDurability");
		JsonObject blockDelay = object(o, "shieldBlockDelay");
		JsonObject axeSeconds = object(o, "shieldAxeSeconds");
		JsonObject explosionAngle = object(o, "shieldExplosionAngle");
		JsonObject shieldBlock = object(o, "shieldBlock");
		JsonObject parry = object(o, "parryKnockback");
		JsonObject shieldSpeedValues = object(o, "shieldWalkSpeed");
		for (String material : SHIELD_MATERIALS) {
			if (shieldDurability != null) {
				SHIELD_DURABILITY.put(material, whole(shieldDurability, material, SHIELD_DURABILITY.get(material), 1,
						MAX_SHIELD_DURABILITY));
			}
			if (blockDelay != null) {
				SHIELD_BLOCK_DELAY.put(material, decimal(blockDelay, material, SHIELD_BLOCK_DELAY.get(material), 0,
						MAX_SHIELD_BLOCK_DELAY));
			}
			if (axeSeconds != null) {
				SHIELD_AXE_SECONDS.put(material, decimal(axeSeconds, material, SHIELD_AXE_SECONDS.get(material), 0,
						MAX_SHIELD_AXE_SECONDS));
			}
			if (explosionAngle != null) {
				SHIELD_EXPLOSION_ANGLE.put(material, whole(explosionAngle, material, SHIELD_EXPLOSION_ANGLE.get(material),
						MIN_SHIELD_EXPLOSION_ANGLE, MAX_SHIELD_EXPLOSION_ANGLE));
			}
			if (shieldBlock != null) {
				SHIELD_BLOCK.put(material, whole(shieldBlock, material, SHIELD_BLOCK.get(material), 0, 100));
			}
			if (shieldSpeedValues != null) {
				SHIELD_SPEED.put(material, whole(shieldSpeedValues, material, SHIELD_SPEED.get(material), MIN_SHIELD_SPEED,
						MAX_SHIELD_SPEED));
			}
			if (parry != null) {
				PARRY_KNOCKBACK.put(material, decimal(parry, material, PARRY_KNOCKBACK.get(material), 0, MAX_PARRY_KNOCKBACK));
			}
		}
	}

	/** A number from min to max; anything else keeps the fallback. */
	private static double decimal(JsonObject o, String key, double fallback, double min, double max) {
		JsonElement e = o.get(key);
		if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			return Math.max(min, Math.min(max, e.getAsDouble()));
		}
		return fallback;
	}

	/** A whole number from min to max; anything else keeps the fallback. */
	private static int whole(JsonObject o, String key, int fallback, int min, int max) {
		JsonElement e = o.get(key);
		if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			return Math.max(min, Math.min(max, e.getAsInt()));
		}
		return fallback;
	}

	private static JsonObject object(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
	}

	private static void readCuts(JsonObject o, String key, double[] target, double max) {
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() != 3) {
			return;
		}
		JsonArray array = e.getAsJsonArray();
		for (int i = 0; i < 3; i++) {
			JsonElement v = array.get(i);
			if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
				target[i] = Math.max(0, Math.min(max, v.getAsDouble()));
			}
		}
	}

	/** A number from 0 to 1; anything else keeps the fallback. */
	private static double chance(JsonObject o, String key, double fallback) {
		JsonElement e = o.get(key);
		if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			return Math.max(0, Math.min(1, e.getAsDouble()));
		}
		return fallback;
	}

	/** Replaces every value with the one in the object; missing or broken entries get their default. */
	public static void valuesFromJson(JsonObject o) {
		valueDefaults();
		readValues(o);
		Upgrades.clearCache();
	}

	/** Every value as written to the file, also used for profiles and exports. */
	public static JsonObject valuesToJson() {
		JsonObject o = new JsonObject();
		o.addProperty("_info", "Also editable in the game (Mod Menu > GearBalance > Functions). Gem bonuses: 'values' = value of each bonus per cut [chipped, cut, flawless], percent bonuses in percent; 'bonusShare' = percent of these values for each of the two bonuses of aquamarine and onyx (50 to 100). Chances: 0.05 = 5 %. Most gems per ore block / per chest: 1 to 5. Tool durability: uses, 20 to 1000. Cooldowns: in-game hours, 0 = off. Weapon upgrades: percent per level. Shield block delay, axe seconds and explosion angle apply after a restart. Other changes in this file apply after /reload.");
		JsonObject gems = new JsonObject();
		JsonObject share = new JsonObject();
		BONUS_SHARE.forEach(share::addProperty);
		gems.add("bonusShare", share);
		JsonObject values = new JsonObject();
		GEM_VALUES.forEach((bonus, cuts) -> values.add(bonus, array(cuts)));
		gems.add("values", values);
		o.add("gems", gems);
		JsonObject ore = new JsonObject();
		ORE_DROP_CHANCE.forEach(ore::addProperty);
		o.add("oreDropChance", ore);
		o.addProperty("mostGemsPerOre", oreDropMax);
		JsonObject dropMax = new JsonObject();
		CHEST_DROP_MAX.forEach(dropMax::addProperty);
		o.add("mostGemsPerChest", dropMax);
		JsonObject chests = new JsonObject();
		CHEST_CHANCE.forEach(chests::addProperty);
		o.add("chestLootChance", chests);
		o.addProperty("grindstoneReturnChance", grindstoneReturnChance);
		o.addProperty("planChestChance", planChestChance);
		o.addProperty("planMobChance", planMobChance);
		o.addProperty("upgradeCooldownHours", upgradeCooldownHours);
		o.addProperty("shieldLowerPercent", shieldLowerPercent);
		o.addProperty("fireLowerPercent", fireLowerPercent);
		o.addProperty("effectRed", effectRed);
		o.addProperty("effectGreen", effectGreen);
		o.addProperty("effectBlue", effectBlue);
		o.addProperty("handEffect", handEffect);
		o.addProperty("effectBrightness", effectBrightness);
		o.addProperty("ringSaturation", ringSaturation);
		o.addProperty("glowPulse", glowPulse);
		o.addProperty("pulseStrength", pulseStrength);
		o.addProperty("pulseSeconds", pulseSeconds);
		o.addProperty("shieldGlow", shieldGlow);
		o.addProperty("shieldFrame", shieldFrame);
		o.addProperty("shieldGlowShaders", shieldGlowShaders);
		o.addProperty("hideGlint", hideGlint);
		o.addProperty("pickupSeconds", pickupSeconds);
		o.addProperty("pickupRows", pickupRows);
		o.addProperty("pickupScale", pickupScale);
		o.addProperty("dayMinutes", dayMinutes);
		o.addProperty("nightMinutes", nightMinutes);
		o.addProperty("handIdlePercent", handIdlePercent);
		o.addProperty("durabilityWarningPercent", warnPercent);
		o.addProperty("armorCrackPercent", armorCrackPercent);
		o.addProperty("armorCrack2Percent", armorCrack2Percent);
		o.addProperty("armorDarkenPercent", armorDarkenPercent);
		o.addProperty("armorBlinkPercent", armorBlinkPercent);
		o.addProperty("hotbarPopPercent", hotbarPopPercent);
		o.addProperty("hotbarGlowPercent", hotbarGlowPercent);
		o.addProperty("deathDespawnMinutes", deathDespawnMinutes);
		o.addProperty("deathXpPercent", deathXpPercent);
		o.addProperty("deathKeepDamage", deathKeepDamage);
		o.addProperty("spawnerIron", spawnerIron);
		o.addProperty("spawnerGemChance", spawnerGemChance);
		o.addProperty("inventoryGuiScale", inventoryGuiScale);
		o.addProperty("socketCooldownHours", socketCooldownHours);
		JsonObject sockets = new JsonObject();
		SOCKETS_PER_MATERIAL.forEach(sockets::addProperty);
		o.add("maxSockets", sockets);
		JsonObject shieldSockets = new JsonObject();
		SHIELD_SOCKETS.forEach(shieldSockets::addProperty);
		o.add("maxShieldSockets", shieldSockets);
		JsonObject bagRows = new JsonObject();
		BAG_ROWS.forEach(bagRows::addProperty);
		o.add("bagRows", bagRows);
		o.addProperty("bagColumns", bagColumns);
		JsonArray cost = new JsonArray();
		for (int c : SOCKET_COST) {
			cost.add(c);
		}
		o.add("socketCost", cost);
		JsonObject durability = new JsonObject();
		TOOL_DURABILITY.forEach(durability::addProperty);
		o.add("toolDurability", durability);
		JsonObject weapons = new JsonObject();
		WEAPON_UPGRADES.forEach((weapon, upgrades) -> {
			JsonObject entry = new JsonObject();
			upgrades.forEach(entry::addProperty);
			weapons.add(weapon, entry);
		});
		o.add("weaponUpgrades", weapons);
		JsonObject shieldDurability = new JsonObject();
		SHIELD_DURABILITY.forEach(shieldDurability::addProperty);
		o.add("shieldDurability", shieldDurability);
		JsonObject blockDelay = new JsonObject();
		SHIELD_BLOCK_DELAY.forEach(blockDelay::addProperty);
		o.add("shieldBlockDelay", blockDelay);
		JsonObject axeSeconds = new JsonObject();
		SHIELD_AXE_SECONDS.forEach(axeSeconds::addProperty);
		o.add("shieldAxeSeconds", axeSeconds);
		JsonObject explosionAngle = new JsonObject();
		SHIELD_EXPLOSION_ANGLE.forEach(explosionAngle::addProperty);
		o.add("shieldExplosionAngle", explosionAngle);
		JsonObject shieldBlock = new JsonObject();
		SHIELD_BLOCK.forEach(shieldBlock::addProperty);
		o.add("shieldBlock", shieldBlock);
		JsonObject shieldSpeedValues = new JsonObject();
		SHIELD_SPEED.forEach(shieldSpeedValues::addProperty);
		o.add("shieldWalkSpeed", shieldSpeedValues);
		JsonObject parry = new JsonObject();
		PARRY_KNOCKBACK.forEach(parry::addProperty);
		o.add("parryKnockback", parry);
		return o;
	}

	private static JsonArray array(double[] values) {
		JsonArray array = new JsonArray();
		for (double v : values) {
			array.add(v);
		}
		return array;
	}

	private static JsonObject defaultValues;

	/** Number of entries in the object that differ from the default numbers (for the profile list). */
	private static int countValues(JsonObject o) {
		if (o == null) {
			return 0;
		}
		if (defaultValues == null) {
			JsonObject now = valuesToJson();
			valueDefaults();
			defaultValues = valuesToJson();
			valuesFromJson(now);
		}
		Map<String, JsonElement> base = new HashMap<>();
		flatten("", defaultValues, base);
		Map<String, JsonElement> other = new HashMap<>();
		flatten("", o, other);
		int count = 0;
		for (Map.Entry<String, JsonElement> entry : other.entrySet()) {
			if (!Objects.equals(entry.getValue(), base.get(entry.getKey()))) {
				count++;
			}
		}
		return count;
	}

	private static void flatten(String prefix, JsonElement element, Map<String, JsonElement> into) {
		if (element.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
				if (!entry.getKey().startsWith("_")) {
					flatten(prefix + "/" + entry.getKey(), entry.getValue(), into);
				}
			}
		} else if (element.isJsonArray()) {
			for (int i = 0; i < element.getAsJsonArray().size(); i++) {
				flatten(prefix + "/" + i, element.getAsJsonArray().get(i), into);
			}
		} else {
			into.put(prefix, element);
		}
	}
}
