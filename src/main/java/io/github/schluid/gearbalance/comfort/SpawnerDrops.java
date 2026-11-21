package io.github.schluid.gearbalance.comfort;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mined spawners (survival) give iron and a chance of a chipped gem. Optional: with a netherite or titanium
 * pickaxe and Silk Touch the whole spawner drops, keeps its mob and spawns it again when placed.
 */
public final class SpawnerDrops {
	private SpawnerDrops() {
	}

	/** The mob a picked up spawner spawns. */
	public static DataComponentType<Identifier> MOB;

	private static final String[] GEMS = {"ruby", "sapphire", "topaz", "emerald", "aquamarine", "onyx"};

	public static void register() {
		MOB = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GearBalance.id("spawner_mob"),
				DataComponentType.<Identifier>builder().persistent(Identifier.CODEC)
						.networkSynchronized(Identifier.STREAM_CODEC).build());
		PlayerBlockBreakEvents.AFTER.register(SpawnerDrops::broken);
	}

	private static void broken(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel server) || !state.is(Blocks.SPAWNER) || player.isCreative()
				|| !GearConfig.enabled || !GearConfig.spawnerDrops) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		if (GearConfig.spawnerWhole && wholeTool(server, tool) && blockEntity instanceof SpawnerBlockEntity spawner) {
			Entity shown = spawner.getSpawner().getOrCreateDisplayEntity(server, pos);
			ItemStack item = new ItemStack(Items.SPAWNER);
			if (shown != null) {
				EntityType<?> type = shown.getType();
				item.set(MOB, EntityType.getKey(type));
				item.set(DataComponents.LORE, new ItemLore(List.of(
						Component.translatable(type.getDescriptionId()).withStyle(ChatFormatting.GRAY))));
			}
			drop(server, pos, item);
			return;
		}
		if (GearConfig.spawnerIron > 0) {
			drop(server, pos, new ItemStack(Items.IRON_INGOT, GearConfig.spawnerIron));
		}
		if (GearConfig.socketsActive() && server.getRandom().nextInt(100) < GearConfig.spawnerGemChance) {
			ItemStack gem = randomGem(server);
			if (!gem.isEmpty()) {
				drop(server, pos, gem);
			}
		}
	}

	/** Netherite or titanium pickaxe with Silk Touch. */
	private static boolean wholeTool(ServerLevel level, ItemStack tool) {
		Identifier id = BuiltInRegistries.ITEM.getKey(tool.getItem());
		boolean pickaxe = id.equals(Identifier.withDefaultNamespace("netherite_pickaxe"))
				|| id.equals(GearBalance.id("titanium_pickaxe"));
		if (!pickaxe) {
			return false;
		}
		Holder<net.minecraft.world.item.enchantment.Enchantment> silk = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		return EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0;
	}

	/** One random chipped gem of all sorts. */
	private static ItemStack randomGem(ServerLevel level) {
		List<Item> all = new ArrayList<>();
		for (String gem : GEMS) {
			TagKey<Item> tag = TagKey.create(Registries.ITEM, GearBalance.id("gems/chipped_" + gem));
			for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
				all.add(holder.value());
			}
		}
		return all.isEmpty() ? ItemStack.EMPTY : new ItemStack(all.get(level.getRandom().nextInt(all.size())));
	}

	private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
		net.minecraft.world.level.block.Block.popResource(level, pos, stack);
	}

	/** A placed spawner that was picked up with its mob spawns that mob again. */
	public static void placed(Level level, BlockPos pos, ItemStack stack) {
		if (level.isClientSide() || MOB == null || !stack.has(MOB)) {
			return;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(stack.get(MOB));
		if (type != null && level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
			spawner.setEntityId(type, level.getRandom());
			spawner.setChanged();
		}
	}
}
