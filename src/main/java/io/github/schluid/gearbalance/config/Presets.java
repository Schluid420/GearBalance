package io.github.schluid.gearbalance.config;

import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;

/**
 * Three ready-made setups for a rounded game out of the box. Loading one replaces all values,
 * material settings, set bonuses and switches. The user's own profiles are not touched.
 */
public enum Presets {
	/** "VanillaPlus": vanilla without its weak spots. Gold lasts, leather holds up, netherite is worth it. */
	BALANCED("balanced"),
	/** Like VanillaPlus, plus level requirements, armor weight and set bonuses. */
	PROGRESSION("progression"),
	/** Less durability, expensive repairs, high requirements. */
	CHALLENGE("challenge");

	public final String key;

	Presets(String key) {
		this.key = key;
	}

	public void apply() {
		GearConfig.resetAll();
		applyItems();
		if (this != BALANCED) {
			applyMaterials();
		}
		applySwitches();
	}

	// Item values

	private void applyItems() {
		for (GearItems.Entry entry : GearItems.all()) {
			Material material = Material.of(entry.id());
			double durability = GearConfig.getEffective(entry, Stat.DURABILITY);
			GearConfig.setOverride(entry, Stat.DURABILITY, (double) Math.round(durability * durabilityFactor(entry, material)));

			if (material == Material.NETHERITE) {
				if (entry.kind() == GearItems.Kind.TOOL || entry.kind() == GearItems.Kind.WEAPON) {
					GearConfig.setOverride(entry, Stat.DAMAGE, GearConfig.getEffective(entry, Stat.DAMAGE) + 1);
				} else if (entry.kind() == GearItems.Kind.ARMOR) {
					GearConfig.setOverride(entry, Stat.TOUGHNESS, GearConfig.getEffective(entry, Stat.TOUGHNESS) + 1);
				}
			}
		}
		if (this == CHALLENGE) {
			GearItems.Entry shield = GearItems.get(Stat.SHIELD);
			GearConfig.setOverride(shield, Stat.SHIELD_DISABLE, 7.0);
			GearConfig.setOverride(shield, Stat.SHIELD_BLOCK, 80.0);
		}
	}

	private double durabilityFactor(GearItems.Entry entry, Material material) {
		boolean armor = entry.kind() == GearItems.Kind.ARMOR;
		if (material == Material.GOLD) {
			// Gold gets its own numbers so it stays a real choice in every preset
			if (this == CHALLENGE) {
				return armor ? 2 : 4;
			}
			return armor ? 3 : 6;
		}
		double factor = 1;
		if (material == Material.LEATHER) {
			factor = 2;
		} else if (material == Material.NETHERITE) {
			factor = 1.5;
		}
		return this == CHALLENGE ? factor * 0.75 : factor;
	}

	// Materials and set bonuses

	private void applyMaterials() {
		boolean hard = this == CHALLENGE;
		level(Material.COPPER, hard ? 10 : 5);
		level(Material.CHAINMAIL, hard ? 15 : 10);
		level(Material.GOLD, hard ? 15 : 10);
		level(Material.IRON, hard ? 20 : 15);
		level(Material.DIAMOND, hard ? 35 : 25);
		level(Material.NETHERITE, hard ? 50 : 35);

		// Iron is the heaviest, netherite as endgame material the lightest of the metals
		weight(Material.COPPER, hard ? 3 : 2);
		weight(Material.CHAINMAIL, hard ? 5 : 3);
		weight(Material.GOLD, hard ? 6 : 4);
		weight(Material.IRON, hard ? 10 : 6);
		weight(Material.DIAMOND, hard ? 8 : 5);
		weight(Material.NETHERITE, hard ? 5 : 3);

		if (hard) {
			for (Material material : Material.values()) {
				GearConfig.setMaterial(MaterialStat.REPAIR, material, 15);
			}
		}

		set(Material.LEATHER, BonusType.SPEED, hard ? 6 : 10);
		set(Material.COPPER, BonusType.ARMOR, hard ? 1 : 2);
		set(Material.CHAINMAIL, BonusType.KNOCKBACK_RESISTANCE, hard ? 10 : 20);
		set(Material.IRON, BonusType.HEALTH, hard ? 1 : 2);
		set(Material.GOLD, BonusType.LUCK, hard ? 1 : 2);
		set(Material.DIAMOND, BonusType.TOUGHNESS, hard ? 1 : 2);
		set(Material.NETHERITE, BonusType.BURNING, hard ? 30 : 50);
	}

	private static void level(Material material, int level) {
		GearConfig.setMaterial(MaterialStat.LEVEL, material, level);
	}

	private static void weight(Material material, double percent) {
		GearConfig.setMaterial(MaterialStat.WEIGHT, material, percent);
	}

	private static void set(Material material, BonusType type, double strength) {
		GearConfig.setSet(material, type, strength);
	}

	// Switches

	private void applySwitches() {
		GearConfig.enabled = true;
		GearConfig.setBonuses = this != BALANCED;
		GearConfig.tridentRecipe = true;
		GearConfig.chainmailRecipes = true;
		GearConfig.rabbitHideRecipe = false;
		GearConfig.rabbitHideArmor = false;
		GearConfig.anvilNoLimit = true;
		GearConfig.anvilMaxCost = 39;
		GearConfig.zombieLeather = true;
		GearConfig.zombieLeatherChance = this == CHALLENGE ? 20 : 33;
		GearConfig.lootTemplates = true;
		GearConfig.lootMaterials = true;
		GearConfig.lootNoCheap = true;
	}
}
