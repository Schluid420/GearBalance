package io.github.schluid.gearbalance.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to adding slots and moving items, for the bag slots of the inventory. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Invoker("addSlot")
	Slot gearbalance$addSlot(Slot slot);

	@Invoker("moveItemStackTo")
	boolean gearbalance$moveItemStackTo(ItemStack stack, int start, int end, boolean backwards);
}
