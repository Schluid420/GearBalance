package io.github.schluid.gearbalance.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.client.HotbarFx;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar: pop of the selected item, low durability light, bouncing item name, smaller items with HD hotbar. */
@Mixin(Hud.class)
public abstract class HudHotbarMixin {
	@WrapOperation(method = "extractItemHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"))
	private void gearbalance$slot(Hud hud, GuiGraphicsExtractor graphics, int x, int y, DeltaTracker delta, Player player,
			ItemStack stack, int seed, Operation<Void> original) {
		// Hotbar slots sit at the middle - 90 + 2 + 20 * slot; the offhand slots are left out
		int offset = x - (graphics.guiWidth() / 2 - 88);
		int slot = offset >= 0 && offset % 20 == 0 ? offset / 20 : -1;
		if (slot > 8) {
			slot = -1;
		}
		HotbarFx.lowDurabilityGlow(graphics, x, y, stack);
		float scale = slot < 0 ? 1.0F : HotbarFx.slotScale(slot, delta.getGameTimeDeltaPartialTick(false));
		// HD hotbar: items 25 % smaller so they sit inside the slot frame
		if (GearConfig.hdHud) {
			scale *= 0.75F;
		}
		if (scale == 1.0F) {
			original.call(hud, graphics, x, y, delta, player, stack, seed);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 8, y + 8);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-x - 8, -y - 8);
		original.call(hud, graphics, x, y, delta, player, stack, seed);
		graphics.pose().popMatrix();
	}

	@Inject(method = "extractSelectedItemName", at = @At("HEAD"))
	private void gearbalance$nameStart(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		float scale = HotbarFx.nameScale(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		graphics.pose().pushMatrix();
		// With more heart rows the armor bar moves up; the name goes up with it
		graphics.pose().translate(0, -HotbarFx.heartRowsShift());
		if (scale != 1.0F) {
			float cx = graphics.guiWidth() / 2.0F;
			float cy = graphics.guiHeight() - 55;
			graphics.pose().translate(cx, cy);
			graphics.pose().scale(scale, scale);
			graphics.pose().translate(-cx, -cy);
		}
	}

	@Inject(method = "extractSelectedItemName", at = @At("RETURN"))
	private void gearbalance$nameEnd(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		graphics.pose().popMatrix();
	}
}
