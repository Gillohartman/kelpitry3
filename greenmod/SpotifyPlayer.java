package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mini player for the Spotify desktop app on Windows. No account or login needed:
 * the song comes from the Spotify window title, and the buttons send the media keys.
 */
public final class SpotifyPlayer {
	private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "KelpClient-Spotify");
		t.setDaemon(true);
		return t;
	});

	private static volatile String title = "";
	private static volatile boolean running;
	private static long lastPoll;
	private static boolean prevPrev, prevPlay, prevNext;
	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

	private SpotifyPlayer() {}

	public static void tick(Minecraft mc) {
		if (!Modules.SPOTIFY.isActive() || !WINDOWS) return;
		long now = System.currentTimeMillis();
		if (now - lastPoll > 2500) {
			lastPoll = now;
			EXEC.execute(SpotifyPlayer::poll);
		}
		if (mc.screen == null) {
			boolean a = Keys.down(Modules.SP_PREV.key), b = Keys.down(Modules.SP_PLAY.key), c = Keys.down(Modules.SP_NEXT.key);
			if (a && !prevPrev) media(177);
			if (b && !prevPlay) media(179);
			if (c && !prevNext) media(176);
			prevPrev = a; prevPlay = b; prevNext = c;
		}
	}

	private static void media(int code) {
		EXEC.execute(() -> {
			try {
				new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
						"(New-Object -ComObject WScript.Shell).SendKeys([char]" + code + ")").start();
			} catch (Throwable ignored) {
			}
		});
	}

	private static void poll() {
		try {
			Process p = new ProcessBuilder("tasklist", "/v", "/fo", "csv", "/nh", "/fi", "IMAGENAME eq Spotify.exe").start();
			boolean any = false;
			String best = "";
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
				String line;
				while ((line = r.readLine()) != null) {
					if (!line.toLowerCase().contains("spotify.exe")) continue;
					any = true;
					List<String> cols = csv(line);
					if (cols.isEmpty()) continue;
					String t = cols.get(cols.size() - 1).trim();
					if (t.isEmpty() || t.equalsIgnoreCase("N/A") || t.equals("AngleHiddenWindow") || t.startsWith("Default IME") || t.startsWith("MSCTFIME")) continue;
					best = t;
				}
			}
			running = any;
			title = best;
		} catch (Throwable e) {
			running = false;
			title = "";
		}
	}

	private static List<String> csv(String line) {
		List<String> out = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean q = false;
		for (char c : line.toCharArray()) {
			if (c == '"') q = !q;
			else if (c == ',' && !q) { out.add(sb.toString()); sb.setLength(0); }
			else sb.append(c);
		}
		out.add(sb.toString());
		return out;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.SPOTIFY.isActive()) return;
		if (!WINDOWS) return;
		if (!running && Modules.SP_HIDE_IDLE.value) return;

		String t = title;
		boolean idle = t.isEmpty() || t.equals("Spotify") || t.startsWith("Spotify Free") || t.startsWith("Spotify Premium");
		String line = !running ? "Spotify is not running" : idle ? "Paused" : t;
		int maxW = 150;
		while (line.length() > 3 && mc.font.width(line) > maxW) line = line.substring(0, line.length() - 2);
		if (!line.equals(idle ? "Paused" : t) && running && !idle) line += "..";

		int w = maxW + 40, h = 26;
		Modules.SPOTIFY_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 6, Ui.a(0x000000, 0.55));
		int ac = Ui.opaque(Modules.SP_COLOR.rgb);
		Ui.rr(g, 5, 5, 16, 16, 8, ac);
		g.text(mc.font, "S", 11, 9, 0xFF000000);
		g.text(mc.font, line, 27, 4, 0xFFFFFFFF);
		Ui.small(g, mc.font, "Spotify  " + Keys.name(Modules.SP_PREV.key) + " / " + Keys.name(Modules.SP_PLAY.key) + " / " + Keys.name(Modules.SP_NEXT.key), 27, 16, ac & 0xFFFFFF, 0.65f);
		Modules.SPOTIFY_POS.end(g);
	}
}
