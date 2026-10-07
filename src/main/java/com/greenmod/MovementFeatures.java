package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Flight, Elytra Boost and Auto Walk. All RESTRICTED; isActive() is false while locked. */
public final class MovementFeatures {
	private MovementFeatures() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;

		if (Modules.FLIGHT.isActive() && !p.isFallFlying()) {
			if (mc.screen != null) {
				// Menu open (inventory, chat, ...): hover in place instead of falling.
				if (Modules.FLIGHT_HOVER.value) {
					p.setDeltaMovement(0, 0, 0);
					p.resetFallDistance();
				}
			} else if (!FreeView.freecam()) {
				double yaw = Math.toRadians(p.getYRot());
				double fwd = 0, side = 0, up = 0;
				if (mc.options.keyUp.isDown()) fwd += 1;
				if (mc.options.keyDown.isDown()) fwd -= 1;
				if (mc.options.keyLeft.isDown()) side += 1;
				if (mc.options.keyRight.isDown()) side -= 1;
				if (mc.options.keyJump.isDown()) up += 1;
				if (mc.options.keyShift.isDown()) up -= 1;

				double sp = Modules.FLIGHT_SPEED.value;
				double vx = (-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * sp;
				double vz = (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * sp;
				p.setDeltaMovement(vx, up * Modules.FLIGHT_VSPEED.value, vz);
				p.resetFallDistance();
			}
		}

		if (Modules.ELYTRA_BOOST.isActive() && p.isFallFlying() && mc.screen == null && Keys.down(Modules.EB_KEY.key)) {
			Vec3 look = p.getLookAngle();
			Vec3 v = p.getDeltaMovement().add(look.scale(Modules.EB_POWER.value));
			double max = Modules.EB_MAX.value;
			if (v.length() > max) v = v.scale(max / v.length());
			p.setDeltaMovement(v);
		}

		if (Modules.ELYTRA_FLY.isActive() && p.isFallFlying() && mc.screen == null && !FreeView.freecam()) {
			double yaw = Math.toRadians(p.getYRot());
			double fwd = mc.options.keyUp.isDown() ? 1 : 0;
			double up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
			double sp = Modules.ELF_SPEED.value;
			p.setDeltaMovement(-Math.sin(yaw) * fwd * sp, up * Modules.ELF_VSPEED.value, Math.cos(yaw) * fwd * sp);
		}

		if (Modules.AUTO_WALK.isActive() && mc.screen == null && !FreeView.freecam()) {
			mc.options.keyUp.setDown(true);
		}
	}
}
