package io.github.schluid.gearbalance.level;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.GearBalance;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

/**
 * Highest experience level the player has ever reached. It only goes up, so spending levels
 * or dying doesn't lock gear again. Synced to the own client for the tooltip.
 */
public final class HighestLevel {
	private HighestLevel() {
	}

	public static final AttachmentType<Integer> TYPE = AttachmentRegistry.create(GearBalance.id("highest_level"),
			builder -> builder
					.persistent(Codec.INT)
					.copyOnDeath()
					.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/** Loads the class, which registers the attachment. */
	public static void init() {
	}

	public static int get(Player player) {
		Integer stored = ((AttachmentTarget) player).getAttachedOrElse(TYPE, 0);
		return Math.max(stored == null ? 0 : stored, player.experienceLevel);
	}

	/** Server side. */
	public static void update(Player player) {
		Integer stored = ((AttachmentTarget) player).getAttachedOrElse(TYPE, 0);
		if (stored == null || player.experienceLevel > stored) {
			((AttachmentTarget) player).setAttached(TYPE, player.experienceLevel);
		}
	}
}
