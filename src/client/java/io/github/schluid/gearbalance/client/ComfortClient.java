package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.comfort.Loadouts;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.network.VersionCheck;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.titanium.Titanium;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Client comfort: sprint fix, name colors by value, damaged-piece tooltip, version check, saved gear sets. */
public final class ComfortClient {
	private ComfortClient() {
	}

	/** Saved gear sets as the server sent them, empty lists until then. */
	static List<List<ItemStack>> loadouts = List.of();

	/** Numbers of the gear sets that hold this piece, e.g. "13"; empty if none. */
	public static String setNumbers(ItemStack stack) {
		if (stack.isEmpty() || loadouts.isEmpty() || !GearConfig.loadoutsActive()) {
			return "";
		}
		StringBuilder numbers = new StringBuilder();
		for (int set = 0; set < loadouts.size(); set++) {
			for (ItemStack saved : loadouts.get(set)) {
				if (saved.is(stack.getItem()) && Loadouts.same(stack, saved)) {
					numbers.append(set + 1);
					break;
				}
			}
		}
		return numbers.toString();
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ComfortClient::sprintFix);
		ClientPlayNetworking.registerGlobalReceiver(VersionCheck.VersionPayload.TYPE, (payload, context) -> {
			String own = VersionCheck.version();
			if (!own.equals(payload.version()) && context.player() != null) {
				context.player().sendSystemMessage(Component.translatable("gearbalance.version.mismatch", own,
						payload.version()).withStyle(ChatFormatting.GOLD));
			}
			context.responseSender().sendPacket(new VersionCheck.VersionPayload(own));
		});
		ClientPlayNetworking.registerGlobalReceiver(Loadouts.SetsPayload.TYPE,
				(payload, context) -> loadouts = payload.sets());
		ClientPlayNetworking.registerGlobalReceiver(Loadouts.MessagePayload.TYPE,
				(payload, context) -> LoadoutButtons.show(Loadouts.text(payload.kind(), payload.set(), payload.count())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> loadouts = List.of());
		LoadoutButtons.register();
		TrashButton.register();
		HotbarHide.register();
	}

	/** Tooltip: name color by value; damaged pieces in gray with repair hint. Registered last to see all lines. */
	public static void registerTooltip() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (lines.isEmpty() || !GearConfig.enabled) {
				return;
			}
			if (BreakProtection.wornOut(stack)) {
				damaged(stack, lines);
				return;
			}
			ChatFormatting color = nameColor(stack);
			if (color != null) {
				lines.set(0, lines.get(0).copy().withStyle(color));
			}
			// Titanium armor: the fall damage it takes off
			if (GearConfig.titaniumActive() && GearConfig.titaniumFallPercent > 0 && Titanium.isArmor(stack)) {
				lines.add(Component.translatable("gearbalance.titanium.fall", GearConfig.titaniumFallPercent)
						.withStyle(ChatFormatting.BLUE));
			}
		});
	}

	private static void damaged(ItemStack stack, List<Component> lines) {
		for (int i = 1; i < lines.size(); i++) {
			lines.set(i, lines.get(i).copy().withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.set(0, Component.translatable("gearbalance.break_protection.name", lines.get(0).copy())
				.withStyle(ChatFormatting.GRAY));
		String material = BreakProtection.repairMaterial(stack.getItem());
		Item repairItem = material == null ? null : BreakProtection.repairItem(material);
		lines.add(1, Component.translatable("gearbalance.break_protection.damaged").withStyle(ChatFormatting.RED));
		if (repairItem != null) {
			lines.add(2, Component.translatable("gearbalance.break_protection.repair",
					Component.translatable(repairItem.getDescriptionId())).withStyle(ChatFormatting.GOLD));
		}
	}

	/** Sprint fix (mode "Hold"): letting go of the sprint key stops sprinting at once; double-tap sprint is off. */
	private static void sprintFix(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (!GearConfig.sprintFix || player == null || minecraft.options.toggleSprint().get()) {
			return;
		}
		if (player.isSprinting() && !minecraft.options.keySprint.isDown()) {
			player.setSprinting(false);
		}
	}

	/** Gold when every socket holds a gem, otherwise green, blue, purple for +1, +2, +3; null = as it is. */
	/** Color of an upgraded or fully socketed piece, null for none. */
	static ChatFormatting nameColor(ItemStack stack) {
		if (!GearConfig.nameColors) {
			return null;
		}
		int max = Sockets.max(stack);
		if (Sockets.active() && max > 0 && Sockets.count(stack) >= max && !Sockets.hasEmptySocket(stack)) {
			return ChatFormatting.GOLD;
		}
		return switch (Upgrades.level(stack)) {
			case 1 -> ChatFormatting.GREEN;
			case 2 -> ChatFormatting.BLUE;
			case 3 -> ChatFormatting.LIGHT_PURPLE;
			default -> null;
		};
	}
}
