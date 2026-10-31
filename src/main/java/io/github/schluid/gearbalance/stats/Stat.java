package io.github.schluid.gearbalance.stats;

import io.github.schluid.gearbalance.stats.GearItems.Kind;
import io.github.schluid.gearbalance.stats.GearItems.Values;
import java.util.List;

/** The values that can be changed. Damage and speed are the totals shown in the tooltip. */
public enum Stat {
	DURABILITY("durability", 1, 10000),
	DAMAGE("damage", 0, 1000),
	SPEED("speed", 0.1, 20),
	MINING("mining", 0, 100),
	ARMOR("armor", 0, 30),
	TOUGHNESS("toughness", 0, 20),
	KNOCKBACK("knockback", 0, 1),
	/** Shield only: seconds the shield stays disabled after an axe hit. */
	SHIELD_DISABLE("shield_disable", 0, 60),
	/** Shield only: share of the damage the shield blocks, in percent. */
	SHIELD_BLOCK("shield_block", 0, 100);

	public static final String SHIELD = "minecraft:shield";
	/** An axe disables a vanilla shield for 5 seconds, and a shield blocks all damage from the front. */
	public static final double VANILLA_SHIELD_DISABLE = 5;
	public static final double VANILLA_SHIELD_BLOCK = 100;

	public final String key;
	public final double min;
	public final double max;

	Stat(String key, double min, double max) {
		this.key = key;
		this.min = min;
		this.max = max;
	}

	public double get(Values v) {
		return switch (this) {
			case DURABILITY -> v.durability();
			case DAMAGE -> v.damage();
			case SPEED -> v.speed();
			case MINING -> v.mining();
			case ARMOR -> v.armor();
			case TOUGHNESS -> v.toughness();
			case KNOCKBACK -> v.knockback();
			case SHIELD_DISABLE -> VANILLA_SHIELD_DISABLE;
			case SHIELD_BLOCK -> VANILLA_SHIELD_BLOCK;
		};
	}

	public boolean appliesTo(GearItems.Entry entry) {
		return switch (this) {
			case SHIELD_DISABLE, SHIELD_BLOCK -> entry.id().equals(SHIELD);
			default -> true;
		};
	}

	public double max(GearItems.Entry entry) {
		return max;
	}

	public double clamp(double value, GearItems.Entry entry) {
		double clamped = Math.max(min, Math.min(max(entry), value));
		return this == DURABILITY ? Math.round(clamped) : clamped;
	}

	/** Columns of the settings screen per item kind. */
	public static List<Stat> forKind(Kind kind) {
		return switch (kind) {
			case TOOL -> List.of(DURABILITY, DAMAGE, SPEED, MINING);
			case WEAPON -> List.of(DURABILITY, DAMAGE, SPEED);
			case ARMOR -> List.of(DURABILITY, ARMOR, TOUGHNESS, KNOCKBACK);
			case OTHER -> List.of(DURABILITY, SHIELD_DISABLE, SHIELD_BLOCK);
		};
	}
}
