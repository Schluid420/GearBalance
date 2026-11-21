package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.SmoothScrolling;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every frame of a scroll area: one gliding step before it draws. */
@Mixin(AbstractWidget.class)
public abstract class WidgetFrameMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void gearbalance$scrollFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		if ((Object) this instanceof SmoothScrolling scrolling) {
			scrolling.gearbalance$scrollFrame();
		}
	}
}
