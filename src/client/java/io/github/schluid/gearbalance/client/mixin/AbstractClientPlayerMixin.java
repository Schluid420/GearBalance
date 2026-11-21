package io.github.schluid.gearbalance.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.GearBalance;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The mod's speed modifiers (armor weight, set bonus, gems) are left out of the field of view. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@WrapOperation(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double gearbalance$ignoreOwnSpeed(AbstractClientPlayer player, Holder<Attribute> attribute,
			Operation<Double> original) {
		double value = original.call(player, attribute);
		if (attribute != Attributes.MOVEMENT_SPEED) {
			return value;
		}
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return value;
		}
		// Both are ADD_MULTIPLIED_BASE modifiers: value = base * (1 + all of them)
		double all = 0;
		double own = 0;
		for (AttributeModifier modifier : speed.getModifiers()) {
			if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
				continue;
			}
			all += modifier.amount();
			if (modifier.id().getNamespace().equals(GearBalance.MOD_ID)) {
				own += modifier.amount();
			}
		}
		if (own == 0 || 1 + all <= 0) {
			return value;
		}
		return value * (1 + all - own) / (1 + all);
	}
}
