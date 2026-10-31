package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.config.VanillaValues;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.GearItems.Kind;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Main settings screen: one tab per item kind plus one for the values per material, each a table
 * with a text field per value. Rows without a value for a column leave that cell empty. Changed values are yellow, invalid input is red.
 * Item values only apply after a restart, so closing after a change offers to quit the game.
 */
public class ConfigScreen extends Screen {
	private static final int TAB_Y = 22;
	private static final int HEADER_Y = 50;
	private static final int TABLE_TOP = 62;
	private static final int ROW_HEIGHT = 22;
	private static final int BOTTOM_SPACE = 50;
	private static final int ICON_WIDTH = 20;
	private static final int NAME_WIDTH = 120;
	private static final int COLUMN_WIDTH = 56;
	private static final int BOX_WIDTH = 50;
	private static final int MAX_COLUMNS = 4;
	private static final int RESET_WIDTH = 20;
	private static final int TOOLTIP_WIDTH = 200;
	private static final int TABLE_WIDTH = ICON_WIDTH + NAME_WIDTH + MAX_COLUMNS * COLUMN_WIDTH + RESET_WIDTH + 4;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFE066;
	private static final int GREEN = 0xFF66E066;
	private static final int ROW_DARK = 0x30000000;
	private static final int ROW_LIGHT = 0x18FFFFFF;

	private enum Tab {
		TOOL(Kind.TOOL),
		WEAPON(Kind.WEAPON),
		ARMOR(Kind.ARMOR),
		OTHER(Kind.OTHER),
		MATERIALS(null),
		SETS(null);

		final Kind kind;

		Tab(Kind kind) {
			this.kind = kind;
		}

		Component title() {
			return Component.translatable("gearbalance.tab." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** One editable value in the table. */
	private record Cell(String id, String headerKey, DoubleSupplier current, double vanilla, double min, double max,
			double step, DoubleConsumer setter, BooleanSupplier changed, Component hint) {
	}

	/**
	 * icon2 is a second icon after the first one (materials: chestplate, then pickaxe), either may be null.
	 * material is only set in the set bonus tab, where the row also gets a button for the bonus type.
	 */
	private record Row(String icon, String icon2, Component name, List<Cell> cells, Runnable reset, Material material) {
	}

	/** Values as they are in the config file, to tell saved changes (green) from unsaved ones (yellow). */
	private final Map<String, Double> saved = new HashMap<>();
	private int savedAt = -1;

	// Kept while the game runs, so the screen opens where it was left
	private static Tab tab = Tab.TOOL;
	private static final Map<Tab, Integer> SCROLL = new EnumMap<>(Tab.class);

	private final Screen parent;
	private final String startSignature = GearConfig.restartSignature();
	private final List<Row> rows = new ArrayList<>();
	private List<String> headers = List.of();
	private int visibleRows;
	private final List<int[]> resetButtons = new ArrayList<>();
	/** The arrow glyph of the font is tiny, so the reset icon is drawn by hand. */
	private static final String[] RESET_ICON = {
			"..###.#",
			".#...##",
			"#...###",
			"#......",
			"#.....#",
			".#...#.",
			"..###.."};
	private static final int RESET_PIXEL = 2;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("gearbalance.config.title"));
		this.parent = parent;
	}

	private int tableLeft() {
		return this.width / 2 - TABLE_WIDTH / 2;
	}

	private int columnX(int column) {
		return tableLeft() + ICON_WIDTH + NAME_WIDTH + column * COLUMN_WIDTH;
	}

	private int scroll() {
		return SCROLL.getOrDefault(tab, 0);
	}

	private int maxScroll() {
		return Math.max(0, rows.size() - visibleRows);
	}


