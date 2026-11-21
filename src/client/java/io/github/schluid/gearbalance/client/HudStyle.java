package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** HD hotbar, selection frame, off hand slot, experience bar and level number (4x); same size and places as vanilla. */
public final class HudStyle {
	private HudStyle() {
	}

	private static final Map<Identifier, Identifier> SWAP = Map.of(
			Identifier.withDefaultNamespace("hud/hotbar"), GearBalance.id("hud/hd/hotbar"),
			Identifier.withDefaultNamespace("hud/hotbar_selection"), GearBalance.id("hud/hd/hotbar_selection"),
			Identifier.withDefaultNamespace("hud/hotbar_offhand_left"), GearBalance.id("hud/hd/hotbar_offhand_left"),
			Identifier.withDefaultNamespace("hud/hotbar_offhand_right"), GearBalance.id("hud/hd/hotbar_offhand_right"),
			Identifier.withDefaultNamespace("hud/experience_bar_background"), GearBalance.id("hud/hd/experience_bar_background"),
			Identifier.withDefaultNamespace("hud/experience_bar_progress"), GearBalance.id("hud/hd/experience_bar_progress"));

	/** Width of one digit of the level number and the room each takes, in GUI pixels. */
	private static final int DIGIT_WIDTH = 7;
	private static final int DIGIT_HEIGHT = 10;
	private static final int DIGIT_STEP = 6;

	public static boolean active() {
		return GearConfig.enabled && GearConfig.hdHud;
	}

	/** The HD sprite for a vanilla hotbar or experience sprite; anything else stays. */
	public static Identifier swap(Identifier sprite) {
		if (!active()) {
			return sprite;
		}
		return SWAP.getOrDefault(sprite, sprite);
	}

	/** The level number in HD digits, where vanilla puts it. */
	public static void level(GuiGraphicsExtractor graphics, int level) {
		String text = Integer.toString(level);
		int width = text.length() * DIGIT_STEP + 1;
		int x = (graphics.guiWidth() - width) / 2;
		int y = graphics.guiHeight() - 24 - 9 - 2 - 3;
		for (int i = 0; i < text.length(); i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GearBalance.id("hud/hd/level_" + text.charAt(i)),
					x + i * DIGIT_STEP, y, DIGIT_WIDTH, DIGIT_HEIGHT);
		}
	}
}
