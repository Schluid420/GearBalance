package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.config.GearConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** Note on the very first start: the mod is free, plus the official download. Waits for the title screen. */
public final class WelcomeToast {
	private WelcomeToast() {
	}

	private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId(10000L);

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (GearConfig.welcomeShown || minecraft.gui.overlay() != null
					|| !(minecraft.gui.screen() instanceof TitleScreen)) {
				return;
			}
			SystemToast.add(minecraft.gui.toastManager(), ID,
					Component.translatable("gearbalance.welcome.title"),
					Component.translatable("gearbalance.welcome.message"));
			GearConfig.welcomeShown = true;
			GearConfig.save();
		});
	}
}
