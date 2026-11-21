package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.padded.Tailoring;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Padded horse armor: the horse takes only part of the fall damage. */
@Mixin(AbstractHorse.class)
public class HorseFallMixin {
	@ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true)
	private float gearbalance$paddedFall(float multiplier) {
		AbstractHorse self = (AbstractHorse) (Object) this;
		return Tailoring.horsePaddingActive(self.getItemBySlot(EquipmentSlot.BODY))
				? multiplier * Tailoring.HORSE_FALL_FACTOR : multiplier;
	}
}
