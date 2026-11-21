package io.github.schluid.gearbalance.smith;

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
import net.minecraft.world.item.Rarity;

/**
 * Endgame tools from the master smithing plan: the netherite repair kit (RepairKitItem) and the diamond, netherite
 * and titanium gem levers (take a gem out without losing it). Uses are set in the settings.
 */
public final class SmithingTools {
	private SmithingTools() {
	}

	public static Item REPAIR_KIT;
	public static Item DIAMOND_LEVER;
	public static Item NETHERITE_LEVER;
	public static Item TITANIUM_LEVER;

	public static void register() {
		REPAIR_KIT = tool("netherite_repair_kit", true, RepairKitItem::new);
		DIAMOND_LEVER = tool("diamond_gem_lever", false, Item::new);
		NETHERITE_LEVER = tool("netherite_gem_lever", true, Item::new);
		TITANIUM_LEVER = tool("titanium_gem_lever", true, Item::new);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(REPAIR_KIT);
			output.accept(DIAMOND_LEVER);
			output.accept(NETHERITE_LEVER);
			output.accept(TITANIUM_LEVER);
		});
	}

	private static Item tool(String path, boolean netherite, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path));
		Item.Properties properties = new Item.Properties().setId(key).rarity(Rarity.RARE)
				.durability(GearConfig.defaultDurability(path));
		if (netherite) {
			properties.fireResistant();
		}
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
	}

	public static boolean isRepairKit(ItemStack stack) {
		return !stack.isEmpty() && stack.is(REPAIR_KIT);
	}

	public static boolean isLever(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(DIAMOND_LEVER) || stack.is(NETHERITE_LEVER) || stack.is(TITANIUM_LEVER));
	}
}
