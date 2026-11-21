package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Trash bin: a real slot after the bag slots. The last item stays and can be taken back; a new one deletes it. */
public final class Trash {
	private Trash() {
	}

	/** Top left corner of the bin slot in the inventory window: left of the stats button. */
	public static final int SLOT_X = 144, SLOT_Y = 1;

	/** The item in the bin, per player. Kept on leaving and on death. */
	public static final AttachmentType<ItemStack> ITEM = AttachmentRegistry.create(GearBalance.id("trash_item"),
			builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath());

	/** Called at start so the attachment is registered. */
	public static void register() {
	}

	/** The bin slot. Its item lives on the player, so it stays when the inventory is closed. */
	public static final class TrashSlot extends Slot {
		private final Player owner;

		public TrashSlot(Player owner) {
			super(new SimpleContainer(1), 0, SLOT_X, SLOT_Y);
			this.owner = owner;
		}

		@Override
		public ItemStack getItem() {
			return owner.getAttachedOrElse(ITEM, ItemStack.EMPTY);
		}

		@Override
		public void set(ItemStack stack) {
			if (stack.isEmpty()) {
				owner.removeAttached(ITEM);
			} else {
				owner.setAttached(ITEM, stack);
			}
			setChanged();
		}

		@Override
		public ItemStack remove(int amount) {
			ItemStack item = getItem();
			ItemStack taken = item.split(amount);
			set(item);
			return taken;
		}

		@Override
		public int getMaxStackSize() {
			return 99;
		}

		/** Only while the bin is on, and not in the creative inventory. */
		@Override
		public boolean isActive() {
			return GearConfig.trashActive() && !BagSlots.hidden.getAsBoolean();
		}
	}

	/**
	 * Slot click (client and server); true if it was the bin. With an item: put in (old one deleted); empty hand: take out.
	 * Shift-click as usual; dragging, swapping and throwing do nothing.
	 */
	public static boolean clicked(AbstractContainerMenu menu, int slotId, int button, ContainerInput input,
			Player player) {
		if (slotId < 0 || slotId >= menu.slots.size() || !(menu.slots.get(slotId) instanceof TrashSlot slot)) {
			return false;
		}
		if (input == ContainerInput.QUICK_MOVE) {
			return !GearConfig.trashActive();
		}
		if (input == ContainerInput.PICKUP && GearConfig.trashActive() && !player.isSpectator()) {
			ItemStack carried = menu.getCarried();
			if (!carried.isEmpty()) {
				slot.set(carried.copy());
				menu.setCarried(ItemStack.EMPTY);
			} else if (slot.hasItem()) {
				menu.setCarried(slot.getItem().copy());
				slot.set(ItemStack.EMPTY);
			}
		}
		return true;
	}
}
