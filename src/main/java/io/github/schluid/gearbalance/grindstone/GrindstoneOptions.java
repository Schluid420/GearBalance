package io.github.schluid.gearbalance.grindstone;

import io.github.schluid.gearbalance.config.GearConfig;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import io.github.schluid.gearbalance.socket.Gems;
import io.github.schluid.gearbalance.socket.Sockets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the grindstone can remove from one piece: padding and gems, several at once. Everything else stays.
 * Removed leather or gems are lost (a gem may return one cut lower, see "gem return").
 */
public final class GrindstoneOptions {
	private GrindstoneOptions() {
	}

	public static final int PADDING = 0;

	/** The piece if exactly one slot holds a single item and the other one is empty, otherwise EMPTY. */
	public static ItemStack single(ItemStack input, ItemStack additional) {
		ItemStack single = input.isEmpty() ? additional : additional.isEmpty() ? input : ItemStack.EMPTY;
		return single.getCount() == 1 ? single : ItemStack.EMPTY;
	}

	/** The choices for this piece, in button order: PADDING and/or socket numbers 1 to 3. */
	public static List<Integer> choices(ItemStack piece) {
		List<Integer> list = new ArrayList<>();
		if (piece.isEmpty()) {
			return list;
		}
		if (PaddedArmor.isActive(piece)) {
			list.add(PADDING);
		}
		if (Sockets.active()) {
			List<String> sockets = Sockets.sockets(piece);
			for (int i = 0; i < sockets.size(); i++) {
				if (!sockets.get(i).isEmpty()) {
					list.add(i + 1);
				}
			}
		}
		return list;
	}

	public static boolean valid(ItemStack piece, int choice) {
		return choice >= 0 && choices(piece).contains(choice);
	}

	/** Bit of one choice in the choice mask. */
	public static int bit(int choice) {
		return 1 << choice;
	}

	public static boolean chosen(int mask, int choice) {
		return (mask & bit(choice)) != 0;
	}

	/** The mask without the choices this piece doesn't have (any more). */
	public static int validMask(ItemStack piece, int mask) {
		int result = 0;
		for (int choice : choices(piece)) {
			if (chosen(mask, choice)) {
				result |= bit(choice);
			}
		}
		return result;
	}

	/** Copy of the piece with all chosen things removed. */
	public static ItemStack apply(ItemStack piece, int mask) {
		ItemStack result = piece;
		for (int choice : choices(piece)) {
			if (chosen(mask, choice)) {
				result = choice == PADDING ? PaddedArmor.unpad(result) : Sockets.withoutGem(result, choice - 1);
			}
		}
		return result;
	}

	/** Gem id in the chosen socket, null for the padding or an empty socket. */
	public static String gemId(ItemStack piece, int choice) {
		List<String> sockets = Sockets.sockets(piece);
		if (choice < 1 || choice > sockets.size()) {
			return null;
		}
		String id = sockets.get(choice - 1);
		return id.isEmpty() ? null : id;
	}

	/** The gem one cut lower than the removed one, null for a chipped gem. */
	public static Item lowerCut(String gemId) {
		return Gems.lowerCut(gemId);
	}

	public static boolean gemReturnActive() {
		return GearConfig.gemReturnActive();
	}
}
