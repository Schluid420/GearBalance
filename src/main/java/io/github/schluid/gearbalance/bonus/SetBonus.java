package io.github.schluid.gearbalance.bonus;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.stats.Material;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Set bonuses: half the bonus from 2 worn pieces of one material, the full bonus with all 4.
 * Pieces of different materials each count for their own set. Checked twice a second on the server,
 * as one attribute modifier per material, so two half sets can be active at the same time.
 */
public final class SetBonus {
	private SetBonus() {
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int INTERVAL = 10;
	private static final Set<Holder<Attribute>> ALL_ATTRIBUTES = new LinkedHashSet<>();

	static {
		for (BonusType type : BonusType.values()) {
			ALL_ATTRIBUTES.addAll(type.attributes);
		}
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				update(player);
			}
		});
	}

	/** Worn armor pieces per material. Also used by the client for the tooltip. */
	public static Map<Material, Integer> wornPieces(Player player) {
		Map<Material, Integer> pieces = new EnumMap<>(Material.class);
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			Material material = Material.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			if (material != null && material.hasArmor()) {
				pieces.merge(material, 1, Integer::sum);
			}
		}
		return pieces;
	}

	/** Bonus amount as the player sees it, for example 1 for "+1 heart". */
	public static double amount(Material material, int pieces) {
		return GearConfig.activeSetStrength(material) * BonusType.share(pieces);
	}

	private static void update(ServerPlayer player) {
		Map<Material, Integer> pieces = wornPieces(player);
		for (Material material : Material.values()) {
			if (!material.hasArmor()) {
				continue;
			}
			Identifier id = GearBalance.id("set_bonus." + material.key);
			BonusType type = GearConfig.getSetType(material);
			double amount = amount(material, pieces.getOrDefault(material, 0));
			for (Holder<Attribute> attribute : ALL_ATTRIBUTES) {
				boolean active = amount > 0 && type.attributes.contains(attribute);
				setModifier(player, attribute, id, active ? amount * type.perUnit : 0, type.operation);
			}
		}
	}

	private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double value,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(id);
		if (value == 0) {
			if (current != null) {
				instance.removeModifier(id);
				if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
					player.setHealth(player.getMaxHealth());
				}
			}
			return;
		}
		if (current == null || current.amount() != value || current.operation() != operation) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, operation));
		}
	}
}
