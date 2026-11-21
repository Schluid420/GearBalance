package io.github.schluid.gearbalance.comfort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table: forge hammer (loses one use) + damaged piece + compressed material = fully repaired piece.
 * One recipe per material: {"type": "gearbalance:restore", "material": "iron"}.
 */
public class RestoreRecipe implements SmithingRecipe {
	public static final MapCodec<RestoreRecipe> CODEC =
			Codec.STRING.fieldOf("material").xmap(RestoreRecipe::new, recipe -> recipe.material);
	public static final RecipeSerializer<RestoreRecipe> SERIALIZER = new RecipeSerializer<>(CODEC,
			ByteBufCodecs.STRING_UTF8.map(RestoreRecipe::new, recipe -> recipe.material).cast());

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, GearBalance.id("restore"), SERIALIZER);
	}

	private final String material;
	private final Ingredient template;
	/** Built on first use: item components are not bound yet while recipes are loaded. */
	private Ingredient base;
	private final Optional<Ingredient> addition;

	public RestoreRecipe(String material) {
		Item repairItem = BreakProtection.repairItem(material);
		if (repairItem == null) {
			throw new IllegalArgumentException("Unknown repair material " + material);
		}
		this.material = material;
		this.template = Ingredient.of(ForgeHammers.hammersFor(BreakProtection.hammerMaterial(material)).stream());
		this.addition = Optional.of(Ingredient.of(repairItem));
	}

	private Ingredient base() {
		if (base == null) {
			List<Item> pieces = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item.components().has(DataComponents.MAX_DAMAGE) && material.equals(BreakProtection.repairMaterial(item))) {
					pieces.add(item);
				}
			}
			base = Ingredient.of(pieces.stream());
		}
		return base;
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		return GearConfig.breakProtectionActive() && template.test(input.template()) && base().test(input.base())
				&& BreakProtection.marked(input.base()) && addition.get().test(input.addition());
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		ItemStack result = input.base().copyWithCount(1);
		BreakProtection.repair(result);
		return result;
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return Optional.of(template);
	}

	@Override
	public Ingredient baseIngredient() {
		return base();
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return addition;
	}

	@Override
	public RecipeSerializer<RestoreRecipe> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}
}