	private void buildRows() {
		if (savedAt != GearConfig.saveCount()) {
			saved.clear();
			savedAt = GearConfig.saveCount();
		}
		rows.clear();
		if (tab == Tab.MATERIALS) {
			List<String> keys = new ArrayList<>();
			for (MaterialStat stat : MaterialStat.values()) {
				keys.add("gearbalance.config.column." + stat.key);
			}
			headers = keys;
			for (Material material : Material.values()) {
				List<Cell> cells = new ArrayList<>();
				for (MaterialStat stat : MaterialStat.values()) {
					cells.add(stat.appliesTo(material) ? cell(material.key + "." + stat.key, "gearbalance.config.column." + stat.key,
							() -> GearConfig.getMaterial(stat, material), stat.vanilla, stat.min, stat.max, stat.step,
							value -> GearConfig.setMaterial(stat, material, value),
							() -> GearConfig.isChanged(stat, material), null) : null);
				}
				// Chestplate first, then pickaxe. A material with only one of them shows it in front.
				String armor = material.armorIcon();
				rows.add(new Row(armor != null ? armor : material.toolIcon(), armor != null ? material.toolIcon() : null,
						Component.translatable("gearbalance.material." + material.key),
						cells, () -> GearConfig.resetMaterial(material), null));
			}
			return;
		}
		if (tab == Tab.SETS) {
			// The bonus type button covers the first two columns, the strength sits in the third
			List<String> keys = new ArrayList<>();
			keys.add("gearbalance.config.column.bonus");
			keys.add(null);
			keys.add("gearbalance.config.column.strength");
			headers = keys;
			for (Material material : Material.values()) {
				if (!material.hasArmor()) {
					continue;
				}
				BonusType type = GearConfig.getSetType(material);
				Cell strength = cell("set." + material.key, "gearbalance.config.column.strength",
						() -> GearConfig.getSetStrength(material), 0, 0, type.max, 0,
						value -> GearConfig.setSet(material, GearConfig.getSetType(material), value),
						() -> GearConfig.getSetStrength(material) > 0,
						Component.translatable("gearbalance.config.column.strength.hint"));
				List<Cell> cells = new ArrayList<>();
				cells.add(null);
				cells.add(null);
				cells.add(strength);
				rows.add(new Row(material.armorIcon(), null, Component.translatable("gearbalance.material." + material.key),
						cells, () -> GearConfig.resetSet(material), material));
			}
			return;
		}

		List<Stat> stats = Stat.forKind(tab.kind);
		List<String> keys = new ArrayList<>();
		for (Stat stat : stats) {
			keys.add("gearbalance.stat." + stat.key);
		}
		headers = keys;
		for (GearItems.Entry entry : GearItems.all()) {
			if (entry.kind() != tab.kind) {
				continue;
			}
			List<Cell> cells = new ArrayList<>();
			for (Stat stat : stats) {
				if (!stat.appliesTo(entry)) {
					cells.add(null);
					continue;
				}
				cells.add(cell(entry.id() + "." + stat.key, "gearbalance.stat." + stat.key,
						() -> GearConfig.getEffective(entry, stat),
						stat.get(VanillaValues.get(entry)), stat.min, stat.max(entry), stat == Stat.DURABILITY ? 1 : 0,
						value -> GearConfig.setOverride(entry, stat, value),
						() -> GearConfig.getOverride(entry.id(), stat) != null, null));
			}
			rows.add(new Row(entry.id(), null, itemName(entry.id()), cells, () -> GearConfig.resetItem(entry.id()), null));
		}
	}

	/** Creates a cell and remembers its value the first time it is shown after the last save. */
	private Cell cell(String id, String headerKey, DoubleSupplier current, double vanilla, double min, double max,
			double step, DoubleConsumer setter, BooleanSupplier changed, Component hint) {
		saved.putIfAbsent(id, current.getAsDouble());
		return new Cell(id, headerKey, current, vanilla, min, max, step, setter, changed, hint);
	}

