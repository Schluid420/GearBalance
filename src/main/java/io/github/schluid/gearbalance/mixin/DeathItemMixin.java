package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.DeathRules;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Items dropped on death (with the switch on) ignore explosions. */
@Mixin(ItemEntity.class)
public abstract class DeathItemMixin {
	@Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
	private void gearbalance$explosionProof(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
		if (((ItemEntity) (Object) this).entityTags().contains(DeathRules.DEATH_TAG)) {
			cir.setReturnValue(true);
		}
	}
}
