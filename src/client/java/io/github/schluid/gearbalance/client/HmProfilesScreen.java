package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.config.HmPresets;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** HM+ profiles, one per HardmodePlus profile; the active one is green. Loading asks first, applies after restart. */
public class HmProfilesScreen extends TipScreen {
	private static final int ROW_HEIGHT = 24;
	private static final int TOP = 54;
	private static final int GREY = 0xFFA0A0A0;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;

	private final Screen parent;
	/** Settings screen; after loading we go back there so it can ask for the restart. */
	private final Screen home;

	private int rowWidth;
	private int buttonWidth;

	public HmProfilesScreen(Screen parent, Screen home) {
		super(Component.translatable("gearbalance.hm.title"));
		this.parent = parent;
		this.home = home;
	}

	private int left() {
		return this.width / 2 - rowWidth / 2;
	}

	@Override
	protected void init() {
		clearTips();
		rowWidth = Math.min(450, this.width - 20);
		buttonWidth = 60;
		int right = left() + rowWidth;

		HmPresets[] presets = HmPresets.values();
		for (int i = 0; i < presets.length; i++) {
			HmPresets preset = presets[i];
			String key = "gearbalance.hm." + preset.key;
			Component name = Component.translatable(key);
			this.addRenderableWidget(tip(Button.builder(Component.translatable("gearbalance.profiles.load"),
					b -> confirm(name, preset::apply))
					.bounds(right - buttonWidth, TOP + i * ROW_HEIGHT, buttonWidth, 20).build(),
					name, Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY)));
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
				.bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
	}

	/** Loading replaces the current values, so it always asks first. */
	private void confirm(Component name, Runnable load) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				load.run();
				GearConfig.save();
			}
			this.minecraft.setScreenAndShow(yes ? this.home : this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("gearbalance.load.confirm.title", name),
				Component.translatable("gearbalance.load.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, WHITE);
		drawFittedCentered(graphics, Component.translatable("gearbalance.hm.subtitle"), this.width / 2, 24,
				rowWidth, GREY);

		int left = left();
		int descriptionLeft = left + 90;
		int descriptionWidth = rowWidth - 90 - buttonWidth - 8;
		String active = GearConfig.activeHmProfile();

		HmPresets[] presets = HmPresets.values();
		List<Component> hovered = null;
		for (int i = 0; i < presets.length; i++) {
			int rowTop = TOP + i * ROW_HEIGHT;
			int y = rowTop + 6;
			String key = "gearbalance.hm." + presets[i].key;
			boolean isActive = presets[i].key.equals(active);
			graphics.text(this.font, Component.translatable(key), left, y, isActive ? GREEN : WHITE);
			Component description = isActive
					? Component.translatable("gearbalance.hm.active")
					: Component.translatable(key + ".short");
			drawFitted(graphics, description, descriptionLeft, y, descriptionWidth, isActive ? GREEN : GREY);
			if (mouseX >= left && mouseX < descriptionLeft + descriptionWidth && mouseY >= rowTop && mouseY < rowTop + 20) {
				hovered = List.of(Component.translatable(key),
						Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
			}
		}
		if (hovered != null) {
			showTip(graphics, hovered, mouseX, mouseY);
		} else {
			drawTips(graphics, mouseX, mouseY);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
