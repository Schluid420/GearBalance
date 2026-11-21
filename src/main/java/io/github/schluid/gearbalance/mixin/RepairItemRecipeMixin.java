package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Break protection: two pieces can't be put together in the crafting grid when one of them is damaged. */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
			at = @At("HEAD"), cancellable = true)
	private void gearbalance$noDamaged(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
		for (int i = 0; i < input.size(); i++) {
			if (BreakProtection.marked(input.getItem(i))) {
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
