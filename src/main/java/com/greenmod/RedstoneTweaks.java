package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Shows the signal strength number above redstone dust. */
public final class RedstoneTweaks {
	private record Dust(BlockPos pos, int power) {}

	private static List<Dust> dust = new ArrayList<>();
	private static int counter;

	private RedstoneTweaks() {}

	public static void tick(Minecraft mc) {
		if (!Modules.REDSTONE.isActive()) { dust = new ArrayList<>(); return; }
		if (++counter % 5 != 0) return;
		ClientLevel level = mc.level;
		LocalPlayer p = mc.player;
		if (level == null || p == null) return;
		int r = Modules.RS_RANGE.i();
		BlockPos c = p.blockPosition();
		List<Dust> out = new ArrayList<>();
		for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
			BlockPos pos = c.offset(x, y, z);
			BlockState st = level.getBlockState(pos);
			if (!(st.getBlock() instanceof RedStoneWireBlock)) continue;
			int power = st.getValue(RedStoneWireBlock.POWER);
			if (power == 0 && !Modules.RS_ZERO.value) continue;
			out.add(new Dust(pos, power));
			if (out.size() > 200) { dust = out; return; }
		}
		dust = out;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.REDSTONE.isActive() || dust.isEmpty()) return;
		float s = Modules.RS_SCALE.f();
		for (Dust d : new ArrayList<>(dust)) {
			if (!Projector.project(d.pos().getX() + 0.5, d.pos().getY() + 0.4, d.pos().getZ() + 0.5)) continue;
			String t = String.valueOf(d.power());
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate((float) Projector.sx - mc.font.width(t) * s / 2f, (float) Projector.sy - 4 * s);
			pose.scale(s, s);
			int c = Ui.mix(0x552222, Modules.RS_COLOR.rgb, d.power() / 15.0);
			g.text(mc.font, t, 0, 0, Ui.opaque(c));
			pose.popMatrix();
		}
	}
}
