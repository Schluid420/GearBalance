package io.github.schluid.gearbalance.socket;

import io.github.schluid.gearbalance.config.GearConfig;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** What a gem adds: the bonuses of its sort (GemSorts) with the value per cut from GearConfig; aquamarine and onyx pairs get their percent share. */
public final class GemValues {
	private GemValues() {
	}

	private static final DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

	/** One bonus of a gem sort; share = percent of the set value (100 for single bonuses). */
	public record Bonus(GemBonuses.Bonus kind, int share) {
		/** Value for cut level 1 chipped, 2 cut, 3 flawless; 0 for anything else. */
		public double amount(int cutLevel) {
			return Math.round(GearConfig.gemValue(kind.id(), cutLevel) * share) / 100.0;
		}
	}

	/** The bonuses of a sort (one, two for aquamarine and onyx). */
	public static List<Bonus> of(GemSorts.Sort sort) {
		List<Bonus> result = new ArrayList<>();
		if (sort == null) {
			return result;
		}
		int share = sort.pair() ? GearConfig.BONUS_SHARE.getOrDefault(sort.type(), 100) : 100;
		for (String id : sort.bonuses()) {
			GemBonuses.Bonus kind = GemBonuses.get(id);
			if (kind != null) {
				result.add(new Bonus(kind, share));
			}
		}
		return result;
	}

	/** The bonuses of a stored gem id. */
	public static List<Bonus> of(String gemId) {
		return of(Gems.sortOf(gemId));
	}

	/** The bonuses a stored gem gives on this kind of gear: none if it belongs on the other kind. */
	public static List<Bonus> on(String gemId, boolean armor) {
		GemSorts.Sort sort = Gems.sortOf(gemId);
		return sort == null || sort.armor() != armor ? List.of() : of(sort);
	}

	/** Name of a bonus ("gearbalance.bonus.<id>"), short and the same in every place. */
	public static Component name(GemBonuses.Bonus kind) {
		return Component.translatable("gearbalance.bonus." + kind.id());
	}

	/** What a bonus does, for the settings screen. */
	public static Component description(GemBonuses.Bonus kind) {
		return Component.translatable("gearbalance.bonus." + kind.id() + ".desc");
	}

	/** "+1.5 Max Health", "+6% Speed", "-30% Burning Time" for a value as typed in the settings. */
	public static MutableComponent line(GemBonuses.Bonus kind, double value) {
		String number = FORMAT.format(value) + (kind.percent() ? "%" : "");
		return Component.literal((kind.negative() ? "-" : "+") + number + " ").append(name(kind));
	}
}
