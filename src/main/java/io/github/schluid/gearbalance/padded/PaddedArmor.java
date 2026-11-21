package io.github.schluid.gearbalance.padded;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Padded armor: a normal piece with the marker "gearbalance:padded"; cold protection like leather (ItemStackMixin),
 * own name and tooltip line. Leather color is kept in "gearbalance:padding_color". With the switch off it acts as normal armor.
 */
public final class PaddedArmor {
	private PaddedArmor() {
	}

	/** Materials that can be padded (item id prefix). Leather and chainmail are not part of it. */
	private static final Set<String> MATERIALS = Set.of("copper", "iron", "golden", "diamond", "netherite", "titanium");
	/** Armor pieces; the matching leather piece has the same name, e.g. leather_chestplate. */
	private static final Set<String> PIECES = Set.of("helmet", "chestplate", "leggings", "boots");

	/** The marker. Registered in onInitialize, null before that. */
	public static DataComponentType<Boolean> PADDED;
	/** Color of the leather (RGB), only set if the leather piece was dyed. */
	public static DataComponentType<Integer> PADDING_COLOR;

	public static void register() {
		PADDED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "padded"),
				DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
		PADDING_COLOR = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "padding_color"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
	}

	/** Color of the padding: the dyed leather color, or the normal leather color. */
	public static int color(ItemStack stack) {
		Integer color = PADDING_COLOR == null ? null : stack.get(PADDING_COLOR);
		return color != null ? color : DyedItemColor.LEATHER_COLOR;
	}

	/** Copy of the armor piece, padded with the leather piece (its dye color is kept). */
	public static ItemStack pad(ItemStack armor, ItemStack leather) {
		ItemStack result = armor.copy();
		result.setCount(1);
		result.set(PADDED, true);
		DyedItemColor dyed = leather.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
		if (dyed != null) {
			result.set(PADDING_COLOR, dyed.rgb());
		}
		return result;
	}

	/** Copy of the piece without padding and padding color. */
	public static ItemStack unpad(ItemStack stack) {
		ItemStack result = stack.copy();
		result.remove(PADDED);
		result.remove(PADDING_COLOR);
		return result;
	}

	/** All armor items that can be padded. */
	public static List<Item> paddableItems() {
		List<Item> items = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (paddablePiece(new ItemStack(item)) != null) {
				items.add(item);
			}
		}
		return items;
	}

	/** The four leather armor items. */
	public static List<Item> leatherItems() {
		List<Item> items = new ArrayList<>();
		for (String piece : PIECES) {
			items.add(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("leather_" + piece)));
		}
		return items;
	}

	/** Whether the stack carries the marker, regardless of the switches. */
	public static boolean isPadded(ItemStack stack) {
		return PADDED != null && !stack.isEmpty() && Boolean.TRUE.equals(stack.get(PADDED));
	}

	/** Padded and the switch is on: only then it protects, is named and shown as padded. */
	public static boolean isActive(ItemStack stack) {
		return GearConfig.paddedArmorActive() && isPadded(stack);
	}

	/** Item path without "minecraft:", e.g. iron_chestplate; null for items of other mods. */
	private static String vanillaPath(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		// Vanilla armor, and the titanium armor of GearBalance
		if ("minecraft".equals(id.getNamespace())
				|| (GearBalance.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("titanium_"))) {
			return id.getPath();
		}
		return null;
	}

	/** The piece name (helmet, chestplate, leggings, boots) if the stack can be padded, otherwise null. */
	public static String paddablePiece(ItemStack stack) {
		String path = vanillaPath(stack);
		if (path == null || isPadded(stack)) {
			return null;
		}
		int split = path.lastIndexOf('_');
		if (split < 0) {
			return null;
		}
		String material = path.substring(0, split);
		String piece = path.substring(split + 1);
		return MATERIALS.contains(material) && PIECES.contains(piece) ? piece : null;
	}

	/** Whether the stack is the leather piece for the given armor piece. */
	public static boolean isLeatherFor(ItemStack stack, String piece) {
		return ("leather_" + piece).equals(vanillaPath(stack));
	}

	/** Translation key of the padded name, e.g. item.gearbalance.padded_iron_chestplate. */
	public static String nameKey(ItemStack stack) {
		String path = vanillaPath(stack);
		return path == null ? null : "item." + GearBalance.MOD_ID + ".padded_" + path;
	}
}
