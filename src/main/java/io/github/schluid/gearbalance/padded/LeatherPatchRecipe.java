package io.github.schluid.gearbalance.padded;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting table: damaged leather armor + leather + thread + sewing needle = a quarter of its durability back.
 * Needle loses one use. Pieces saved by break protection still need the smithing table.
 */
public class LeatherPatchRecipe extends CustomRecipe {
	public static final LeatherPatchRecipe INSTANCE = new LeatherPatchRecipe();
	public static final RecipeSerializer<LeatherPatchRecipe> SERIALIZER =
			new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, GearBalance.id("leather_patch"), SERIALIZER);
	}

	/** The armor piece, or empty if the grid holds anything else than one piece, leather, thread and needle. */
	private static ItemStack piece(CraftingInput input) {
		ItemStack piece = ItemStack.EMPTY;
		int leather = 0;
		int thread = 0;
		int needle = 0;
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (Tailoring.isLeatherArmor(stack) && piece.isEmpty()) {
				piece = stack;
			} else if (stack.is(Items.LEATHER)) {
				leather++;
			} else if (Tailoring.isThread(stack)) {
				thread++;
			} else if (SewingNeedle.isNeedle(stack)) {
				needle++;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return leather == 1 && thread == 1 && needle == 1 ? piece : ItemStack.EMPTY;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (!GearConfig.leatherPatchActive()) {
			return false;
		}
		ItemStack piece = piece(input);
		return !piece.isEmpty() && piece.isDamaged() && !BreakProtection.marked(piece);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack result = piece(input).copyWithCount(1);
		int back = (int) Math.ceil(result.getMaxDamage() * Tailoring.PATCH_SHARE);
		result.setDamageValue(Math.max(0, result.getDamageValue() - back));
		return result;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		return Tailoring.remainingWithNeedle(input);
	}

	@Override
	public RecipeSerializer<LeatherPatchRecipe> getSerializer() {
		return SERIALIZER;
	}
}
