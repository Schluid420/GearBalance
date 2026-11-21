package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.BagPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inventory with a bag: the window moves down by half the bag panel, before its buttons are placed. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected int topPos;

	@Inject(method = "init", at = @At("TAIL"))
	private void gearbalance$makeRoomForBag(CallbackInfo ci) {
		if ((Object) this instanceof InventoryScreen) {
			int rows = BagPanel.rows();
			BagPanel.layoutRows = rows;
			topPos += BagPanel.shift(rows);
		}
	}
}
