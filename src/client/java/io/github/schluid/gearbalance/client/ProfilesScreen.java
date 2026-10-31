package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.config.Presets;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Three ready-made presets at the top, below them the three profiles the player saves himself.
 * Everything that is loaded applies after a restart, the settings screen asks for it when closed.
 */
public class ProfilesScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int GAP = 4;
	private static final int PRESET_HEADER = 42;
	private static final int PRESET_TOP = 54;
	private static final int GREY = 0xFFA0A0A0;
	private static final int WHITE = 0xFFFFFFFF;

	private final Screen parent;

	// Sized in init() so the rows also fit on small screens or with a large GUI scale.
	private int rowWidth;
	private int nameWidth;
	private int infoWidth;
	private int buttonWidth;

	public ProfilesScreen(Screen parent) {
		super(Component.translatable("gearbalance.profiles.title"));
		this.parent = parent;
	}

	private int left() {
		return this.width / 2 - rowWidth / 2;
	}

	private int profileHeader() {
		return PRESET_TOP + Presets.values().length * ROW_HEIGHT + 4;
	}

	private int profileTop() {
		return profileHeader() + 12;
	}

	private void layout() {
		rowWidth = Math.min(450, this.width - 20);
		boolean narrow = rowWidth < 450;
		int infoText = this.font.width(Component.translatable("gearbalance.profiles.count", 999)) + 8;
		infoWidth = narrow ? infoText : Math.max(90, infoText);
		buttonWidth = Math.clamp((rowWidth - 160) / 3 - GAP, 54, 70);
		nameWidth = rowWidth - infoWidth - 3 * (buttonWidth + GAP);
	}

	@Override
	protected void init() {
		layout();
		int left = left();
		int right = left + rowWidth;

		// Presets: name and description on the left, one load button on the right
		Presets[] presets = Presets.values();
		for (int i = 0; i < presets.length; i++) {
			Presets preset = presets[i];
			int y = PRESET_TOP + i * ROW_HEIGHT;
			Component name = Component.translatable("gearbalance.preset." + preset.key);
			this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.profiles.load"),
					b -> confirm(name, preset::apply))
					.bounds(right - buttonWidth, y, buttonWidth, 20)
					.tooltip(Tooltip.create(Component.translatable("gearbalance.preset." + preset.key + ".tip")))
					.build());
		}

		// Own profiles
		for (int i = 0; i < GearConfig.PROFILE_COUNT; i++) {
			int slot = i;
			int y = profileTop() + i * ROW_HEIGHT;
			GearConfig.Profile profile = GearConfig.profile(slot);

			EditBox name = new EditBox(this.font, left, y, nameWidth - 6, 20,
					Component.translatable("gearbalance.profiles.name"));
			name.setMaxLength(24);
			name.setValue(profile.name);
			name.setHint(Component.translatable("gearbalance.profiles.default", slot + 1));
			name.setResponder(text -> GearConfig.profile(slot).name = text);
			this.addRenderableWidget(name);

			int bx = left + nameWidth + infoWidth;
			Component profileName = profile.name.isBlank()
					? Component.translatable("gearbalance.profiles.default", slot + 1)
					: Component.literal(profile.name);
			Runnable saveAction = () -> {
				GearConfig.saveToProfile(slot);
				GearConfig.save();
			};
			// An empty slot can be filled right away, a used one is only overwritten after asking
			this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.profiles.save"), b -> {
				if (profile.isEmpty()) {
					saveAction.run();
					this.rebuildWidgets();
				} else {
					ask(Component.translatable("gearbalance.profiles.save.confirm.title", profileName),
							Component.translatable("gearbalance.profiles.save.confirm.message"), saveAction);
				}
			}).bounds(bx, y, buttonWidth, 20)
					.tooltip(Tooltip.create(Component.translatable("gearbalance.profiles.save.tip"))).build());

			Button load = Button.builder(Component.translatable("gearbalance.profiles.load"),
					b -> confirm(profileName, () -> GearConfig.loadProfile(slot))).bounds(bx + buttonWidth + GAP, y, buttonWidth, 20)
					.tooltip(Tooltip.create(Component.translatable("gearbalance.profiles.load.tip"))).build();
			load.active = !profile.isEmpty();
			this.addRenderableWidget(load);

			Button clear = Button.builder(Component.translatable("gearbalance.profiles.clear"),
					b -> ask(Component.translatable("gearbalance.profiles.clear.confirm.title", profileName),
							Component.translatable("gearbalance.profiles.clear.confirm.message"), () -> {
								GearConfig.clearProfile(slot);
								GearConfig.save();
							})).bounds(bx + 2 * (buttonWidth + GAP), y, buttonWidth, 20).build();
			clear.active = !profile.isEmpty();
			this.addRenderableWidget(clear);
		}

		// Export, import and back in one row at the bottom
		int bottomWidth = 100;
		int bottomLeft = this.width / 2 - (3 * bottomWidth + 2 * GAP) / 2;
		int by = this.height - 26;
		this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.export"), b -> export())
				.bounds(bottomLeft, by, bottomWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("gearbalance.export.tip"))).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.import"),
				b -> this.minecraft.setScreenAndShow(new ImportScreen(this, this.parent)))
				.bounds(bottomLeft + bottomWidth + GAP, by, bottomWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("gearbalance.import.tip"))).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
				.bounds(bottomLeft + 2 * (bottomWidth + GAP), by, bottomWidth, 20).build());
	}

	/** Saves the current setup as a file and says where it went. */
	private void export() {
		GearConfig.save();
		String file = GearConfig.exportCurrent();
		Component title = Component.translatable(file != null ? "gearbalance.export.done" : "gearbalance.export.failed");
		Component message = Component.literal(file != null ? "config/gearbalance/profiles/" + file : "");
		SystemToast.add(this.minecraft.gui.toastManager(), EXPORT_TOAST, title, message);
	}

	private static final SystemToast.SystemToastId EXPORT_TOAST = new SystemToast.SystemToastId(5000L);

	/** Yes/No before anything that can't be undone. Comes back to this screen either way. */
	private void ask(Component title, Component message, Runnable action) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				action.run();
			}
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer, title, message,
				Component.translatable("gui.yes"), Component.translatable("gui.no")));
	}

	/** Loading replaces the current values, so it always asks first. */
	private void confirm(Component name, Runnable load) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				load.run();
				GearConfig.save();
			}
			this.minecraft.setScreenAndShow(yes ? this.parent : this);
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
		graphics.centeredText(this.font, Component.translatable("gearbalance.profiles.current", GearConfig.changeCount()),
				this.width / 2, 24, GREY);

		int left = left();
		int descriptionLeft = left + 90;
		int descriptionWidth = rowWidth - 90 - buttonWidth - 8;

		graphics.text(this.font, Component.translatable("gearbalance.preset.header").withStyle(ChatFormatting.UNDERLINE),
				left, PRESET_HEADER, GREY);
		Presets[] presets = Presets.values();
		for (int i = 0; i < presets.length; i++) {
			int y = PRESET_TOP + i * ROW_HEIGHT + 6;
			String key = "gearbalance.preset." + presets[i].key;
			graphics.text(this.font, Component.translatable(key), left, y, WHITE);
			Component description = Component.translatable(key + ".short");
			graphics.text(this.font, this.font.plainSubstrByWidth(description.getString(), descriptionWidth),
					descriptionLeft, y, GREY);
		}

		graphics.text(this.font, Component.translatable("gearbalance.profiles.header").withStyle(ChatFormatting.UNDERLINE),
				left, profileHeader(), GREY);
		for (int i = 0; i < GearConfig.PROFILE_COUNT; i++) {
			GearConfig.Profile profile = GearConfig.profile(i);
			Component info = profile.isEmpty()
					? Component.translatable("gearbalance.profiles.empty")
					: Component.translatable("gearbalance.profiles.count", profile.changeCount());
			graphics.text(this.font, info, left + nameWidth, profileTop() + i * ROW_HEIGHT + 6,
					profile.isEmpty() ? 0xFF808080 : 0xFFE0E0E0);
		}
	}

	@Override
	public void onClose() {
		GearConfig.save();
		this.minecraft.setScreenAndShow(this.parent);
	}
}
