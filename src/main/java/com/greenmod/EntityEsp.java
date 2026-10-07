package com.greenmod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** RESTRICTED: highlights entities through walls. Never runs while the module is locked. */
public final class EntityEsp {
	private static final double[] SEGS = new double[48];
	private static final double[] RECT = new double[4];

	private EntityEsp() {}

	public static void render(GuiGraphicsExtractor g, LocalPlayer me, List<Entity> list) {
		if (!Modules.ESP.isActive()) return;
		double range = Modules.ESP_RANGE.value;
		Px.begin(g);
		for (Entity e : list) {
			int rgb;
			if (e instanceof Player p) {
				if (!Modules.ESP_PLAYERS.value || p.isSpectator()) continue;
				rgb = Modules.ESP_C_PLAYERS.rgb;
			} else if (e instanceof Enemy) {
				if (!Modules.ESP_HOSTILE.value) continue;
				rgb = Modules.ESP_C_HOSTILE.rgb;
			} else if (e instanceof LivingEntity) {
				if (!Modules.ESP_PASSIVE.value) continue;
				rgb = Modules.ESP_C_PASSIVE.rgb;
			} else continue;

			if (me.distanceToSqr(e) > range * range) continue;
			float pt = Projector.pt;
			double ox = Mth.lerp(pt, e.xOld, e.getX()) - e.getX();
			double oy = Mth.lerp(pt, e.yOld, e.getY()) - e.getY();
			double oz = Mth.lerp(pt, e.zOld, e.getZ()) - e.getZ();

			int c = Ui.opaque(rgb);
			int style = Modules.ESP_STYLE.index;
			if (style == 1) {
				Hitboxes.draw(g, e, ox, oy, oz, c, Modules.ESP_LINE.i(), true);
			} else if (style == 2 || style == 3) {
				if (Projector.rect(e.getBoundingBox(), ox, oy, oz, RECT, SEGS)) {
					int t = Modules.ESP_LINE.i();
					if (style == 3) {
						Px.rectFill(g, RECT[0], RECT[1], RECT[2], RECT[3], Ui.a(rgb, 0.22));
						Px.rectOutline(g, RECT[0], RECT[1], RECT[2], RECT[3], c, t);
					} else {
						double cw = (RECT[2] - RECT[0]) * 0.28, ch = (RECT[3] - RECT[1]) * 0.28;
						double x1 = RECT[0], y1 = RECT[1], x2 = RECT[2], y2 = RECT[3];
						Px.line(g, x1, y1, x1 + cw, y1, c, t); Px.line(g, x1, y1, x1, y1 + ch, c, t);
						Px.line(g, x2, y1, x2 - cw, y1, c, t); Px.line(g, x2, y1, x2, y1 + ch, c, t);
						Px.line(g, x1, y2, x1 + cw, y2, c, t); Px.line(g, x1, y2, x1, y2 - ch, c, t);
						Px.line(g, x2, y2, x2 - cw, y2, c, t); Px.line(g, x2, y2, x2, y2 - ch, c, t);
					}
				}
			} else {
				Hitboxes.draw(g, e, ox, oy, oz, c, Modules.ESP_LINE.i(), false);
			}

			if (Modules.ESP_TRACERS.value) {
				double[] o = Tracers.origin(Modules.ESP_ORIGIN.index);
				var c2 = e.getBoundingBox().getCenter();
				Tracers.draw(g, o[0], o[1], c2.x + ox, c2.y + oy, c2.z + oz, Ui.a(rgb, 0.85), Modules.ESP_LINE.i(), false, 0, false);
			}
		}
		Px.end(g);
	}
}
