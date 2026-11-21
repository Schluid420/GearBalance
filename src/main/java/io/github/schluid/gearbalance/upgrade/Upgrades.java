package io.github.schluid.gearbalance.upgrade;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Upgrades +1 to +3, stored in "gearbalance:upgrade". Values are computed each time from the item's values and UpgradeValues
 * (percent per level; toughness and knockback resistance fixed per level). Spears are weapons.
 * Mace, trident, bow and crossbow are their own "material" with own upgrade item (breeze rod, prismarine crystals, compressed iron ingot).
 */
public final class Upgrades {
	private Upgrades() {
	}

	public static final int MAX_LEVEL = 3;

	/** Materials that can be upgraded, as keys of GearBalance. */
	public static final List<String> MATERIALS = UpgradeValues.MATERIALS;
	/** Gear pieces that can be upgraded, the last part of the item id. */
	public static final Set<String> PIECES = Set.of("helmet", "chestplate", "leggings", "boots",
			"sword", "spear", "pickaxe", "axe", "shovel", "hoe");

	/** Weapons without a material tier -> their key in UpgradeValues. */
	private static final Map<Item, String> WEAPONS = Map.of(Items.MACE, "mace", Items.TRIDENT, "trident",
			Items.BOW, "bow", Items.CROSSBOW, "crossbow");

	/** The upgrade level. Registered in onInitialize, null before that. */
	public static DataComponentType<Integer> UPGRADE;

	/** "material|piece" -> item, built on first use (all items are registered by then). */
	private static Map<String, Item> itemsByPiece;
	/** Modifiers per item and level, only for the item's own default modifiers. */
	private static final Map<Item, ItemAttributeModifiers[]> CACHE = new ConcurrentHashMap<>();

