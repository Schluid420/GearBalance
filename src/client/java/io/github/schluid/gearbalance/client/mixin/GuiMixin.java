package io.github.schluid.gearbalance.client.mixin;

import com.mojang.blaze3d.platform.Window;
import io.github.schluid.gearbalance.client.InventoryScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opening a screen: the inventory gets its smaller GUI size, every other screen the normal one. */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "setScreen", at = @At("HEAD"))
	private void gearbalance$inventoryScale(@Nullable Screen screen, CallbackInfo ci) {
		if (InventoryScale.normal <= 0) {
			return;
		}
		Window window = Minecraft.getInstance().getWindow();
		int wanted = InventoryScale.scaleFor(screen, InventoryScale.normal);
		if (window.getGuiScale() != wanted) {
			window.setGuiScale(wanted);
		}
	}
}
