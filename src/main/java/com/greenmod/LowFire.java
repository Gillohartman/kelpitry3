package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small flame strip at the bottom of the screen, replacing the vanilla fire overlay. */
public final class LowFire {
	private LowFire() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.LOW_FIRE.isActive() || !GreenMod.realBurning) return;
		double opacity = Modules.FIRE_OPACITY.value / 100.0;
		double heightFrac = Modules.FIRE_HEIGHT.value / 100.0;
		if (opacity <= 0 || heightFrac <= 0) return;

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int maxH = (int) (h * heightFrac);
		double t = System.currentTimeMillis() / 140.0;

		for (int x = 0; x < w; x += 6) {
			double n = 0.55 + 0.45 * Math.sin(t + x * 0.21) * Math.sin(t * 0.7 + x * 0.05);
			int fh = Math.max(2, (int) (maxH * Math.max(0.15, n)));
			g.fill(x, h - fh, x + 6, h, Ui.a(0xFF5A00, opacity * 0.85));
			int ih = (int) (fh * 0.6);
			g.fill(x + 1, h - ih, x + 5, h, Ui.a(0xFFB300, opacity));
			int th = (int) (fh * 0.28);
			g.fill(x + 2, h - th, x + 4, h, Ui.a(0xFFF2A0, opacity));
		}
	}
}
