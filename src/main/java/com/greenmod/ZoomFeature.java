package com.greenmod;

import net.minecraft.client.Minecraft;

/** Smooth zoom with scroll adjustment. Changes the FOV option while the key is held. */
public final class ZoomFeature {
	private static double level = 1.0, target = 1.0;
	private static Integer baseFov;
	private static long lastNs = System.nanoTime();
	private static double rememberedTarget = -1;
	public static boolean zooming;

	private ZoomFeature() {}

	private static boolean wanted(Minecraft mc) {
		return Modules.ZOOM.isActive() && mc.screen == null && Keys.down(Modules.ZOOM_KEY.key);
	}

	/** Called by the scroll mixin. Returns true if the scroll was used for zooming. */
	public static boolean onScroll(double dy) {
		if (!zooming) return false;
		double min = Modules.ZOOM_MIN.value, max = Math.max(min, Modules.ZOOM_MAX.value);
		target = Math.max(min, Math.min(max, target + dy * Modules.ZOOM_STEP.value));
		rememberedTarget = target;
		return true;
	}

	/** Called every frame (from the HUD) and every tick. */
	public static void update() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		long now = System.nanoTime();
		double dt = Math.min(0.1, (now - lastNs) / 1e9);
		lastNs = now;

		boolean want = wanted(mc);
		if (want) {
			if (baseFov == null) {
				baseFov = mc.options.fov().get();
				level = 1.0;
				double start = Modules.ZOOM_REMEMBER.value && rememberedTarget > 0 ? rememberedTarget : Modules.ZOOM_LEVEL.value;
				target = Math.max(Modules.ZOOM_MIN.value, Math.min(Modules.ZOOM_MAX.value, start));
			}
			zooming = true;
		} else {
			zooming = false;
			target = 1.0;
		}

		if (baseFov == null) return;

		double k = Math.min(1.0, dt * Modules.ZOOM_SMOOTH.value);
		level += (target - level) * k;

		if (!want && Math.abs(level - 1.0) < 0.015) {
			GreenMod.forceOption(mc.options.fov(), baseFov);
			baseFov = null;
			level = 1.0;
			return;
		}
		int fov = (int) Math.round(baseFov / level);
		GreenMod.forceOption(mc.options.fov(), Math.max(5, fov));
	}

	public static void restore() {
		Minecraft mc = Minecraft.getInstance();
		if (baseFov != null) {
			GreenMod.forceOption(mc.options.fov(), baseFov);
			baseFov = null;
		}
		level = 1.0;
		target = 1.0;
		zooming = false;
	}
}
