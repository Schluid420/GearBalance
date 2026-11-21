package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
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

/** Socket chisels in four tiers; they wear out after the uses in GearConfig and cut a socket in the left smithing slot. Same materials as the hammer of the tier. */
public final class SocketChisels {
	private SocketChisels() {
	}

	/** Chisel -> tier (1 iron, 2 gold, 3 diamond, 4 netherite, 5 titanium). */
	private static final Map<Item, Integer> TIERS = new LinkedHashMap<>();

	public static Item IRON;
	public static Item GOLD;
	public static Item DIAMOND;
	public static Item NETHERITE;
	public static Item TITANIUM;

	public static void register() {
		IRON = chisel("iron_socket_chisel", 1, false);
		GOLD = chisel("golden_socket_chisel", 2, false);
		DIAMOND = chisel("diamond_socket_chisel", 3, false);
		NETHERITE = chisel("netherite_socket_chisel", 4, true);
		TITANIUM = chisel("titanium_socket_chisel", 5, true);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Item item : TIERS.keySet()) {
				output.accept(item);
			}
		});
	}

	private static Item chisel(String path, int tier, boolean fireResistant) {
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

	/** Tier of the chisel, 0 if the stack is no socket chisel. */
	public static int tier(ItemStack stack) {
		return stack.isEmpty() ? 0 : TIERS.getOrDefault(stack.getItem(), 0);
	}

	public static boolean isChisel(ItemStack stack) {
		return tier(stack) > 0;
	}

	/** All chisels that are good enough for the material (same rule as the forge hammers). */
	public static List<Item> chiselsFor(String material) {
		List<Item> result = new ArrayList<>();
		int needed = ForgeHammers.needed(material);
		TIERS.forEach((item, tier) -> {
			if (needed > 0 && tier >= needed) {
				result.add(item);
			}
		});
		return result;
	}
}
