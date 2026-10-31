package io.github.schluid.gearbalance.loot;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Optional changes to the chests of the endgame areas: end cities and end ships, bastions, ancient cities
 * and the trial chamber vaults. Checked whenever a chest or vault is opened, so the switches apply at once.
 * The supply chests of trial chambers are left alone, their arrows and food are meant for the fight.
 *
 * Enchanted books and plain books are never added, removed or replaced here, so a mod that controls
 * where books come from (EnchantPlus) keeps full control over them, whichever mod runs first.
 */
public final class EndgameLoot {
	private EndgameLoot() {
	}

	private enum Area {
		END, BASTION, ANCIENT_CITY, TRIAL, TRIAL_OMINOUS
	}

	/** An item added with a chance and an amount between min and max. */
	private record Extra(String item, float chance, int min, int max) {
	}

	private static final Map<ResourceKey<LootTable>, Area> TABLES = Map.ofEntries(
			Map.entry(BuiltInLootTables.END_CITY_TREASURE, Area.END),
			Map.entry(BuiltInLootTables.BASTION_TREASURE, Area.BASTION),
			Map.entry(BuiltInLootTables.BASTION_OTHER, Area.BASTION),
			Map.entry(BuiltInLootTables.BASTION_BRIDGE, Area.BASTION),
			Map.entry(BuiltInLootTables.BASTION_HOGLIN_STABLE, Area.BASTION),
			Map.entry(BuiltInLootTables.ANCIENT_CITY, Area.ANCIENT_CITY),
			Map.entry(BuiltInLootTables.ANCIENT_CITY_ICE_BOX, Area.ANCIENT_CITY),
			// Only the tables the vaults use, the common/rare/unique tables are part of them
			Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD, Area.TRIAL),
			Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, Area.TRIAL_OMINOUS));

	private static final Map<Area, List<Extra>> TEMPLATES = Map.of(
			Area.END, List.of(new Extra("spire_armor_trim_smithing_template", 0.20f, 1, 1)),
			Area.BASTION, List.of(
					new Extra("netherite_upgrade_smithing_template", 0.15f, 1, 1),
					new Extra("snout_armor_trim_smithing_template", 0.10f, 1, 1)),
			Area.ANCIENT_CITY, List.of(
					new Extra("ward_armor_trim_smithing_template", 0.15f, 1, 1),
					new Extra("silence_armor_trim_smithing_template", 0.05f, 1, 1)),
			Area.TRIAL, List.of(new Extra("bolt_armor_trim_smithing_template", 0.10f, 1, 1)),
			Area.TRIAL_OMINOUS, List.of(new Extra("flow_armor_trim_smithing_template", 0.10f, 1, 1)));

	/** Valuable loot per area, picked by weight. Used for replaced filler and for extra rolls. */
	private record Valuable(String item, int weight, int min, int max) {
	}

	private static final Map<Area, List<Valuable>> VALUABLES = Map.of(
			// Piglins love gold, so bastions are about gold and netherite
			Area.BASTION, List.of(
					new Valuable("gold_ingot", 35, 2, 5),
					new Valuable("golden_carrot", 15, 2, 4),
					new Valuable("golden_apple", 12, 1, 1),
					new Valuable("gold_block", 10, 1, 1),
					new Valuable("netherite_scrap", 10, 1, 1),
					new Valuable("ancient_debris", 8, 1, 1),
					new Valuable("gilded_blackstone", 6, 1, 2),
					new Valuable("enchanted_golden_apple", 1, 1, 1),
					new Valuable("music_disc_pigstep", 1, 1, 1)),
			// The end is the last stop: gold, gems, shulker shells, potions for the fight and a touch of netherite
			Area.END, List.of(
					new Valuable("gold_ingot", 25, 2, 5),
					new Valuable("emerald", 20, 2, 4),
					new Valuable("shulker_shell", 15, 1, 2),
					new Valuable("diamond", 15, 1, 2),
					new Valuable("ender_pearl", 12, 2, 4),
					new Valuable("potion:long_slow_falling", 8, 1, 1),
					new Valuable("potion:strong_healing", 6, 1, 1),
					new Valuable("potion:long_regeneration", 5, 1, 1),
					new Valuable("potion:long_strength", 5, 1, 1),
					new Valuable("golden_apple", 8, 1, 1),
					new Valuable("experience_bottle", 4, 1, 3),
					new Valuable("netherite_scrap", 6, 1, 1)),
			// Deep dark: echo shards, amethyst and experience
			Area.ANCIENT_CITY, List.of(
					new Valuable("echo_shard", 20, 1, 2),
					new Valuable("amethyst_shard", 18, 3, 6),
					new Valuable("experience_bottle", 18, 3, 6),
					new Valuable("lapis_lazuli", 15, 4, 8),
					new Valuable("diamond", 10, 1, 2),
					new Valuable("golden_apple", 10, 1, 1),
					new Valuable("disc_fragment_5", 6, 1, 1),
					new Valuable("enchanted_golden_apple", 1, 1, 1)),
			Area.TRIAL, List.of(
					new Valuable("emerald", 25, 2, 4),
					new Valuable("gold_ingot", 20, 2, 4),
					new Valuable("golden_carrot", 15, 2, 4),
					new Valuable("experience_bottle", 15, 2, 4),
					new Valuable("diamond", 10, 1, 1),
					new Valuable("golden_apple", 8, 1, 1)),
			Area.TRIAL_OMINOUS, List.of(
					new Valuable("emerald", 20, 3, 6),
					new Valuable("gold_ingot", 18, 3, 5),
					new Valuable("diamond", 15, 1, 2),
					new Valuable("experience_bottle", 15, 3, 6),
					new Valuable("golden_apple", 12, 1, 1),
					new Valuable("netherite_scrap", 6, 1, 1),
					new Valuable("enchanted_golden_apple", 2, 1, 1)));

	private static final String POTION = "potion:";

	/** Filler that is easy to get anywhere else. */
	private static final Set<String> CHEAP = Set.of(
			"rotten_flesh", "bone", "string", "gunpowder", "coal", "stick", "feather", "leather",
			"iron_nugget", "gold_nugget", "arrow", "tipped_arrow", "spectral_arrow",
			"wheat", "wheat_seeds", "beetroot_seeds", "pumpkin_seeds", "melon_seeds",
			"bread", "baked_potato", "cooked_porkchop", "cooked_cod", "glow_berries", "sweet_berries",
			"candle", "sculk", "obsidian", "crying_obsidian", "magma_cream", "chain", "iron_chain",
			"honey_bottle", "packed_ice", "snowball");

	/** End cities are full of iron, which is not much of a reward that late in the game. */
	private static final Set<String> CHEAP_IN_END = Set.of("iron_ingot", "iron_block");

	public static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!GearConfig.lootTemplates && !GearConfig.lootMaterials && !GearConfig.lootNoCheap) {
				return;
			}
			Area area = table.unwrapKey().map(TABLES::get).orElse(null);
			if (area == null) {
				return;
			}
			RandomSource random = context.getRandom();
			List<Valuable> valuables = VALUABLES.get(area);
			if (GearConfig.lootNoCheap) {
				// Every cheap stack is swapped for something valuable, so the chest doesn't end up empty
				for (int i = 0; i < drops.size(); i++) {
					if (isCheap(drops.get(i), area)) {
						drops.set(i, roll(valuables, random));
					}
				}
			}
			if (GearConfig.lootTemplates) {
				add(drops, TEMPLATES.get(area), random);
			}
			if (GearConfig.lootMaterials) {
				// One or two extra valuable stacks
				drops.add(roll(valuables, random));
				if (random.nextFloat() < 0.5f) {
					drops.add(roll(valuables, random));
				}
			}
		});
	}

	private static boolean isCheap(ItemStack stack, Area area) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return CHEAP.contains(path) || (area == Area.END && CHEAP_IN_END.contains(path));
	}

	private static ItemStack roll(List<Valuable> valuables, RandomSource random) {
		int total = 0;
		for (Valuable valuable : valuables) {
			total += valuable.weight();
		}
		int pick = random.nextInt(total);
		for (Valuable valuable : valuables) {
			pick -= valuable.weight();
			if (pick < 0) {
				return stack(valuable.item(), valuable.min() + random.nextInt(valuable.max() - valuable.min() + 1));
			}
		}
		return ItemStack.EMPTY;
	}

	/** "potion:<name>" makes one potion of that kind, anything else is an item id. */
	private static ItemStack stack(String id, int count) {
		if (id.startsWith(POTION)) {
			ItemStack potion = BuiltInRegistries.POTION.get(Identifier.withDefaultNamespace(id.substring(POTION.length())))
					.map(holder -> PotionContents.createItemStack(Items.POTION, holder))
					.orElse(ItemStack.EMPTY);
			// Potions don't stack, several of them come from several rolls
			return potion;
		}
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
		return new ItemStack(item, count);
	}

	private static void add(List<ItemStack> drops, List<Extra> extras, RandomSource random) {
		if (extras == null) {
			return;
		}
		for (Extra extra : extras) {
			if (random.nextFloat() < extra.chance()) {
				drops.add(stack(extra.item(), extra.min() + random.nextInt(extra.max() - extra.min() + 1)));
			}
		}
	}
}
