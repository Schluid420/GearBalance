package io.github.schluid.gearbalance.api;

import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.stats.GearItems;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.stats.MaterialStat;
import io.github.schluid.gearbalance.stats.Stat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for add-ons. The values of tools, weapons and armor are
 * not offered as numbers here: GearBalance writes them into the items at start, so other mods can read
 * them from the item itself.
 *
 * Add-ons with their own gear register it in a GearBalanceAddon entrypoint. Without add-ons GearBalance
 * works exactly as before. Values saved for an add-on stay in the config when it is removed for a while.
 *
 * Other mods should only call this when FabricLoader reports "gearbalance" as loaded.
 * The method names will stay the same in later versions.
 */
public final class GearBalanceApi {
	private GearBalanceApi() {
	}

	/** Can raise the level an item stack needs, e.g. for an upgraded piece. */
	@FunctionalInterface
	public interface LevelModifier {
		/**
		 * @param stack the item stack, never empty
		 * @param required the level GearBalance asks for (material level, 0 = none)
		 * @return the level that should be needed; lower values than "required" are ignored
		 */
		int required(ItemStack stack, int required);
	}

	/**
	 * Called after a preset or HM+ profile was loaded, so an add-on can set values for its own
	 * materials and items. Keys: "balanced" (VanillaPlus), "progression", "challenge" and the HM+
	 * profiles "relaxed", "tougher", "veteran", "nightmare", "realism". HM+ profiles start from a
	 * preset, so the hook first runs for that preset and then for the HM+ profile.
	 */
	@FunctionalInterface
	public interface PresetHook {
		void apply(String presetKey);
	}

	private static final List<LevelModifier> LEVEL_MODIFIERS = new CopyOnWriteArrayList<>();
	private static final List<PresetHook> PRESET_HOOKS = new CopyOnWriteArrayList<>();
	private static boolean registrationOpen;

	// ---------------------------------------------------------------- reading

	/** Whether GearBalance is switched on in its options. */
	public static boolean isEnabled() {
		return GearConfig.enabled;
	}

	/**
	 * Level the material of an item needs in the active settings, e.g. "minecraft:iron_sword".
	 * 0 = no requirement or the mod is off.
	 */
	public static int materialLevel(String itemId) {
		return GearConfig.enabled ? GearConfig.requiredLevel(itemId) : 0;
	}

	/** Keys of all materials in the order of the materials tab, vanilla and registered ones. */
	public static List<String> materialKeys() {
		return Material.values().stream().map(material -> material.key).toList();
	}

	/** Key of the material of an item ("iron" for "minecraft:iron_sword"), null if it has none. */
	public static String materialOf(String itemId) {
		Material material = Material.of(itemId);
		return material == null ? null : material.key;
	}

	// ---------------------------------------------------------------- levels

	/** Registers a level modifier. Call once in onInitialize. */
	public static void registerLevelModifier(LevelModifier modifier) {
		LEVEL_MODIFIERS.add(modifier);
	}

	/** Used by GearBalance itself: the highest level asked for by the registered modifiers. */
	public static int applyLevelModifiers(ItemStack stack, int required) {
		int result = required;
		for (LevelModifier modifier : LEVEL_MODIFIERS) {
			result = Math.max(result, modifier.required(stack, required));
		}
		return result;
	}

	// ---------------------------------------------------------------- new gear (only in GearBalanceAddon)

	/**
	 * Registers a material, e.g. ("steel", "mymod:steel_", "mymod.material.steel",
	 * true, true, "iron", "knockback_resistance"). It appears in the materials and set bonus tabs right
	 * after the material "after" (null = at the end) and gets level, repair and weight like the others.
	 *
	 * @param itemPrefix start of the item ids with namespace; the icons are prefix + "chestplate" / "pickaxe"
	 * @param nameKey translation key of the name, the add-on brings the translation
	 * @param defaultBonus key of the set bonus kind without a setting (see BonusType), null = burning
	 */
	public static void registerMaterial(String key, String itemPrefix, String nameKey, boolean armor, boolean tools,
			String after, String defaultBonus) {
		checkOpen();
		if (defaultBonus != null && BonusType.byKey(defaultBonus) == null) {
			throw new IllegalArgumentException("Unknown set bonus " + defaultBonus);
		}
		Material.register(key, itemPrefix, armor, tools, nameKey, after, defaultBonus);
	}

