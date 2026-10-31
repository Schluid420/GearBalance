package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.level.LevelRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Locked tools don't wear out, they are used like the bare hand. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
	private void gearbalance$noWearWhenLocked(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
		ItemStack stack = (ItemStack) (Object) this;
		if (LevelRules.isLocked(player, stack)) {
			LevelRules.showLockedHint(player, stack);
			ci.cancel();
		}
	}
}
