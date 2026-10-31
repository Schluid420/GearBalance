package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.bonus.SetBonus;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.level.HighestLevel;
import io.github.schluid.gearbalance.level.LevelRules;
import io.github.schluid.gearbalance.stats.Material;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GearBalanceClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WelcomeToast.register();
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			addSetBonus(stack, lines);

			int required = LevelRules.required(stack);
			if (required <= 0) {
				return;
			}
			var player = Minecraft.getInstance().player;
			boolean unlocked = player == null || LevelRules.exempt(player) || HighestLevel.get(player) >= required;
			lines.add(Component.translatable("gearbalance.level.tooltip", required)
					.withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.RED));
		});
	}

	/**
	 * Set bonus lines: the set with the worn pieces, then the bonus for 2 and for 4 pieces,
	 * green when active and grey when not.
	 */
	private static void addSetBonus(ItemStack stack, List<Component> lines) {
		if (stack.isEmpty() || !stack.has(DataComponents.EQUIPPABLE)) {
			return;
		}
		Material material = Material.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (material == null || !material.hasArmor() || GearConfig.activeSetStrength(material) <= 0) {
			return;
		}
		var player = Minecraft.getInstance().player;
		int worn = player == null ? 0 : SetBonus.wornPieces(player).getOrDefault(material, 0);
		BonusType type = GearConfig.getSetType(material);

		lines.add(Component.empty());
		lines.add(Component.translatable("gearbalance.set.title",
				Component.translatable("gearbalance.material." + material.key), worn).withStyle(ChatFormatting.YELLOW));
		for (int pieces : new int[] {2, 4}) {
			double amount = SetBonus.amount(material, pieces);
			// "+1 Herz" instead of "+1 Herzen" where the language has a singular form
			String key = "gearbalance.bonus." + type.key + ".effect";
			if (amount == 1 && Language.getInstance().has(key + ".one")) {
				key += ".one";
			}
			Component effect = Component.translatable(key, ConfigScreen.format(amount));
			lines.add(Component.translatable("gearbalance.set.line", pieces, effect)
					.withStyle(worn >= pieces ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
	}
}