	/**
	 * Registers an item of a registered material with its default values, which are only shown in the
	 * main menu (in a world the real values are read from the game). Kind: "tool", "weapon" or "armor".
	 * Damage and speed are the totals from the tooltip, values that don't fit the kind are 0.
	 */
	public static void registerItem(String id, String kind, int durability, double damage, double speed,
			double mining, double armor, double toughness, double knockback) {
		checkOpen();
		GearItems.Kind itemKind = switch (kind) {
			case "tool" -> GearItems.Kind.TOOL;
			case "weapon" -> GearItems.Kind.WEAPON;
			case "armor" -> GearItems.Kind.ARMOR;
			default -> throw new IllegalArgumentException("Unknown kind " + kind);
		};
		GearItems.register(id, itemKind,
				new GearItems.Values(durability, damage, speed, mining, armor, toughness, knockback));
	}

	/** Registers a hook that sets the add-on's values when a preset is loaded. */
	public static void registerPresetHook(PresetHook hook) {
		checkOpen();
		PRESET_HOOKS.add(hook);
	}

	// ---------------------------------------------------------------- values (for preset hooks)

	/** Material value: stat "level", "repair" or "weight". */
	public static double getMaterialValue(String materialKey, String stat) {
		return GearConfig.getMaterial(materialStat(stat), material(materialKey));
	}

	public static void setMaterialValue(String materialKey, String stat, double value) {
		GearConfig.setMaterial(materialStat(stat), material(materialKey), value);
	}

	/** Item value as used now: stat "durability", "damage", "speed", "mining", "armor", "toughness", "knockback". */
	public static double getItemValue(String itemId, String stat) {
		return GearConfig.getEffective(entry(itemId), itemStat(stat));
	}

	public static void setItemValue(String itemId, String stat, double value) {
		GearConfig.setOverride(entry(itemId), itemStat(stat), value);
	}

	/** Set bonus of a material: bonus kind key (see BonusType) and strength with 4 pieces. */
	public static void setSetBonus(String materialKey, String bonus, double strength) {
		BonusType type = BonusType.byKey(bonus);
		if (type == null) {
			throw new IllegalArgumentException("Unknown set bonus " + bonus);
		}
		GearConfig.setSet(material(materialKey), type, strength);
	}

	// ---------------------------------------------------------------- used by GearBalance itself

	/** Opens and closes the registration around the GearBalanceAddon entrypoints. */
	public static void setRegistrationOpen(boolean open) {
		registrationOpen = open;
	}

	/** Runs the preset hooks after a preset or HM+ profile was applied. */
	public static void runPresetHooks(String presetKey) {
		for (PresetHook hook : PRESET_HOOKS) {
			hook.apply(presetKey);
		}
	}

	private static void checkOpen() {
		if (!registrationOpen) {
			throw new IllegalStateException("GearBalance gear can only be registered in a GearBalanceAddon entrypoint");
		}
	}

	private static Material material(String key) {
		Material material = Material.byKey(key);
		if (material == null) {
			throw new IllegalArgumentException("Unknown material " + key);
		}
		return material;
	}

	private static GearItems.Entry entry(String itemId) {
		GearItems.Entry entry = GearItems.get(itemId);
		if (entry == null) {
			throw new IllegalArgumentException("Unknown item " + itemId);
		}
		return entry;
	}

	private static MaterialStat materialStat(String key) {
		for (MaterialStat stat : MaterialStat.values()) {
			if (stat.key.equals(key)) {
				return stat;
			}
		}
		throw new IllegalArgumentException("Unknown material value " + key);
	}

	private static Stat itemStat(String key) {
		for (Stat stat : Stat.values()) {
			if (stat.key.equals(key)) {
				return stat;
			}
		}
		throw new IllegalArgumentException("Unknown item value " + key);
	}
}
