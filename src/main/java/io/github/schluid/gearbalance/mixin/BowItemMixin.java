package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.schluid.gearbalance.upgrade.UpgradeValues;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Upgraded bows draw faster: the time the bow was drawn counts for more. */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
	@ModifyArg(method = "releaseUsing", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private int gearbalance$drawSpeed(int ticks, @Local(argsOnly = true) ItemStack stack) {
		double factor = Upgrades.factor(stack, UpgradeValues.Value.DRAW_SPEED);
		return factor == 1 ? ticks : (int) Math.round(ticks * factor);
	}
}
