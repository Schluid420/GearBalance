package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.upgrade.UpgradeValues;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** An upgraded trident also hits harder when thrown (vanilla: 8 damage). */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
	@ModifyConstant(method = "onHitEntity", constant = @Constant(floatValue = 8.0F))
	private float gearbalance$thrownDamage(float damage) {
		ItemStack weapon = ((ThrownTrident) (Object) this).getWeaponItem();
		return weapon == null ? damage : (float) (damage * Upgrades.factor(weapon, UpgradeValues.Value.DAMAGE));
	}
}
