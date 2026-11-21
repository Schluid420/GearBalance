package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.DeathRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Items added while a player's death drops happen go onto the death pile. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void gearbalance$deathPile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof ItemEntity item) {
			DeathRules.onItemAdded(item);
		}
	}
}
