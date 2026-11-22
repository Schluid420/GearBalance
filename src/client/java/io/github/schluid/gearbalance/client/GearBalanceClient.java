package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.bag.BagSlots;
import io.github.schluid.gearbalance.bag.BagTooltip;
import io.github.schluid.gearbalance.bonus.BonusType;
import io.github.schluid.gearbalance.bonus.SetBonus;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.cooldown.Cooldowns;
import io.github.schluid.gearbalance.level.HighestLevel;
import io.github.schluid.gearbalance.level.LevelRules;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import io.github.schluid.gearbalance.padded.Tailoring;
import io.github.schluid.gearbalance.plan.MasterPlan;
import io.github.schluid.gearbalance.socket.GemValues;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Titanite;
import io.github.schluid.gearbalance.titanium.Titanium;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.stats.Material;
import io.github.schluid.gearbalance.tool.ToolDurability;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GearBalanceClient implements ClientModInitializer {
	/** Font with the socket symbols (assets/gearbalance/font/sockets.json). */
	static final FontDescription SOCKET_FONT =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "sockets"));
	/** Symbols in the socket font: empty socket and the snowflake; the gems have their own symbols below. */
	private static final char EMPTY_SOCKET = '\uE000';
	private static final char SNOWFLAKE = '\uE005';
	/** First gem symbol in font/gem_sockets.png: ruby (3 cuts), then sapphire, topaz, emerald, aquamarine, onyx. */
	private static final char GEM_SYMBOLS = '\uE010';
	/** Titanite symbols (chipped, cut, flawless), in the color of titanium. */
	private static final char TITANITE_SYMBOLS = '\uE022';
	private static final int TITANIUM_BLUE = 0x96AAD2;
	/** Shadow of the symbols: faint, the full text shadow doubles the small icons. */
	static final int ICON_SHADOW = 0x603F3F3F;

	@Override
	public void onInitializeClient() {
		FullSocketsProperty.register();
		HandEffectProperty.register();
		EffectTint.register();
		BagSlots.closed = () -> BagPanel.closed;
		WelcomeToast.register();
		ServerSettingsClient.register();
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (GearConfig.enabled && Titanite.ITEM != null && stack.is(Titanite.ITEM)) {
				if (GearConfig.showHints) {
					addWrapped(lines, Component.translatable("gearbalance.titanite.rough.tip"));
				}
			} else if (GearConfig.enabled && Titanite.is(stack)) {
				lines.add(Component.translatable("gearbalance.titanite.effect",
						GearConfig.titaniteBoost(Titanite.cutOf(stack))).withColor(TITANIUM_BLUE));
				if (GearConfig.showHints) {
					addWrapped(lines, Component.translatable("gearbalance.titanite.tip"));
				}
			}
			if (GearConfig.enabled && GearConfig.showHints && Titanium.TEMPLATE != null && stack.is(Titanium.TEMPLATE)) {
				addWrapped(lines, Component.translatable("gearbalance.titanium.template.tip"));
			}
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
		GrindstoneButtons.register();
		StatsPanel.register();
		ShieldRenderer.register();
		DurabilityWarning.register();
		ArmorBar.register();
		HotbarFx.register();
		ComfortClient.register();
		PickupDisplay.register();
		// The creative inventory can't place the bag slots
		BagSlots.hidden = () -> Minecraft.getInstance().gui.screen() instanceof CreativeModeInventoryScreen;
		// Bag tooltip: how full it is, as a bar like the bundle's
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof BagTooltip bag ? new ClientBagTooltip(bag) : null);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (lines.isEmpty()) {
				return;
			}
			int at = 1;
			// Padded armor: a line right under the name, with a snowflake
			if (PaddedArmor.isActive(stack)) {
				MutableComponent padded = Component.translatable("gearbalance.padded.tooltip").withStyle(ChatFormatting.AQUA);
				lines.add(at++, symbolLine(SNOWFLAKE, padded));
			}
			if (Tailoring.horsePaddingActive(stack)) {
				lines.add(at++, Component.translatable("gearbalance.horse_padding.tooltip").withStyle(ChatFormatting.AQUA));
			}
			// Wait time against upgrade spam
			Level level = Minecraft.getInstance().level;
			if (GearConfig.upgradesActive() && Upgrades.storedLevel(stack) < Upgrades.MAX_LEVEL) {
				long hours = Cooldowns.remainingHours(stack, level, true);
				if (hours > 0) {
					lines.add(Math.min(at++, lines.size()),
							Component.translatable("gearbalance.cooldown.upgrade", hours).withStyle(ChatFormatting.GOLD));
				}
			}
			// Only while the piece can still get another socket
			if (Sockets.active() && Sockets.count(stack) < Sockets.max(stack)) {
				long hours = Cooldowns.remainingHours(stack, level, false);
				if (hours > 0) {
					lines.add(Math.min(at++, lines.size()),
							Component.translatable("gearbalance.cooldown.socket", hours).withStyle(ChatFormatting.GOLD));
				}
			}
			// Sockets: one line per socket, under the name (and the padded line)
			if (Sockets.active()) {
				boolean armor = stack.getItem().components().has(DataComponents.EQUIPPABLE);
				for (String gem : Sockets.sockets(stack)) {
					lines.add(Math.min(at++, lines.size()), socketLine(gem, armor));
				}
				// The gem bonuses are counted into the attribute lines, with where each part comes from
				AttributeBreakdown.apply(stack, lines);
			}
			// Uses left (vanilla shows it only in advanced tooltips and only once damaged): the smithing tools
			// with one switch, every other damageable item with another
			boolean modTool = ToolDurability.max(stack) > 0;
			boolean show = modTool ? GearConfig.toolDurability : GearConfig.durabilityAll && stack.isDamageableItem();
			if (GearConfig.enabled && show && !(flag.isAdvanced() && stack.isDamaged())) {
				lines.add(Component.translatable("gearbalance.tool.durability", stack.getMaxDamage() - stack.getDamageValue(),
						stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
			}
			ShieldTooltip.add(stack, lines);
			UpgradeTooltip.add(stack, lines);
			GemTooltips.add(stack, lines);
			MaterialHints.add(stack, lines);
			CompareTooltip.add(stack, lines);
			if (GearConfig.showHints && MasterPlan.PLAN != null && stack.is(MasterPlan.PLAN)) {
				lines.add(Component.translatable("gearbalance.master_plan.tip").withStyle(ChatFormatting.GRAY));
			}
			// Vanilla puts an empty line before the attributes; right under the name it only looks like a gap
			if (lines.size() > 2 && lines.get(1).getString().isEmpty()) {
				lines.remove(1);
			}
			// Framed tooltips: lines with one leading space (base and shield values) also start at the left edge
			if (TooltipFrames.active()) {
				for (int i = 1; i < lines.size(); i++) {
					lines.set(i, enchantmentPurple(withoutLeadingSpace(lines.get(i))));
				}
			}
		});
		// Last: sees every line the parts above added
		ComfortClient.registerTooltip();
	}

	/** Set bonus lines: set with worn pieces, then the 2- and 4-piece bonus (green active, grey not). */
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
				Component.translatable(material.nameKey), worn).withStyle(ChatFormatting.YELLOW));
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

	/**
	 * "(symbol) Empty Socket" or "(symbol) Flawless Ruby": socket font symbol, gem name in gem color.
	 * With Shift its bonuses follow ("+1.5 Attack Damage"); without, they count into the attribute lines.
	 */
	private static Component socketLine(String gem, boolean armor) {
		int titanite = Titanite.cutOf(gem);
		if (titanite > 0) {
			// Titanite: no bonus of its own, the other gems of the piece are stronger
			return symbolLine((char) (TITANITE_SYMBOLS + Math.min(2, titanite - 1)),
					Component.translatable(Titanite.byId(gem).getDescriptionId()).withColor(TITANIUM_BLUE)
							.append(Component.translatable("gearbalance.titanite.socket", GearConfig.titaniteBoost(titanite))));
		}
		Item item = Gems.byId(gem);
		String type = Gems.typeOf(gem);
		if (item == null || type == null) {
			return symbolLine(EMPTY_SOCKET, Component.translatable("gearbalance.socket.empty").withStyle(ChatFormatting.DARK_GRAY));
		}
		MutableComponent text = new ItemStack(item).getHoverName().copy();
		Minecraft minecraft = Minecraft.getInstance();
		// The creative search builds its index on another thread, without keys: there no bonuses
		if (minecraft.isSameThread() && minecraft.hasShiftDown()) {
			for (GemValues.Bonus bonus : GemValues.on(gem, armor)) {
				text.append(" ").append(GemValues.line(bonus.kind(), bonus.amount(Gems.cutOf(gem))));
			}
		}
		return symbolLine(gemSymbol(type, Gems.cutOf(gem)), gemColored(type, text));
	}

	/** Socket font symbol per type and cut: bronze triangle (chipped), silver rhombus (cut), gold round (flawless). Unknown = empty socket. */
	static char gemSymbol(String type, int cut) {
		int typeIndex = type == null ? -1 : Gems.TYPES.indexOf(type);
		return typeIndex < 0 ? EMPTY_SOCKET : (char) (GEM_SYMBOLS + typeIndex * 3 + Math.max(0, Math.min(2, cut - 1)));
	}

	/** Text in the color of a gem type (onyx in its own grey). */
	static MutableComponent gemColored(String type, MutableComponent text) {
		return "onyx".equals(type) ? text.withColor(ONYX_GRAY) : text.withStyle(color(type));
	}

	/** Socket font symbol, a space and the text. Parts sit side by side under an empty root so the text keeps the normal font. */
	/** Width at which long grey tooltip texts wrap, like the other GearBalance tooltips. */
	/** Purple for enchantments in framed tooltips; curses keep their red. */
	private static final int ENCHANTMENT_PURPLE = 0xB070FF;

	private static Component enchantmentPurple(Component line) {
		if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith("enchantment.")
				&& !TextColor.fromLegacyFormat(ChatFormatting.RED).equals(line.getStyle().getColor())) {
			return line.copy().withColor(ENCHANTMENT_PURPLE);
		}
		return line;
	}

	/** " 7 Attack Damage" -> "7 Attack Damage": the space is its own literal in front of the rest. */
	private static Component withoutLeadingSpace(Component line) {
		if (!(line.getContents() instanceof PlainTextContents.LiteralContents literal) || !literal.text().equals(" ")
				|| line.getSiblings().isEmpty()) {
			return line;
		}
		MutableComponent result = Component.empty().withStyle(line.getStyle());
		for (Component part : line.getSiblings()) {
			result.append(part);
		}
		return result;
	}

	private static final int TOOLTIP_WIDTH = 200;

	/** Adds a long text as grey tooltip lines of at most TOOLTIP_WIDTH pixels. */
	private static void addWrapped(List<Component> lines, Component text) {
		Minecraft minecraft = Minecraft.getInstance();
		// The creative search builds its index on another thread, where the font can't be measured:
		// there the whole text as one line is enough
		if (!minecraft.isSameThread()) {
			lines.add(text.copy().withStyle(ChatFormatting.GRAY));
			return;
		}
		for (FormattedText part : minecraft.font.getSplitter().splitLines(text, TOOLTIP_WIDTH, Style.EMPTY)) {
			lines.add(Component.literal(part.getString()).withStyle(ChatFormatting.GRAY));
		}
	}

	static Component symbolLine(char symbol, Component text) {
		MutableComponent line = Component.empty();
		line.append(Component.literal(String.valueOf(symbol))
				.withStyle(style -> style.withFont(SOCKET_FONT).withColor(ChatFormatting.WHITE).withShadowColor(ICON_SHADOW)));
		line.append(Component.literal(" "));
		line.append(text);
		return line;
	}

	/** Tooltip color of a gem type. */
	static ChatFormatting color(String type) {
		return switch (type == null ? "" : type) {
			case "ruby" -> ChatFormatting.RED;
			case "sapphire" -> ChatFormatting.BLUE;
			case "topaz" -> ChatFormatting.GOLD;
			case "emerald" -> ChatFormatting.GREEN;
			case "aquamarine" -> ChatFormatting.AQUA;
			default -> ChatFormatting.GRAY;
		};
	}

	/** Onyx is shown in a darker gray than the default gray, other gems in their color. */
	private static final int ONYX_GRAY = 0x6E6E6E;
}
