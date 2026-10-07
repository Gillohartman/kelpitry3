package com.greenmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RESTRICTED world-analysis tool. It counts growth indicator blocks (vines, kelp, dripstone) per chunk.
 * "Observed" = those blocks were really seen. "Estimated" = the count passed your threshold.
 * Natural generation also places these blocks, so an estimate is only a hint, never proof of anyone's presence.
 */
public final class ChunkFinder {
	public static final class Info {
		public int vines, kelp, drip;
		/** Amethyst growth blocks (buds + clusters) and the geode center (sum of positions / amethyst count). */
		public int amethyst, amethystAll;
		public double ax, ay, az;
		/** Seconds YOU spent in this chunk (measured by your client = confirmed). */
		public long seconds;
		public long lastScan;
		public int total() { return vines + kelp + drip; }
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<Long, Info> DATA = new HashMap<>();
	private static final ArrayDeque<Long> QUEUE = new ArrayDeque<>();
	private static final Set<Long> QUEUED = new HashSet<>();
	private static String worldKey = "";
	private static int counter;

	private ChunkFinder() {}

	private static long key(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }
	private static int cxOf(long k) { return (int) (k >> 32); }
	private static int czOf(long k) { return (int) k; }

	public static void tick(Minecraft mc) {
		if (!Modules.CHUNK_FINDER.isActive()) return;
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (level == null || p == null) return;

		String wk = Waypoints.worldKey(mc) + "|" + level.dimension();
		if (!wk.equals(worldKey)) {
			save();
			worldKey = wk;
			DATA.clear();
			QUEUE.clear();
			QUEUED.clear();
			load();
		}

		// measure how long you stay in each chunk (1 second resolution)
		if (counter % 20 == 0) {
			long k = key(p.blockPosition().getX() >> 4, p.blockPosition().getZ() >> 4);
			DATA.computeIfAbsent(k, kk -> new Info()).seconds++;
		}

		// queue chunks around the player that were not scanned recently
		if (++counter % 40 == 0) {
			int r = Modules.CF_RADIUS.i();
			int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
			long now = System.currentTimeMillis();
			for (int cx = pcx - r; cx <= pcx + r; cx++) for (int cz = pcz - r; cz <= pcz + r; cz++) {
				long k = key(cx, cz);
				Info in = DATA.get(k);
				if ((in == null || now - in.lastScan > 60000) && QUEUED.add(k)) QUEUE.add(k);
			}
		}

		// scan a couple of chunks per tick
		int budget = 1 + (int) (Modules.CF_SENS.value / 4);
		while (budget-- > 0 && !QUEUE.isEmpty()) {
			long k = QUEUE.poll();
			QUEUED.remove(k);
			scan(level, k);
		}
		if (counter % 1200 == 0) save();
	}

	private static boolean indicator(Block b) {
		if (Modules.CF_VINES.value && (b == Blocks.VINE || b == Blocks.CAVE_VINES || b == Blocks.CAVE_VINES_PLANT
				|| b == Blocks.WEEPING_VINES || b == Blocks.WEEPING_VINES_PLANT)) return true;
		if (Modules.CF_KELP.value && (b == Blocks.KELP || b == Blocks.KELP_PLANT)) return true;
		return Modules.CF_DRIP.value && b == Blocks.POINTED_DRIPSTONE;
	}

