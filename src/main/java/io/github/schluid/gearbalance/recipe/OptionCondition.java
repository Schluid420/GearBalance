package io.github.schluid.gearbalance.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.Nullable;

/**
 * Load condition "gearbalance:option" for the optional recipes. Also used in pack.mcmeta to switch
 * the gearbalance_rabbit_armor overlay, which replaces the vanilla leather armor recipes.
 * Recipes are read on world load and on /reload.
 */
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
			default -> false;
		};
	}
}
