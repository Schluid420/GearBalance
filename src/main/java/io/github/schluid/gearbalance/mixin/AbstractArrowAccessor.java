package io.github.schluid.gearbalance.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the base damage of an arrow (vanilla has only a setter). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
	@Accessor("baseDamage")
	double gearbalance$baseDamage();
}
