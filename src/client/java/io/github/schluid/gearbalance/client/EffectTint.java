package io.github.schluid.gearbalance.client;

import com.mojang.serialization.MapCodec;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Item tints for the hand effects. Textures are grey and get the effect color; alpha carries the menu opacity.
 * <ul>
 * <li>"gearbalance:effect_color": color and brightness (flames, shadow).</li>
 * <li>"gearbalance:glow_color": same, plus the pulse of the glow (smoother than texture frames).</li>
 * <li>"gearbalance:ring_color": the 1 px outline ring with its side faces, own opacity and saturation.</li>
 * </ul>
 * The shield renderer uses the same colors through {@link #glowColor()} and {@link #ringColor()}.
 */
public record EffectTint(Kind kind) implements ItemTintSource {
	public enum Kind { COLOR, GLOW, RING }

	public static final MapCodec<EffectTint> COLOR_CODEC = MapCodec.unit(new EffectTint(Kind.COLOR));
	public static final MapCodec<EffectTint> GLOW_CODEC = MapCodec.unit(new EffectTint(Kind.GLOW));
	public static final MapCodec<EffectTint> RING_CODEC = MapCodec.unit(new EffectTint(Kind.RING));

	/** Before the first resource load, so the item definitions can name the tints. */
	public static void register() {
		ItemTintSources.ID_MAPPER.put(id("effect_color"), COLOR_CODEC);
		ItemTintSources.ID_MAPPER.put(id("glow_color"), GLOW_CODEC);
		ItemTintSources.ID_MAPPER.put(id("ring_color"), RING_CODEC);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, path);
	}

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
		return switch (kind) {
			case GLOW -> glowColor();
			case RING -> ringColor();
			default -> argb(GearConfig.effectBrightness / 100.0F, GearConfig.effectRed, GearConfig.effectGreen,
					GearConfig.effectBlue);
		};
	}

	/** Effect color with the brightness, dimmed by the pulse when it is on. */
	public static int glowColor() {
		return argb(GearConfig.effectBrightness / 100.0F * pulse(), GearConfig.effectRed, GearConfig.effectGreen,
				GearConfig.effectBlue);
	}

	/** Effect color with the brightness, without the pulse (shields). */
	public static int glowColorSteady() {
		return argb(GearConfig.effectBrightness / 100.0F, GearConfig.effectRed, GearConfig.effectGreen,
				GearConfig.effectBlue);
	}

	/** 1 at the top of the pulse, down to 1 - strength at the bottom; always 1 with the pulse off. */
	public static float pulse() {
		if (!GearConfig.glowPulse) {
			return 1.0F;
		}
		double period = GearConfig.pulseSeconds * 1000.0;
		double wave = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / period * 2.0 * Math.PI);
		float strength = GearConfig.pulseStrength / 100.0F;
		return (float) (1.0 - strength + strength * wave);
	}

	/** Effect color blended towards grey by the saturation setting. Always opaque (a see-through ring turned black). */
	public static int ringColor() {
		int r = GearConfig.effectRed & 0xFF;
		int g = GearConfig.effectGreen & 0xFF;
		int b = GearConfig.effectBlue & 0xFF;
		float grey = 0.299F * r + 0.587F * g + 0.114F * b;
		float keep = GearConfig.ringSaturation / 100.0F;
		return argb(1.0F, mix(grey, r, keep), mix(grey, g, keep), mix(grey, b, keep));
	}

	private static int mix(float grey, int channel, float keep) {
		return Math.round(grey + (channel - grey) * keep);
	}

	private static int argb(float alpha, int r, int g, int b) {
		int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
		return a << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
	}

	@Override
	public MapCodec<EffectTint> type() {
		return switch (kind) {
			case GLOW -> GLOW_CODEC;
			case RING -> RING_CODEC;
			default -> COLOR_CODEC;
		};
	}
}
