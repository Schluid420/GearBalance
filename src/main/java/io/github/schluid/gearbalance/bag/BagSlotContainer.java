package io.github.schluid.gearbalance.bag;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The single bag slot next to the player; the bag itself is kept on the player. */
public final class BagSlotContainer implements Container {
	private final Player player;

	public BagSlotContainer(Player player) {
		this.player = player;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return Bags.worn(player).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? Bags.worn(player) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack bag = Bags.worn(player);
		if (slot != 0 || amount <= 0 || bag.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = bag.split(amount);
		if (bag.isEmpty()) {
			Bags.setWorn(player, ItemStack.EMPTY);
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack bag = Bags.worn(player);
		Bags.setWorn(player, ItemStack.EMPTY);
		return bag;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			Bags.setWorn(player, stack);
		}
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		Bags.setWorn(player, ItemStack.EMPTY);
	}
}
