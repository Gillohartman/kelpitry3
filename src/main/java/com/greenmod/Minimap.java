package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.List;

/** Small north-up map colored with the same colors vanilla maps use. */
public final class Minimap {
	private static int[] pix = new int[0];
	private static int n;
	private static int counter;

	private Minimap() {}

	public static void tick(Minecraft mc) {
		if (!Modules.MINIMAP.isActive()) return;
		if (++counter % 8 != 0) return;
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (level == null || p == null) return;

		int r = Modules.MM_RADIUS.i();
		n = r * 2;
		int[] out = new int[n * n];
		int px = (int) Math.floor(p.getX()), py = (int) Math.floor(p.getY()), pz = (int) Math.floor(p.getZ());
		boolean ceiling = level.dimensionType().hasCeiling();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int x = px - r + i, z = pz - r + j;
				if (!level.hasChunk(x >> 4, z >> 4)) { out[j * n + i] = 0; continue; }
				int y;
				if (ceiling) {
					y = py;
					for (int k = 0; k < 24 && y > level.getMinY(); k++, y--) {
						if (!level.getBlockState(pos.set(x, y, z)).isAir()) break;
					}
				} else {
					y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				}
				BlockState st = level.getBlockState(pos.set(x, y, z));
				MapColor mc2 = st.getMapColor(level, pos);
				if (mc2 == MapColor.NONE) { out[j * n + i] = 0; continue; }
				double shade = Math.max(0.7, Math.min(1.25, 1.0 + (y - py) * 0.015));
				int c = mc2.col;
				int rr = Math.min(255, (int) (((c >> 16) & 255) * shade));
				int gg = Math.min(255, (int) (((c >> 8) & 255) * shade));
				int bb = Math.min(255, (int) ((c & 255) * shade));
				out[j * n + i] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
			}
		}
		pix = out;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.MINIMAP.isActive() || n == 0 || pix.length != n * n) return;
		int size = Modules.MM_SIZE.i();
		float cell = size / (float) n;
		Modules.MM_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), size + 6, size + 6);
		Ui.rr(g, 0, 0, size + 6, size + 6, 5, Ui.a(0xFFFFFF, 0.35));
		Ui.rr(g, 1, 1, size + 4, size + 4, 4, 0xFF0B0D10);

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(3f, 3f);
		pose.scale(cell, cell);
		for (int row = 0; row < n; row++) {
			int start = 0;
			int cur = pix[row * n];
			for (int col = 1; col <= n; col++) {
				int c = col < n ? pix[row * n + col] : cur ^ 1;
				if (c != cur) {
					if (cur != 0) g.fill(start, row, col, row + 1, cur);
					start = col;
					cur = c;
				}
			}
		}
		pose.popMatrix();

		// you: an arrow that points where you look, plus N / E / S / W on the edges
		int cx = 3 + size / 2, cy = 3 + size / 2;
		Ui.arrow(g, cx, cy, Math.toRadians(me.getYRot()), 4, 0xFFFFFFFF);
		Ui.compassLetters(g, mc.font, 3, 3, size);

		if (Modules.MM_WAYPOINTS.value && Modules.WAYPOINTS.isActive() && !StreamerMode.hideWaypoints()) {
			List<Waypoints.Waypoint> list = Waypoints.forCurrent(mc);
			String dim = Waypoints.currentDim(mc);
			for (Waypoints.Waypoint w : list) {
				if (!w.visible || !dim.equals(w.dim)) continue;
				int wx = cx + Math.round((float) ((w.x + 0.5 - me.getX()) * cell));
				int wy = cy + Math.round((float) ((w.z + 0.5 - me.getZ()) * cell));
				if (wx < 4 || wy < 4 || wx > size + 1 || wy > size + 1) continue;
				g.fill(wx - 1, wy - 1, wx + 2, wy + 2, Ui.opaque(w.rgb));
			}
		}
		Modules.MM_POS.end(g);
	}
}
