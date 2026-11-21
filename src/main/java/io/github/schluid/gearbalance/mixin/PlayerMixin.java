package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.comfort.DeathRules;
import io.github.schluid.gearbalance.level.LevelRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Locked and worn out tools mine like the bare hand (both sides, so the animation matches).
 * Death: the worn bag drops with its items; the death rules wrap around the drops.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@WrapOperation(method = "getDestroySpeed",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
	private float gearbalance$handSpeedWhenLocked(ItemStack stack, BlockState state, Operation<Float> original) {
		if (LevelRules.isLocked((Player) (Object) this, stack) || BreakProtection.wornOut(stack)) {
			return 1.0F;
		}
		return original.call(stack, state);
	}

	@WrapOperation(method = "hasCorrectToolForDrops",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean gearbalance$noToolDropsWhenLocked(ItemStack stack, BlockState state, Operation<Boolean> original) {
		if (LevelRules.isLocked((Player) (Object) this, stack) || BreakProtection.wornOut(stack)) {
			return false;
		}
		return original.call(stack, state);
	}

	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void gearbalance$beforeDeathDrops(ServerLevel level, CallbackInfo ci) {
		DeathRules.beforeDrops((Player) (Object) this, level);
	}

	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void gearbalance$dropBag(ServerLevel level, CallbackInfo ci) {
		Bags.dropOnDeath((Player) (Object) this, level);
		DeathRules.afterDrops((Player) (Object) this, level);
	}

	@ModifyReturnValue(method = "getBaseExperienceReward", at = @At("RETURN"))
	private int gearbalance$deathExperience(int vanilla) {
		return DeathRules.experience((Player) (Object) this, vanilla);
	}
}
