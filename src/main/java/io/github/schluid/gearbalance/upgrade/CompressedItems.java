package io.github.schluid.gearbalance.upgrade;

import io.github.schluid.gearbalance.GearBalance;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Compressed upgrade materials: 8 ingots, leather or chains in a ring (diamond and netherite scraps with a blaze rod in the middle);
 * one splits back into 8. Items always exist; the "Upgrades" switch controls recipes and upgrading.
 */
public final class CompressedItems {
	private CompressedItems() {
	}

	/** Material key (as in GearBalance) to the compressed item. */
	public static final Map<String, Item> BY_MATERIAL = new LinkedHashMap<>();

	public static Item LEATHER;
	public static Item COPPER;
	public static Item CHAINMAIL;
	public static Item IRON;
	public static Item GOLD;
	public static Item DIAMOND;
	public static Item NETHERITE;
	public static Item TITANIUM;
	/** Compressed phantom membrane: repairs a damaged elytra (break protection). Not an upgrade material. */
	public static Item PHANTOM_MEMBRANE;

	public static void register() {
		LEATHER = item("leather", "compressed_leather");
		COPPER = item("copper", "compressed_copper_ingot");
		CHAINMAIL = item("chainmail", "compressed_chain");
		IRON = item("iron", "compressed_iron_ingot");
		GOLD = item("gold", "compressed_gold_ingot");
		DIAMOND = item("diamond", "compressed_diamond");
		NETHERITE = item("netherite", "compressed_netherite_ingot");
		TITANIUM = item("titanium", "compressed_titanium_ingot");
		ResourceKey<Item> phantom = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "compressed_phantom_membrane"));
		PHANTOM_MEMBRANE = Registry.register(BuiltInRegistries.ITEM, phantom, new Item(new Item.Properties().setId(phantom)));

		// Creative inventory, ingredients tab: right after the netherite ingot
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.NETHERITE_INGOT, Stream.concat(BY_MATERIAL.values().stream(),
						Stream.of(PHANTOM_MEMBRANE)).toArray(Item[]::new)));
	}

	private static Item item(String material, String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
		BY_MATERIAL.put(material, item);
		return item;
	}
}
