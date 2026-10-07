package com.greenmod.mixin;

import com.greenmod.FreeView;
import com.greenmod.GreenMod;
import com.greenmod.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

	/** Low Fire: remember the real burning state, hide the vanilla overlay (we draw a small one). */
	@Inject(method = "isOnFire", at = @At("RETURN"), cancellable = true, require = 0)
	private void kelp$lowFire(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Minecraft.getInstance().player) {
			GreenMod.realBurning = cir.getReturnValue();
			if (Modules.LOW_FIRE.isActive()) cir.setReturnValue(false);
		}
	}

	/** Freelook / Freecam: mouse movement turns the camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
	private void kelp$turn(double yRot, double xRot, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && FreeView.onTurn(yRot, xRot)) ci.cancel();
	}
}
