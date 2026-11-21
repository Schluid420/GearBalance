package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.client.BagPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bags: left click with an empty hand opens/closes the rows; right click takes it off as usual. */
@Mixin(AbstractContainerScreen.class)
public abstract class BagToggleMixin {
	@Shadow
	private Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	@Shadow
	public abstract AbstractContainerMenu getMenu();

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void gearbalance$toggleBag(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() != 0 || !getMenu().getCarried().isEmpty()) {
			return;
		}
		Slot slot = getHoveredSlot(event.x(), event.y());
		if (slot instanceof BagSlots.BagSlot && Bags.isBag(slot.getItem())) {
			BagPanel.closed = !BagPanel.closed;
			cir.setReturnValue(true);
		}
	}
}
