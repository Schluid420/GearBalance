package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OptionsScreen extends Screen {
	/** Which list of switches this screen shows. */
	private final boolean loot;
	private static final int WIDTH = 220;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW = 22;

	private final Screen parent;

	public OptionsScreen(Screen parent) {
		this(parent, false);
	}

	private OptionsScreen(Screen parent, boolean loot) {
		super(Component.translatable(loot ? "gearbalance.loot.title" : "gearbalance.options.title"));
		this.parent = parent;
		this.loot = loot;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - WIDTH / 2;
		int y = 30;
		if (loot) {
			numberRow(x, y, "zombie_leather", GearConfig.zombieLeather, v -> GearConfig.zombieLeather = v,
					GearConfig.zombieLeatherChance, 100, v -> GearConfig.zombieLeatherChance = v, "zombie_leather_chance");
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "loot_templates", GearConfig.lootTemplates, v -> GearConfig.lootTemplates = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "loot_materials", GearConfig.lootMaterials, v -> GearConfig.lootMaterials = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "loot_no_cheap", GearConfig.lootNoCheap, v -> GearConfig.lootNoCheap = v));
		} else {
			this.addRenderableWidget(toggle(x, y, "enabled", GearConfig.enabled, v -> GearConfig.enabled = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "set_bonuses", GearConfig.setBonuses, v -> GearConfig.setBonuses = v));
			y += ROW;
			// Loot settings live in their own screen, so this one still fits with a large GUI scale
			this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.options.loot"),
					b -> this.minecraft.setScreenAndShow(new OptionsScreen(this, true)))
					.bounds(x, y, WIDTH, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("gearbalance.options.loot.tip"))).build());
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "trident", GearConfig.tridentRecipe, v -> GearConfig.tridentRecipe = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "chainmail", GearConfig.chainmailRecipes, v -> GearConfig.chainmailRecipes = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "rabbit_recipe", GearConfig.rabbitHideRecipe, v -> GearConfig.rabbitHideRecipe = v));
			y += ROW;
			this.addRenderableWidget(toggle(x, y, "rabbit_armor", GearConfig.rabbitHideArmor, v -> GearConfig.rabbitHideArmor = v));
			y += ROW;
			numberRow(x, y, "anvil", GearConfig.anvilNoLimit, v -> GearConfig.anvilNoLimit = v,
					GearConfig.anvilMaxCost, GearConfig.ANVIL_MAX_COST_LIMIT, v -> GearConfig.anvilMaxCost = v, "anvil_max");
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(this.width / 2 - 100, this.height - 32, 200, BUTTON_HEIGHT).build());
	}

	/** Switch with a number box next to it, for example the anvil cost cap. */
	private void numberRow(int x, int y, String key, boolean current, Consumer<Boolean> setter,
			int number, int max, IntConsumer numberSetter, String numberKey) {
		int boxWidth = 40;
		CycleButton<Boolean> button = CycleButton.onOffBuilder(current)
				.create(x, y, WIDTH - boxWidth - 4, BUTTON_HEIGHT, Component.translatable("gearbalance.options." + key),
						(b, value) -> setter.accept(value));
		button.setTooltip(Tooltip.create(Component.translatable("gearbalance.options." + key + ".tip")));
		this.addRenderableWidget(button);

		EditBox box = new EditBox(this.font, x + WIDTH - boxWidth, y + 1, boxWidth, BUTTON_HEIGHT - 2,
				Component.translatable("gearbalance.options." + numberKey));
		box.setMaxLength(4);
		box.setValue(String.valueOf(number));
		box.setTooltip(Tooltip.create(Component.translatable("gearbalance.options." + numberKey + ".tip", max)));
		box.setResponder(text -> {
			try {
				int value = Integer.parseInt(text.trim());
				if (value >= 1 && value <= max) {
					numberSetter.accept(value);
					box.setTextColor(0xFFE0E0E0);
					return;
				}
			} catch (NumberFormatException ignored) {
			}
			box.setTextColor(0xFFFF5555);
		});
		this.addRenderableWidget(box);
	}

	private static CycleButton<Boolean> toggle(int x, int y, String key, boolean current, Consumer<Boolean> setter) {
		CycleButton<Boolean> button = CycleButton.onOffBuilder(current)
				.create(x, y, WIDTH, BUTTON_HEIGHT, Component.translatable("gearbalance.options." + key),
						(b, value) -> setter.accept(value));
		button.setTooltip(Tooltip.create(Component.translatable("gearbalance.options." + key + ".tip")));
		return button;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		if (!loot) {
			GearConfig.save();
		}
		this.minecraft.setScreenAndShow(this.parent);
	}
}
