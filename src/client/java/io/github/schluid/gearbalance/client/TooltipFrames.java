package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Tooltip frames: gear gets a material-colored frame with chains; everything else a plain white one.
 * Sprites: gearbalance:tooltip/(material)_frame and _background.
 */
public final class TooltipFrames {
	private TooltipFrames() {
	}

	/** Style id of the plain frame. */
	public static final String PLAIN = "plain";

	/** Item prefix -> material of the frame; the longest prefix wins, so "netherite_" never reads as anything else. */
	private static final Map<String, String> PREFIXES = Map.ofEntries(
			Map.entry("wooden_", "wood"), Map.entry("leather_", "leather"), Map.entry("stone_", "stone"),
			Map.entry("copper_", "copper"), Map.entry("chainmail_", "chainmail"), Map.entry("iron_", "iron"),
			Map.entry("golden_", "gold"), Map.entry("diamond_", "diamond"), Map.entry("netherite_", "netherite"),
			Map.entry("titanium_", "titanium"));

	/** Name color per material, a bright shade of the frame. */
	private static final Map<String, Integer> COLORS = Map.ofEntries(
			Map.entry("wood", 0xC39150), Map.entry("leather", 0xB57243), Map.entry("stone", 0xB4B4B4),
			Map.entry("copper", 0xF0A070), Map.entry("chainmail", 0xC3C5D2), Map.entry("iron", 0xF0F0F0),
			Map.entry("gold", 0xFFE066), Map.entry("diamond", 0x70FFF0), Map.entry("netherite", 0xBEAFB9),
			Map.entry("titanium", 0x96AAD2));

	/** The item whose tooltip is shown right now, for the picture in the head; null for other tooltips. */
	public static @Nullable ItemStack current;

	public static boolean active() {
		return GearConfig.enabled && GearConfig.tooltipFrames;
	}

	/** The style of this item's tooltip: its material, or the plain frame. */
	public static Identifier styleFor(ItemStack stack) {
		String material = material(stack);
		return Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, material == null ? PLAIN : material);
	}

	/** Material of a piece of gear (armor, weapons, tools, shields), null for everything else. */
	public static @Nullable String material(ItemStack stack) {
		boolean gear = stack.isDamageableItem() || stack.has(DataComponents.EQUIPPABLE);
		if (!gear) {
			return null;
		}
		// Gear without a material in its name
		if (stack.is(Items.SHIELD) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.FISHING_ROD)) {
			return "wood";
		}
		if (stack.is(Items.MACE)) {
			return "iron";
		}
		if (stack.is(Items.TRIDENT)) {
			return "diamond";
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (path.startsWith("padded_")) {
			path = path.substring("padded_".length());
		}
		String found = null;
		int length = 0;
		for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
			if (path.startsWith(entry.getKey()) && entry.getKey().length() > length) {
				found = entry.getValue();
				length = entry.getKey().length();
			}
		}
		return found;
	}

	/** True for the styles of this feature. */
	public static boolean isOwn(@Nullable Identifier style) {
		return style != null && style.getNamespace().equals(GearBalance.MOD_ID);
	}

	public static boolean isPlain(Identifier style) {
		return style.getPath().equals(PLAIN);
	}

	public static int color(Identifier style) {
		return COLORS.getOrDefault(style.getPath(), 0xFFFFFF);
	}
}
