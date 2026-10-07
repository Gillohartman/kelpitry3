package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/** Uses the game's own options for "No Hurt Cam" and "Particles", and puts your values back when switched off. */
public final class OptionTweaks {
	private static Double savedTilt;
	private static ParticleStatus savedParticles;

	private OptionTweaks() {}

	public static void tick(Minecraft mc) {
		if (Modules.NO_HURTCAM.isActive()) {
			if (savedTilt == null) savedTilt = mc.options.damageTiltStrength().get();
			if (mc.options.damageTiltStrength().get() != 0.0) mc.options.damageTiltStrength().set(0.0);
		} else if (savedTilt != null) {
			mc.options.damageTiltStrength().set(savedTilt);
			savedTilt = null;
		}

		if (Modules.PARTICLES.isActive()) {
			if (savedParticles == null) savedParticles = mc.options.particles().get();
			ParticleStatus want = switch (Modules.PA_LEVEL.index) {
				case 0 -> ParticleStatus.ALL;
				case 1 -> ParticleStatus.DECREASED;
				default -> ParticleStatus.MINIMAL;
			};
			if (mc.options.particles().get() != want) mc.options.particles().set(want);
		} else if (savedParticles != null) {
			mc.options.particles().set(savedParticles);
			savedParticles = null;
		}
	}

	public static void restore(Minecraft mc) {
		if (savedTilt != null) { mc.options.damageTiltStrength().set(savedTilt); savedTilt = null; }
		if (savedParticles != null) { mc.options.particles().set(savedParticles); savedParticles = null; }
	}
}
