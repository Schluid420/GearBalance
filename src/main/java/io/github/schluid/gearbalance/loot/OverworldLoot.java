package io.github.schluid.gearbalance.loot;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Items;

/**
 * Optional: no diamonds or (enchanted) golden apples in easy overworld chests (mineshafts, temples, dungeons, villages ...);
 * they stay in harder places, the Nether, the End and the deep dark. Archaeology and mob drops are unchanged.
 */
public final class OverworldLoot {
	private OverworldLoot() {
	}

	/** Chest loot tables of the overworld, without "minecraft:chests/". Village chests start with "village/". */
	private static final Set<String> CHESTS = Set.of(
			"abandoned_mineshaft", "desert_pyramid", "jungle_temple", "simple_dungeon", "stronghold_corridor",
			"stronghold_crossing", "stronghold_library", "buried_treasure", "shipwreck_treasure", "shipwreck_supply",
			"shipwreck_map", "ruined_portal", "woodland_mansion", "pillager_outpost", "igloo_chest",
			"underwater_ruin_big", "underwater_ruin_small", "spawn_bonus_chest", "jungle_temple_dispenser");

	public static void register() {
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!GearConfig.enabled || !GearConfig.lootOverworld) {
				return;
			}
			String id = table.unwrapKey().map(key -> key.identifier().toString()).orElse("");
			if (!id.startsWith("minecraft:chests/")) {
				return;
			}
			String name = id.substring("minecraft:chests/".length());
			if (!CHESTS.contains(name) && !name.startsWith("village/")) {
				return;
			}
			drops.removeIf(stack -> stack.is(Items.DIAMOND) || stack.is(Items.GOLDEN_APPLE)
					|| stack.is(Items.ENCHANTED_GOLDEN_APPLE));
		});
	}
}
