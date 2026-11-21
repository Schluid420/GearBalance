package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.schluid.gearbalance.combat.Parry;
import io.github.schluid.gearbalance.comfort.ToolRefill;
import io.github.schluid.gearbalance.titanium.Titanium;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.Sockets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gem resistances of worn armor (poison, fire, frost; 100 % = immune) and the parry
 * (perfectly timed block plus the stronger hit after it).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"), cancellable = true)
	private void gearbalance$blockPoison(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (effect.is(MobEffects.POISON) && Sockets.resistance((LivingEntity) (Object) this, GemBonuses.Special.POISON) >= 1) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"), argsOnly = true)
	private MobEffectInstance gearbalance$shortenPoison(MobEffectInstance effect) {
		if (!effect.is(MobEffects.POISON)) {
			return effect;
		}
		double resistance = Sockets.resistance((LivingEntity) (Object) this, GemBonuses.Special.POISON);
		if (resistance <= 0 || resistance >= 1 || effect.isInfiniteDuration()) {
			return effect;
		}
		int duration = Math.max(1, (int) Math.round(effect.getDuration() * (1 - resistance)));
		return new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(),
				effect.isVisible(), effect.showIcon());
	}

	@Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
	private void gearbalance$frostResistance(CallbackInfoReturnable<Boolean> cir) {
		if (Sockets.resistance((LivingEntity) (Object) this, GemBonuses.Special.FROST) >= 1) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void gearbalance$blockDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (gearbalance$resistance(source) >= 1) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float gearbalance$lowerDamage(float amount, ServerLevel level, DamageSource source) {
		double resistance = gearbalance$resistance(source);
		float result = resistance > 0 && resistance < 1 ? (float) (amount * (1 - resistance)) : amount;
		// A hit right after a parry deals more damage
		return Parry.damage(source, result, level);
	}

	/** A successful block may be a parry. */
	@Inject(method = "applyItemBlocking", at = @At("RETURN"))
	private void gearbalance$parry(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		Parry.onBlocked((LivingEntity) (Object) this, level, source, cir.getReturnValueF());
	}

	/** Fire or frost resistance against this damage, 0 for any other damage. */
	@Unique
	private double gearbalance$resistance(DamageSource source) {
		GemBonuses.Special special = source.is(DamageTypeTags.IS_FIRE) ? GemBonuses.Special.FIRE
				: source.is(DamageTypeTags.IS_FREEZING) ? GemBonuses.Special.FROST : GemBonuses.Special.NONE;
		return special == GemBonuses.Special.NONE ? 0 : Sockets.resistance((LivingEntity) (Object) this, special);
	}

	/** Tool refill: a tool or weapon that broke in the hand is replaced from the inventory. */
	@Inject(method = "onEquippedItemBroken", at = @At("TAIL"))
	private void gearbalance$refill(ItemStack broken, EquipmentSlot slot, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			ToolRefill.onBroken(player, broken, slot);
		}
	}

	/** Titanium armor: every worn piece lowers the fall damage. */
	@ModifyReturnValue(method = "calculateFallDamage", at = @At("RETURN"))
	private int gearbalance$titaniumFall(int damage) {
		return Titanium.fallDamage((LivingEntity) (Object) this, damage);
	}
}
