package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.PickupDisplay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Picked up items: the sparkle of new items in the inventory, gone once the mouse was over the slot. */
@Mixin(AbstractContainerScreen.class)
public abstract class PickupSlotMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void gearbalance$markNew(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		PickupDisplay.slot(slot, slot == hoveredSlot);
	}

	@Inject(method = "extractSlot", at = @At("RETURN"))
	private void gearbalance$markNewEnd(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		PickupDisplay.sparkleSlot = false;
	}
}
