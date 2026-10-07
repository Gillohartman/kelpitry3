package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;

/**
 * One key, four steps, six commands: press 1 sends commands 1+2, press 2 sends 3+4, press 3 sends 5, press 4 sends 6.
 * The second key sets the counter back to step 1.
 * Commands go out one by one with a real-time delay between them (default 500 ms), never in a single tick.
 * Your own chat messages typed meanwhile wait until the sequence is done.
 */
public final class AutoHome {
	private static final int[][] STEPS = {{0, 1}, {2, 3}, {4}, {5}};
	private static int step;
	private static boolean prevRun, prevReset;

	private static final ArrayDeque<String> PENDING = new ArrayDeque<>();
	private static final ArrayDeque<String> DEFERRED = new ArrayDeque<>();
	private static long nextAt;
	private static int total, done;
	private static volatile boolean sending;

	private AutoHome() {}

	public static int step() { return step; }
	public static boolean running() { return !PENDING.isEmpty(); }

	/** Called from the chat mixin. Returns true if the message was held back. */
	public static boolean defer(String text, boolean command) {
		if (sending || PENDING.isEmpty()) return false;
		DEFERRED.add((command ? "/" : "") + text);
		return true;
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			// disconnect or world change: drop everything silently
			PENDING.clear();
			DEFERRED.clear();
			total = done = 0;
			prevRun = prevReset = false;
			return;
		}
		boolean run = Keys.down(Modules.AH_KEY.key);
		boolean reset = Keys.down(Modules.AH_RESET_KEY.key);
		boolean usable = Modules.AUTO_HOME.isActive() && mc.screen == null;

		if (usable && reset && !prevReset) {
			step = 0;
			Notifications.push("Auto Set Home: back to step 1.");
		}
		if (usable && run && !prevRun) {
			if (running()) Notifications.push("Auto Set Home: still running.");
			else runStep(mc);
		}
		prevRun = run;
		prevReset = reset;

		long now = System.currentTimeMillis();
		if (now < nextAt) return;
		if (!PENDING.isEmpty()) {
			send(mc, PENDING.poll());
			done++;
			nextAt = now + (long) Modules.AH_DELAY.value;
			if (PENDING.isEmpty()) total = done = 0;
		} else if (!DEFERRED.isEmpty()) {
			send(mc, DEFERRED.poll());
			nextAt = now + (long) Modules.AH_DELAY.value;
		}
	}

	private static void send(Minecraft mc, String text) {
		sending = true;
		try {
			Macros.sendNow(mc, text);
		} finally {
			sending = false;
		}
	}

	private static String fill(Minecraft mc, String cmd) {
		return cmd.replace("{x}", String.valueOf((int) Math.floor(mc.player.getX())))
				.replace("{y}", String.valueOf((int) Math.floor(mc.player.getY())))
				.replace("{z}", String.valueOf((int) Math.floor(mc.player.getZ())));
	}

	private static void runStep(Minecraft mc) {
		if (step >= STEPS.length) {
			if (!Modules.AH_WRAP.value) {
				Notifications.push("Auto Set Home: all 4 steps done. Press the reset key.");
				return;
			}
			step = 0;
		}
		int sent = 0;
		for (int idx : STEPS[step]) {
			String cmd = Modules.AH_CMD[idx].value;
			if (cmd == null || cmd.isBlank()) continue;
			PENDING.add(fill(mc, cmd.trim()));
			sent++;
		}
		total = sent;
		done = 0;
		nextAt = Math.max(nextAt, System.currentTimeMillis());
		if (Modules.AH_NOTIFY.value) {
			Notifications.push("Auto Set Home: step " + (step + 1) + "/4" + (sent == 0 ? " (no commands set)" : ""));
		}
		step++;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!running() || total == 0) return;
		String s = "Auto Set Home " + (done + 1) + "/" + total + "...";
		int w = mc.font.width(s) + 16;
		int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
		Ui.rr(g, x, 40, w, 14, 7, Ui.a(0x0A0A0B, 0.9));
		Ui.rrOutline(g, x, 40, w, 14, 7, Ui.a(0xFFFFFF, 0.4));
		g.text(mc.font, s, x + 8, 43, 0xFFFFFFFF);
	}
}
