package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.client.mixin.AbstractContainerScreenAccessor;
import io.github.schluid.gearbalance.comfort.Loadouts;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Gear sets: three buttons left of the window; click puts on, Shift + click saves, Ctrl + click deletes. Hidden with recipe book. */
final class LoadoutButtons {
	private LoadoutButtons() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int SIZE = 13;
	private static final int GAP = 2;
	private static final int ICON_SIZE = 9;
	private static final Identifier ARMOR_ICON = Identifier.withDefaultNamespace("hud/armor_full");
	private static final long MESSAGE_MS = 3000;

	/** Message shown above the window, with the time it ends. */
	private static Component notice;
	private static long noticeEnd;

	static void show(Component text) {
		notice = text;
		noticeEnd = System.currentTimeMillis() + MESSAGE_MS;
	}

	private static int color(int set) {
		return 0xFF000000 | Loadouts.rgb(set);
	}

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			for (int set = 0; set < Loadouts.SETS; set++) {
				SetButton button = new SetButton(set, screen);
				Screens.getWidgets(screen).add(button);
				place(screen, button);
				ScreenEvents.afterTick(screen).register(s -> place(s, button));
			}
		});
	}

	/** Space the buttons take left of the inventory (for JEI), null while they are hidden. */
	static Rect2i area(Screen screen) {
		if (!(screen instanceof InventoryScreen) || !GearConfig.loadoutsActive()) {
			return null;
		}
		return new Rect2i(guiLeft(screen) - SIZE - GAP, guiTop(screen) + 3, SIZE, Loadouts.SETS * (SIZE + GAP));
	}

	private static int guiLeft(Screen screen) {
		return screen instanceof AbstractContainerScreenAccessor window ? window.gearbalance$leftPos()
				: (screen.width - GUI_WIDTH) / 2;
	}

	private static int guiTop(Screen screen) {
		return screen instanceof AbstractContainerScreenAccessor window ? window.gearbalance$topPos()
				: (screen.height - GUI_HEIGHT) / 2;
	}

	private static void place(Screen screen, SetButton button) {
		int left = guiLeft(screen);
		button.setX(left - SIZE - GAP);
		button.setY(guiTop(screen) + 3 + button.set * (SIZE + GAP));
		// The recipe book moves the window to the right and would lie under the buttons
		boolean recipeBook = left > (screen.width - GUI_WIDTH) / 2 + SIZE;
		button.visible = GearConfig.loadoutsActive() && !recipeBook;
		button.updateTooltip();
	}

	private static final class SetButton extends Button.Plain {
		final int set;
		private final Screen screen;
		private String shown = "";

		SetButton(int set, Screen screen) {
			super(0, 0, SIZE, SIZE, Component.translatable("gearbalance.loadout.button", set + 1),
					b -> ClientPlayNetworking.send(new Loadouts.UsePayload(set, mode())),
					DEFAULT_NARRATION);
			this.set = set;
			this.screen = screen;
		}

		private static int mode() {
			Minecraft minecraft = Minecraft.getInstance();
			return minecraft.hasControlDown() ? Loadouts.MODE_DELETE
					: minecraft.hasShiftDown() ? Loadouts.MODE_SAVE : Loadouts.MODE_LOAD;
		}

		/** Name, use and the saved pieces; built again only when the set changed. */
		void updateTooltip() {
			List<ItemStack> pieces = set < ComfortClient.loadouts.size() ? ComfortClient.loadouts.get(set) : List.of();
			MutableComponent text = Component.translatable("gearbalance.loadout.button", set + 1)
					.withStyle(Loadouts.color(set));
			text.append("\n").append(Component.translatable("gearbalance.loadout.button.tip").withStyle(ChatFormatting.GRAY));
			boolean any = false;
			for (ItemStack piece : pieces) {
				if (!piece.isEmpty()) {
					text.append("\n").append(Component.literal("• ").append(piece.getHoverName()).withStyle(ChatFormatting.WHITE));
					any = true;
				}
			}
			if (!any) {
				text.append("\n").append(Component.translatable("gearbalance.loadout.button.empty")
						.withStyle(ChatFormatting.DARK_GRAY));
			}
			String key = text.getString();
			if (!key.equals(shown)) {
				shown = key;
				setTooltip(Tooltip.create(text));
			}
		}

		/** Message centered above the window; drawn by the last button so it lies in front. */
		private void extractMessage(GuiGraphicsExtractor graphics) {
			if (notice == null || System.currentTimeMillis() > noticeEnd) {
				return;
			}
			var font = Minecraft.getInstance().font;
			int x = guiLeft(screen) + GUI_WIDTH / 2 - font.width(notice) / 2;
			graphics.text(font, notice, x, Math.max(2, guiTop(screen) - 12), 0xFFFFFFFF, true);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			extractDefaultSprite(graphics);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_ICON, getX() + 1, getY() + 2, ICON_SIZE, ICON_SIZE);
			graphics.pose().pushMatrix();
			graphics.pose().translate(getX() + 8.5F, getY() + 6.5F);
			graphics.pose().scale(0.75F, 0.75F);
			graphics.text(Minecraft.getInstance().font, Component.literal(String.valueOf(set + 1)), 0, 0, color(set), true);
			graphics.pose().popMatrix();
			if (set == Loadouts.SETS - 1) {
				extractMessage(graphics);
			}
		}
	}
}
