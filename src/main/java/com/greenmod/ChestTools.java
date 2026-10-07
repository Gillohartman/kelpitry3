package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import java.util.ArrayDeque;

/**
 * RESTRICTED: move everything out of (steal) or into (dump) the open chest with shift-clicks.
 * Items move one at a time with a short pause between them (default 3 ticks), not all in the same moment.
 */
public final class ChestTools {
	private static final ArrayDeque<Integer> QUEUE = new ArrayDeque<>();
	private static int queueMenu = -1;
	private static int wait;
	private static boolean prevSteal, prevDump;

	private ChestTools() {}

	public static boolean available(Minecraft mc) {
		AbstractContainerMenu m = mc.player == null ? null : mc.player.containerMenu;
		return Modules.CHEST_TOOLS.isActive() && (m instanceof ChestMenu || m instanceof ShulkerBoxMenu) && m.slots.size() > 36;
	}

	public static boolean busy() { return !QUEUE.isEmpty(); }

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) { QUEUE.clear(); return; }

		boolean s = Keys.down(Modules.CT_STEAL_KEY.key), d = Keys.down(Modules.CT_DUMP_KEY.key);
		if (available(mc)) {
			if (s && !prevSteal) steal(mc);
			if (d && !prevDump) dump(mc);
		}
		prevSteal = s;
		prevDump = d;

		if (QUEUE.isEmpty()) return;
		MultiPlayerGameMode gm = mc.gameMode;
		if (gm == null || !available(mc) || p.containerMenu.containerId != queueMenu) { QUEUE.clear(); return; }
		if (--wait > 0) return;
		wait = Modules.CT_DELAY.i();
		int slot = QUEUE.poll();
		if (!p.containerMenu.slots.get(slot).getItem().isEmpty()) InventorySorter.quickMove(gm, queueMenu, slot, p);
	}

	public static void steal(Minecraft mc) { fill(mc, true); }
	public static void dump(Minecraft mc) { fill(mc, false); }

	private static void fill(Minecraft mc, boolean fromChest) {
		if (!available(mc) || busy()) return;
		AbstractContainerMenu menu = mc.player.containerMenu;
		int size = menu.slots.size();
		int from = fromChest ? 0 : size - 36;
		int to = fromChest ? size - 36 : size;
		queueMenu = menu.containerId;
		for (int i = from; i < to; i++) {
			if (!menu.slots.get(i).getItem().isEmpty()) QUEUE.add(i);
		}
		wait = 1;
	}
}
