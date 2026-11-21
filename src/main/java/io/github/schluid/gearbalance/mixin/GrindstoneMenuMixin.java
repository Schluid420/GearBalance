package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.grindstone.GrindstoneChoice;
import io.github.schluid.gearbalance.grindstone.GrindstoneOptions;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grindstone: with one piece of gear, buttons choose what to remove (padding and/or gems).
 * Only that is removed, no experience. Without a choice it works as in vanilla.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin implements GrindstoneChoice {
	@Shadow
	@Final
	private Container repairSlots;

	@Shadow
	private void createResult() {
	}

	/** Chosen things as bits (GrindstoneOptions.bit), 0 = nothing chosen. */
	@Unique
	private int gearbalance$choice = 0;
	/** The piece the choice belongs to; a different piece resets the choice. */
	@Unique
	private ItemStack gearbalance$piece = ItemStack.EMPTY;
	/** Gems the current result removes. */
	@Unique
	private List<String> gearbalance$removed = List.of();

	@Override
	public List<String> gearbalance$removedGems() {
		return gearbalance$removed;
	}

	@Unique
	private ItemStack gearbalance$currentPiece() {
		return GrindstoneOptions.single(repairSlots.getItem(0), repairSlots.getItem(1));
	}

	@Override
	public int gearbalance$choice() {
		return gearbalance$choice;
	}

	@Override
	public void gearbalance$choose(int choice) {
		ItemStack piece = gearbalance$currentPiece();
		if (GrindstoneOptions.valid(piece, choice)) {
			gearbalance$choice = GrindstoneOptions.validMask(piece, gearbalance$choice ^ GrindstoneOptions.bit(choice));
			gearbalance$piece = piece.copy();
		}
		createResult();
	}

	/** A different piece (or none) in the slots: forget the choice. */
	@Inject(method = "slotsChanged", at = @At("HEAD"))
	private void gearbalance$checkPiece(Container container, CallbackInfo ci) {
		if (gearbalance$choice != 0) {
			ItemStack piece = gearbalance$currentPiece();
			if (!ItemStack.matches(piece, gearbalance$piece)) {
				gearbalance$choice = 0;
				gearbalance$piece = ItemStack.EMPTY;
			}
		}
	}

	@Inject(method = "computeResult", at = @At("RETURN"), cancellable = true)
	private void gearbalance$removeChosen(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
		gearbalance$removed = List.of();
		if (gearbalance$choice == 0) {
			return;
		}
		ItemStack piece = GrindstoneOptions.single(input, additional);
		if (GrindstoneOptions.validMask(piece, gearbalance$choice) != 0) {
			List<String> removed = new ArrayList<>();
			for (int choice : GrindstoneOptions.choices(piece)) {
				String gem = GrindstoneOptions.chosen(gearbalance$choice, choice) ? GrindstoneOptions.gemId(piece, choice) : null;
				if (gem != null) {
					removed.add(gem);
				}
			}
			gearbalance$removed = List.copyOf(removed);
			cir.setReturnValue(GrindstoneOptions.apply(piece, gearbalance$choice));
		}
	}

	/** Break protection: a damaged piece can't be put together with another one (that would repair it). */
	@Inject(method = "computeResult", at = @At("RETURN"), cancellable = true)
	private void gearbalance$noDamagedMerge(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
		if (!input.isEmpty() && !additional.isEmpty()
				&& (BreakProtection.marked(input) || BreakProtection.marked(additional))) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}
}
