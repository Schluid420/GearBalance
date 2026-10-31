package io.github.schluid.gearbalance;

import io.github.schluid.gearbalance.bonus.SetBonus;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.level.LevelRules;
import io.github.schluid.gearbalance.loot.EndgameLoot;
import io.github.schluid.gearbalance.loot.ZombieLeather;
import io.github.schluid.gearbalance.recipe.OptionCondition;
import io.github.schluid.gearbalance.stats.StatApplier;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearBalance implements ModInitializer {
	public static final String MOD_ID = "gearbalance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GearConfig.load();
		StatApplier.register();
		OptionCondition.register();
		LevelRules.register();
		ZombieLeather.register();
		EndgameLoot.register();
		SetBonus.register();
		LOGGER.info("GearBalance loaded with {} changed values", GearConfig.changeCount());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
