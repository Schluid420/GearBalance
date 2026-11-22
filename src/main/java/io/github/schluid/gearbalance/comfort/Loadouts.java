package io.github.schluid.gearbalance.comfort;

import com.mojang.serialization.Codec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.bag.Bags;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Gear sets: three saved sets of the four worn armor pieces per player. Loading finds exactly these pieces
 * (same item, enchantments, upgrades, gems; damage ignored) in the inventory, then the bag, and puts them on.
 * Pieces worn before go where the new ones came from.
 */
public final class Loadouts {
	private Loadouts() {
	}

	public static final int SETS = 3;
	/** Slots a set holds, in this order; saving replaces the whole set. */
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
			EquipmentSlot.LEGS, EquipmentSlot.FEET);

	/** Own id of a saved piece, so equal pieces in the inventory do not count as the saved one. */
	public static DataComponentType<Long> SET_ID;

	/** The three sets, each with one stack per slot (empty = nothing saved there). Kept on death. */
	public static final AttachmentType<List<List<ItemStack>>> SAVED =
			AttachmentRegistry.create(GearBalance.id("loadouts"),
					builder -> builder.persistent(ItemStack.OPTIONAL_CODEC.listOf().listOf()).copyOnDeath());

	public static final int MODE_LOAD = 0;
	public static final int MODE_SAVE = 1;
	public static final int MODE_DELETE = 2;

	/** Client to server: put on, save or delete a set (mode). */
	public record UsePayload(int set, int mode) implements CustomPacketPayload {
		public static final Type<UsePayload> TYPE = new Type<>(GearBalance.id("loadout_use"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UsePayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, UsePayload::set, ByteBufCodecs.VAR_INT, UsePayload::mode, UsePayload::new);

		@Override
		public Type<UsePayload> type() {
			return TYPE;
		}
	}

	/** Server to client: the saved sets, for the tooltips of the buttons. */
	public record SetsPayload(List<List<ItemStack>> sets) implements CustomPacketPayload {
		public static final Type<SetsPayload> TYPE = new Type<>(GearBalance.id("loadouts"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetsPayload> CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
				SetsPayload::sets, SetsPayload::new);

		@Override
		public Type<SetsPayload> type() {
			return TYPE;
		}
	}

	public static final int MSG_SAVED = 0;
	public static final int MSG_LOADED = 1;
	public static final int MSG_EMPTY = 2;
	public static final int MSG_MISSING = 3;
	public static final int MSG_DELETED = 4;

	/** Server to client: a message for the inventory window (the action bar lies behind it). */
	public record MessagePayload(int kind, int set, int count) implements CustomPacketPayload {
		public static final Type<MessagePayload> TYPE = new Type<>(GearBalance.id("loadout_message"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MessagePayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, MessagePayload::kind, ByteBufCodecs.VAR_INT, MessagePayload::set,
				ByteBufCodecs.VAR_INT, MessagePayload::count, MessagePayload::new);

		@Override
		public Type<MessagePayload> type() {
			return TYPE;
		}
	}

	/** Color of a set: yellow, green, red. */
	public static ChatFormatting color(int set) {
		return switch (set) {
			case 0 -> ChatFormatting.YELLOW;
			case 1 -> ChatFormatting.GREEN;
			default -> ChatFormatting.RED;
		};
	}

	/** RGB of the set color, for the number on the button. */
	public static int rgb(int set) {
		return switch (set) {
			case 0 -> 0xFFFF55;
			case 1 -> 0x55FF55;
			default -> 0xFF5555;
		};
	}

	/** Message text with the set number in the color of the set. */
	public static Component text(int kind, int set, int count) {
		Component number = Component.literal(String.valueOf(set + 1)).withStyle(color(set));
		return switch (kind) {
			case MSG_SAVED -> Component.translatable("gearbalance.loadout.saved", number).withStyle(ChatFormatting.GREEN);
			case MSG_LOADED -> Component.translatable("gearbalance.loadout.loaded", number).withStyle(ChatFormatting.GREEN);
			case MSG_EMPTY -> Component.translatable("gearbalance.loadout.empty", number).withStyle(ChatFormatting.GRAY);
			case MSG_MISSING -> Component.translatable("gearbalance.loadout.missing", number, count)
					.withStyle(ChatFormatting.GOLD);
			default -> Component.translatable("gearbalance.loadout.deleted", number).withStyle(ChatFormatting.RED);
		};
	}

	private static void say(ServerPlayer player, int kind, int set, int count) {
		if (ServerPlayNetworking.canSend(player, MessagePayload.TYPE)) {
			ServerPlayNetworking.send(player, new MessagePayload(kind, set, count));
		} else {
			player.sendOverlayMessage(text(kind, set, count));
		}
	}

	public static void register() {
		SET_ID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GearBalance.id("set_id"),
				DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
		PayloadTypeRegistry.serverboundPlay().register(UsePayload.TYPE, UsePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SetsPayload.TYPE, SetsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MessagePayload.TYPE, MessagePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UsePayload.TYPE, (payload, context) -> use(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));
	}

	/** The saved sets of the player, always SETS lists of SLOTS stacks. */
	public static List<List<ItemStack>> sets(ServerPlayer player) {
		List<List<ItemStack>> stored = player.getAttachedOrElse(SAVED, List.of());
		List<List<ItemStack>> result = new ArrayList<>();
		for (int set = 0; set < SETS; set++) {
			List<ItemStack> pieces = new ArrayList<>();
			List<ItemStack> saved = set < stored.size() ? stored.get(set) : List.of();
			for (int slot = 0; slot < SLOTS.size(); slot++) {
				pieces.add(slot < saved.size() ? saved.get(slot) : ItemStack.EMPTY);
			}
			result.add(pieces);
		}
		return result;
	}

	private static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, SetsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new SetsPayload(sets(player)));
		}
	}

	private static void use(ServerPlayer player, UsePayload payload) {
		if (!GearConfig.loadoutsActive() || payload.set() < 0 || payload.set() >= SETS) {
			return;
		}
		switch (payload.mode()) {
			case MODE_SAVE -> save(player, payload.set());
			case MODE_DELETE -> delete(player, payload.set());
			default -> load(player, payload.set());
		}
	}

	private static void delete(ServerPlayer player, int set) {
		List<List<ItemStack>> sets = sets(player);
		sets.set(set, new ArrayList<>(Collections.nCopies(SLOTS.size(), ItemStack.EMPTY)));
		player.setAttached(SAVED, sets);
		send(player);
		say(player, MSG_DELETED, set, 0);
	}

	private static void save(ServerPlayer player, int set) {
		List<List<ItemStack>> sets = sets(player);
		List<ItemStack> pieces = new ArrayList<>();
		for (EquipmentSlot slot : SLOTS) {
			ItemStack worn = player.getItemBySlot(slot);
			if (!worn.isEmpty() && !worn.has(SET_ID)) {
				worn.set(SET_ID, player.getRandom().nextLong());
			}
			pieces.add(template(worn));
		}
		sets.set(set, pieces);
		player.setAttached(SAVED, sets);
		send(player);
		say(player, MSG_SAVED, set, 0);
	}

	private static void load(ServerPlayer player, int set) {
		List<ItemStack> pieces = sets(player).get(set);
		if (pieces.stream().allMatch(ItemStack::isEmpty)) {
			say(player, MSG_EMPTY, set, 0);
			return;
		}
		Inventory inventory = player.getInventory();
		int missing = 0;
		for (int i = 0; i < SLOTS.size(); i++) {
			ItemStack wanted = pieces.get(i);
			EquipmentSlot slot = SLOTS.get(i);
			if (wanted.isEmpty() || same(player.getItemBySlot(slot), wanted)) {
				continue;
			}
			int found = find(inventory, wanted);
			if (found >= 0) {
				ItemStack piece = inventory.getItem(found);
				inventory.setItem(found, player.getItemBySlot(slot));
				player.setItemSlot(slot, piece);
			} else if (!fromBag(player, wanted, slot)) {
				missing++;
			}
		}
		say(player, missing > 0 ? MSG_MISSING : MSG_LOADED, set, missing);
	}

	/** Takes a matching piece out of the worn bag; the piece worn before goes to its place. */
	private static boolean fromBag(ServerPlayer player, ItemStack wanted, EquipmentSlot slot) {
		ItemStack bag = Bags.worn(player);
		if (bag.isEmpty() || !Bags.active()) {
			return false;
		}
		NonNullList<ItemStack> items = NonNullList.withSize(Bags.SLOTS, ItemStack.EMPTY);
		bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
		for (int i = 0; i < items.size(); i++) {
			if (same(items.get(i), wanted)) {
				ItemStack piece = items.get(i);
				items.set(i, player.getItemBySlot(slot).copy());
				bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
				player.setItemSlot(slot, piece);
				return true;
			}
		}
		return false;
	}

	/** Slot of a matching piece in the inventory (without the worn ones), -1 if there is none. */
	private static int find(Inventory inventory, ItemStack wanted) {
		for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
			if (same(inventory.getItem(i), wanted)) {
				return i;
			}
		}
		return -1;
	}

	/** The piece as it is saved: one item, undamaged. */
	private static ItemStack template(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack copy = stack.copyWithCount(1);
		copy.remove(DataComponents.DAMAGE);
		return copy;
	}

	/** Same piece: same item and components, the damage does not count. */
	public static boolean same(ItemStack stack, ItemStack wanted) {
		return !stack.isEmpty() && ItemStack.isSameItemSameComponents(template(stack), wanted);
	}
}
