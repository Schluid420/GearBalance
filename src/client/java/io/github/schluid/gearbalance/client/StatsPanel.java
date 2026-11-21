package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.client.mixin.AbstractContainerScreenAccessor;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.Sockets;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
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
 * one line per value with its sources, e.g. "Armor Toughness 12 (8 Armor / 4 Gems)".
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

	private static final int BACKGROUND = 0xF0100010;
	private static final int FRAME = 0xFF5000A0;
	private static final int HEADING = 0xFFFFC850;
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
		}
		return values;
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
		armorValues.forEach((key, value) -> addLine(armor, key, value, bonuses, "gearbalance.stats.part.armor", 0));
		for (GemBonuses.Special special : GemBonuses.Special.values()) {
			double resistance = Sockets.resistance(player, special);
			if (special != GemBonuses.Special.NONE && resistance > 0) {
				String id = special.name().toLowerCase(Locale.ROOT) + "_resistance";
				armor.add(Component.translatable("gearbalance.bonus." + id).withStyle(ChatFormatting.GRAY)
						.append(Component.literal(" " + FORMAT.format(resistance * 100) + "%").withStyle(ChatFormatting.GREEN))
						.append(Component.literal(" (").append(Component.translatable("gearbalance.stats.part.gems"))
								.append(")").withStyle(ChatFormatting.DARK_GRAY)));
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
			addLine(handLines, key, value, bonuses, "gearbalance.stats.part.item", base);
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

	/** "Armor Toughness 12 (8 Armor / 4 Gems)"; nothing for a value of 0. */
	private static void addLine(List<Component> lines, String key, Value value, Map<String, GemBonuses.Bonus> bonuses,
			String itemPart, double base) {
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
		String[] keys = {itemPart, "gearbalance.stats.part.upgrades", "gearbalance.stats.part.gems"};
		MutableComponent split = Component.empty();
		int count = 0;
		for (int i = 0; i < 3; i++) {
			double part = value.parts[i] + (i == ITEM ? base : 0);
			if (Math.abs(part) < 1.0E-6) {
				continue;
			}
			if (count++ > 0) {
				split.append(" / ");
			}
			split.append(number(part, percent, false) + " ").append(Component.translatable(keys[i]));
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
		int rows = Math.max(1, (GUI_HEIGHT - TITLE_HEIGHT - 4) / ROW_HEIGHT);
		scroll = Math.min(scroll, Math.max(0, lines.size() - rows));
		graphics.fill(x, y, x + width, y + GUI_HEIGHT, BACKGROUND);
		graphics.outline(x, y, width, GUI_HEIGHT, FRAME);
		drawFitted(graphics, font, Component.translatable("gearbalance.stats.title").withStyle(ChatFormatting.BOLD),
				x + 4, y + 4, width - 8, TITLE);
		if (lines.isEmpty()) {
			drawFitted(graphics, font, Component.translatable("gearbalance.stats.none"), x + 4, y + TITLE_HEIGHT + 2,
					width - 8, 0xFFAAAAAA);
			return;
		}
		for (int i = 0; i < rows && scroll + i < lines.size(); i++) {
			Line line = lines.get(scroll + i);
			boolean heading = line.heading();
			int lineY = y + TITLE_HEIGHT + 2 + i * ROW_HEIGHT;
			drawFitted(graphics, font, heading ? line.text().copy().withStyle(ChatFormatting.BOLD) : line.text(),
					x + (heading ? 4 : 8), lineY, width - (heading ? 8 : 12), heading ? HEADING : 0xFFFFFFFF);
		}
		if (lines.size() > rows) {
			int track = GUI_HEIGHT - TITLE_HEIGHT - 4;
			int bar = Math.max(10, track * rows / lines.size());
			int barY = y + TITLE_HEIGHT + (track - bar) * scroll / Math.max(1, lines.size() - rows);
			graphics.fill(x + width - 3, barY, x + width - 1, barY + bar, 0xFFC0C0C0);
		}
	}

	/** Draws text at TEXT_SCALE, smaller if it is still too wide (down to MIN_SCALE), then cut off. */
	private static void drawFitted(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int maxWidth,
			int color) {
		int width = font.width(text);
		float scale = Math.max(MIN_SCALE, Math.min(TEXT_SCALE, (float) maxWidth / width));
		Component shown = text;
		if (width * scale > maxWidth) {
			String plain = font.plainSubstrByWidth(text.getString(), (int) (maxWidth / scale) - font.width("…"));
			shown = Component.literal(plain + "…").withStyle(text.getStyle());
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + 8 * (1 - scale) / 2);
		graphics.pose().scale(scale, scale);
		graphics.text(font, shown, 0, 0, color, true);
		graphics.pose().popMatrix();
	}
}
