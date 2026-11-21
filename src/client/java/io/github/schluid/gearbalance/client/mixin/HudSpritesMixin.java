package io.github.schluid.gearbalance.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.client.HudStyle;
import io.github.schluid.gearbalance.client.SmoothScroll;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** HD hotbar, selection frame, off hand slot and level number (HudStyle); gliding selection frame (SmoothScroll). */
@Mixin(Hud.class)
public abstract class HudSpritesMixin {
	@ModifyArg(method = "extractItemHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 1)
	private Identifier gearbalance$hdHotbar(Identifier sprite) {
		return HudStyle.swap(sprite);
	}

	@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
	private void gearbalance$hdLevel(GuiGraphicsExtractor graphics, Font font, int level, Operation<Void> original) {
		if (HudStyle.active()) {
			HudStyle.level(graphics, level);
			return;
		}
		original.call(graphics, font, level);
	}

	/** The selection frame glides to the new slot (SmoothScroll). */
	@WrapOperation(method = "extractItemHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void gearbalance$glidingFrame(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
			int width, int height, Operation<Void> original) {
		if (SmoothScroll.hotbar() && sprite.getPath().endsWith("hotbar_selection")) {
			x = SmoothScroll.frameX(x);
		}
		original.call(graphics, pipeline, sprite, x, y, width, height);
	}
}
