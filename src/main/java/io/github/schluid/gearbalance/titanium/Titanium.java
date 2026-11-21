package io.github.schluid.gearbalance.titanium;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.api.GearBalanceApi;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Titanium, the material after netherite. Deepslate titanium ore (below Y 0, rare as ancient debris, netherite pickaxe only),
 * blast furnace = fragments; 4 fragments + netherite ingot = titanium ingot. Netherite gear + ingot + upgrade template (end city
 * chests) = titanium gear: ~15 % better, fire resistant, armor lowers fall damage. Items always exist; the "Titanium" switch
 * controls ore, recipes, chest template and fall damage.
 */
public final class Titanium {
	private Titanium() {
	}

	public static final String MATERIAL = "titanium";

	public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, GearBalance.id("titanium_tool_materials"));
	public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, GearBalance.id("repairs_titanium_armor"));

	/** 15 % more durability, 1 more damage and a faster mining speed than netherite. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2336, 10.0F, 5.0F, 15,
			TOOL_MATERIALS);
	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GearBalance.id(MATERIAL));
	/** Durability factor 43 (netherite 37), armor 4/9/7/3 (netherite 3/8/6/3), toughness 3.5 (netherite 3). */
	public static final ArmorMaterial ARMOR = new ArmorMaterial(43,
			Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 22),
			15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.1F, REPAIRS_ARMOR, ASSET);

	public static final ResourceKey<PlacedFeature> ORE_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, GearBalance.id("ore_titanium"));

	public static Block ORE;
	public static Block BLOCK;
	public static Item FRAGMENT;
	public static Item INGOT;
	public static Item TEMPLATE;
	public static Item SWORD, SHOVEL, PICKAXE, AXE, HOE, SPEAR;
	public static Item HELMET, CHESTPLATE, LEGGINGS, BOOTS;

	/** Chance per end city chest for a titanium upgrade smithing template. */
	private static final float TEMPLATE_CHANCE = 0.10F;

	/** Titanium as a GearBalance material with items and default values. Called while material registration is open, before settings are read. */
	public static void registerGear() {
		GearBalanceApi.registerMaterial(MATERIAL, GearBalance.MOD_ID + ":titanium_", "gearbalance.material.titanium",
				true, true, "netherite", "toughness");
		String prefix = GearBalance.MOD_ID + ":titanium_";
		GearBalanceApi.registerItem(prefix + "sword", "weapon", 2336, 9, 1.6, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "spear", "weapon", 2336, 6, 0.87, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "pickaxe", "tool", 2336, 7, 1.2, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "axe", "tool", 2336, 11, 1, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "shovel", "tool", 2336, 7.5, 1, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "hoe", "tool", 2336, 1, 4, 0, 0, 0, 0);
		GearBalanceApi.registerItem(prefix + "helmet", "armor", 473, 0, 0, 0, 4, 3.5, 0.1);
		GearBalanceApi.registerItem(prefix + "chestplate", "armor", 688, 0, 0, 0, 9, 3.5, 0.1);
		GearBalanceApi.registerItem(prefix + "leggings", "armor", 645, 0, 0, 0, 7, 3.5, 0.1);
		GearBalanceApi.registerItem(prefix + "boots", "armor", 559, 0, 0, 0, 3, 3.5, 0.1);
	}

	/** Blocks and items; before the shields, hammers and the other tiers that use them. */
	public static void register() {
		ORE = block("deepslate_titanium_ore", BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
				.requiresCorrectToolForDrops().strength(30.0F, 1200.0F).sound(SoundType.DEEPSLATE));
		BLOCK = block("titanium_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
				.requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK));
		FRAGMENT = item("titanium_fragment", properties -> new Item(properties.fireResistant()));
		INGOT = item("titanium_ingot", properties -> new Item(properties.fireResistant()));
		TEMPLATE = item("titanium_upgrade_smithing_template", properties -> new SmithingTemplateItem(
				Component.translatable("item.gearbalance.smithing_template.titanium_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
				Component.translatable("item.gearbalance.smithing_template.titanium_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
				Component.translatable("item.gearbalance.smithing_template.titanium_upgrade.base_slot_description"),
				Component.translatable("item.gearbalance.smithing_template.titanium_upgrade.additions_slot_description"),
				List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"),
						slot("boots"), slot("hoe"), slot("shovel"), slot("spear")),
				List.of(slot("ingot")), properties.fireResistant()));
		SWORD = item("titanium_sword", properties -> new Item(properties.sword(TOOL, 3.0F, -2.4F).fireResistant()));
		SHOVEL = item("titanium_shovel", properties -> new Item(properties.shovel(TOOL, 1.5F, -3.0F).fireResistant()));
		PICKAXE = item("titanium_pickaxe", properties -> new Item(properties.pickaxe(TOOL, 1.0F, -2.8F).fireResistant()));
		AXE = item("titanium_axe", properties -> new Item(properties.axe(TOOL, 5.0F, -3.0F).fireResistant()));
		HOE = item("titanium_hoe", properties -> new Item(properties.hoe(TOOL, -4.0F, 0.0F).fireResistant()));
		// Same reach, timing and charge values as the netherite spear
		SPEAR = item("titanium_spear", properties -> new Item(properties.spear(TOOL, 1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F,
				5.1F, 8.75F, 4.6F).fireResistant()));
		HELMET = item("titanium_helmet", properties -> new Item(properties.humanoidArmor(ARMOR, ArmorType.HELMET).fireResistant()));
		CHESTPLATE = item("titanium_chestplate",
				properties -> new Item(properties.humanoidArmor(ARMOR, ArmorType.CHESTPLATE).fireResistant()));
		LEGGINGS = item("titanium_leggings", properties -> new Item(properties.humanoidArmor(ARMOR, ArmorType.LEGGINGS).fireResistant()));
		BOOTS = item("titanium_boots", properties -> new Item(properties.humanoidArmor(ARMOR, ArmorType.BOOTS).fireResistant()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.NETHERITE_INGOT, FRAGMENT, INGOT, TEMPLATE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output ->
				output.insertAfter(Items.ANCIENT_DEBRIS, ORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				output.insertAfter(Items.NETHERITE_BLOCK, BLOCK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertAfter(Items.NETHERITE_HOE, SHOVEL, PICKAXE, AXE, HOE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(Items.NETHERITE_SWORD, SWORD);
			output.insertAfter(Items.NETHERITE_BOOTS, HELMET, CHESTPLATE, LEGGINGS,
					BOOTS, SPEAR);
		});
	}

	/** Ore in the overworld and the template in the end city chests, both only while the switch is on. */
	public static void registerWorld() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(context -> GearConfig.titaniumActive()),
				GenerationStep.Decoration.UNDERGROUND_ORES, ORE_FEATURE);
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (GearConfig.titaniumActive() && table.unwrapKey().filter(BuiltInLootTables.END_CITY_TREASURE::equals).isPresent()
					&& context.getRandom().nextFloat() < TEMPLATE_CHANCE) {
				drops.add(new ItemStack(TEMPLATE));
			}
		});
	}

	/** Titanium armor pieces worn, 0 to 4. */
	public static int armorPieces(LivingEntity entity) {
		int pieces = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (isArmor(entity.getItemBySlot(slot))) {
				pieces++;
			}
		}
		return pieces;
	}

	public static boolean isArmor(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(HELMET) || stack.is(CHESTPLATE) || stack.is(LEGGINGS) || stack.is(BOOTS));
	}

	/** Fall damage after the titanium armor: each worn piece takes off its share. */
	public static int fallDamage(LivingEntity entity, int damage) {
		if (damage <= 0 || !GearConfig.titaniumActive()) {
			return damage;
		}
		int percent = Math.min(100, armorPieces(entity) * GearConfig.titaniumFallPercent);
		return percent <= 0 ? damage : (int) Math.round(damage * (100 - percent) / 100.0);
	}

	private static Identifier slot(String name) {
		return Identifier.withDefaultNamespace("container/slot/" + name);
	}

	private static Block block(String path, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, GearBalance.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, new Block(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, GearBalance.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().fireResistant()));
		return block;
	}

	private static Item item(String path, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GearBalance.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
