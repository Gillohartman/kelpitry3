package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class DeathInfo {
	private static boolean wasDead;

	private DeathInfo() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead && Modules.DEATH_INFO.isActive()) {
			String cause;
			try {
				cause = p.getCombatTracker().getDeathMessage().getString();
			} catch (Throwable t) {
				cause = "unknown cause";
			}
			String where = (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
			p.displayClientMessage(Component.literal("[Ruined Client] You died at " + where + " (" + Waypoints.currentDim(mc) + ")"), false);
			p.displayClientMessage(Component.literal("[Ruined Client] " + cause), false);
		}
		wasDead = dead;
	}
}
