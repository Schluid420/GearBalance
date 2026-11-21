package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Sockets on gear, stored in "gearbalance:sockets": one entry per socket, empty text = empty, else the gem's item id
 * ("gearbalance:cut_ruby"). Socket count per material and the rising amethyst cost are set in GearConfig.
 */
public final class Sockets {
	private Sockets() {
	}

	public static final int MAX_SOCKETS = GearConfig.MAX_SOCKETS;

	/** The sockets. Registered in onInitialize, null before that. */
	public static DataComponentType<List<String>> SOCKETS;

	public static void register() {
		SOCKETS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "sockets"),
				DataComponentType.<List<String>>builder()
						.persistent(Codec.STRING.sizeLimitedListOf(MAX_SOCKETS))
						.networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_SOCKETS)))
						.build());
	}

	/** The socket list, empty if the piece has none. */
	public static List<String> sockets(ItemStack stack) {
		if (SOCKETS == null || stack.isEmpty()) {
			return List.of();
		}
		List<String> list = stack.get(SOCKETS);
		return list == null ? List.of() : list;
	}

	public static int count(ItemStack stack) {
		return sockets(stack).size();
	}

	/** Material key of a gear piece that can get sockets, null otherwise ("wood" for the vanilla shield). */
	public static String material(ItemStack stack) {
		if (stack.isEmpty() || Upgrades.piece(stack.getItem()) == null) {
			return null;
		}
		String shield = Shields.material(stack.getItem());
		if (shield != null) {
			return shield;
		}
		String material = GearBalanceApi.materialOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		return material != null && GearConfig.SOCKETS_PER_MATERIAL.containsKey(material) ? material : null;
	}

	/** Most sockets the piece may have, 0 if it can't get any. Shields have their own numbers. */
	public static int max(ItemStack stack) {
		String material = material(stack);
		if (material == null) {
			return 0;
		}
		return (Shields.isShield(stack) ? GearConfig.SHIELD_SOCKETS : GearConfig.SOCKETS_PER_MATERIAL)
				.getOrDefault(material, 0);
	}

	/** Amethyst shards for the next socket. */
	public static int amethystCost(ItemStack stack) {
		return GearConfig.SOCKET_COST[Math.min(count(stack), MAX_SOCKETS - 1)];
	}

	/** Copy of the piece with one more empty socket. */
	public static ItemStack withNewSocket(ItemStack stack) {
		ItemStack result = stack.copy();
		result.setCount(1);
		List<String> list = new ArrayList<>(sockets(stack));
		list.add("");
		result.set(SOCKETS, List.copyOf(list));
		return result;
	}

	/** True if the piece has at least one empty socket. */
	public static boolean hasEmptySocket(ItemStack stack) {
		return sockets(stack).contains("");
	}

	/** True if at least one socket holds a gem. */
	public static boolean hasGems(ItemStack stack) {
		for (String gem : sockets(stack)) {
			if (!gem.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Copy of the piece with the gem in its first empty socket. */
	public static ItemStack withGem(ItemStack stack, ItemStack gem) {
		ItemStack result = stack.copy();
		result.setCount(1);
		List<String> list = new ArrayList<>(sockets(stack));
		int empty = list.indexOf("");
		if (empty >= 0) {
			list.set(empty, Gems.id(gem));
			result.set(SOCKETS, List.copyOf(list));
		}
		return result;
	}

	/** Copy of the piece with the gem in one socket (0 based) removed; the socket stays, empty. */
	public static ItemStack withoutGem(ItemStack stack, int index) {
		ItemStack result = stack.copy();
		List<String> list = new ArrayList<>(sockets(stack));
		if (index >= 0 && index < list.size()) {
			list.set(index, "");
			result.set(SOCKETS, List.copyOf(list));
		}
		return result;
	}

	/** Attribute modifiers with gem bonuses added; "original" = without them (upgrades included). Only with sockets on. */
	public static ItemAttributeModifiers modifiers(ItemStack stack, ItemAttributeModifiers original) {
		if (!active() || !hasGems(stack)) {
			return original;
		}
		boolean armor = stack.getItem().components().has(DataComponents.EQUIPPABLE);
		// Flat bonuses are added to the existing values; percent bonuses (multiplied) get their own entry
		Map<Holder<Attribute>, Double> deltas = new LinkedHashMap<>();
		Map<Holder<Attribute>, Double> multiplied = new LinkedHashMap<>();
		double factor = Titanite.factor(stack);
		for (String id : sockets(stack)) {
			for (GemValues.Bonus bonus : GemValues.on(id, armor)) {
				if (bonus.kind().special() != GemBonuses.Special.NONE) {
					continue;
				}
				double amount = bonus.kind().modifierAmount(bonus.amount(Gems.cutOf(id)) * factor);
				boolean add = bonus.kind().operation() == AttributeModifier.Operation.ADD_VALUE;
				for (Holder<Attribute> attribute : bonus.kind().attributes()) {
					(add ? deltas : multiplied).merge(attribute, amount, Double::sum);
				}
			}
		}
		if (deltas.isEmpty() && multiplied.isEmpty()) {
			return original;
		}
		String piece = Upgrades.piece(stack.getItem());
		String prefix = "gem." + (piece == null ? "item" : piece) + ".";
		EquipmentSlotGroup slot = Upgrades.slotGroup(stack.getItem(), original);
		ItemAttributeModifiers result = deltas.isEmpty() ? original : Upgrades.apply(original, deltas, slot, prefix);
		if (multiplied.isEmpty()) {
			return result;
		}
		List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(result.modifiers());
		multiplied.forEach((attribute, amount) -> entries.add(new ItemAttributeModifiers.Entry(attribute,
				new AttributeModifier(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, prefix + "percent."
						+ attribute.unwrapKey().map(key -> key.identifier().getPath()).orElse("value")),
						amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
				slot)));
		return new ItemAttributeModifiers(List.copyOf(entries));
	}

	/** Resistance from the gems in worn armor and shields, 0 (none) to 1 (full), summed over all pieces. */
	public static double resistance(LivingEntity entity, GemBonuses.Special special) {
		if (!active() || special == GemBonuses.Special.NONE) {
			return 0;
		}
		double percent = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);
			// Worn armor and a shield in either hand
			boolean shield = (slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.MAINHAND) && Shields.isShield(stack);
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR && !shield) {
				continue;
			}
			if (!hasGems(stack) || BreakProtection.wornOut(stack)) {
				continue;
			}
			percent += resistancePercent(stack, special);
		}
		return Math.max(0, Math.min(1, percent / 100.0));
	}

	/** Percent of a resistance that the gems of one piece give (armor bonuses), 0 if none. */
	public static double resistancePercent(ItemStack stack, GemBonuses.Special special) {
		double percent = 0;
		if (special == GemBonuses.Special.NONE || !hasGems(stack)) {
			return 0;
		}
		for (String id : sockets(stack)) {
			for (GemValues.Bonus bonus : GemValues.on(id, true)) {
				if (bonus.kind().special() == special) {
					percent += bonus.amount(Gems.cutOf(id));
				}
			}
		}
		return percent * Titanite.factor(stack);
	}


	public static boolean active() {
		return GearConfig.socketsActive();
	}

	/** Armor and shields take the armor gems, everything else the gems for weapons and tools. */
	public static boolean isArmorPiece(ItemStack stack) {
		return stack.getItem().components().has(DataComponents.EQUIPPABLE);
	}
}
