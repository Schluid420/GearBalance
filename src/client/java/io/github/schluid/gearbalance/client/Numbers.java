package io.github.schluid.gearbalance.client;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/** Numbers in tooltips: up to two decimals, with a decimal comma in German. */
final class Numbers {
	private Numbers() {
	}

	static String format(double value) {
		String language = Minecraft.getInstance().getLanguageManager().getSelected();
		Locale locale = language != null && language.startsWith("de") ? Locale.GERMAN : Locale.ROOT;
		return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(locale)).format(value);
	}
}
