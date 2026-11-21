package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Gems in structure chests, in three tiers like EnchantPlus book loot: easy = rough/chipped, medium = chipped/cut,
 * hard = cut/flawless. One gem per find (chance in config/gearbalance.json). Only with sockets on.
 */
public final class GemLoot {
	private GemLoot() {
	}

	/** Loot table id -> tier (easy, medium, hard). */
	private static final Map<String, String> TABLES = new HashMap<>();

	static {
		tier("easy", "simple_dungeon", "desert_pyramid", "jungle_temple", "abandoned_mineshaft",
				"shipwreck_treasure", "buried_treasure", "pillager_outpost", "underwater_ruin_big");
		tier("medium", "nether_bridge", "stronghold_corridor", "stronghold_crossing", "stronghold_library",
				"woodland_mansion", "trial_chambers/reward");
		tier("hard", "bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable",
				"end_city_treasure", "ancient_city", "ancient_city_ice_box", "trial_chambers/reward_ominous");
	}

	private static void tier(String tier, String... chests) {
		for (String chest : chests) {
			TABLES.put("minecraft:chests/" + chest, tier);
		}
	}

	public static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!Sockets.active()) {
				return;
			}
			String tier = table.unwrapKey().map(key -> TABLES.get(key.identifier().toString())).orElse(null);
			if (tier == null) {
				return;
			}
			RandomSource random = context.getRandom();
			if (random.nextDouble() < GearConfig.CHEST_CHANCE.getOrDefault(tier, 0.0)) {
				String[] picked = pick(tier, random);
				Item gem = picked[0].equals("rough") ? Gems.rough(picked[1])
						: Gems.random(picked[1], Gems.CUTS.indexOf(picked[0]) + 1, random);
				if (gem != null) {
					drops.add(new ItemStack(gem, Gems.count(GearConfig.CHEST_DROP_MAX.getOrDefault(picked[0], 1), random)));
				}
			}
		});
	}

	/** Stage and type of the gem for the tier (easy rough/chipped, medium chipped/cut, hard cut/flawless); no aquamarine or onyx. */
	private static String[] pick(String tier, RandomSource random) {
		boolean better = random.nextBoolean();
		String type = GearConfig.CHEST_TYPES.get(random.nextInt(GearConfig.CHEST_TYPES.size()));
		String cut = switch (tier) {
			case "easy" -> better ? "chipped" : "rough";
			case "medium" -> better ? "cut" : "chipped";
			default -> better ? "flawless" : "cut";
		};
		// There is no rough emerald: use a chipped one instead
		if (cut.equals("rough") && type.equals("emerald")) {
			cut = "chipped";
		}
		return new String[] {cut, type};
	}
}
