package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.upgrade.UpgradeValues;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Upgraded bows, crossbows and tridents: non-attribute bonuses (arrow damage, draw speed, thrown damage). */
final class UpgradeTooltip {
	private UpgradeTooltip() {
	}

	static void add(ItemStack stack, List<Component> lines) {
		if (Upgrades.level(stack) == 0) {
			return;
		}
		if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
			percent(lines, "gearbalance.upgrade.arrow_damage", Upgrades.factor(stack, UpgradeValues.Value.ARROW_DAMAGE));
			percent(lines, "gearbalance.upgrade.draw_speed", Upgrades.factor(stack, UpgradeValues.Value.DRAW_SPEED));
		} else if (stack.is(Items.TRIDENT)) {
			percent(lines, "gearbalance.upgrade.thrown_damage", Upgrades.factor(stack, UpgradeValues.Value.DAMAGE));
		}
	}

	private static void percent(List<Component> lines, String key, double factor) {
		if (factor != 1) {
			lines.add(Component.literal(" ").append(Component.translatable(key, "+" + Numbers.format((factor - 1) * 100)))
					.withStyle(ChatFormatting.BLUE));
		}
	}
}
