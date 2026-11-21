package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.FoodStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HD food bar with optional golden saturation (FoodStyle). */
@Mixin(Hud.class)
public abstract class HudFoodMixin {
	@Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
	private void gearbalance$food(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
		if (FoodStyle.active()) {
			FoodStyle.extract(graphics, player, top, right);
			ci.cancel();
		}
	}
}
