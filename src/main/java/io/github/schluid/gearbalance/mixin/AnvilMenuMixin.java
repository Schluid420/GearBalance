package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.stats.MaterialStat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional replacement for "Too Expensive!": the cost is capped at anvilMaxCost instead.
 * Vanilla clears the result at a cost of 40 or more, so that check is lifted as well.
 *
 * Also sets how much one repair item restores. Vanilla uses getMaxDamage() / 4 (25 %) twice in
 * createResult, so the max damage seen there is scaled to match the percentage of the material.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

	@Inject(method = "createResult", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/DataSlot;get()I", ordinal = 1))
	private void gearbalance$capCost(CallbackInfo ci) {
		if (GearConfig.anvilNoLimit && this.cost.get() > GearConfig.anvilMaxCost) {
			this.cost.set(GearConfig.anvilMaxCost);
		}
	}

	@WrapOperation(method = "createResult", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getMaxDamage()I", ordinal = 0))
	private int gearbalance$repairFirst(ItemStack stack, Operation<Integer> original) {
		return repairBase(stack, original.call(stack));
	}

	@WrapOperation(method = "createResult", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getMaxDamage()I", ordinal = 1))
	private int gearbalance$repairNext(ItemStack stack, Operation<Integer> original) {
		return repairBase(stack, original.call(stack));
	}

	/** Value that gives the wanted percentage after vanilla divides it by 4, at least 1 point. */
	private static int repairBase(ItemStack stack, int maxDamage) {
		double percent = GearConfig.forItem(MaterialStat.REPAIR, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (percent == MaterialStat.REPAIR.vanilla) {
			return maxDamage;
		}
		return Math.max(4, (int) Math.round(maxDamage * percent * 4 / 100.0));
	}

	// The third 40 in createResult is the "too expensive" check
	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
	private int gearbalance$liftLimit(int limit) {
		return GearConfig.anvilNoLimit ? Integer.MAX_VALUE : limit;
	}
}
