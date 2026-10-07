package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** RESTRICTED experimental aim assistance. Only runs while the module is active (never on public servers). */
public final class AimAssist {
	private AimAssist() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null || mc.screen != null || FreeView.active()) return;

		if (Modules.BOW_AIM.isActive() && Keys.down(Modules.BA_KEY.key) && p.isUsingItem()
				&& (p.getUseItem().is(Items.BOW) || p.getUseItem().is(Items.CROSSBOW))) {
			Entity t = pick(mc, p, Modules.BA_RANGE.value, Modules.BA_FOV.value, 0,
					Modules.BA_PLAYERS.value, Modules.BA_HOSTILE.value, Modules.BA_PASSIVE.value);
			if (t != null) {
				Vec3 aim = t.getBoundingBox().getCenter();
				if (Modules.BA_PREDICT.value) {
					double speed = p.getUseItem().is(Items.BOW)
							? Math.max(0.3, Math.min(1.0, p.getTicksUsingItem() / 20.0)) * 3.0 : 3.15;
					Vec3 eye = p.getEyePosition();
					Vec3 vel = t.getDeltaMovement();
					double time = eye.distanceTo(aim) / speed;
					for (int i = 0; i < 2; i++) {
						Vec3 predicted = aim.add(vel.scale(time));
						time = eye.distanceTo(predicted) / speed;
					}
					aim = aim.add(vel.scale(time)).add(0, 0.5 * 0.05 * time * time, 0);
				}
				turnTo(p, aim, Modules.BA_SMOOTH.value);
			}
		}

		if (Modules.SWORD_AIM.isActive() && Keys.down(Modules.SA_KEY.key)) {
			Entity t = pick(mc, p, Modules.SA_RANGE.value, Modules.SA_FOV.value, Modules.SA_PRIORITY.index,
					Modules.SA_PLAYERS.value, Modules.SA_HOSTILE.value, Modules.SA_PASSIVE.value);
			if (t != null) turnTo(p, t.getBoundingBox().getCenter(), Modules.SA_SMOOTH.value);
		}
	}

	private static Entity pick(Minecraft mc, LocalPlayer p, double range, double fov, int priority,
							   boolean players, boolean hostile, boolean passive) {
		List<Entity> list = mc.level.getEntities(p, p.getBoundingBox().inflate(range), e -> true);
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		Entity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity e : list) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.isSpectator()) continue;
			boolean ok = e instanceof Player ? players : e instanceof Enemy ? hostile : passive;
			if (!ok) continue;
			Vec3 c = e.getBoundingBox().getCenter();
			double dist = eye.distanceTo(c);
			if (dist > range || !p.hasLineOfSight(e)) continue;
			Vec3 dir = c.subtract(eye).normalize();
			double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(dir), -1.0, 1.0)));
			if (angle > fov / 2.0) continue;
			double score = priority == 1 ? le.getHealth() : priority == 2 ? angle : dist;
			if (score < bestScore) { bestScore = score; best = e; }
		}
		return best;
	}

	private static void turnTo(LocalPlayer p, Vec3 target, double smooth) {
		Vec3 eye = p.getEyePosition();
		double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
		float k = (float) (1.0 / Math.max(1.0, smooth));
		p.setYRot(p.getYRot() + Mth.wrapDegrees(yaw - p.getYRot()) * k);
		p.setXRot(Mth.clamp(p.getXRot() + (pitch - p.getXRot()) * k, -90f, 90f));
	}
}
