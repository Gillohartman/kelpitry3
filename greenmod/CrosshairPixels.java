package com.greenmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** A 15x15 pixel crosshair that you can draw, save under a name and load again. */
public final class CrosshairPixels {
	public static final int N = 15;
	public static final boolean[][] CURRENT = new boolean[N][N];
	public static final Map<String, String> SAVED = new LinkedHashMap<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static boolean loaded;

	private CrosshairPixels() {}

	static {
		// default: a small plus
		for (int i = 3; i <= 11; i++) { if (i != 7) { CURRENT[7][i] = true; CURRENT[i][7] = true; } }
	}

	public static String encode() {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) sb.append(CURRENT[y][x] ? '1' : '0');
		return sb.toString();
	}

	public static void decode(String s) {
		for (int i = 0; i < N * N && i < s.length(); i++) CURRENT[i / N][i % N] = s.charAt(i) == '1';
	}

	public static void clear() { for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) CURRENT[y][x] = false; }

	public static void invert() { for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) CURRENT[y][x] = !CURRENT[y][x]; }

	public static void draw(GuiGraphicsExtractor g, int cx, int cy, int px, int color) {
		int half = N / 2;
		for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
			if (!CURRENT[y][x]) continue;
			int sx = cx + (x - half) * px, sy = cy + (y - half) * px;
			g.fill(sx, sy, sx + px, sy + px, color);
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("kelpclient").resolve("crosshairs.json");
	}

	public static void load() {
		if (loaded) return;
		loaded = true;
		try {
			Path p = file();
			if (!Files.exists(p)) return;
			JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			if (root.has("current")) decode(root.get("current").getAsString());
			JsonObject saved = root.getAsJsonObject("saved");
			if (saved != null) for (String k : saved.keySet()) SAVED.put(k, saved.get(k).getAsString());
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		try {
			Path p = file();
			Files.createDirectories(p.getParent());
			JsonObject root = new JsonObject();
			root.addProperty("current", encode());
			JsonObject saved = new JsonObject();
			for (Map.Entry<String, String> e : SAVED.entrySet()) saved.addProperty(e.getKey(), e.getValue());
			root.add("saved", saved);
			Files.writeString(p, GSON.toJson(root));
		} catch (Exception ignored) {
		}
	}
}
