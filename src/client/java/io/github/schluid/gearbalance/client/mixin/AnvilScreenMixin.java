package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Shows the real cost instead of "Too Expensive!" when the anvil limit is switched off. */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
	private int gearbalance$liftLimit(int limit) {
		return GearConfig.anvilNoLimit ? Integer.MAX_VALUE : limit;
	}
}
