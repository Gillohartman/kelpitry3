package com.greenmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Waypoints stored separately for every singleplayer world and every server. */
public final class Waypoints {
	public static final class Waypoint {
		public String name;
		public int x, y, z;
		public String dim;
		public int rgb;
		public boolean visible = true;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int[] COLORS = {0x39FF14, 0xFF3B30, 0xFF9500, 0xFFD60A, 0x00E5FF, 0x0A84FF, 0xBF5AF2, 0xFF2D95, 0xFFFFFF};
	private static final Map<String, List<Waypoint>> ALL = new HashMap<>();
	private static boolean loaded, wasDead, prevAddKey;

	private Waypoints() {}

	public static String worldKey(Minecraft mc) {
		if (mc.hasSingleplayerServer()) {
			IntegratedServer s = mc.getSingleplayerServer();
			String n = s != null ? s.getWorldData().getLevelName() : "world";
			return "sp:" + n;
		}
		ServerData sd = mc.getCurrentServer();
		if (sd == null) return "unknown";
		String ip = sd.ip.trim().toLowerCase(Locale.ROOT);
		int c = ip.lastIndexOf(':');
		if (c > 0 && ip.indexOf(':') == c) ip = ip.substring(0, c);
		return "mp:" + ip;
	}

	public static String currentDim(Minecraft mc) {
		return mc.level == null ? "" : mc.level.dimension().toString();
	}

	public static List<Waypoint> forCurrent(Minecraft mc) {
		ensureLoaded();
		return ALL.computeIfAbsent(worldKey(mc), k -> new ArrayList<>());
	}

	public static Waypoint addHere(Minecraft mc, String name) {
		LocalPlayer p = mc.player;
		if (p == null) return null;
		Waypoint w = new Waypoint();
		List<Waypoint> list = forCurrent(mc);
		w.name = name != null ? name : "Waypoint " + (list.size() + 1);
		w.x = (int) Math.floor(p.getX());
		w.y = (int) Math.floor(p.getY());
		w.z = (int) Math.floor(p.getZ());
		w.dim = currentDim(mc);
		w.rgb = COLORS[list.size() % COLORS.length];
		list.add(w);
		save();
		return w;
	}

	public static int nextColor(int current) {
		for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == current) return COLORS[(i + 1) % COLORS.length];
		return COLORS[0];
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || !Modules.WAYPOINTS.isActive()) return;

		boolean d = Keys.down(Modules.WP_ADD_KEY.key);
		if (d && !prevAddKey && mc.screen == null) {
			Waypoint w = addHere(mc, null);
			if (w != null) Notifications.push("Waypoint added: " + w.name);
		}
		prevAddKey = d;

		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead && Modules.WP_DEATH.value) {
			addHere(mc, "Death");
			Notifications.push("Death waypoint saved.");
		}
		wasDead = dead;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.WAYPOINTS.isActive() || StreamerMode.hideWaypoints()) return;
		List<Waypoint> list = forCurrent(mc);
		if (list.isEmpty()) return;
		String dim = currentDim(mc);
		double maxDist = Modules.WP_RANGE.value;
		float scale = Modules.WP_SCALE.f();

		for (Waypoint w : list) {
			if (!w.visible || !dim.equals(w.dim)) continue;
			double dx = w.x + 0.5 - me.getX(), dy = w.y + 0.5 - me.getY(), dz = w.z + 0.5 - me.getZ();
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (maxDist > 0 && dist > maxDist) continue;
			if (!Projector.project(w.x + 0.5, w.y + 1.2, w.z + 0.5)) continue;

			String label = Modules.WP_DIST.value ? w.name + " " + (int) dist + "m" : w.name;
			int tw = mc.font.width(label);
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate((float) Projector.sx, (float) Projector.sy);
			pose.scale(scale, scale);
			Ui.rr(g, -tw / 2 - 3, -22, tw + 6, 11, 3, Ui.a(0x000000, 0.5));
			g.text(mc.font, label, -tw / 2, -20, Ui.opaque(w.rgb));
			// small diamond marker
			int c = Ui.opaque(w.rgb);
			g.fill(-1, -8, 1, -6, c);
			g.fill(-3, -6, 3, -4, c);
			g.fill(-1, -4, 1, -2, c);
			pose.popMatrix();
		}
	}

	// ---------------- persistence ----------------
	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("kelpclient").resolve("waypoints.json");
	}

	private static void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		try {
			Path p = file();
			if (!Files.exists(p)) return;
			JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			for (String key : root.keySet()) {
				List<Waypoint> list = new ArrayList<>();
				for (var e : root.getAsJsonArray(key)) {
					JsonObject o = e.getAsJsonObject();
					Waypoint w = new Waypoint();
					w.name = o.get("name").getAsString();
					w.x = o.get("x").getAsInt();
					w.y = o.get("y").getAsInt();
					w.z = o.get("z").getAsInt();
					w.dim = o.get("dim").getAsString();
					w.rgb = o.get("rgb").getAsInt();
					w.visible = o.get("visible").getAsBoolean();
					list.add(w);
				}
				ALL.put(key, list);
			}
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		try {
			Path p = file();
			Files.createDirectories(p.getParent());
			JsonObject root = new JsonObject();
			for (Map.Entry<String, List<Waypoint>> en : ALL.entrySet()) {
				JsonArray arr = new JsonArray();
				for (Waypoint w : en.getValue()) {
					JsonObject o = new JsonObject();
					o.addProperty("name", w.name);
					o.addProperty("x", w.x);
					o.addProperty("y", w.y);
					o.addProperty("z", w.z);
					o.addProperty("dim", w.dim);
					o.addProperty("rgb", w.rgb);
					o.addProperty("visible", w.visible);
					arr.add(o);
				}
				root.add(en.getKey(), arr);
			}
			Files.writeString(p, GSON.toJson(root));
		} catch (Exception ignored) {
		}
	}
}
