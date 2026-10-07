package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public final class CpsCounter {
	private static final List<Long> LEFT = new ArrayList<>();
	private static final List<Long> RIGHT = new ArrayList<>();
	private static boolean pl, pr;

	private CpsCounter() {}

	public static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		boolean l = Keys.mouse(0), r = Keys.mouse(1);
		if (mc.screen == null) {
			if (l && !pl) LEFT.add(now);
			if (r && !pr) RIGHT.add(now);
		}
		pl = l;
		pr = r;
		LEFT.removeIf(t -> now - t > 1000);
		RIGHT.removeIf(t -> now - t > 1000);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.CPS.isActive()) return;
		String s = LEFT.size() + " | " + RIGHT.size() + " CPS";
		int w = mc.font.width(s) + 12, h = 16;
		Modules.CPS_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 5, Ui.a(0x000000, 0.45));
		g.text(mc.font, s, 6, 4, Ui.opaque(Modules.CPS_COLOR.rgb));
		Modules.CPS_POS.end(g);
	}
}
