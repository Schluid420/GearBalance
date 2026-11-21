package io.github.schluid.gearbalance.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.socket.Sockets;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model select "gearbalance:hand_effect": "off" without full sockets, else the menu effect; falls back to "glow". */
public record HandEffectProperty() implements SelectItemModelProperty<String> {
	public static final SelectItemModelProperty.Type<HandEffectProperty, String> TYPE =
			SelectItemModelProperty.Type.create(MapCodec.unit(new HandEffectProperty()), Codec.STRING);

	/** Before the first resource load, so the item definitions can name the property. */
	public static void register() {
		SelectItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "hand_effect"), TYPE);
	}

	@Override
	public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed,
			ItemDisplayContext context) {
		if (!glowing(stack)) {
			return "off";
		}
		return switch (GearConfig.handEffect) {
			case GearConfig.EFFECT_FLAMES -> "flames";
			case GearConfig.EFFECT_SHADOW -> "shadow";
			default -> "glow";
		};
	}

	/** True while the piece shows the glow or an effect: switch on and every socket filled. */
	public static boolean glowing(ItemStack stack) {
		if (!GearConfig.enabled || !GearConfig.outlines || !Sockets.active()) {
			return false;
		}
		int max = Sockets.max(stack);
		return max > 0 && Sockets.count(stack) >= max && !Sockets.hasEmptySocket(stack);
	}

	@Override
	public Codec<String> valueCodec() {
		return Codec.STRING;
	}

	@Override
	public SelectItemModelProperty.Type<HandEffectProperty, String> type() {
		return TYPE;
	}
}
