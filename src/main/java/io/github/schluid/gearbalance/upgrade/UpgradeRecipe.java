package io.github.schluid.gearbalance.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.cooldown.Cooldowns;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table: forge hammer (loses one use) + gear piece + compressed material = piece one level higher (up to +3).
 * Mace, trident, bow and crossbow use their own item (Upgrades.upgradeItem). The hammer must suit the material (ForgeHammers).
 * One recipe per material: {"type": "gearbalance:upgrade", "material": "iron"}.
 */
public class UpgradeRecipe implements SmithingRecipe {
	public static final MapCodec<UpgradeRecipe> CODEC =
			Codec.STRING.fieldOf("material").xmap(UpgradeRecipe::new, recipe -> recipe.material);
	public static final RecipeSerializer<UpgradeRecipe> SERIALIZER = new RecipeSerializer<>(CODEC,
			ByteBufCodecs.STRING_UTF8.map(UpgradeRecipe::new, recipe -> recipe.material).cast());

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "upgrade"), SERIALIZER);
	}

	private final String material;
	private final Ingredient template;
	private final Ingredient base;
	private final Optional<Ingredient> addition;

	public UpgradeRecipe(String material) {
		if (!Upgrades.MATERIALS.contains(material) || Upgrades.upgradeItem(material) == null) {
			throw new IllegalArgumentException("Unknown upgrade material " + material);
		}
		this.material = material;
		this.template = Ingredient.of(ForgeHammers.hammersFor(material).stream());
		this.base = Ingredient.of(Upgrades.itemsOf(material).stream());
		this.addition = Optional.of(Ingredient.of(Upgrades.upgradeItem(material)));
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		if (!GearConfig.upgradesActive() || !template.test(input.template()) || !base.test(input.base())) {
			return false;
		}
		return Upgrades.storedLevel(input.base()) < Upgrades.MAX_LEVEL && addition.get().test(input.addition())
				&& Cooldowns.ready(input.base(), level, true);
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		ItemStack result = input.base().copy();
		result.setCount(1);
		result.set(Upgrades.UPGRADE, Upgrades.storedLevel(input.base()) + 1);
		return result;
	}

	@Override
	public Optional<Ingredient> templateIngredient() {
		return Optional.of(template);
	}

	@Override
	public Ingredient baseIngredient() {
		return base;
	}

	@Override
	public Optional<Ingredient> additionIngredient() {
		return addition;
	}

	@Override
	public RecipeSerializer<UpgradeRecipe> getSerializer() {
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
