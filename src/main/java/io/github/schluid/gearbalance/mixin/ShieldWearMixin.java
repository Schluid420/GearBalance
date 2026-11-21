package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shield wear: below the set durability, blocked damage falls evenly to the set minimum. */
@Mixin(LivingEntity.class)
public abstract class ShieldWearMixin {
	@ModifyExpressionValue(method = "applyItemBlocking",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"))
	private float gearbalance$wornShieldBlocksLess(float blocked) {
		ItemStack stack = ((LivingEntity) (Object) this).getItemBlockingWith();
		if (stack == null || !stack.isDamageableItem()) {
			return blocked;
		}
		return blocked * GearConfig.shieldBlockFactor(stack.getDamageValue(), stack.getMaxDamage());
	}
}
