package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.List;

/** A strip at the top of the screen that shows where north/east/south/west and your waypoints are. */
public final class CompassBar {
	private CompassBar() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.COMPASS.isActive()) return;
		int w = 190, h = 18;
		Modules.COMPASS_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 5, Ui.a(0x000000, 0.45));
		int rgb = Modules.CMP_COLOR.rgb;
		float yaw = me.getYRot();
		double half = 90.0; // degrees shown to each side

		// tick marks every 15 degrees (yaw 0 = south, 90 = west, 180 = north, -90 = east)
		for (int d = 0; d < 360; d += 15) {
			double rel = Mth.wrapDegrees(d - yaw);
			if (Math.abs(rel) > half) continue;
			int x = (int) (w / 2.0 + rel / half * (w / 2.0 - 6));
			g.fill(x, 12, x + 1, 16, Ui.a(rgb, 0.5));
		}
		String[] names = {"S", "W", "N", "E"};
		for (int i = 0; i < 4; i++) {
			double rel = Mth.wrapDegrees(i * 90 - yaw);
			if (Math.abs(rel) > half) continue;
			int x = (int) (w / 2.0 + rel / half * (w / 2.0 - 6));
			int c = names[i].equals("N") ? 0xFF5555 : rgb;
			g.text(mc.font, names[i], x - mc.font.width(names[i]) / 2, 3, Ui.opaque(c));
		}

		if (Modules.CMP_WP.value && Modules.WAYPOINTS.isActive() && !StreamerMode.hideWaypoints()) {
			List<Waypoints.Waypoint> list = Waypoints.forCurrent(mc);
			String dim = Waypoints.currentDim(mc);
			for (Waypoints.Waypoint wp : list) {
				if (!wp.visible || !dim.equals(wp.dim)) continue;
				double dx = wp.x + 0.5 - me.getX(), dz = wp.z + 0.5 - me.getZ();
				double bearing = Math.toDegrees(Math.atan2(-dx, dz));
				double rel = Mth.wrapDegrees(bearing - yaw);
				if (Math.abs(rel) > half) continue;
				int x = (int) (w / 2.0 + rel / half * (w / 2.0 - 6));
				g.fill(x - 1, 9, x + 2, 12, Ui.opaque(wp.rgb));
			}
		}
		g.fill(w / 2, 0, w / 2 + 1, 3, Ui.opaque(Ui.accent()));
		Modules.COMPASS_POS.end(g);
	}
}
