package io.github.schluid.gearbalance.socket;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts of cut gems: each has a prefix (WotLK adjectives, "bold" = "Bold Ruby"), one bonus (aquamarine and onyx two) and goes
 * into weapons/tools or armor/shields. Themes: ruby fight and life, sapphire protection and water, topaz work and skill, emerald movement and nature.
 */
public final class GemSorts {
	private GemSorts() {
	}

	/** One sort: key (part of the item id), gem type, bonuses (GemBonuses ids) and where it goes. */
	public record Sort(String key, String type, List<String> bonuses, boolean armor) {
		/** True for the sorts with two bonuses (aquamarine and onyx). */
		public boolean pair() {
			return bonuses.size() > 1;
		}
	}

	public static final List<Sort> ALL = List.of(
			new Sort("bold", "ruby", List.of("attack_damage"), false),
			new Sort("fierce", "ruby", List.of("attack_knockback"), false),
			new Sort("flashing", "ruby", List.of("sweeping_damage"), false),
			new Sort("solid", "ruby", List.of("max_health"), true),
			new Sort("bright", "ruby", List.of("max_absorption"), true),
			new Sort("glowing", "ruby", List.of("fire_resistance"), true),
			new Sort("stalwart", "sapphire", List.of("armor"), true),
			new Sort("durable", "sapphire", List.of("armor_toughness"), true),
			new Sort("steady", "sapphire", List.of("knockback_resistance"), true),
			new Sort("thick", "sapphire", List.of("explosion_knockback_resistance"), true),
			new Sort("misty", "sapphire", List.of("oxygen_bonus"), true),
			new Sort("lustrous", "sapphire", List.of("water_movement_efficiency"), true),
			new Sort("stormy", "sapphire", List.of("frost_resistance"), true),
			new Sort("quick", "topaz", List.of("attack_speed"), false),
			new Sort("precise", "topaz", List.of("mining_efficiency"), false),
			new Sort("turbid", "topaz", List.of("submerged_mining_speed"), false),
			new Sort("deft", "topaz", List.of("reach"), false),
			new Sort("glinting", "topaz", List.of("luck"), false),
			new Sort("purified", "topaz", List.of("burning_time"), true),
			new Sort("vivid", "emerald", List.of("movement_speed"), true),
			new Sort("energized", "emerald", List.of("jump_strength"), true),
			new Sort("jagged", "emerald", List.of("step_height"), true),
			new Sort("delicate", "emerald", List.of("safe_fall_distance"), true),
			new Sort("subtle", "emerald", List.of("sneaking_speed"), true),
			new Sort("enduring", "emerald", List.of("poison_resistance"), true),
			new Sort("shining", "aquamarine", List.of("oxygen_bonus", "water_movement_efficiency"), true),
			new Sort("intricate", "aquamarine", List.of("movement_speed", "jump_strength"), true),
			new Sort("shifting", "aquamarine", List.of("armor", "frost_resistance"), true),
			new Sort("seers", "aquamarine", List.of("submerged_mining_speed", "luck"), false),
			new Sort("lambent", "aquamarine", List.of("safe_fall_distance", "step_height"), true),
			new Sort("opaque", "aquamarine", List.of("sneaking_speed", "poison_resistance"), true),
			new Sort("dazzling", "aquamarine", List.of("water_movement_efficiency", "movement_speed"), true),
			new Sort("radiant", "aquamarine", List.of("reach", "mining_efficiency"), false),
			new Sort("swift", "aquamarine", List.of("movement_speed", "step_height"), true),
			new Sort("nimble", "aquamarine", List.of("jump_strength", "safe_fall_distance"), true),
			new Sort("balanced", "aquamarine", List.of("fire_resistance", "frost_resistance"), true),
			new Sort("veiled", "aquamarine", List.of("sneaking_speed", "movement_speed"), true),
			new Sort("lucent", "aquamarine", List.of("mining_efficiency", "luck"), false),
			new Sort("deep", "aquamarine", List.of("mining_efficiency", "submerged_mining_speed"), false),
			new Sort("deadly", "onyx", List.of("attack_damage", "attack_speed"), false),
			new Sort("wicked", "onyx", List.of("attack_damage", "attack_knockback"), false),
			new Sort("regal", "onyx", List.of("max_health", "armor"), true),
			new Sort("infused", "onyx", List.of("fire_resistance", "armor_toughness"), true),
			new Sort("reckless", "onyx", List.of("sweeping_damage", "attack_speed"), false),
			new Sort("sovereign", "onyx", List.of("max_absorption", "max_health"), true),
			new Sort("resplendent", "onyx", List.of("knockback_resistance", "armor"), true),
			new Sort("puissant", "onyx", List.of("attack_damage", "reach"), false),
			new Sort("eternal", "onyx", List.of("armor", "armor_toughness"), true),
			new Sort("hardy", "onyx", List.of("max_health", "armor_toughness"), true),
			new Sort("shielding", "onyx", List.of("max_absorption", "armor"), true),
			new Sort("steadfast", "onyx", List.of("knockback_resistance", "armor_toughness"), true),
			new Sort("brutal", "onyx", List.of("attack_damage", "sweeping_damage"), false),
			new Sort("forceful", "onyx", List.of("attack_knockback", "reach"), false));

	/** All sorts of a gem type, in menu order. */
	public static List<Sort> of(String type) {
		List<Sort> result = new ArrayList<>();
		for (Sort sort : ALL) {
			if (sort.type().equals(type)) {
				result.add(sort);
			}
		}
		return result;
	}
}
