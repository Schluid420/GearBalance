package io.github.schluid.gearbalance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.bonus.BonusType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings, stored in config/gearbalance.json.
 *
 * Only changed values are written to the file, everything else stays vanilla:
 * "items": { "minecraft:diamond_sword": { "durability": 1951, "damage": 8 } }
 * "materials" holds the values per material: "iron": { "level": 10, "repair": 20, "weight": 2 }
 * "sets" holds the set bonus per material: "iron": { "bonus": "health", "strength": 2 }
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

	public static boolean enabled = true;
	public static boolean tridentRecipe = false;
	public static boolean chainmailRecipes = false;
	/** 1 leather -> 4 rabbit hides. The other way round already exists in vanilla. */
	public static boolean rabbitHideRecipe = false;
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
	/** Set once the note about the official download was shown. */
	public static boolean welcomeShown = false;
	/** Main switch for the set bonuses, the values per material stay saved while it is off. */
	public static boolean setBonuses = false;

	private static Map<String, Map<Stat, Double>> overrides = new LinkedHashMap<>();
	private static Map<MaterialStat, Map<Material, Double>> materials = new EnumMap<>(MaterialStat.class);
	private static Map<Material, SetEntry> sets = new EnumMap<>(Material.class);
	private static final Profile[] PROFILES = new Profile[PROFILE_COUNT];

	public static final class Profile {
		/** Empty = the screen shows "Profile 1", "Profile 2", ... */
		public String name = "";
		/** null while nothing is saved in this slot. */
		Map<String, Map<Stat, Double>> overrides;
		Map<MaterialStat, Map<Material, Double>> materials = new EnumMap<>(MaterialStat.class);
		Map<Material, SetEntry> sets = new EnumMap<>(Material.class);
		/** Options like loot and recipes, null in profiles saved before they were included. */
		JsonObject switches;

		public boolean isEmpty() {
			return overrides == null;
		}

		public int changeCount() {
			return overrides == null ? 0 : count(overrides) + countMaterials(materials) + sets.size();
		}
	}

	/** Set bonus of one material. Only stored while it differs from the default (strength 0). */
	public record SetEntry(BonusType type, double strength) {
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
		return enabled + "|" + itemsToJson(overrides) + "|" + materials.get(MaterialStat.WEIGHT);
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
		Map<Material, Double> values = materials.computeIfAbsent(stat, k -> new EnumMap<>(Material.class));
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
		PROFILES[index].sets = new EnumMap<>(sets);
		PROFILES[index].switches = switchesToJson();
	}

	public static void loadProfile(int index) {
		if (!PROFILES[index].isEmpty()) {
			overrides = copy(PROFILES[index].overrides);
			materials = copyMaterials(PROFILES[index].materials);
			sets = new EnumMap<>(PROFILES[index].sets);
			if (PROFILES[index].switches != null) {
				applySwitches(PROFILES[index].switches);
			}
		}
	}

	// Switches that belong to a setup. "Mod active" and the welcome note stay out of it.

	private static JsonObject switchesToJson() {
		JsonObject o = new JsonObject();
		o.addProperty("setBonuses", setBonuses);
		o.addProperty("tridentRecipe", tridentRecipe);
		o.addProperty("chainmailRecipes", chainmailRecipes);
		o.addProperty("rabbitHideRecipe", rabbitHideRecipe);
		o.addProperty("rabbitHideArmor", rabbitHideArmor);
		o.addProperty("anvilNoLimit", anvilNoLimit);
		o.addProperty("anvilMaxCost", anvilMaxCost);
		o.addProperty("zombieLeather", zombieLeather);
		o.addProperty("zombieLeatherChance", zombieLeatherChance);
		o.addProperty("lootTemplates", lootTemplates);
		o.addProperty("lootMaterials", lootMaterials);
		o.addProperty("lootNoCheap", lootNoCheap);
		return o;
	}

	/** Missing entries keep their current value. */
	private static void applySwitches(JsonObject o) {
		setBonuses = bool(o, "setBonuses", setBonuses);
		tridentRecipe = bool(o, "tridentRecipe", tridentRecipe);
		chainmailRecipes = bool(o, "chainmailRecipes", chainmailRecipes);
		rabbitHideRecipe = bool(o, "rabbitHideRecipe", rabbitHideRecipe);
		rabbitHideArmor = bool(o, "rabbitHideArmor", rabbitHideArmor);
		anvilNoLimit = bool(o, "anvilNoLimit", anvilNoLimit);
		anvilMaxCost = number(o, "anvilMaxCost", 1, ANVIL_MAX_COST_LIMIT, anvilMaxCost);
		zombieLeather = bool(o, "zombieLeather", zombieLeather);
		zombieLeatherChance = number(o, "zombieLeatherChance", 1, 100, zombieLeatherChance);
		lootTemplates = bool(o, "lootTemplates", lootTemplates);
		lootMaterials = bool(o, "lootMaterials", lootMaterials);
		lootNoCheap = bool(o, "lootNoCheap", lootNoCheap);
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
		root.add("switches", switchesToJson());
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
			Map<String, Map<Stat, Double>> newItems = itemsFromJson(root.get("items"));
			Map<MaterialStat, Map<Material, Double>> newMaterials = materialsFromJson(root.get("materials"));
			Map<Material, SetEntry> newSets = setsFromJson(root.get("sets"));
			overrides = newItems;
			materials = newMaterials;
			sets = newSets;
			JsonElement switches = root.get("switches");
			if (switches != null && switches.isJsonObject()) {
				applySwitches(switches.getAsJsonObject());
			}
			return true;
		} catch (IOException | RuntimeException e) {
			GearBalance.LOGGER.error("Could not import {}", fileName, e);
			return false;
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
		source.forEach((stat, values) -> result.put(stat, new EnumMap<>(values)));
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
		rabbitHideArmor = bool(root, "rabbitHideArmor", false);
		anvilNoLimit = bool(root, "anvilNoLimit", false);
		anvilMaxCost = number(root, "anvilMaxCost", 1, ANVIL_MAX_COST_LIMIT, VANILLA_ANVIL_MAX_COST);
		zombieLeather = bool(root, "zombieLeather", false);
		zombieLeatherChance = number(root, "zombieLeatherChance", 1, 100, DEFAULT_ZOMBIE_LEATHER_CHANCE);
		overrides = itemsFromJson(root.get("items"));
		materials = materialsFromJson(root.get("materials"));
		sets = setsFromJson(root.get("sets"));
		setBonuses = bool(root, "setBonuses", false);
		welcomeShown = bool(root, "welcomeShown", false);
		lootTemplates = bool(root, "lootTemplates", false);
		lootMaterials = bool(root, "lootMaterials", false);
		lootNoCheap = bool(root, "lootNoCheap", false);

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
				JsonElement switches = p.get("switches");
				profile.switches = switches != null && switches.isJsonObject() ? switches.getAsJsonObject() : null;
			}
			PROFILES[i] = profile;
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
		saves++;
		JsonObject root = new JsonObject();
		root.addProperty("enabled", enabled);
		root.addProperty("tridentRecipe", tridentRecipe);
		root.addProperty("chainmailRecipes", chainmailRecipes);
		root.addProperty("rabbitHideRecipe", rabbitHideRecipe);
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
		root.add("items", itemsToJson(overrides));
		root.add("materials", materialsToJson(materials));
		root.add("sets", setsToJson(sets));

		JsonArray profiles = new JsonArray();
		for (Profile profile : PROFILES) {
			JsonObject p = new JsonObject();
			p.addProperty("name", profile.name);
			if (!profile.isEmpty()) {
				p.add("items", itemsToJson(profile.overrides));
				p.add("materials", materialsToJson(profile.materials));
				p.add("sets", setsToJson(profile.sets));
				if (profile.switches != null) {
					p.add("switches", profile.switches);
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
		map.forEach((material, set) -> {
			JsonObject o = new JsonObject();
			o.addProperty("bonus", set.type().key);
			o.addProperty("strength", set.strength());
			result.add(material.key, o);
		});
		return result;
	}

	private static Map<Material, SetEntry> setsFromJson(JsonElement element) {
		Map<Material, SetEntry> result = new EnumMap<>(Material.class);
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
					result.computeIfAbsent(stat, k -> new EnumMap<>(Material.class)).put(material, clamped);
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
					item.put(stat, stat.clamp(value.getAsDouble(), entry));
				}
			}
			if (!item.isEmpty()) {
				result.put(entry.id(), item);
			}
		}
		return result;
	}
}
