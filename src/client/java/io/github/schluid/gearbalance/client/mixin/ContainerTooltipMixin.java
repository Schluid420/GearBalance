package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.TooltipFrames;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Tooltip frames: the item tooltip of a slot gets the frame of its material, unless the item brings its own style. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerTooltipMixin {
	@Shadow
	protected Slot hoveredSlot;

	@ModifyArg(method = "extractTooltip", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"),
			index = 5)
	private Identifier gearbalance$frameStyle(Identifier style) {
		if (style != null || !TooltipFrames.active() || hoveredSlot == null || !hoveredSlot.hasItem()) {
			TooltipFrames.current = null;
			return style;
		}
		TooltipFrames.current = hoveredSlot.getItem().copy();
		return TooltipFrames.styleFor(hoveredSlot.getItem());
	}
}
