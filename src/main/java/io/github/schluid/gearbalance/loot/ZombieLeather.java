package io.github.schluid.gearbalance.loot;

import io.github.schluid.gearbalance.config.GearConfig;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/** Optional leather drop for zombies. Checked on every drop, so the setting applies at once. */
public final class ZombieLeather {
	private ZombieLeather() {
	}

	private static final Set<ResourceKey<LootTable>> TABLES = new HashSet<>();

	public static void register() {
		for (EntityType<?> type : new EntityType<?>[] {
				EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER}) {
			type.getDefaultLootTable().ifPresent(TABLES::add);
		}

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!GearConfig.zombieLeather) {
				return;
			}
			if (table.unwrapKey().filter(TABLES::contains).isEmpty()) {
				return;
			}
			if (context.getRandom().nextInt(100) < GearConfig.zombieLeatherChance) {
				drops.add(new ItemStack(Items.LEATHER));
			}
		});
	}
}
