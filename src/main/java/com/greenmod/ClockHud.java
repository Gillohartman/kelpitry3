package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockHud {
	private static Method dayTime;
	private static boolean searched;

	private ClockHud() {}

	private static String gameTime(Minecraft mc) {
		try {
			if (!searched) {
				searched = true;
				for (Method m : mc.level.getClass().getMethods()) {
					if (m.getName().equals("getDayTime") && m.getParameterCount() == 0) { dayTime = m; break; }
				}
			}
			if (dayTime == null) return null;
			long t = ((Number) dayTime.invoke(mc.level)).longValue() % 24000L;
			int hour = (int) ((t / 1000 + 6) % 24);
			int min = (int) ((t % 1000) * 60 / 1000);
			return String.format("%02d:%02d", hour, min);
		} catch (Throwable e) {
			return null;
		}
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.CLOCK.isActive()) return;
		String pattern = Modules.CLOCK_24H.value ? (Modules.CLOCK_SECONDS.value ? "HH:mm:ss" : "HH:mm") : (Modules.CLOCK_SECONDS.value ? "h:mm:ss a" : "h:mm a");
		String s = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
		if (Modules.CLOCK_GAME.value && mc.level != null) {
			String gt = gameTime(mc);
			if (gt != null) s += "  (day " + gt + ")";
		}
		int w = mc.font.width(s) + 12, h = 16;
		Modules.CLOCK_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		if (Modules.CLOCK_BG.value) Ui.rr(g, 0, 0, w, h, 5, Ui.a(0x000000, 0.45));
		g.text(mc.font, s, 6, 4, Ui.opaque(Modules.CLOCK_COLOR.rgb));
		Modules.CLOCK_POS.end(g);
	}
}
