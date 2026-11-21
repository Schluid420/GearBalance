package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.BagPanel;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A click on the bag panel counts as inside the inventory, so the held item isn't thrown out. */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	@Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
	private void gearbalance$bagPanelIsInside(double mouseX, double mouseY, int left, int top,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof InventoryScreen && BagPanel.inside(left, top, mouseX, mouseY)) {
			cir.setReturnValue(false);
		}
	}
}
