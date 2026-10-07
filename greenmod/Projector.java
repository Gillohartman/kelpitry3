package com.greenmod;

import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Projects world positions onto the GUI-scaled screen. Works in first/third person, freelook and freecam. */
public final class Projector {
	private static final double NEAR = 0.05;
	private static double ex, ey, ez, fx, fy, fz, rx, ry, rz, ux, uy, uz, tanHalf, aspect;
	public static int w, h;
	public static float pt;
	public static boolean thirdPerson;
	public static double sx, sy, depth;

	private Projector() {}

	public static boolean begin(DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer me = mc.player;
		if (me == null) return false;

		pt = delta.getGameTimeDeltaPartialTick(false);
		double yawDeg, pitchDeg;

		if (FreeView.active()) {
			thirdPerson = true;
			Vec3 c = FreeView.cameraPos(pt);
			ex = c.x; ey = c.y; ez = c.z;
			yawDeg = FreeView.yaw;
			pitchDeg = FreeView.pitch;
		} else {
			CameraType ct = mc.options.getCameraType();
			thirdPerson = !ct.isFirstPerson();
			Vec3 eye = me.getEyePosition(pt);
			yawDeg = me.getViewYRot(pt);
			pitchDeg = me.getViewXRot(pt);
			if (thirdPerson && ct.isMirrored()) {
				yawDeg += 180.0;
				pitchDeg = -pitchDeg;
			}
			ex = eye.x; ey = eye.y; ez = eye.z;
		}

		double yaw = Math.toRadians(yawDeg);
		double pitch = Math.toRadians(pitchDeg);
		fx = -Math.sin(yaw) * Math.cos(pitch);
		fy = -Math.sin(pitch);
		fz = Math.cos(yaw) * Math.cos(pitch);
		rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
		ux = ry * fz - rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy - ry * fx;

		if (!FreeView.active() && thirdPerson) {
			ex -= fx * 4.0; ey -= fy * 4.0; ez -= fz * 4.0;
		}

		w = mc.getWindow().getGuiScaledWidth();
		h = mc.getWindow().getGuiScaledHeight();
		tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
		aspect = w / (double) h;
		return true;
	}

	/** Camera-space coordinates {right, up, depth}. */
	public static double[] cam(double x, double y, double z) {
		double dx = x - ex, dy = y - ey, dz = z - ez;
		return new double[] {dx * rx + dy * ry + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
	}

	private static void screen(double[] c, double[] out, int off) {
		out[off] = w / 2.0 + (c[0] / (c[2] * tanHalf * aspect)) * (w / 2.0);
		out[off + 1] = h / 2.0 - (c[1] / (c[2] * tanHalf)) * (h / 2.0);
	}

	private static double[] clipNear(double[] from, double[] to) {
		double t = (NEAR - from[2]) / (to[2] - from[2]);
		return new double[] {from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t, NEAR};
	}

	public static boolean project(double x, double y, double z) {
		double[] c = cam(x, y, z);
		if (c[2] < 0.1) return false;
		double[] o = new double[2];
		screen(c, o, 0);
		sx = o[0];
		sy = o[1];
		depth = c[2];
		return true;
	}

	/** Projects a 3D line segment, clipped against the camera's near plane. out = {x1, y1, x2, y2} in GUI pixels. */
	public static boolean segment(double ax, double ay, double az, double bx, double by, double bz, double[] out) {
		double[] a = cam(ax, ay, az), b = cam(bx, by, bz);
		if (a[2] < NEAR && b[2] < NEAR) return false;
		if (a[2] < NEAR) a = clipNear(a, b);
		else if (b[2] < NEAR) b = clipNear(b, a);
		screen(a, out, 0);
		screen(b, out, 2);
		return true;
	}

	/** Fills the 12 edges of a box. Returns how many are visible; segments are stored 4 numbers each. */
	public static int box(AABB bb, double ox, double oy, double oz, double[] segs) {
		int n = 0;
		double[] tmp = new double[4];
		for (int i = 0; i < 8; i++) {
			for (int bit = 1; bit <= 4; bit <<= 1) {
				if ((i & bit) != 0) continue;
				int j = i | bit;
				double x1 = ((i & 1) == 0 ? bb.minX : bb.maxX) + ox, y1 = ((i & 2) == 0 ? bb.minY : bb.maxY) + oy, z1 = ((i & 4) == 0 ? bb.minZ : bb.maxZ) + oz;
				double x2 = ((j & 1) == 0 ? bb.minX : bb.maxX) + ox, y2 = ((j & 2) == 0 ? bb.minY : bb.maxY) + oy, z2 = ((j & 4) == 0 ? bb.minZ : bb.maxZ) + oz;
				if (segment(x1, y1, z1, x2, y2, z2, tmp)) {
					System.arraycopy(tmp, 0, segs, n * 4, 4);
					n++;
				}
			}
		}
		return n;
	}

	/** Screen rectangle {x1, y1, x2, y2} (GUI pixels) around a box, or false if it is entirely behind the camera. */
	public static boolean rect(AABB bb, double ox, double oy, double oz, double[] out, double[] segs) {
		int n = box(bb, ox, oy, oz, segs);
		if (n == 0) return false;
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			for (int k = 0; k < 2; k++) {
				double x = segs[i * 4 + k * 2], y = segs[i * 4 + k * 2 + 1];
				minX = Math.min(minX, x); maxX = Math.max(maxX, x);
				minY = Math.min(minY, y); maxY = Math.max(maxY, y);
			}
		}
		out[0] = minX; out[1] = minY; out[2] = maxX; out[3] = maxY;
		return true;
	}
}
