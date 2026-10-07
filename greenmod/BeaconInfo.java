package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** Labels beacons with their pyramid level and draws the area they cover. */
public final class BeaconInfo {
	private record Beacon(BlockPos pos, int level) {}

	private static List<Beacon> beacons = new ArrayList<>();
	private static int counter;
	private static final double[] SEG = new double[4];

	private BeaconInfo() {}

	public static void tick(Minecraft mc) {
		if (!Modules.BEACON.isActive()) { beacons = new ArrayList<>(); return; }
		if (++counter % 40 != 0) return;
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (level == null || p == null) return;
		int range = Modules.BC_RANGE.i();
		int cr = (range >> 4) + 1;
		int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
		List<Beacon> out = new ArrayList<>();
		for (int cx = pcx - cr; cx <= pcx + cr; cx++) for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
			LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
			if (chunk == null) continue;
			for (BlockEntity be : chunk.getBlockEntities().values()) {
				if (!(be instanceof BeaconBlockEntity)) continue;
				BlockPos pos = be.getBlockPos();
				if (pos.distSqr(p.blockPosition()) > (double) range * range) continue;
				out.add(new Beacon(pos, pyramid(level, pos)));
			}
		}
		beacons = out;
	}

	private static int pyramid(ClientLevel level, BlockPos pos) {
		int lvl = 0;
		for (int l = 1; l <= 4; l++) {
			boolean ok = true;
			for (int x = -l; x <= l && ok; x++) for (int z = -l; z <= l && ok; z++) {
				if (!level.getBlockState(pos.offset(x, -l, z)).is(BlockTags.BEACON_BASE_BLOCKS)) ok = false;
			}
			if (!ok) break;
			lvl = l;
		}
		return lvl;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.BEACON.isActive() || beacons.isEmpty()) return;
		int rgb = Modules.BC_COLOR.rgb;
		for (Beacon b : new ArrayList<>(beacons)) {
			double reach = b.level() == 0 ? 0 : 10 + b.level() * 10;
			if (Projector.project(b.pos().getX() + 0.5, b.pos().getY() + 1.5, b.pos().getZ() + 0.5)) {
				String t = "Beacon L" + b.level() + (reach > 0 ? "  " + (int) reach + " blocks" : "  (inactive)");
				int tw = mc.font.width(t);
				var pose = g.pose();
				pose.pushMatrix();
				pose.translate((float) Projector.sx, (float) Projector.sy);
				pose.scale(0.8f, 0.8f);
				Ui.rr(g, -tw / 2 - 3, -12, tw + 6, 11, 3, Ui.a(0x000000, 0.5));
				g.text(mc.font, t, -tw / 2, -10, Ui.opaque(rgb));
				pose.popMatrix();
			}
		}
		if (!Modules.BC_OUTLINE.value) return;
		Px.begin(g);
		for (Beacon b : new ArrayList<>(beacons)) {
			double reach = b.level() == 0 ? 0 : 10 + b.level() * 10;
			if (reach <= 0) continue;
			double cx = b.pos().getX() + 0.5, cz = b.pos().getZ() + 0.5, y = b.pos().getY() + 0.5;
			double[][] e = {{cx - reach, cz - reach, cx + reach, cz - reach}, {cx + reach, cz - reach, cx + reach, cz + reach},
					{cx + reach, cz + reach, cx - reach, cz + reach}, {cx - reach, cz + reach, cx - reach, cz - reach}};
			for (double[] s : e) {
				if (Projector.segment(s[0], y, s[1], s[2], y, s[3], SEG)) Px.line(g, SEG[0], SEG[1], SEG[2], SEG[3], Ui.a(rgb, 0.7), 1);
			}
		}
		Px.end(g);
	}
}
