package io.github.schluid.gearbalance.tool;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Durability of smithing tools (hammers, chisels, needle): uses are read live from GearConfig; each smithing step costs one use. */
public final class ToolDurability {
	private ToolDurability() {
	}

	/** Uses of the tool as set in the values, 0 if the stack is no smithing tool. */
	public static int max(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!GearBalance.MOD_ID.equals(id.getNamespace())) {
			return 0;
		}
		Integer uses = GearConfig.TOOL_DURABILITY.get(id.getPath());
		return uses == null ? 0 : uses;
	}

	/** Wears the tool in the slot by one use. Returns true if it broke and is gone. */
	public static boolean wear(Slot slot) {
		ItemStack tool = slot.getItem();
		if (max(tool) <= 0) {
			return false;
		}
		int damage = tool.getDamageValue() + 1;
		if (damage >= tool.getMaxDamage()) {
			slot.set(ItemStack.EMPTY);
			return true;
		}
		tool.setDamageValue(damage);
		slot.setChanged();
		return false;
	}
}
