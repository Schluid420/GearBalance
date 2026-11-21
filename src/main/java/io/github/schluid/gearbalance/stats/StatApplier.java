package io.github.schluid.gearbalance.stats;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.config.VanillaValues;
import io.github.schluid.gearbalance.stats.GearItems.Kind;
import io.github.schluid.gearbalance.stats.GearItems.Values;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

/** Changes the default components of managed items so existing stacks get new values. Fabric calls it on every component build, starting from vanilla. */
public final class StatApplier {
	private StatApplier() {
	}

	// Player base values, the tooltip shows them added to the item
	private static final double BASE_DAMAGE = 1.0;
	private static final double BASE_SPEED = 4.0;

	private static final Identifier MINING_ID = GearBalance.id("mining_bonus");
	private static final Identifier ARMOR_FALLBACK_ID = GearBalance.id("armor");

	public static void register() {
		DefaultItemComponentEvents.MODIFY.register(context ->
				context.modify(item -> entryFor(item) != null,
						(builder, item) -> apply(entryFor(item), item, builder)));
	}

	private static GearItems.Entry entryFor(Item item) {
		return GearItems.get(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	private static void apply(GearItems.Entry entry, Item item, DataComponentMap.Builder builder) {
		DataComponentMap original = item.components();
		ItemAttributeModifiers originalModifiers =
				original.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

		VanillaValues.capture(entry.id(), readValues(original, originalModifiers));

		// The weight is set for the full set, each of the four pieces carries a quarter
		double weight = entry.kind() == Kind.ARMOR ? GearConfig.forItem(MaterialStat.WEIGHT, entry.id()) / 4 : 0;
		if (!GearConfig.enabled || (!GearConfig.hasOverrides(entry.id()) && weight == 0)) {
			return;
		}

		try {
			Double durability = GearConfig.getOverride(entry.id(), Stat.DURABILITY);
			if (durability != null && original.has(DataComponents.MAX_DAMAGE)) {
				builder.set(DataComponents.MAX_DAMAGE, (int) Math.round(durability));
			}
			if (entry.kind() == Kind.OTHER) {
				applyShield(entry.id(), original, builder);
				return;
			}

			String id = entry.id();
			List<ItemAttributeModifiers.Entry> list = new ArrayList<>(originalModifiers.modifiers());
			boolean changed;
			if (entry.kind() == Kind.ARMOR) {
				EquipmentSlotGroup slot = armorSlot(original, originalModifiers);
				Identifier armorId = armorId(originalModifiers);
				changed = apply(list, id, Stat.ARMOR, Attributes.ARMOR, 0, armorId, slot, false)
						| apply(list, id, Stat.TOUGHNESS, Attributes.ARMOR_TOUGHNESS, 0, armorId, slot, false)
						| apply(list, id, Stat.KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE, 0, armorId, slot, false);
				if (weight > 0) {
					// One id per slot, otherwise the pieces would replace each other
					list.add(new ItemAttributeModifiers.Entry(Attributes.MOVEMENT_SPEED,
							new AttributeModifier(weightId(slot),
									-weight / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot));
					changed = true;
				}
			} else {
				EquipmentSlotGroup hand = EquipmentSlotGroup.MAINHAND;
				changed = apply(list, id, Stat.DAMAGE, Attributes.ATTACK_DAMAGE, BASE_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID, hand, true)
						| apply(list, id, Stat.SPEED, Attributes.ATTACK_SPEED, BASE_SPEED, Item.BASE_ATTACK_SPEED_ID, hand, true);
				if (entry.kind() == Kind.TOOL) {
					changed |= apply(list, id, Stat.MINING, Attributes.MINING_EFFICIENCY, 0, MINING_ID, hand, false);
				}
			}
			if (changed) {
				builder.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.copyOf(list)));
			}
		} catch (RuntimeException e) {
			GearBalance.LOGGER.error("Could not change the values of {}", entry.id(), e);
		}
	}

	private static final String WEIGHT_PREFIX = "armor_weight.";

	private static Identifier weightId(EquipmentSlotGroup slot) {
		return GearBalance.id(WEIGHT_PREFIX + slot.getSerializedName());
	}

	/** Axe disable time and blocked damage of the shield. */
	private static void applyShield(String id, DataComponentMap original, DataComponentMap.Builder builder) {
		Double disable = GearConfig.getOverride(id, Stat.SHIELD_DISABLE);
		Double block = GearConfig.getOverride(id, Stat.SHIELD_BLOCK);
		BlocksAttacks blocking = original.get(DataComponents.BLOCKS_ATTACKS);
		if (blocking == null || (disable == null && block == null)) {
			return;
		}
		float scale = disable == null ? blocking.disableCooldownScale()
				: (float) (disable / Stat.VANILLA_SHIELD_DISABLE);
		List<BlocksAttacks.DamageReduction> reductions = blocking.damageReductions();
		if (block != null) {
			float factor = (float) (block / 100.0);
			reductions = reductions.stream()
					.map(r -> new BlocksAttacks.DamageReduction(r.horizontalBlockingAngle(), r.type(), r.base(), factor))
					.toList();
		}
		builder.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(blocking.blockDelaySeconds(), scale, reductions,
				blocking.itemDamage(), blocking.bypassedBy(), blocking.blockSound(), blocking.disableSound()));
	}

