package io.github.schluid.gearbalance.bag;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip image of a bag: used and total slots. Drawn by the client (ClientBagTooltip). */
public record BagTooltip(int used, int total) implements TooltipComponent {
}
