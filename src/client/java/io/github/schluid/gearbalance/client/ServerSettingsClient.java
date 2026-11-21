package io.github.schluid.gearbalance.client;

import com.google.gson.JsonObject;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.network.SettingsSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client side of server settings: a foreign server's settings are used in memory, own ones return on leaving. */
public final class ServerSettingsClient {
	private ServerSettingsClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsSync.SettingsPayload.TYPE, (payload, context) ->
				use(context.client(), payload));
		// While joining: before the item components are built, so the gear values of the items are the server's
		ClientConfigurationNetworking.registerGlobalReceiver(SettingsSync.SettingsPayload.TYPE, (payload, context) ->
				use(context.client(), payload));
		// A new connection starts with the own settings (also after being kicked while joining)
		ClientConfigurationConnectionEvents.START.register((handler, client) -> client.execute(GearConfig::useOwnSettings));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(GearConfig::useOwnSettings));
	}

	private static void use(Minecraft client, SettingsSync.SettingsPayload payload) {
		// The host of a single player or LAN world keeps its own settings (they are the server's)
		if (client.getSingleplayerServer() != null) {
			return;
		}
		JsonObject settings = SettingsSync.unpack(payload.data());
		if (settings != null) {
			GearConfig.useServerSettings(settings, payload.canEdit());
		}
	}

	/** The settings in the menu are locked: they come from a server and the player is no operator. */
	public static boolean locked() {
		return GearConfig.serverMode() && !GearConfig.serverCanEdit();
	}

	/** An operator changed the settings of the server in the menu: send them, the server saves them. */
	public static void sendToServer() {
		if (GearConfig.serverMode() && GearConfig.serverCanEdit() && ClientPlayNetworking.canSend(SettingsSync.EditPayload.TYPE)) {
			ClientPlayNetworking.send(new SettingsSync.EditPayload(SettingsSync.pack(GearConfig.settingsToJson())));
		}
	}
}