	/** Writes one changed stat into the list. Returns false if the stat is still vanilla. */
	private static boolean apply(List<ItemAttributeModifiers.Entry> list, String id, Stat stat, Holder<Attribute> attribute,
			double base, Identifier newId, EquipmentSlotGroup slot, boolean keepZero) {
		Double value = GearConfig.getOverride(id, stat);
		if (value == null) {
			return false;
		}
		setValue(list, attribute, value - base, newId, slot, keepZero);
		return true;
	}

	/** The first modifier of the attribute keeps id, slot and style, others are dropped. keepZero: damage and speed show 0 in the tooltip. */
	private static void setValue(List<ItemAttributeModifiers.Entry> list, Holder<Attribute> attribute, double amount,
			Identifier newId, EquipmentSlotGroup slot, boolean keepZero) {
		int first = -1;
		for (int i = 0; i < list.size(); i++) {
			if (same(list.get(i).attribute(), attribute)) {
				if (first < 0) {
					first = i;
				} else {
					list.remove(i);
					i--;
				}
			}
		}
		boolean remove = !keepZero && Math.abs(amount) < 1.0E-6;
		if (first >= 0) {
			ItemAttributeModifiers.Entry old = list.get(first);
			if (remove) {
				list.remove(first);
			} else {
				AttributeModifier modifier = new AttributeModifier(old.modifier().id(), amount, AttributeModifier.Operation.ADD_VALUE);
				list.set(first, new ItemAttributeModifiers.Entry(old.attribute(), modifier, old.slot(), old.display()));
			}
		} else if (!remove) {
			list.add(new ItemAttributeModifiers.Entry(attribute,
					new AttributeModifier(newId, amount, AttributeModifier.Operation.ADD_VALUE), slot));
		}
	}

	private static boolean same(Holder<Attribute> a, Holder<Attribute> b) {
		return a.unwrapKey().isPresent() && a.unwrapKey().equals(b.unwrapKey());
	}

	/** Vanilla uses one id per armor piece, e.g. "minecraft:armor.chestplate". */
	private static Identifier armorId(ItemAttributeModifiers modifiers) {
		for (ItemAttributeModifiers.Entry e : modifiers.modifiers()) {
			if (same(e.attribute(), Attributes.ARMOR)) {
				return e.modifier().id();
			}
		}
		return ARMOR_FALLBACK_ID;
	}

	private static EquipmentSlotGroup armorSlot(DataComponentMap components, ItemAttributeModifiers modifiers) {
		for (ItemAttributeModifiers.Entry e : modifiers.modifiers()) {
			if (same(e.attribute(), Attributes.ARMOR)) {
				return e.slot();
			}
		}
		Equippable equippable = components.get(DataComponents.EQUIPPABLE);
		return equippable != null ? EquipmentSlotGroup.bySlot(equippable.slot()) : EquipmentSlotGroup.ARMOR;
	}

	/** Same form as the config: damage and speed as the totals shown in the tooltip. */
	private static Values readValues(DataComponentMap components, ItemAttributeModifiers modifiers) {
		Integer durability = components.get(DataComponents.MAX_DAMAGE);
		double damage = 0;
		double speed = 0;
		double mining = 0;
		double armor = 0;
		double toughness = 0;
		double knockback = 0;
		boolean hasDamage = false;
		boolean hasSpeed = false;
		for (ItemAttributeModifiers.Entry e : modifiers.modifiers()) {
			double amount = e.modifier().amount();
			if (same(e.attribute(), Attributes.ATTACK_DAMAGE)) {
				damage += amount;
				hasDamage = true;
			} else if (same(e.attribute(), Attributes.ATTACK_SPEED)) {
				speed += amount;
				hasSpeed = true;
			} else if (same(e.attribute(), Attributes.MINING_EFFICIENCY)) {
				mining += amount;
			} else if (same(e.attribute(), Attributes.ARMOR)) {
				armor += amount;
			} else if (same(e.attribute(), Attributes.ARMOR_TOUGHNESS)) {
				toughness += amount;
			} else if (same(e.attribute(), Attributes.KNOCKBACK_RESISTANCE)) {
				knockback += amount;
			}
		}
		return new Values(durability == null ? 0 : durability,
				hasDamage ? round(BASE_DAMAGE + damage) : 0,
				hasSpeed ? round(BASE_SPEED + speed) : 0,
				round(mining), round(armor), round(toughness), round(knockback));
	}

	// 1.2000000000000002 -> 1.2
	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}
}
