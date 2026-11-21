package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Hotbar hiding: slides down after some seconds without a slot change.
 * "hotbar only": the bars above move into its place; "everything": the bars slide out too.
 * Damage brings everything back for a while; low food keeps it visible.
 */
public final class HotbarHide {
	private HotbarHide() {
	}

	/** Height of the hotbar; the bars above move down by this much in the mode "hotbar only". */
	private static final int HOTBAR = 22;
	/** Far enough to take the hotbar (and in the mode "everything" the bars) out of the screen. */
	private static final int HOTBAR_OUT = 24;
	private static final int ALL_OUT = 64;
	/** Part of the way moved per tick. */
	private static final float SPEED = 0.2F;

	/** Pixels everything at the bottom is drawn higher (TVs often cut off the edge). */
	private static final int HUD_LIFT = 1;

	/** Pixels the name of the held item is drawn higher than in vanilla. */
	private static final int ITEM_NAME_UP = 10;

	private static int selected = -1;
	private static long ticks;
	private static long lastChange;
	private static long damageUntil;
	/** 0 = shown, 1 = hidden; the one before for smooth moving between ticks. */
	private static float hidden, hiddenBefore;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(HotbarHide::tick);
		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, element -> moved(element, Part.HOTBAR));
		for (Identifier bar : new Identifier[] {VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
				VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH}) {
			HudElementRegistry.replaceElement(bar, element -> moved(element, Part.BAR));
		}
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, element -> moved(element, Part.ARMOR));
		HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, element -> moved(element, Part.EXPERIENCE));
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, element -> moved(element, Part.EXPERIENCE));
		HudElementRegistry.replaceElement(VanillaHudElements.HELD_ITEM_TOOLTIP, element -> moved(element, Part.ITEM_NAME));
		HudElementRegistry.replaceElement(VanillaHudElements.OVERLAY_MESSAGE, element -> moved(element, Part.TEXT));
		// Bars of other mods at the bottom move along; they are registered once every mod has started
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			if (FabricLoader.getInstance().isModLoaded(HARDMODEPLUS)) {
				for (String path : HARDMODEPLUS_BARS) {
					moveOther(Identifier.fromNamespaceAndPath(HARDMODEPLUS, path));
				}
			}
		});
	}

	/** HardmodePlus: stamina above the hotbar, thirst and saturation at the food bar, the max level text. */
	private static final String HARDMODEPLUS = "hardmodeplus";
	private static final String[] HARDMODEPLUS_BARS = {"stamina_bar", "thirst_bar", "food_saturation", "max_level"};

	private static void moveOther(Identifier id) {
		try {
			HudElementRegistry.replaceElement(id, element -> moved(element, Part.BAR));
		} catch (IllegalArgumentException e) {
			// Not there in this version of the other mod
		}
	}

	private enum Part {
		HOTBAR, BAR, ARMOR, EXPERIENCE, TEXT, ITEM_NAME
	}

	private static HudElement moved(HudElement element, Part part) {
		return (graphics, tick) -> {
			float amount = Mth.lerp(tick.getGameTimeDeltaPartialTick(false), hiddenBefore, hidden);
			int down = Math.round(amount * switch (part) {
				case HOTBAR -> HOTBAR_OUT;
				case BAR -> GearConfig.hotbarHideAll ? ALL_OUT : HOTBAR;
				// Armor and experience can go away on their own while the other bars only move down
				case ARMOR -> GearConfig.hotbarHideAll || GearConfig.hotbarHideArmor ? ALL_OUT : HOTBAR;
				case EXPERIENCE -> GearConfig.hotbarHideAll || GearConfig.hotbarHideXp ? ALL_OUT : HOTBAR;
				case TEXT, ITEM_NAME -> HOTBAR;
			});
			// The name of the held item sits one row higher, so it does not cover the armor bar
			if (part == Part.ITEM_NAME) {
				down -= ITEM_NAME_UP;
			}
			// Everything at the bottom a bit higher, so screens that cut off the edge show it whole
			down -= HUD_LIFT;
			if (down == 0) {
				element.extractRenderState(graphics, tick);
				return;
			}
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, down);
			element.extractRenderState(graphics, tick);
			graphics.pose().popMatrix();
		};
	}

	private static void tick(Minecraft minecraft) {
		ticks++;
		hiddenBefore = hidden;
		LocalPlayer player = minecraft.player;
		boolean hide = false;
		if (player != null && GearConfig.enabled && GearConfig.hotbarHide) {
			int slot = player.getInventory().getSelectedSlot();
			if (slot != selected) {
				selected = slot;
				lastChange = ticks;
			}
			if (player.hurtTime > 0) {
				damageUntil = ticks + GearConfig.hotbarDamageSeconds * 20L;
			}
			boolean hungry = GearConfig.hotbarFoodThreshold > 0
					&& player.getFoodData().getFoodLevel() <= GearConfig.hotbarFoodThreshold;
			hide = ticks - lastChange >= GearConfig.hotbarHideSeconds * 20L && ticks >= damageUntil && !hungry;
		} else {
			lastChange = ticks;
		}
		hidden = hide ? Math.min(1, hidden + SPEED) : Math.max(0, hidden - SPEED);
	}

	/** How far the hotbar is hidden right now, 0 = shown, 1 = hidden. */
	public static float amount() {
		return hidden;
	}
}
