package io.github.schluid.gearbalance.shield;

import io.github.schluid.gearbalance.titanium.Titanium;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Shields of wood, copper, iron, gold, diamond and netherite. Like vanilla (banners, blocking), but better per material:
 * more durability, faster block, shorter axe disable, wider explosion block.
 * The wooden shield is the base tier; metal ones are made from it (or a vanilla shield). With the switch on, the vanilla recipe is gone.
 * Values come from the settings: durability at once, the rest is built into the item on world load or join.
 */
public final class Shields {
	private Shields() {
	}

	/** One shield. repairItem null: repaired with planks (wooden tool materials). */
	public record Tier(String material, String path, Item repairItem) {
	}

	/** An axe disables a vanilla shield for 5 seconds; the axe time of a shield is this times its scale. */
	private static final float VANILLA_AXE_SECONDS = 5.0F;

	/** Same angle for normal hits on every shield (vanilla 90). */
	public static final float BLOCK_ANGLE = 90.0F;

	public static final List<Tier> TIERS = List.of(
			new Tier("wood", "wooden_shield", null),
			new Tier("copper", "copper_shield", Items.COPPER_INGOT),
			new Tier("iron", "iron_shield", Items.IRON_INGOT),
			new Tier("gold", "golden_shield", Items.GOLD_INGOT),
			new Tier("diamond", "diamond_shield", Items.DIAMOND),
			new Tier("netherite", "netherite_shield", Items.NETHERITE_INGOT),
			// Repaired with titanium ingots (tag, the ingot is registered later)
			new Tier("titanium", "titanium_shield", null));

	/** Material of the vanilla shield and the wooden shield. */
	public static final String WOOD = "wood";

	/** Shield item -> material key. */
	private static final Map<Item, String> MATERIAL = new LinkedHashMap<>();
	/** Material key -> shield item of GearBalance. */
	private static final Map<String, Item> BY_MATERIAL = new LinkedHashMap<>();

	public static void register() {
		for (Tier tier : TIERS) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, tier.path()));
			String material = tier.material();
			Item.Properties properties = new Item.Properties().setId(key)
					.durability(GearConfig.SHIELD_DURABILITY.get(material))
					.component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
					.equippableUnswappable(EquipmentSlot.OFFHAND)
					// Delayed: built when a world is loaded or joined, so on a server its settings count
					.delayedComponent(DataComponents.BLOCKS_ATTACKS, registries -> blocksAttacks(material, registries))
					.delayedComponent(DataComponents.USE_EFFECTS, registries -> useEffects(material))
					.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
			if (tier.repairItem() != null) {
				properties.repairable(tier.repairItem());
			} else if (tier.material().equals(Titanium.MATERIAL)) {
				properties.repairable(Titanium.TOOL_MATERIALS);
			} else {
				properties.repairable(ItemTags.WOODEN_TOOL_MATERIALS);
			}
			if (tier.material().equals("netherite") || tier.material().equals(Titanium.MATERIAL)) {
				properties.fireResistant();
			}
			Item item = Registry.register(BuiltInRegistries.ITEM, key, new ShieldItem(properties));
			MATERIAL.put(item, tier.material());
			BY_MATERIAL.put(tier.material(), item);
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> BY_MATERIAL.values().forEach(output::accept));
	}

	/** Block delay, axe time, blocked damage and explosion angle of a shield from the settings. */
	private static BlocksAttacks blocksAttacks(String material, HolderLookup.Provider registries) {
		float blocked = GearConfig.SHIELD_BLOCK.get(material) / 100.0F;
		return new BlocksAttacks((float) (double) GearConfig.SHIELD_BLOCK_DELAY.get(material),
				(float) (GearConfig.SHIELD_AXE_SECONDS.get(material) / VANILLA_AXE_SECONDS),
				List.of(new BlocksAttacks.DamageReduction(BLOCK_ANGLE, Optional.empty(), 0.0F, blocked),
						// Explosions are also blocked from further to the side
						new BlocksAttacks.DamageReduction(GearConfig.SHIELD_EXPLOSION_ANGLE.get(material),
								Optional.of(registries.getOrThrow(DamageTypeTags.IS_EXPLOSION)), 0.0F, blocked)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(registries.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
				Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK));
	}

	/** Walking speed while blocking: from the settings with "Shield speed" on, else vanilla (20 %). */
	private static UseEffects useEffects(String material) {
		float speed = GearConfig.shieldSpeedActive() ? GearConfig.SHIELD_SPEED.get(material) / 100.0F : 0.2F;
		return new UseEffects(false, true, speed);
	}

	/** Material key of a shield ("wood" for the vanilla and the wooden shield), null for anything else. */
	public static String material(Item item) {
		return item == Items.SHIELD ? WOOD : MATERIAL.get(item);
	}

	/** Durability of a GearBalance shield from the settings (applies at once), 0 for any other item. */
	public static int maxDamage(ItemStack stack) {
		String material = stack.isEmpty() ? null : MATERIAL.get(stack.getItem());
		Integer durability = material == null ? null : GearConfig.SHIELD_DURABILITY.get(material);
		return durability == null ? 0 : durability;
	}

	public static boolean isShield(Item item) {
		return material(item) != null;
	}

	public static boolean isShield(ItemStack stack) {
		return !stack.isEmpty() && isShield(stack.getItem());
	}

	/** The GearBalance shield of a material, null if there is none. */
	public static Item of(String material) {
		return BY_MATERIAL.get(material);
	}

	/** The items plus vanilla and wooden shield for the lowest tier ("leather"): sockets and gems, but no upgrades (no compressed wood). */
	public static List<Item> withVanilla(String material, List<Item> items) {
		if (!material.equals("leather")) {
			return items;
		}
		List<Item> result = new java.util.ArrayList<>(items);
		result.add(Items.SHIELD);
		if (of(WOOD) != null) {
			result.add(of(WOOD));
		}
		return result;
	}

	/** All GearBalance shields. */
	public static List<Item> all() {
		return List.copyOf(BY_MATERIAL.values());
	}
}
