package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.bag.Bags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Inventory with a bag: bag slot next to the player, bag rows panel above; the window moves down half the panel. */
public final class BagPanel {
	private BagPanel() {
	}

	private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
	/** How far the panel sticks out on each side of the 176 wide window: one slot per extra column pair. */
	private static int out() {
		return (Bags.shownColumns() - 9) / 2 * 18;
	}

	private static int width() {
		return 176 + 2 * out();
	}
	private static final int BACKGROUND = 0xFFC6C6C6, BORDER = 0xFF000000, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555;

	/** Rows the window was laid out for; a change lays it out again. */
	public static int layoutRows;

	/** Closed with a right click on the bag: the rows are hidden, the bag stays worn. */
	public static boolean closed;

	/** Rows of the worn bag right now (0 while it is closed). */
	public static int rows() {
		Player player = Minecraft.getInstance().player;
		return player == null || closed ? 0 : Bags.shownRows(player);
	}

	/** Height of the panel with border and gap, 0 without a bag. */
	private static int height(int rows) {
		return rows == 0 ? 0 : rows * 18 + 10;
	}

	/** How far the window moves down. */
	public static int shift(int rows) {
		return height(rows) / 2;
	}

	/** The point lies on the panel (clicks there don't drop the held item). */
	public static boolean inside(int left, int top, double mouseX, double mouseY) {
		int rows = rows();
		return rows > 0 && mouseX >= left - out() && mouseX < left - out() + width() && mouseY >= top - height(rows) && mouseY < top;
	}

	/** Draws the bag slot and the panel; called after the inventory background. */
	public static void draw(GuiGraphicsExtractor graphics, int left, int top) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (Bags.active() || !Bags.worn(player).isEmpty()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, left + BagSlots.BAG_X - 1, top + BagSlots.BAG_Y - 1, 18, 18);
		}
		int rows = rows();
		if (rows == 0) {
			return;
		}
		int h = rows * 18 + 8;
		int x0 = left - out(), x1 = left - out() + width(), y0 = top - 2 - h, y1 = top - 2;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, BORDER);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, BORDER);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, BACKGROUND);
		graphics.fill(x0 + 1, y0 + 1, x1 - 3, y0 + 3, LIGHT);
		graphics.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 3, LIGHT);
		graphics.fill(x0 + 3, y1 - 3, x1 - 1, y1 - 1, SHADOW);
		graphics.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
		for (int row = 0; row < rows; row++) {
			for (int column = Bags.firstColumn(); column < Bags.firstColumn() + Bags.shownColumns(); column++) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, left + BagSlots.columnX(column) - 1, top + BagSlots.rowY(row) - 1, 18, 18);
			}
		}
	}
}
