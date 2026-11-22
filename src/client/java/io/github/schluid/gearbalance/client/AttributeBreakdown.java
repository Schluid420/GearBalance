package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.GemValues;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.socket.Titanite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Attribute lines of an item with gems: total plus its sources, e.g. "+0.58 Armor Toughness (chestplate 0.38 + onyx 0.2)";
 * a line only from the item gets just its symbol. Same attribute and kind merge into one line; vanilla order stays.
 */
final class AttributeBreakdown {
	private AttributeBreakdown() {
	}

	/** Item symbols in the socket font: iron sword (weapons and tools), iron chestplate (armor), shield. */
	private static final char TOOL_SYMBOL = '';
	private static final char ARMOR_SYMBOL = '';
	private static final char SHIELD_SYMBOL = '';

	/** What the gems of one type give to one attribute line, and the symbol of their highest cut. */
	private static final class GemPart {
		double amount;
		int cut;
	}

	/** One attribute line found in the tooltip. */
	private record Found(int index, String attribute, int operation, double value, Component name, boolean equals) {
	}

	static void apply(ItemStack stack, List<Component> lines) {
		if (!Sockets.active() || !Sockets.hasGems(stack)) {
			return;
		}
		boolean armor = stack.getItem().components().has(DataComponents.EQUIPPABLE);
		Set<String> percentKeys = new HashSet<>();
		Map<String, Map<String, GemPart>> gems = gemParts(stack, armor, percentKeys);

		// Find the attribute lines; lines of the same attribute and kind are merged into the first one
		Map<String, Found> first = new LinkedHashMap<>();
		Map<String, Double> totals = new LinkedHashMap<>();
		Map<String, Integer> counts = new LinkedHashMap<>();
		List<Integer> remove = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			Found found = find(i, lines.get(i));
			if (found == null) {
				continue;
			}
			String key = found.attribute() + "|" + found.operation() + "|" + found.equals();
			if (first.containsKey(key)) {
				remove.add(i);
			} else {
				first.put(key, found);
			}
			totals.merge(key, found.value(), Double::sum);
			counts.merge(key, 1, Integer::sum);
		}
		char symbol = Shields.isShield(stack.getItem()) ? SHIELD_SYMBOL : armor ? ARMOR_SYMBOL : TOOL_SYMBOL;
		for (Map.Entry<String, Found> entry : first.entrySet()) {
			Found found = entry.getValue();
			double total = totals.get(entry.getKey());
			String gemKey = found.attribute() + "|" + found.operation();
			Map<String, GemPart> parts = gems.getOrDefault(gemKey, Map.of());
			double gemTotal = 0;
			for (GemPart part : parts.values()) {
				gemTotal += part.amount;
			}
			// Gem bonuses typed in percent but added as a plain value (e.g. submerged mining speed): vanilla shows
			// "+0.05", the line is shown in percent like the gem ("+5 %"), its parts too
			boolean percent = !found.equals() && found.operation() == 0 && !parts.isEmpty() && percentKeys.contains(gemKey);
			MutableComponent line;
			if (percent) {
				total *= found.attribute().equals(Attributes.KNOCKBACK_RESISTANCE.value().getDescriptionId()) ? 10 : 100;
				line = rebuildPercent(found, total);
			} else {
				// A line that got others merged into it is written anew with the total
				line = counts.get(entry.getKey()) > 1 && !found.equals()
						? rebuild(found, total)
						: lines.get(found.index()).copy();
			}
			line.append(breakdown(symbol, total - gemTotal, parts, percent));
			lines.set(found.index(), line);
		}
		// Remove merged lines from the bottom up, so the indexes stay right
		for (int i = remove.size() - 1; i >= 0; i--) {
			lines.remove((int) remove.get(i));
		}
		int last = -1;
		if (!first.isEmpty()) {
			last = first.values().stream().mapToInt(Found::index).max().getAsInt();
			final int kept = last;
			last -= (int) remove.stream().filter(i -> i < kept).count();
		}
		addResistances(stack, lines, last);
	}

	/** Resistances (fire, frost, poison) are no attributes: own lines after the attribute lines, with the gem symbol. */
	private static void addResistances(ItemStack stack, List<Component> lines, int lastAttribute) {
		if (!Sockets.isArmorPiece(stack)) {
			return;
		}
		Map<GemBonuses.Bonus, Map<String, GemPart>> specials = new LinkedHashMap<>();
		double factor = Titanite.factor(stack);
		for (String id : Sockets.sockets(stack)) {
			String type = Gems.typeOf(id);
			if (type == null) {
				continue;
			}
			int cut = Gems.cutOf(id);
			for (GemValues.Bonus bonus : GemValues.on(id, true)) {
				if (bonus.kind().special() == GemBonuses.Special.NONE) {
					continue;
				}
				GemPart part = specials.computeIfAbsent(bonus.kind(), k -> new LinkedHashMap<>())
						.computeIfAbsent(type, t -> new GemPart());
				part.amount += bonus.amount(cut) * factor;
				part.cut = Math.max(part.cut, cut);
			}
		}
		int at = lastAttribute < 0 ? lines.size() : lastAttribute + 1;
		for (Map.Entry<GemBonuses.Bonus, Map<String, GemPart>> entry : specials.entrySet()) {
			double total = 0;
			for (GemPart part : entry.getValue().values()) {
				total += part.amount;
			}
			MutableComponent line = GemValues.line(entry.getKey(), total).withStyle(ChatFormatting.BLUE);
			for (Map.Entry<String, GemPart> part : entry.getValue().entrySet()) {
				line.append(Component.literal(" ")).append(symbol(String.valueOf(gemSymbol(part))));
			}
			lines.add(Math.min(at++, lines.size()), line);
		}
	}

	/** "+7 % Speed" or "-2 % Speed" with the merged total, in the vanilla colors. */
	private static MutableComponent rebuild(Found found, double total) {
		boolean minus = total < 0;
		return Component.translatable("attribute.modifier." + (minus ? "take." : "plus.") + found.operation(),
				ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(total)), found.name())
				.withStyle(minus ? ChatFormatting.RED : ChatFormatting.BLUE);
	}

	/** "+5 % Submerged Mining Speed": a plain-value line in percent, with vanilla's percent format and colors. */
	private static MutableComponent rebuildPercent(Found found, double percent) {
		boolean minus = percent < 0;
		return Component.translatable("attribute.modifier." + (minus ? "take." : "plus.") + "1",
				ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(percent)), found.name())
				.withStyle(minus ? ChatFormatting.RED : ChatFormatting.BLUE);
	}

	/** " (chestplate 0.38 + onyx 0.2)": the item's own part, then each gem type in its color; with "%" on percent lines. */
	private static Component breakdown(char symbol, double own, Map<String, GemPart> parts, boolean percent) {
		String unit = percent ? "%" : "";
		MutableComponent text = Component.empty();
		boolean hasOwn = parts.isEmpty() || Math.abs(own) > 0.0001;
		// Only one source (the item itself or one gem type): just its symbol, without brackets and numbers
		if (parts.isEmpty() || !hasOwn && parts.size() == 1) {
			char only = parts.isEmpty() ? symbol : gemSymbol(parts.entrySet().iterator().next());
			return text.append(Component.literal(" ")).append(symbol(String.valueOf(only)));
		}
		text.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
		boolean firstPart = true;
		if (hasOwn) {
			text.append(symbol(symbol + GAP)).append(Component.literal(number(own) + unit).withStyle(ChatFormatting.BLUE));
			firstPart = false;
		}
		for (Map.Entry<String, GemPart> entry : parts.entrySet()) {
			if (!firstPart) {
				text.append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY));
			}
			firstPart = false;
			text.append(symbol(gemSymbol(entry) + GAP));
			text.append(GearBalanceClient.gemColored(entry.getKey(), Component.literal(number(entry.getValue().amount) + unit)));
		}
		text.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
		return text;
	}

	/** Small gap between a symbol and its number: two 1-pixel spacers of the socket font instead of a space. */
	private static final String GAP = "\uE033\uE033";

	private static char gemSymbol(Map.Entry<String, GemPart> entry) {
		return GearBalanceClient.gemSymbol(entry.getKey(), entry.getValue().cut);
	}

	private static Component symbol(String symbols) {
		return Component.literal(symbols)
				.withStyle(style -> style.withFont(GearBalanceClient.SOCKET_FONT).withColor(ChatFormatting.WHITE)
						.withShadowColor(GearBalanceClient.ICON_SHADOW));
	}

	private static String number(double value) {
		return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value);
	}

	/**
	 * Gem parts per "attribute description id|operation id" and gem type, in tooltip units (percent; knockback x10).
	 * Bonuses typed in percent but added as a plain value are kept in percent; their keys go into percentKeys.
	 */
	private static Map<String, Map<String, GemPart>> gemParts(ItemStack stack, boolean armor, Set<String> percentKeys) {
		Map<String, Map<String, GemPart>> result = new LinkedHashMap<>();
		double factor = Titanite.factor(stack);
		for (String id : Sockets.sockets(stack)) {
			String type = Gems.typeOf(id);
			if (type == null) {
				continue;
			}
			int cut = Gems.cutOf(id);
			for (GemValues.Bonus bonus : GemValues.on(id, armor)) {
				GemBonuses.Bonus kind = bonus.kind();
				if (kind.special() != GemBonuses.Special.NONE) {
					continue;
				}
				double amount = kind.modifierAmount(bonus.amount(cut) * factor);
				boolean add = kind.operation() == AttributeModifier.Operation.ADD_VALUE;
				for (Holder<Attribute> attribute : kind.attributes()) {
					String key = attribute.value().getDescriptionId() + "|" + kind.operation().id();
					double shown;
					if (add && kind.percent()) {
						// Shown in percent, as typed: 5 stays 5
						shown = amount * 100;
						percentKeys.add(key);
					} else {
						shown = add ? (attribute.value() == Attributes.KNOCKBACK_RESISTANCE.value() ? amount * 10 : amount) : amount * 100;
					}
					GemPart part = result.computeIfAbsent(key, k -> new LinkedHashMap<>()).computeIfAbsent(type, t -> new GemPart());
					part.amount += shown;
					part.cut = Math.max(part.cut, cut);
				}
			}
		}
		return result;
	}

	/** An attribute line ("+2 Armor", "-5 % Speed", " 7 Attack Damage"), or null. */
	private static Found find(int index, Component line) {
		TranslatableContents contents = attributeContents(line);
		if (contents == null) {
			return null;
		}
		String key = contents.getKey();
		Object[] args = contents.getArgs();
		if (args.length < 2 || !(args[1] instanceof Component name)
				|| !(name.getContents() instanceof TranslatableContents attribute)) {
			return null;
		}
		double value;
		try {
			value = Double.parseDouble(String.valueOf(args[0] instanceof Component c ? c.getString() : args[0]));
		} catch (NumberFormatException e) {
			return null;
		}
		String[] parts = key.split("\\.");
		int operation;
		try {
			operation = Integer.parseInt(parts[parts.length - 1]);
		} catch (NumberFormatException e) {
			return null;
		}
		boolean take = key.startsWith("attribute.modifier.take.");
		boolean equals = key.startsWith("attribute.modifier.equals.");
		return new Found(index, attribute.getKey(), operation, take ? -value : value, name, equals);
	}

	/** The "attribute.modifier.*" part of a line: the line itself or one of its parts (" " + text for weapons). */
	private static TranslatableContents attributeContents(Component line) {
		if (line.getContents() instanceof TranslatableContents contents && contents.getKey().startsWith("attribute.modifier.")) {
			return contents;
		}
		for (Component part : line.getSiblings()) {
			if (part.getContents() instanceof TranslatableContents contents && contents.getKey().startsWith("attribute.modifier.")) {
				return contents;
			}
		}
		return null;
	}
}
