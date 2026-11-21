package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.padded.Tailoring;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Padded wolf armor: the wolf takes only part of the fall damage. Horses have their own (HorseFallMixin). */
@Mixin(LivingEntity.class)
public class WolfFallMixin {
	@ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true)
	private float gearbalance$paddedWolfFall(float multiplier) {
		if ((Object) this instanceof Wolf wolf && Tailoring.horsePaddingActive(wolf.getItemBySlot(EquipmentSlot.BODY))) {
			return multiplier * Tailoring.HORSE_FALL_FACTOR;
		}
		return multiplier;
	}
}
