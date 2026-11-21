package io.github.schluid.gearbalance.client.compat;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.client.GuiAreas;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.comfort.RestoreRecipe;
import io.github.schluid.gearbalance.padded.PaddedArmorRecipe;
import io.github.schluid.gearbalance.smith.LeverRecipe;
import io.github.schluid.gearbalance.socket.GemRecipe;
import io.github.schluid.gearbalance.socket.SocketRecipe;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.upgrade.UpgradeRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.IExtendableSmithingRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

/** JEI (only loaded if installed): shows GearBalance's smithing recipes; results come from the recipes themselves. */
public class GearBalanceJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return GearBalance.id("jei");
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		IExtendableSmithingRecipeCategory smithing = registration.getSmithingCategory();
		smithing.addExtension(UpgradeRecipe.class, new Extension<>(UnaryOperator.identity()));
		smithing.addExtension(SocketRecipe.class, new Extension<>(UnaryOperator.identity()));
		// A gem needs an empty socket: show the pieces with one
		smithing.addExtension(GemRecipe.class, new Extension<>(Sockets::withNewSocket));
		smithing.addExtension(PaddedArmorRecipe.class, new Extension<>(UnaryOperator.identity()));
		smithing.addExtension(LeverRecipe.class, new Extension<>(UnaryOperator.identity()));
		// Repair of damaged pieces (break protection): show them damaged
		smithing.addExtension(RestoreRecipe.class, new Extension<>(GearBalanceJeiPlugin::damaged));
	}

	private static ItemStack damaged(ItemStack stack) {
		ItemStack result = stack.copy();
		result.set(BreakProtection.DAMAGED, Unit.INSTANCE);
		result.setDamageValue(Math.max(0, result.getMaxDamage() - 1));
		return result;
	}

	/** JEI keeps its lists away from the values panel and the gear set buttons of the inventory. */
	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(InventoryScreen.class, new IGuiContainerHandler<InventoryScreen>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
				return GuiAreas.outside(screen);
			}
		});
	}

	/** Template, base and addition from the recipe's ingredients; the base pieces prepared as the recipe needs them. */
	private static final class Extension<R extends SmithingRecipe> implements ISmithingCategoryExtension<R> {
		private final UnaryOperator<ItemStack> base;

		Extension(UnaryOperator<ItemStack> base) {
			this.base = base;
		}

		@Override
		public <T extends IIngredientAcceptor<T>> void setTemplate(R recipe, T acceptor) {
			recipe.templateIngredient().ifPresent(ingredient -> acceptor.add(ingredient));
		}

		@Override
		public <T extends IIngredientAcceptor<T>> void setBase(R recipe, T acceptor) {
			acceptor.addItemStacks(bases(recipe));
		}

		@Override
		public <T extends IIngredientAcceptor<T>> void setAddition(R recipe, T acceptor) {
			recipe.additionIngredient().ifPresent(ingredient -> acceptor.add(ingredient));
		}

		@Override
		public <T extends IIngredientAcceptor<T>> void setOutput(R recipe, T acceptor) {
			ItemStack template = first(recipe.templateIngredient());
			ItemStack addition = first(recipe.additionIngredient());
			List<ItemStack> results = new ArrayList<>();
			for (ItemStack piece : bases(recipe)) {
				ItemStack result = result(recipe, template, piece, addition);
				if (!result.isEmpty()) {
					results.add(result);
				}
			}
			acceptor.addItemStacks(results);
		}

		@Override
		public void onDisplayedIngredientsUpdate(R recipe, IRecipeSlotDrawable templateSlot, IRecipeSlotDrawable baseSlot,
				IRecipeSlotDrawable additionSlot, IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
			ItemStack template = templateSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
			ItemStack piece = baseSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
			ItemStack addition = additionSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
			ItemStack result = result(recipe, template, piece, addition);
			if (!result.isEmpty()) {
				outputSlot.createDisplayOverrides().add(result);
			}
		}

		private List<ItemStack> bases(R recipe) {
			List<ItemStack> result = new ArrayList<>();
			recipe.baseIngredient().items().forEach(item -> result.add(base.apply(new ItemStack(item))));
			return result;
		}

		private static ItemStack result(SmithingRecipe recipe, ItemStack template, ItemStack piece, ItemStack addition) {
			if (piece.isEmpty()) {
				return ItemStack.EMPTY;
			}
			try {
				return recipe.assemble(new SmithingRecipeInput(template, piece, addition));
			} catch (RuntimeException e) {
				return ItemStack.EMPTY;
			}
		}

		private static ItemStack first(Optional<Ingredient> ingredient) {
			return ingredient.flatMap(i -> i.items().findFirst()).map(ItemStack::new).orElse(ItemStack.EMPTY);
		}
	}
}
