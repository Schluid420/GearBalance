package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.padded.Tailoring;
import io.github.schluid.gearbalance.upgrade.CompressedItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Hint lines (option "Tooltip hints") for thread and the compressed materials, like the gem hints. */
final class MaterialHints {
	private MaterialHints() {
	}

	static void add(ItemStack stack, List<Component> lines) {
		if (!GearConfig.enabled || !GearConfig.showHints || stack.isEmpty()) {
			return;
		}
		if (Tailoring.isThread(stack)) {
			// Thread only has a recipe while one of its uses is on
			boolean patch = GearConfig.leatherPatchActive();
			boolean padding = GearConfig.horsePaddingActive();
			if (patch || padding) {
				lines.add(gray("gearbalance.thread.tip"));
			}
			if (patch) {
				lines.add(gray("gearbalance.thread.patch.tip"));
			}
			if (padding) {
				lines.add(gray("gearbalance.thread.padding.tip"));
			}
			return;
		}
		if (CompressedItems.PHANTOM_MEMBRANE != null && stack.is(CompressedItems.PHANTOM_MEMBRANE)) {
			if (GearConfig.breakProtectionActive()) {
				lines.add(gray("gearbalance.compressed.phantom.tip"));
				lines.add(gray("gearbalance.compressed.split.tip"));
			}
			return;
		}
		if (CompressedItems.BY_MATERIAL.containsValue(stack.getItem())) {
			boolean upgrades = GearConfig.upgradesActive();
			boolean restore = GearConfig.breakProtectionActive();
			if (upgrades) {
				lines.add(gray("gearbalance.compressed.upgrade.tip"));
			}
			if (restore) {
				lines.add(gray("gearbalance.compressed.restore.tip"));
			}
			// The recipes to make and split them exist while upgrades or break protection are on
			if (upgrades || restore) {
				lines.add(gray("gearbalance.compressed.split.tip"));
			}
		}
	}

	private static Component gray(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.GRAY);
	}
}
