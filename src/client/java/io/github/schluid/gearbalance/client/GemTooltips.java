package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.socket.GemBonuses;
import io.github.schluid.gearbalance.socket.GemSorts;
import io.github.schluid.gearbalance.socket.GemValues;
import io.github.schluid.gearbalance.socket.Gems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Gem tooltips: hints for uncut gems (can be switched off) and what a cut gem gives,
 * with an icon for where it goes (weapons/tools or armor/shields).
 */
final class GemTooltips {
	private GemTooltips() {
	}


	static void add(ItemStack stack, List<Component> lines) {
		if (stack.isEmpty()) {
			return;
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		if (!id.startsWith(GearBalance.MOD_ID + ":")) {
			return;
		}
		String path = id.substring(GearBalance.MOD_ID.length() + 1);
		boolean hints = GearConfig.showHints;
		if (Gems.isRough(stack)) {
			if (hints) {
				lines.add(gray("gearbalance.gem.rough.tip"));
			}
			return;
		}
		String type = Gems.type(stack);
		int cut = Gems.cut(stack);
		if (type == null || cut == 0) {
			return;
		}
		GemSorts.Sort sort = Gems.sort(stack);
		if (hints && cut < 3) {
			Item next = Gems.of(sort, cut + 1);
			if (next != null) {
				lines.add(Component.translatable("gearbalance.gem.combine.tip", new ItemStack(next).getHoverName())
						.withStyle(ChatFormatting.GRAY));
			}
		}
		if (hints && GearConfig.gemRecutActive()) {
			lines.add(gray("gearbalance.gem.recut.tip"));
		}
		if (hints) {
			lines.add(gray("gearbalance.gem.set.tip"));
			if (GemBonuses.TWO_BONUS_TYPES.contains(type)) {
				lines.add(gray("gearbalance.gem.rare_once.tip." + type));
			}
		}
		// What the gem gives: an icon instead of a heading, straight under the name and hints
		bonuses(lines, sort != null && sort.armor() ? ARMOR_ICON : TOOLS_ICON, GemValues.of(sort), cut);
	}

	/** Socket font symbols: iron sword and pickaxe (weapons, tools), iron chestplate and shield (armor, shields). */
	private static final String TOOLS_ICON = "\uE030\uE031";
	private static final String ARMOR_ICON = "\uE032\uE034";

	/** Spacer of 1 pixel from the socket font, to line up the bonuses exactly. */
	private static final char SPACER = '\uE033';

	/** One line per bonus like the vanilla attribute lines, all texts aligned after the icon. */
	private static void bonuses(List<Component> lines, String icon, List<GemValues.Bonus> bonuses, int cut) {
		// The creative search builds its index on another thread, where the font can't be measured:
		// there only the bonus texts are needed
		if (!Minecraft.getInstance().isSameThread()) {
			for (GemValues.Bonus bonus : bonuses) {
				lines.add(GemValues.line(bonus.kind(), bonus.amount(cut)).withStyle(ChatFormatting.BLUE));
			}
			return;
		}
		Font font = Minecraft.getInstance().font;
		int start = Math.max(iconWidth(font, TOOLS_ICON), iconWidth(font, ARMOR_ICON)) + font.width(" ");
		for (int i = 0; i < bonuses.size(); i++) {
			GemValues.Bonus bonus = bonuses.get(i);
			String shown = i == 0 ? icon : "";
			StringBuilder text = new StringBuilder(shown);
			int width = iconWidth(font, shown);
			while (width < start) {
				text.append(SPACER);
				width = iconWidth(font, text.toString());
			}
			MutableComponent line = Component.empty();
			line.append(Component.literal(text.toString())
					.withStyle(style -> style.withFont(GearBalanceClient.SOCKET_FONT).withColor(ChatFormatting.WHITE)
							.withShadowColor(GearBalanceClient.ICON_SHADOW)));
			line.append(GemValues.line(bonus.kind(), bonus.amount(cut)).withStyle(ChatFormatting.BLUE));
			lines.add(line);
		}
	}

	/** Width of text in the socket font. */
	private static int iconWidth(Font font, String text) {
		return text.isEmpty() ? 0 : font.width(Component.literal(text).withStyle(style -> style.withFont(GearBalanceClient.SOCKET_FONT)));
	}

	private static Component gray(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.GRAY);
	}
}
