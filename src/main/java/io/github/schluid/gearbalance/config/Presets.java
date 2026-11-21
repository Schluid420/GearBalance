package io.github.schluid.gearbalance.config;

import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;

/**
 * Three ready-made setups, all based on BaseValues. Loading one replaces all values, material settings, set bonuses
 * and switches (except titanium and view settings). The user's own profiles are not touched.
 */
public enum Presets {
	/** "VanillaPlus": the base without level requirements, armor weight and set bonuses. */
	BALANCED("balanced"),
	/** The base as it is: level requirements, armor weight and set bonuses included. */
	PROGRESSION("progression"),
	/** Less durability, expensive repairs, high requirements, weaker set bonuses. */
	CHALLENGE("challenge");

	public final String key;

	Presets(String key) {
		this.key = key;
	}

	public void apply() {
		GearConfig.resetAll();
		// The numbers of the functions (shields, gems, ...) start from their defaults too
		GearConfig.resetValues();
		// The base also sets every switch, shield wear included, so nothing is left over from before
		GearConfig.applyJson(BaseValues.json());
		GearConfig.enabled = true;
		functions(this != BALANCED);
		if (this == BALANCED) {
			balanced();
		} else if (this == CHALLENGE) {
			challenge();
		}
		// Add-ons set the values of their own materials and items
		GearBalanceApi.runPresetHooks(key);
	}

	/** Functions: comfort in every preset, the rest (smithing, shields, bags, loot, recipes) from Progression on. Titanium and view stay. */
	private static void functions(boolean all) {
		GearConfig.sprintFix = true;
		GearConfig.toolRefill = true;
		GearConfig.breakProtection = true;
		GearConfig.loadouts = true;
		GearConfig.trash = true;

		GearConfig.paddedArmor = all;
		GearConfig.leatherPatch = all;
		GearConfig.horsePadding = all;
		GearConfig.upgrades = all;
		GearConfig.sockets = all;
		GearConfig.gemReturn = all;
		GearConfig.gemRecut = all;
		GearConfig.masterPlan = all;
		GearConfig.shields = all;
		GearConfig.parry = all;
		GearConfig.repairKit = all;
		GearConfig.gemLever = all;
		GearConfig.bags = all;
		GearConfig.lootOverworld = all;
		GearConfig.horseArmorRecipes = all;
	}

	private static void balanced() {
		for (Material material : Material.values()) {
			GearConfig.setMaterial(MaterialStat.LEVEL, material, MaterialStat.LEVEL.vanilla);
			GearConfig.setMaterial(MaterialStat.WEIGHT, material, MaterialStat.WEIGHT.vanilla);
			GearConfig.resetSet(material);
		}
		GearConfig.setBonuses = false;
	}

	private static void challenge() {
		// A quarter less durability for everything
		for (GearItems.Entry entry : GearItems.all()) {
			double durability = GearConfig.getEffective(entry, Stat.DURABILITY);
			GearConfig.setOverride(entry, Stat.DURABILITY, (double) Math.round(durability * 0.75));
		}

		level(Material.COPPER, 10);
		level(Material.CHAINMAIL, 15);
		level(Material.GOLD, 27);
		level(Material.IRON, 20);
		level(Material.DIAMOND, 35);
		level(Material.NETHERITE, 50);
		Material titanium = Material.byKey("titanium");
		if (titanium != null) {
			level(titanium, 60);
		}

		// Iron is the heaviest, netherite as endgame material the lightest of the metals
		weight(Material.COPPER, 3);
		weight(Material.CHAINMAIL, 5);
		weight(Material.GOLD, 6);
		weight(Material.IRON, 10);
		weight(Material.DIAMOND, 8);
		weight(Material.NETHERITE, 5);
		if (titanium != null) {
			weight(titanium, 4);
		}

		for (Material material : Material.values()) {
			GearConfig.setMaterial(MaterialStat.REPAIR, material, 15);
			// Set bonuses half as strong
			if (GearConfig.isSetChanged(material)) {
				GearConfig.setSet(material, GearConfig.getSetType(material), GearConfig.getSetStrength(material) / 2);
			}
		}

		GearItems.Entry shield = GearItems.get(Stat.SHIELD);
		GearConfig.setOverride(shield, Stat.SHIELD_DISABLE, 7.0);
		GearConfig.setOverride(shield, Stat.SHIELD_BLOCK, 80.0);
		// GearBalance shields: a quarter less durability, axes disable them twice as long, they block 80 %
		for (String material : GearConfig.SHIELD_MATERIALS) {
			GearConfig.SHIELD_BLOCK.put(material, 80);
			GearConfig.SHIELD_DURABILITY.put(material,
					(int) Math.round(GearConfig.defaultShieldDurability(material) * 0.75));
			GearConfig.SHIELD_AXE_SECONDS.put(material, Math.min(GearConfig.MAX_SHIELD_AXE_SECONDS,
					GearConfig.defaultShieldAxeSeconds(material) * 2));
		}

		GearConfig.zombieLeatherChance = 20;
	}

	private static void level(Material material, int level) {
		GearConfig.setMaterial(MaterialStat.LEVEL, material, level);
	}

	private static void weight(Material material, double percent) {
		GearConfig.setMaterial(MaterialStat.WEIGHT, material, percent);
	}
}
