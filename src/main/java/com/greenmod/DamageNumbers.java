package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Floating damage numbers above entities that lose health. */
public final class DamageNumbers {
	private record Pop(double x, double y, double z, String text, long born) {}

	private static final Map<Integer, Float> HEALTH = new HashMap<>();
	private static final List<Pop> POPS = new ArrayList<>();

	private DamageNumbers() {}

	public static void tick(Minecraft mc) {
		LocalPlayer me = mc.player;
		if (me == null || mc.level == null || !Modules.DAMAGE_NUMBERS.isActive()) { HEALTH.clear(); POPS.clear(); return; }
		long now = System.currentTimeMillis();
		Map<Integer, Float> seen = new HashMap<>();
		for (Entity e : mc.level.getEntities(me, me.getBoundingBox().inflate(32), x -> x instanceof LivingEntity)) {
			LivingEntity le = (LivingEntity) e;
			float hp = le.getHealth();
			Float prev = HEALTH.get(le.getId());
			if (prev != null && hp < prev - 0.4f) {
				POPS.add(new Pop(le.getX(), le.getY() + le.getBbHeight(), le.getZ(), String.format("-%.1f", prev - hp), now));
			}
			seen.put(le.getId(), hp);
		}
		HEALTH.clear();
		HEALTH.putAll(seen);
		POPS.removeIf(p -> now - p.born() > 1000);
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.DAMAGE_NUMBERS.isActive() || POPS.isEmpty()) return;
		long now = System.currentTimeMillis();
		for (Pop p : new ArrayList<>(POPS)) {
			double age = (now - p.born()) / 1000.0;
			if (!Projector.project(p.x(), p.y() + 0.3 + age * 0.8, p.z())) continue;
			double a = Math.max(0, 1.0 - age * age);
			float s = Modules.DN_SCALE.f();
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate((float) Projector.sx - mc.font.width(p.text()) * s / 2f, (float) Projector.sy);
			pose.scale(s, s);
			g.text(mc.font, p.text(), 0, 0, Ui.a(Modules.DN_COLOR.rgb, a));
			pose.popMatrix();
		}
	}
}
