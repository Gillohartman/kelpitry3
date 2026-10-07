package com.greenmod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Hitboxes as crisp, straight lines. Edges are clipped at the near plane so boxes never vanish up close. */
public final class Hitboxes {
	private static final double[] SEGS = new double[48];
	private static final double[] RECT = new double[4];

	private Hitboxes() {}

	public static void render(GuiGraphicsExtractor g, LocalPlayer me, List<Entity> list) {
		boolean any = Modules.H_PLAYERS.module.isActive() || Modules.H_HOSTILE.module.isActive()
				|| Modules.H_PASSIVE.module.isActive() || Modules.H_ITEMS.module.isActive();
		if (!any) return;
		Px.begin(g);
		for (Entity e : list) box(g, me, e);
		Px.end(g);
	}

	private static void box(GuiGraphicsExtractor g, LocalPlayer me, Entity e) {
		Modules.Hit hit = null;
		if (e instanceof ItemEntity) hit = Modules.H_ITEMS;
		else if (e instanceof Player p) {
			if (p.isSpectator()) return;
			hit = Modules.H_PLAYERS;
		} else if (e instanceof Enemy) hit = Modules.H_HOSTILE;
		else if (e instanceof LivingEntity) hit = Modules.H_PASSIVE;
		if (hit == null || !hit.module.isActive()) return;

		double range = hit.range.value;
		if (me.distanceToSqr(e) > range * range) return;
		if (hit.visibleOnly.value && !me.hasLineOfSight(e)) return;

		float pt = Projector.pt;
		double ox = Mth.lerp(pt, e.xOld, e.getX()) - e.getX();
		double oy = Mth.lerp(pt, e.yOld, e.getY()) - e.getY();
		double oz = Mth.lerp(pt, e.zOld, e.getZ()) - e.getZ();

		int rgb = hit.color.rgb;
		if (hit.hitFlash.value && e instanceof LivingEntity le && le.hurtTime > 0) rgb = hit.hitColor.rgb;
		int color = Ui.a(rgb, hit.opacity.value / 100.0);
		draw(g, e, ox, oy, oz, color, hit.thickness.i(), hit.style.index == 1);
	}

	/** Draws a box around an entity, as a 2D rectangle or as a 3D wireframe. */
	public static void draw(GuiGraphicsExtractor g, Entity e, double ox, double oy, double oz, int color, int t, boolean threeD) {
		int n = Projector.box(e.getBoundingBox(), ox, oy, oz, SEGS);
		if (n == 0) return;
		if (threeD) {
			for (int i = 0; i < n; i++) Px.line(g, SEGS[i * 4], SEGS[i * 4 + 1], SEGS[i * 4 + 2], SEGS[i * 4 + 3], color, t);
			return;
		}
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			for (int k = 0; k < 2; k++) {
				double x = SEGS[i * 4 + k * 2], y = SEGS[i * 4 + k * 2 + 1];
				minX = Math.min(minX, x); maxX = Math.max(maxX, x);
				minY = Math.min(minY, y); maxY = Math.max(maxY, y);
			}
		}
		Px.rectOutline(g, minX, minY, maxX, maxY, color, t);
	}
}
