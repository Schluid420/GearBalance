package io.github.schluid.gearbalance.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import io.github.schluid.gearbalance.shield.Shields;
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
 * Smithing table: forge hammer (loses one use) + piece with an empty socket + cut gem or titanite = gem in the first empty socket.
 * The hammer must suit the material, as for upgrades. One recipe per material: {"type": "gearbalance:gem", "material": "iron"}.
 */
public class GemRecipe implements SmithingRecipe {
	public static final MapCodec<GemRecipe> CODEC =
			Codec.STRING.fieldOf("material").xmap(GemRecipe::new, recipe -> recipe.material);
	public static final RecipeSerializer<GemRecipe> SERIALIZER = new RecipeSerializer<>(CODEC,
			ByteBufCodecs.STRING_UTF8.map(GemRecipe::new, recipe -> recipe.material).cast());

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "gem"), SERIALIZER);
	}

	private final String material;
	private final Ingredient template;
	private final Ingredient base;
	private final Optional<Ingredient> addition;

	public GemRecipe(String material) {
		if (!Upgrades.MATERIALS.contains(material)) {
			throw new IllegalArgumentException("Unknown gem material " + material);
		}
		this.material = material;
		this.template = Ingredient.of(ForgeHammers.hammersFor(material).stream());
		this.base = Ingredient.of(Shields.withVanilla(material, Upgrades.itemsOf(material)).stream());
		this.addition = Optional.of(Ingredient.of(java.util.stream.Stream.concat(Gems.all().stream(),
				java.util.stream.Stream.of(Titanite.CHIPPED, Titanite.CUT, Titanite.FLAWLESS))));
	}

	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		if (!Sockets.active() || !template.test(input.template()) || !base.test(input.base())) {
			return false;
		}
		// Titanite goes into any piece, once
		if (Titanite.is(input.addition())) {
			return Titanite.active() && Sockets.hasEmptySocket(input.base()) && !Titanite.in(input.base());
		}
		// Armor gems go into armor and shields, the others into weapons and tools
		GemSorts.Sort sort = Gems.sort(input.addition());
		return Sockets.hasEmptySocket(input.base()) && sort != null && sort.armor() == Sockets.isArmorPiece(input.base())
				&& !rareCutTaken(input.addition(), sort, input.base());
	}

	/** Aquamarine and onyx: an identical gem (same sort and cut) only once per piece; other sorts or cuts may go together. */
	private static boolean rareCutTaken(ItemStack gem, GemSorts.Sort sort, ItemStack piece) {
		if (!GemBonuses.TWO_BONUS_TYPES.contains(sort.type())) {
			return false;
		}
		int cut = Gems.cutOf(Gems.id(gem));
		for (String socket : Sockets.sockets(piece)) {
			GemSorts.Sort taken = Gems.sortOf(socket);
			if (taken != null && taken.key().equals(sort.key()) && taken.type().equals(sort.type())
					&& Gems.cutOf(socket) == cut) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		return Sockets.withGem(input.base(), input.addition());
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
	public RecipeSerializer<GemRecipe> getSerializer() {
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
