package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.bag.BagTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Bag tooltip: a bundle-style bar with "20/54 used"; green while there is room, red when full. */
final class ClientBagTooltip implements ClientTooltipComponent {
	private static final Identifier BORDER = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
	private static final int WIDTH = 72;
	private static final int HEIGHT = 11;
	private static final int MARGIN_Y = 2;
	private static final int FILL_MAX = WIDTH - 2;
	private static final int GREEN = 0xFF55C34B;
	private static final int GREEN_LIGHT = 0xFF86E07C;
	private static final int RED = 0xFFE04848;
	private static final int RED_LIGHT = 0xFFFF7A7A;

	private final BagTooltip bag;

	ClientBagTooltip(BagTooltip bag) {
		this.bag = bag;
	}

	@Override
	public int getHeight(Font font) {
		return HEIGHT + MARGIN_Y;
	}

	@Override
	public int getWidth(Font font) {
		return WIDTH;
	}

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
		int total = Math.max(1, bag.total());
		int used = Math.max(0, Math.min(total, bag.used()));
		boolean full = used >= total;
		int fill = used == 0 ? 0 : Math.max(1, FILL_MAX * used / total);
		int top = y + MARGIN_Y / 2;
		if (fill > 0) {
			graphics.fill(x + 1, top + 1, x + 1 + fill, top + HEIGHT - 1, full ? RED : GREEN);
			graphics.fill(x + 1, top + 1, x + 1 + fill, top + 2, full ? RED_LIGHT : GREEN_LIGHT);
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BORDER, x, top, WIDTH, HEIGHT);
		graphics.centeredText(font, Component.translatable("gearbalance.bag.used", used, total), x + WIDTH / 2, top + 2, -1);
	}
}
