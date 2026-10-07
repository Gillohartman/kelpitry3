package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Shows the picture of a hovered map item. Only reads the map data the client already has. */
public final class MapPreview {
	private MapPreview() {}

	public static void render(GuiGraphicsExtractor g, int mouseX, int mouseY, Slot hovered) {
		if (!Modules.MAP_PREVIEW.isActive() || hovered == null) return;
		ItemStack st = hovered.getItem();
		if (st.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (st.is(Items.MAP)) { placeholder(g, mc, mouseX, mouseY, "Empty map"); return; }
		if (!st.is(Items.FILLED_MAP)) return;
		MapId id = st.get(DataComponents.MAP_ID);
		if (id == null || mc.level == null) return;
		MapItemSavedData data = mc.level.getMapData(id);
		if (data == null) { placeholder(g, mc, mouseX, mouseY, "No map data on this client"); return; }

		float s = (float) Modules.MP_SCALE.value;
		int w = 128 + 8, h = 128 + 20;
		int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
		float pw = w * s, ph = h * s;
		float x, y;
		switch (Modules.MP_ANCHOR.index) {
			case 1: x = 6; y = 6; break;
			case 2: x = sw - pw - 6; y = 6; break;
			case 3: x = 6; y = sh - ph - 6; break;
			case 4: x = sw - pw - 6; y = sh - ph - 6; break;
			default: x = mouseX + 14; y = mouseY - ph / 2; break;
		}
		x += (float) Modules.MP_OX.value;
		y += (float) Modules.MP_OY.value;
		x = Math.max(2, Math.min(sw - pw - 2, x));
		y = Math.max(2, Math.min(sh - ph - 2, y));

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(s, s);
		Ui.rr(g, 0, 0, w, h, 6, Ui.a(0x101214, 0.95));
		g.text(mc.font, st.getHoverName().getString(), 5, 5, 0xFFFFFFFF);

		byte[] colors = data.colors;
		int ox = 4, oy = 16;
		for (int row = 0; row < 128; row++) {
			int runStart = 0;
			int runColor = argb(colors[row * 128] & 255);
			for (int col = 1; col <= 128; col++) {
				int c = col < 128 ? argb(colors[row * 128 + col] & 255) : runColor ^ 1;
				if (c != runColor) {
					if (runColor != 0) g.fill(ox + runStart, oy + row, ox + col, oy + row + 1, runColor);
					runStart = col;
					runColor = c;
				}
			}
		}
		pose.popMatrix();
	}

	private static void placeholder(GuiGraphicsExtractor g, Minecraft mc, int mouseX, int mouseY, String text) {
		int w = mc.font.width(text) + 14, h = 18;
		int x = Math.min(mouseX + 14, mc.getWindow().getGuiScaledWidth() - w - 2), y = Math.max(2, mouseY - 9);
		Ui.rr(g, x, y, w, h, 6, Ui.a(0x0A0A0B, 0.95));
		Ui.rrOutline(g, x, y, w, h, 6, Ui.a(0xFFFFFF, 0.3));
		g.text(mc.font, text, x + 7, y + 5, 0xFFCCCCCC);
	}

	private static int argb(int packed) {
		if (packed / 4 == 0) return 0;
		int abgr = MapColor.getColorFromPackedId(packed);
		int r = abgr & 255, gr = (abgr >> 8) & 255, b = (abgr >> 16) & 255;
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}
}
