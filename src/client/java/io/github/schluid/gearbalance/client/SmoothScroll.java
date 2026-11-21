package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;

/** Smooth scrolling: lists glide to the new position, chat scrolls line by line, hotbar frame glides. */
public final class SmoothScroll {
	private SmoothScroll() {
	}

	/** Part of the remaining way covered per 1/60 second. */
	private static final double SPEED = 0.35;
	/** Time for one chat line, in nanoseconds. */
	private static final long CHAT_STEP = 12_000_000L;

	public static boolean lists() {
		return GearConfig.enabled && GearConfig.smoothScroll;
	}

	public static boolean hotbar() {
		return GearConfig.enabled && GearConfig.hotbarGlide;
	}

	/** Part of the way to move now, from the time since the last frame. */
	public static double step(long lastNanos, long nowNanos) {
		double frames = Math.min(10.0, (nowNanos - lastNanos) / 16_666_666.0);
		return 1.0 - Math.pow(1.0 - SPEED, frames);
	}

	// ---------------------------------------------------------------- chat

	private static int chatPending;
	private static long chatLast;
	/** True while this class scrolls the chat itself. */
	public static boolean chatOwnStep;

	public static void chatScrolled(int lines) {
		chatPending += lines;
	}

	/** One frame of the chat: a few lines towards the wanted position. */
	public static void chatFrame(net.minecraft.client.gui.components.ChatComponent chat) {
		if (chatPending == 0) {
			chatLast = System.nanoTime();
			return;
		}
		long now = System.nanoTime();
		int lines = (int) Math.min(Math.abs(chatPending), Math.max(1, (now - chatLast) / CHAT_STEP));
		if (lines <= 0) {
			return;
		}
		chatLast = now;
		int sign = Integer.signum(chatPending);
		chatOwnStep = true;
		chat.scrollChat(sign * lines);
		chatOwnStep = false;
		chatPending -= sign * lines;
	}

	public static void chatReset() {
		chatPending = 0;
	}

	// ---------------------------------------------------------------- hotbar frame

	private static double frameX = Double.NaN;
	private static long frameLast;

	/** x of the selection frame this frame, gliding to the wanted x. */
	public static int frameX(int wanted) {
		long now = System.nanoTime();
		if (Double.isNaN(frameX) || Math.abs(frameX - wanted) > 200) {
			frameX = wanted;
		} else {
			frameX += (wanted - frameX) * step(frameLast, now);
			if (Math.abs(frameX - wanted) < 0.5) {
				frameX = wanted;
			}
		}
		frameLast = now;
		return (int) Math.round(frameX);
	}
}
