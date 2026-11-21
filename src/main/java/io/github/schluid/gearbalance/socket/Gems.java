package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Gems: ruby, sapphire, topaz, emerald, rare aquamarine and onyx in three cuts (chipped, cut, flawless) and sorts (GemSorts).
 * Item id "<cut>_<sort>_<type>", e.g. "flawless_bold_ruby"; a type and cut share one texture.
 * Drops (small chance): ruby from redstone, sapphire from lapis, topaz from gold ore, aquamarine from diamond ore, onyx from ancient debris;
 * the uncut emerald is vanilla. Stonecutter: uncut gem = chipped gem of a sort; three of a cut and sort = next cut.
 */
public final class Gems {
	private Gems() {
	}

	public static final List<String> TYPES = GearConfig.GEM_TYPES;
	public static final List<String> CUTS = List.of("chipped", "cut", "flawless");

	/** Cut gem -> {type, cut level 1..3, sort}. */
	private static final Map<Item, Object[]> GEMS = new LinkedHashMap<>();
	/** "cut|sort key" -> cut gem. */
	private static final Map<String, Item> BY_SORT = new LinkedHashMap<>();
	/** Rough gems, in the order of TYPES (no rough emerald). */
	private static final Map<String, Item> ROUGH = new LinkedHashMap<>();
	/** Item id ("gearbalance:cut_ruby") -> cut gem, for the gems stored in sockets. */
	private static final Map<String, Item> BY_ID = new LinkedHashMap<>();
	/** Ore loot table -> rough gem type. */
	private static final Map<String, String> ORES = Map.ofEntries(
			Map.entry("minecraft:blocks/redstone_ore", "ruby"),
			Map.entry("minecraft:blocks/deepslate_redstone_ore", "ruby"),
			Map.entry("minecraft:blocks/lapis_ore", "sapphire"),
			Map.entry("minecraft:blocks/deepslate_lapis_ore", "sapphire"),
			Map.entry("minecraft:blocks/gold_ore", "topaz"),
			Map.entry("minecraft:blocks/deepslate_gold_ore", "topaz"),
			Map.entry("minecraft:blocks/nether_gold_ore", "topaz"),
			Map.entry("minecraft:blocks/diamond_ore", "aquamarine"),
			Map.entry("minecraft:blocks/deepslate_diamond_ore", "aquamarine"),
			Map.entry("minecraft:blocks/ancient_debris", "onyx"));

	public static void register() {
		for (String type : TYPES) {
			if (!type.equals("emerald")) {
				ROUGH.put(type, item(type));
			}
		}
		for (GemSorts.Sort sort : GemSorts.ALL) {
			for (int i = 0; i < CUTS.size(); i++) {
				String path = CUTS.get(i) + "_" + sort.key() + "_" + sort.type();
				Item gem = item(path);
				GEMS.put(gem, new Object[] {sort.type(), i + 1, sort});
				BY_ID.put(GearBalance.MOD_ID + ":" + path, gem);
				BY_SORT.put((i + 1) + "|" + sort.key(), gem);
			}
		}

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			ROUGH.values().forEach(output::accept);
			GEMS.keySet().forEach(output::accept);
		});

		// Rough gems from ores, only while sockets are switched on and not with silk touch
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!Sockets.active() || drops.isEmpty()) {
				return;
			}
			String type = table.unwrapKey().map(key -> ORES.get(key.identifier().toString())).orElse(null);
			if (type == null) {
				return;
			}
			for (ItemStack drop : drops) {
				// Silk touch drops the ore block itself
				if (BuiltInRegistries.ITEM.getKey(drop.getItem()).getPath().endsWith("_ore")) {
					return;
				}
			}
			if (context.getRandom().nextDouble() < GearConfig.ORE_DROP_CHANCE.getOrDefault(type, 0.0)) {
				drops.add(new ItemStack(ROUGH.get(type), count(GearConfig.oreDropMax, context.getRandom())));
			}
		});
	}

	/** How many gems one find gives: 1 to the most set in the settings. */
	public static int count(int max, RandomSource random) {
		return max <= 1 ? 1 : 1 + random.nextInt(max);
	}

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}

	/** Gem type ("ruby" ...) of a cut gem, null if the stack is none. */
	public static String type(ItemStack stack) {
		Object[] gem = stack.isEmpty() ? null : GEMS.get(stack.getItem());
		return gem == null ? null : (String) gem[0];
	}

	/** Cut level 1 chipped, 2 cut, 3 flawless, 0 if the stack is no cut gem. */
	public static int cut(ItemStack stack) {
		Object[] gem = stack.isEmpty() ? null : GEMS.get(stack.getItem());
		return gem == null ? 0 : (Integer) gem[1];
	}

	/** Item id of a cut gem, as stored in a socket. */
	public static String id(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	/** Uncut gem of a type (all but emerald), null for emerald (that is the vanilla emerald). */
	public static Item rough(String type) {
		return ROUGH.get(type);
	}

	/** True for the uncut gems of GearBalance. */
	public static boolean isRough(ItemStack stack) {
		return !stack.isEmpty() && ROUGH.containsValue(stack.getItem());
	}

	/** Cut gem with this item id, null if there is none. */
	public static Item byId(String id) {
		Item gem = BY_ID.get(id);
		// Titanite is no gem with a type, but comes back like one (gem lever)
		return gem == null ? Titanite.byId(id) : gem;
	}

	/** Gem type of a stored gem id, null if it is no cut gem. */
	public static String typeOf(String id) {
		Object[] gem = GEMS.get(BY_ID.get(id));
		return gem == null ? null : (String) gem[0];
	}

	/** Cut level of a stored gem id, 0 if it is no cut gem. */
	public static int cutOf(String id) {
		Object[] gem = GEMS.get(BY_ID.get(id));
		return gem == null ? 0 : (Integer) gem[1];
	}

	/** Sort of a stored gem id, null if it is no cut gem. */
	public static GemSorts.Sort sortOf(String id) {
		Object[] gem = GEMS.get(BY_ID.get(id));
		return gem == null ? null : (GemSorts.Sort) gem[2];
	}

	/** Sort of a cut gem, null if the stack is none. */
	public static GemSorts.Sort sort(ItemStack stack) {
		Object[] gem = stack.isEmpty() ? null : GEMS.get(stack.getItem());
		return gem == null ? null : (GemSorts.Sort) gem[2];
	}

	/** The gem of a sort in a cut (1 chipped, 2 cut, 3 flawless), null if there is none. */
	public static Item of(GemSorts.Sort sort, int cut) {
		return sort == null ? null : BY_SORT.get(cut + "|" + sort.key());
	}

	/** A gem of a random sort of the type in the cut (chest loot). */
	public static Item random(String type, int cut, RandomSource random) {
		List<GemSorts.Sort> sorts = GemSorts.of(type);
		return sorts.isEmpty() ? null : of(sorts.get(random.nextInt(sorts.size())), cut);
	}

	/** The gem of the same sort one cut lower, null for a chipped gem or an unknown id. */
	public static Item lowerCut(String id) {
		int cut = cutOf(id);
		return cut < 2 ? null : of(sortOf(id), cut - 1);
	}

	public static boolean isGem(ItemStack stack) {
		return cut(stack) > 0;
	}

	/** All cut gems, for recipes. */
	public static List<Item> all() {
		return List.copyOf(GEMS.keySet());
	}
}
