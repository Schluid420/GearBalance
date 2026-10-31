package io.github.schluid.gearbalance.level;

import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Level requirements per material. A piece counts as unlocked once the player has reached
 * the level at least once (HighestLevel). Creative and spectator players are not affected.
 *
 * Locked armor is moved back to the inventory every tick, which catches every way of putting it on.
 * Locked tools mine like the bare hand (PlayerMixin, ItemStackMixin) and can't hurt mobs.
 */
public final class LevelRules {
	private LevelRules() {
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public static void register() {
		HighestLevel.init();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HighestLevel.update(player);
				checkArmor(player);
			}
		});

		// Also runs on the client (the level is synced), so the swing isn't shown at all
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
				blocked(player, player.getItemInHand(hand)) ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	public static int required(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		return GearConfig.requiredLevel(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	public static boolean exempt(Player player) {
		return player.isCreative() || player.isSpectator();
	}

	public static boolean isLocked(Player player, ItemStack stack) {
		if (player == null) {
			return false;
		}
		int required = required(stack);
		return required > 0 && !exempt(player) && HighestLevel.get(player) < required;
	}

	public static void showLockedHint(Player player, ItemStack stack) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("gearbalance.level.locked", required(stack)));
		}
	}

	private static boolean blocked(Player player, ItemStack stack) {
		if (!isLocked(player, stack)) {
			return false;
		}
		showLockedHint(player, stack);
		return true;
	}

	private static void checkArmor(ServerPlayer player) {
		if (exempt(player)) {
			return;
		}
		int highest = HighestLevel.get(player);
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			int required = required(stack);
			if (required <= 0 || highest >= required) {
				continue;
			}
			player.setItemSlot(slot, ItemStack.EMPTY);
			ItemStack rest = stack.copy();
			if (!player.getInventory().add(rest) || !rest.isEmpty()) {
				player.spawnAtLocation(player.level(), rest);
			}
			player.sendOverlayMessage(Component.translatable("gearbalance.level.locked", required));
		}
	}
}
