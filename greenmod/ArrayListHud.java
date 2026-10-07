package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The list of modules that are currently running, longest name first. */
public final class ArrayListHud {
	private ArrayListHud() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.ARRAYLIST.isActive()) return;
		List<String> lines = new ArrayList<>();
		for (Module m : Modules.ALL) {
			if (m.noToggle || m == Modules.ARRAYLIST || !m.isActive()) continue;
			lines.add(Modules.ARR_KEYS.value && m.key >= 0 ? m.name + " [" + Keys.name(m.key) + "]" : m.name);
		}
		if (lines.isEmpty()) return;
		lines.sort(Comparator.comparingInt((String s) -> mc.font.width(s)).reversed());
		int w = 20;
		for (String s : lines) w = Math.max(w, mc.font.width(s));
		w += 10;
		int h = lines.size() * 10 + 4;
		Modules.AL_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		if (Modules.ARR_BG.value) Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.4));
		int y = 3;
		for (String s : lines) {
			g.text(mc.font, s, w - 5 - mc.font.width(s), y, Ui.opaque(Modules.ARR_COLOR.rgb));
			y += 10;
		}
		Modules.AL_POS.end(g);
	}
}
