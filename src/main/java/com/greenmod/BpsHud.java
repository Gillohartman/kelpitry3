package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** Blocks per second (horizontal), averaged over the last second. */
public final class BpsHud {
	private static double lx, lz;
	private static boolean has;
	private static final double[] RING = new double[20];
	private static int idx;
	private static double bps;

	private BpsHud() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) { has = false; return; }
		if (has) {
			RING[idx++ % RING.length] = Math.sqrt((p.getX() - lx) * (p.getX() - lx) + (p.getZ() - lz) * (p.getZ() - lz));
			double sum = 0;
			for (double v : RING) sum += v;
			bps = sum; // 20 ticks of distance = blocks in one second
		}
		lx = p.getX();
		lz = p.getZ();
		has = true;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.BPS.isActive()) return;
		String s = String.format("%.1f BPS", bps);
		int w = mc.font.width(s) + 12, h = 16;
		Modules.BPS_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		if (Modules.BPS_BG.value) Ui.rr(g, 0, 0, w, h, 5, Ui.a(0x000000, 0.45));
		g.text(mc.font, s, 6, 4, Ui.opaque(Modules.BPS_COLOR.rgb));
		Modules.BPS_POS.end(g);
	}
}
