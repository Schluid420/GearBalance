package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.advancement.GearAdvancements;
import io.github.schluid.gearbalance.cooldown.Cooldowns;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.hammer.ForgeHammers;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import io.github.schluid.gearbalance.padded.SewingNeedle;
import io.github.schluid.gearbalance.smith.LeverRecipe;
import io.github.schluid.gearbalance.smith.SmithingTools;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Titanite;
import io.github.schluid.gearbalance.socket.SocketChisels;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.tool.ToolDurability;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smithing table: forge hammer, socket chisel and sewing needle lose one use per result.
 * A socket takes all its amethyst shards. GearBalance results play sound, particles and advancements.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {
	@Unique
	private static final int NONE = 0, PADDING = 1, UPGRADE = 2, SOCKET = 3, GEM = 4, REPAIR = 5, LEVER = 6;

	/** Gem that the gem lever takes out right now, given back after the take. */
	@Unique
	private String gearbalance$leverGem = "";

	/** Amethyst shards of the socket that is being taken right now, 0 otherwise. */
	@Unique
	private int gearbalance$amethystCost;
	/** What the result being taken right now is (NONE, PADDING ...). */
	@Unique
	private int gearbalance$kind;
	/** Position of the smithing table, for sound and particles. */
	@Unique
	private ContainerLevelAccess gearbalance$access = ContainerLevelAccess.NULL;
	/** The tool in the left slot broke while the result was taken. */
	@Unique
	private boolean gearbalance$toolBroke;

	@Unique
	private ItemStack gearbalance$item(int slot) {
		return ((SmithingMenu) (Object) this).getSlot(slot).getItem();
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void gearbalance$keepAccess(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		gearbalance$access = access;
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void gearbalance$beforeTake(Player player, ItemStack result, CallbackInfo ci) {
		gearbalance$amethystCost = 0;
		ItemStack tool = gearbalance$item(SmithingMenu.TEMPLATE_SLOT);
		ItemStack addition = gearbalance$item(SmithingMenu.ADDITIONAL_SLOT);
		if (SocketChisels.isChisel(tool)) {
			gearbalance$amethystCost = Sockets.amethystCost(gearbalance$item(SmithingMenu.BASE_SLOT));
			gearbalance$kind = SOCKET;
		} else if (SewingNeedle.isNeedle(tool)) {
			gearbalance$kind = PADDING;
		} else if (ForgeHammers.isHammer(tool) && BreakProtection.marked(gearbalance$item(SmithingMenu.BASE_SLOT))) {
			// Repair of a damaged piece (break protection)
			gearbalance$kind = REPAIR;
		} else if (ForgeHammers.isHammer(tool)) {
			gearbalance$kind = Gems.isGem(addition) || Titanite.is(addition) ? GEM : UPGRADE;
		} else if (SmithingTools.isRepairKit(tool)) {
			gearbalance$kind = REPAIR;
		} else if (SmithingTools.isLever(tool)) {
			gearbalance$kind = LEVER;
			ItemStack piece = gearbalance$item(SmithingMenu.BASE_SLOT);
			int index = LeverRecipe.lastGem(piece);
			gearbalance$leverGem = index >= 0 ? Sockets.sockets(piece).get(index) : "";
		} else {
			gearbalance$kind = NONE;
		}
		// Wait time against upgrade spam starts with this result
		if (gearbalance$kind == UPGRADE || gearbalance$kind == SOCKET) {
			Cooldowns.mark(result, player.level().getGameTime(), gearbalance$kind == UPGRADE);
		}
	}

	@Inject(method = "onTake", at = @At("RETURN"))
	private void gearbalance$afterTake(Player player, ItemStack result, CallbackInfo ci) {
		int kind = gearbalance$kind;
		gearbalance$kind = NONE;
		if (gearbalance$toolBroke) {
			gearbalance$toolBroke = false;
			gearbalance$access.execute((level, pos) -> {
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
				}
			});
		}
		switch (kind) {
			case PADDING -> {
				if (PaddedArmor.isPadded(result)) {
					GearAdvancements.award(player, GearAdvancements.PADDED);
				}
				gearbalance$effects(SoundEvents.WOOL_PLACE, 1.0F, ParticleTypes.WHITE_ASH, 20);
			}
			case UPGRADE -> {
				int level = Upgrades.storedLevel(result);
				if (level > 0) {
					GearAdvancements.award(player, GearAdvancements.UPGRADE);
				}
				if (level >= Upgrades.MAX_LEVEL) {
					GearAdvancements.award(player, GearAdvancements.UPGRADE_MAX);
					gearbalance$effects(SoundEvents.PLAYER_LEVELUP, 1.2F, ParticleTypes.ENCHANTED_HIT, 30);
				} else {
					gearbalance$effects(SoundEvents.ANVIL_USE, 1.3F, ParticleTypes.CRIT, 15);
				}
			}
			case SOCKET -> {
				GearAdvancements.award(player, GearAdvancements.SOCKET);
				gearbalance$effects(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0F, ParticleTypes.ELECTRIC_SPARK, 12);
			}
			case GEM -> {
				GearAdvancements.award(player, GearAdvancements.GEM);
				int gems = 0;
				for (String gem : Sockets.sockets(result)) {
					if (!gem.isEmpty()) {
						gems++;
					}
				}
				// Every socket the piece can have holds a gem
				if (gems >= Math.max(1, Sockets.max(result))) {
					GearAdvancements.award(player, GearAdvancements.GEMS_FULL);
				}
				gearbalance$effects(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, ParticleTypes.END_ROD, 12);
			}
			case REPAIR -> gearbalance$effects(SoundEvents.ANVIL_USE, 0.8F, ParticleTypes.WAX_OFF, 15);
			case LEVER -> {
				// The gem comes back whole
				Item gem = gearbalance$leverGem.isEmpty() ? null : Gems.byId(gearbalance$leverGem);
				gearbalance$leverGem = "";
				if (gem != null && !player.level().isClientSide()) {
					player.getInventory().placeItemBackInInventory(new ItemStack(gem), Prediction.SERVER_ONLY);
				}
				gearbalance$effects(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, ParticleTypes.END_ROD, 10);
			}
			default -> {
			}
		}
	}

	/** Sound and particles above the smithing table, sent from the server to everyone nearby. */
	@Unique
	private void gearbalance$effects(SoundEvent sound, float pitch, ParticleOptions particle, int count) {
		gearbalance$access.execute((level, pos) -> {
			if (level instanceof ServerLevel serverLevel) {
				serverLevel.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, pitch);
				serverLevel.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
						count, 0.25, 0.1, 0.25, 0.05);
			}
		});
	}

	@Inject(method = "shrinkStackInSlot", at = @At("HEAD"), cancellable = true)
	private void gearbalance$shrink(int slot, CallbackInfo ci) {
		if (slot == SmithingMenu.TEMPLATE_SLOT) {
			ItemStack tool = gearbalance$item(slot);
			if (ForgeHammers.isHammer(tool) || SocketChisels.isChisel(tool) || SewingNeedle.isNeedle(tool)
					|| SmithingTools.isRepairKit(tool) || SmithingTools.isLever(tool)) {
				if (ToolDurability.wear(((SmithingMenu) (Object) this).getSlot(slot))) {
					gearbalance$toolBroke = true;
				}
				ci.cancel();
			}
		} else if (slot == SmithingMenu.ADDITIONAL_SLOT && gearbalance$amethystCost > 0) {
			Slot shards = ((SmithingMenu) (Object) this).getSlot(slot);
			ItemStack stack = shards.getItem();
			stack.shrink(gearbalance$amethystCost);
			shards.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
			gearbalance$amethystCost = 0;
			ci.cancel();
		}
	}

	/** Break protection: a damaged piece must be repaired first; only taking gems out with a gem lever works. */
	@Inject(method = "createResult", at = @At("TAIL"))
	private void gearbalance$onlyRepairDamaged(CallbackInfo ci) {
		SmithingMenu menu = (SmithingMenu) (Object) this;
		if (BreakProtection.marked(gearbalance$item(SmithingMenu.BASE_SLOT))
				&& !SmithingTools.isLever(gearbalance$item(SmithingMenu.TEMPLATE_SLOT))
				&& BreakProtection.marked(menu.getSlot(SmithingMenu.RESULT_SLOT).getItem())) {
			menu.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		}
	}
}
