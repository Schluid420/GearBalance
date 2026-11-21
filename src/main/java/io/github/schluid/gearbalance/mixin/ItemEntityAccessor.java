package io.github.schluid.gearbalance.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The age of a dropped item; it despawns at 6000. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
	@Accessor("age")
	void gearbalance$setAge(int age);
}
