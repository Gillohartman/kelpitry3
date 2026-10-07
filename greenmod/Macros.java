package com.greenmod;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Macro data and runner. Macros only send chat messages and commands. */
public final class Macros {
	public static final class Step {
		public String text;
		public int delayMs;
		public Step(String text, int delayMs) { this.text = text; this.delayMs = delayMs; }
	}

	public static final class Macro {
		public String name = "New Macro";
		public int key = -1;
		public boolean enabled = true;
		public boolean lastDown;
		public final List<Step> steps = new ArrayList<>();
	}

	public static final List<Macro> LIST = new ArrayList<>();

	private record Pending(String text, long at) {}
	private static final List<Pending> QUEUE = new ArrayList<>();

	private Macros() {}

	/** Longest queue we allow, so a macro can never flood a server. */
	private static final int MAX_QUEUED = 12;

	public static void tick(Minecraft mc) {
		if (mc.player == null) return;
		long now = System.currentTimeMillis();

		if (Modules.MACROS.isActive() && mc.screen == null) {
			for (Macro m : LIST) {
				if (m.key < 0) continue;
				boolean d = Keys.down(m.key);
				if (d && !m.lastDown && m.enabled) start(m, now);
				m.lastDown = d;
			}
		}

		for (int i = 0; i < QUEUE.size(); i++) {
			Pending p = QUEUE.get(i);
			if (p.at() <= now) {
				QUEUE.remove(i);
				i--;
				send(mc, p.text());
			}
		}
	}

	private static void start(Macro m, long now) {
		if (QUEUE.size() >= MAX_QUEUED) return;
		// Outside singleplayer / allowed servers every step waits at least one second.
		int minDelay = ServerSafety.allowsRestricted() ? 0 : 1000;
		long t = now;
		for (Step s : m.steps) {
			if (s.text == null || s.text.isBlank()) continue;
			t += Math.max(s.delayMs, minDelay);
			QUEUE.add(new Pending(s.text.trim(), t));
		}
	}

	/** Sends a chat message or command right now. */
	public static void sendNow(Minecraft mc, String text) {
		send(mc, text.trim());
	}

	/** Sends a chat message or command after a delay (ms). */
	public static void enqueue(String text, long delayMs) {
		if (QUEUE.size() >= MAX_QUEUED) return;
		QUEUE.add(new Pending(text.trim(), System.currentTimeMillis() + Math.max(0, delayMs)));
	}

	private static void send(Minecraft mc, String text) {
		if (mc.player == null || mc.player.connection == null) return;
		if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
		else mc.player.connection.sendChat(text);
	}
}
