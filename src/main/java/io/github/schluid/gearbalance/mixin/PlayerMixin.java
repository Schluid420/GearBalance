package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.level.LevelRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Locked tools mine like the bare hand: hand speed and no drops from blocks that need a tool.
 * Applied on both sides, so the client's mining animation matches the server.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@WrapOperation(method = "getDestroySpeed",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
	private float gearbalance$handSpeedWhenLocked(ItemStack stack, BlockState state, Operation<Float> original) {
		if (LevelRules.isLocked((Player) (Object) this, stack)) {
			return 1.0F;
		}
		return original.call(stack, state);
	}

	@WrapOperation(method = "hasCorrectToolForDrops",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean gearbalance$noToolDropsWhenLocked(ItemStack stack, BlockState state, Operation<Boolean> original) {
		if (LevelRules.isLocked((Player) (Object) this, stack)) {
			return false;
		}
		return original.call(stack, state);
	}
}
