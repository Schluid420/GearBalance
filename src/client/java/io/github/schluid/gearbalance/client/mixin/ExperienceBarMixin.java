package io.github.schluid.gearbalance.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.schluid.gearbalance.client.HotbarFx;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.resources.Identifier;
import io.github.schluid.gearbalance.client.HudStyle;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animated experience bar: smooth fill, light on gain, sparks on a level up; HD sprites (HudStyle). */
@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@ModifyExpressionValue(method = "extractBackground", at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/player/LocalPlayer;experienceProgress:F"))
	private float gearbalance$smoothFill(float real, GuiGraphicsExtractor graphics, DeltaTracker delta) {
		return HotbarFx.experience(real, delta.getGameTimeDeltaPartialTick(false));
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void gearbalance$effects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		ContextualBar bar = (ContextualBar) (Object) this;
		HotbarFx.experienceEffects(graphics, bar.left(minecraft.getWindow()), bar.top(minecraft.getWindow()),
				delta.getGameTimeDeltaPartialTick(false));
	}

	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 1)
	private Identifier gearbalance$hdBackground(Identifier sprite) {
		return HudStyle.swap(sprite);
	}

	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"),
			index = 1)
	private Identifier gearbalance$hdProgress(Identifier sprite) {
		return HudStyle.swap(sprite);
	}
}
