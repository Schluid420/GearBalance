package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.HeartStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** New heart pictures and the silver ring for hearts from gear (HeartStyle). */
@Mixin(Hud.class)
public abstract class HudHeartMixin {
	@Inject(method = "extractHearts", at = @At("HEAD"))
	private void gearbalance$heartsStart(GuiGraphicsExtractor graphics, Player player, int x, int y, int rowHeight,
			int regeneration, float maxHealth, int health, int displayHealth, int absorption, boolean blinking,
			CallbackInfo ci) {
		if (HeartStyle.active()) {
			HeartStyle.start(player, maxHealth, absorption);
		}
	}

	@Inject(method = "extractHeart", at = @At("HEAD"), cancellable = true)
	private void gearbalance$heart(GuiGraphicsExtractor graphics, @Coerce Object type, int x, int y, boolean hardcore,
			boolean blinking, boolean half, CallbackInfo ci) {
		if (HeartStyle.active()) {
			HeartStyle.heart(graphics, ((Enum<?>) type).name(), x, y, hardcore, blinking, half);
			ci.cancel();
		}
	}
}
