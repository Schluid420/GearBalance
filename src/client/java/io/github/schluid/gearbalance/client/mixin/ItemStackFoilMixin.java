package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.HandEffectProperty;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** "Hide enchantment glint": no glint on pieces with full sockets while the glow is on (client only). */
@Mixin(ItemStack.class)
public abstract class ItemStackFoilMixin {
	@Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
	private void gearbalance$hideGlint(CallbackInfoReturnable<Boolean> cir) {
		if (GearConfig.hideGlint && HandEffectProperty.glowing((ItemStack) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
