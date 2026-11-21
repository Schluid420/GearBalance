package io.github.schluid.gearbalance.bag;

import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A bag: holds its items itself, can't go into another bag, shulker box or bundle. Tooltip bar: BagTooltip (client). */
public class BagItem extends Item {
	public BagItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (!Bags.active()) {
			return Optional.empty();
		}
		return Optional.of(new BagTooltip(Bags.usedSlots(stack), Bags.rows(stack) * Bags.shownColumns()));
	}
}
