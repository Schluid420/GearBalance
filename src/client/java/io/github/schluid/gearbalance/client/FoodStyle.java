package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/** Food bar in heart style (36 px), optionally with golden saturation. Same places and shaking as vanilla. */
public final class FoodStyle {
	private FoodStyle() {
	}

	private static final int SIZE = 9;
	private static final RandomSource RANDOM = RandomSource.create();

	public static boolean active() {
		return GearConfig.enabled && GearConfig.hdFood;
	}

	/** Draws the food bar instead of vanilla; top and right as vanilla gets them. */
	public static void extract(GuiGraphicsExtractor graphics, Player player, int top, int right) {
		FoodData food = player.getFoodData();
		int level = food.getFoodLevel();
		float saturation = food.getSaturationLevel();
		boolean hunger = player.hasEffect(MobEffects.HUNGER);
		// Saturation points shown in gold, counted from the right like the food
		int gold = GearConfig.saturationGold && !hunger ? Mth.ceil(Math.min(saturation, level)) : 0;
		for (int i = 0; i < 10; i++) {
			int y = top;
			if (saturation <= 0.0F && player.tickCount % (level * 3 + 1) == 0) {
				y += RANDOM.nextInt(3) - 1;
			}
			int x = right - i * 8 - 9;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("empty"), x, y, SIZE, SIZE);
			String kind = hunger ? "hunger_" : i * 2 < gold ? "gold_" : "";
			if (i * 2 + 1 < level) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(kind + "full"), x, y, SIZE, SIZE);
			} else if (i * 2 + 1 == level) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(kind + "half"), x, y, SIZE, SIZE);
			}
		}
	}

	private static Identifier sprite(String name) {
		return GearBalance.id("hud/food/" + name);
	}
}
