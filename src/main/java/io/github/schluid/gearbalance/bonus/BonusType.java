package io.github.schluid.gearbalance.bonus;

import io.github.schluid.gearbalance.stats.Material;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** What a set bonus does. The setting is multiplied by perUnit (2 hearts = 4 health points). */
public enum BonusType {
	SPEED("speed", 50, 0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.MOVEMENT_SPEED),
	FALL_DAMAGE("fall_damage", 100, -0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.FALL_DAMAGE_MULTIPLIER),
	SNEAKING("sneaking", 100, 0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.SNEAKING_SPEED),
	JUMP("jump", 50, 0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.JUMP_STRENGTH),
	ARMOR("armor", 10, 1, Operation.ADD_VALUE, Attributes.ARMOR),
	TOUGHNESS("toughness", 10, 1, Operation.ADD_VALUE, Attributes.ARMOR_TOUGHNESS),
	KNOCKBACK_RESISTANCE("knockback_resistance", 100, 0.01, Operation.ADD_VALUE, Attributes.KNOCKBACK_RESISTANCE),
	EXPLOSION_RESISTANCE("explosion_resistance", 100, 0.01, Operation.ADD_VALUE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE),
	HEALTH("health", 10, 2, Operation.ADD_VALUE, Attributes.MAX_HEALTH),
	DAMAGE("damage", 10, 1, Operation.ADD_VALUE, Attributes.ATTACK_DAMAGE),
	ATTACK_SPEED("attack_speed", 50, 0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.ATTACK_SPEED),
	ATTACK_KNOCKBACK("attack_knockback", 5, 1, Operation.ADD_VALUE, Attributes.ATTACK_KNOCKBACK),
	REACH("reach", 3, 1, Operation.ADD_VALUE, Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE),
	MINING("mining", 100, 0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.BLOCK_BREAK_SPEED),
	OXYGEN("oxygen", 3, 1, Operation.ADD_VALUE, Attributes.OXYGEN_BONUS),
	SWIMMING("swimming", 100, 0.01, Operation.ADD_VALUE, Attributes.WATER_MOVEMENT_EFFICIENCY),
	BURNING("burning", 100, -0.01, Operation.ADD_MULTIPLIED_BASE, Attributes.BURNING_TIME),
	LUCK("luck", 10, 1, Operation.ADD_VALUE, Attributes.LUCK);

	public final String key;
	/** Highest strength that can be typed in. */
	public final double max;
	final double perUnit;
	final Operation operation;
	final List<Holder<Attribute>> attributes;

	@SafeVarargs
	BonusType(String key, double max, double perUnit, Operation operation, Holder<Attribute>... attributes) {
		this.key = key;
		this.max = max;
		this.perUnit = perUnit;
		this.operation = operation;
		this.attributes = List.of(attributes);
	}

	public static BonusType defaultFor(Material material) {
		if (material.defaultBonus != null) {
			BonusType type = byKey(material.defaultBonus);
			if (type != null) {
				return type;
			}
		}
		if (material == Material.LEATHER) {
			return SPEED;
		} else if (material == Material.COPPER) {
			return ARMOR;
		} else if (material == Material.CHAINMAIL) {
			return KNOCKBACK_RESISTANCE;
		} else if (material == Material.IRON) {
			return HEALTH;
		} else if (material == Material.GOLD) {
			return LUCK;
		} else if (material == Material.DIAMOND) {
			return TOUGHNESS;
		}
		return BURNING;
	}

	public static BonusType byKey(String key) {
		for (BonusType type : values()) {
			if (type.key.equals(key)) {
				return type;
			}
		}
		return null;
	}

	/** Share of the bonus for the number of worn pieces: half from 2 pieces, full with 4. */
	public static double share(int pieces) {
		return pieces >= 4 ? 1.0 : pieces >= 2 ? 0.5 : 0.0;
	}
}
