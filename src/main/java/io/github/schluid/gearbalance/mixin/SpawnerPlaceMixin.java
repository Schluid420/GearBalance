package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.SpawnerDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A spawner picked up with its mob spawns that mob again when placed. */
@Mixin(Block.class)
public abstract class SpawnerPlaceMixin {
	@Inject(method = "setPlacedBy", at = @At("TAIL"))
	private void gearbalance$spawnerMob(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
			ItemStack stack, CallbackInfo ci) {
		SpawnerDrops.placed(level, pos, stack);
	}
}
