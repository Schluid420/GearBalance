package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Hearts in armor bar style (36 px); hearts from gear get a silver ring. Only the pictures are swapped. */
public final class HeartStyle {
	private HeartStyle() {
	}

	private static final int SIZE = 9;

	/** Heart being drawn (counted down like vanilla), and the range of hearts from gear. */
	private static int next;
	private static int current;
	private static int gearFrom;
	private static int gearTo;

	public static boolean active() {
		return GearConfig.enabled && GearConfig.hdHearts;
	}

	/** Start of the hearts: how many there are and which ones come from gear. */
	public static void start(Player player, float maxHealth, int absorption) {
		int hearts = Mth.ceil(maxHealth / 2.0);
		next = hearts + Mth.ceil(absorption / 2.0);
		gearTo = hearts;
		gearFrom = hearts;
		if (!GearConfig.heartRing) {
			return;
		}
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}
		double extra = 0;
		for (AttributeModifier modifier : health.getModifiers()) {
			// Potion effects (health boost) are not gear
			if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE && modifier.amount() > 0
					&& !modifier.id().getPath().startsWith("effect.")) {
				extra += modifier.amount();
			}
		}
		gearFrom = Math.max(0, Mth.ceil((maxHealth - extra) / 2.0));
	}

	/** Draws one heart picture instead of vanilla. type is the vanilla heart type name. */
	public static void heart(GuiGraphicsExtractor graphics, String type, int x, int y, boolean hardcore, boolean blinking,
			boolean half) {
		boolean container = "CONTAINER".equals(type);
		if (container) {
			current = --next;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(blinking ? "container_blink" : "container"), x, y, SIZE,
					SIZE);
		} else {
			String name = switch (type) {
				case "POISIONED" -> "poisoned";
				default -> type.toLowerCase(Locale.ROOT);
			};
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(name + (half ? "_half" : "_full")), x, y, SIZE, SIZE);
			if (blinking) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("blink"), x, y, SIZE, SIZE);
			}
		}
		if (hardcore) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("hardcore"), x, y, SIZE, SIZE);
		}
		if (current >= gearFrom && current < gearTo) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("ring"), x, y, SIZE, SIZE);
		}
	}

	private static Identifier sprite(String name) {
		return GearBalance.id("hud/heart/" + name);
	}
}
