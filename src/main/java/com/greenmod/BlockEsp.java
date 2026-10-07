package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** RESTRICTED: highlights chosen blocks, each in its own color. Scans loaded chunk sections once per second. */
public final class BlockEsp {
	private record Found(BlockPos pos, int rgb, String id) {}

	private static final int MAX_RESULTS = 600;
	private static final double[] SEGS = new double[48];
	private static final double[] RECT = new double[4];

	private static List<Found> found = new ArrayList<>();
	private static Map<Block, int[]> targets = new HashMap<>();   // block -> {rgb}
	private static String targetsKey = "";
	private static int counter;

	private BlockEsp() {}

	private static void rebuildTargets() {
		StringBuilder key = new StringBuilder();
		for (Setting.BlockColors.Entry e : Modules.BE_BLOCKS.values) key.append(e.id).append(e.rgb).append(e.on).append(',');
		if (key.toString().equals(targetsKey)) return;
		targetsKey = key.toString();

		Map<String, Integer> colors = new HashMap<>();
		for (Setting.BlockColors.Entry e : Modules.BE_BLOCKS.values) {
			if (!e.on) continue;
			String n = e.id.trim().toLowerCase(Locale.ROOT);
			if (n.startsWith("minecraft:")) n = n.substring(10);
			if (!n.isEmpty()) colors.put(n, e.rgb);
		}
		Map<Block, int[]> map = new HashMap<>();
		for (Block b : BuiltInRegistries.BLOCK) {
			Integer c = colors.get(BuiltInRegistries.BLOCK.getKey(b).getPath());
			if (c != null) map.put(b, new int[] {c});
		}
		targets = map;
	}

	public static void tick(Minecraft mc) {
		if (!Modules.BLOCK_ESP.isActive()) {
			found = new ArrayList<>();
			return;
		}
		if (++counter % 20 != 0) return;
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (level == null || p == null) return;
		rebuildTargets();
		if (targets.isEmpty()) { found = new ArrayList<>(); return; }

		int range = Modules.BE_RANGE.i();
		int cr = (range >> 4) + 1;
		int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
		List<Found> out = new ArrayList<>();
		double r2 = (double) range * range;

		outer:
		for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
			for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				LevelChunkSection[] sections = chunk.getSections();
				for (int si = 0; si < sections.length; si++) {
					LevelChunkSection sec = sections[si];
					if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(st -> targets.containsKey(st.getBlock()))) continue;
					int baseY = (chunk.getMinSectionY() + si) << 4;
					for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
						BlockState st = sec.getBlockState(x, y, z);
						int[] c = targets.get(st.getBlock());
						if (c == null) continue;
						BlockPos pos = new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z);
						if (p.blockPosition().distSqr(pos) > r2) continue;
						out.add(new Found(pos, c[0], BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath()));
						if (out.size() >= MAX_RESULTS) break outer;
					}
				}
			}
		}
		found = out;
	}

	private static double[] origin() {
		switch (Modules.BE_ORIGIN.index) {
			case 0: case 1: case 2: return Tracers.origin(Modules.BE_ORIGIN.index);
			default: return new double[] {Projector.w * Modules.BE_ANCHOR_X.value / 100.0, Projector.h * Modules.BE_ANCHOR_Y.value / 100.0};
		}
	}

	public static void render(GuiGraphicsExtractor g, LocalPlayer me) {
		if (!Modules.BLOCK_ESP.isActive() || found.isEmpty()) return;
		int t = Modules.BE_LINE.i();
		double fill = Modules.BE_OPACITY.value / 100.0;
		double[] o = origin();
		boolean stub = Modules.BE_TRACER_STYLE.index == 1;
		List<Found> list = new ArrayList<>(found);

		Px.begin(g);
		for (Found f : list) {
			AABB bb = new AABB(f.pos());
			int rgb = f.rgb();
			boolean visible = Projector.rect(bb, 0, 0, 0, RECT, SEGS);
			if (visible) {
				if (fill > 0) Px.rectFill(g, RECT[0], RECT[1], RECT[2], RECT[3], Ui.a(rgb, fill));
				if (Modules.BE_OUTLINE.value) {
					int n = Projector.box(bb, 0, 0, 0, SEGS);
					for (int i = 0; i < n; i++) Px.line(g, SEGS[i * 4], SEGS[i * 4 + 1], SEGS[i * 4 + 2], SEGS[i * 4 + 3], Ui.opaque(rgb), t);
				}
			}
			if (Modules.BE_TRACERS.value) {
				Tracers.draw(g, o[0], o[1], f.pos().getX() + 0.5, f.pos().getY() + 0.5, f.pos().getZ() + 0.5,
						Ui.a(rgb, 0.9), t, stub, Modules.BE_STUB_LEN.value, Modules.BE_EDGE.value);
			}
		}
		Px.end(g);
	}

	public static void renderLegend(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.BLOCK_ESP.isActive() || !Modules.BE_LEGEND.value || found.isEmpty()) return;
		Map<String, int[]> counts = new TreeMap<>();
		for (Found f : new ArrayList<>(found)) counts.computeIfAbsent(f.id(), k -> new int[] {0, f.rgb()})[0]++;

		int w = 60;
		for (Map.Entry<String, int[]> e : counts.entrySet()) w = Math.max(w, mc.font.width(pretty(e.getKey()) + " " + e.getValue()[0]) + 18);
		int h = counts.size() * 10 + 14;
		Modules.BE_LEGEND_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.5));
		g.text(mc.font, "Block ESP", 4, 3, Ui.opaque(Ui.accent()));
		int y = 14;
		for (Map.Entry<String, int[]> e : counts.entrySet()) {
			g.fill(4, y + 1, 10, y + 7, Ui.opaque(e.getValue()[1]));
			g.text(mc.font, pretty(e.getKey()) + " " + e.getValue()[0], 14, y, 0xFFFFFFFF);
			y += 10;
		}
		Modules.BE_LEGEND_POS.end(g);
	}

	private static String pretty(String id) {
		String s = id.replace('_', ' ');
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
