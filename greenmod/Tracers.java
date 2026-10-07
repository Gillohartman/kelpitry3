package com.greenmod;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Stable tracer lines: fixed origin you choose, optionally a short stub that always stays on screen. */
public final class Tracers {
	private Tracers() {}

	/** 0 = middle down, 1 = middle, 2 = middle up. */
	public static double[] origin(int choice) {
		double x = Projector.w / 2.0;
		switch (choice) {
			case 1: return new double[] {x, Projector.h / 2.0};
			case 2: return new double[] {x, 6};
			default: return new double[] {x, Projector.h - 4.0};
		}
	}

	public static void draw(GuiGraphicsExtractor g, double ox, double oy, double wx, double wy, double wz,
							int color, int thickness, boolean stub, double stubLen, boolean keepOnScreen) {
		double[] c = Projector.cam(wx, wy, wz);
		double tx, ty;
		boolean ahead = c[2] > 0.1;
		if (ahead) {
			Projector.project(wx, wy, wz);
			tx = Projector.sx;
			ty = Projector.sy;
		} else {
			if (!keepOnScreen) return;
			double dx = -c[0], dy = c[1];
			double len = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy));
			if (len < 1e-5) { dx = 0; dy = 1; len = 1; }
			tx = ox + dx / len * 4000;
			ty = oy + dy / len * 4000;
		}
		boolean onScreen = tx >= 0 && tx <= Projector.w && ty >= 0 && ty <= Projector.h;
		if (!ahead || !onScreen) {
			if (!keepOnScreen) return;
		}
		double dx = tx - ox, dy = ty - oy;
		double len = Math.sqrt(dx * dx + dy * dy);
		if (len < 1) return;
		double ex = tx, ey = ty;
		if (stub && len > stubLen) {
			ex = ox + dx / len * stubLen;
			ey = oy + dy / len * stubLen;
		} else if (!ahead || !onScreen) {
			// off-screen targets: stop at the screen edge direction, still a short readable line
			double l = Math.min(len, Math.max(stubLen, 80));
			ex = ox + dx / len * l;
			ey = oy + dy / len * l;
		}
		Px.line(g, ox, oy, ex, ey, color, thickness);
	}
}
