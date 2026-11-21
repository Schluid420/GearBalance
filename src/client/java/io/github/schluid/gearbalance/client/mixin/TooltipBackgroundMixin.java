package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.TooltipFrames;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tooltip frames: drawn 4 px further out than vanilla's; width rounded up to whole chain links
 * so the tiled chain never ends in a cut link.
 */
@Mixin(TooltipRenderUtil.class)
public class TooltipBackgroundMixin {
	/** Vanilla: 3 padding + 9 margin around the text. */
	private static final int OUT = 16;
	/** Left and right border of the frame sprites (15 + 11): the chain between them repeats every 10 px. */
	private static final int CHAIN_ENDS = 26;
	private static final int CHAIN_LINK = 10;

	@Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
	private static void gearbalance$frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
			Identifier style, CallbackInfo ci) {
		if (!TooltipFrames.isOwn(style)) {
			return;
		}
		Identifier background = Identifier.fromNamespaceAndPath(style.getNamespace(), "tooltip/" + style.getPath() + "_background");
		Identifier frame = Identifier.fromNamespaceAndPath(style.getNamespace(), "tooltip/" + style.getPath() + "_frame");
		int fullWidth = width + 2 * OUT;
		int extra = Math.floorMod(-(fullWidth - CHAIN_ENDS), CHAIN_LINK);
		int left = x - OUT - extra / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, left, y - OUT, fullWidth + extra, height + 2 * OUT);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, left, y - OUT, fullWidth + extra, height + 2 * OUT);
		ci.cancel();
	}
}
