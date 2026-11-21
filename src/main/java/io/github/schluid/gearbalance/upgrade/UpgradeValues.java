package io.github.schluid.gearbalance.upgrade;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one upgrade level adds per material (fixed; mace, trident, bow and crossbow use the settings).
 * Damage, speed, mining, armor, durability = percent of the item's value; toughness and knockback resistance = fixed per piece.
 * Chosen so +3 is just above the next material in the base values.
 */
public final class UpgradeValues {
	private UpgradeValues() {
	}

	public enum Value {
		DAMAGE, SPEED, MINING, TOOL_DURABILITY, ARMOR, TOUGHNESS, KNOCKBACK, ARMOR_DURABILITY, ARROW_DAMAGE, DRAW_SPEED
	}

	/** Gear materials that can be upgraded (keys as in GearBalance). */
	public static final List<String> GEAR_MATERIALS = List.of("leather", "copper", "chainmail", "iron", "gold", "diamond", "netherite",
			"titanium");
	/** Weapons without a material tier; each is its own "material". */
	public static final List<String> WEAPONS = List.of("mace", "trident", "bow", "crossbow");
	/** Everything that can be upgraded. */
	public static final List<String> MATERIALS = java.util.stream.Stream.concat(GEAR_MATERIALS.stream(), WEAPONS.stream()).toList();

	/** null = the value does not apply to this material (no weapons or tools). */
	private static final Map<String, Double[]> VALUES = new LinkedHashMap<>();

	static {
		// damage, speed, mining, tool durab. | armor, toughness, knockback, armor durab. | arrow damage, draw speed
		VALUES.put("leather", new Double[] {null, null, null, null, 15.0, 0.0, 0.0, 10.0, null, null});
		VALUES.put("copper", new Double[] {6.0, 3.0, 7.0, 10.0, 7.0, 0.05, 0.005, 10.0, null, null});
		VALUES.put("chainmail", new Double[] {null, null, null, null, 9.0, 0.1, 0.01, 10.0, null, null});
		VALUES.put("iron", new Double[] {5.0, 3.0, 11.0, 10.0, 11.0, 0.125, 0.01, 10.0, null, null});
		VALUES.put("gold", new Double[] {4.5, 3.0, 4.0, 10.0, 12.0, 0.125, 0.01, 10.0, null, null});
		VALUES.put("diamond", new Double[] {11.5, 8.5, 4.0, 10.0, 5.0, 0.175, 0.015, 10.0, null, null});
		VALUES.put("netherite", new Double[] {5.0, 5.0, 5.0, 10.0, 5.0, 0.2, 0.02, 10.0, null, null});
		VALUES.put("titanium", new Double[] {5.0, 5.0, 5.0, 10.0, 5.0, 0.2, 0.02, 10.0, null, null});
	}

	/** Value per upgrade level: percent (e.g. 5 = 5 %) or fixed amount. 0 if it does not apply. */
	public static double get(String material, Value value) {
		if (WEAPONS.contains(material)) {
			// Mace and trident: damage (the trident also when thrown); bow and crossbow: arrow damage and draw speed
			return switch (value) {
				case DAMAGE -> GearConfig.weaponUpgrade(material, "damage");
				case ARROW_DAMAGE -> GearConfig.weaponUpgrade(material, "arrow_damage");
				case DRAW_SPEED -> GearConfig.weaponUpgrade(material, "draw_speed");
				case TOOL_DURABILITY -> GearConfig.weaponUpgrade(material, "durability");
				default -> 0;
			};
		}
		Double[] values = VALUES.get(material);
		Double result = values == null ? null : values[value.ordinal()];
		return result == null ? 0 : result;
	}
}
