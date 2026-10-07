package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Keystrokes {
	private Keystrokes() {}

	private static void key(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int w, String label, boolean down) {
		boolean creamy = Modules.KEYS_STYLE.index == 1;
		int bg = creamy
				? (down ? Ui.opaque(Modules.KEYS_COLOR.rgb) : Ui.a(0xF1EEE6, 0.82))
				: (down ? Ui.a(Modules.KEYS_COLOR.rgb, 0.85) : Ui.a(0x000000, 0.45));
		Ui.rr(g, x, y, w, 18, creamy ? 7 : 4, bg);
		if (creamy) Ui.rrOutline(g, x, y, w, 18, 7, Ui.a(0xFFFFFF, down ? 0.9 : 0.35));
		boolean darkText = creamy || down;
		g.text(mc.font, label, x + (w - mc.font.width(label)) / 2, y + 5, darkText ? 0xFF1A1A1A : 0xFFFFFFFF);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.KEYSTROKES.isActive()) return;
		var o = mc.options;
		Modules.KEYS_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 58, 78);
		key(g, mc, 20, 0, 18, "W", o.keyUp.isDown());
		key(g, mc, 0, 20, 18, "A", o.keyLeft.isDown());
		key(g, mc, 20, 20, 18, "S", o.keyDown.isDown());
		key(g, mc, 40, 20, 18, "D", o.keyRight.isDown());
		key(g, mc, 0, 40, 58, "Space", o.keyJump.isDown());
		key(g, mc, 0, 60, 28, "LMB", o.keyAttack.isDown());
		key(g, mc, 30, 60, 28, "RMB", o.keyUse.isDown());
		Modules.KEYS_POS.end(g);
	}
}
