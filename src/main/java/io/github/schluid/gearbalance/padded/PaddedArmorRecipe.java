package io.github.schluid.gearbalance.padded;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table: sewing needle (loses one use) + armor piece (copper to netherite) + matching leather piece = padded piece.
 * Enchantments, damage, upgrades, sockets, custom name and leather color stay. Needs the switch on.
 */
public class PaddedArmorRecipe implements SmithingRecipe {
	public static final PaddedArmorRecipe INSTANCE = new PaddedArmorRecipe();
	public static final RecipeSerializer<PaddedArmorRecipe> SERIALIZER =
			new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "padded_armor"), SERIALIZER);
	}

	/** Built on first use, all items are registered by then. */
	private Ingredient template;
	private Ingredient base;
	private Optional<Ingredient> addition;

	private void build() {
		if (template == null) {
			template = Ingredient.of(SewingNeedle.NEEDLE);
			base = Ingredient.of(PaddedArmor.paddableItems().stream());
			addition = Optional.of(Ingredient.of(PaddedArmor.leatherItems().stream()));
		}
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		if (!GearConfig.paddedArmorActive() || !SewingNeedle.isNeedle(input.template())) {
			return false;
		}
		String piece = PaddedArmor.paddablePiece(input.base());
		return piece != null && PaddedArmor.isLeatherFor(input.addition(), piece);
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return PaddedArmor.pad(input.base(), input.addition());
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		build();
		return Optional.of(template);
	}

	@Override
	public Ingredient baseIngredient() {
		build();
		return base;
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		build();
		return addition;
	}

	@Override
	public RecipeSerializer<PaddedArmorRecipe> getSerializer() {
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
