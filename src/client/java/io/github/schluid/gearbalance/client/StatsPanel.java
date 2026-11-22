package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.client.mixin.AbstractContainerScreenAccessor;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.GemValues;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.socket.Titanite;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Inventory: an armor button opens a panel on the right listing what worn armor and the held item give,
 * one line per value with its sources as symbols, e.g. "Armor Toughness 12 (chestplate 8 / onyx 4)".
 * Only values the gear changes are listed; the panel stays open until the button is pressed again.
 */
final class StatsPanel {
	private StatsPanel() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int GAP = 4;
	private static final int MAX_WIDTH = 220;
	private static final int BUTTON_SIZE = 13;
	/** Armor symbol of the health bar, drawn on the button. */
	private static final Identifier ARMOR_ICON = Identifier.withDefaultNamespace("hud/armor_full");
	private static final int ICON_SIZE = 9;
	/** All text in the panel is drawn at this size. */
	private static final float TEXT_SCALE = 0.75F;
	private static final int ROW_HEIGHT = 8;
	private static final int TITLE_HEIGHT = 12;

	/**
	 * Frame in titanium blue (the color of titanium tooltips): double line, corner brackets with a light point,
	 * blue head bar with a line and two diamonds.
	 */
	private static final int EDGE = 0xFF0E1119, FRAME_LINE = 0xFF96AAD2, FRAME_LIGHT = 0xFFB4C4E6, FRAME_DARK = 0xFF34405C,
			GAP_DARK = 0xFF131721, BACKGROUND = 0xF6161B28, HEAD = 0xFF3A4A70, SPARK = 0xFFF2F6FF;
	/** Width of the frame lines; the content starts inside. */
	private static final int INSET = 4;
	/** Top of the first line: below the head bar and its line. */
	private static final int CONTENT_TOP = INSET + TITLE_HEIGHT + 3;
	private static final int HEADING = 0xFFFFC850;
	/** Symbols of the socket font for the sources, the same as in item tooltips: chestplate, sword, shield. */
	private static final char ARMOR_ICON_CHAR = '\uE032', TOOL_ICON_CHAR = '\uE030', SHIELD_ICON_CHAR = '\uE034';
	/** Small gap between a symbol and its number: two 1-pixel spacers of the socket font. */
	private static final String ICON_GAP = "\uE033\uE033";
	private static final int TITLE = 0xFFFFFFFF;
	private static final float MIN_SCALE = 0.5F;

	private static final DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

