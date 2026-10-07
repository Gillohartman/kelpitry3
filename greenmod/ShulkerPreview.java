package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Read-only preview of a hovered shulker box. Hold the freeze key (Left Alt) to keep the preview where it is,
 * so you can move the mouse over its items and read their names. Never opens or modifies the container.
 */
public final class ShulkerPreview {
	private static String frozenTitle;
	private static NonNullList<ItemStack> frozenItems;
	private static int frozenTint;
	private static float frozenX, frozenY;

	private ShulkerPreview() {}

	public static void render(GuiGraphicsExtractor g, int mouseX, int mouseY, Slot hovered) {
		if (!Modules.SHULKER.isActive()) { frozenItems = null; return; }
		Minecraft mc = Minecraft.getInstance();
		boolean hold = Keys.down(Modules.SH_FREEZE.key);
		if (!hold) frozenItems = null;

		String title;
		NonNullList<ItemStack> items;
		int tint;
		float x, y;

		float s = (float) Modules.SH_SCALE.value;
		int w = 9 * 18 + 10, h = 3 * 18 + 24;
		int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
		float pw = w * s, ph = h * s;

		if (frozenItems != null) {
			title = frozenTitle; items = frozenItems; tint = frozenTint; x = frozenX; y = frozenY;
		} else {
			if (hovered == null) return;
			ItemStack st = hovered.getItem();
			if (st.isEmpty()) return;
			if (!(st.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock sb)) return;

			items = NonNullList.withSize(27, ItemStack.EMPTY);
			ItemContainerContents contents = st.get(DataComponents.CONTAINER);
			if (contents != null) contents.copyInto(items);
			DyeColor dye = sb.getColor();
			tint = dye == null ? 0x946794 : dye.getMapColor().col;
			title = st.getHoverName().getString();

			switch (Modules.SH_ANCHOR.index) {
				case 1: x = 6; y = 6; break;
				case 2: x = sw - pw - 6; y = 6; break;
				case 3: x = 6; y = sh - ph - 6; break;
				case 4: x = sw - pw - 6; y = sh - ph - 6; break;
				default: x = mouseX + 14; y = mouseY - ph - 6; break;
			}
			x += (float) Modules.SH_OX.value;
			y += (float) Modules.SH_OY.value;
			x = Math.max(2, Math.min(sw - pw - 2, x));
			y = Math.max(2, Math.min(sh - ph - 2, y));

			if (hold) {
				frozenTitle = title; frozenItems = items; frozenTint = tint; frozenX = x; frozenY = y;
			}
		}

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(s, s);

		Ui.rr(g, 0, 0, w, h, 6, Ui.a(Ui.mix(0x101214, tint, 0.18), 0.95));
		Ui.rr(g, 0, 0, w, 3, 2, Ui.opaque(tint));
		g.text(mc.font, title + (frozenItems != null ? "  (frozen)" : ""), 6, 7, 0xFFFFFFFF);

		// mouse position inside the preview, for item names while frozen
		double lx = (mouseX - x) / s, ly = (mouseY - y) / s;
		ItemStack tipStack = ItemStack.EMPTY;
		int tipX = 0, tipY = 0;

		for (int i = 0; i < 27; i++) {
			int ix = 5 + (i % 9) * 18;
			int iy = 20 + (i / 9) * 18;
			boolean over = lx >= ix && lx < ix + 17 && ly >= iy && ly < iy + 17;
			Ui.rr(g, ix, iy, 17, 17, 2, Ui.a(over ? 0xFFFFFF : 0x000000, over ? 0.25 : 0.35));
			ItemStack it = items.get(i);
			if (it.isEmpty()) continue;
			g.item(it, ix + 1, iy + 1);
			if (Modules.SH_COUNTS.value) g.itemDecorations(mc.font, it, ix + 1, iy + 1);
			if (over) { tipStack = it; tipX = ix; tipY = iy; }
		}

		if (!tipStack.isEmpty()) {
			String t = tipStack.getHoverName().getString() + (tipStack.getCount() > 1 ? " x" + tipStack.getCount() : "");
			int tw = mc.font.width(t);
			int bx = Math.min(w - tw - 8, Math.max(2, tipX - tw / 2));
			int by = tipY + 20;
			Ui.rr(g, bx, by, tw + 8, 12, 3, Ui.a(0x000000, 0.92));
			g.text(mc.font, t, bx + 4, by + 2, 0xFFFFFFFF);
		}
		pose.popMatrix();
	}
}