	/** White = vanilla, green = changed and saved, yellow = changed but not saved yet. */
	private int color(Cell cell) {
		if (!cell.changed().getAsBoolean()) {
			return WHITE;
		}
		Double inFile = saved.get(cell.id());
		return inFile != null && Math.abs(inFile - cell.current().getAsDouble()) < 1.0E-6 ? GREEN : YELLOW;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	private static Component itemName(String id) {
		return Component.translatable(item(id).getDescriptionId());
	}


	@Override
	protected void init() {
		buildRows();
		visibleRows = Math.max(3, (this.height - TABLE_TOP - BOTTOM_SPACE) / ROW_HEIGHT);
		SCROLL.put(tab, Math.max(0, Math.min(scroll(), maxScroll())));

		// Tabs
		Tab[] tabs = Tab.values();
		int tabWidth = Math.min(80, (this.width - 20) / tabs.length - 4);
		int tabsLeft = this.width / 2 - (tabs.length * (tabWidth + 4) - 4) / 2;
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			Button button = Button.builder(t.title(), b -> {
				tab = t;
				this.rebuildWidgets();
			}).bounds(tabsLeft + i * (tabWidth + 4), TAB_Y, tabWidth, 20).build();
			button.active = t != tab;
			if (t == Tab.MATERIALS || t == Tab.SETS) {
				button.setTooltip(Tooltip.create(Component.translatable(
						"gearbalance.tab." + t.name().toLowerCase(Locale.ROOT) + ".tip")));
			}
			this.addRenderableWidget(button);
		}

		// Table rows
		resetButtons.clear();
		for (int r = 0; r < visibleRows; r++) {
			int index = scroll() + r;
			if (index >= rows.size()) {
				break;
			}
			Row row = rows.get(index);
			int y = TABLE_TOP + r * ROW_HEIGHT;

			if (row.material() != null) {
				Material material = row.material();
				CycleButton<BonusType> type = CycleButton.builder(
								(BonusType t) -> Component.translatable("gearbalance.bonus." + t.key), GearConfig.getSetType(material))
						.withValues(BonusType.values())
						.displayOnlyValue()
						.withTooltip(t -> Tooltip.create(Component.translatable("gearbalance.bonus." + t.key + ".tip")))
						.create(columnX(0), y + 1, 2 * COLUMN_WIDTH - 6, ROW_HEIGHT - 2,
								Component.translatable("gearbalance.config.column.bonus"), (button, value) -> {
									GearConfig.setSet(material, value, GearConfig.getSetStrength(material));
									this.rebuildWidgets();
								});
				this.addRenderableWidget(type);
			}

			for (int c = 0; c < row.cells().size(); c++) {
				Cell cell = row.cells().get(c);
				if (cell == null) {
					continue;
				}
				EditBox box = new EditBox(this.font, columnX(c), y + 2, BOX_WIDTH, ROW_HEIGHT - 4,
						Component.translatable(cell.headerKey()));
				box.setMaxLength(9);
				box.setValue(format(cell.current().getAsDouble()));
				box.setTextColor(color(cell));
				MutableComponent tip = Component.empty();
				if (cell.hint() != null) {
					tip.append(cell.hint()).append("\n");
				}
				tip.append(Component.translatable("gearbalance.config.vanilla", format(cell.vanilla())))
						.append("\n")
						.append(Component.translatable("gearbalance.config.range", format(cell.min()), format(cell.max()))
								.withStyle(ChatFormatting.GRAY));
				box.setTooltip(Tooltip.create(tip));
				box.setResponder(text -> {
					Double parsed = parse(text);
					if (parsed == null || parsed < cell.min() || parsed > cell.max()
							|| !fitsStep(parsed, cell.step())) {
						box.setTextColor(RED);
						return;
					}
					cell.setter().accept(parsed);
					box.setTextColor(color(cell));
				});
				this.addRenderableWidget(box);
			}

			// The label stays empty, the icon is drawn in extractRenderState
			resetButtons.add(new int[] {columnX(MAX_COLUMNS) + 2, y + 1});
			Button reset = Button.builder(Component.empty(), b -> {
				row.reset().run();
				this.rebuildWidgets();
			}).bounds(columnX(MAX_COLUMNS) + 2, y + 1, RESET_WIDTH, ROW_HEIGHT - 2)
					.tooltip(Tooltip.create(Component.translatable("gearbalance.config.reset_row"))).build();
			this.addRenderableWidget(reset);
		}

		// Bottom buttons
		int buttonWidth = 100;
		int bottomLeft = this.width / 2 - (4 * (buttonWidth + 4) - 4) / 2;
		int by = this.height - 28;
		this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.config.all_vanilla"), b -> {
			BooleanConsumer answer = yes -> {
				if (yes) {
					GearConfig.resetAll();
				}
				this.minecraft.setScreenAndShow(this);
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
					Component.translatable("gearbalance.reset.title"),
					Component.translatable("gearbalance.reset.message"),
					Component.translatable("gui.yes"),
					Component.translatable("gui.no")));
		}).bounds(bottomLeft, by, buttonWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("gearbalance.config.all_vanilla.tip"))).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.config.profiles"),
				b -> this.minecraft.setScreenAndShow(new ProfilesScreen(this)))
				.bounds(bottomLeft + (buttonWidth + 4), by, buttonWidth, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.config.options"),
				b -> this.minecraft.setScreenAndShow(new OptionsScreen(this)))
				.bounds(bottomLeft + 2 * (buttonWidth + 4), by, buttonWidth, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(bottomLeft + 3 * (buttonWidth + 4), by, buttonWidth, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && maxScroll() > 0) {
			int next = Math.max(0, Math.min(maxScroll(), scroll() + (scrollY > 0 ? -1 : 1)));
			if (next != scroll()) {
				SCROLL.put(tab, next);
				this.rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}


	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int left = tableLeft();
		int right = left + TABLE_WIDTH;

		// Row stripes behind the widgets
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			int y = TABLE_TOP + r * ROW_HEIGHT;
			graphics.fill(left, y, right, y + ROW_HEIGHT, r % 2 == 0 ? ROW_DARK : ROW_LIGHT);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Component title = this.title.copy().append(Component.translatable("gearbalance.config.changes",
				GearConfig.changeCount()).withStyle(ChatFormatting.GRAY));
		graphics.centeredText(this.font, title, this.width / 2, 8, WHITE);

		// Column headers, hover for an explanation
		Component firstColumn = Component.translatable(tab == Tab.MATERIALS || tab == Tab.SETS
				? "gearbalance.config.column.material" : "gearbalance.config.column.item");
		graphics.text(this.font, firstColumn, left + ICON_WIDTH * (tab == Tab.MATERIALS ? 2 : 1), HEADER_Y, GREY);
		for (int c = 0; c < headers.size(); c++) {
			String key = headers.get(c);
			if (key == null) {
				continue;
			}
			int x = columnX(c);
			graphics.text(this.font, Component.translatable(key + ".short"), x, HEADER_Y, GREY);
			if (mouseX >= x && mouseX < x + COLUMN_WIDTH && mouseY >= HEADER_Y - 2 && mouseY < HEADER_Y + 10) {
				// Wrapped into a box like widget tooltips, instead of one line across the screen
				List<FormattedCharSequence> lines = new ArrayList<>(
						this.font.split(Component.translatable(key), TOOLTIP_WIDTH));
				lines.addAll(this.font.split(Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY),
						TOOLTIP_WIDTH));
				graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
			}
		}
		graphics.horizontalLine(left, right - 1, TABLE_TOP - 2, 0xFF606060);

		// Icons and names
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			Row row = rows.get(scroll() + r);
			int y = TABLE_TOP + r * ROW_HEIGHT;
			drawItem(graphics, row.icon(), left + 2, y + 3);
			drawItem(graphics, row.icon2(), left + 2 + ICON_WIDTH, y + 3);
			// The materials tab has two icons per row, the name moves one icon to the right
			int nameLeft = left + ICON_WIDTH + (tab == Tab.MATERIALS ? ICON_WIDTH : 0);
			String name = row.name().getString();
			graphics.text(this.font, this.font.plainSubstrByWidth(name, NAME_WIDTH - 4 - (nameLeft - left - ICON_WIDTH)),
					nameLeft, y + (ROW_HEIGHT - 8) / 2, WHITE);
		}

		// Reset icons with a shadow like button text
		int iconSize = RESET_ICON.length * RESET_PIXEL;
		for (int[] pos : resetButtons) {
			int left0 = pos[0] + (RESET_WIDTH - iconSize) / 2;
			int top0 = pos[1] + (ROW_HEIGHT - 2 - iconSize) / 2;
			drawIcon(graphics, left0 + 1, top0 + 1, 0xFF3F3F3F);
			drawIcon(graphics, left0, top0, WHITE);
		}

		// Scrollbar
		if (maxScroll() > 0) {
			int trackHeight = visibleRows * ROW_HEIGHT;
			int barHeight = Math.max(10, trackHeight * visibleRows / rows.size());
			int barY = TABLE_TOP + (trackHeight - barHeight) * scroll() / maxScroll();
			graphics.fill(right + 2, TABLE_TOP, right + 5, TABLE_TOP + trackHeight, 0x40FFFFFF);
			graphics.fill(right + 2, barY, right + 5, barY + barHeight, 0xFFC0C0C0);
		}

		String note = switch (tab) {
			case MATERIALS -> "gearbalance.config.note_materials";
			case SETS -> GearConfig.setBonuses ? "gearbalance.config.note_sets" : "gearbalance.config.note_sets_off";
			default -> "gearbalance.config.note";
		};
		graphics.centeredText(this.font, Component.translatable(note), this.width / 2, this.height - 42,
				tab == Tab.SETS && !GearConfig.setBonuses ? RED : GREY);
	}

	private void drawItem(GuiGraphicsExtractor graphics, String id, int x, int y) {
		if (id == null) {
			return;
		}
		// Item components are not bound in the main menu, an ItemStack would crash there
		boolean bound = BuiltInRegistries.ITEM.get(Identifier.parse(id))
				.map(Holder.Reference::areComponentsBound).orElse(false);
		if (bound) {
			graphics.item(new ItemStack(item(id)), x, y);
		}
	}

	private static void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int row = 0; row < RESET_ICON.length; row++) {
			String line = RESET_ICON[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					int px = x + col * RESET_PIXEL;
					int py = y + row * RESET_PIXEL;
					graphics.fill(px, py, px + RESET_PIXEL, py + RESET_PIXEL, color);
				}
			}
		}
	}


	@Override
	public void onClose() {
		GearConfig.save();
		if (GearConfig.restartSignature().equals(startSignature)) {
			this.minecraft.setScreenAndShow(this.parent);
			return;
		}
		BooleanConsumer answer = quit -> {
			if (quit) {
				this.minecraft.stop();
			} else {
				this.minecraft.setScreenAndShow(this.parent);
			}
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("gearbalance.restart.title"),
				Component.translatable("gearbalance.restart.message"),
				Component.translatable("gearbalance.restart.quit"),
				Component.translatable("gearbalance.restart.later")));
	}

	/** 0 = any value, otherwise only multiples of step (1 = whole numbers, 0.1 = one decimal). */
	private static boolean fitsStep(double value, double step) {
		if (step <= 0) {
			return true;
		}
		double units = value / step;
		return Math.abs(units - Math.rint(units)) < 1.0E-6;
	}

	/** Accepts "1.6" and "1,6", null for invalid input. */
	static Double parse(String text) {
		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** 74 instead of 74.0, 1.6 instead of 1.6000000000000001. */
	static String format(double value) {
		double rounded = Math.round(value * 1000.0) / 1000.0;
		if (rounded == Math.rint(rounded)) {
			return String.valueOf((long) rounded);
		}
		return String.format(Locale.ROOT, "%.3f", rounded).replaceAll("0+$", "");
	}
}
