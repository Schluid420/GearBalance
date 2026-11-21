package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.comfort.BreakProtection;
import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Colored armor bar: each point shows the material of the piece that gives it, a thin line where two pieces meet.
 * Optional damage display (cracks, darker, red blink when low, broken symbol).
 * Symbols are 9 x 9 like vanilla (hud/armor_std); "HD armor" draws 36 px sprites in the same place.
 */
public final class ArmorBar {
	private ArmorBar() {
	}

	private static final EquipmentSlot[] ORDER = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET};
	/** HD sprite size; the symbol is drawn at a quarter of it, the vanilla 9 x 9. */
	private static final int SIZE = 36;
	private static final int HALF = SIZE / 2;
	private static final float QUARTER = 0.25F;
	/** Normal sprites: 9 x 9, left half 5 columns (with the middle one), right half 4, as vanilla. */
	private static final int STD = 9;
	private static final int STD_LEFT = 5;
	/** How long a piece that just broke stays visible as broken, in ticks. */
	private static final int BROKEN_TICKS = 60;
	private static final int BLINK_TICKS = 8;
	private static final int RED = 0xFFFF5050;
	private static final int DIVIDER = 0xFF000000;
	/** The thin line between two pieces, in sprite pixels. */
	private static final int DIVIDER_TOP = 6;
	private static final int DIVIDER_BOTTOM = 33;

	/** One armor piece as the bar shows it. */
	private record Piece(String material, int dye, double left, boolean broken) {
		boolean sameLook(Piece other) {
			return other != null && material.equals(other.material) && dye == other.dye
					&& crackLevel(left) == crackLevel(other.left) && broken == other.broken;
		}
	}

	private static final Piece OTHER = new Piece("iron", -1, 1.0, false);

	/** Per slot: the piece of the last tick and how long ago it broke. */
	private static final Map<EquipmentSlot, Piece> LAST = new EnumMap<>(EquipmentSlot.class);
	private static final Map<EquipmentSlot, Long> BROKE_AT = new EnumMap<>(EquipmentSlot.class);
	private static final Map<EquipmentSlot, Piece> BROKE = new EnumMap<>(EquipmentSlot.class);
	private static long ticks;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ArmorBar::tick);
	}

	/** The own bar (switch "armor bar"); without colors the symbols stay gray. */
	public static boolean active() {
		return GearConfig.enabled && GearConfig.armorBar;
	}

	private static void tick(Minecraft minecraft) {
		ticks++;
		Player player = minecraft.player;
		if (player == null || !active()) {
			LAST.clear();
			BROKE.clear();
			BROKE_AT.clear();
			return;
		}
		for (EquipmentSlot slot : ORDER) {
			ItemStack stack = player.getItemBySlot(slot);
			Piece now = stack.isEmpty() ? null : piece(stack);
			Piece before = LAST.get(slot);
			// Gone while almost worn out: it broke
			if (now == null && before != null && before.left() <= 0.05) {
				BROKE.put(slot, new Piece(before.material(), before.dye(), 0.0, true));
				BROKE_AT.put(slot, ticks);
			}
			if (now != null) {
				BROKE.remove(slot);
			}
			LAST.put(slot, now);
		}
		BROKE_AT.entrySet().removeIf(entry -> {
			boolean over = ticks - entry.getValue() > BROKEN_TICKS;
			if (over) {
				BROKE.remove(entry.getKey());
			}
			return over;
		});
	}

	private static Piece piece(ItemStack stack) {
		double left = 1.0;
		if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
			left = (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage();
		}
		return new Piece(material(stack), dye(stack), left, BreakProtection.wornOut(stack));
	}

	/** Sprite name of the material, from the equipment look of the piece; unknown ones look like iron. */
	private static String material(ItemStack stack) {
		if (!GearConfig.armorColors) {
			return "iron";
		}
		if (PaddedArmor.isActive(stack)) {
			return "padded";
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return "iron";
		}
		Identifier id = equippable.assetId().get().identifier();
		return switch (id.getPath()) {
			case "leather" -> "leather";
			case "chainmail" -> "chain";
			case "copper" -> "copper";
			case "gold" -> "gold";
			case "diamond" -> "diamond";
			case "netherite" -> "netherite";
			case "turtle_scute" -> "turtle";
			case "titanium" -> GearBalance.MOD_ID.equals(id.getNamespace()) ? "titanium" : "iron";
			default -> "iron";
		};
	}

	private static int dye(ItemStack stack) {
		if (!GearConfig.armorColors) {
			return -1;
		}
		DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
		return dyed == null ? -1 : dyed.rgb();
	}

	/** Armor points of the piece itself (added armor on its own slot). */
	private static int points(ItemStack stack, EquipmentSlot slot) {
		double[] sum = {0};
		stack.forEachModifier(slot, (attribute, modifier) -> {
			if (attribute.value() == Attributes.ARMOR.value()
					&& modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				sum[0] += modifier.amount();
			}
		});
		return (int) Math.floor(sum[0]);
	}

	private static int crackLevel(double left) {
		if (!GearConfig.armorCracks) {
			return 0;
		}
		if (left <= GearConfig.armorCrack2Percent / 100.0) {
			return 2;
		}
		return left <= GearConfig.armorCrackPercent / 100.0 ? 1 : 0;
	}

	/** Draws the bar instead of vanilla. top/left as vanilla computes them. */
	public static void extract(GuiGraphicsExtractor graphics, Player player, int top, int left) {
		int total = Math.min(20, player.getArmorValue());
		List<Piece> points = new ArrayList<>();
		for (EquipmentSlot slot : ORDER) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			Piece piece = piece(stack);
			int count = points(stack, slot);
			for (int i = 0; i < count && points.size() < total; i++) {
				points.add(piece);
			}
		}
		// Armor from other sources (effects, other mods)
		while (points.size() < total) {
			points.add(OTHER);
		}
		// Broken pieces: one full symbol each, behind the armor
		List<Piece> broken = new ArrayList<>();
		if (GearConfig.armorBroken) {
			for (EquipmentSlot slot : ORDER) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty() && BreakProtection.wornOut(stack)) {
					broken.add(piece(stack));
				} else if (BROKE.containsKey(slot)) {
					broken.add(BROKE.get(slot));
				}
			}
		}
		if (points.isEmpty() && broken.isEmpty()) {
			return;
		}
		boolean hd = GearConfig.hdArmor;
		int size = hd ? SIZE : STD;
		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		if (hd) {
			graphics.pose().scale(QUARTER, QUARTER);
		}
		int filled = (points.size() + 1) / 2;
		for (int i = 0; i < 10; i++) {
			int x = i * 8 * (hd ? 4 : 1);
			if (i < filled) {
				Piece a = points.get(i * 2);
				Piece b = i * 2 + 1 < points.size() ? points.get(i * 2 + 1) : null;
				half(graphics, a, x, true);
				half(graphics, b, x, false);
				if (!a.sameLook(b)) {
					if (hd) {
						graphics.fill(x + HALF, DIVIDER_TOP, x + HALF + 1, DIVIDER_BOTTOM, DIVIDER);
					} else {
						// Middle column, rows 2 to 7, as in the vanilla half symbol
						graphics.fill(x + 4, 2, x + 5, 8, DIVIDER);
					}
				}
			} else if (i - filled < broken.size()) {
				brokenSymbol(graphics, x);
			} else {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("empty"), x, 0, size, size);
			}
		}
		graphics.pose().popMatrix();
	}

	/** One half of a symbol in the look of the piece; null = empty. */
	private static void half(GuiGraphicsExtractor graphics, Piece piece, int x, boolean leftHalf) {
		boolean hd = GearConfig.hdArmor;
		int size = hd ? SIZE : STD;
		int u = leftHalf ? 0 : hd ? HALF : STD_LEFT;
		int width = hd ? HALF : leftHalf ? STD_LEFT : STD - STD_LEFT;
		if (piece == null) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("empty"), size, size, u, 0, x + u, 0, width, size);
			return;
		}
		boolean dyed = "leather".equals(piece.material()) && piece.dye() >= 0;
		Identifier body = sprite(dyed ? "leather_dyed" : piece.material());
		int color = dyed ? 0xFF000000 | piece.dye() : 0xFFFFFFFF;
		if (GearConfig.armorDarken) {
			float dark = (float) ((1.0 - piece.left()) * GearConfig.armorDarkenPercent / 100.0);
			color = scale(color, 1.0F - dark);
		}
		boolean blink = GearConfig.armorBlink && piece.left() <= GearConfig.armorBlinkPercent / 100.0
				&& (ticks / BLINK_TICKS) % 2 == 0;
		if (blink) {
			color = multiply(color, RED);
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, body, size, size, u, 0, x + u, 0, width, size, color);
		int level = crackLevel(piece.left());
		if (level > 0) {
			String shade = lightCracks(piece.material()) ? "_light" : "_dark";
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("crack" + level + shade), size, size, u, 0, x + u, 0,
					width, size);
		}
	}

	private static void brokenSymbol(GuiGraphicsExtractor graphics, int x) {
		int size = GearConfig.hdArmor ? SIZE : STD;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("broken"), x, 0, size, size);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite("crack2_light"), x, 0, size, size);
	}

	/** Dark materials get light cracks, so they stay visible. */
	private static boolean lightCracks(String material) {
		return "netherite".equals(material) || "leather".equals(material);
	}

	private static Identifier sprite(String name) {
		return GearBalance.id((GearConfig.hdArmor ? "hud/armor/" : "hud/armor_std/") + name);
	}

	private static int scale(int argb, float factor) {
		int r = Math.round(((argb >> 16) & 0xFF) * factor);
		int g = Math.round(((argb >> 8) & 0xFF) * factor);
		int b = Math.round((argb & 0xFF) * factor);
		return (argb & 0xFF000000) | r << 16 | g << 8 | b;
	}

	private static int multiply(int a, int b) {
		int r = ((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) / 255;
		int g = ((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) / 255;
		int bl = (a & 0xFF) * (b & 0xFF) / 255;
		return (a & 0xFF000000) | r << 16 | g << 8 | bl;
	}
}
