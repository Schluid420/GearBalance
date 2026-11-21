package io.github.schluid.gearbalance.smith;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.tool.ToolDurability;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Netherite repair kit, used in the inventory like a bundle: right-click it on a damaged piece (or reverse) to fully repair. Loses one use. */
public class RepairKitItem extends Item {
	public RepairKitItem(Properties properties) {
		super(properties);
	}

	/** The kit is held on the cursor and right-clicked onto the piece in the slot. */
	@Override
	public boolean overrideStackedOnOther(ItemStack kit, Slot slot, ClickAction action, Player player) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
			return false;
		}
		ItemStack piece = slot.getItem();
		if (!canRepair(piece)) {
			return false;
		}
		repair(kit, piece, player);
		slot.setChanged();
		return true;
	}

	/** The piece is held on the cursor and right-clicked onto the kit in the slot. */
	@Override
	public boolean overrideOtherStackedOnMe(ItemStack kit, ItemStack piece, Slot slot, ClickAction action, Player player,
			SlotAccess carried) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player) || !canRepair(piece)) {
			return false;
		}
		repair(kit, piece, player);
		slot.setChanged();
		return true;
	}

	private static boolean canRepair(ItemStack piece) {
		return GearConfig.repairKitActive() && !piece.isEmpty() && piece.getCount() == 1 && piece.isDamageableItem()
				&& piece.isDamaged() && !SmithingTools.isRepairKit(piece) && !SmithingTools.isLever(piece)
				&& ToolDurability.max(piece) <= 0;
	}

	/** Repairs the piece fully and wears the kit by one use; at the last use it breaks. */
	private static void repair(ItemStack kit, ItemStack piece, Player player) {
		// Also takes away the damaged mark of the break protection
		BreakProtection.repair(piece);
		player.playSound(SoundEvents.ANVIL_USE, 0.6F, 1.2F);
		int damage = kit.getDamageValue() + 1;
		if (damage >= kit.getMaxDamage()) {
			kit.shrink(1);
			player.playSound(SoundEvents.ITEM_BREAK.value(), 0.8F, 1.0F);
		} else {
			kit.setDamageValue(damage);
		}
	}
}
