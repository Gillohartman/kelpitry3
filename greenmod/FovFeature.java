package com.greenmod;

import net.minecraft.client.Minecraft;

/** Turns off FOV and distortion effects (sprint, speed, underwater, nausea, portal). */
public final class FovFeature {
	private static Double savedFov, savedDistort;

	private FovFeature() {}

	public static void tick(Minecraft mc) {
		boolean on = Modules.NO_FOV.isActive();

		if (on && Modules.NF_FOV.value) {
			if (savedFov == null) savedFov = mc.options.fovEffectScale().get();
			if (mc.options.fovEffectScale().get() != 0.0) mc.options.fovEffectScale().set(0.0);
		} else if (savedFov != null) {
			mc.options.fovEffectScale().set(savedFov);
			savedFov = null;
		}

		if (on && Modules.NF_DISTORT.value) {
			if (savedDistort == null) savedDistort = mc.options.screenEffectScale().get();
			if (mc.options.screenEffectScale().get() != 0.0) mc.options.screenEffectScale().set(0.0);
		} else if (savedDistort != null) {
			mc.options.screenEffectScale().set(savedDistort);
			savedDistort = null;
		}
	}

	public static void restore(Minecraft mc) {
		if (savedFov != null) { mc.options.fovEffectScale().set(savedFov); savedFov = null; }
		if (savedDistort != null) { mc.options.screenEffectScale().set(savedDistort); savedDistort = null; }
	}
}
