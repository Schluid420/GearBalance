package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.combat.Parry;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;

/** Shield tooltip (switch "New shields"): parry window, blocked damage and blocking angles; durability is the grey line of all items. */
final class ShieldTooltip {
	private ShieldTooltip() {
	}

	static void add(ItemStack stack, List<Component> lines) {
		if (!GearConfig.shieldsActive() || !Shields.isShield(stack)) {
			return;
		}
		BlocksAttacks blocks = stack.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks == null) {
			return;
		}
		if (GearConfig.parryActive()) {
			lines.add(line("gearbalance.shield.parry", Numbers.format(Parry.windowTicks(stack) / 20.0)));
		}
		List<BlocksAttacks.DamageReduction> reductions = blocks.damageReductions();
		if (!reductions.isEmpty()) {
			BlocksAttacks.DamageReduction normal = reductions.get(0);
			// What it blocks right now: a worn shield blocks less (option "Shield wear")
			float wear = stack.isDamageableItem() ? GearConfig.shieldBlockFactor(stack.getDamageValue(), stack.getMaxDamage()) : 1.0F;
			if (wear < 1.0F) {
				lines.add(line("gearbalance.shield.blocked_worn", Numbers.format(normal.factor() * wear * 100),
						Numbers.format(normal.factor() * 100)));
			} else {
				lines.add(line("gearbalance.shield.blocked", Numbers.format(normal.factor() * 100)));
			}
			float explosion = reductions.size() > 1 ? reductions.get(1).horizontalBlockingAngle() : normal.horizontalBlockingAngle();
			if (explosion != normal.horizontalBlockingAngle()) {
				lines.add(line("gearbalance.shield.angle_explosion", Numbers.format(normal.horizontalBlockingAngle()),
						Numbers.format(explosion)));
			} else {
				lines.add(line("gearbalance.shield.angle", Numbers.format(normal.horizontalBlockingAngle())));
			}
		}
	}

	private static Component line(String key, Object... args) {
		return Component.literal(" ").append(Component.translatable(key, args)).withStyle(ChatFormatting.BLUE);
	}
}
