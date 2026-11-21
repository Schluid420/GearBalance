package io.github.schluid.gearbalance.advancement;

import io.github.schluid.gearbalance.GearBalance;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Advancements granted by code ("minecraft:impossible" trigger, criterion "done") when a smithing result is taken. */
public final class GearAdvancements {
	private GearAdvancements() {
	}

	public static final String PADDED = "padded";
	public static final String UPGRADE = "upgrade";
	public static final String UPGRADE_MAX = "upgrade_max";
	public static final String SOCKET = "socket";
	public static final String GEM = "gem";
	public static final String GEMS_FULL = "gems_full";

	/** Awards "gearbalance:<name>" to the player (server side only, does nothing on the client). */
	public static void award(Player player, String name) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		MinecraftServer server = serverPlayer.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, name));
		if (holder != null) {
			serverPlayer.getAdvancements().award(holder, "done");
		}
	}
}
