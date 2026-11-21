package io.github.schluid.gearbalance.combat;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.shield.Shields;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

/**
 * Parrying: blocking a melee hit right after the shield is ready (window grows with better shields) throws the
 * attacker back; the next melee hit within 2 seconds deals more damage. The block itself is vanilla.
 */
public final class Parry {
	private Parry() {
	}

	/** Damage bonus of the next hit after a parry. */
	private static final float BONUS = 1.3F;
	/** Ticks the bonus waits for the next hit (2 seconds). */
	private static final int BONUS_TICKS = 40;
	/** How far the attacker is thrown back, by shield material (netherite the farthest), from the settings. */
	public static double throwStrength(ItemStack shield) {
		String material = Shields.material(shield.getItem());
		return GearConfig.PARRY_KNOCKBACK.getOrDefault(material == null ? Shields.WOOD : material,
				GearConfig.defaultParryKnockback(Shields.WOOD));
	}

	/** Player -> game time until which the next melee hit gets the bonus. */
	private static final Map<UUID, Long> READY = new HashMap<>();

	/** Ticks after the shield is ready in which a block counts as a parry, by shield material. */
	public static int windowTicks(ItemStack shield) {
		String material = Shields.material(shield.getItem());
		if (material == null) {
			return 3;
		}
		return switch (material) {
			case "copper" -> 4;
			case "iron", "gold" -> 5;
			case "diamond" -> 6;
			case "netherite" -> 7;
			case "titanium" -> 8;
			default -> 3;
		};
	}

	/** Called after a block (blocked > 0): checks the timing and throws the attacker back. */
	public static void onBlocked(LivingEntity defender, ServerLevel level, DamageSource source, float blocked) {
		if (!GearConfig.parryActive() || blocked <= 0 || !(defender instanceof ServerPlayer player)) {
			return;
		}
		ItemStack shield = player.getItemBlockingWith();
		Entity direct = source.getDirectEntity();
		// Only melee: the hitting entity itself is the attacker
		if (shield == null || shield.isEmpty() || !(direct instanceof LivingEntity attacker) || direct != source.getEntity()) {
			return;
		}
		BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
		int delay = blocks == null ? 0 : Math.round(blocks.blockDelaySeconds() * 20);
		int ready = player.getTicksUsingItem() - delay;
		if (ready < 0 || ready > windowTicks(shield)) {
			return;
		}
		Vec3 away = attacker.position().subtract(player.position()).multiply(1, 0, 1);
		if (away.lengthSqr() > 1.0E-4) {
			away = away.normalize();
			double strength = throwStrength(shield);
			attacker.push(away.x * strength, 0.15 + 0.2 * strength, away.z * strength);
			attacker.needsSync = true;
		}
		READY.put(player.getUUID(), level.getGameTime() + BONUS_TICKS);
		level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35F, 1.8F);
		level.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6,
				attacker.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
	}

	/** Damage of a hit: more if the attacker parried just before; the bonus is used up. */
	public static float damage(DamageSource source, float amount, ServerLevel level) {
		if (READY.isEmpty() || !(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
			return amount;
		}
		Long until = READY.remove(player.getUUID());
		return until != null && level.getGameTime() <= until ? amount * BONUS : amount;
	}
}
