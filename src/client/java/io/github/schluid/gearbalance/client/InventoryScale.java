package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.jspecify.annotations.Nullable;

/**
 * Own GUI size while the inventory is open, and a settings GUI size with the same screen share on every resolution.
 * The game's GUI size setting is not changed; closing the inventory restores it.
 */
public final class InventoryScale {
	private InventoryScale() {
	}

	/** GUI size from the game settings, as last worked out by the game; 0 before that. */
	public static int normal;

	/** Share of the largest GUI size used by the settings screens: half, so they look alike on every resolution. */
	private static final double SETTINGS_SHARE = 1.0 / 2.0;

	/** GUI size to use while this screen is open. */
	public static int scaleFor(@Nullable Screen screen, int normalScale) {
		if (screen instanceof ConfigScreen || screen instanceof ProfilesScreen || screen instanceof HmProfilesScreen
				|| screen instanceof ImportScreen || screen instanceof TipScreen) {
			// Never larger than the window allows
			int largest = Minecraft.getInstance().getWindow().calculateScale(0, false);
			return Math.max(1, (int) Math.round(largest * SETTINGS_SHARE));
		}
		if (screen instanceof InventoryScreen && GearConfig.enabled && GearConfig.inventoryScale) {
			// Smaller or larger than the normal size, but never larger than the window allows
			int largest = Minecraft.getInstance().getWindow().calculateScale(0, false);
			return Math.max(1, Math.min(largest, GearConfig.inventoryGuiScale));
		}
		return normalScale;
	}
}