	public static void register() {
		UPGRADE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "upgrade"),
				DataComponentType.<Integer>builder().persistent(Codec.intRange(1, MAX_LEVEL))
						.networkSynchronized(ByteBufCodecs.VAR_INT).build());
	}

	/** Forget worked out values, e.g. after the table was changed. */
	public static void clearCache() {
		CACHE.clear();
	}

	// ---------------------------------------------------------------- level

	/** Stored level, 0 = not upgraded, regardless of the switches. */
	public static int storedLevel(ItemStack stack) {
		if (UPGRADE == null || stack.isEmpty()) {
			return 0;
		}
		Integer level = stack.get(UPGRADE);
		return level == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** Level that counts right now: 0 while the switch or the mod is off. */
	public static int level(ItemStack stack) {
		return GearConfig.upgradesActive() ? storedLevel(stack) : 0;
	}

	// ---------------------------------------------------------------- items

	private static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	/** Last part of the item id ("sword" for "minecraft:iron_sword"), null if it is no upgradable piece. */
	public static String piece(Item item) {
		if (Shields.isShield(item)) {
			return "shield";
		}
		String weapon = WEAPONS.get(item);
		if (weapon != null) {
			return weapon;
		}
		String id = id(item);
		String piece = id.substring(id.lastIndexOf('_') + 1);
		return PIECES.contains(piece) ? piece : null;
	}

	private static synchronized Map<String, Item> itemsByPiece() {
		if (itemsByPiece == null) {
			Map<String, Item> map = new HashMap<>();
			for (Item item : BuiltInRegistries.ITEM) {
				String material = GearBalanceApi.materialOf(id(item));
				String piece = material == null ? null : piece(item);
				if (piece != null) {
					map.put(material + "|" + piece, item);
				}
			}
			itemsByPiece = map;
		}
		return itemsByPiece;
	}

	/** All pieces of a material that can be upgraded, the GearBalance shield of the material included. */
	public static List<Item> itemsOf(String material) {
		List<Item> items = new ArrayList<>();
		if (UpgradeValues.WEAPONS.contains(material)) {
			WEAPONS.forEach((item, key) -> {
				if (key.equals(material)) {
					items.add(item);
				}
			});
			return items;
		}
		if (Shields.of(material) != null) {
			items.add(Shields.of(material));
		}
		for (String piece : PIECES) {
			Item item = itemsByPiece().get(material + "|" + piece);
			if (item != null) {
				items.add(item);
			}
		}
		return items;
	}

	// ---------------------------------------------------------------- values

	/** Upgrade "material" of an item: the shield or GearBalance material, or the weapon key. */
	public static String material(Item item) {
		String weapon = WEAPONS.get(item);
		if (weapon != null) {
			return weapon;
		}
		String shield = Shields.material(item);
		return shield != null ? shield : GearBalanceApi.materialOf(id(item));
	}

	/** Item for the right slot of the smithing table, null if the material can't be upgraded. */
	public static Item upgradeItem(String material) {
		return switch (material) {
			case "mace" -> Items.BREEZE_ROD;
			case "trident" -> Items.PRISMARINE_CRYSTALS;
			case "bow", "crossbow" -> CompressedItems.IRON;
			default -> CompressedItems.BY_MATERIAL.get(material);
		};
	}

	/** Factor for non-attribute values (arrow damage, draw speed): 1 + percent per level; 1 if not upgraded or switch off. */
	public static double factor(ItemStack stack, UpgradeValues.Value value) {
		int level = level(stack);
		if (level == 0) {
			return 1;
		}
		String material = material(stack.getItem());
		return material == null ? 1 : 1 + UpgradeValues.get(material, value) / 100.0 * level;
	}

	/** Durability with the upgrade. */
	public static int maxDamage(ItemStack stack, int maxDamage) {
		int level = level(stack);
		if (level == 0 || maxDamage <= 0) {
			return maxDamage;
		}
		boolean armor = stack.getItem().components().has(DataComponents.EQUIPPABLE);
		double percent = UpgradeValues.get(material(stack.getItem()),
				armor ? UpgradeValues.Value.ARMOR_DURABILITY : UpgradeValues.Value.TOOL_DURABILITY);
		return (int) Math.round(maxDamage * (1 + percent / 100.0 * level));
	}

	/** Attribute modifiers with the upgrade. "original" is what the stack would use without it. */
	public static ItemAttributeModifiers modifiers(ItemStack stack, ItemAttributeModifiers original) {
		int level = level(stack);
		if (level == 0) {
			return original;
		}
		Item item = stack.getItem();
		ItemAttributeModifiers defaults = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (original != defaults) {
			// Changed by a command or another mod: work it out every time
			return compute(item, original, level);
		}
		ItemAttributeModifiers[] perLevel = CACHE.computeIfAbsent(item, k -> new ItemAttributeModifiers[MAX_LEVEL + 1]);
		if (perLevel[level] == null) {
			perLevel[level] = compute(item, original, level);
		}
		return perLevel[level];
	}

	private static ItemAttributeModifiers compute(Item item, ItemAttributeModifiers original, int level) {
		String material = material(item);
		if (material == null || !MATERIALS.contains(material)) {
			return original;
		}
		Map<Holder<Attribute>, Double> deltas = new java.util.LinkedHashMap<>();
		// Damage and speed: percent of the total from the tooltip (player base 1 and 4 included)
		addPercent(deltas, original, Attributes.ATTACK_DAMAGE, 1.0, UpgradeValues.get(material, UpgradeValues.Value.DAMAGE), level);
		addPercent(deltas, original, Attributes.ATTACK_SPEED, 4.0, UpgradeValues.get(material, UpgradeValues.Value.SPEED), level);
		addPercent(deltas, original, Attributes.ARMOR, 0.0, UpgradeValues.get(material, UpgradeValues.Value.ARMOR), level);

		double mining = miningSpeed(item, original);
		double miningPercent = UpgradeValues.get(material, UpgradeValues.Value.MINING);
		if (mining > 0 && miningPercent > 0) {
			deltas.put(Attributes.MINING_EFFICIENCY, mining * miningPercent / 100.0 * level);
		}
		// Toughness and knockback resistance: fixed amount, only for armor
		if (sum(original, Attributes.ARMOR) > 0) {
			double toughness = UpgradeValues.get(material, UpgradeValues.Value.TOUGHNESS) * level;
			double knockback = UpgradeValues.get(material, UpgradeValues.Value.KNOCKBACK) * level;
			if (toughness > 0) {
				deltas.put(Attributes.ARMOR_TOUGHNESS, toughness);
			}
			if (knockback > 0) {
				deltas.put(Attributes.KNOCKBACK_RESISTANCE, knockback);
			}
		}
		return apply(original, deltas, slotGroup(item, original), "upgrade." + piece(item) + ".");
	}

	/** Adds percent of the total, only if the item has this value at all. */
	private static void addPercent(Map<Holder<Attribute>, Double> deltas, ItemAttributeModifiers modifiers,
			Holder<Attribute> attribute, double base, double percent, int level) {
		double own = sum(modifiers, attribute);
		if (own != 0 && percent > 0) {
			deltas.put(attribute, (base + own) * percent / 100.0 * level);
		}
	}
	/**
	 * Adds the deltas to the first matching modifier (one total in the tooltip) or as a new one.
	 * New ids: "gearbalance:" + prefix + attribute; the prefix holds the piece ("upgrade.helmet."), as equal ids on two pieces count once.
	 */
	public static ItemAttributeModifiers apply(ItemAttributeModifiers original, Map<Holder<Attribute>, Double> deltas,
			EquipmentSlotGroup slot, String prefix) {
		List<ItemAttributeModifiers.Entry> entries = new ArrayList<>();
		Map<Holder<Attribute>, Double> left = new java.util.LinkedHashMap<>(deltas);
		for (ItemAttributeModifiers.Entry entry : original.modifiers()) {
			Double delta = left.get(entry.attribute());
			AttributeModifier modifier = entry.modifier();
			if (delta != null && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				left.remove(entry.attribute());
				entries.add(new ItemAttributeModifiers.Entry(entry.attribute(),
						new AttributeModifier(modifier.id(), modifier.amount() + delta, modifier.operation()),
						entry.slot(), entry.display()));
			} else {
				entries.add(entry);
			}
		}
		left.forEach((attribute, delta) -> entries.add(new ItemAttributeModifiers.Entry(attribute,
				new AttributeModifier(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID,
						prefix + attribute.unwrapKey().map(key -> key.identifier().getPath()).orElse("value")),
						delta, AttributeModifier.Operation.ADD_VALUE),
				slot)));
		return new ItemAttributeModifiers(List.copyOf(entries));
	}

	private static double sum(ItemAttributeModifiers modifiers, Holder<Attribute> attribute) {
		double total = 0;
		if (modifiers == null) {
			return 0;
		}
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().equals(attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}

	/** Mining speed on the right blocks plus the mining bonus; 0 for items that are no tools. */
	private static final java.util.Set<String> MINING_PIECES = java.util.Set.of("pickaxe", "axe", "shovel", "hoe");

	private static double miningSpeed(Item item, ItemAttributeModifiers modifiers) {
		Tool tool = item.components().get(DataComponents.TOOL);
		// Swords and spears have tool rules too (cobweb, bamboo), but are no mining tools
		if (tool == null || !MINING_PIECES.contains(piece(item))) {
			return 0;
		}
		double speed = 0;
		for (Tool.Rule rule : tool.rules()) {
			// Instant-mine rules use Float.MAX_VALUE as speed
			if (rule.speed().isPresent() && rule.speed().get() < 1000) {
				speed = Math.max(speed, rule.speed().get());
			}
		}
		return speed <= 0 ? 0 : speed + sum(modifiers, Attributes.MINING_EFFICIENCY);
	}

	/** Slot of the item's own modifiers; armor without modifiers uses its equipment slot; shields count in both hands. */
	public static EquipmentSlotGroup slotGroup(Item item, ItemAttributeModifiers modifiers) {
		if (Shields.isShield(item)) {
			return EquipmentSlotGroup.HAND;
		}
		if (modifiers != null && !modifiers.modifiers().isEmpty()) {
			return modifiers.modifiers().get(0).slot();
		}
		Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
		return equippable != null ? EquipmentSlotGroup.bySlot(equippable.slot()) : EquipmentSlotGroup.MAINHAND;
	}
}
