package io.github.schluid.gearbalance;

import io.github.schluid.gearbalance.api.GearBalanceAddon;
import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.bonus.SetBonus;
import io.github.schluid.gearbalance.command.GearBalanceCommand;
import io.github.schluid.gearbalance.comfort.DayNight;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.comfort.DeathRules;
import io.github.schluid.gearbalance.comfort.Loadouts;
import io.github.schluid.gearbalance.comfort.Trash;
import io.github.schluid.gearbalance.comfort.RestoreRecipe;
import io.github.schluid.gearbalance.comfort.SpawnerDrops;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.cooldown.Cooldowns;
import io.github.schluid.gearbalance.grindstone.GrindstoneChoicePayload;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
import io.github.schluid.gearbalance.level.LevelRules;
import io.github.schluid.gearbalance.loot.EndgameLoot;
import io.github.schluid.gearbalance.loot.OverworldLoot;
import io.github.schluid.gearbalance.loot.ZombieLeather;
import io.github.schluid.gearbalance.network.SettingsSync;
import io.github.schluid.gearbalance.network.VersionCheck;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import io.github.schluid.gearbalance.padded.PaddedArmorRecipe;
import io.github.schluid.gearbalance.padded.SewingNeedle;
import io.github.schluid.gearbalance.padded.Tailoring;
import io.github.schluid.gearbalance.plan.MasterPlan;
import io.github.schluid.gearbalance.recipe.OptionCondition;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.smith.LeverRecipe;
import io.github.schluid.gearbalance.smith.SmithingTools;
import io.github.schluid.gearbalance.socket.GemLoot;
import io.github.schluid.gearbalance.socket.GemRecipe;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Titanite;
import io.github.schluid.gearbalance.socket.SocketChisels;
import io.github.schluid.gearbalance.socket.SocketRecipe;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.stats.StatApplier;
import io.github.schluid.gearbalance.table.SmithingTableItems;
import io.github.schluid.gearbalance.titanium.Titanium;
import io.github.schluid.gearbalance.upgrade.CompressedItems;
import io.github.schluid.gearbalance.upgrade.UpgradeRecipe;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearBalance implements ModInitializer {
	public static final String MOD_ID = "gearbalance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		registerAddons();
		GearConfig.load();
		Titanium.register();
		Titanium.registerWorld();
		StatApplier.register();
		OptionCondition.register();
		LevelRules.register();
		ZombieLeather.register();
		EndgameLoot.register();
		OverworldLoot.register();
		SetBonus.register();
		PaddedArmor.register();
		SewingNeedle.register();
		PaddedArmorRecipe.register();
		Tailoring.register();
		CompressedItems.register();
		ForgeHammers.register();
		Upgrades.register();
		UpgradeRecipe.register();
		SocketChisels.register();
		Sockets.register();
		SocketRecipe.register();
		Shields.register();
		SmithingTools.register();
		LeverRecipe.register();
		Bags.register();
		Gems.register();
		Titanite.register();
		GemRecipe.register();
		GemLoot.register();
		MasterPlan.register();
		Cooldowns.register();
		GrindstoneChoicePayload.register();
		SmithingTableItems.register();
		SettingsSync.register();
		VersionCheck.register();
		Loadouts.register();
		Trash.register();
		DayNight.register();
		BreakProtection.register();
		DeathRules.register();
		SpawnerDrops.register();
		RestoreRecipe.register();
		GearBalanceCommand.register();
		// /reload also reads the function switches and values of config/gearbalance.json again and sends them
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			GearConfig.reloadFeatures();
			SettingsSync.sendToAll(server);
		});
		LOGGER.info("GearBalance loaded with {} changed values (mod active: {}, padded armor: {}, upgrades: {}, sockets: {})",
				GearConfig.changeCount(), GearConfig.enabled, GearConfig.paddedArmor, GearConfig.upgrades, GearConfig.sockets);
	}

	/** Add-ons register their gear before the config is read, so their saved values are found. */
	private static void registerAddons() {
		GearBalanceApi.setRegistrationOpen(true);
		// Titanium is part of GearBalance, registered like the gear of an add-on
		Titanium.registerGear();
		for (EntrypointContainer<GearBalanceAddon> container
				: FabricLoader.getInstance().getEntrypointContainers("gearbalance", GearBalanceAddon.class)) {
			String modId = container.getProvider().getMetadata().getId();
			try {
				container.getEntrypoint().registerGear();
				LOGGER.info("Gear from {} registered", modId);
			} catch (RuntimeException e) {
				LOGGER.error("Could not register the gear from {}", modId, e);
			}
		}
		GearBalanceApi.setRegistrationOpen(false);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
