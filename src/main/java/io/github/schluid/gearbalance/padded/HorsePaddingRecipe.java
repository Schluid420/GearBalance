package io.github.schluid.gearbalance.padded;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Crafting table: horse or wolf armor + wool + thread + sewing needle = padded armor (half fall damage). Needle loses one use. */
public class HorsePaddingRecipe extends CustomRecipe {
	public static final HorsePaddingRecipe INSTANCE = new HorsePaddingRecipe();
	public static final RecipeSerializer<HorsePaddingRecipe> SERIALIZER =
			new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, GearBalance.id("horse_padding"), SERIALIZER);
	}

	/** The animal armor, or empty if the grid holds anything else than armor, wool, thread and needle. */
	private static ItemStack armor(CraftingInput input) {
		ItemStack armor = ItemStack.EMPTY;
		int wool = 0;
		int thread = 0;
		int needle = 0;
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (Tailoring.isAnimalArmor(stack) && armor.isEmpty()) {
				armor = stack;
			} else if (stack.is(ItemTags.WOOL)) {
				wool++;
			} else if (Tailoring.isThread(stack)) {
				thread++;
			} else if (SewingNeedle.isNeedle(stack)) {
				needle++;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return wool == 1 && thread == 1 && needle == 1 ? armor : ItemStack.EMPTY;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (!GearConfig.horsePaddingActive()) {
			return false;
		}
		ItemStack armor = armor(input);
		return !armor.isEmpty() && !Tailoring.isHorsePadded(armor);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack result = armor(input).copyWithCount(1);
		result.set(Tailoring.HORSE_PADDED, true);
		return result;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		return Tailoring.remainingWithNeedle(input);
	}

	@Override
	public RecipeSerializer<HorsePaddingRecipe> getSerializer() {
		return SERIALIZER;
	}
}
