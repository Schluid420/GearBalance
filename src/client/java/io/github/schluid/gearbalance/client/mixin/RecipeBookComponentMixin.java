package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Smaller inventory": the inventory stays in the middle, the recipe book opens further left (77 px
 * more than vanilla's 86). Only if the screen is wide enough, otherwise vanilla behavior.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	/** Vanilla offset of the book from the middle, and how far vanilla moves the inventory. */
	@Unique
	private static final int VANILLA_OFFSET = 86;
	@Unique
	private static final int INVENTORY_SHIFT = 77;
	/** Screen width needed for book and inventory side by side with the inventory in the middle. */
	@Unique
	private static final int MIN_WIDTH = 2 * (VANILLA_OFFSET + INVENTORY_SHIFT) + RecipeBookComponent.IMAGE_WIDTH;

	@Shadow
	@Final
	protected RecipeBookMenu menu;
	@Shadow
	private int width;
	@Shadow
	private boolean widthTooNarrow;
	@Shadow
	private int xOffset;

	@Shadow
	public abstract boolean isVisible();

	@Unique
	private boolean gearbalance$bookLeft() {
		return GearConfig.enabled && GearConfig.inventoryScale && menu instanceof InventoryMenu
				&& !widthTooNarrow && width >= MIN_WIDTH;
	}

	@ModifyConstant(method = "initVisuals", constant = @Constant(intValue = VANILLA_OFFSET))
	private int gearbalance$bookFurtherLeft(int offset) {
		return gearbalance$bookLeft() ? VANILLA_OFFSET + INVENTORY_SHIFT : offset;
	}

	@Inject(method = "updateScreenPosition", at = @At("HEAD"), cancellable = true)
	private void gearbalance$inventoryInMiddle(int screenWidth, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		if (isVisible() && gearbalance$bookLeft()) {
			cir.setReturnValue((screenWidth - imageWidth) / 2);
		}
	}

	/** The book still sits next to the inventory (vanilla checks the offset for 86). */
	@Inject(method = "isOffsetNextToMainGUI", at = @At("HEAD"), cancellable = true)
	private void gearbalance$nextToInventory(CallbackInfoReturnable<Boolean> cir) {
		if (xOffset == VANILLA_OFFSET + INVENTORY_SHIFT) {
			cir.setReturnValue(true);
		}
	}
}
