package io.github.schluid.gearbalance.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position of a container window on the screen. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int gearbalance$leftPos();

	@Accessor("topPos")
	int gearbalance$topPos();
}
