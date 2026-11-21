package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Short sound once when a worn or held item drops to the warning limit (default 10 %); not on switching to it. */
final class DurabilityWarning {
	private DurabilityWarning() {
	}

	/** Item and "already below the limit" per slot, from the last tick. */
	private record State(Item item, boolean low) {
	}

	private static final Map<EquipmentSlot, State> LAST = new EnumMap<>(EquipmentSlot.class);

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DurabilityWarning::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			LAST.clear();
			return;
		}
		if (!GearConfig.enabled || !GearConfig.durabilityWarning || GearConfig.warnPercent <= 0) {
			LAST.clear();
			return;
		}
		double limit = GearConfig.warnPercent / 100.0;
		boolean warn = false;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = minecraft.player.getItemBySlot(slot);
			boolean low = false;
			if (!stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
				double left = (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage();
				low = left <= limit;
			}
			State last = LAST.get(slot);
			if (low && last != null && last.item() == stack.getItem() && !last.low()) {
				warn = true;
			}
			LAST.put(slot, new State(stack.isEmpty() ? null : stack.getItem(), low));
		}
		if (warn) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK.value(), 1.8F, 0.4F));
		}
	}
}
