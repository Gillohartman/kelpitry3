package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Small drawing helpers: rounded rectangles, small text, colours. */
public final class Ui {
	private Ui() {}

	public static int a(int rgb, double opacity) {
		int al = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255);
		return (al << 24) | (rgb & 0xFFFFFF);
	}

	public static int opaque(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

	public static int mix(int c1, int c2, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
		int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
		int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
		return (r << 16) | (g << 8) | b;
	}

	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float h6 = h * 6f;
		int i = (int) h6;
		float f = h6 - i;
		float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
		float r, g, b;
		switch (i % 6) {
			case 0: r = v; g = t; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = t; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = t; g = p; b = v; break;
			default: r = v; g = p; b = q; break;
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	public static float[] toHsv(int rgb) {
		float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float d = max - min, h;
		if (d == 0) h = 0;
		else if (max == r) h = ((g - b) / d) % 6f;
		else if (max == g) h = (b - r) / d + 2f;
		else h = (r - g) / d + 4f;
		h /= 6f;
		if (h < 0) h += 1f;
		return new float[] {h, max == 0 ? 0 : d / max, max};
	}

	// ------------------------------------------------------------------
	// Rounded shapes are drawn in real screen pixels with soft corners,
	// no matter how the GUI is scaled. That keeps edges smooth and lines thin.
	// ------------------------------------------------------------------

	/** Device pixels per drawing unit under the current transform. */
	private static float devScale(GuiGraphicsExtractor g) {
		double gs = Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
		float m = Math.abs(g.pose().m00());
		return (float) Math.max(0.05, gs * m);
	}

	private static void fillA(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int c, double frac) {
		int a = (int) (((c >>> 24) & 255) * frac);
		if (a <= 0 || x1 <= x0 || y1 <= y0) return;
		g.fill(x0, y0, x1, y1, (a << 24) | (c & 0xFFFFFF));
	}

	private static void rrDev(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int r, int c) {
		int w = x1 - x0, h = y1 - y0;
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) { g.fill(x0, y0, x1, y1, c); return; }
		g.fill(x0, y0 + r, x1, y1 - r, c);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			double inset = r - Math.sqrt(Math.max(0, r * (double) r - dy * dy));
			int xi = (int) Math.ceil(inset);
			double part = xi - inset;
			g.fill(x0 + xi, y0 + i, x1 - xi, y0 + i + 1, c);
			g.fill(x0 + xi, y1 - i - 1, x1 - xi, y1 - i, c);
			if (xi > 0 && part > 0.04) {
				fillA(g, x0 + xi - 1, y0 + i, x0 + xi, y0 + i + 1, c, part);
				fillA(g, x1 - xi, y0 + i, x1 - xi + 1, y0 + i + 1, c, part);
				fillA(g, x0 + xi - 1, y1 - i - 1, x0 + xi, y1 - i, c, part);
				fillA(g, x1 - xi, y1 - i - 1, x1 - xi + 1, y1 - i, c, part);
			}
		}
	}

	private static void ringDev(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int r, int c) {
		int w = x1 - x0, h = y1 - y0;
		if (w <= 1 || h <= 1) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) {
			g.fill(x0, y0, x1, y0 + 1, c);
			g.fill(x0, y1 - 1, x1, y1, c);
			g.fill(x0, y0 + 1, x0 + 1, y1 - 1, c);
			g.fill(x1 - 1, y0 + 1, x1, y1 - 1, c);
			return;
		}
		g.fill(x0 + r, y0, x1 - r, y0 + 1, c);
		g.fill(x0 + r, y1 - 1, x1 - r, y1, c);
		g.fill(x0, y0 + r, x0 + 1, y1 - r, c);
		g.fill(x1 - 1, y0 + r, x1, y1 - r, c);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			double outer = r - Math.sqrt(Math.max(0, r * (double) r - dy * dy));
			double inner = dy <= r - 1 ? r - Math.sqrt(Math.max(0, (r - 1.0) * (r - 1.0) - dy * dy)) : r;
			int o = (int) Math.ceil(outer), n = (int) Math.ceil(inner);
			if (n <= o) n = o + 1;
			g.fill(x0 + o, y0 + i, x0 + n, y0 + i + 1, c);
			g.fill(x1 - n, y0 + i, x1 - o, y0 + i + 1, c);
			g.fill(x0 + o, y1 - i - 1, x0 + n, y1 - i, c);
			g.fill(x1 - n, y1 - i - 1, x1 - o, y1 - i, c);
		}
	}

	/** Filled rounded rectangle with smooth corners. */
	public static void rr(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c) {
		if (w <= 0 || h <= 0) return;
		float t = devScale(g);
		int x0 = Math.round(x * t), y0 = Math.round(y * t), x1 = Math.round((x + w) * t), y1 = Math.round((y + h) * t);
		var p = g.pose();
		p.pushMatrix();
		p.scale(1f / t, 1f / t);
		rrDev(g, x0, y0, x1, y1, Math.round(r * t), c);
		p.popMatrix();
	}

	/** 1-pixel outline of a rounded rectangle. */
	public static void rrOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c) {
		if (w <= 0 || h <= 0) return;
		float t = devScale(g);
		int x0 = Math.round(x * t), y0 = Math.round(y * t), x1 = Math.round((x + w) * t), y1 = Math.round((y + h) * t);
		var p = g.pose();
		p.pushMatrix();
		p.scale(1f / t, 1f / t);
		ringDev(g, x0, y0, x1, y1, Math.round(r * t), c);
		p.popMatrix();
	}

	/** 1-pixel horizontal hairline. */
	public static void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
		float t = devScale(g);
		int x0 = Math.round(x * t), x1 = Math.round((x + w) * t), y0 = Math.round(y * t);
		var p = g.pose();
		p.pushMatrix();
		p.scale(1f / t, 1f / t);
		g.fill(x0, y0, x1, y0 + 1, c);
		p.popMatrix();
	}

	public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int rgb) {
		g.text(f, s, x, y, opaque(rgb));
	}

	public static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, int rgb, float scale) {
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(scale, scale);
		g.text(f, s, 0, 0, opaque(rgb));
		p.popMatrix();
	}

	/** Small padlock icon, about 7x9. */
	public static void lock(GuiGraphicsExtractor g, int x, int y, int rgb) {
		int c = opaque(rgb);
		g.fill(x + 1, y, x + 6, y + 1, c);
		g.fill(x + 1, y + 1, x + 2, y + 4, c);
		g.fill(x + 5, y + 1, x + 6, y + 4, c);
		rr(g, x, y + 4, 7, 5, 1, c);
	}

	/** Straight line made of 1-pixel-wide runs (Bresenham style, no gaps). */
	public static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		int dx = x2 - x1, dy = y2 - y1;
		if (Math.abs(dy) >= Math.abs(dx)) {
			if (dy == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, color); return; }
			int step = dy > 0 ? 1 : -1;
			int n = Math.abs(dy);
			for (int i = 0; i <= n; i++) {
				int y = y1 + i * step;
				double t0 = Math.max(0, i - 0.5) / n, t1 = Math.min(n, i + 0.5) / n;
				int xa = (int) Math.round(x1 + dx * t0), xb = (int) Math.round(x1 + dx * t1);
				g.fill(Math.min(xa, xb), y, Math.max(xa, xb) + 1, y + 1, color);
			}
		} else {
			int step = dx > 0 ? 1 : -1;
			int n = Math.abs(dx);
			for (int i = 0; i <= n; i++) {
				int x = x1 + i * step;
				double t0 = Math.max(0, i - 0.5) / n, t1 = Math.min(n, i + 0.5) / n;
				int ya = (int) Math.round(y1 + dy * t0), yb = (int) Math.round(y1 + dy * t1);
				g.fill(x, Math.min(ya, yb), x + 1, Math.max(ya, yb) + 1, color);
			}
		}
	}

	public static int accent() { return Modules.GUI_ACCENT.rgb; }

	public static void clickSound() {
		if (!Modules.GUI_SOUND.value) return;
		try {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		} catch (Throwable ignored) {
		}
	}

	/** The white sphere logo (soft shading, light from the top left). Diameter in drawing units. */
	public static void orb(GuiGraphicsExtractor g, int x, int y, int d) {
		if (d <= 0) return;
		float t = devScale(g);
		int dd = Math.max(4, Math.round(d * t));
		int x0 = Math.round(x * t), y0 = Math.round(y * t);
		double r = dd / 2.0, cx = x0 + r, cy = y0 + r;
		var p = g.pose();
		p.pushMatrix();
		p.scale(1f / t, 1f / t);
		for (int row = 0; row < dd; row++) {
			double dy = row + 0.5 - r;
			double half = Math.sqrt(Math.max(0, r * r - dy * dy));
			int left = (int) Math.round(cx - half), right = (int) Math.round(cx + half);
			int step = Math.max(2, dd / 14);
			for (int px = left; px < right; px += step) {
				int px2 = Math.min(right, px + step);
				double mx = (px + px2) / 2.0 - cx, my = row + 0.5 - r;
				// light comes from the top left: brightest there, softly darker toward the bottom right
				double lit = 1.0 - 0.34 * (((mx + my) / (2.0 * r)) * 0.5 + 0.5);
				double edge = Math.sqrt(mx * mx + my * my) / r;
				lit -= 0.10 * edge * edge;
				int v = (int) Math.max(150, Math.min(255, 255 * lit + 8));
				g.fill(px, y0 + row, px2, y0 + row + 1, 0xFF000000 | (v << 16) | (v << 8) | v);
			}
		}
		p.popMatrix();
	}

	/** N / E / S / W letters on the four edges of a square map that starts at (x, y). */
	public static void compassLetters(GuiGraphicsExtractor g, Font f, int x, int y, int size) {
		float sc = 0.7f;
		int cx = x + size / 2, cy = y + size / 2;
		small(g, "N", cx - f.width("N") * sc / 2f, y + 1, 0xFFFFFF, sc);
		small(g, "S", cx - f.width("S") * sc / 2f, y + size - 8, 0xBBBBBB, sc);
		small(g, "W", x + 2, cy - 3, 0xBBBBBB, sc);
		small(g, "E", x + size - 2 - f.width("E") * sc, cy - 3, 0xBBBBBB, sc);
	}

	/** A small arrow at (cx, cy) pointing the way the player looks (yaw in radians, north is up). */
	public static void arrow(GuiGraphicsExtractor g, int cx, int cy, double yaw, int size, int color) {
		double dx = -Math.sin(yaw), dy = Math.cos(yaw);        // direction on the map (x right, z down)
		double px = -dy, py = dx;                              // perpendicular
		double[][] tri = {
				{cx + dx * size, cy + dy * size},
				{cx - dx * size * 0.7 + px * size * 0.7, cy - dy * size * 0.7 + py * size * 0.7},
				{cx - dx * size * 0.3, cy - dy * size * 0.3},
				{cx - dx * size * 0.7 - px * size * 0.7, cy - dy * size * 0.7 - py * size * 0.7}
		};
		// scan-convert the (concave) arrow head row by row
		int minY = (int) Math.floor(Math.min(Math.min(tri[0][1], tri[1][1]), Math.min(tri[2][1], tri[3][1])));
		int maxY = (int) Math.ceil(Math.max(Math.max(tri[0][1], tri[1][1]), Math.max(tri[2][1], tri[3][1])));
		for (int y = minY; y <= maxY; y++) {
			double yy = y + 0.5;
			double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
			for (int i = 0; i < 4; i++) {
				double[] a = tri[i], b = tri[(i + 1) % 4];
				if ((a[1] <= yy && b[1] > yy) || (b[1] <= yy && a[1] > yy)) {
					double x = a[0] + (yy - a[1]) / (b[1] - a[1]) * (b[0] - a[0]);
					lo = Math.min(lo, x);
					hi = Math.max(hi, x);
				}
			}
			if (hi > lo) g.fill((int) Math.round(lo), y, (int) Math.round(hi), y + 1, color);
		}
	}
}
