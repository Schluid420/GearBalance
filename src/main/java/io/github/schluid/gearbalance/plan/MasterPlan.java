package io.github.schluid.gearbalance.plan;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Master smithing plan: with its switch on, the diamond forge hammer and socket chisel (and so the netherite ones) need it.
 * Found in hard chests, dropped by wither skeletons and piglin brutes (chances in GearConfig); 7 diamonds + iron block copy it.
 */
public final class MasterPlan {
	private MasterPlan() {
	}

	public static Item PLAN;

	/** Chests of hard places (same as the hard gem tier). */
	private static final Set<String> CHESTS = Set.of("bastion_treasure", "bastion_other", "bastion_bridge",
			"bastion_hoglin_stable", "end_city_treasure", "ancient_city", "ancient_city_ice_box",
			"trial_chambers/reward_ominous");
	private static final Set<String> MOBS = Set.of("wither_skeleton", "piglin_brute");

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "master_smithing_plan"));
		PLAN = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).rarity(Rarity.UNCOMMON)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(PLAN));

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!active()) {
				return;
			}
			String id = table.unwrapKey().map(k -> k.identifier().toString()).orElse("");
			double chance;
			if (id.startsWith("minecraft:chests/") && CHESTS.contains(id.substring("minecraft:chests/".length()))) {
				chance = GearConfig.planChestChance;
			} else if (id.startsWith("minecraft:entities/") && MOBS.contains(id.substring("minecraft:entities/".length()))) {
				chance = GearConfig.planMobChance;
			} else {
				return;
			}
			if (context.getRandom().nextDouble() < chance) {
				drops.add(new ItemStack(PLAN));
			}
		});
	}

	/** The plan is needed (its switch): then it drops and all endgame tools need it. */
	public static boolean active() {
		return GearConfig.masterPlanActive();
	}
}
