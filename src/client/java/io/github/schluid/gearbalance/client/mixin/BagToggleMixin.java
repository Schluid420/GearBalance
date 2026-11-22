package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.client.BagPanel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bags: left click with an empty hand opens/closes the rows instead of taking the bag; right click takes it off. */
@Mixin(MultiPlayerGameMode.class)
public abstract class BagToggleMixin {
	@Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
	private void gearbalance$toggleBag(int containerId, int slotId, int button, ContainerInput input, Player player,
			CallbackInfo ci) {
		AbstractContainerMenu menu = player.containerMenu;
		if (button != 0 || input != ContainerInput.PICKUP || menu.containerId != containerId
				|| !menu.getCarried().isEmpty() || slotId < 0 || slotId >= menu.slots.size()) {
			return;
		}
		Slot slot = menu.getSlot(slotId);
		if (slot instanceof BagSlots.BagSlot && Bags.isBag(slot.getItem())) {
			BagPanel.closed = !BagPanel.closed;
			ci.cancel();
		}
	}
}
