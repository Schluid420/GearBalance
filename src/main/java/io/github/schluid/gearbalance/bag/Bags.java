package io.github.schluid.gearbalance.bag;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bags: worn in their own slot, add rows above the inventory. Made with the sewing needle at the smithing table,
 * upgraded tier by tier. On death the bag drops with its items.
 */
public final class Bags {
	private Bags() {
	}

	/** Most rows a bag can have (and slots the inventory gets for it). */
	public static final int MAX_ROWS = 7;
	/** Slots per row in storage: 9 plus 3 on each side. Fewer columns hide the outer ones; their items stay in the bag. */
	public static final int COLUMNS = GearConfig.MAX_BAG_COLUMNS;
	public static final int SLOTS = MAX_ROWS * COLUMNS;

	/** Columns in use (settings), always odd so they stay centered over the inventory. */
	public static int shownColumns() {
		return GearConfig.bagColumns(GearConfig.bagColumns);
	}

	/** First storage column in use. */
	public static int firstColumn() {
		return (COLUMNS - shownColumns()) / 2;
	}

	/** The storage column is in use. */
	public static boolean columnShown(int column) {
		return column >= firstColumn() && column < firstColumn() + shownColumns();
	}

	/** Tier -> item, in the order of the tiers (leather first). */
	public static final Map<String, Item> BAGS = new LinkedHashMap<>();

	/** The bag the player wears. */
	public static final AttachmentType<ItemStack> WORN = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "worn_bag"),
			builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath());

	public static void register() {
		for (String tier : GearConfig.BAG_TIERS) {
			String path = tier + "_bag";
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path));
			// The contents are not listed in the tooltip, only how full the bag is (BagTooltip)
			Item.Properties properties = new Item.Properties().setId(key).stacksTo(1)
					.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
					.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true));
			if (tier.equals("netherite") || tier.equals("titanium")) {
				properties.fireResistant();
			}
			BAGS.put(tier, Registry.register(BuiltInRegistries.ITEM, key, new BagItem(properties)));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Item bag : BAGS.values()) {
				output.accept(bag);
			}
		});
	}

	public static boolean active() {
		return GearConfig.bagsActive();
	}

	public static boolean isBag(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof BagItem;
	}

	/** Tier of a bag ("leather" ...), empty for anything else. */
	public static String tier(ItemStack stack) {
		if (isBag(stack)) {
			for (Map.Entry<String, Item> entry : BAGS.entrySet()) {
				if (stack.is(entry.getValue())) {
					return entry.getKey();
				}
			}
		}
		return "";
	}

	/** Rows of a bag as set in the settings, 0 for anything else. */
	public static int rows(ItemStack stack) {
		String tier = tier(stack);
		return tier.isEmpty() ? 0 : Math.max(1, Math.min(MAX_ROWS, GearConfig.BAG_ROWS.getOrDefault(tier, 1)));
	}

	/** Slots of the bag that hold something. */
	public static int usedSlots(ItemStack stack) {
		int used = 0;
		for (var ignored : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
			used++;
		}
		return used;
	}

	/** The worn bag (the stack itself, not a copy). */
	public static ItemStack worn(Player player) {
		return player.getAttachedOrElse(WORN, ItemStack.EMPTY);
	}

	public static void setWorn(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			player.removeAttached(WORN);
		} else {
			player.setAttached(WORN, stack);
		}
	}

	/** Rows shown in the inventory right now: those of the worn bag while bags are switched on. */
	public static int shownRows(Player player) {
		return active() ? rows(worn(player)) : 0;
	}

	/** Death without "keep inventory": the bag drops with everything in it. */
	public static void dropOnDeath(Player player, ServerLevel level) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		ItemStack bag = worn(player);
		if (!bag.isEmpty()) {
			setWorn(player, ItemStack.EMPTY);
			player.spawnAtLocation(level, bag);
		}
	}
}
