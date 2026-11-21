package io.github.schluid.gearbalance.cooldown;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Wait time against upgrade spam: each piece stores the game time of its last upgrade and socket
 * ("gearbalance:upgrade_time", "gearbalance:socket_time"). Next one after the hours in GearConfig (1 hour = 1000 ticks; 0 = no wait).
 */
public final class Cooldowns {
	private Cooldowns() {
	}

	private static final long TICKS_PER_HOUR = 1000L;

	public static DataComponentType<Long> UPGRADE_TIME;
	public static DataComponentType<Long> SOCKET_TIME;

	public static void register() {
		UPGRADE_TIME = component("upgrade_time");
		SOCKET_TIME = component("socket_time");
	}

	private static DataComponentType<Long> component(String name) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, name),
				DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
	}

	private static int hours(boolean upgrade) {
		return upgrade ? GearConfig.upgradeCooldownHours : GearConfig.socketCooldownHours;
	}

	/** Ticks until the piece can get its next upgrade (or socket), 0 if it can now. */
	public static long remainingTicks(ItemStack stack, Level level, boolean upgrade) {
		int hours = hours(upgrade);
		if (hours <= 0 || level == null || stack.isEmpty()) {
			return 0;
		}
		Long last = stack.get(upgrade ? UPGRADE_TIME : SOCKET_TIME);
		if (last == null) {
			return 0;
		}
		long left = last + hours * TICKS_PER_HOUR - level.getGameTime();
		// A time in the future (other world, changed time) never blocks for longer than the wait time
		return Math.max(0, Math.min(left, hours * TICKS_PER_HOUR));
	}

	public static boolean ready(ItemStack stack, Level level, boolean upgrade) {
		return remainingTicks(stack, level, upgrade) == 0;
	}

	/** Remembers the game time of this upgrade (or socket) on the piece, only while a wait time is set. */
	public static void mark(ItemStack stack, long gameTime, boolean upgrade) {
		if (hours(upgrade) > 0 && !stack.isEmpty()) {
			stack.set(upgrade ? UPGRADE_TIME : SOCKET_TIME, gameTime);
		}
	}

	/** Remaining wait in whole in-game hours, rounded up (for the tooltip). */
	public static long remainingHours(ItemStack stack, Level level, boolean upgrade) {
		long ticks = remainingTicks(stack, level, upgrade);
		return (ticks + TICKS_PER_HOUR - 1) / TICKS_PER_HOUR;
	}
}
