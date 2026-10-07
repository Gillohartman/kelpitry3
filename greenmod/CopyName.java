package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

/** Copies the nearest player's name to the clipboard with a key. */
public final class CopyName {
	private static String nearest;
	private static boolean prev;

	private CopyName() {}

	public static void tick(Minecraft mc) {
		LocalPlayer me = mc.player;
		nearest = null;
		if (me == null || mc.level == null || !Modules.COPY_NAME.isActive()) return;
		double range = Modules.CN_RANGE.value, best = range * range;
		for (AbstractClientPlayer p : mc.level.players()) {
			if (p == me || p.isSpectator()) continue;
			double d = me.distanceToSqr(p);
			if (d < best) { best = d; nearest = p.getName().getString(); }
		}
		boolean down = Keys.down(Modules.CN_KEY.key);
		if (down && !prev && mc.screen == null && nearest != null) {
			mc.keyboardHandler.setClipboard(nearest);
			Notifications.push("Copied name: " + nearest);
		}
		prev = down;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (nearest == null || !Modules.CN_HINT.value || mc.screen != null) return;
		String s = "[" + Keys.name(Modules.CN_KEY.key) + "] copy " + nearest;
		int w = mc.font.width(s) + 10;
		int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
		int y = mc.getWindow().getGuiScaledHeight() / 2 + 18;
		Ui.rr(g, x, y, w, 13, 4, Ui.a(0x000000, 0.5));
		g.text(mc.font, s, x + 5, y + 3, Ui.opaque(Ui.accent()));
	}
}
