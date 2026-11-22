package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.combat.Parry;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Item tooltip: with Shift, what this item gives more or less than the one worn or held in the same place
 * (upgrades, gems, resistances, durability, sockets, enchantments). Without Shift a grey hint line.
 */
final class CompareTooltip {
	private CompareTooltip() {
	}

	static void add(ItemStack stack, List<Component> lines) {
		Minecraft minecraft = Minecraft.getInstance();
		// The creative search builds its index on another thread, where keys can't be read: no comparison there
		if (!minecraft.isSameThread()) {
			return;
		}
		LocalPlayer player = minecraft.player;
		if (!GearConfig.enabled || !GearConfig.compareTooltip || player == null || stack.isEmpty()) {
			return;
		}
		EquipmentSlot slot = slotOf(stack);
		if (slot == null) {
			return;
		}
		ItemStack worn = player.getItemBySlot(slot);
		// Nothing to compare with, the hovered item is the worn one itself, or the held item is no gear of
		// the same place (e.g. a gem or a block in the hand)
		if (worn.isEmpty() || worn == stack || slotOf(worn) != slot) {
			return;
		}
		if (!minecraft.hasShiftDown()) {
			lines.add(Component.translatable("gearbalance.compare.hint").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		lines.add(Component.translatable("gearbalance.compare.title", worn.getHoverName()).withStyle(ChatFormatting.GRAY));
		Map<String, double[]> mine = sums(stack, slot);
		Map<String, double[]> theirs = sums(worn, slot);
		Map<String, GemBonuses.Bonus> bonuses = StatsPanel.bonusByAttribute();
		Map<String, Boolean> keys = new LinkedHashMap<>();
		mine.keySet().forEach(key -> keys.put(key, true));
		theirs.keySet().forEach(key -> keys.put(key, true));
		int count = 0;
		for (String key : keys.keySet()) {
			double difference = mine.getOrDefault(key, new double[1])[0] - theirs.getOrDefault(key, new double[1])[0];
			if (Math.abs(difference) < 1.0E-6) {
				continue;
			}
			boolean multiplied = key.endsWith("%");
			String id = multiplied ? key.substring(0, key.length() - 1) : key;
			GemBonuses.Bonus bonus = bonuses.get(id);
			boolean percent = multiplied || (bonus != null && bonus.percent());
			Component name = bonus != null ? Component.translatable("gearbalance.bonus." + bonus.id()) : StatsPanel.attributeName(id);
			// Burning time is better when lower
			boolean better = id.equals(Attributes.BURNING_TIME.getRegisteredName()) ? difference < 0 : difference > 0;
			lines.add(Component.literal(" " + StatsPanel.number(difference, percent, true) + " ").append(name)
					.withStyle(better ? ChatFormatting.GREEN : ChatFormatting.RED));
			count++;
		}
		// Gem resistances (not attributes)
		for (GemBonuses.Special special : GemBonuses.Special.values()) {
			if (special == GemBonuses.Special.NONE || !Sockets.active()) {
				continue;
			}
			double difference = Sockets.resistancePercent(stack, special) - Sockets.resistancePercent(worn, special);
			if (Math.abs(difference) > 1.0E-6) {
				String id = special.name().toLowerCase(Locale.ROOT) + "_resistance";
				lines.add(Component.literal(" " + StatsPanel.number(difference / 100.0, true, true) + " ")
						.append(Component.translatable("gearbalance.bonus." + id))
						.withStyle(difference > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
				count++;
			}
		}
		if (stack.isDamageableItem() && worn.isDamageableItem()) {
			int durability = stack.getMaxDamage() - worn.getMaxDamage();
			if (durability != 0) {
				lines.add(Component.literal(" " + (durability > 0 ? "+" : "") + durability + " ")
						.append(Component.translatable("gearbalance.compare.durability"))
						.withStyle(durability > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
				count++;
			}
			// Durability left as numbers: this piece and the worn one (shields show it in their own lines)
			if (!Shields.isShield(stack)) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			int wornLeft = worn.getMaxDamage() - worn.getDamageValue();
			lines.add(Component.literal(" ").append(Component.translatable("gearbalance.compare.durability_now",
					left + "/" + stack.getMaxDamage()))
					.withStyle(left > wornLeft ? ChatFormatting.GREEN : left < wornLeft ? ChatFormatting.RED : ChatFormatting.GRAY));
			}
		}
		count += shieldValues(stack, worn, lines);
		// Upgrade level and sockets
		if (GearConfig.upgradesActive()) {
			int mineLevel = Upgrades.level(stack);
			int wornLevel = Upgrades.level(worn);
			if (mineLevel != wornLevel || mineLevel > 0) {
				lines.add(Component.literal(" ").append(Component.translatable("gearbalance.compare.level", "+" + mineLevel))
						.withStyle(color(mineLevel - wornLevel)));
			}
		}
		if (Sockets.active() && (Sockets.max(stack) > 0 || Sockets.max(worn) > 0)) {
			int mineSockets = Sockets.count(stack);
			int wornSockets = Sockets.count(worn);
			if (mineSockets != wornSockets || mineSockets > 0) {
				lines.add(Component.literal(" ").append(Component.translatable("gearbalance.compare.sockets", mineSockets))
						.withStyle(color(mineSockets - wornSockets)));
			}
		}
		count += enchantments(stack, worn, lines);
		if (count == 0) {
			lines.add(Component.literal(" ").append(Component.translatable("gearbalance.compare.same")).withStyle(ChatFormatting.GRAY));
		}
	}

	/** Shields: parry window, ready to block, axe lockout and explosion angle compared. Returns the lines added. */
	private static int shieldValues(ItemStack stack, ItemStack worn, List<Component> lines) {
		BlocksAttacks mine = stack.get(DataComponents.BLOCKS_ATTACKS);
		BlocksAttacks theirs = worn.get(DataComponents.BLOCKS_ATTACKS);
		if (!GearConfig.shieldsActive() || !Shields.isShield(stack) || !Shields.isShield(worn) || mine == null
				|| theirs == null) {
			return 0;
		}
		int count = 0;
		if (GearConfig.parryActive()) {
			count += shieldLine(lines, (Parry.windowTicks(stack) - Parry.windowTicks(worn)) / 20.0, " s ",
					"gearbalance.compare.parry", true);
		}
		count += shieldLine(lines, mine.blockDelaySeconds() - theirs.blockDelaySeconds(), " s ", "gearbalance.compare.block_delay",
				false);
		count += shieldLine(lines, (mine.disableCooldownScale() - theirs.disableCooldownScale()) * 100, " % ",
				"gearbalance.compare.axe", false);
		count += shieldLine(lines, explosionAngle(mine) - explosionAngle(theirs), "° ", "gearbalance.compare.explosion", true);
		return count;
	}

	private static float explosionAngle(BlocksAttacks blocks) {
		List<BlocksAttacks.DamageReduction> reductions = blocks.damageReductions();
		return reductions.isEmpty() ? 0 : reductions.get(reductions.size() - 1).horizontalBlockingAngle();
	}

	/** "+0,15 s Parry window": green if better (more when higherIsBetter, else less). */
	private static int shieldLine(List<Component> lines, double difference, String unit, String key, boolean higherIsBetter) {
		if (Math.abs(difference) < 1.0E-4) {
			return 0;
		}
		boolean better = higherIsBetter ? difference > 0 : difference < 0;
		lines.add(Component.literal(" " + (difference > 0 ? "+" : "") + Numbers.format(difference) + unit)
				.append(Component.translatable(key)).withStyle(better ? ChatFormatting.GREEN : ChatFormatting.RED));
		return 1;
	}

	private static ChatFormatting color(int difference) {
		return difference > 0 ? ChatFormatting.GREEN : difference < 0 ? ChatFormatting.RED : ChatFormatting.GRAY;
	}

	/** Differing enchantments: "+Sharpness IV" (only here), "-Unbreaking II" (only worn), "IV instead of III". Returns added lines. */
	private static int enchantments(ItemStack stack, ItemStack worn, List<Component> lines) {
		ItemEnchantments mine = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemEnchantments theirs = worn.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		Set<Holder<Enchantment>> all = new LinkedHashSet<>(mine.keySet());
		all.addAll(theirs.keySet());
		int count = 0;
		for (Holder<Enchantment> enchantment : all) {
			int a = mine.getLevel(enchantment);
			int b = theirs.getLevel(enchantment);
			if (a == b) {
				continue;
			}
			Component line;
			if (b == 0) {
				line = Component.literal(" +").append(Enchantment.getFullname(enchantment, a).copy().withStyle(ChatFormatting.GREEN))
						.withStyle(ChatFormatting.GREEN);
			} else if (a == 0) {
				line = Component.literal(" -").append(Enchantment.getFullname(enchantment, b).copy().withStyle(ChatFormatting.RED))
						.withStyle(ChatFormatting.RED);
			} else {
				ChatFormatting color = a > b ? ChatFormatting.GREEN : ChatFormatting.RED;
				line = Component.literal(" ").append(Component.translatable("gearbalance.compare.enchantment",
						Enchantment.getFullname(enchantment, a), Component.translatable("enchantment.level." + b))
						.withStyle(color));
			}
			lines.add(line);
			count++;
		}
		return count;
	}

	/** Where the item would be worn or held: its armor slot, the off hand for shields, else the main hand. */
	private static EquipmentSlot slotOf(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null) {
			return equippable.slot();
		}
		boolean hasModifiers = !sums(stack, EquipmentSlot.MAINHAND).isEmpty();
		return hasModifiers ? EquipmentSlot.MAINHAND : null;
	}

	/** Attribute id (plus "%" for multiplied modifiers) -> sum, as the item gives it in this slot. */
	private static Map<String, double[]> sums(ItemStack stack, EquipmentSlot slot) {
		Map<String, double[]> sums = new LinkedHashMap<>();
		stack.forEachModifier(slot, (attribute, modifier) -> {
			if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					|| attribute.getRegisteredName().equals(Attributes.BLOCK_INTERACTION_RANGE.getRegisteredName())) {
				return;
			}
			String key = attribute.getRegisteredName()
					+ (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ? "%" : "");
			sums.computeIfAbsent(key, k -> new double[1])[0] += modifier.amount();
		});
		return sums;
	}
}
