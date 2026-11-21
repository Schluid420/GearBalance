package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.Trash;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clicks on the trash bin slot of the inventory: delete or bring back, nothing else. */
@Mixin(AbstractContainerMenu.class)
public abstract class TrashClickMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void gearbalance$trashBin(int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
		if (Trash.clicked((AbstractContainerMenu) (Object) this, slotId, button, input, player)) {
			ci.cancel();
		}
	}
}
