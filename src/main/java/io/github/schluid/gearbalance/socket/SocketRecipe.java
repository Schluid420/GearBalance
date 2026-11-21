package io.github.schluid.gearbalance.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.cooldown.Cooldowns;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import io.github.schluid.gearbalance.shield.Shields;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing table: socket chisel (loses one use) + gear piece + amethyst shards = one more empty socket (shards taken in
 * SmithingMenuMixin). One recipe per material: {"type": "gearbalance:socket", "material": "iron"}.
 */
public class SocketRecipe implements SmithingRecipe {
	public static final MapCodec<SocketRecipe> CODEC =
			Codec.STRING.fieldOf("material").xmap(SocketRecipe::new, recipe -> recipe.material);
	public static final RecipeSerializer<SocketRecipe> SERIALIZER = new RecipeSerializer<>(CODEC,
			ByteBufCodecs.STRING_UTF8.map(SocketRecipe::new, recipe -> recipe.material).cast());

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "socket"), SERIALIZER);
	}

	private final String material;
	private final Ingredient template;
	private final Ingredient base;
	private final Optional<Ingredient> addition;

	public SocketRecipe(String material) {
		if (!Upgrades.MATERIALS.contains(material)) {
			throw new IllegalArgumentException("Unknown socket material " + material);
		}
		this.material = material;
		this.template = Ingredient.of(SocketChisels.chiselsFor(material).stream());
		this.base = Ingredient.of(Shields.withVanilla(material, Upgrades.itemsOf(material)).stream());
		this.addition = Optional.of(Ingredient.of(Items.AMETHYST_SHARD));
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		ItemStack piece = input.base();
		if (!Sockets.active() || !template.test(input.template()) || !base.test(piece)) {
			return false;
		}
		return Sockets.count(piece) < Sockets.max(piece) && addition.get().test(input.addition())
				&& input.addition().getCount() >= Sockets.amethystCost(piece) && Cooldowns.ready(piece, level, false);
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return Sockets.withNewSocket(input.base());
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
	public RecipeSerializer<SocketRecipe> getSerializer() {
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
