package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private ArmorHud() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.ARMOR.isActive()) return;

		boolean horizontal = Modules.ARMOR_ORIENT.index == 0;
		int spacing = Modules.ARMOR_SPACING.i();
		int style = Modules.ARMOR_DUR.index; // 0 none, 1 bar, 2 number, 3 percent
		boolean textStyle = style >= 2;

		int cellW = horizontal ? (textStyle ? 22 : 16) : 16 + (textStyle ? 26 : 0);
		int cellH = 16 + (style == 1 ? 3 : 0) + (horizontal && textStyle ? 9 : 0);
		int cw = horizontal ? 4 * cellW + 3 * spacing : cellW;
		int ch = horizontal ? cellH : 4 * cellH + 3 * spacing;

		Modules.ARMOR_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), cw + 6, ch + 6);
		if (Modules.ARMOR_BG.value) {
			Ui.rr(g, 0, 0, cw + 6, ch + 6, 4, Ui.a(0x000000, Modules.ARMOR_BG_OP.value / 100.0));
		}
		for (int i = 0; i < 4; i++) {
			ItemStack st = me.getItemBySlot(SLOTS[i]);
			if (st.isEmpty()) continue;
			int px = 3 + (horizontal ? i * (cellW + spacing) + (cellW - 16) / 2 : 0);
			int py = 3 + (horizontal ? 0 : i * (cellH + spacing));
			g.item(st, px, py);

			if (!st.isDamageableItem() || style == 0) continue;
			int max = st.getMaxDamage();
			int left = max - st.getDamageValue();
			double frac = max <= 0 ? 1 : left / (double) max;
			int color = Ui.opaque(st.getBarColor());
			if (style == 1) {
				g.fill(px, py + 17, px + 16, py + 19, 0xFF202020);
				g.fill(px, py + 17, px + (int) Math.round(16 * frac), py + 19, color);
			} else {
				String s = style == 2 ? String.valueOf(left) : Math.round(frac * 100) + "%";
				if (horizontal) {
					int tw = mc.font.width(s);
					Ui.small(g, mc.font, s, px + 8 - tw * 0.35f, py + 17, color & 0xFFFFFF, 0.7f);
				} else {
					Ui.small(g, mc.font, s, px + 19, py + 4, color & 0xFFFFFF, 0.8f);
				}
			}
		}
		Modules.ARMOR_POS.end(g);
	}
}
