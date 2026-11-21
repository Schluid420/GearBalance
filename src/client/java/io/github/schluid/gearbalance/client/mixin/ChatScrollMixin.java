package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.SmoothScroll;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The chat scrolls line by line over a few frames instead of jumping (SmoothScroll). */
@Mixin(ChatComponent.class)
public abstract class ChatScrollMixin {
	@Inject(method = "scrollChat", at = @At("HEAD"), cancellable = true)
	private void gearbalance$smoothChat(int lines, CallbackInfo ci) {
		if (SmoothScroll.lists() && !SmoothScroll.chatOwnStep) {
			SmoothScroll.chatScrolled(lines);
			ci.cancel();
		}
	}

	@Inject(method = "resetChatScroll", at = @At("HEAD"))
	private void gearbalance$resetChat(CallbackInfo ci) {
		SmoothScroll.chatReset();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
			at = @At("HEAD"))
	private void gearbalance$chatFrame(GuiGraphicsExtractor graphics, Font font, int a, int b, int c, @Coerce Object mode,
			boolean d, CallbackInfo ci) {
		if (SmoothScroll.lists()) {
			SmoothScroll.chatFrame((ChatComponent) (Object) this);
		}
	}
}
