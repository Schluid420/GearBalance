package io.github.schluid.gearbalance.client.mixin;

import io.github.schluid.gearbalance.client.SmoothScroll;
import io.github.schluid.gearbalance.client.SmoothScrolling;
import net.minecraft.client.gui.components.AbstractScrollArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lists glide to the scrolled position instead of jumping (SmoothScroll). */
@Mixin(AbstractScrollArea.class)
public abstract class ScrollAreaMixin implements SmoothScrolling {
	@Shadow
	public abstract double scrollAmount();

	@Shadow
	public abstract void setScrollAmount(double amount);

	@Shadow
	public abstract int maxScrollAmount();

	@Shadow
	protected abstract boolean scrollable();

	@Shadow
	protected abstract double scrollRate();

	@Unique
	private double gearbalance$target = Double.NaN;
	@Unique
	private long gearbalance$last;
	@Unique
	private boolean gearbalance$own;

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void gearbalance$smooth(double mouseX, double mouseY, double scrollX, double scrollY,
			CallbackInfoReturnable<Boolean> cir) {
		if (!SmoothScroll.lists() || !((AbstractScrollArea) (Object) this).visible || !scrollable() || scrollY == 0) {
			return;
		}
		double from = Double.isNaN(gearbalance$target) ? scrollAmount() : gearbalance$target;
		gearbalance$target = Math.max(0, Math.min(maxScrollAmount(), from - scrollY * scrollRate()));
		gearbalance$last = System.nanoTime();
		cir.setReturnValue(true);
	}

	/** Any other change (scrollbar, keys, a new list) ends the gliding. */
	@Inject(method = "setScrollAmount", at = @At("HEAD"))
	private void gearbalance$otherScroll(double amount, CallbackInfo ci) {
		if (!gearbalance$own) {
			gearbalance$target = Double.NaN;
		}
	}

	@Override
	public void gearbalance$scrollFrame() {
		if (Double.isNaN(gearbalance$target)) {
			return;
		}
		long now = System.nanoTime();
		double current = scrollAmount();
		double next = current + (gearbalance$target - current) * SmoothScroll.step(gearbalance$last, now);
		gearbalance$last = now;
		if (Math.abs(gearbalance$target - next) < 0.5) {
			next = gearbalance$target;
			gearbalance$target = Double.NaN;
		}
		gearbalance$own = true;
		setScrollAmount(next);
		gearbalance$own = false;
	}
}