	/** Open or closed; stays as it is while the game runs. */
	private static boolean open = false;
	private static int scroll = 0;

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			Button button = new IconButton(Component.translatable("gearbalance.stats.button"), b -> {
				open = !open;
				scroll = 0;
			});
			button.setTooltip(Tooltip.create(
					Component.translatable("gearbalance.stats.button")));
			Screens.getWidgets(screen).add(button);
			place(screen, button);
			ScreenEvents.afterTick(screen).register(s -> place(s, button));
			ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				if (button.visible && open) {
					render(client, s, graphics);
				}
			});
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) -> {
				if (!open || !button.visible || mouseX < panelX(s) || mouseX > panelX(s) + panelWidth(s)) {
					return true;
				}
				scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
				return false;
			});
		});
	}

	/** Left edge of the inventory window; it moves when the recipe book opens. */
	private static int guiLeft(Screen screen) {
		if (screen instanceof AbstractContainerScreenAccessor window) {
			return window.gearbalance$leftPos();
		}
		return (screen.width - GUI_WIDTH) / 2;
	}

	/** Top edge of the inventory window; it moves down with a bag. */
	private static int guiTop(Screen screen) {
		if (screen instanceof AbstractContainerScreenAccessor window) {
			return window.gearbalance$topPos();
		}
		return (screen.height - GUI_HEIGHT) / 2;
	}

	/** Button with the armor symbol, always drawn at the same place (it does not move when pressed or hovered). */
	private static final class IconButton extends Button.Plain {
		IconButton(Component message, OnPress onPress) {
			super(0, 0, BUTTON_SIZE, BUTTON_SIZE, message, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			extractDefaultSprite(graphics);
			int offset = (BUTTON_SIZE - ICON_SIZE) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_ICON, getX() + offset, getY() + offset, ICON_SIZE, ICON_SIZE);
		}
	}

	/** Small button in the top right corner of the inventory window; hidden while the mod is off. */
	private static void place(Screen screen, AbstractWidget button) {
		button.setX(guiLeft(screen) + GUI_WIDTH - BUTTON_SIZE - 3);
		button.setY(guiTop(screen) + 3);
		button.visible = GearConfig.enabled;
	}

	/** Space the open panel takes next to the inventory (for JEI), null while it is closed. */
	static Rect2i area(Screen screen) {
		if (!open || !GearConfig.enabled || !(screen instanceof InventoryScreen)) {
			return null;
		}
		return new Rect2i(panelX(screen), guiTop(screen), panelWidth(screen), GUI_HEIGHT);
	}

	private static int panelX(Screen screen) {
		return guiLeft(screen) + GUI_WIDTH + GAP;
	}

	private static int panelWidth(Screen screen) {
		return Math.max(60, Math.min(MAX_WIDTH, screen.width - panelX(screen) - GAP));
	}

	// ---------------------------------------------------------------- values

	/** One value: item, upgrade and gems as parts; GearBalance's player changes count in the total only. Percent in percent. */
	private static final class Value {
		final double[] parts = new double[4];
		boolean percent;
		/** The gem part split by gem type, to show each with its symbol. */
		final Map<String, GemShare> gems = new LinkedHashMap<>();
	}

	/** What the gems of one type add to a value, and their highest cut (for the symbol). */
	private static final class GemShare {
		double amount;
		int cut;

		void add(double value, int gemCut) {
			amount += value;
			cut = Math.max(cut, gemCut);
		}
	}

	private static final int ITEM = 0, UPGRADE = 1, GEMS = 2, GEARBALANCE = 3;

	/** Section ("armor" or "hand") -> attribute (plus "%" for percent modifiers) -> value. */
	private static Map<String, Value> collect(Player player, List<EquipmentSlot> slots) {
		Map<String, Value> values = new LinkedHashMap<>();
		for (EquipmentSlot slot : slots) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemAttributeModifiers base = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			ItemAttributeModifiers upgraded = Upgrades.modifiers(stack, base);
			ItemAttributeModifiers gems = Sockets.modifiers(stack, upgraded);
			Map<String, double[]> b = sums(base, slot);
			Map<String, double[]> u = sums(upgraded, slot);
			Map<String, double[]> g = sums(gems, slot);
			for (String key : g.keySet()) {
				double item = b.getOrDefault(key, new double[1])[0];
				double upgrade = u.getOrDefault(key, new double[1])[0] - item;
				double gem = g.get(key)[0] - item - upgrade;
				Value value = values.computeIfAbsent(key, k -> new Value());
				value.percent = key.endsWith("%");
				value.parts[ITEM] += item;
				value.parts[UPGRADE] += upgrade;
				value.parts[GEMS] += gem;
			}
			addGemTypes(stack, values);
		}
		return values;
	}

	/** What each gem type of the piece adds to the values (attribute keys as in sums()), for the symbols. */
	private static void addGemTypes(ItemStack stack, Map<String, Value> values) {
		if (!Sockets.active() || BreakProtection.wornOut(stack)) {
			return;
		}
		boolean armor = stack.getItem().components().has(DataComponents.EQUIPPABLE);
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
				String suffix = kind.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ? "%" : "";
				for (Holder<Attribute> attribute : kind.attributes()) {
					Value value = values.get(attribute.getRegisteredName() + suffix);
					if (value != null) {
						value.gems.computeIfAbsent(type, t -> new GemShare()).add(amount, cut);
					}
				}
			}
		}
	}

	/** Attribute id (with "%" for multiplied modifiers) -> sum of the modifiers for this slot. */
	private static Map<String, double[]> sums(ItemAttributeModifiers modifiers, EquipmentSlot slot) {
		Map<String, double[]> sums = new LinkedHashMap<>();
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (!entry.slot().test(slot) || entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
				continue;
			}
			String key = entry.attribute().getRegisteredName()
					+ (entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ? "%" : "");
			sums.computeIfAbsent(key, k -> new double[1])[0] += entry.modifier().amount();
		}
		return sums;
	}

	/** Attribute -> gem bonus with only this attribute (for its name and percent display). */
	static Map<String, GemBonuses.Bonus> bonusByAttribute() {
		Map<String, GemBonuses.Bonus> map = new HashMap<>();
		List<GemBonuses.Bonus> all = new ArrayList<>(GemBonuses.choices(false));
		all.addAll(GemBonuses.choices(true));
		for (GemBonuses.Bonus bonus : all) {
			if (!bonus.attributes().isEmpty()) {
				map.putIfAbsent(bonus.attributes().get(0).getRegisteredName(), bonus);
			}
		}
		return map;
	}

	/** A line of the panel: a section heading or a value. */
	private record Line(Component text, boolean heading) {
	}

	/** The lines of the panel. */
	private static List<Line> lines(Player player) {
		List<Line> lines = new ArrayList<>();
		Map<String, GemBonuses.Bonus> bonuses = bonusByAttribute();
		List<Component> armor = new ArrayList<>();
		// The shield in the off hand counts to the armor (its gems give armor bonuses)
		Map<String, Value> armorValues = collect(player,
				List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND));
		// What GearBalance changes on the player itself (armor weight, set bonus ...) goes straight into the values
		addGearBalance(player, armorValues);
		armorValues.forEach((key, value) -> addLine(armor, key, value, bonuses, ARMOR_ICON_CHAR, 0));
		for (GemBonuses.Special special : GemBonuses.Special.values()) {
			double resistance = Sockets.resistance(player, special);
			if (special != GemBonuses.Special.NONE && resistance > 0) {
				String id = special.name().toLowerCase(Locale.ROOT) + "_resistance";
				MutableComponent sources = Component.empty();
				Map<String, GemShare> shares = resistanceShares(player, special);
				if (shares.isEmpty()) {
					sources.append(Component.translatable("gearbalance.stats.part.gems"));
				}
				int count = 0;
				for (Map.Entry<String, GemShare> share : shares.entrySet()) {
					if (count++ > 0) {
						sources.append(" / ");
					}
					sources.append(gemPart(share.getKey(), share.getValue(), FORMAT.format(share.getValue().amount) + "%"));
				}
				armor.add(Component.translatable("gearbalance.bonus." + id).withStyle(ChatFormatting.GRAY)
						.append(Component.literal(" " + FORMAT.format(resistance * 100) + "%").withStyle(ChatFormatting.GREEN))
						.append(Component.literal(" (").append(sources).append(")").withStyle(ChatFormatting.DARK_GRAY)));
			}
		}
		if (!armor.isEmpty()) {
			lines.add(new Line(Component.translatable("gearbalance.stats.armor"), true));
			armor.forEach(text -> lines.add(new Line(text, false)));
		}
		ItemStack hand = player.getMainHandItem();
		List<Component> handLines = new ArrayList<>();
		collect(player, List.of(EquipmentSlot.MAINHAND)).forEach((key, value) -> {
			// Like the item tooltip: damage and speed include the player's base value
			double base = key.equals(Attributes.ATTACK_DAMAGE.getRegisteredName()) ? 1
					: key.equals(Attributes.ATTACK_SPEED.getRegisteredName()) ? 4 : 0;
			addLine(handLines, key, value, bonuses, Shields.isShield(hand) ? SHIELD_ICON_CHAR : TOOL_ICON_CHAR, base);
		});
		if (!handLines.isEmpty()) {
			lines.add(new Line(Component.translatable("gearbalance.stats.hand", hand.getHoverName()), true));
			handLines.forEach(text -> lines.add(new Line(text, false)));
		}
		return lines;
	}

	private static final String GEARBALANCE_NAMESPACE = "gearbalance";

	/** Adds the modifiers GearBalance puts on the player to the values (added and multiplied apart). */
	private static void addGearBalance(Player player, Map<String, Value> values) {
		for (AttributeInstance instance : player.getAttributes().getSyncableAttributes()) {
			double add = 0;
			double multiplied = 0;
			for (AttributeModifier modifier : instance.getModifiers()) {
				if (!modifier.id().getNamespace().equals(GEARBALANCE_NAMESPACE)) {
					continue;
				}
				if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
					add += modifier.amount();
				} else {
					multiplied += modifier.amount();
				}
			}
			String id = instance.getAttribute().getRegisteredName();
			if (Math.abs(add) > 1.0E-6) {
				values.computeIfAbsent(id, k -> new Value()).parts[GEARBALANCE] += add;
			}
			if (Math.abs(multiplied) > 1.0E-6) {
				Value value = values.computeIfAbsent(id + "%", k -> new Value());
				value.percent = true;
				value.parts[GEARBALANCE] += multiplied;
			}
		}
	}

	/** "Armor Toughness 12 (chestplate 8 / onyx 4)" with symbols; nothing for a value of 0. */
	private static void addLine(List<Component> lines, String key, Value value, Map<String, GemBonuses.Bonus> bonuses,
			char itemIcon, double base) {
		String attributeId = key.endsWith("%") ? key.substring(0, key.length() - 1) : key;
		// Reach changes two attributes: show it once
		if (attributeId.equals(Attributes.BLOCK_INTERACTION_RANGE.getRegisteredName())) {
			return;
		}
		double total = base + value.parts[ITEM] + value.parts[UPGRADE] + value.parts[GEMS] + value.parts[GEARBALANCE];
		if (Math.abs(total - base) < 1.0E-6) {
			return;
		}
		GemBonuses.Bonus bonus = bonuses.get(attributeId);
		boolean percent = value.percent || (bonus != null && bonus.percent());
		Component name = bonus != null ? Component.translatable("gearbalance.bonus." + bonus.id())
				: attributeName(attributeId);
		MutableComponent line = Component.empty().append(name.copy().withStyle(ChatFormatting.GRAY));
		line.append(Component.literal(" " + number(total, percent, value.percent))
				.withStyle(total - base >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
		MutableComponent split = Component.empty();
		int count = 0;
		for (int i = 0; i < 3; i++) {
			double part = value.parts[i] + (i == ITEM ? base : 0);
			if (Math.abs(part) < 1.0E-6) {
				continue;
			}
			// Gems: each type with its symbol and color, as long as the split adds up to the gem part
			if (i == GEMS && gemsAddUp(value)) {
				for (Map.Entry<String, GemShare> gem : value.gems.entrySet()) {
					if (count++ > 0) {
						split.append(" / ");
					}
					split.append(gemPart(gem.getKey(), gem.getValue(), number(gem.getValue().amount, percent, false)));
				}
				continue;
			}
			if (count++ > 0) {
				split.append(" / ");
			}
			if (i == ITEM) {
				split.append(icon(itemIcon)).append(number(part, percent, false));
			} else {
				split.append(number(part, percent, false) + " ")
						.append(Component.translatable(i == UPGRADE ? "gearbalance.stats.part.upgrades" : "gearbalance.stats.part.gems"));
			}
		}
		// Where it comes from (what GearBalance adds is in the total but not listed)
		if (count > 0) {
			line.append(Component.literal(" (").append(split).append(")").withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.add(line);
	}

	/** "12", "+8%" (multiplied modifiers always with sign), "-10%". */
	static String number(double value, boolean percent, boolean signed) {
		String text = FORMAT.format(percent ? value * 100 : value) + (percent ? "%" : "");
		return signed && value > 0 ? "+" + text : text;
	}

	static Component attributeName(String id) {
		Optional<Holder.Reference<Attribute>> attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
				.get(net.minecraft.resources.Identifier.parse(id));
		return attribute.<Component>map(a -> Component.translatable(a.value().getDescriptionId()))
				.orElse(Component.literal(id));
	}

	// ---------------------------------------------------------------- drawing

	private static void render(Minecraft client, Screen screen, GuiGraphicsExtractor graphics) {
		if (client.player == null) {
			return;
		}
		Font font = client.font;
		List<Line> lines = lines(client.player);
		int x = panelX(screen);
		int width = panelWidth(screen);
		int y = guiTop(screen);
		int track = GUI_HEIGHT - CONTENT_TOP - INSET - 1;
		int rows = Math.max(1, track / ROW_HEIGHT);
		scroll = Math.min(scroll, Math.max(0, lines.size() - rows));
		drawFrame(graphics, x, y, width, GUI_HEIGHT);
		// Title in the head bar, left of the two diamonds
		drawFitted(graphics, font, Component.translatable("gearbalance.stats.title").withStyle(ChatFormatting.BOLD),
				x + INSET + 3, y + INSET + 2, width - 2 * INSET - 22, TITLE);
		if (lines.isEmpty()) {
			drawFitted(graphics, font, Component.translatable("gearbalance.stats.none"), x + INSET + 2, y + CONTENT_TOP,
					width - 2 * INSET - 4, 0xFFAAAAAA);
			return;
		}
		for (int i = 0; i < rows && scroll + i < lines.size(); i++) {
			Line line = lines.get(scroll + i);
			boolean heading = line.heading();
			int lineY = y + CONTENT_TOP + i * ROW_HEIGHT;
			drawFitted(graphics, font, heading ? line.text().copy().withStyle(ChatFormatting.BOLD) : line.text(),
					x + INSET + (heading ? 2 : 6), lineY, width - 2 * INSET - (heading ? 4 : 10), heading ? HEADING : 0xFFFFFFFF);
		}
		if (lines.size() > rows) {
			int bar = Math.max(10, track * rows / lines.size());
			int barY = y + CONTENT_TOP + (track - bar) * scroll / Math.max(1, lines.size() - rows);
			int barX = x + width - INSET - 3;
			graphics.fill(barX, barY, barX + 2, barY + bar, FRAME_LINE);
		}
	}

	/** Dark edge, double line, background, head bar with line and diamonds, corner brackets. */
	private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, EDGE);
		graphics.outline(x + 1, y + 1, width - 2, height - 2, FRAME_LINE);
		graphics.outline(x + 2, y + 2, width - 4, height - 4, GAP_DARK);
		graphics.outline(x + 3, y + 3, width - 6, height - 6, FRAME_DARK);
		graphics.fill(x + INSET, y + INSET, x + width - INSET, y + height - INSET, BACKGROUND);
		graphics.fill(x + INSET, y + INSET, x + width - INSET, y + INSET + TITLE_HEIGHT, HEAD);
		graphics.fill(x + INSET, y + INSET + TITLE_HEIGHT, x + width - INSET, y + INSET + TITLE_HEIGHT + 1, FRAME_LINE);
		int centerY = y + INSET + TITLE_HEIGHT / 2;
		diamond(graphics, x + width - INSET - 5, centerY);
		diamond(graphics, x + width - INSET - 11, centerY);
		bracket(graphics, x, y, 1, 1);
		bracket(graphics, x + width - 1, y, -1, 1);
		bracket(graphics, x, y + height - 1, 1, -1);
		bracket(graphics, x + width - 1, y + height - 1, -1, -1);
	}

	/** A diamond 5 pixels wide around (centerX, centerY) with a light middle. */
	private static void diamond(GuiGraphicsExtractor graphics, int centerX, int centerY) {
		graphics.fill(centerX, centerY - 2, centerX + 1, centerY + 3, FRAME_LIGHT);
		graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, FRAME_LIGHT);
		graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, FRAME_LIGHT);
		graphics.fill(centerX, centerY, centerX + 1, centerY + 1, SPARK);
	}

	/** A corner bracket: an L of 7 pixels, 2 thick, from the corner pixel inwards (dx, dy = 1 or -1), light point inside. */
	private static void bracket(GuiGraphicsExtractor graphics, int cornerX, int cornerY, int dx, int dy) {
		block(graphics, cornerX, cornerY, dx, dy, 7, 2, FRAME_LIGHT);
		block(graphics, cornerX, cornerY, dx, dy, 2, 7, FRAME_LIGHT);
		block(graphics, cornerX + 2 * dx, cornerY + 2 * dy, dx, dy, 1, 1, SPARK);
	}

	/** A block of w x h pixels starting at the pixel (px, py) and growing in the direction dx, dy. */
	private static void block(GuiGraphicsExtractor graphics, int px, int py, int dx, int dy, int w, int h, int color) {
		int x0 = dx > 0 ? px : px - w + 1;
		int y0 = dy > 0 ? py : py - h + 1;
		graphics.fill(x0, y0, x0 + w, y0 + h, color);
	}

	/** True if the gem types add up to the gem part (else the part is shown as one number). */
	private static boolean gemsAddUp(Value value) {
		if (value.gems.isEmpty()) {
			return false;
		}
		double sum = 0;
		for (GemShare share : value.gems.values()) {
			sum += share.amount;
		}
		return Math.abs(sum - value.parts[GEMS]) <= Math.max(1.0E-3, Math.abs(value.parts[GEMS]) * 0.01);
	}

	/** Resistance (percent) per gem type from worn armor and shields, like Sockets.resistance. */
	private static Map<String, GemShare> resistanceShares(Player player, GemBonuses.Special special) {
		Map<String, GemShare> shares = new LinkedHashMap<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = player.getItemBySlot(slot);
			boolean shield = (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.MAINHAND) && Shields.isShield(stack);
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR && !shield || BreakProtection.wornOut(stack)) {
				continue;
			}
			double factor = Titanite.factor(stack);
			for (String id : Sockets.sockets(stack)) {
				String type = Gems.typeOf(id);
				if (type == null) {
					continue;
				}
				int cut = Gems.cutOf(id);
				for (GemValues.Bonus bonus : GemValues.on(id, true)) {
					if (bonus.kind().special() == special) {
						shares.computeIfAbsent(type, t -> new GemShare()).add(bonus.amount(cut) * factor, cut);
					}
				}
			}
		}
		return shares;
	}

	/** Gem symbol of the type and cut, then the number in the gem's color. */
	private static Component gemPart(String type, GemShare share, String number) {
		return Component.empty().append(icon(GearBalanceClient.gemSymbol(type, share.cut)))
				.append(GearBalanceClient.gemColored(type, Component.literal(number)));
	}

	/** A symbol of the socket font with a small gap after it, white with the icon shadow like in tooltips. */
	private static Component icon(char symbol) {
		return Component.literal(symbol + ICON_GAP).withStyle(style -> style.withFont(GearBalanceClient.SOCKET_FONT)
				.withColor(ChatFormatting.WHITE).withShadowColor(GearBalanceClient.ICON_SHADOW));
	}

	/** Draws text at TEXT_SCALE, smaller if it is still too wide (down to MIN_SCALE), then cut off. */
	private static void drawFitted(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int maxWidth,
			int color) {
		int width = font.width(text);
		float scale = Math.max(MIN_SCALE, Math.min(TEXT_SCALE, (float) maxWidth / width));
		FormattedCharSequence shown = text.getVisualOrderText();
		if (width * scale > maxWidth) {
			// Cut with the styles kept, so the symbols keep their font
			FormattedText cut = font.substrByWidth(text, (int) (maxWidth / scale) - font.width("…"));
			shown = Language.getInstance().getVisualOrder(FormattedText.composite(cut, Component.literal("…")));
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + 8 * (1 - scale) / 2);
		graphics.pose().scale(scale, scale);
		graphics.text(font, shown, 0, 0, color, true);
		graphics.pose().popMatrix();
	}
}
