package io.github.schluid.gearbalance.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.gearbalance.comfort.DeathRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Death experience of a player: one orb on the pile instead of many orbs flying around. */
@Mixin(LivingEntity.class)
public abstract class DeathExperienceMixin {
	@WrapOperation(method = "dropExperience", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
	private void gearbalance$experiencePile(ServerLevel level, Vec3 pos, int amount, Operation<Void> original) {
		if ((Object) this instanceof Player player && DeathRules.dropExperience(player, level, amount)) {
			return;
		}
		original.call(level, pos, amount);
	}
}
