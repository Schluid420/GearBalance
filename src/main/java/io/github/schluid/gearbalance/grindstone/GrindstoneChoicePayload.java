package io.github.schluid.gearbalance.grindstone;

import io.github.schluid.gearbalance.GearBalance;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.GrindstoneMenu;

/** Client to server: a GearBalance grindstone button was pressed. Own packet, so other mods' (EnchantPlus) buttons are unaffected. */
public record GrindstoneChoicePayload(int choice) implements CustomPacketPayload {
	public static final Type<GrindstoneChoicePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "grindstone_choice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GrindstoneChoicePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, GrindstoneChoicePayload::choice, GrindstoneChoicePayload::new);

	@Override
	public Type<GrindstoneChoicePayload> type() {
		return TYPE;
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof GrindstoneMenu menu && menu instanceof GrindstoneChoice choice) {
				choice.gearbalance$choose(payload.choice());
			}
		});
	}
}
