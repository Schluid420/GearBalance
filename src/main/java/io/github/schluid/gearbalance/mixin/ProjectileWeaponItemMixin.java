package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.upgrade.UpgradeValues;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Upgraded bows and crossbows: arrows shot from them deal more damage. */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
	@Inject(method = "createProjectile", at = @At("RETURN"))
	private void gearbalance$arrowDamage(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit,
			CallbackInfoReturnable<Projectile> cir) {
		if (!(cir.getReturnValue() instanceof AbstractArrow arrow)) {
			return;
		}
		double factor = Upgrades.factor(weapon, UpgradeValues.Value.ARROW_DAMAGE);
		if (factor != 1) {
			arrow.setBaseDamage(((AbstractArrowAccessor) arrow).gearbalance$baseDamage() * factor);
		}
	}
}
