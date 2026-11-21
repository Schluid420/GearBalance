package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.ArmorBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The colored armor bar replaces the gray vanilla symbols (same place, same size). */
@Mixin(Hud.class)
public abstract class HudArmorMixin {
	@Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
	private static void gearbalance$armorBar(GuiGraphicsExtractor graphics, Player player, int healthTop, int healthRows,
			int rowHeight, int left, CallbackInfo ci) {
		if (!ArmorBar.active()) {
			return;
		}
		// Same place as vanilla: above the hearts, one row per heart row
		int top = healthTop - (healthRows - 1) * rowHeight - 10;
		ArmorBar.extract(graphics, player, top, left);
		ci.cancel();
	}
}
