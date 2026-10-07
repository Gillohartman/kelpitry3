package com.greenmod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag HUD elements with the mouse. Scroll over an element to resize it. Esc / Right Shift to go back. */
public class HudEditor extends Screen {
	private boolean prevL;
	private boolean prevEsc = true;
	private HudPos drag;
	private int startMx, startMy;
	private double startOx, startOy;

	public HudEditor() {
		super(Component.literal("HUD Editor"));
		prevL = Keys.mouse(0);
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean shouldCloseOnEsc() { return false; }

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		HudPos hp = hit((int) x, (int) y);
		if (hp != null) {
			hp.scale.value = Math.max(hp.scale.min, Math.min(hp.scale.max, hp.scale.value + scrollY * 0.05));
			return true;
		}
		return false;
	}

	private static boolean visible(HudPos hp) {
		return hp.module.isActive() && System.currentTimeMillis() - hp.lastSeen < 1500;
	}

	private HudPos hit(int mx, int my) {
		for (int i = HudPos.ALL.size() - 1; i >= 0; i--) {
			HudPos hp = HudPos.ALL.get(i);
			if (!visible(hp)) continue;
			if (mx >= hp.lastX && mx < hp.lastX + hp.lastW && my >= hp.lastY && my < hp.lastY + hp.lastH) return hp;
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		boolean l = Keys.mouse(0);
		boolean click = l && !prevL;
		prevL = l;

		boolean esc = Keys.down(GLFW.GLFW_KEY_ESCAPE) || Keys.down(GLFW.GLFW_KEY_RIGHT_SHIFT);
		if (esc && !prevEsc) {
			Config.save();
			this.minecraft.setScreen(new GreenGui());
			return;
		}
		prevEsc = esc;

		if (click) {
			drag = hit(mouseX, mouseY);
			if (drag != null) {
				startMx = mouseX; startMy = mouseY;
				startOx = drag.ox.value; startOy = drag.oy.value;
			}
		}
		if (!l) drag = null;
		if (drag != null) {
			drag.ox.value = Math.max(drag.ox.min, Math.min(drag.ox.max, startOx + (mouseX - startMx)));
			drag.oy.value = Math.max(drag.oy.min, Math.min(drag.oy.max, startOy + (mouseY - startMy)));
		}

		g.fill(0, 0, this.width, this.height, Ui.a(0x000000, 0.35));
		String title = "HUD Editor - drag elements, scroll to resize, Esc to go back";
		Ui.rr(g, (this.width - this.font.width(title)) / 2 - 8, 6, this.font.width(title) + 16, 16, 6, Ui.a(0x111418, 0.9));
		g.text(this.font, title, (this.width - this.font.width(title)) / 2, 10, Ui.opaque(Ui.accent()));

		for (HudPos hp : HudPos.ALL) {
			if (!visible(hp)) continue;
			boolean hv = hp == drag || (drag == null && hp == hit(mouseX, mouseY));
			int c = hv ? Ui.accent() : 0xFFFFFF;
			Ui.rr(g, hp.lastX - 2, hp.lastY - 2, hp.lastW + 4, hp.lastH + 4, 4, Ui.a(c, hv ? 0.3 : 0.15));
			g.fill(hp.lastX - 2, hp.lastY - 2, hp.lastX + hp.lastW + 2, hp.lastY - 1, Ui.a(c, 0.9));
			g.fill(hp.lastX - 2, hp.lastY + hp.lastH + 1, hp.lastX + hp.lastW + 2, hp.lastY + hp.lastH + 2, Ui.a(c, 0.9));
			g.fill(hp.lastX - 2, hp.lastY - 1, hp.lastX - 1, hp.lastY + hp.lastH + 1, Ui.a(c, 0.9));
			g.fill(hp.lastX + hp.lastW + 1, hp.lastY - 1, hp.lastX + hp.lastW + 2, hp.lastY + hp.lastH + 1, Ui.a(c, 0.9));
			Ui.small(g, this.font, hp.label, hp.lastX, hp.lastY - 11, c, 0.8f);
		}
	}
}
