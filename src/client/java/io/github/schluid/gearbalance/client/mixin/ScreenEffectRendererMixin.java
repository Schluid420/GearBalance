package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Lower fire: the flames on screen while burning are moved down, so they hide less of the view. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	/** One fire quad is 1 unit high; at 100 % it would be moved fully below its vanilla place. */
	private static final float DROP_PER_PERCENT = 0.01F;

	@ModifyConstant(method = "lambda$submitFire$0", constant = @Constant(floatValue = -0.3F))
	private static float gearbalance$lowerFire(float y) {
		if (GearConfig.enabled && GearConfig.fireLower) {
			return y - DROP_PER_PERCENT * GearConfig.fireLowerPercent;
		}
		return y;
	}
}
