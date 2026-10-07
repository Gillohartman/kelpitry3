package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Active effects with the time left (red when about to run out) and breath underwater. */
public final class PotionHud {
	private PotionHud() {}

	private static String time(int ticks) {
		int s = ticks / 20;
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}

	private static String roman(int n) {
		String[] r = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
		return n >= 0 && n < r.length ? r[n] : " " + (n + 1);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.POTION.isActive()) return;

		List<String> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (MobEffectInstance e : me.getActiveEffects()) {
			String name = e.getEffect().value().getDisplayName().getString() + roman(e.getAmplifier());
			boolean inf = e.isInfiniteDuration();
			String t = inf ? "inf" : time(e.getDuration());
			lines.add(name + "  " + t);
			colors.add(!inf && e.getDuration() / 20 < Modules.PO_WARN.value ? 0xFF6B6B : 0xFFFFFF);
		}
		if (Modules.PO_AIR.value && me.getAirSupply() < me.getMaxAirSupply()) {
			lines.add("Breath  " + Math.max(0, me.getAirSupply() / 20) + "s");
			colors.add(me.getAirSupply() < 100 ? 0xFF6B6B : 0x6BC7FF);
		}
		if (lines.isEmpty()) return;

		int w = 40;
		for (String s : lines) w = Math.max(w, mc.font.width(s));
		w += 12;
		int h = lines.size() * 10 + 6;
		Modules.POTION_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.45));
		int y = 4;
		for (int i = 0; i < lines.size(); i++) {
			g.text(mc.font, lines.get(i), 6, y, Ui.opaque(colors.get(i)));
			y += 10;
		}
		Modules.POTION_POS.end(g);
	}
}
