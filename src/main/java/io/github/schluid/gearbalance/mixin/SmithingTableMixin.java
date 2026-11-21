package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.table.SmithingTableItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Option "Items stay in the smithing table": input items stay when closed (SmithingTableItems). Server only. */
@Mixin(SmithingMenu.class)
public abstract class SmithingTableMixin extends ItemCombinerMenu {
	@Unique
	private static final int INPUT_SLOTS = 3;

	private SmithingTableMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
			ItemCombinerMenuSlotDefinition slots) {
		super(type, containerId, inventory, access, slots);
	}

	/** Takes the items stored in this table back into the slots. */
	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void gearbalance$loadItems(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		access.execute((level, pos) -> {
			if (!(level instanceof ServerLevel server)) {
				return;
			}
			List<ItemStack> saved = SmithingTableItems.take(server, pos);
			for (int i = 0; i < saved.size() && i < INPUT_SLOTS; i++) {
				this.inputSlots.setItem(i, saved.get(i).copy());
			}
		});
	}

	/** With the option on, the items are stored in the table instead of going back to the player. */
	@Override
	public void removed(Player player) {
		if (GearConfig.keepTableItemsActive() && player instanceof ServerPlayer) {
			this.access.execute((level, pos) -> {
				if (!(level instanceof ServerLevel server) || !level.getBlockState(pos).is(Blocks.SMITHING_TABLE)) {
					return;
				}
				List<ItemStack> keep = new ArrayList<>();
				boolean any = false;
				for (int i = 0; i < INPUT_SLOTS; i++) {
					ItemStack stack = this.inputSlots.getItem(i).copy();
					any |= !stack.isEmpty();
					keep.add(stack);
				}
				if (any && SmithingTableItems.store(server, pos, keep)) {
					// Empty slots: vanilla has nothing left to give back
					for (int i = 0; i < INPUT_SLOTS; i++) {
						this.inputSlots.removeItemNoUpdate(i);
					}
				}
			});
		}
		super.removed(player);
	}
}
