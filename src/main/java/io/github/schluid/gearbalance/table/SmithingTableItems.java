package io.github.schluid.gearbalance.table;

import io.github.schluid.gearbalance.GearBalance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Items left in a smithing table (like EnchantPlus' enchanting table). No block entity, so they are stored per dimension:
 * position -> three input items. Breaking the table drops them; if it vanishes otherwise they drop at the old place on the next check.
 */
public final class SmithingTableItems {
	private SmithingTableItems() {
	}

	/** Position (BlockPos.asLong as text) -> items of the three input slots. */
	public static final AttachmentType<Map<String, List<ItemStack>>> TABLES =
			AttachmentRegistry.create(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "smithing_table_items"),
					builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC.listOf())));

	/** Ticks between two checks for tables that are gone. */
	private static final int CHECK_INTERVAL = 100;

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server) {
				drop(server, pos);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL == 0) {
				for (ServerLevel level : server.getAllLevels()) {
					dropMissing(level);
				}
			}
		});
	}

	private static Map<String, List<ItemStack>> all(Level level) {
		return level.getAttachedOrElse(TABLES, Map.of());
	}

	private static String key(BlockPos pos) {
		return String.valueOf(pos.asLong());
	}

	/** Takes the items out of the table (empty list if there are none). */
	public static List<ItemStack> take(ServerLevel level, BlockPos pos) {
		Map<String, List<ItemStack>> map = all(level);
		List<ItemStack> items = map.get(key(pos));
		if (items == null) {
			return List.of();
		}
		Map<String, List<ItemStack>> copy = new HashMap<>(map);
		copy.remove(key(pos));
		set(level, copy);
		return items;
	}

	/** Stores the items. Returns false (nothing stored) if the table already holds items; the player then gets them back as in vanilla. */
	public static boolean store(ServerLevel level, BlockPos pos, List<ItemStack> items) {
		Map<String, List<ItemStack>> map = all(level);
		if (map.containsKey(key(pos))) {
			return false;
		}
		Map<String, List<ItemStack>> copy = new HashMap<>(map);
		copy.put(key(pos), List.copyOf(items));
		set(level, copy);
		return true;
	}

	/** Drops the items of a table at its place. */
	public static void drop(ServerLevel level, BlockPos pos) {
		for (ItemStack stack : take(level, pos)) {
			if (!stack.isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
	}

	/** Drops the items of every loaded place that is no smithing table any more. */
	private static void dropMissing(ServerLevel level) {
		Map<String, List<ItemStack>> map = all(level);
		if (map.isEmpty()) {
			return;
		}
		for (String key : List.copyOf(map.keySet())) {
			BlockPos pos;
			try {
				pos = BlockPos.of(Long.parseLong(key));
			} catch (NumberFormatException e) {
				continue;
			}
			if (level.isLoaded(pos) && !level.getBlockState(pos).is(Blocks.SMITHING_TABLE)) {
				drop(level, pos);
			}
		}
	}

	private static void set(ServerLevel level, Map<String, List<ItemStack>> map) {
		if (map.isEmpty()) {
			level.removeAttached(TABLES);
		} else {
			level.setAttached(TABLES, Map.copyOf(map));
		}
	}
}
