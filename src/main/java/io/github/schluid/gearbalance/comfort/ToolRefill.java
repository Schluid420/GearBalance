package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Tool refill: a tool or weapon that breaks is replaced by the same item from the hotbar, then the inventory. */
public final class ToolRefill {
	private ToolRefill() {
	}

	public static void onBroken(ServerPlayer player, ItemStack broken, EquipmentSlot slot) {
		if (!GearConfig.toolRefillActive() || (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND)
				|| broken.isEmpty()) {
			return;
		}
		// The broken stack is taken out of the hand after this call: move the new one on the next tick
		player.level().getServer().execute(() -> refill(player, broken, slot));
	}

	private static void refill(ServerPlayer player, ItemStack broken, EquipmentSlot slot) {
		if (player.isRemoved() || !player.getItemBySlot(slot).isEmpty()) {
			return;
		}
		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
			if (slot == EquipmentSlot.MAINHAND && i == selected) {
				continue;
			}
			ItemStack candidate = inventory.getItem(i);
			if (!candidate.isEmpty() && candidate.is(broken.getItem())) {
				inventory.setItem(i, ItemStack.EMPTY);
				player.setItemSlot(slot, candidate);
				return;
			}
		}
	}
}
