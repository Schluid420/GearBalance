package io.github.schluid.gearbalance.smith;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import io.github.schluid.gearbalance.shield.Shields;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** Smithing table: gem lever (loses one use) + piece with gems, right slot empty = piece without its last gem; the gem returns whole (SmithingMenuMixin). */
public class LeverRecipe implements SmithingRecipe {
	public static final LeverRecipe INSTANCE = new LeverRecipe();
	public static final MapCodec<LeverRecipe> CODEC = MapCodec.unit(INSTANCE);
	public static final RecipeSerializer<LeverRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, StreamCodec.unit(INSTANCE));

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "gem_lever"), SERIALIZER);
	}

	private Ingredient template;
	private Ingredient base;

	/** Built on first use: the items are registered and bound by then. */
	private void build() {
		if (template == null) {
			template = Ingredient.of(SmithingTools.DIAMOND_LEVER, SmithingTools.NETHERITE_LEVER, SmithingTools.TITANIUM_LEVER);
			base = Ingredient.of(socketable().stream());
		}
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		build();
		if (!template.test(input.template()) || !input.addition().isEmpty()) {
			return false;
		}
		return GearConfig.gemLeverActive() && lastGem(input.base()) >= 0;
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		ItemStack result = Sockets.withoutGem(input.base(), lastGem(input.base()));
		result.setCount(1);
		return result;
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
		return Optional.empty();
	}

	/** Index of the last socket with a gem, -1 if the piece has none. */
	public static int lastGem(ItemStack stack) {
		List<String> sockets = Sockets.sockets(stack);
		for (int i = sockets.size() - 1; i >= 0; i--) {
			if (!sockets.get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	/** Every piece that can have sockets: gear of all materials and the shields. */
	private static List<Item> socketable() {
		java.util.List<Item> items = new java.util.ArrayList<>();
		for (String material : Upgrades.MATERIALS) {
			items.addAll(Shields.withVanilla(material, Upgrades.itemsOf(material)));
		}
		return items;
	}

	@Override
	public RecipeSerializer<LeverRecipe> getSerializer() {
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
