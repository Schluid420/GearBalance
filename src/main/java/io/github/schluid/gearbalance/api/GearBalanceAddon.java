package io.github.schluid.gearbalance.api;

/**
 * Entrypoint for add-ons ("gearbalance" in fabric.mod.json). GearBalance calls it while the game
 * starts, before it reads its config, so the values saved for the add-on's materials and items are
 * found. Register materials and items here with GearBalanceApi, and preset values if wanted.
 */
@FunctionalInterface
public interface GearBalanceAddon {
	void registerGear();
}
