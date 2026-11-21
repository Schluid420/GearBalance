package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.level.LevelRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spear jab and charge end in stabAttack, not the normal attack; a locked spear deals no damage. */
@Mixin(Player.class)
public abstract class PlayerStabMixin {
	@Inject(method = "stabAttack", at = @At("HEAD"), cancellable = true)
	private void gearbalance$noStabWhenLocked(EquipmentSlot slot, Entity target, float damage, boolean dealsDamage,
			boolean knocksBack, boolean dismounts, CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;
		ItemStack stack = player.getItemBySlot(slot);
		if (LevelRules.isLocked(player, stack)) {
			LevelRules.showLockedHint(player, stack);
			cir.setReturnValue(false);
		}
	}
}
