package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ruined Client badge on the Minecraft main menu. */
public final class Branding {
	private Branding() {}

	public static void draw(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		String title = "Ruined Client";
		String sub = "v" + GreenModClient.VERSION + "  -  made by ocxh";
		int w = Math.max(40 + mc.font.width(title), 34 + (int) (mc.font.width(sub) * 0.75f) + 10), h = 30;
		int x = 8, y = 8;

		Ui.rr(g, x, y, w, h, 8, Ui.a(0x0A0A0B, 0.9));
		Ui.rrOutline(g, x, y, w, h, 8, Ui.a(0xFFFFFF, 0.28));
		Ui.orb(g, x + 7, y + 7, 16);
		g.text(mc.font, title, x + 31, y + 6, 0xFFFFFFFF);
		Ui.small(g, mc.font, sub, x + 31, y + 18, 0xAAAAAA, 0.75f);
	}
}
