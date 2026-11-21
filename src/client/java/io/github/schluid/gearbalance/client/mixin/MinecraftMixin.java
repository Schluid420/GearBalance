package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.InventoryScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Window resized or GUI size changed: remember the normal size and keep the inventory size. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@ModifyArg(method = "resizeGui",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(I)V"))
	private int gearbalance$inventoryScale(int scale) {
		InventoryScale.normal = scale;
		return InventoryScale.scaleFor(((Minecraft) (Object) this).gui.screen(), scale);
	}
}
