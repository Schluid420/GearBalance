package io.github.schluid.gearbalance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.schluid.gearbalance.GearBalance;
import io.github.schluid.gearbalance.config.GearConfig;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.joml.Vector3fc;

/**
 * Draws a GearBalance shield: the vanilla shield with own textures "gearbalance:entity/shield/<material>_shield_base"
 * and "..._base_nopattern" (vanilla's shield atlas takes all "textures/entity/shield" files).
 * Used in assets/gearbalance/items/*_shield.json as {"type": "gearbalance:shield", "material": "iron"};
 * with "outline": true it also draws the full-sockets ring and a soft glow (colors and opacity from the glow settings).
 */
final class ShieldRenderer implements SpecialModelRenderer<DataComponentMap> {
	private final SpriteGetter sprites;
	private final ShieldModel model;
	private final SpriteId base;
	private final SpriteId baseNoPattern;
	/** Draws the pulsing outline around the plate (the item definition asks for it with full sockets in the hand). */
	private final boolean outline;

	/** Plain white texture, tinted by the vertex color. */
	private static final Identifier OUTLINE_TEXTURE = Identifier.fromNamespaceAndPath(GearBalance.MOD_ID,
			"textures/entity/shield_outline.png");
	/** White soft halo around the plate (transparent over the plate), tinted by the vertex color. */
	private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(GearBalance.MOD_ID,
			"textures/entity/shield_glow.png");
	/** Plate of the vanilla shield model in model units (pixels / 16): x -6..6, y -11..11, z -2..-1. */
	private static final float PX = 1.0F / 16.0F;
	private static final float X0 = -6 * PX, X1 = 6 * PX, Y0 = -11 * PX, Y1 = 11 * PX, Z0 = -2 * PX, Z1 = -1 * PX;
	/** Width of the outline: 0.3 pixel. */
	private static final float WIDTH = 0.3F * PX;
	/** How far the glow reaches past the plate: 4 pixels, the size the glow texture is made for. */
	private static final float GLOW = 4 * PX;
	private static final int FULL_BRIGHT = 0xF000F0;

	private ShieldRenderer(SpriteGetter sprites, ShieldModel model, String material, boolean outline) {
		this.sprites = sprites;
		this.model = model;
		this.base = sprite(material + "_shield_base");
		this.baseNoPattern = sprite(material + "_shield_base_nopattern");
		this.outline = outline;
	}

