package io.github.schluid.gearbalance.grindstone;

import java.util.List;

/** Added to the grindstone menu (GrindstoneMenuMixin): chosen removals as bits (GrindstoneOptions.bit): 0 = padding, 1-3 = gems; 0 = vanilla. */
public interface GrindstoneChoice {
	int gearbalance$choice();

	/** Ids of the gems the current result removes (noted when the result is worked out), for the gem return. */
	List<String> gearbalance$removedGems();

	/** Switches one thing (0 = padding, 1 to 3 = socket) on or off and works out the result again. */
	void gearbalance$choose(int choice);
}
