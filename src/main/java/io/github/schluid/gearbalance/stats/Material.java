package io.github.schluid.gearbalance.stats;

/**
 * Materials for the values in the materials tab, taken from the item id ("diamond_sword" -> DIAMOND).
 * Bow, mace, shield and the like have no material.
 */
public enum Material {
	WOOD("wood", "wooden_"),
	LEATHER("leather", "leather_"),
	STONE("stone", "stone_"),
	COPPER("copper", "copper_"),
	CHAINMAIL("chainmail", "chainmail_"),
	IRON("iron", "iron_"),
	GOLD("gold", "golden_"),
	DIAMOND("diamond", "diamond_"),
	NETHERITE("netherite", "netherite_");

	public final String key;
	private final String prefix;

	Material(String key, String prefix) {
		this.key = key;
		this.prefix = prefix;
	}

	/** Wood and stone only exist as tools. */
	public boolean hasArmor() {
		return this != WOOD && this != STONE;
	}

	/** Leather and chainmail only exist as armor. */
	public boolean hasTools() {
		return this != LEATHER && this != CHAINMAIL;
	}

	/** Chestplate for the settings screen, null without armor. */
	public String armorIcon() {
		return hasArmor() ? "minecraft:" + prefix + "chestplate" : null;
	}

	/** Pickaxe for the settings screen, null without tools. */
	public String toolIcon() {
		return hasTools() ? "minecraft:" + prefix + "pickaxe" : null;
	}

	public static Material of(String itemId) {
		GearItems.Entry entry = GearItems.get(itemId);
		if (entry == null || entry.kind() == GearItems.Kind.OTHER) {
			return null;
		}
		String path = itemId.substring(itemId.indexOf(':') + 1);
		for (Material material : values()) {
			if (path.startsWith(material.prefix)) {
				return material;
			}
		}
		return null;
	}
}
