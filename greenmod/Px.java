package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws straight, crisp lines in real screen pixels (not chunky GUI pixels). */
public final class Px {
	private static double gs = 1.0;
	private static int sw, sh;

	private Px() {}

	public static void begin(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		gs = Math.max(1.0, mc.getWindow().getGuiScale());
		sw = (int) Math.round(mc.getWindow().getGuiScaledWidth() * gs);
		sh = (int) Math.round(mc.getWindow().getGuiScaledHeight() * gs);
		var p = g.pose();
		p.pushMatrix();
		p.scale((float) (1.0 / gs), (float) (1.0 / gs));
	}

	public static void end(GuiGraphicsExtractor g) {
		g.pose().popMatrix();
	}

	/** Line between two points given in GUI pixels. Thickness in real pixels. */
	public static void line(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int color, int thickness) {
		double ax = x1 * gs, ay = y1 * gs, bx = x2 * gs, by = y2 * gs;

		// Liang-Barsky clip to the screen so huge off-screen coordinates never cost anything
		double dx = bx - ax, dy = by - ay;
		double t0 = 0, t1 = 1;
		double[] p = {-dx, dx, -dy, dy};
		double[] q = {ax - (-2), (sw + 2) - ax, ay - (-2), (sh + 2) - ay};
		for (int i = 0; i < 4; i++) {
			if (p[i] == 0) {
				if (q[i] < 0) return;
			} else {
				double r = q[i] / p[i];
				if (p[i] < 0) {
					if (r > t1) return;
					if (r > t0) t0 = r;
				} else {
					if (r < t0) return;
					if (r < t1) t1 = r;
				}
			}
		}
		int cx1 = (int) Math.round(ax + t0 * dx), cy1 = (int) Math.round(ay + t0 * dy);
		int cx2 = (int) Math.round(ax + t1 * dx), cy2 = (int) Math.round(ay + t1 * dy);

		int t = Math.max(1, thickness);
		boolean steep = Math.abs(cy2 - cy1) >= Math.abs(cx2 - cx1);
		for (int k = 0; k < t; k++) {
			int off = k - (t - 1) / 2;
			if (steep) Ui.line(g, cx1 + off, cy1, cx2 + off, cy2, color);
			else Ui.line(g, cx1, cy1 + off, cx2, cy2 + off, color);
		}
		if (cx1 == cx2 && cy1 == cy2) g.fill(cx1, cy1, cx1 + t, cy1 + t, color);
	}

	/** Axis-aligned rectangle outline in GUI pixels. */
	public static void rectOutline(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int color, int t) {
		int a = (int) Math.round(x1 * gs), b = (int) Math.round(y1 * gs), c = (int) Math.round(x2 * gs), d = (int) Math.round(y2 * gs);
		if (c < -50 || a > sw + 50 || d < -50 || b > sh + 50) return;
		a = Math.max(a, -50); b = Math.max(b, -50); c = Math.min(c, sw + 50); d = Math.min(d, sh + 50);
		g.fill(a, b, c, b + t, color);
		g.fill(a, d - t, c, d, color);
		g.fill(a, b + t, a + t, d - t, color);
		g.fill(c - t, b + t, c, d - t, color);
	}

	public static void rectFill(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int color) {
		int a = (int) Math.round(x1 * gs), b = (int) Math.round(y1 * gs), c = (int) Math.round(x2 * gs), d = (int) Math.round(y2 * gs);
		if (c < -50 || a > sw + 50 || d < -50 || b > sh + 50) return;
		g.fill(Math.max(a, -50), Math.max(b, -50), Math.min(c, sw + 50), Math.min(d, sh + 50), color);
	}
}
