package io.github.schluid.gearbalance.client;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.socket.Sockets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model condition "gearbalance:full_sockets": true when every socket holds a gem (outlined model in hand). */
public record FullSocketsProperty() implements ConditionalItemModelProperty {
	public static final MapCodec<FullSocketsProperty> MAP_CODEC = MapCodec.unit(new FullSocketsProperty());

	/** Before the first resource load, so the item definitions can name the condition. */
	public static void register() {
		ConditionalItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "full_sockets"), MAP_CODEC);
	}

	@Override
	public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed,
			ItemDisplayContext context) {
		if (!GearConfig.enabled || !GearConfig.outlines || !Sockets.active()) {
			return false;
		}
		int max = Sockets.max(stack);
		return max > 0 && Sockets.count(stack) >= max && !Sockets.hasEmptySocket(stack);
	}

	@Override
	public MapCodec<FullSocketsProperty> type() {
		return MAP_CODEC;
	}
}
