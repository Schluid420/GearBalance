package io.github.schluid.gearbalance.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person: lower shield (also while blocking) and gentle idle hand movement.
 * Applied per hand right after vanilla saves its pose, so nothing carries over to the other hand.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	/** How far down 1 % of the setting moves the shield (30 % = 0.25). */
	@Unique
	private static final float SHIELD_DROP_PER_PERCENT = 0.25F / 30.0F;
	/** Size of the idle movement. */
	@Unique
	private static final float IDLE_MOVE = 0.012F;
	@Unique
	private static final float IDLE_TURN = 0.8F;
	/** Above this speed (blocks per tick) the idle movement is gone. */
	@Unique
	private static final double IDLE_SPEED = 0.08;

	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void gearbalance$handPose(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partialTick,
			float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poseStack,
			SubmitNodeCollector collector, int light, CallbackInfo ci) {
		LocalPlayer local = Minecraft.getInstance().player;
		if (local == null || !GearConfig.enabled) {
			return;
		}
		boolean blocking = local.isUsingItem() && local.getUsedItemHand() == hand;
		// Also while blocking, so the shield stays at the same height
		if (GearConfig.shieldLower && Shields.isShield(stack)) {
			poseStack.translate(0.0F, -SHIELD_DROP_PER_PERCENT * GearConfig.shieldLowerPercent, 0.0F);
		}
		if (GearConfig.handIdle && !blocking) {
			double speed = local.getDeltaMovement().horizontalDistance();
			// Strength from the settings: 100 % = normal
			float still = (float) Math.max(0.0, 1.0 - speed / IDLE_SPEED) * GearConfig.handIdlePercent / 100.0F;
			if (still > 0.0F) {
				// Slow breathing: up and down, a little to the side, the off hand a bit later
				float time = local.tickCount + partialTick + (hand == InteractionHand.OFF_HAND ? 20.0F : 0.0F);
				float up = (float) Math.sin(time * 0.07) * IDLE_MOVE * still;
				float side = (float) Math.cos(time * 0.045) * IDLE_MOVE * 0.5F * still;
				poseStack.translate(side, up, 0.0F);
				poseStack.rotateDegrees(Axis.ZP, (float) Math.sin(time * 0.045) * IDLE_TURN * still);
			}
		}
	}
}
