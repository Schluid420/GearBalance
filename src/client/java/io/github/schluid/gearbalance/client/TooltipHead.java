package io.github.schluid.gearbalance.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * First line of a framed tooltip. Gear: centered name plus item box and a line below (upgrade or material color).
 * Other items: just the centered name.
 */
public final class TooltipHead implements ClientTooltipComponent {
	private static final int BOX = 20;
	private static final int GAP = 4;
	/** Room between box and line, the same as between frame and box. */
	private static final int BELOW = 4;
	/** Room under the line: with vanilla's 2 px it equals an empty line (EMPTY_LINE), so sockets sit centered. */
	private static final int AFTER_LINE = 4;

	/** An empty line in a framed tooltip: 4 pixels, with the 2 pixels vanilla puts after the head 6. */
	public static final ClientTooltipComponent EMPTY_LINE = new ClientTooltipComponent() {
		@Override
		public int getHeight(Font font) {
			return 4;
		}

		@Override
		public int getWidth(Font font) {
			return 0;
		}
	};

	private final @Nullable ItemStack stack;
	private final FormattedCharSequence name;
	private final int color;

	/** stack null = plain head (name only). */
	public TooltipHead(@Nullable ItemStack stack, FormattedCharSequence name, int color) {
		this.stack = stack;
		this.name = stack == null || ComfortClient.nameColor(stack) != null ? name : recolor(name, color);
		this.color = color;
	}

	/** The name in one color: the frame decides the color, not the rarity or the upgrade. */
	private static FormattedCharSequence recolor(FormattedCharSequence name, int color) {
		StringBuilder text = new StringBuilder();
		name.accept((index, style, codePoint) -> {
			text.appendCodePoint(codePoint);
			return true;
		});
		return Component.literal(text.toString()).withColor(color).getVisualOrderText();
	}

	@Override
	public int getHeight(Font font) {
		return stack == null ? 10 : BOX + BELOW + 1 + AFTER_LINE;
	}

	@Override
	public int getWidth(Font font) {
		return stack == null ? font.width(name) : BOX + GAP + font.width(name) + GAP;
	}

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
		if (stack == null) {
			graphics.text(font, name, x + (width - font.width(name)) / 2, y, 0xFFFFFFFF, true);
			return;
		}
		int dark = 0xFF000000 | scale(color, 0.55);
		int nameWidth = font.width(name);
		// Name and box side by side, the pair in the middle of the tooltip
		int left = x + (width - (nameWidth + GAP + BOX)) / 2;
		// Capitals are 7 pixels high: centered on the box
		graphics.text(font, name, left, y + (BOX - 7) / 2, 0xFFFFFFFF, true);
		int boxX = left + nameWidth + GAP;
		graphics.outline(boxX, y, BOX, BOX, dark);
		graphics.item(stack, boxX + 2, y + 2);
		graphics.fill(x, y + BOX + BELOW, x + width, y + BOX + BELOW + 1, 0xFF000000 | scale(color, 0.75));
	}

	private static int scale(int rgb, double factor) {
		int r = (int) (((rgb >> 16) & 0xFF) * factor);
		int g = (int) (((rgb >> 8) & 0xFF) * factor);
		int b = (int) ((rgb & 0xFF) * factor);
		return (r << 16) | (g << 8) | b;
	}
}
