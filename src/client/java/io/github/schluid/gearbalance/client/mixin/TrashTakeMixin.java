package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.comfort.Trash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Trash bin slot: take the item back with an empty hand (sent on mouse release, the press
 * doesn't reach the slot) and draw it at 56 % to fit the small button.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class TrashTakeMixin {
	@Shadow
	private Slot getHoveredSlot(double x, double y) {
		throw new AssertionError();
	}

	/** Size of the item in the bin. */
	@Unique
	private static final float BIN_ITEM_SCALE = 0.5625F;

	@Unique
	private boolean gearbalance$binScaled;

	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void gearbalance$smallBinItem(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
			CallbackInfo ci) {
		gearbalance$binScaled = slot instanceof Trash.TrashSlot;
		if (gearbalance$binScaled) {
			// Around the middle of the bin button (it sits 1 right and 2 down in the slot, 13 pixels wide)
			float centerX = slot.x + 7.5F;
			float centerY = slot.y + 8.5F;
			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY);
			graphics.pose().scale(BIN_ITEM_SCALE, BIN_ITEM_SCALE);
			graphics.pose().translate(-slot.x - 8, -slot.y - 8);
		}
	}

	@Inject(method = "extractSlot", at = @At("RETURN"))
	private void gearbalance$smallBinItemEnd(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
			CallbackInfo ci) {
		if (gearbalance$binScaled) {
			gearbalance$binScaled = false;
			graphics.pose().popMatrix();
		}
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void gearbalance$takeFromBin(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof InventoryScreen screen) || event.button() > 1) {
			return;
		}
		AbstractContainerMenu menu = screen.getMenu();
		Slot slot = getHoveredSlot(event.x(), event.y());
		Minecraft minecraft = Minecraft.getInstance();
		if (!(slot instanceof Trash.TrashSlot) || !menu.getCarried().isEmpty() || !slot.hasItem()
				|| minecraft.gameMode == null || minecraft.player == null) {
			return;
		}
		minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, event.button(), ContainerInput.PICKUP,
				minecraft.player);
		cir.setReturnValue(true);
	}
}
