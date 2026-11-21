package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Break protection: a damaged piece's enchantments do nothing and Mending skips it. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
			at = @At("HEAD"), cancellable = true)
	private static void gearbalance$damagedItem(ItemStack stack, EnchantmentHelper.EnchantmentVisitor visitor, CallbackInfo ci) {
		if (BreakProtection.wornOut(stack)) {
			ci.cancel();
		}
	}

	@Inject(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
			at = @At("HEAD"), cancellable = true)
	private static void gearbalance$damagedWorn(ItemStack stack, EquipmentSlot slot, LivingEntity entity,
			EnchantmentHelper.EnchantmentInSlotVisitor visitor, CallbackInfo ci) {
		if (BreakProtection.wornOut(stack)) {
			ci.cancel();
		}
	}

	/** Mending (and other "random worn item with ..." effects) skip damaged pieces. */
	@WrapOperation(method = "getRandomItemWith", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object gearbalance$skipDamaged(ItemStack stack, DataComponentType<?> type, Object fallback,
			Operation<Object> original) {
		if (BreakProtection.wornOut(stack)) {
			return ItemEnchantments.EMPTY;
		}
		return original.call(stack, type, fallback);
	}
}