	private static SpriteId sprite(String name) {
		return new SpriteId(Sheets.SHIELD_SHEET, Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "entity/shield/" + name));
	}

	static void register() {
		SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(GearBalance.MOD_ID, "shield"), Json.MAP_CODEC);
	}

	@Override
	public DataComponentMap extractArgument(ItemStack stack) {
		return stack.immutableComponents();
	}

	/** Same as the vanilla shield renderer, only with the own textures. */
	@Override
	public void submit(DataComponentMap components, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
			boolean foil, int outlineColor) {
		BannerPatternLayers patterns = components != null
				? components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY) : BannerPatternLayers.EMPTY;
		DyeColor color = components != null ? components.get(DataComponents.BASE_COLOR) : null;
		boolean patterned = !patterns.layers().isEmpty() || color != null;
		SpriteId sprite = patterned ? base : baseNoPattern;
		if (foil && !patterned) {
			collector.submitModel(model, Unit.INSTANCE, poseStack, RenderTypes.entitySolidGlint(sprite.atlasLocation()), light,
					overlay, -1, sprites.get(sprite), outlineColor);
		} else {
			collector.submitModel(model, Unit.INSTANCE, poseStack, light, overlay, -1, sprite, sprites, outlineColor);
		}
		if (patterned) {
			BannerRenderer.submitPatterns(sprites, poseStack, collector, light, overlay, model, Unit.INSTANCE, false,
					Objects.requireNonNullElse(color, DyeColor.WHITE), patterns);
			if (foil) {
				collector.order(patterns.layers().size() + 2).submitModel(model, Unit.INSTANCE, poseStack,
						RenderTypes.patternedShieldGlint(), light, overlay, -1, sprites.get(sprite), 0);
			}
		}
		if (outline) {
			// The frame is lit fully, so it is darkened to sit with the outline of held items
			int ring = scaleRgb(EffectTint.ringColor(), GearConfig.shieldFrame / 100.0F);
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(OUTLINE_TEXTURE),
					(pose, consumer) -> drawOutline(pose, consumer, ring));
			// No pulse on shields; the frame is opaque (cutout), so the glow in front cannot hide parts of it
			int glow = scaleAlpha(EffectTint.glowColorSteady(), GearConfig.shieldGlow / 100.0F);
			if (glow >>> 24 > 0) {
				// eyes: blended without depth writes, so the glow never hides the frame or itself
				collector.submitCustomGeometry(poseStack, RenderTypes.eyes(GLOW_TEXTURE),
						(pose, consumer) -> drawGlow(pose, consumer, glow));
			}
		}
	}

	private static int scaleRgb(int argb, float factor) {

		int r = Math.round((argb >> 16 & 0xFF) * factor);
		int g = Math.round((argb >> 8 & 0xFF) * factor);
		int b = Math.round((argb & 0xFF) * factor);
		return argb & 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
	}

	private static int scaleAlpha(int argb, float factor) {
		int a = Math.round((argb >>> 24) * factor);
		return Math.max(0, Math.min(255, a)) << 24 | argb & 0xFFFFFF;
	}

	/** Two flat planes just in front of and behind the plate, reaching GLOW past its edges. */
	private static void drawGlow(PoseStack.Pose pose, VertexConsumer consumer, int color) {
		float a0 = X0 - GLOW, a1 = X1 + GLOW, b0 = Y0 - GLOW, b1 = Y1 + GLOW;
		// Clearly off the plate and the outline: closer planes flickered (z-fighting)
		float zf = Z0 - 0.012F, zb = Z1 + 0.012F;
		face(pose, consumer, color, a0, b0, zf, a1, b0, zf, a1, b1, zf, a0, b1, zf, 0, 0, -1);
		face(pose, consumer, color, a0, b0, zb, a1, b0, zb, a1, b1, zb, a0, b1, zb, 0, 0, 1);
	}

	/** A ring around the plate: front and back faces plus the outer edge, every face drawn from both sides. */
	private static void drawOutline(PoseStack.Pose pose, VertexConsumer consumer, int color) {
		float a0 = X0 - WIDTH, a1 = X1 + WIDTH, b0 = Y0 - WIDTH, b1 = Y1 + WIDTH;
		float zf = Z0 - 0.003F, zb = Z1 + 0.003F;
		for (float z : new float[] {zf, zb}) {
			float nz = z == zf ? -1 : 1;
			face(pose, consumer, color, a0, b0, z, a1, b0, z, a1, Y0, z, a0, Y0, z, 0, 0, nz);
			face(pose, consumer, color, a0, Y1, z, a1, Y1, z, a1, b1, z, a0, b1, z, 0, 0, nz);
			face(pose, consumer, color, a0, Y0, z, X0, Y0, z, X0, Y1, z, a0, Y1, z, 0, 0, nz);
			face(pose, consumer, color, X1, Y0, z, a1, Y0, z, a1, Y1, z, X1, Y1, z, 0, 0, nz);
		}
		face(pose, consumer, color, a0, b0, zf, a0, b0, zb, a0, b1, zb, a0, b1, zf, -1, 0, 0);
		face(pose, consumer, color, a1, b0, zf, a1, b1, zf, a1, b1, zb, a1, b0, zb, 1, 0, 0);
		face(pose, consumer, color, a0, b0, zf, a1, b0, zf, a1, b0, zb, a0, b0, zb, 0, -1, 0);
		face(pose, consumer, color, a0, b1, zf, a0, b1, zb, a1, b1, zb, a1, b1, zf, 0, 1, 0);
	}

	/** One quad, once in each winding so it shows from both sides. */
	private static void face(PoseStack.Pose pose, VertexConsumer consumer, int color, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
			float nx, float ny, float nz) {
		vertex(pose, consumer, color, x1, y1, z1, 0, 0, nx, ny, nz);
		vertex(pose, consumer, color, x2, y2, z2, 1, 0, nx, ny, nz);
		vertex(pose, consumer, color, x3, y3, z3, 1, 1, nx, ny, nz);
		vertex(pose, consumer, color, x4, y4, z4, 0, 1, nx, ny, nz);
		vertex(pose, consumer, color, x4, y4, z4, 0, 1, -nx, -ny, -nz);
		vertex(pose, consumer, color, x3, y3, z3, 1, 1, -nx, -ny, -nz);
		vertex(pose, consumer, color, x2, y2, z2, 1, 0, -nx, -ny, -nz);
		vertex(pose, consumer, color, x1, y1, z1, 0, 0, -nx, -ny, -nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int color, float x, float y, float z,
			float u, float v, float nx, float ny, float nz) {
		consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		model.root().getExtentsForGui(new PoseStack(), output);
	}

	/** The JSON part: which material's textures to use. */
	record Json(String material, boolean outline) implements SpecialModelRenderer.Unbaked<DataComponentMap> {
		static final MapCodec<Json> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.fieldOf("material").forGetter(Json::material),
				Codec.BOOL.optionalFieldOf("outline", false).forGetter(Json::outline)).apply(instance, Json::new));

		@Override
		public SpecialModelRenderer<DataComponentMap> bake(SpecialModelRenderer.BakingContext context) {
			return new ShieldRenderer(context.sprites(), new ShieldModel(context.entityModelSet().bakeLayer(ModelLayers.SHIELD)),
					material, outline);
		}

		@Override
		public MapCodec<Json> type() {
			return MAP_CODEC;
		}
	}
}
