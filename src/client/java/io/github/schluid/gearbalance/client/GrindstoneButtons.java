package io.github.schluid.gearbalance.client;

import io.github.schluid.gearbalance.grindstone.GrindstoneChoice;
import io.github.schluid.gearbalance.grindstone.GrindstoneChoicePayload;
import io.github.schluid.gearbalance.grindstone.GrindstoneOptions;
import io.github.schluid.gearbalance.socket.Gems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Grindstone buttons: one per removable thing (padding, gems 1-3), toggled green/off; all chosen are removed together.
 * Left of the window, as the right side belongs to EnchantPlus.
 */
final class GrindstoneButtons {
	private GrindstoneButtons() {
	}

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int WIDTH = 130;
	private static final int ROWS = 4;

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof GrindstoneScreen grindstone)) {
				return;
			}
			int left = (width - GUI_WIDTH) / 2 - 4 - WIDTH;
			int top = (height - GUI_HEIGHT) / 2 + 4;
			int[] choiceOf = new int[ROWS];
			List<Button> buttons = new ArrayList<>();
			for (int i = 0; i < ROWS; i++) {
				int row = i;
				Button button = new SmallButton(left, top + i * 18, WIDTH, 16, Component.empty(),
						b -> press(grindstone.getMenu(), choiceOf[row]), 0.75F);
				button.visible = false;
				buttons.add(button);
				Screens.getWidgets(screen).add(button);
			}
			update(grindstone.getMenu(), buttons, choiceOf);
			ScreenEvents.afterTick(screen).register(s -> update(grindstone.getMenu(), buttons, choiceOf));
		});
	}

	private static void press(GrindstoneMenu menu, int choice) {
		// Same on both sides: the client shows the result right away, the server makes it real
		((GrindstoneChoice) menu).gearbalance$choose(choice);
		ClientPlayNetworking.send(new GrindstoneChoicePayload(choice));
	}

	private static void update(GrindstoneMenu menu, List<Button> buttons, int[] choiceOf) {
		ItemStack piece = GrindstoneOptions.single(menu.getSlot(0).getItem(), menu.getSlot(1).getItem());
		List<Integer> choices = GrindstoneOptions.choices(piece);
		int mask = ((GrindstoneChoice) menu).gearbalance$choice();
		for (int i = 0; i < buttons.size(); i++) {
			Button button = buttons.get(i);
			if (i >= choices.size()) {
				button.visible = false;
				continue;
			}
			int choice = choices.get(i);
			choiceOf[i] = choice;
			button.visible = true;
			button.setMessage(label(piece, choice, GrindstoneOptions.chosen(mask, choice)));
		}
	}

	/** "Remove padding" or "Remove: Flawless Ruby", green while chosen. */
	private static Component label(ItemStack piece, int choice, boolean chosen) {
		MutableComponent text;
		if (choice == GrindstoneOptions.PADDING) {
			text = Component.translatable("gearbalance.grindstone.padding");
		} else {
			String gem = GrindstoneOptions.gemId(piece, choice);
			Item item = gem == null ? null : Gems.byId(gem);
			Component name = item == null ? Component.literal("?") : new ItemStack(item).getHoverName();
			text = Component.translatable("gearbalance.grindstone.gem", name);
		}
		return chosen ? Component.literal("✔ ").append(text).withStyle(ChatFormatting.GREEN) : text;
	}
}
