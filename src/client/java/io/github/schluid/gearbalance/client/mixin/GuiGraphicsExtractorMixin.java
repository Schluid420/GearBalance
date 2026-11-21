package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.ComfortClient;
import io.github.schluid.gearbalance.client.PickupDisplay;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Slot decorations: damaged pieces grayed out, set numbers top left, sparkle on new items top right. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
			at = @At("HEAD"))
	private void gearbalance$grayDamaged(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
		if (BreakProtection.wornOut(stack)) {
			((GuiGraphicsExtractor) (Object) this).fill(x, y, x + 16, y + 16, 0xB0404040);
		}
	}

	/** Gear sets: small numbers of the sets that hold the piece, top left in the slot. */
	@Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
			at = @At("TAIL"))
	private void gearbalance$setNumbers(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
		GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
		if (PickupDisplay.sparkleSlot) {
			PickupDisplay.drawSparkle(graphics, x, y);
		}
		String numbers = ComfortClient.setNumbers(stack);
		if (numbers.isEmpty()) {
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 1, y + 1);
		graphics.pose().scale(0.5F, 0.5F);
		graphics.text(font, numbers, 0, 0, 0xFFFFFF55, true);
		graphics.pose().popMatrix();
	}
}
