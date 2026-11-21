package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Break protection: a damaged mace gives no fall damage bonus. */
@Mixin(MaceItem.class)
public abstract class MaceItemMixin {
	@Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
	private void gearbalance$damagedMace(Entity target, float damage, DamageSource source, CallbackInfoReturnable<Float> cir) {
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && BreakProtection.wornOut(weapon)) {
			cir.setReturnValue(0.0F);
		}
	}
}
