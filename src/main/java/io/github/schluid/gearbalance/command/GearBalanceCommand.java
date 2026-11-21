package io.github.schluid.gearbalance.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.network.SettingsSync;
import io.github.schluid.gearbalance.network.VersionCheck;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * /gearbalance for operators:
 * reload – read config/gearbalance.json again and send it to every player,
 * sync – send the settings to every player again,
 * info – version, mod active, changed values, switched on functions,
 * versions – the GearBalance version of every player online.
 */
public final class GearBalanceCommand {
	private GearBalanceCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("gearbalance")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reload").executes(context -> {
					CommandSourceStack source = context.getSource();
					GearConfig.load();
					SettingsSync.sendToAll(source.getServer());
					source.sendSuccess(() -> text("gearbalance.command.reload",
							"Settings read again and sent. Gear values need a restart of the server."), true);
					return 1;
				}))
				.then(Commands.literal("sync").executes(context -> {
					CommandSourceStack source = context.getSource();
					SettingsSync.sendToAll(source.getServer());
					source.sendSuccess(() -> text("gearbalance.command.sync", "Settings sent to every player."), true);
					return 1;
				}))
				.then(Commands.literal("info").executes(context -> {
					context.getSource().sendSuccess(GearBalanceCommand::info, false);
					return 1;
				}))
				.then(Commands.literal("versions").executes(context -> {
					CommandSourceStack source = context.getSource();
					List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
					source.sendSuccess(() -> text("gearbalance.command.versions", "GearBalance %s on the server, players:",
							VersionCheck.version()), false);
					for (ServerPlayer player : players) {
						String version = VersionCheck.of(player);
						ChatFormatting color = version == null ? ChatFormatting.RED
								: version.equals(VersionCheck.version()) ? ChatFormatting.GREEN : ChatFormatting.GOLD;
						Component line = Component.literal(" " + player.getName().getString() + ": ")
								.append(version == null ? text("gearbalance.command.versions.none", "no GearBalance")
										: Component.literal(version))
								.withStyle(color);
						source.sendSuccess(() -> line, false);
					}
					return players.size();
				})));
	}

	private static Component info() {
		List<String> on = new ArrayList<>();
		add(on, "padded_armor", GearConfig.paddedArmorActive());
		add(on, "leather_patch", GearConfig.leatherPatchActive());
		add(on, "horse_padding", GearConfig.horsePaddingActive());
		add(on, "upgrades", GearConfig.upgradesActive());
		add(on, "sockets", GearConfig.socketsActive());
		add(on, "shields", GearConfig.shieldsActive());
		add(on, "parry", GearConfig.parryActive());
		add(on, "bags", GearConfig.bagsActive());
		add(on, "repair_kit", GearConfig.repairKitActive());
		add(on, "tool_refill", GearConfig.toolRefillActive());
		add(on, "break_protection", GearConfig.breakProtectionActive());
		add(on, "loadouts", GearConfig.loadoutsActive());
		MutableComponent functions = Component.empty();
		for (int i = 0; i < on.size(); i++) {
			if (i > 0) {
				functions.append(", ");
			}
			functions.append(Component.translatable("gearbalance.options." + on.get(i)));
		}
		if (on.isEmpty()) {
			functions.append("–");
		}
		return text("gearbalance.command.info", "GearBalance %s – mod active: %s – changed values: %s – functions: %s",
				VersionCheck.version(), Component.translatable(GearConfig.enabled ? "options.on" : "options.off"),
				GearConfig.changeCount(), functions);
	}

	private static void add(List<String> list, String key, boolean active) {
		if (active) {
			list.add(key);
		}
	}

	/** Translated where the player has GearBalance, English otherwise. */
	private static MutableComponent text(String key, String fallback, Object... args) {
		return Component.translatableWithFallback(key, fallback, args);
	}
}
