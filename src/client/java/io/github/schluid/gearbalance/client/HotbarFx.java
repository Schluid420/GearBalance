package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Hotbar animations, each with its own switch: item pop, red glow on low durability, bouncing item name,
 * smooth experience bar (light on gain, sparks on level up).
 */
public final class HotbarFx {
	private HotbarFx() {
	}

	/** Damped spring: how fast it settles and how fast it swings. */
	private static final float DAMPING = 0.35F;
	private static final float SWING = 0.9F;
	private static final float NAME_BOUNCE = 0.25F;
	/** Part of the way the shown experience moves per tick. */
	private static final float XP_SPEED = 0.25F;
	private static final float GLOW_FADE = 0.06F;
	private static final int SPARKS = 40;
	private static final int SPARK_TICKS = 22;
	private static final int BAR_WIDTH = 182;
	private static final int BAR_HEIGHT = 5;

	private static long ticks;
	private static int selected = -1;
	private static long selectedAt = -100;
	private static ItemStack held = ItemStack.EMPTY;
	private static long nameAt = -100;

	private static float xpShown, xpShownBefore;
	private static int lastLevel = -1;
	private static float lastProgress;
	private static float glow, glowBefore;
	private static final List<Spark> SPARK_LIST = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();

	private static final class Spark {
		float x, y, vx, vy;
		int age;
		final int color;

		Spark(float x, float y, float vx, float vy, int color) {
			this.x = x;
			this.y = y;
			this.vx = vx;
			this.vy = vy;
			this.color = color;
		}
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(HotbarFx::tick);
	}

	private static boolean on(boolean option) {
		return GearConfig.enabled && option;
	}

	private static void tick(Minecraft minecraft) {
		ticks++;
		LocalPlayer player = minecraft.player;
		if (player == null) {
			lastLevel = -1;
			SPARK_LIST.clear();
			return;
		}
		int slot = player.getInventory().getSelectedSlot();
		if (slot != selected) {
			selected = slot;
			selectedAt = ticks;
		}
		ItemStack main = player.getMainHandItem();
		if (!ItemStack.isSameItemSameComponents(main, held)) {
			if (!main.isEmpty()) {
				nameAt = ticks;
			}
			held = main.copy();
		}
		tickExperience(player);
	}

	private static void tickExperience(LocalPlayer player) {
		xpShownBefore = xpShown;
		glowBefore = glow;
		float target = player.experienceProgress;
		int level = player.experienceLevel;
		if (lastLevel < 0) {
			xpShown = xpShownBefore = target;
		} else if (level > lastLevel) {
			// Level up: start the bar again from the left, sparks along the bar
			xpShown = xpShownBefore = 0;
			glow = 1;
			if (on(GearConfig.xpAnimated)) {
				spawnSparks();
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.5F));
			}
		} else if (level < lastLevel) {
			xpShown = xpShownBefore = target;
		} else if (target > lastProgress + 1.0E-4F) {
			glow = Math.min(1, glow + 0.5F);
		}
		xpShown += (target - xpShown) * XP_SPEED;
		glow = Math.max(0, glow - GLOW_FADE);
		lastLevel = level;
		lastProgress = target;
		for (Spark spark : SPARK_LIST) {
			spark.x += spark.vx;
			spark.y += spark.vy;
			spark.vy += 0.08F;
			spark.age++;
		}
		SPARK_LIST.removeIf(spark -> spark.age > SPARK_TICKS);
	}

	private static void spawnSparks() {
		for (int i = 0; i < SPARKS; i++) {
			float x = RANDOM.nextFloat() * BAR_WIDTH;
			int color = switch (RANDOM.nextInt(3)) {
				case 0 -> 0xFFFFF27A;
				case 1 -> 0xFF9CFF6A;
				default -> 0xFFFFFFFF;
			};
			SPARK_LIST.add(new Spark(x, 2, (RANDOM.nextFloat() - 0.5F) * 1.8F, -1.0F - RANDOM.nextFloat() * 2.2F, color));
		}
	}

	private static float spring(long since, float partial) {
		float t = Math.max(0, ticks - since - 1 + partial);
		return (float) (Math.exp(-DAMPING * t) * Math.cos(SWING * t));
	}

	// ---------------------------------------------------------------- hotbar slots

	/** Size of a hotbar item: the selected one is a bit bigger and pops when you switch to it. */
	public static float slotScale(int slot, float partial) {
		if (!on(GearConfig.hotbarPop) || slot != selected) {
			return 1.0F;
		}
		float rest = 1.0F + GearConfig.hotbarPopPercent / 200.0F;
		float peak = 1.0F + GearConfig.hotbarPopPercent / 100.0F;
		return rest + (peak - rest) * spring(selectedAt, partial);
	}

	/** A soft red light behind a slot whose item is low on durability. */
	public static void lowDurabilityGlow(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack) {
		if (!on(GearConfig.hotbarLowGlow) || stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
			return;
		}
		double left = (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage();
		if (left > GearConfig.hotbarGlowPercent / 100.0) {
			return;
		}
		float pulse = 0.5F + 0.5F * Mth.sin(ticks * 0.25F);
		int alpha = (int) (60 + 90 * pulse);
		graphics.fill(x - 1, y - 1, x + 17, y + 17, alpha << 24 | 0xFF3030);
		graphics.fill(x, y, x + 16, y + 16, (alpha / 2) << 24 | 0xFF6040);
	}

	// ---------------------------------------------------------------- item name

	/** How far the armor bar moved up for extra heart rows, as vanilla works it out. */
	public static int heartRowsShift() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || player.isCreative() || player.isSpectator()) {
			return 0;
		}
		int rows = Mth.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		return Math.max(0, rows - 1) * rowHeight;
	}

	/** Size of the held item name; bounces when the item changes. */
	public static float nameScale(float partial) {
		if (!on(GearConfig.itemNameBounce)) {
			return 1.0F;
		}
		return 1.0F + NAME_BOUNCE * spring(nameAt, partial);
	}

	// ---------------------------------------------------------------- experience bar

	/** The shown fill of the experience bar, moving smoothly to the real one. */
	public static float experience(float real, float partial) {
		if (!on(GearConfig.xpAnimated)) {
			return real;
		}
		return Mth.clamp(Mth.lerp(partial, xpShownBefore, xpShown), 0, 1);
	}

	/** Light over the filled part after gaining experience, and the level up sparks. */
	public static void experienceEffects(GuiGraphicsExtractor graphics, int left, int top, float partial) {
		if (!on(GearConfig.xpAnimated)) {
			return;
		}
		float light = Mth.lerp(partial, glowBefore, glow);
		int filled = (int) (experience(lastProgress, partial) * BAR_WIDTH);
		if (light > 0.01F && filled > 0) {
			int alpha = (int) (110 * light);
			graphics.fill(left, top, left + filled, top + BAR_HEIGHT, alpha << 24 | 0xFFFFA0);
			// a brighter tip at the end of the fill
			graphics.fill(Math.max(left, left + filled - 3), top - 1, left + filled, top + BAR_HEIGHT + 1,
					(int) (180 * light) << 24 | 0xFFFFFF);
		}
		for (Spark spark : SPARK_LIST) {
			float fade = 1.0F - spark.age / (float) SPARK_TICKS;
			int x = left + Math.round(spark.x + spark.vx * partial);
			int y = top + Math.round(spark.y + spark.vy * partial);
			int alpha = (int) (255 * fade) << 24;
			int size = spark.age < SPARK_TICKS / 2 ? 2 : 1;
			graphics.fill(x, y, x + size, y + size, alpha | (spark.color & 0xFFFFFF));
		}
	}
}
