package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.schluid.gearbalance.upgrade.UpgradeValues;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Upgraded crossbows load faster. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
	@ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
	private static int gearbalance$chargeSpeed(int ticks, @Local(argsOnly = true) ItemStack stack) {
		double factor = Upgrades.factor(stack, UpgradeValues.Value.DRAW_SPEED);
		return factor == 1 ? ticks : Math.max(1, (int) Math.round(ticks / factor));
	}
}
