package io.github.schluid.gearbalance.hammer;

import io.github.schluid.gearbalance.GearBalance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Forge hammers in four tiers; they wear out after the uses set in GearConfig and go into the left smithing slot (upgrades, gems).
 * A hammer works for its own material and below: iron = leather, copper, chainmail, iron; then gold, diamond, netherite; titanium all.
 */
public final class ForgeHammers {
	private ForgeHammers() {
	}

	/** Hammer -> tier (1 iron, 2 gold, 3 diamond, 4 netherite, 5 titanium). */
	private static final Map<Item, Integer> TIERS = new LinkedHashMap<>();
	/** Material key (as in GearBalance) -> tier of hammer it needs. */
	private static final Map<String, Integer> NEEDED = Map.ofEntries(
			Map.entry("leather", 1), Map.entry("copper", 1), Map.entry("chainmail", 1), Map.entry("iron", 1),
			Map.entry("gold", 2), Map.entry("diamond", 3), Map.entry("netherite", 4), Map.entry("titanium", 5),
			// Weapons without a material tier: only the diamond and the netherite hammer
			Map.entry("mace", 3), Map.entry("trident", 3), Map.entry("bow", 3), Map.entry("crossbow", 3));

	public static Item IRON;
	public static Item GOLD;
	public static Item DIAMOND;
	public static Item NETHERITE;
	public static Item TITANIUM;

	public static void register() {
		IRON = hammer("iron_forge_hammer", 1, false);
		GOLD = hammer("golden_forge_hammer", 2, false);
		DIAMOND = hammer("diamond_forge_hammer", 3, false);
		NETHERITE = hammer("netherite_forge_hammer", 4, true);
		TITANIUM = hammer("titanium_forge_hammer", 5, true);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Item item : TIERS.keySet()) {
				output.accept(item);
			}
		});
	}

	private static Item hammer(String path, int tier, boolean fireResistant) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path));
		Item.Properties properties = new Item.Properties().setId(key)
				.durability(GearConfig.defaultDurability(path));
		if (fireResistant) {
			properties.fireResistant();
		}
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
		TIERS.put(item, tier);
		return item;
	}

	/** Tier of the hammer, 0 if the stack is no forge hammer. */
	public static int tier(ItemStack stack) {
		return stack.isEmpty() ? 0 : TIERS.getOrDefault(stack.getItem(), 0);
	}

	public static boolean isHammer(ItemStack stack) {
		return tier(stack) > 0;
	}

	/** Tier a material needs, 0 for materials that can't be worked on. */
	public static int needed(String material) {
		return NEEDED.getOrDefault(material, 0);
	}

	/** All hammers that are good enough for the material. */
	public static List<Item> hammersFor(String material) {
		List<Item> result = new ArrayList<>();
		int needed = needed(material);
		TIERS.forEach((item, tier) -> {
			if (needed > 0 && tier >= needed) {
				result.add(item);
			}
		});
		return result;
	}
}
