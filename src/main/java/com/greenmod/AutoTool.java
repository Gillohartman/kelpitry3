package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** RESTRICTED: while you mine, switches to the fastest tool in your hotbar. */
public final class AutoTool {
	private static int prevSlot = -1;

	private AutoTool() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return;
		if (!Modules.AUTO_TOOL.isActive() || mc.screen != null) {
			if (prevSlot >= 0 && p != null) { p.getInventory().setSelectedSlot(prevSlot); prevSlot = -1; }
			return;
		}
		HitResult hit = mc.hitResult;
		if (mc.options.keyAttack.isDown() && hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
			BlockState st = mc.level.getBlockState(bhr.getBlockPos());
			int sel = p.getInventory().getSelectedSlot();
			int best = sel;
			float bestSpeed = p.getInventory().getItem(sel).getDestroySpeed(st);
			for (int i = 0; i < 9; i++) {
				ItemStack s = p.getInventory().getItem(i);
				float sp = s.getDestroySpeed(st);
				if (sp > bestSpeed + 0.1f) { bestSpeed = sp; best = i; }
			}
			if (best != sel) {
				if (prevSlot < 0) prevSlot = sel;
				p.getInventory().setSelectedSlot(best);
			}
		} else if (prevSlot >= 0 && !mc.options.keyAttack.isDown()) {
			if (Modules.AT_BACK.value) p.getInventory().setSelectedSlot(prevSlot);
			prevSlot = -1;
		}
	}
}
