package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.BagPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inventory with a bag: draws bag slot and rows, moves the recipe book button, re-lays out on bag change. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(method = "getRecipeBookButtonPosition", at = @At("RETURN"), cancellable = true)
	private void gearbalance$moveBookButton(CallbackInfoReturnable<ScreenPosition> cir) {
		int shift = BagPanel.shift(BagPanel.layoutRows);
		if (shift != 0) {
			ScreenPosition position = cir.getReturnValue();
			cir.setReturnValue(new ScreenPosition(position.x(), position.y() + shift));
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void gearbalance$drawBag(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
		BagPanel.draw(graphics, screen.gearbalance$leftPos(), screen.gearbalance$topPos());
	}

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void gearbalance$followBag(CallbackInfo ci) {
		if (BagPanel.rows() != BagPanel.layoutRows) {
			InventoryScreen self = (InventoryScreen) (Object) this;
			self.resize(self.width, self.height);
		}
	}
}
