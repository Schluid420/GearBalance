package io.github.schluid.gearbalance.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.plan.MasterPlan;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/** Load condition "gearbalance:option" for switch-bound recipes; also switches the gearbalance_rabbit_armor overlay (pack.mcmeta). Read on world load and /reload. */
public record OptionCondition(String option) implements ResourceCondition {
	public static final MapCodec<OptionCondition> CODEC =
			Codec.STRING.fieldOf("option").xmap(OptionCondition::new, OptionCondition::option);

	public static final ResourceConditionType<OptionCondition> TYPE =
			ResourceConditionType.create(GearBalance.id("option"), CODEC);

	public static void register() {
		ResourceConditions.register(TYPE);
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return switch (option) {
			case "trident_recipe" -> GearConfig.tridentRecipe;
			case "chainmail_recipes" -> GearConfig.chainmailRecipes;
			case "rabbit_hide_recipe" -> GearConfig.rabbitHideRecipe;
			case "rabbit_hide_armor" -> GearConfig.rabbitHideArmor;
			case "horse_armor_recipes" -> GearConfig.horseArmorRecipes;
			case "padded_armor" -> GearConfig.paddedArmorActive();
			case "upgrades" -> GearConfig.upgradesActive();
			case "sockets" -> GearConfig.socketsActive();
			case "gem_recut" -> GearConfig.gemRecutActive();
			// Diamond tools: normal recipe without the master plan switch, the recipe with the plan with it
			case "upgrades_without_plan" -> GearConfig.upgradesActive() && !GearConfig.masterPlanActive();
			case "upgrades_with_plan" -> GearConfig.upgradesActive() && GearConfig.masterPlanActive();
			case "sockets_without_plan" -> GearConfig.socketsActive() && !GearConfig.masterPlanActive();
			case "sockets_with_plan" -> GearConfig.socketsActive() && GearConfig.masterPlanActive();
			case "master_plan" -> GearConfig.masterPlanActive();
			case "shields" -> GearConfig.shieldsActive();
			case "repair_kit" -> GearConfig.repairKitActive();
			case "gem_lever" -> GearConfig.gemLeverActive();
			case "plan_needed" -> MasterPlan.active();
			case "gem_lever_with_plan" -> GearConfig.gemLeverActive() && MasterPlan.active();
			case "gem_lever_without_plan" -> GearConfig.gemLeverActive() && !MasterPlan.active();
			case "repair_kit_with_plan" -> GearConfig.repairKitActive() && MasterPlan.active();
			case "repair_kit_without_plan" -> GearConfig.repairKitActive() && !MasterPlan.active();
			case "bags" -> GearConfig.bagsActive();
			// Compressed materials belong to the upgrades only; the sewing needle is shared
			case "compressed" -> GearConfig.upgradesActive() || GearConfig.breakProtectionActive();
			case "break_protection" -> GearConfig.breakProtectionActive();
			case "titanium" -> GearConfig.titaniumActive();
			case "needle" -> GearConfig.paddedArmorActive() || GearConfig.bagsActive()
					|| GearConfig.leatherPatchActive() || GearConfig.horsePaddingActive();
			case "thread" -> GearConfig.leatherPatchActive() || GearConfig.horsePaddingActive();
			default -> false;
		};
	}
}
