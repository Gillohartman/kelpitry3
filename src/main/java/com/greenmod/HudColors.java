package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** Custom-colored hearts, hunger, XP bar and level number (replace the vanilla elements). */
public final class HudColors {
	private static final String[] HEART = {
			".XX...XX.",
			"XXXX.XXXX",
			"XXXXXXXXX",
			"XXXXXXXXX",
			".XXXXXXX.",
			"..XXXXX..",
			"...XXX...",
			"....X...."
	};

	private HudColors() {}

	private static boolean canDraw(Minecraft mc) {
		return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer();
	}

	private static void heart(GuiGraphicsExtractor g, int x, int y, int rgb, int cols) {
		int c = Ui.opaque(rgb);
		for (int r = 0; r < HEART.length; r++) {
			String row = HEART[r];
			for (int col = 0; col < Math.min(cols, 9); col++) {
				if (row.charAt(col) == 'X') g.fill(x + col, y + r, x + col + 1, y + r + 1, c);
			}
		}
	}

	public static void health(GuiGraphicsExtractor g, Minecraft mc) {
		if (!canDraw(mc)) return;
		LocalPlayer p = mc.player;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		int left = w / 2 - 91;
		int top = h - 39;

		float hp = p.getHealth();
		int slots = (int) Math.ceil(p.getMaxHealth() / 2.0);
		for (int i = 0; i < slots; i++) {
			int row = i / 10, col = i % 10;
			int x = left + col * 8;
			int y = top - row * 10;
			heart(g, x, y, Modules.HC_HEART_BG.rgb, 9);
			float v = hp - i * 2;
			if (v >= 2) heart(g, x, y, Modules.HC_HEART.rgb, 9);
			else if (v >= 1) heart(g, x, y, Modules.HC_HEART.rgb, 5);
		}

		float abs = p.getAbsorptionAmount();
		if (abs > 0) {
			int rows = (slots + 9) / 10;
			int aslots = (int) Math.ceil(abs / 2.0);
			for (int i = 0; i < aslots && i < 10; i++) {
				int x = left + i * 8;
				int y = top - rows * 10;
				float v = abs - i * 2;
				heart(g, x, y, Modules.HC_ABSORB.rgb, v >= 2 ? 9 : 5);
			}
		}
	}

	public static void food(GuiGraphicsExtractor g, Minecraft mc) {
		if (!canDraw(mc)) return;
		LocalPlayer p = mc.player;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		int right = w / 2 + 91;
		int top = h - 39;
		int food = p.getFoodData().getFoodLevel();
		for (int i = 0; i < 10; i++) {
			int x = right - i * 8 - 9;
			Ui.rr(g, x, top, 9, 9, 3, Ui.opaque(Modules.HC_FOOD_BG.rgb));
			int v = food - i * 2;
			if (v >= 2) Ui.rr(g, x, top, 9, 9, 3, Ui.opaque(Modules.HC_FOOD.rgb));
			else if (v == 1) Ui.rr(g, x + 4, top, 5, 9, 2, Ui.opaque(Modules.HC_FOOD.rgb));
		}
	}

	public static void xpBar(GuiGraphicsExtractor g, Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		int x = w / 2 - 91, y = h - 29;
		Ui.rr(g, x, y, 182, 5, 2, Ui.opaque(Modules.HC_XP_BG.rgb));
		int fill = (int) (182 * Math.max(0f, Math.min(1f, p.experienceProgress)));
		if (fill > 0) Ui.rr(g, x, y, Math.max(fill, 4), 5, 2, Ui.opaque(Modules.HC_XP_COLOR.rgb));
	}

	public static void level(GuiGraphicsExtractor g, Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || p.experienceLevel <= 0) return;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		String s = String.valueOf(p.experienceLevel);
		int x = (w - mc.font.width(s)) / 2;
		int y = h - 35;
		int dark = 0xFF000000;
		g.text(mc.font, s, x + 1, y, dark);
		g.text(mc.font, s, x - 1, y, dark);
		g.text(mc.font, s, x, y + 1, dark);
		g.text(mc.font, s, x, y - 1, dark);
		g.text(mc.font, s, x, y, Ui.opaque(Modules.HC_LEVEL.rgb));
	}
}
