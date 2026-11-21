package io.github.schluid.gearbalance.padded;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** Crafting table sewing: thread from wool, patching leather armor, padding horse and wolf armor. The needle loses one use. */
public final class Tailoring {
	private Tailoring() {
	}

	/** Thread, from wool. Registered in onInitialize, null before that. */
	public static Item THREAD;
	/** Marker of padded horse and wolf armor. */
	public static DataComponentType<Boolean> HORSE_PADDED;

	/** Share of the durability a patch gives back. */
	public static final double PATCH_SHARE = 0.25;
	/** Fall damage of a horse or wolf in padded armor, as a share of the normal damage. */
	public static final float HORSE_FALL_FACTOR = 0.5F;

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GearBalance.id("thread"));
		THREAD = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(THREAD));
		HORSE_PADDED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GearBalance.id("horse_padded"),
				DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
		LeatherPatchRecipe.register();
		HorsePaddingRecipe.register();
	}

	public static boolean isThread(ItemStack stack) {
		return THREAD != null && !stack.isEmpty() && stack.getItem() == THREAD;
	}

	/** Vanilla horse armor of any material, and the wolf armor. */
	public static boolean isAnimalArmor(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return "minecraft".equals(id.getNamespace())
				&& (id.getPath().endsWith("_horse_armor") || id.getPath().equals("wolf_armor"));
	}

	/** Whether the stack carries the marker, regardless of the switch. */
	public static boolean isHorsePadded(ItemStack stack) {
		return HORSE_PADDED != null && !stack.isEmpty() && Boolean.TRUE.equals(stack.get(HORSE_PADDED));
	}

	/** Padded animal armor with the switch on: only then it protects and is named padded. */
	public static boolean horsePaddingActive(ItemStack stack) {
		return GearConfig.horsePaddingActive() && isHorsePadded(stack);
	}

	/** The four leather armor pieces. */
	public static boolean isLeatherArmor(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return "minecraft".equals(id.getNamespace()) && id.getPath().startsWith("leather_")
				&& !id.getPath().equals("leather_horse_armor") && stack.isDamageableItem();
	}

	/** Remaining items: the needle comes back with one use less, everything else as vanilla does it. */
	public static NonNullList<ItemStack> remainingWithNeedle(CraftingInput input) {
		NonNullList<ItemStack> rest = CraftingRecipe.defaultCraftingReminder(input);
		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (SewingNeedle.isNeedle(stack)) {
				ItemStack needle = stack.copyWithCount(1);
				int damage = needle.getDamageValue() + 1;
				if (damage < needle.getMaxDamage()) {
					needle.setDamageValue(damage);
					rest.set(i, needle);
				} else {
					rest.set(i, ItemStack.EMPTY);
				}
			}
		}
		return rest;
	}
}
