package io.github.schluid.gearbalance.bag;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Items of the worn bag. They live in the bag item itself; a working copy is written back on every change. */
public final class BagContainer implements Container {
	private final Player player;
	private final NonNullList<ItemStack> items = NonNullList.withSize(Bags.SLOTS, ItemStack.EMPTY);
	private ItemStack loadedBag = ItemStack.EMPTY;
	private ItemContainerContents loadedContents = null;

	public BagContainer(Player player) {
		this.player = player;
	}

	/** The working copy, read again when the bag or its contents changed elsewhere. */
	private NonNullList<ItemStack> items() {
		ItemStack bag = Bags.worn(player);
		ItemContainerContents contents = bag.isEmpty() ? null : bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		if (bag != loadedBag || contents != loadedContents) {
			for (int i = 0; i < items.size(); i++) {
				items.set(i, ItemStack.EMPTY);
			}
			if (contents != null) {
				contents.copyInto(items);
			}
			loadedBag = bag;
			loadedContents = contents;
		}
		return items;
	}

	/** Writes the working copy back into the bag. */
	private void save() {
		items();
		if (loadedBag.isEmpty()) {
			return;
		}
		List<ItemStack> copies = new ArrayList<>(items.size());
		for (ItemStack stack : items) {
			copies.add(stack.copy());
		}
		ItemContainerContents contents = ItemContainerContents.fromItems(copies);
		loadedBag.set(DataComponents.CONTAINER, contents);
		loadedContents = contents;
	}

	@Override
	public int getContainerSize() {
		return Bags.SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items()) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items().get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(items(), slot, amount);
		if (!taken.isEmpty()) {
			save();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack taken = ContainerHelper.takeItem(items(), slot);
		save();
		return taken;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items().set(slot, stack);
		save();
	}

	@Override
	public void setChanged() {
		save();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < Bags.SLOTS; i++) {
			items().set(i, ItemStack.EMPTY);
		}
		save();
	}
}
