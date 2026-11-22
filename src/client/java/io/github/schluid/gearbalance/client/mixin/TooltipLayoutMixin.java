package io.github.schluid.gearbalance.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.schluid.gearbalance.client.TooltipFrames;
import io.github.schluid.gearbalance.client.TooltipHead;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Tooltip frames: name line becomes the head; the tooltip opens towards the screen middle. */
@Mixin(GuiGraphicsExtractor.class)
public class TooltipLayoutMixin {
	/** Room the frame takes around the text (TooltipBackgroundMixin). */
	@Unique
	private static final int FRAME = 16;
	/** Room kept free at the bottom of the screen (TVs often cut off the edge). */
	@Unique
	private static final int BOTTOM = 2 * FRAME;
	/** Distance between mouse and tooltip, like vanilla. */
	@Unique
	private static final int MOUSE_GAP = 12;

	@Unique
	private static boolean isEmpty(FormattedCharSequence text) {
		return text.accept((index, style, codePoint) -> Character.isWhitespace(codePoint));
	}

	@ModifyVariable(method = "tooltip", at = @At("HEAD"), argsOnly = true)
	private ClientTooltipPositioner gearbalance$towardsMiddle(ClientTooltipPositioner positioner,
			@Local(argsOnly = true) Identifier style) {
		if (!TooltipFrames.isOwn(style)) {
			return positioner;
		}
		return (screenWidth, screenHeight, mouseX, mouseY, width, height) -> {
			// Right half: open to the left of the mouse; lower half: open upwards
			int x = mouseX < screenWidth / 2 ? mouseX + MOUSE_GAP : mouseX - MOUSE_GAP - width;
			int y = mouseY < screenHeight / 2 ? mouseY - MOUSE_GAP : mouseY + MOUSE_GAP - height;
			x = Math.max(FRAME, Math.min(x, screenWidth - FRAME - width));
			y = Math.max(FRAME, Math.min(y, screenHeight - BOTTOM - height));
			return new Vector2i(x, y);
		};
	}

	@ModifyVariable(method = "tooltip", at = @At("HEAD"), argsOnly = true)
	private List<ClientTooltipComponent> gearbalance$head(List<ClientTooltipComponent> lines,
			@Local(argsOnly = true) Identifier style) {
		return gearbalance$withHead(lines, style);
	}

	/**
	 * Iceberg rebuilds the lines at the start of the same method, possibly after the head above was set, and so
	 * throws the head away. Right before vanilla measures the lines (the list is loaded for its first iterator()),
	 * every such change is done: set it again. Optional, so a changed method body only loses this second chance.
	 */
	@ModifyVariable(method = "tooltip", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;",
			ordinal = 0, shift = At.Shift.BEFORE), argsOnly = true, require = 0)
	private List<ClientTooltipComponent> gearbalance$headAfterOtherMods(List<ClientTooltipComponent> lines,
			@Local(argsOnly = true) Identifier style) {
		return gearbalance$withHead(lines, style);
	}

	/** The name line becomes the head, empty lines get half as high; lines that already have the head stay as they are. */
	@Unique
	private static List<ClientTooltipComponent> gearbalance$withHead(List<ClientTooltipComponent> lines, Identifier style) {
		if (!lines.isEmpty() && lines.get(0) instanceof TooltipHead) {
			return lines;
		}
		if (!TooltipFrames.isOwn(style) || lines.isEmpty()) {
			return lines;
		}
		Font font = Minecraft.getInstance().font;
		FormattedCharSequence name;
		if (lines.get(0) instanceof ClientTextTooltip title) {
			name = ((ClientTextTooltipAccessor) title).gearbalance$text();
		} else {
			// Tooltip mods like Iceberg replace the vanilla line parts with their own: find the name by its size
			name = TooltipHead.nameOf(lines.get(0), TooltipFrames.current, font);
			if (name == null) {
				return lines;
			}
		}
		ItemStack stack = TooltipFrames.isPlain(style) ? null : TooltipFrames.current;
		List<ClientTooltipComponent> result = new ArrayList<>(lines);
		result.set(0, new TooltipHead(stack, name, TooltipFrames.color(style)));
		// Empty lines are half as high, the same room as under the head line (also empty parts of other mods)
		for (int i = 1; i < result.size(); i++) {
			ClientTooltipComponent line = result.get(i);
			boolean empty = line instanceof ClientTextTooltip text
					? isEmpty(((ClientTextTooltipAccessor) text).gearbalance$text())
					: line.getWidth(font) == 0 && line.getHeight(font) > 0 && line.getHeight(font) <= 12;
			if (empty) {
				result.set(i, TooltipHead.EMPTY_LINE);
			}
		}
		return result;
	}
}
