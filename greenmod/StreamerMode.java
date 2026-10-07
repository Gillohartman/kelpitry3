package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Privacy mode for recording: hides coordinates, waypoints and the server address.
 * It does NOT hide or switch off any module. What the safety system locks stays locked.
 */
public final class StreamerMode {
	private StreamerMode() {}

	public static boolean on() { return Modules.STREAMER.isActive(); }
	public static boolean hideCoords() { return on() && Modules.SM_COORDS.value; }
	public static boolean hideWaypoints() { return on() && Modules.SM_WAYPOINTS.value; }
	public static boolean hideServer() { return on() && Modules.SM_SERVER.value; }

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!on() || !Modules.SM_INDICATOR.value) return;
		String s = "STREAMER MODE";
		int w = mc.font.width(s) + 16;
		int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
		int y = mc.getWindow().getGuiScaledHeight() - 44;
		Ui.rr(g, x, y, w, 14, 7, Ui.a(0x0A0A0B, 0.85));
		Ui.rrOutline(g, x, y, w, 14, 7, Ui.a(0xFFFFFF, 0.5));
		g.text(mc.font, s, x + 8, y + 3, 0xFFFFFFFF);
	}
}
