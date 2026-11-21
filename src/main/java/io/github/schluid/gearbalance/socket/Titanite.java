package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.List;
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
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Rarity;

/**
 * Titanite: very rare stone from titanium ore, cut like other gems. In a socket it has no bonus but strengthens every other
 * gem of the piece (default 8 / 16 / 25 %). One per piece. Only with titanium and sockets on.
 */
public final class Titanite {
	private Titanite() {
	}

	/** Rough stone from the ore. */
	public static final String ID = GearBalance.MOD_ID + ":titanite";
	/** Socket ids, index = cut - 1. */
	public static final List<String> CUT_IDS = List.of(GearBalance.MOD_ID + ":chipped_titanite",
			GearBalance.MOD_ID + ":cut_titanite", GearBalance.MOD_ID + ":flawless_titanite");

	public static Item ITEM;
	public static Item CHIPPED;
	public static Item CUT;
	public static Item FLAWLESS;

	public static void register() {
		ITEM = item(ID, Rarity.RARE);
		CHIPPED = item(CUT_IDS.get(0), Rarity.RARE);
		CUT = item(CUT_IDS.get(1), Rarity.EPIC);
		FLAWLESS = item(CUT_IDS.get(2), Rarity.EPIC);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(ITEM);
			output.accept(CHIPPED);
			output.accept(CUT);
			output.accept(FLAWLESS);
		});

		// Rough titanite from deepslate titanium ore, not with silk touch
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!active() || drops.isEmpty()) {
				return;
			}
			String id = table.unwrapKey().map(k -> k.identifier().toString()).orElse("");
			if (!id.equals(GearBalance.MOD_ID + ":blocks/deepslate_titanium_ore")) {
				return;
			}
			// The ore always drops itself (like ancient debris), so silk touch is read from the tool
			ItemInstance tool = context.getOptional(LootContextParams.TOOL);
			if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(
					context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
				return;
			}
			if (context.getRandom().nextDouble() * 100 < GearConfig.titaniteChance) {
				drops.add(new ItemStack(ITEM));
			}
		});
	}

	private static Item item(String id, Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
		return Registry.register(BuiltInRegistries.ITEM, key,
				new Item(new Item.Properties().setId(key).rarity(rarity).fireResistant()));
	}

	public static boolean active() {
		return GearConfig.titaniumActive() && Sockets.active();
	}

	/** Cut of a titanite item or socket id: 1 chipped, 2 cut, 3 flawless, 0 = none (also the rough stone). */
	public static int cutOf(String id) {
		return CUT_IDS.indexOf(id) + 1;
	}

	public static int cutOf(ItemStack stack) {
		return stack.isEmpty() ? 0 : cutOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	/** A titanite that fits a socket (chipped, cut or flawless). */
	public static boolean is(ItemStack stack) {
		return cutOf(stack) > 0;
	}

	/** Item of a socket id, null if it is no titanite. */
	public static Item byId(String id) {
		return switch (cutOf(id)) {
			case 1 -> CHIPPED;
			case 2 -> CUT;
			case 3 -> FLAWLESS;
			default -> null;
		};
	}

	/** Cut of the titanite in the sockets of the piece, 0 = none. */
	public static int inPiece(ItemStack piece) {
		for (String id : Sockets.sockets(piece)) {
			int cut = cutOf(id);
			if (cut > 0) {
				return cut;
			}
		}
		return 0;
	}

	public static boolean in(ItemStack piece) {
		return inPiece(piece) > 0;
	}

	/** Factor for the other gems of the piece, e.g. 1.25 with flawless titanite; 1 without. */
	public static double factor(ItemStack piece) {
		return 1 + GearConfig.titaniteBoost(inPiece(piece)) / 100.0;
	}
}
