package io.github.schluid.gearbalance.padded;

import io.github.schluid.gearbalance.GearBalance;
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

/** Sewing needle: left smithing slot, pads an armor piece. Wears out after a set number of uses like hammer and chisel. */
public final class SewingNeedle {
	private SewingNeedle() {
	}

	public static Item NEEDLE;

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "sewing_needle"));
		NEEDLE = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)
				.durability(GearConfig.defaultDurability("sewing_needle"))));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(NEEDLE));
	}

	public static boolean isNeedle(ItemStack stack) {
		return NEEDLE != null && !stack.isEmpty() && stack.getItem() == NEEDLE;
	}
}
