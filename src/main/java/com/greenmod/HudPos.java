package com.greenmod;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Anchor, offset and scale settings shared by all HUD elements. Elements can be dragged in the HUD editor. */
public final class HudPos {
	public static final List<HudPos> ALL = new ArrayList<>();

	public final Module module;
	public final String label;
	public final Setting.Choice anchor;
	public final Setting.Num ox, oy, scale;

	/** Screen bounds from the last time the element was drawn (used by the HUD editor). */
	public int lastX, lastY, lastW, lastH;
	public long lastSeen;

	public HudPos(Module m, String label, int defaultAnchor, double dx, double dy, double defScale) {
		this.module = m;
		this.label = label;
		anchor = m.choice("Anchor", defaultAnchor, "Top Left", "Top Center", "Top Right", "Bottom Left", "Bottom Center", "Bottom Right", "Center");
		ox = m.num("Offset X", -1500, 1500, dx, 1);
		oy = m.num("Offset Y", -1500, 1500, dy, 1);
		scale = m.num("Scale", 0.4, 3.0, defScale, 0.05);
		ALL.add(this);
	}

	/** Pushes a transform so content can be drawn at (0,0). Call end() afterwards. */
	public void begin(GuiGraphicsExtractor g, int sw, int sh, int cw, int ch) {
		float s = scale.f();
		float w = cw * s, h = ch * s;
		float x, y;
		switch (anchor.index) {
			case 0: x = 0; y = 0; break;
			case 1: x = (sw - w) / 2f; y = 0; break;
			case 2: x = sw - w; y = 0; break;
			case 3: x = 0; y = sh - h; break;
			case 4: x = (sw - w) / 2f; y = sh - h; break;
			case 5: x = sw - w; y = sh - h; break;
			default: x = (sw - w) / 2f; y = (sh - h) / 2f; break;
		}
		x += (float) ox.value;
		y += (float) oy.value;
		lastX = Math.round(x);
		lastY = Math.round(y);
		lastW = Math.max(8, Math.round(w));
		lastH = Math.max(8, Math.round(h));
		lastSeen = System.currentTimeMillis();
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(s, s);
	}

	public void end(GuiGraphicsExtractor g) {
		g.pose().popMatrix();
	}
}
