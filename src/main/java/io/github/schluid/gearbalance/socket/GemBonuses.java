package io.github.schluid.gearbalance.socket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Every bonus a gem can give. Each gem type gets one bonus for weapons/tools and one for armor (aquamarine and onyx two each),
 * with a value per cut. Most raise a vanilla attribute; the three resistances are handled in LivingEntityMixin.
 */
public final class GemBonuses {
	private GemBonuses() {
	}

	/** Resistances that are not vanilla attributes. */
	public enum Special {
		NONE, POISON, FROST, FIRE
	}

	/** One bonus. "percent": typed and shown in percent (5 = 5 %); "negative": lowers the attribute (burning time); "max": highest value per gem. */
	public record Bonus(String id, List<Holder<Attribute>> attributes, AttributeModifier.Operation operation,
			boolean percent, boolean negative, Special special, double[] defaults, double max,
			boolean tools, boolean armor) {
		/** The amount as attribute modifier (percent divided by 100, negative bonuses below 0). */
		public double modifierAmount(double value) {
			double amount = percent ? value / 100.0 : value;
			return negative ? -amount : amount;
		}

		public boolean allowed(boolean onArmor) {
			return onArmor ? armor : tools;
		}
	}

	private static final Map<String, Bonus> ALL = new LinkedHashMap<>();

	private static void add(String id, List<Holder<Attribute>> attributes, AttributeModifier.Operation operation,
			boolean percent, boolean negative, double[] defaults, double max, boolean tools, boolean armor) {
		ALL.put(id, new Bonus(id, attributes, operation, percent, negative, Special.NONE, defaults, max, tools, armor));
	}

	private static void special(String id, Special special, double[] defaults) {
		ALL.put(id, new Bonus(id, List.of(), AttributeModifier.Operation.ADD_VALUE, true, false, special, defaults, 25,
				false, true));
	}

	static {
		AttributeModifier.Operation add = AttributeModifier.Operation.ADD_VALUE;
		AttributeModifier.Operation base = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
		// Values per cut (chipped, cut, flawless) and the highest value per gem. Kept small on purpose: a
		// resistance needs many gems for 100 %, nothing gets supernatural.
		// Weapons and tools (some also on armor)
		add("attack_damage", List.of(Attributes.ATTACK_DAMAGE), add, false, false, new double[] {0.25, 0.5, 1.0}, 2, true, false);
		add("attack_speed", List.of(Attributes.ATTACK_SPEED), add, false, false, new double[] {0.05, 0.1, 0.15}, 0.3, true, false);
		add("mining_efficiency", List.of(Attributes.MINING_EFFICIENCY), add, false, false, new double[] {0.5, 1.0, 1.5}, 3, true, false);
		add("attack_knockback", List.of(Attributes.ATTACK_KNOCKBACK), add, false, false, new double[] {0.1, 0.2, 0.4}, 1, true, false);
		add("sweeping_damage", List.of(Attributes.SWEEPING_DAMAGE_RATIO), add, true, false, new double[] {2.5, 5, 10}, 25, true, false);
		add("reach", List.of(Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE), add, false, false,
				new double[] {0.25, 0.5, 0.75}, 1.5, true, true);
		add("submerged_mining_speed", List.of(Attributes.SUBMERGED_MINING_SPEED), add, true, false, new double[] {2.5, 5, 10}, 25, true, true);
		add("luck", List.of(Attributes.LUCK), add, false, false, new double[] {0.25, 0.5, 1.0}, 2, true, true);
		add("max_health", List.of(Attributes.MAX_HEALTH), add, false, false, new double[] {1, 2, 3}, 6, false, true);
		// Armor
		add("armor", List.of(Attributes.ARMOR), add, false, false, new double[] {0.25, 0.5, 1.0}, 2, false, true);
		add("armor_toughness", List.of(Attributes.ARMOR_TOUGHNESS), add, false, false, new double[] {0.25, 0.5, 1.0}, 2, false, true);
		add("movement_speed", List.of(Attributes.MOVEMENT_SPEED), base, true, false, new double[] {1, 2, 4}, 8, false, true);
		add("max_absorption", List.of(Attributes.MAX_ABSORPTION), add, false, false, new double[] {0.5, 1, 2}, 4, false, true);
		add("knockback_resistance", List.of(Attributes.KNOCKBACK_RESISTANCE), add, true, false, new double[] {2.5, 5, 10}, 25, false, true);
		add("explosion_knockback_resistance", List.of(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), add, true, false,
				new double[] {2.5, 5, 10}, 25, false, true);
		add("jump_strength", List.of(Attributes.JUMP_STRENGTH), base, true, false, new double[] {1, 2, 4}, 8, false, true);
		add("step_height", List.of(Attributes.STEP_HEIGHT), add, false, false, new double[] {0.05, 0.1, 0.2}, 0.4, false, true);
		add("safe_fall_distance", List.of(Attributes.SAFE_FALL_DISTANCE), add, false, false, new double[] {0.25, 0.5, 1}, 2, false, true);
		add("burning_time", List.of(Attributes.BURNING_TIME), base, true, true, new double[] {2.5, 5, 10}, 25, false, true);
		add("oxygen_bonus", List.of(Attributes.OXYGEN_BONUS), add, false, false, new double[] {0.25, 0.5, 1}, 2, false, true);
		add("water_movement_efficiency", List.of(Attributes.WATER_MOVEMENT_EFFICIENCY), add, true, false,
				new double[] {2.5, 5, 10}, 25, false, true);
		add("sneaking_speed", List.of(Attributes.SNEAKING_SPEED), add, true, false, new double[] {2.5, 5, 10}, 25, false, true);
		// Resistances, handled by GearBalance (armor only)
		special("poison_resistance", Special.POISON, new double[] {2.5, 5, 10});
		special("frost_resistance", Special.FROST, new double[] {2.5, 5, 10});
		special("fire_resistance", Special.FIRE, new double[] {2.5, 5, 10});
	}

