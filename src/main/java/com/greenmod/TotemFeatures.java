package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Totem counter and the green glow on totems. */
public final class TotemFeatures {
	private static final ItemStack ICON = new ItemStack(Items.TOTEM_OF_UNDYING);

	private TotemFeatures() {}

	public static int count(LocalPlayer p) {
		int n = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount();
		}
		ItemStack off = p.getOffhandItem();
		if (off.is(Items.TOTEM_OF_UNDYING)) n += off.getCount();
		return n;
	}

	public static void renderCounter(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.TOTEM_COUNT.isActive()) return;
		int n = count(me);
		if (n == 0 && Modules.TOTEM_HIDE0.value) return;
		String s = "x" + n;
		int tw = mc.font.width(s);
		int w = 16 + 4 + tw + 8, h = 20;
		Modules.TOTEM_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		if (Modules.TOTEM_BG.value) Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.4));
		g.item(ICON, 4, 2);
		g.text(mc.font, s, 4 + 16 + 4, 6, Ui.opaque(Modules.TOTEM_TEXT.rgb));
		Modules.TOTEM_POS.end(g);
	}

	/** Green glow over totems in the hotbar. */
	public static void renderHotbarGlow(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.TOTEM_GLOW.isActive() || !Modules.TG_HOTBAR.value) return;
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		for (int i = 0; i < 9; i++) {
			if (!me.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
			glow(g, w / 2 - 90 + i * 20 + 3, h - 16 - 3);
		}
	}

	public static void glow(GuiGraphicsExtractor g, int x, int y) {
		double pulse = Modules.TG_PULSE.value ? 0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 260.0) : 1.0;
		int rgb = Modules.TG_COLOR.rgb;
		Ui.rr(g, x - 2, y - 2, 20, 20, 5, Ui.a(rgb, 0.22 * pulse));
		Ui.rr(g, x - 1, y - 1, 18, 18, 4, Ui.a(rgb, 0.30 * pulse));
	}
}
