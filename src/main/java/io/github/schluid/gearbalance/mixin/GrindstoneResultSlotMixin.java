package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.List;
import io.github.schluid.gearbalance.grindstone.GrindstoneChoice;
import io.github.schluid.gearbalance.grindstone.GrindstoneOptions;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Grindstone result: with a choice there is no experience; removed gems may return one cut lower. */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	@Shadow
	@Final
	GrindstoneMenu this$0;

	@Inject(method = "getExperienceAmount", at = @At("HEAD"), cancellable = true)
	private void gearbalance$noExperience(Level level, CallbackInfoReturnable<Integer> cir) {
		if (((GrindstoneChoice) this$0).gearbalance$choice() != 0) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void gearbalance$returnGem(Player player, ItemStack taken, CallbackInfo ci) {
		if (player.level().isClientSide() || !GrindstoneOptions.gemReturnActive()) {
			return;
		}
		// The gems noted when the result was worked out; one roll per removed gem
		List<String> removed = ((GrindstoneChoice) this$0).gearbalance$removedGems();
		for (String gem : removed) {
			Item lower = GrindstoneOptions.lowerCut(gem);
			if (lower != null && player.level().getRandom().nextDouble() < GearConfig.grindstoneReturnChance) {
				player.getInventory().placeItemBackInInventory(new ItemStack(lower), Prediction.SERVER_ONLY);
			}
		}
	}
}
