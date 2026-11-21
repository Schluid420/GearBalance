package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Picked up items: tiles beside the hotbar (item with "+2", same item counts up) and a sparkle in the inventory. */
public final class PickupDisplay {
	private PickupDisplay() {
	}

	/** One tile per item, as big as a hotbar slot. */
	private static final int TILE = 22;
	private static final int GAP = 6;
	private static final int MARGIN = 4;
	private static final long FADE_MS = 500;

	private static final class Entry {
		final ItemStack item;
		int count;
		long time;

		Entry(ItemStack item, int count, long time) {
			this.item = item;
			this.count = count;
			this.time = time;
		}
	}

	/** Lines of the list, oldest first. */
	private static final List<Entry> LINES = new ArrayList<>();
	/** Items that came in and were not looked at yet. */
	private static final List<ItemStack> NEW = new ArrayList<>();

	/** Set while a slot of the player's inventory with a new item is drawn, for the sparkle in its decorations. */
	public static boolean sparkleSlot;

	public static void register() {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "pickups"), PickupDisplay::render);
	}

	public static boolean displayActive() {
		return GearConfig.enabled && GearConfig.pickupDisplay;
	}

	public static boolean marksActive() {
		return GearConfig.enabled && GearConfig.pickupMarks;
	}

	/** The local player picked up an item. */
	public static void picked(ItemStack stack, int amount) {
		if (stack.isEmpty() || amount <= 0) {
			return;
		}
		long now = System.currentTimeMillis();
		if (displayActive()) {
			Entry found = null;
			for (Entry entry : LINES) {
				if (ItemStack.isSameItemSameComponents(entry.item, stack)) {
					found = entry;
					break;
				}
			}
			if (found != null && now - found.time < GearConfig.pickupSeconds * 1000L) {
				LINES.remove(found);
				found.count += amount;
				found.time = now;
				LINES.add(found);
			} else {
				if (found != null) {
					LINES.remove(found);
				}
				LINES.add(new Entry(stack.copyWithCount(1), amount, now));
			}
			while (LINES.size() > GearConfig.pickupRows) {
				LINES.remove(0);
			}
		}
		if (marksActive() && !isNew(stack)) {
			NEW.add(stack.copyWithCount(1));
		}
	}

	private static boolean isNew(ItemStack stack) {
		for (ItemStack item : NEW) {
			if (ItemStack.isSameItemSameComponents(item, stack)) {
				return true;
			}
		}
		return false;
	}

	/** Called for every drawn slot: decides the sparkle; a new item under the mouse counts as seen. */
	public static void slot(Slot slot, boolean hovered) {
		sparkleSlot = false;
		if (!marksActive() || NEW.isEmpty() || !(slot.container instanceof Inventory) || !slot.hasItem()) {
			return;
		}
		ItemStack stack = slot.getItem();
		if (!isNew(stack)) {
			return;
		}
		if (hovered) {
			NEW.removeIf(item -> ItemStack.isSameItemSameComponents(item, stack));
			return;
		}
		sparkleSlot = true;
	}

	/** Four-pointed sparkle in the top right corner of the item (x, y = top left of the item). */
	public static void drawSparkle(GuiGraphicsExtractor graphics, int x, int y) {
		int frame = (int) (System.currentTimeMillis() / 250 % 4);
		int left = x + 10;
		int top = y - 1;
		if (frame == 1) {
			// Small: just the center cross
			dot(graphics, left + 3, top + 3, 0xFFFFFFFF);
			dot(graphics, left + 2, top + 3, 0xE6FFF096);
			dot(graphics, left + 4, top + 3, 0xE6FFF096);
			dot(graphics, left + 3, top + 2, 0xE6FFF096);
			dot(graphics, left + 3, top + 4, 0xE6FFF096);
			return;
		}
		int alpha = frame == 3 ? 0xB0 : 0xFF;
		int white = (alpha << 24) | 0xFFFFFF;
		int light = (alpha << 24) | 0xFFF096;
		int gold = (alpha << 24) | 0xFFC83C;
		dot(graphics, left + 3, top, gold);
		dot(graphics, left + 3, top + 6, gold);
		dot(graphics, left, top + 3, gold);
		dot(graphics, left + 6, top + 3, gold);
		dot(graphics, left + 3, top + 1, light);
		dot(graphics, left + 3, top + 5, light);
		dot(graphics, left + 1, top + 3, light);
		dot(graphics, left + 5, top + 3, light);
		dot(graphics, left + 2, top + 2, light);
		dot(graphics, left + 4, top + 2, light);
		dot(graphics, left + 2, top + 4, light);
		dot(graphics, left + 4, top + 4, light);
		dot(graphics, left + 3, top + 2, white);
		dot(graphics, left + 2, top + 3, white);
		dot(graphics, left + 3, top + 3, white);
		dot(graphics, left + 4, top + 3, white);
		dot(graphics, left + 3, top + 4, white);
	}

	private static void dot(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 1, y + 1, color);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker tick) {
		if (LINES.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		long shown = GearConfig.pickupSeconds * 1000L;
		Iterator<Entry> old = LINES.iterator();
		while (old.hasNext()) {
			if (now - old.next().time > shown) {
				old.remove();
			}
		}
		if (!displayActive() || LINES.isEmpty()) {
			return;
		}
		Font font = minecraft.font;
		float scale = GearConfig.pickupScale / 100.0F;
		boolean right = GearConfig.pickupRight;
		// The offhand slot sits on the side of the weaker hand while it holds something: one slot further out there
		boolean offhandLeft = minecraft.player != null && minecraft.player.getMainArm() == HumanoidArm.RIGHT;
		boolean offhandShown = minecraft.player != null && !minecraft.player.getOffhandItem().isEmpty();
		int skip = offhandShown && offhandLeft != right ? 29 : 0;
		// Edge of the first tile next to the hotbar
		int edge = right ? graphics.guiWidth() / 2 + 91 + GAP + skip : graphics.guiWidth() / 2 - 91 - GAP - skip;
		int top = graphics.guiHeight() - TILE;
		graphics.pose().pushMatrix();
		// Scaled around the hotbar side of the first tile, so the row stays next to the hotbar
		graphics.pose().translate(edge, top + TILE);
		graphics.pose().scale(scale, scale);
		int room = (int) ((right ? graphics.guiWidth() - MARGIN - edge : edge - MARGIN) / scale);
		int used = 0;
		// Newest tile next to the hotbar, older ones further out
		for (int i = LINES.size() - 1; i >= 0 && used + TILE <= room; i--) {
			Entry entry = LINES.get(i);
			long rest = shown - (now - entry.time);
			int alpha = rest >= FADE_MS ? 255 : (int) Math.max(0, 255 * rest / FADE_MS);
			if (alpha < 8) {
				continue;
			}
			int x = right ? used : -used - TILE;
			graphics.fill(x, -TILE, x + TILE, 0, (alpha * 140 / 255) << 24);
			if (alpha > 128) {
				graphics.item(entry.item, x + 3, -TILE + 3);
			}
			String count = "+" + entry.count;
			graphics.text(font, count, x + TILE - 1 - font.width(count), -TILE + 13, (alpha << 24) | 0xFFFFFF, true);
			used += TILE + GAP / 3;
		}
		graphics.pose().popMatrix();
	}
}
