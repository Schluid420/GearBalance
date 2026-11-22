package io.github.schluid.gearbalance.bag;

import io.github.schluid.gearbalance.GearBalance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/** Inventory slots for bags: the bag slot and up to 7 rows of 9 to 15 above the inventory; only the worn bag's rows work. */
public final class BagSlots {
	private BagSlots() {
	}

	/** Position of the bag slot (next to the player, at the top, opposite the offhand). */
	public static final int BAG_X = 77, BAG_Y = 8;
	/** Empty bag slot: outline of a bag. */
	public static final Identifier EMPTY_ICON = Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "container/slot/bag");
	/** Set by the client: the slots are hidden (creative inventory, which can't place them). */
	public static BooleanSupplier hidden = () -> false;
	/** Set by the client: the bag was closed with a left click on it, its rows are hidden while it stays worn. */
	public static BooleanSupplier closed = () -> false;

	/** Left of the item in a bag column: 3 columns stick out on each side of the inventory. */
	public static int columnX(int column) {
		return 8 + (column - 3) * 18;
	}

	/** Top of the item in a bag row (0 = right above the inventory). */
	public static int rowY(int row) {
		return -23 - row * 18;
	}

	public static final class BagSlot extends Slot {
		private final Player player;

		public BagSlot(Player player) {
			super(new BagSlotContainer(player), 0, BAG_X, BAG_Y);
			this.player = player;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return Bags.active() && Bags.isBag(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		/** Shown while bags are on; a bag still in it stays reachable after switching them off. */
		@Override
		public boolean isActive() {
			return !hidden.getAsBoolean() && (Bags.active() || !Bags.worn(player).isEmpty());
		}

		@Override
		public Identifier getNoItemIcon() {
			return EMPTY_ICON;
		}
	}

	public static final class ContentSlot extends Slot {
		private final Player player;

		public ContentSlot(Player player, BagContainer container, int index) {
			super(container, index, columnX(index % Bags.COLUMNS), rowY(index / Bags.COLUMNS));
			this.player = player;
		}

		private boolean open() {
			int index = getContainerSlot();
			return index < Bags.shownRows(player) * Bags.COLUMNS && Bags.columnShown(index % Bags.COLUMNS);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return open() && !Bags.isBag(stack) && stack.getItem().canFitInsideContainerItems();
		}

		@Override
		public boolean isActive() {
			return !hidden.getAsBoolean() && !closed.getAsBoolean() && open();
		}
	}
}
