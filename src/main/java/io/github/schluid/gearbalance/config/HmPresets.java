package io.github.schluid.gearbalance.config;

import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;

/**
 * HM+ profiles, one per HardmodePlus profile (same names). Level requirements are above Progression and rise with the profile;
 * gear helps against the harder mobs (more durability, lighter armor, stronger swords and spears). One active at a time.
 */
public enum HmPresets {
	/** Mobs like vanilla: VanillaPlus without level requirements, weight or set bonuses. */
	RELAXED("relaxed"),
	/** +25 %, -10 % experience. */
	TOUGHER("tougher"),
	/** +50 %, faster zombies and spiders, skeletons see further, -25 % experience. */
	VETERAN("veteran"),
	/** +75 %, armored undead, sieges, -40 % experience. */
	NIGHTMARE("nightmare"),
	/** +100 %, heavily armored undead, -50 % experience. Heavy armor on purpose. */
	REALISM("realism");

	public final String key;

	HmPresets(String key) {
		this.key = key;
	}

	public void apply() {
		if (this == RELAXED) {
			Presets.BALANCED.apply();
		} else {
			Presets.PROGRESSION.apply();
			durability();
			levels();
			weights();
			bonuses();
			weaponDamage();
			if (this == NIGHTMARE) {
				// Anvil repair at least 30 %, materials that already repair more keep their value
				for (Material material : Material.values()) {
					double repair = GearConfig.getMaterial(MaterialStat.REPAIR, material);
					GearConfig.setMaterial(MaterialStat.REPAIR, material, Math.max(repair, 30));
				}
			}
		}
		GearConfig.shieldWear = this == REALISM;
		// Add-ons set the values of their own materials and items, before the profile is marked active
		GearBalanceApi.runPresetHooks(key);
		GearConfig.markHmProfile(key);
	}

	/** More monsters wear gear down faster. Applied on top of the base durability. */
	private void durability() {
		double factor = switch (this) {
			case TOUGHER -> 1.1;
			case VETERAN -> 1.25;
			case NIGHTMARE -> 1.4;
			default -> 1.5;
		};
		for (GearItems.Entry entry : GearItems.all()) {
			double durability = GearConfig.getEffective(entry, Stat.DURABILITY);
			GearConfig.setOverride(entry, Stat.DURABILITY, (double) Math.round(durability * factor));
		}
	}

	/** Higher requirements than Progression (+10 to +50 %) for copper, chainmail, iron, gold, diamond and up. */
	private void levels() {
		int[] levels = switch (this) {
			case TOUGHER -> new int[] {6, 11, 17, 22, 28, 39};
			case VETERAN -> new int[] {6, 12, 18, 24, 30, 42};
			case NIGHTMARE -> new int[] {7, 13, 20, 26, 33, 46};
			default -> new int[] {8, 15, 23, 30, 38, 53};
		};
		Material[] materials = {Material.COPPER, Material.CHAINMAIL, Material.IRON, Material.GOLD,
				Material.DIAMOND, Material.NETHERITE};
		for (int i = 0; i < materials.length; i++) {
			GearConfig.setMaterial(MaterialStat.LEVEL, materials[i], levels[i]);
		}
		Material titanium = Material.byKey("titanium");
		if (titanium != null) {
			GearConfig.setMaterial(MaterialStat.LEVEL, titanium, switch (this) {
				case TOUGHER -> 50;
				case VETERAN -> 54;
				case NIGHTMARE -> 59;
				default -> 68;
			});
		}
	}

	/** Faster mobs, lighter armor so the player can still get away. Realism stays heavy. */
	private void weights() {
		if (this == TOUGHER) {
			return; // same as Progression
		}
		double[] weights = switch (this) {
			case VETERAN -> new double[] {1.5, 2.3, 3, 4.5, 3.8, 2.3};
			case NIGHTMARE -> new double[] {1, 1.5, 2, 3, 2.5, 1.5};
			default -> new double[] {3, 5, 6, 10, 8, 5};
		};
		Material[] materials = {Material.COPPER, Material.CHAINMAIL, Material.GOLD, Material.IRON,
				Material.DIAMOND, Material.NETHERITE};
		for (int i = 0; i < materials.length; i++) {
			GearConfig.setMaterial(MaterialStat.WEIGHT, materials[i], weights[i]);
		}
		Material titanium = Material.byKey("titanium");
		if (titanium != null) {
			GearConfig.setMaterial(MaterialStat.WEIGHT, titanium, switch (this) {
				case VETERAN -> 1.5;
				case NIGHTMARE -> 1.0;
				default -> 4.0;
			});
		}
	}

	/** Set bonuses start like Progression and get stronger where the mobs call for it. */
	private void bonuses() {
		if (this == VETERAN) {
			// Against fast zombies and spiders
			GearConfig.setSet(Material.LEATHER, BonusType.SPEED, 12);
		} else if (this == NIGHTMARE) {
			// Sieges: faster hits with copper, one more heart from iron
			GearConfig.setSet(Material.COPPER, BonusType.ATTACK_SPEED, 12);
			GearConfig.setSet(Material.IRON, BonusType.HEALTH, 5);
		}
	}

	/** Armored undead: more damage for swords and spears, rising with the profile. Axes stay as they are. */
	private void weaponDamage() {
		double bonus = switch (this) {
			case TOUGHER -> 0.5;
			case VETERAN -> 1;
			case NIGHTMARE -> 1.5;
			default -> 2;
		};
		for (GearItems.Entry entry : GearItems.all()) {
			if (entry.id().endsWith("_sword") || entry.id().endsWith("_spear")) {
				GearConfig.setOverride(entry, Stat.DAMAGE, GearConfig.getEffective(entry, Stat.DAMAGE) + bonus);
			}
		}
	}
}
