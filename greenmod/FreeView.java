package com.greenmod;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Freelook (look around the player) and Freecam (detached camera). */
public final class FreeView {
	public enum Mode { NONE, FREELOOK, FREECAM }

	public static Mode mode = Mode.NONE;
	public static float yaw, pitch;
	public static double x, y, z, px, py, pz;
	public static double dist = 4.0;

	private static boolean fwd, back, left, right, up, down;
	private static CameraType savedType;

	private FreeView() {}

	public static boolean active() { return mode != Mode.NONE; }
	public static boolean freecam() { return mode == Mode.FREECAM; }

	private static int kUp = 87, kDown = 83, kLeft = 65, kRight = 68, kJump = 32, kShift = 340, kSprint = 341;
	private static int codeRefresh;
	private static boolean sprinting;
	private static double vx, vy, vz;

	/**
	 * Start of every client tick: read the REAL keyboard state (not the key binding state, which does not repeat
	 * while a key is held) and keep the player standing still. Every direction is recomputed from the keys that
	 * are held right now, so any combination and any release order works.
	 */
	public static void startTick(Minecraft mc) {
		fwd = back = left = right = up = down = sprinting = false;
		if (mode != Mode.FREECAM) return;
		var o = mc.options;
		if (codeRefresh++ % 40 == 0) {
			kUp = KeyCodes.of(o.keyUp, 87);
			kDown = KeyCodes.of(o.keyDown, 83);
			kLeft = KeyCodes.of(o.keyLeft, 65);
			kRight = KeyCodes.of(o.keyRight, 68);
			kJump = KeyCodes.of(o.keyJump, 32);
			kShift = KeyCodes.of(o.keyShift, 340);
			kSprint = KeyCodes.of(o.keySprint, 341);
		}
		boolean live = mc.screen == null && mc.isWindowActive();   // no stuck keys after Alt-Tab or while a menu is open
		if (live) {
			fwd = Keys.down(kUp);
			back = Keys.down(kDown);
			left = Keys.down(kLeft);
			right = Keys.down(kRight);
			up = Keys.down(kJump);
			down = Keys.down(kShift);
			sprinting = Keys.down(kSprint);
		}
		o.keyUp.setDown(false);
		o.keyDown.setDown(false);
		o.keyLeft.setDown(false);
		o.keyRight.setDown(false);
		o.keyJump.setDown(false);
		o.keyShift.setDown(false);
		o.keySprint.setDown(false);
		o.keyAttack.setDown(false);
		o.keyUse.setDown(false);
	}

	/** End of every client tick: switch modes and move the freecam. */
	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			mode = Mode.NONE;
			return;
		}
		boolean wantCam = Modules.FREECAM.isActive();
		boolean wantLook = !wantCam && Modules.FREELOOK.isActive() && mc.screen == null && Keys.down(Modules.FL_KEY.key);

		if (wantCam) {
			if (mode != Mode.FREECAM) enter(mc, Mode.FREECAM);
		} else if (wantLook) {
			if (mode != Mode.FREELOOK) enter(mc, Mode.FREELOOK);
		} else if (mode != Mode.NONE) {
			exit(mc);
		}

		if (mode == Mode.FREECAM) {
			px = x; py = y; pz = z;
			double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
			double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
			double rx = -Math.cos(yr), rz = -Math.sin(yr);
			double f = (fwd ? 1 : 0) - (back ? 1 : 0);
			double s = (right ? 1 : 0) - (left ? 1 : 0);
			double mx = fx * f + rx * s;
			double mz = fz * f + rz * s;
			double my = fy * f;
			double len = Math.sqrt(mx * mx + my * my + mz * mz);
			double sp = Modules.FC_SPEED.value * (sprinting ? Modules.FC_SPRINT.value : 1.0);
			double tx = 0, ty = 0, tz = 0;
			if (len > 1e-6) { tx = mx / len * sp; ty = my / len * sp; tz = mz / len * sp; }
			ty += ((up ? 1 : 0) - (down ? 1 : 0)) * Modules.FC_VSPEED.value;
			double k = 1.0 - Math.max(0.0, Math.min(0.95, Modules.FC_SMOOTH.value));
			vx += (tx - vx) * k;
			vy += (ty - vy) * k;
			vz += (tz - vz) * k;
			x += vx; y += vy; z += vz;
		}
		if (mode == Mode.FREELOOK) dist = Modules.FL_DIST.value;
	}

	private static void enter(Minecraft mc, Mode m) {
		LocalPlayer p = mc.player;
		vx = vy = vz = 0;
		mode = m;
		yaw = p.getYRot();
		pitch = p.getXRot();
		Vec3 eye = p.getEyePosition(1.0f);
		x = px = eye.x; y = py = eye.y; z = pz = eye.z;
		if (m == Mode.FREELOOK) {
			savedType = mc.options.getCameraType();
			dist = Modules.FL_DIST.value;
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	private static void exit(Minecraft mc) {
		vx = vy = vz = 0;
		if (mode == Mode.FREELOOK && savedType != null) mc.options.setCameraType(savedType);
		savedType = null;
		mode = Mode.NONE;
	}

	/** Called from the player turn mixin. Returns true if the mouse movement was consumed. */
	public static boolean onTurn(double dyaw, double dpitch) {
		if (mode == Mode.NONE) return false;
		yaw += (float) (dyaw * 0.15);
		pitch = Math.max(-90f, Math.min(90f, pitch + (float) (dpitch * 0.15)));
		return true;
	}

	/** Scroll: freecam speed or freelook distance. */
	public static boolean onScroll(double dy) {
		if (mode == Mode.FREECAM && Modules.FC_SCROLL.value) {
			Modules.FC_SPEED.value = clamp(Modules.FC_SPEED.value + dy * 0.1, Modules.FC_SPEED.min, Modules.FC_SPEED.max);
			Notifications.push(String.format("Freecam speed %.1f", Modules.FC_SPEED.value));
			return true;
		}
		if (mode == Mode.FREELOOK && Modules.FL_SCROLL.value) {
			Modules.FL_DIST.value = clamp(Modules.FL_DIST.value - dy * 0.5, Modules.FL_DIST.min, Modules.FL_DIST.max);
			dist = Modules.FL_DIST.value;
			return true;
		}
		return false;
	}

	private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

	public static Vec3 cameraPos(float pt) {
		if (mode == Mode.FREECAM) {
			return new Vec3(px + (x - px) * pt, py + (y - py) * pt, pz + (z - pz) * pt);
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 eye = mc.player.getEyePosition(pt);
		double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
		double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
		return new Vec3(eye.x - fx * dist, eye.y - fy * dist, eye.z - fz * dist);
	}
}
