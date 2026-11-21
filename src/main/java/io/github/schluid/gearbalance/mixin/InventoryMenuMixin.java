package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.bag.BagContainer;
import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.comfort.Trash;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inventory: after the vanilla slots (0-45) come the bag slot (46), the 54 bag slots (47 on) and the trash slot.
 * Shift-click on a bag fills the empty bag slot; everything else moves as in vanilla.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
	@Unique
	private static final int BAG_SLOT = 46;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void gearbalance$addBagSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		AbstractContainerMenuAccessor menu = (AbstractContainerMenuAccessor) this;
		menu.gearbalance$addSlot(new BagSlots.BagSlot(owner));
		BagContainer contents = new BagContainer(owner);
		for (int i = 0; i < Bags.SLOTS; i++) {
			menu.gearbalance$addSlot(new BagSlots.ContentSlot(owner, contents, i));
		}
		// Trash bin after the bag slots, so their numbers stay the same
		menu.gearbalance$addSlot(new Trash.TrashSlot(owner));
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void gearbalance$bagToSlot(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		InventoryMenu self = (InventoryMenu) (Object) this;
		if (index < InventoryMenu.INV_SLOT_START || index >= InventoryMenu.USE_ROW_SLOT_END || self.slots.size() <= BAG_SLOT) {
			return;
		}
		Slot from = self.slots.get(index);
		Slot bagSlot = self.slots.get(BAG_SLOT);
		ItemStack stack = from.getItem();
		if (!Bags.isBag(stack) || bagSlot.hasItem() || !bagSlot.mayPlace(stack)) {
			return;
		}
		ItemStack before = stack.copy();
		if (((AbstractContainerMenuAccessor) this).gearbalance$moveItemStackTo(stack, BAG_SLOT, BAG_SLOT + 1, false)) {
			from.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack, before);
			from.setChanged();
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}
}
