package io.github.schluid.gearbalance.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All managed items with their vanilla values in 26.3 (damage and speed as tooltip totals).
 * The numbers are only used in the main menu, in a world the real values are read from the game.
 */
public final class GearItems {
	private GearItems() {
	}

	public enum Kind {
		TOOL,
		WEAPON,
		ARMOR,
		/** Bow, shield, shears and so on: durability, plus blocking values for the shield. */
		OTHER
	}

	/** Values that don't apply to the kind are 0. */
	public record Values(int durability, double damage, double speed, double mining, double armor,
			double toughness, double knockback) {
	}

	public record Entry(String id, Kind kind, Values vanilla) {
	}

	private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();

	static {
		add("wooden_sword", Kind.WEAPON, new Values(59, 4, 1.6, 0, 0, 0, 0));
		add("wooden_pickaxe", Kind.TOOL, new Values(59, 2, 1.2, 0, 0, 0, 0));
		add("wooden_axe", Kind.TOOL, new Values(59, 7, 0.8, 0, 0, 0, 0));
		add("wooden_shovel", Kind.TOOL, new Values(59, 2.5, 1, 0, 0, 0, 0));
		add("wooden_hoe", Kind.TOOL, new Values(59, 1, 1, 0, 0, 0, 0));
		add("stone_sword", Kind.WEAPON, new Values(131, 5, 1.6, 0, 0, 0, 0));
		add("stone_pickaxe", Kind.TOOL, new Values(131, 3, 1.2, 0, 0, 0, 0));
		add("stone_axe", Kind.TOOL, new Values(131, 9, 0.8, 0, 0, 0, 0));
		add("stone_shovel", Kind.TOOL, new Values(131, 3.5, 1, 0, 0, 0, 0));
		add("stone_hoe", Kind.TOOL, new Values(131, 1, 2, 0, 0, 0, 0));
		add("copper_sword", Kind.WEAPON, new Values(190, 5, 1.6, 0, 0, 0, 0));
		add("copper_pickaxe", Kind.TOOL, new Values(190, 3, 1.2, 0, 0, 0, 0));
		add("copper_axe", Kind.TOOL, new Values(190, 9, 0.8, 0, 0, 0, 0));
		add("copper_shovel", Kind.TOOL, new Values(190, 3.5, 1, 0, 0, 0, 0));
		add("copper_hoe", Kind.TOOL, new Values(190, 1, 2, 0, 0, 0, 0));
		add("iron_sword", Kind.WEAPON, new Values(250, 6, 1.6, 0, 0, 0, 0));
		add("iron_pickaxe", Kind.TOOL, new Values(250, 4, 1.2, 0, 0, 0, 0));
		add("iron_axe", Kind.TOOL, new Values(250, 9, 0.9, 0, 0, 0, 0));
		add("iron_shovel", Kind.TOOL, new Values(250, 4.5, 1, 0, 0, 0, 0));
		add("iron_hoe", Kind.TOOL, new Values(250, 1, 3, 0, 0, 0, 0));
		add("golden_sword", Kind.WEAPON, new Values(32, 4, 1.6, 0, 0, 0, 0));
		add("golden_pickaxe", Kind.TOOL, new Values(32, 2, 1.2, 0, 0, 0, 0));
		add("golden_axe", Kind.TOOL, new Values(32, 7, 1, 0, 0, 0, 0));
		add("golden_shovel", Kind.TOOL, new Values(32, 2.5, 1, 0, 0, 0, 0));
		add("golden_hoe", Kind.TOOL, new Values(32, 1, 1, 0, 0, 0, 0));
		add("diamond_sword", Kind.WEAPON, new Values(1561, 7, 1.6, 0, 0, 0, 0));
		add("diamond_pickaxe", Kind.TOOL, new Values(1561, 5, 1.2, 0, 0, 0, 0));
		add("diamond_axe", Kind.TOOL, new Values(1561, 9, 1, 0, 0, 0, 0));
		add("diamond_shovel", Kind.TOOL, new Values(1561, 5.5, 1, 0, 0, 0, 0));
		add("diamond_hoe", Kind.TOOL, new Values(1561, 1, 4, 0, 0, 0, 0));
		add("netherite_sword", Kind.WEAPON, new Values(2031, 8, 1.6, 0, 0, 0, 0));
		add("netherite_pickaxe", Kind.TOOL, new Values(2031, 6, 1.2, 0, 0, 0, 0));
		add("netherite_axe", Kind.TOOL, new Values(2031, 10, 1, 0, 0, 0, 0));
		add("netherite_shovel", Kind.TOOL, new Values(2031, 6.5, 1, 0, 0, 0, 0));
		add("netherite_hoe", Kind.TOOL, new Values(2031, 1, 4, 0, 0, 0, 0));
		add("mace", Kind.WEAPON, new Values(500, 6, 0.6, 0, 0, 0, 0));
		add("trident", Kind.WEAPON, new Values(250, 9, 1.1, 0, 0, 0, 0));
		add("bow", Kind.OTHER, new Values(384, 0, 0, 0, 0, 0, 0));
		add("crossbow", Kind.OTHER, new Values(465, 0, 0, 0, 0, 0, 0));
		add("shears", Kind.OTHER, new Values(238, 0, 0, 0, 0, 0, 0));
		add("flint_and_steel", Kind.OTHER, new Values(64, 0, 0, 0, 0, 0, 0));
		add("fishing_rod", Kind.OTHER, new Values(64, 0, 0, 0, 0, 0, 0));
		add("carrot_on_a_stick", Kind.OTHER, new Values(25, 0, 0, 0, 0, 0, 0));
		add("warped_fungus_on_a_stick", Kind.OTHER, new Values(100, 0, 0, 0, 0, 0, 0));
		add("brush", Kind.OTHER, new Values(64, 0, 0, 0, 0, 0, 0));
		add("shield", Kind.OTHER, new Values(336, 0, 0, 0, 0, 0, 0));
		add("leather_helmet", Kind.ARMOR, new Values(55, 0, 0, 0, 1, 0, 0));
		add("leather_chestplate", Kind.ARMOR, new Values(80, 0, 0, 0, 3, 0, 0));
		add("leather_leggings", Kind.ARMOR, new Values(75, 0, 0, 0, 2, 0, 0));
		add("leather_boots", Kind.ARMOR, new Values(65, 0, 0, 0, 1, 0, 0));
		add("copper_helmet", Kind.ARMOR, new Values(121, 0, 0, 0, 2, 0, 0));
		add("copper_chestplate", Kind.ARMOR, new Values(176, 0, 0, 0, 4, 0, 0));
		add("copper_leggings", Kind.ARMOR, new Values(165, 0, 0, 0, 3, 0, 0));
		add("copper_boots", Kind.ARMOR, new Values(143, 0, 0, 0, 1, 0, 0));
		add("chainmail_helmet", Kind.ARMOR, new Values(165, 0, 0, 0, 2, 0, 0));
		add("chainmail_chestplate", Kind.ARMOR, new Values(240, 0, 0, 0, 5, 0, 0));
		add("chainmail_leggings", Kind.ARMOR, new Values(225, 0, 0, 0, 4, 0, 0));
		add("chainmail_boots", Kind.ARMOR, new Values(195, 0, 0, 0, 1, 0, 0));
		add("iron_helmet", Kind.ARMOR, new Values(165, 0, 0, 0, 2, 0, 0));
		add("iron_chestplate", Kind.ARMOR, new Values(240, 0, 0, 0, 6, 0, 0));
		add("iron_leggings", Kind.ARMOR, new Values(225, 0, 0, 0, 5, 0, 0));
		add("iron_boots", Kind.ARMOR, new Values(195, 0, 0, 0, 2, 0, 0));
		add("golden_helmet", Kind.ARMOR, new Values(77, 0, 0, 0, 2, 0, 0));
		add("golden_chestplate", Kind.ARMOR, new Values(112, 0, 0, 0, 5, 0, 0));
		add("golden_leggings", Kind.ARMOR, new Values(105, 0, 0, 0, 3, 0, 0));
		add("golden_boots", Kind.ARMOR, new Values(91, 0, 0, 0, 1, 0, 0));
		add("diamond_helmet", Kind.ARMOR, new Values(363, 0, 0, 0, 3, 2, 0));
		add("diamond_chestplate", Kind.ARMOR, new Values(528, 0, 0, 0, 8, 2, 0));
		add("diamond_leggings", Kind.ARMOR, new Values(495, 0, 0, 0, 6, 2, 0));
		add("diamond_boots", Kind.ARMOR, new Values(429, 0, 0, 0, 3, 2, 0));
		add("netherite_helmet", Kind.ARMOR, new Values(407, 0, 0, 0, 3, 3, 0.1));
		add("netherite_chestplate", Kind.ARMOR, new Values(592, 0, 0, 0, 8, 3, 0.1));
		add("netherite_leggings", Kind.ARMOR, new Values(555, 0, 0, 0, 6, 3, 0.1));
		add("netherite_boots", Kind.ARMOR, new Values(481, 0, 0, 0, 3, 3, 0.1));
		add("wolf_armor", Kind.ARMOR, new Values(64, 0, 0, 0, 11, 0, 0));
	}

	private static void add(String path, Kind kind, Values vanilla) {
		String id = "minecraft:" + path;
		BY_ID.put(id, new Entry(id, kind, vanilla));
	}

	public static List<Entry> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
	}

	public static Entry get(String id) {
		return BY_ID.get(id);
	}
}
