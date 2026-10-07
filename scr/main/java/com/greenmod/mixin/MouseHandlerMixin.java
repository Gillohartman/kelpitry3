package com.greenmod.mixin;

import com.greenmod.ScrollRouter;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	/** The scroll wheel can zoom, change flight/freecam speed or freelook distance. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
	private void kelp$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (ScrollRouter.onScroll(yOffset)) ci.cancel();
	}
}
