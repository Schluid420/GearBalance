package io.github.schluid.gearbalance.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * The server decides how the game plays: on join it sends all settings, the client uses them only in memory.
 * Operators may change them in the menu; the server saves and sends them to everyone. A single player or LAN host
 * uses its own settings. Transfer: gzipped JSON.
 */
public final class SettingsSync {
	private SettingsSync() {
	}

	/** Server to client: the settings of the server, and whether this player may change them. */
	public record SettingsPayload(byte[] data, boolean canEdit) implements CustomPacketPayload {
		public static final Type<SettingsPayload> TYPE = new Type<>(GearBalance.id("settings"));
		/** Plain bytes, so it works in the configuration phase too. */
		public static final StreamCodec<ByteBuf, SettingsPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.BYTE_ARRAY, SettingsPayload::data, ByteBufCodecs.BOOL, SettingsPayload::canEdit,
				SettingsPayload::new);

		@Override
		public Type<SettingsPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: an operator changed the settings of the server in the menu. */
	public record EditPayload(byte[] data) implements CustomPacketPayload {
		public static final Type<EditPayload> TYPE = new Type<>(GearBalance.id("settings_edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditPayload> CODEC =
				StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, EditPayload::data, EditPayload::new);

		@Override
		public Type<EditPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
		PayloadTypeRegistry.clientboundConfiguration().register(SettingsPayload.TYPE, SettingsPayload.CODEC);
		// Already while joining, before the client builds the item components: the gear values of the items
		// (tooltips, mining speed, durability) are then those of the server. Operator rights follow on JOIN.
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, SettingsPayload.TYPE)) {
				ServerConfigurationNetworking.send(handler, new SettingsPayload(pack(GearConfig.settingsToJson()), false));
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(EditPayload.TYPE, EditPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));
		ServerPlayNetworking.registerGlobalReceiver(EditPayload.TYPE, (payload, context) -> edit(context.player(), payload));
	}

	/** Operators (permission level "gamemaster", like for /gamemode) may change the settings of the server. */
	public static boolean canEdit(ServerPlayer player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Sends the current settings to one player, if the player has GearBalance. */
	public static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, SettingsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new SettingsPayload(pack(GearConfig.settingsToJson()), canEdit(player)));
		}
	}

	/** Sends the current settings to every player, e.g. after an operator changed them or after /reload. */
	public static void sendToAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
		}
	}

	private static void edit(ServerPlayer player, EditPayload payload) {
		if (!canEdit(player)) {
			GearBalance.LOGGER.warn("{} tried to change the GearBalance settings without permission", player.getName().getString());
			return;
		}
		JsonObject settings = unpack(payload.data());
		if (settings == null) {
			return;
		}
		String restartBefore = GearConfig.restartSignature();
		String recipesBefore = GearConfig.recipeState();
		GearConfig.settingsFromJson(settings);
		GearConfig.save();
		GearBalance.LOGGER.info("{} changed the GearBalance settings", player.getName().getString());
		sendToAll(player.level().getServer());
		player.sendSystemMessage(Component.translatable("gearbalance.server.saved").withStyle(ChatFormatting.GREEN));
		if (!restartBefore.equals(GearConfig.restartSignature()) || !recipesBefore.equals(GearConfig.recipeState())) {
			player.sendSystemMessage(Component.translatable("gearbalance.server.restart").withStyle(ChatFormatting.GOLD));
		}
	}

	/** The settings as gzipped JSON. */
	public static byte[] pack(JsonObject settings) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
			zip.write(settings.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException("Could not pack the settings", e);
		}
		return bytes.toByteArray();
	}

	/** Reads gzipped JSON; null if it is broken. */
	public static JsonObject unpack(byte[] data) {
		try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(data))) {
			return JsonParser.parseString(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			GearBalance.LOGGER.error("Could not read the GearBalance settings that were sent", e);
			return null;
		}
	}
}
