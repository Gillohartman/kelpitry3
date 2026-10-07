package com.greenmod.mixin;

import com.greenmod.FreeView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow protected abstract void setPosition(double x, double y, double z);
	@Shadow protected abstract void setRotation(float yRot, float xRot);

	/** Freelook and Freecam: after vanilla places the camera, move it where we want it. */
	@Inject(method = {"setup", "update"}, at = @At("TAIL"), require = 0)
	private void kelp$freeView(CallbackInfo ci) {
		if (!FreeView.active()) return;
		float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 p = FreeView.cameraPos(pt);
		this.setRotation(FreeView.yaw, FreeView.pitch);
		this.setPosition(p.x, p.y, p.z);
	}
}
