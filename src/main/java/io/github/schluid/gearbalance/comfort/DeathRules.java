package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.mixin.ItemEntityAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Gentler deaths, each part with its own switch (the server decides):
 * <ul>
 * <li>items and experience land in one pile (items don't despawn, or only after the set minutes),</li>
 * <li>death in lava or the void puts the pile on the last safe block,</li>
 * <li>a set share of all experience drops,</li>
 * <li>explosions don't destroy death items,</li>
 * <li>armor, hotbar and off hand can be kept (some durability lost, never broken),</li>
 * <li>a chat line tells where the pile lies.</li>
 * </ul>
 */
public final class DeathRules {
	private DeathRules() {
	}

	/** Tag of items dropped on death, they ignore explosions. */
	public static final String DEATH_TAG = "gearbalance_death_drop";
	/** The vanilla item lifetime in ticks. */
	private static final int LIFETIME = 6000;
	/** How often the safe spot is checked, in ticks. */
	private static final int SAFE_CHECK = 5;
	/** Kept order: hotbar 0 to 8, then head, chest, legs, feet, off hand. */
	private static final EquipmentSlot[] KEPT_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
	private static final int HOTBAR = 9;

	/** Pieces kept over death, copied to the respawned player and put back there. */
	public static final AttachmentType<List<ItemStack>> KEPT = AttachmentRegistry.create(GearBalance.id("death_kept"),
			builder -> builder.persistent(ItemStack.OPTIONAL_CODEC.listOf()));

	private record Spot(ResourceKey<Level> dimension, Vec3 pos) {
	}

	private static final Map<UUID, Spot> SAFE = new HashMap<>();
	/** The player dying right now and where the pile goes; null outside of the drops. */
	private static Player dying;
	private static Vec3 pile;
	/** Where the pile of a player went, until the experience drops. */
	private static final Map<UUID, Vec3> PILES = new HashMap<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(DeathRules::tick);
		// Taken straight from the dead player when the new one is made, so nothing is lost or doubled
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				giveBack(oldPlayer, newPlayer);
			}
		});
	}

	private static boolean on(boolean option) {
		return GearConfig.enabled && option;
	}

	// ---------------------------------------------------------------- safe spot

	private static void tick(MinecraftServer server) {
		if (!on(GearConfig.deathSafe) || server.getTickCount() % SAFE_CHECK != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.onGround() && !player.isInLava() && !player.isSpectator() && safeBelow(player.level(), player.blockPosition())) {
				SAFE.put(player.getUUID(), new Spot(player.level().dimension(), player.position()));
			}
		}
	}

	/** Solid ground under the position and no lava within two blocks around it. */
	private static boolean safeBelow(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		if (lava(below) || !below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
			return false;
		}
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
			if (lava(level.getBlockState(near))) {
				return false;
			}
		}
		return true;
	}

	private static boolean lava(BlockState state) {
		return state.getFluidState().getType() == Fluids.LAVA || state.getFluidState().getType() == Fluids.FLOWING_LAVA;
	}

	/** In lava, or nothing but air and lava down to the bottom of the world. */
	private static boolean unsafe(Player player) {
		Level level = player.level();
		if (player.isInLava() || player.getY() < level.getMinY()) {
			return true;
		}
		BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
		while (pos.getY() >= level.getMinY()) {
			BlockState state = level.getBlockState(pos);
			if (lava(state)) {
				return true;
			}
			if (!state.isAir()) {
				return false;
			}
			pos.move(0, -1, 0);
		}
		return true;
	}

	// ---------------------------------------------------------------- drops

	/** Before the inventory drops: take out the kept pieces, choose the place of the pile. */
	public static void beforeDrops(Player player, ServerLevel level) {
		if (!GearConfig.enabled || level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		keep(player);
		dying = player;
		pile = player.position();
		if (on(GearConfig.deathSafe) && unsafe(player)) {
			Spot spot = SAFE.get(player.getUUID());
			if (spot != null && spot.dimension() == level.dimension()) {
				pile = spot.pos();
			}
		}
	}

	/** After all drops (with the bag): done, tell where the pile is. */
	public static void afterDrops(Player player, ServerLevel level) {
		if (dying != player) {
			return;
		}
		if (on(GearConfig.deathPoint) && player instanceof ServerPlayer serverPlayer) {
			BlockPos at = BlockPos.containing(pile);
			serverPlayer.sendSystemMessage(Component.translatable("gearbalance.death.point", at.getX(), at.getY(), at.getZ(),
					level.dimension().identifier().getPath()).withStyle(ChatFormatting.GOLD));
		}
		PILES.put(player.getUUID(), pile);
		dying = null;
	}

	/** Every item entity added while a player's drops happen: into the pile, kept longer, safe from explosions. */
	public static void onItemAdded(ItemEntity item) {
		if (dying == null) {
			return;
		}
		if (on(GearConfig.deathPile)) {
			item.setPos(pile.x, pile.y + 0.1, pile.z);
			item.setDeltaMovement(Vec3.ZERO);
			if (GearConfig.deathDespawnMinutes <= 0) {
				item.setUnlimitedLifetime();
			} else {
				((ItemEntityAccessor) item).gearbalance$setAge(Math.max(-32767, LIFETIME - GearConfig.deathDespawnMinutes * 1200));
			}
		} else if (on(GearConfig.deathSafe) && !pile.equals(dying.position())) {
			item.setPos(pile.x, pile.y + 0.1, pile.z);
		}
		if (on(GearConfig.deathExplosionProof)) {
			item.addTag(DEATH_TAG);
		}
	}

	/** Death experience: true when it was handled here (one orb at the pile). */
	public static boolean dropExperience(Player player, ServerLevel level, int amount) {
		Vec3 at = PILES.remove(player.getUUID());
		if (amount <= 0 || at == null || !(on(GearConfig.deathPile) || on(GearConfig.deathSafe))) {
			return false;
		}
		ExperienceOrb orb = new ExperienceOrb(level, at.x, at.y + 0.1, at.z, amount);
		orb.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(orb);
		return true;
	}

	/** Experience points a dying player drops: the set share of all points (vanilla: level x 7, at most 100). */
	public static int experience(Player player, int vanilla) {
		if (!on(GearConfig.deathXp)) {
			return vanilla;
		}
		long total = 0;
		for (int level = 0; level < player.experienceLevel; level++) {
			total += needed(level);
		}
		total += Math.round(player.experienceProgress * needed(player.experienceLevel));
		return (int) Math.min(Integer.MAX_VALUE, total * GearConfig.deathXpPercent / 100);
	}

	/** Vanilla experience needed from a level to the next. */
	private static int needed(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	// ---------------------------------------------------------------- keeping

	private static void keep(Player player) {
		boolean armor = on(GearConfig.deathKeepArmor);
		boolean hotbar = on(GearConfig.deathKeepHotbar);
		boolean offhand = on(GearConfig.deathKeepOffhand);
		if (!armor && !hotbar && !offhand) {
			return;
		}
		List<ItemStack> kept = new ArrayList<>();
		boolean any = false;
		for (int slot = 0; slot < HOTBAR; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (hotbar && keepable(stack)) {
				kept.add(worn(stack));
				player.getInventory().setItem(slot, ItemStack.EMPTY);
				any = true;
			} else {
				kept.add(ItemStack.EMPTY);
			}
		}
		for (EquipmentSlot slot : KEPT_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			boolean wanted = slot == EquipmentSlot.OFFHAND ? offhand : armor;
			if (wanted && keepable(stack)) {
				kept.add(worn(stack));
				player.setItemSlot(slot, ItemStack.EMPTY);
				any = true;
			} else {
				kept.add(ItemStack.EMPTY);
			}
		}
		if (any) {
			player.setAttached(KEPT, kept);
		}
	}

	/** Not empty and without curse of vanishing (those vanish as in vanilla). */
	private static boolean keepable(ItemStack stack) {
		return !stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
	}

	/** The kept copy, with the set share of its durability gone; never broken by it. */
	private static ItemStack worn(ItemStack stack) {
		ItemStack copy = stack.copy();
		if (copy.isDamageableItem() && copy.getMaxDamage() > 0 && GearConfig.deathKeepDamage > 0) {
			int loss = Math.max(1, copy.getMaxDamage() * GearConfig.deathKeepDamage / 100);
			copy.setDamageValue(Math.min(copy.getMaxDamage() - 1, copy.getDamageValue() + loss));
		}
		return copy;
	}

	/** After the respawn: the kept pieces back in their places; if taken, into the inventory or dropped. */
	private static void giveBack(ServerPlayer oldPlayer, ServerPlayer player) {
		List<ItemStack> kept = oldPlayer.getAttached(KEPT);
		if (kept == null) {
			return;
		}
		oldPlayer.removeAttached(KEPT);
		player.removeAttached(KEPT);
		for (int i = 0; i < kept.size(); i++) {
			ItemStack stack = kept.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (i < HOTBAR) {
				if (player.getInventory().getItem(i).isEmpty()) {
					player.getInventory().setItem(i, stack);
					continue;
				}
			} else if (i - HOTBAR < KEPT_SLOTS.length) {
				EquipmentSlot slot = KEPT_SLOTS[i - HOTBAR];
				if (player.getItemBySlot(slot).isEmpty()) {
					player.setItemSlot(slot, stack);
					continue;
				}
			}
			if (!player.getInventory().add(stack)) {
				player.spawnAtLocation(player.level(), stack);
			}
		}
	}
}
