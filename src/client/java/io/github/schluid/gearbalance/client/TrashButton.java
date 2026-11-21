package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.client.mixin.AbstractContainerScreenAccessor;
import io.github.schluid.gearbalance.comfort.Trash;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Look and tooltip of the trash bin; clicks go to the slot underneath (Trash.TrashSlot). */
final class TrashButton {
	private TrashButton() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int SIZE = 13;
	private static final int ICON_SIZE = 9;
	private static final Identifier BACKGROUND = GearBalance.id("trash_button");
	private static final Identifier ICON = GearBalance.id("trash");

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			BinPicture picture = new BinPicture();
			Screens.getWidgets(screen).add(picture);
			place(screen, picture);
			ScreenEvents.afterTick(screen).register(s -> place(s, picture));
		});
	}

	/** Over the bin slot, which sits left of the stats button. */
	private static void place(Screen screen, BinPicture picture) {
		int left = screen instanceof AbstractContainerScreenAccessor window ? window.gearbalance$leftPos()
				: (screen.width - GUI_WIDTH) / 2;
		int top = screen instanceof AbstractContainerScreenAccessor window ? window.gearbalance$topPos()
				: (screen.height - GUI_HEIGHT) / 2;
		picture.setX(left + Trash.SLOT_X + 1);
		picture.setY(top + Trash.SLOT_Y + 2);
		picture.visible = GearConfig.trashActive();
		picture.updateTooltip();
	}

	private static final class BinPicture extends Button.Plain {
		private String shown = "";

		BinPicture() {
			super(0, 0, SIZE, SIZE, Component.translatable("gearbalance.trash.button"), b -> {
			}, DEFAULT_NARRATION);
		}

		/** The slot below takes the click. */
		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			return false;
		}

		/** Name only; hidden while an item lies in the bin, so its own tooltip shows. */
		void updateTooltip() {
			Minecraft minecraft = Minecraft.getInstance();
			boolean full = minecraft.player != null && !minecraft.player.getAttachedOrElse(Trash.ITEM, ItemStack.EMPTY).isEmpty();
			String key = full ? "full" : "empty";
			if (!key.equals(shown)) {
				shown = key;
				setTooltip(full ? null : Tooltip.create(Component.translatable("gearbalance.trash.button")
						.withStyle(ChatFormatting.YELLOW)));
			}
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, getX(), getY(), SIZE, SIZE);
			if (isHovered()) {
				graphics.outline(getX(), getY(), SIZE, SIZE, 0xFFFFFFFF);
			}
			int offset = (SIZE - ICON_SIZE) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON, getX() + offset, getY() + offset, ICON_SIZE, ICON_SIZE);
		}
	}
}
