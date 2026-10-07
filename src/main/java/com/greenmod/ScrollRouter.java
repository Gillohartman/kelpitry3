package com.greenmod;

import net.minecraft.client.Minecraft;

/** Decides what the mouse wheel does: zoom, freecam speed, freelook distance, flight speed. */
public final class ScrollRouter {
	private ScrollRouter() {}

	public static boolean onScroll(double dy) {
		if (ZoomFeature.onScroll(dy)) return true;
		if (FreeView.onScroll(dy)) return true;
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null && Modules.FLIGHT.isActive() && Modules.FLIGHT_SCROLL.value) {
			Setting.Num n = Modules.FLIGHT_SPEED;
			n.value = Math.max(n.min, Math.min(n.max, n.value + dy * 0.1));
			Modules.FLIGHT_VSPEED.value = Math.max(Modules.FLIGHT_VSPEED.min, Math.min(Modules.FLIGHT_VSPEED.max, n.value * 0.8));
			Notifications.push(String.format("Flight speed %.1f", n.value));
			return true;
		}
		return false;
	}
}
