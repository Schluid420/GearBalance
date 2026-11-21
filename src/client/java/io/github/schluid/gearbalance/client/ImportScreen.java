package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import com.mojang.blaze3d.Blaze3D;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists config/gearbalance/profiles/, newest first. Picking one asks first, then replaces the setup. */
public class ImportScreen extends TipScreen {
	private static final int TOP = 44;
	private static final int ROW = 24;
	private static final int WIDTH = 300;
	private static final int GREY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;

	private final Screen parent;
	/** Where to go after a successful import: the settings screen, which then asks for the restart. */
	private final Screen afterImport;
	private List<String> files = List.of();
	private int scroll;
	/** Shown in red at the bottom after something went wrong, null otherwise. */
	private Component error;

	public ImportScreen(Screen parent, Screen afterImport) {
		super(Component.translatable("gearbalance.import.title"));
		this.parent = parent;
		this.afterImport = afterImport;
	}

	private int visibleRows() {
		return Math.max(1, (this.height - TOP - 50) / ROW);
	}

	@Override
	protected void init() {
		clearTips();
		files = GearConfig.listExports();
		scroll = Math.max(0, Math.min(scroll, files.size() - visibleRows()));
		int x = this.width / 2 - WIDTH / 2;
		int deleteWidth = 50;
		for (int i = 0; i < visibleRows() && scroll + i < files.size(); i++) {
			String file = files.get(scroll + i);
			int y = TOP + i * ROW;
			this.addRenderableWidget(tip(Button.builder(Component.literal(file), b -> confirm(file))
					.bounds(x, y, WIDTH - deleteWidth - 4, 20).build(),
					Component.literal(file),
					Component.translatable("gearbalance.import.file.tip").withStyle(ChatFormatting.GRAY)));
			this.addRenderableWidget(Button.builder(Component.translatable("gearbalance.import.delete"), b -> confirmDelete(file))
					.bounds(x + WIDTH - deleteWidth, y, deleteWidth, 20).build());
		}

		int bottomWidth = 150;
		int bottomLeft = this.width / 2 - (2 * bottomWidth + 4) / 2;
		// Opens the folder in the file explorer, like the button for resource packs does
		this.addRenderableWidget(tip(Button.builder(Component.translatable("gearbalance.import.folder"), b -> {
			try {
				Files.createDirectories(GearConfig.exportFolder());
			} catch (IOException ignored) {
			}
			Blaze3D.openPath(GearConfig.exportFolder());
		}).bounds(bottomLeft, this.height - 26, bottomWidth, 20).build(),
				Component.translatable("gearbalance.import.folder"),
				Component.literal(GearConfig.exportFolder().toString()).withStyle(ChatFormatting.GRAY)));
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
				.bounds(bottomLeft + bottomWidth + 4, this.height - 26, bottomWidth, 20).build());
	}

	private void confirm(String file) {
		BooleanConsumer answer = yes -> {
			if (yes && GearConfig.importFile(file)) {
				GearConfig.save();
				this.minecraft.setScreenAndShow(afterImport);
				return;
			}
			error = yes ? Component.translatable("gearbalance.import.failed") : null;
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("gearbalance.load.confirm.title", file),
				Component.translatable("gearbalance.load.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	private void confirmDelete(String file) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				error = GearConfig.deleteExport(file) ? null : Component.translatable("gearbalance.import.delete.failed");
			}
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("gearbalance.import.delete.confirm.title", file),
				Component.translatable("gearbalance.import.delete.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int max = Math.max(0, files.size() - visibleRows());
		int next = Math.max(0, Math.min(max, scroll + (scrollY > 0 ? -1 : 1)));
		if (scrollY != 0 && next != scroll) {
			scroll = next;
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("config/gearbalance/profiles/"), this.width / 2, 26, GREY);
		if (files.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("gearbalance.import.empty"), this.width / 2, TOP + 6, GREY);
		}
		if (error != null) {
			drawFittedCentered(graphics, error, this.width / 2, this.height - 40, this.width - 20, RED);
		}
		drawTips(graphics, mouseX, mouseY);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
