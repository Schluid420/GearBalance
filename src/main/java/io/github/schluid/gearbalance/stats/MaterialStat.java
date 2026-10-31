package io.github.schluid.gearbalance.stats;

/** Values set per material in the "Materials" tab. */
public enum MaterialStat {
	/** Player level that must have been reached once. */
	LEVEL("level", 0, 0, 1000, 1),
	/** Durability one repair item restores at the anvil, in percent. */
	REPAIR("repair", 25, 1, 100, 1),
	/** Movement speed lost with the full set, in percent. Every worn piece carries a quarter of it. */
	WEIGHT("weight", 0, 0, 90, 0.1);

	public final String key;
	public final double vanilla;
	public final double min;
	public final double max;
	/** Smallest step a value can take: 1 for whole numbers, 0.1 for the weight. */
	public final double step;

	MaterialStat(String key, double vanilla, double min, double max, double step) {
		this.key = key;
		this.vanilla = vanilla;
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public boolean whole() {
		return step == 1;
	}

	/** Wood and stone have no armor, and leather is too light to slow anyone down. */
	public boolean appliesTo(Material material) {
		return this != WEIGHT || (material.hasArmor() && material != Material.LEATHER);
	}

	public double clamp(double value) {
		double clamped = Math.max(min, Math.min(max, value));
		double rounded = Math.round(clamped / step) * step;
		// Removes float noise like 0.30000000000000004
		return Math.round(rounded * 10) / 10.0;
	}
}
