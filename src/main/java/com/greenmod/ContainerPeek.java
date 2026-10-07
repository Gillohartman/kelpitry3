package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * RESTRICTED + singleplayer only: with Freecam on, shows the contents of the container you look at.
 * It reads the integrated server's own data. On multiplayer servers there is nothing to read, so it does nothing.
 */
public final class ContainerPeek {
	private ContainerPeek() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me) {
		if (!Modules.CONTAINER_PEEK.isActive() || !FreeView.freecam()) return;
		if (!mc.hasSingleplayerServer() || mc.getSingleplayerServer() == null || mc.level == null) return;

		float pt = Projector.pt;
		Vec3 from = FreeView.cameraPos(pt);
		double yr = Math.toRadians(FreeView.yaw), pr = Math.toRadians(FreeView.pitch);
		Vec3 dir = new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
		Vec3 to = from.add(dir.scale(Modules.PEEK_RANGE.value));

		BlockHitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, me));
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

		ServerLevel sl = mc.getSingleplayerServer().getLevel(mc.level.dimension());
		if (sl == null) return;
		var pos = hit.getBlockPos();
		BlockState state = sl.getBlockState(pos);
		Container c = null;
		if (state.getBlock() instanceof ChestBlock cb) {
			c = ChestBlock.getContainer(cb, state, sl, pos, true);
		} else {
			BlockEntity be = sl.getBlockEntity(pos);
			if (be instanceof Container cc) c = cc;
		}
		if (c == null) return;

		int size = Math.min(54, c.getContainerSize());
		int rows = (size + 8) / 9;
		int w = 9 * 18 + 10, h = rows * 18 + 22;
		Modules.PEEK_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 6, Ui.a(0x101214, 0.92));
		g.text(mc.font, "Container (peek)", 6, 6, Ui.opaque(Ui.accent()));
		for (int i = 0; i < size; i++) {
			int ix = 5 + (i % 9) * 18, iy = 18 + (i / 9) * 18;
			Ui.rr(g, ix, iy, 17, 17, 2, Ui.a(0x000000, 0.4));
			ItemStack it = c.getItem(i);
			if (it.isEmpty()) continue;
			g.item(it, ix + 1, iy + 1);
			g.itemDecorations(mc.font, it, ix + 1, iy + 1);
		}
		Modules.PEEK_POS.end(g);
	}
}
