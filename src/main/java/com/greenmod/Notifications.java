package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Small toast messages at the top of the screen. */
public final class Notifications {
	private record Note(String text, long until) {}

	private static final List<Note> NOTES = new ArrayList<>();
	private static final List<String> HISTORY = new ArrayList<>();

	public static List<String> history() { return new ArrayList<>(HISTORY); }

	private Notifications() {}

	public static void push(String text) {
		long now = System.currentTimeMillis();
		for (Note n : NOTES) if (n.text().equals(text) && n.until() - now > 1500) return;
		NOTES.add(new Note(text, now + 3000));
		HISTORY.add(java.time.LocalTime.now().withNano(0) + "  " + text);
		if (HISTORY.size() > 80) HISTORY.remove(0);
		if (NOTES.size() > 4) NOTES.remove(0);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		long now = System.currentTimeMillis();
		NOTES.removeIf(n -> n.until() < now);
		int w = mc.getWindow().getGuiScaledWidth();
		int y = 6;
		for (Note n : NOTES) {
			long left = n.until() - now;
			double fade = left < 400 ? left / 400.0 : 1.0;
			int tw = mc.font.width(n.text());
			int x = (w - tw) / 2;
			Ui.rr(g, x - 6, y, tw + 12, 14, 6, Ui.a(0x101214, 0.85 * fade));
			Ui.rr(g, x - 6, y + 3, 2, 8, 1, Ui.a(Ui.accent(), fade));
			g.text(mc.font, n.text(), x, y + 3, Ui.a(0xFFFFFF, fade));
			y += 17;
		}
	}
}
