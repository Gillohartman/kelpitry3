package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class InfoHud {
	private InfoHud() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.INFO.isActive()) return;
		List<String> lines = new ArrayList<>();
		if (Modules.INFO_COORDS.value && !StreamerMode.hideCoords()) lines.add("XYZ " + Mth.floor(me.getX()) + " " + Mth.floor(me.getY()) + " " + Mth.floor(me.getZ()));
		if (Modules.INFO_DIR.value) lines.add("Facing " + me.getDirection().getName());
		if (Modules.INFO_FPS.value) lines.add("FPS " + mc.getFps());
		if (Modules.INFO_PING.value) lines.add("Ping " + PingTracker.mine(mc));
		if (lines.isEmpty()) return;

		int w = 20;
		for (String s : lines) w = Math.max(w, mc.font.width(s));
		w += 10;
		int h = lines.size() * 10 + 6;
		Modules.INFO_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.4));
		int y = 4;
		for (String s : lines) {
			g.text(mc.font, s, 5, y, Ui.opaque(Modules.INFO_COLOR.rgb));
			y += 10;
		}
		Modules.INFO_POS.end(g);
	}
}
