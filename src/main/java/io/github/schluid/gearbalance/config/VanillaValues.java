package io.github.schluid.gearbalance.config;

import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.GearItems.Values;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla values read from the game while the item components are built (StatApplier).
 * Before a world was loaded, the table in GearItems is used instead.
 */
public final class VanillaValues {
	private VanillaValues() {
	}

	private static final Map<String, Values> CAPTURED = new ConcurrentHashMap<>();

	public static void capture(String id, Values values) {
		CAPTURED.put(id, values);
	}

	public static Values get(GearItems.Entry entry) {
		return CAPTURED.getOrDefault(entry.id(), entry.vanilla());
	}
}
