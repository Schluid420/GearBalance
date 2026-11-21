package io.github.schluid.gearbalance.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Materials of the materials tab, from the item id ("diamond_sword" -> DIAMOND). Bow, mace, shield have none. Add-ons add more via GearBalanceApi. */
public final class Material {
	public static final Material WOOD = vanilla("wood", "wooden_", false, true);
	public static final Material LEATHER = vanilla("leather", "leather_", true, false);
	public static final Material STONE = vanilla("stone", "stone_", false, true);
	public static final Material COPPER = vanilla("copper", "copper_", true, true);
	public static final Material CHAINMAIL = vanilla("chainmail", "chainmail_", true, false);
	public static final Material IRON = vanilla("iron", "iron_", true, true);
	public static final Material GOLD = vanilla("gold", "golden_", true, true);
	public static final Material DIAMOND = vanilla("diamond", "diamond_", true, true);
	public static final Material NETHERITE = vanilla("netherite", "netherite_", true, true);

	/** All materials in the order of the materials tab. */
	private static final List<Material> ALL = new ArrayList<>(
			List.of(WOOD, LEATHER, STONE, COPPER, CHAINMAIL, IRON, GOLD, DIAMOND, NETHERITE));

	public final String key;
	/** Start of the item ids, with namespace, e.g. "minecraft:golden_". */
	private final String prefix;
	private final boolean armor;
	private final boolean tools;
	/** Translation key of the name. */
	public final String nameKey;
	/** Set bonus kind without a setting, null = the vanilla choice in BonusType. */
	public final String defaultBonus;

	private Material(String key, String prefix, boolean armor, boolean tools, String nameKey, String defaultBonus) {
		this.key = key;
		this.prefix = prefix;
		this.armor = armor;
		this.tools = tools;
		this.nameKey = nameKey;
		this.defaultBonus = defaultBonus;
	}

	private static Material vanilla(String key, String prefix, boolean armor, boolean tools) {
		return new Material(key, "minecraft:" + prefix, armor, tools, "gearbalance.material." + key, null);
	}

	/** Adds an add-on material after another (null = at the end). Only called by GearBalanceApi. */
	public static Material register(String key, String prefix, boolean armor, boolean tools, String nameKey,
			String after, String defaultBonus) {
		if (byKey(key) != null) {
			throw new IllegalArgumentException("Material " + key + " is already registered");
		}
		if (prefix.indexOf(':') < 1 || (!armor && !tools)) {
			throw new IllegalArgumentException("Material " + key + " needs a prefix with namespace and armor or tools");
		}
		Material material = new Material(key, prefix, armor, tools, nameKey, defaultBonus);
		Material previous = after == null ? null : byKey(after);
		ALL.add(previous == null ? ALL.size() : ALL.indexOf(previous) + 1, material);
		return material;
	}

	/** All materials, vanilla and registered ones, in the order of the materials tab. */
	public static List<Material> values() {
		return Collections.unmodifiableList(ALL);
	}

	/** Position in the order of the materials tab. */
	public int position() {
		return ALL.indexOf(this);
	}

	public static Material byKey(String key) {
		for (Material material : ALL) {
			if (material.key.equals(key)) {
				return material;
			}
		}
		return null;
	}

	/** Wood and stone only exist as tools. */
	public boolean hasArmor() {
		return armor;
	}

	/** Leather and chainmail only exist as armor. */
	public boolean hasTools() {
		return tools;
	}

	/** Chestplate for the settings screen, null without armor. */
	public String armorIcon() {
		return hasArmor() ? prefix + "chestplate" : null;
	}

	/** Pickaxe for the settings screen, null without tools. */
	public String toolIcon() {
		return hasTools() ? prefix + "pickaxe" : null;
	}

	public static Material of(String itemId) {
		GearItems.Entry entry = GearItems.get(itemId);
		if (entry == null || entry.kind() == GearItems.Kind.OTHER) {
			return null;
		}
		return byPrefix(itemId);
	}

	/** The material whose prefix fits the item id; the longest prefix wins. */
	static Material byPrefix(String itemId) {
		Material found = null;
		for (Material material : ALL) {
			if (itemId.startsWith(material.prefix) && (found == null || material.prefix.length() > found.prefix.length())) {
				found = material;
			}
		}
		return found;
	}

	@Override
	public String toString() {
		return key;
	}
}
