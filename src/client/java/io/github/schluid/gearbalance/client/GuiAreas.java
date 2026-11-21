package io.github.schluid.gearbalance.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

/** Areas outside the inventory window (values panel right, gear set buttons left); JEI keeps away from them. */
public final class GuiAreas {
	private GuiAreas() {
	}

	public static List<Rect2i> outside(Screen screen) {
		List<Rect2i> areas = new ArrayList<>();
		Rect2i stats = StatsPanel.area(screen);
		if (stats != null) {
			areas.add(stats);
		}
		Rect2i sets = LoadoutButtons.area(screen);
		if (sets != null) {
			areas.add(sets);
		}
		return areas;
	}
}
