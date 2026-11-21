package io.github.schluid.gearbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** Vanilla button with smaller text for container screens; too wide text scrolls back and forth. */
final class SmallButton extends Button.Plain {
	private static final int PADDING = 3;
	private final float scale;

	SmallButton(int x, int y, int width, int height, Component message, OnPress onPress, float scale) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.scale = scale;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractDefaultSprite(graphics);
		Font font = Minecraft.getInstance().font;
		Component text = getMessage();
		float textWidth = font.width(text) * scale;
		float room = getWidth() - 2 * PADDING;
		float left = getX() + (getWidth() - textWidth) / 2;
		boolean scrolls = textWidth > room;
		if (scrolls) {
			// Back and forth over the overflow, about 1 pixel per 50 ms with a pause at each end
			float overflow = textWidth - room;
			double seconds = Util.getMillis() / 1000.0;
			double period = Math.max(overflow / 20.0, 1.0) + 2.0;
			double phase = (Math.sin(Math.PI * 2 * seconds / (period * 2)) + 1) / 2;
			left = getX() + PADDING - (float) (Math.max(0, Math.min(1, phase * 1.4 - 0.2)) * overflow);
			graphics.enableScissor(getX() + PADDING, getY(), getX() + getWidth() - PADDING, getY() + getHeight());
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, getY() + (getHeight() - 8 * scale) / 2 + 0.5F);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, 0, 0, this.active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
		graphics.pose().popMatrix();
		if (scrolls) {
			graphics.disableScissor();
		}
	}
}
