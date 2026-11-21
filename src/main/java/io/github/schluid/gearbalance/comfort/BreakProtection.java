package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.upgrade.CompressedItems;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Break protection: valuable pieces (enchanted, upgraded, gems) stop at 1 durability and become damaged:
 * no values, no enchantments, no use; tools mine like the bare hand.
 * Repair only at the smithing table (forge hammer + compressed material, RestoreRecipe) or with the repair kit.
 */
public final class BreakProtection {
	private BreakProtection() {
	}

	/** Marks a damaged piece. Stays until the piece is repaired at the smithing table or with the repair kit. */
	public static DataComponentType<Unit> DAMAGED;

	/** Repair materials besides the compressed ones of the gear materials. */
	public static final String PHANTOM = "phantom";

	public static void register() {
		DAMAGED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GearBalance.id("damaged"),
				DataComponentType.<Unit>builder().persistent(Unit.CODEC)
						.networkSynchronized(StreamCodec.unit(Unit.INSTANCE)).build());
	}

	/** Enchanted, upgraded or holding at least one gem, and there is a way to repair it. */
	public static boolean valuable(ItemStack stack) {
		return !stack.isEmpty() && repairMaterial(stack.getItem()) != null
				&& (stack.isEnchanted() || Upgrades.storedLevel(stack) > 0 || Sockets.hasGems(stack));
	}

	/** True for a damaged piece while the break protection is on. */
	public static boolean wornOut(ItemStack stack) {
		return DAMAGED != null && !stack.isEmpty() && stack.has(DAMAGED) && GearConfig.breakProtectionActive();
	}

	/** Damaged mark is there (whatever the switch says), for repairs that remove it. */
	public static boolean marked(ItemStack stack) {
		return DAMAGED != null && !stack.isEmpty() && stack.has(DAMAGED);
	}

	/** The new damage value; a protected piece that would break keeps 1 durability. */
	public static int limit(ItemStack stack, int damage) {
		if (!GearConfig.breakProtectionActive() || !stack.isDamageableItem() || damage < stack.getMaxDamage()
				|| !valuable(stack)) {
			return damage;
		}
		stack.set(DAMAGED, Unit.INSTANCE);
		return stack.getMaxDamage() - 1;
	}

	/** Repaired: full durability and no damaged mark. */
	public static void repair(ItemStack stack) {
		stack.setDamageValue(0);
		if (DAMAGED != null) {
			stack.remove(DAMAGED);
		}
	}

	/**
	 * Compressed repair material: the gear material; leather for bow/crossbow; diamond for trident, mace, turtle helmet;
	 * phantom membrane for elytra. Null (wooden/stone tools, wooden shields, shears ...): no break protection.
	 */
	public static String repairMaterial(Item item) {
		if (item == Items.ELYTRA) {
			return PHANTOM;
		}
		if (item == Items.BOW || item == Items.CROSSBOW) {
			return "leather";
		}
		if (item == Items.TRIDENT || item == Items.MACE || item == Items.TURTLE_HELMET) {
			return "diamond";
		}
		String material = Shields.material(item);
		if (material == null) {
			material = GearBalanceApi.materialOf(BuiltInRegistries.ITEM.getKey(item).toString());
		}
		if (material == null) {
			return null;
		}
		// Wooden and stone tools and the wooden shields break as usual
		return CompressedItems.BY_MATERIAL.containsKey(material) ? material : null;
	}

	/** The compressed item for a repair material. */
	public static Item repairItem(String material) {
		return PHANTOM.equals(material) ? CompressedItems.PHANTOM_MEMBRANE : CompressedItems.BY_MATERIAL.get(material);
	}

	/** Material whose forge hammer tier the repair needs (the elytra needs the diamond hammer). */
	public static String hammerMaterial(String material) {
		return PHANTOM.equals(material) ? "diamond" : material;
	}
}