	/** All bonuses, in menu order. */
	public static List<Bonus> all() {
		return List.copyOf(ALL.values());
	}

	/** The bonus with this id, null if unknown. */
	public static Bonus get(String id) {
		return id == null ? null : ALL.get(id);
	}

	/** All bonuses usable on armor (true) or on weapons and tools (false), in menu order. */
	public static List<Bonus> choices(boolean onArmor) {
		List<Bonus> result = new ArrayList<>();
		for (Bonus bonus : ALL.values()) {
			if (bonus.allowed(onArmor)) {
				result.add(bonus);
			}
		}
		return result;
	}

	/** Gem types with two bonuses per kind of gear (somewhat weaker each, see GearConfig). */
	public static final List<String> TWO_BONUS_TYPES = List.of("aquamarine", "onyx");

	/** Number of bonuses a gem type gives on one kind of gear: 1, or 2 for the rare gems. */
	public static int slots(String type) {
		return TWO_BONUS_TYPES.contains(type) ? 2 : 1;
	}

	/** Bonus a gem type gives without any change in the settings (slot 0 or 1 for the rare gems). */
	public static String standard(String type, boolean onArmor, int slot) {
		return switch (type) {
			case "ruby" -> onArmor ? "max_health" : "attack_damage";
			case "sapphire" -> onArmor ? "armor" : "attack_speed";
			case "topaz" -> onArmor ? "armor_toughness" : "mining_efficiency";
			case "aquamarine" -> onArmor ? (slot == 0 ? "oxygen_bonus" : "water_movement_efficiency")
					: (slot == 0 ? "submerged_mining_speed" : "luck");
			case "onyx" -> onArmor ? (slot == 0 ? "fire_resistance" : "armor_toughness")
					: (slot == 0 ? "attack_damage" : "attack_knockback");
			default -> onArmor ? "movement_speed" : "reach";
		};
	}
}
