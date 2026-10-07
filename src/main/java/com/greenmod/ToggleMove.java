package com.greenmod;

import net.minecraft.client.Minecraft;

/** Uses the game's own accessibility options for toggle sneak / toggle sprint. */
public final class ToggleMove {
	private static Boolean savedCrouch, savedSprint;

	private ToggleMove() {}

	public static void tick(Minecraft mc) {
		boolean on = Modules.TOGGLE_MOVE.isActive();
		if (on) {
			if (savedCrouch == null) { savedCrouch = mc.options.toggleCrouch().get(); savedSprint = mc.options.toggleSprint().get(); }
			if (mc.options.toggleCrouch().get() != Modules.TM_SNEAK.value) mc.options.toggleCrouch().set(Modules.TM_SNEAK.value);
			if (mc.options.toggleSprint().get() != Modules.TM_SPRINT.value) mc.options.toggleSprint().set(Modules.TM_SPRINT.value);
		} else if (savedCrouch != null) {
			mc.options.toggleCrouch().set(savedCrouch);
			mc.options.toggleSprint().set(savedSprint);
			savedCrouch = null;
			savedSprint = null;
		}
	}
}
