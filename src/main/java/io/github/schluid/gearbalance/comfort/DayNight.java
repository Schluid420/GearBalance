package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;

/** Own length of day (time 0-12000) and night (12000-24000, 10 real minutes each in vanilla) by changing the clock rate. */
public final class DayNight {
	private DayNight() {
	}

	private static final long DAY = 24000;
	private static final long HALF = 12000;
	/** Vanilla minutes of each half (12000 ticks at 20 per second). */
	private static final float VANILLA_MINUTES = 10.0F;

	/** True while the rate may differ from normal, so switching off resets the clock once. Starts true (saved world). */
	private static boolean changed = true;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(DayNight::tick);
	}

	private static void tick(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		Holder<WorldClock> clock = overworld.dimensionType().defaultClock().orElse(null);
		if (clock == null) {
			return;
		}
		ServerClockManager clocks = overworld.clockManager();
		float wanted = 1.0F;
		if (GearConfig.dayNightActive()) {
			boolean day = overworld.getOverworldClockTime() % DAY < HALF;
			int minutes = day ? GearConfig.dayMinutes : GearConfig.nightMinutes;
			wanted = VANILLA_MINUTES / Math.max(1, minutes);
		} else if (!changed) {
			return;
		}
		// Setting the rate sends a packet to every player, so only when it really changes
		if (Math.abs(clocks.getInstance(clock).rate() - wanted) > 0.0001F) {
			clocks.setRate(clock, wanted);
		}
		changed = wanted != 1.0F;
	}
}