	private static void scan(ClientLevel level, long k) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(cxOf(k), czOf(k));
		if (chunk == null) return;
		Info old = DATA.get(k);
		Info info = new Info();
		if (old != null) info.seconds = old.seconds;
		info.lastScan = System.currentTimeMillis();
		final boolean amethystOn = Modules.CF_AMETHYST.value;
		double sx = 0, sy = 0, sz = 0;
		int cxBase = cxOf(k) << 4, czBase = czOf(k) << 4;
		int si = 0;
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection sec : sections) {
			int baseY = (chunk.getMinSectionY() + si++) << 4;
			if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(st -> indicator(st.getBlock()) || (amethystOn && amethyst(st.getBlock())))) continue;
			for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
				Block b = sec.getBlockState(x, y, z).getBlock();
				if (amethystOn && amethyst(b)) {
					info.amethystAll++;
					if (b != Blocks.AMETHYST_BLOCK && b != Blocks.BUDDING_AMETHYST) {
						info.amethyst++;
						sx += cxBase + x; sy += baseY + y; sz += czBase + z;
					}
					continue;
				}
				if (!indicator(b)) continue;
				if (b == Blocks.KELP || b == Blocks.KELP_PLANT) info.kelp++;
				else if (b == Blocks.POINTED_DRIPSTONE) info.drip++;
				else info.vines++;
			}
		}
		if (info.amethyst > 0) { info.ax = sx / info.amethyst; info.ay = sy / info.amethyst; info.az = sz / info.amethyst; }
		DATA.put(k, info);
	}

	private static boolean amethyst(Block b) {
		return b == Blocks.AMETHYST_BLOCK || b == Blocks.BUDDING_AMETHYST || b == Blocks.AMETHYST_CLUSTER
				|| b == Blocks.LARGE_AMETHYST_BUD || b == Blocks.MEDIUM_AMETHYST_BUD || b == Blocks.SMALL_AMETHYST_BUD;
	}

	/** Time you really spent in the chunk as a percentage of the "100%" hours setting (confirmed, measured by your client). */
	public static int timePercent(Info i) {
		double full = Modules.CF_FULL_HOURS.value * 3600.0;
		return (int) Math.min(100, Math.round(i.seconds * 100.0 / full));
	}

	/** Growth-based estimate: the indicator count against the threshold. Only a hint. */
	public static int estimatePercent(Info i) {
		double full = Math.max(1.0, Modules.CF_THRESHOLD.value * 2.0);
		return (int) Math.min(100, Math.round(i.total() * 100.0 / full));
	}

	/** An estimate needs the indicator count to pass the threshold. Everything below is "observed only". */
	public static boolean estimated(Info i) {
		return i.total() >= Modules.CF_THRESHOLD.value;
	}

	public static void clear() {
		DATA.clear();
		QUEUE.clear();
		QUEUED.clear();
		save();
	}

	public static void renderLabels(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.CHUNK_FINDER.isActive()) return;
		int pcx = me.blockPosition().getX() >> 4, pcz = me.blockPosition().getZ() >> 4;
		int r = Math.min(Modules.CF_RADIUS.i(), 6);
		int rgb = Modules.CF_COLOR.rgb;
		for (int cx = pcx - r; cx <= pcx + r; cx++) for (int cz = pcz - r; cz <= pcz + r; cz++) {
			Info in = DATA.get(key(cx, cz));
			if (in == null) continue;

			// amethyst count in the middle of the geode
			if (Modules.CF_AMETHYST.value && in.amethyst > 0 && Projector.project(in.ax, in.ay, in.az)) {
				label(g, mc, "Amethyst " + in.amethyst, 0xBF5AF2);
			}
			// time percentage in the middle of the chunk (your own measured time, plus the growth estimate)
			if (Modules.CF_TIME.value && (in.seconds > 20 || in.total() > 0)) {
				double wy = me.getY() + 1.5;
				if (Projector.project(cx * 16.0 + 8, wy, cz * 16.0 + 8)) {
					String t = in.seconds > 20 ? timePercent(in) + "% time (you)" : "~" + estimatePercent(in) + "% growth est.";
					label(g, mc, t, rgb);
				}
			}
		}
	}

	private static void label(GuiGraphicsExtractor g, Minecraft mc, String text, int rgb) {
		int tw = mc.font.width(text);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate((float) Projector.sx, (float) Projector.sy);
		pose.scale(0.75f, 0.75f);
		Ui.rr(g, -tw / 2 - 3, -2, tw + 6, 11, 3, Ui.a(0x000000, 0.55));
		g.text(mc.font, text, -tw / 2, 0, Ui.opaque(rgb));
		pose.popMatrix();
	}

	public static int observedChunks() {
		int n = 0;
		for (Info i : DATA.values()) if (i.total() > 0 || i.seconds > 0) n++;
		return n;
	}

	public static int estimatedChunks() {
		int n = 0;
		for (Info i : DATA.values()) if (estimated(i)) n++;
		return n;
	}

	// ---------------- rendering ----------------
	public static void renderWorld(GuiGraphicsExtractor g, LocalPlayer me) {
		if (!Modules.CHUNK_FINDER.isActive() || !Modules.CF_WORLD.value) return;
		int rgb = Modules.CF_COLOR.rgb;
		double y = me.getY() + 0.1;
		double[] seg = new double[4];
		Px.begin(g);
		int pcx = me.blockPosition().getX() >> 4, pcz = me.blockPosition().getZ() >> 4;
		int r = Math.min(Modules.CF_RADIUS.i(), 6);
		for (int cx = pcx - r; cx <= pcx + r; cx++) for (int cz = pcz - r; cz <= pcz + r; cz++) {
			Info in = DATA.get(key(cx, cz));
			if (in == null || in.total() == 0) continue;
			boolean est = estimated(in);
			// confirmed observation = thin outline, estimate = brighter outline
			int c = est ? Ui.opaque(rgb) : Ui.a(rgb, 0.35);
			double x0 = cx * 16.0, z0 = cz * 16.0, x1 = x0 + 16, z1 = z0 + 16;
			double[][] edges = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
			for (double[] e : edges) {
				if (Projector.segment(e[0], y, e[1], e[2], y, e[3], seg)) Px.line(g, seg[0], seg[1], seg[2], seg[3], c, est ? 2 : 1);
			}
		}
		Px.end(g);
	}

	public static void renderMinimap(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.CHUNK_FINDER.isActive() || !Modules.CF_MINIMAP.value) return;
		int r = Math.min(Modules.CF_RADIUS.i(), 8);
		int cell = 5;
		int size = (2 * r + 1) * cell;
		Modules.CF_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), size + 8, size + 22);
		Ui.rr(g, 0, 0, size + 8, size + 22, 4, Ui.a(0x000000, 0.55));
		g.text(mc.font, "Chunks", 4, 3, Ui.opaque(Modules.CF_COLOR.rgb));
		int pcx = me.blockPosition().getX() >> 4, pcz = me.blockPosition().getZ() >> 4;
		for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
			int x = 4 + (dx + r) * cell, y = 14 + (dz + r) * cell;
			Info in = DATA.get(key(pcx + dx, pcz + dz));
			int c = in == null ? 0x30FFFFFF : in.total() == 0 ? 0x40FFFFFF : estimated(in) ? Ui.opaque(Modules.CF_COLOR.rgb) : Ui.a(Modules.CF_COLOR.rgb, 0.4);
			g.fill(x, y, x + cell - 1, y + cell - 1, c);
		}
		int mx = 4 + r * cell + cell / 2, my = 14 + r * cell + cell / 2;
		Ui.arrow(g, mx, my, Math.toRadians(me.getYRot()), 4, 0xFFFFFFFF);
		Ui.compassLetters(g, mc.font, 4, 14, size);
		Ui.small(g, mc.font, "bright = estimated, dim = observed", 4, size + 15, 0xAAAAAA, 0.6f);
		Modules.CF_POS.end(g);
	}

	// ---------------- persistence ----------------
	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("kelpclient").resolve("chunkfinder.json");
	}

	private static void load() {
		try {
			Path p = file();
			if (!Files.exists(p)) return;
			JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			JsonArray arr = root.getAsJsonArray(worldKey);
			if (arr == null) return;
			arr.forEach(e -> {
				JsonObject o = e.getAsJsonObject();
				Info in = new Info();
				in.vines = o.get("v").getAsInt();
				in.kelp = o.get("k").getAsInt();
				in.drip = o.get("d").getAsInt();
				if (o.has("s")) in.seconds = o.get("s").getAsLong();
				if (o.has("a")) in.amethyst = o.get("a").getAsInt();
				if (o.has("ax")) { in.ax = o.get("ax").getAsDouble(); in.ay = o.get("ay").getAsDouble(); in.az = o.get("az").getAsDouble(); }
				DATA.put(key(o.get("x").getAsInt(), o.get("z").getAsInt()), in);
			});
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		if (worldKey.isEmpty()) return;
		try {
			Path p = file();
			Files.createDirectories(p.getParent());
			JsonObject root = Files.exists(p) ? JsonParser.parseString(Files.readString(p)).getAsJsonObject() : new JsonObject();
			JsonArray arr = new JsonArray();
			for (Map.Entry<Long, Info> en : DATA.entrySet()) {
				if (en.getValue().total() == 0 && en.getValue().seconds == 0 && en.getValue().amethyst == 0) continue;
				JsonObject o = new JsonObject();
				o.addProperty("x", cxOf(en.getKey()));
				o.addProperty("z", czOf(en.getKey()));
				o.addProperty("v", en.getValue().vines);
				o.addProperty("k", en.getValue().kelp);
				o.addProperty("d", en.getValue().drip);
				o.addProperty("s", en.getValue().seconds);
				o.addProperty("a", en.getValue().amethyst);
				o.addProperty("ax", en.getValue().ax);
				o.addProperty("ay", en.getValue().ay);
				o.addProperty("az", en.getValue().az);
				arr.add(o);
			}
			root.add(worldKey, arr);
			Files.writeString(p, GSON.toJson(root));
		} catch (Exception ignored) {
		}
	}
}
