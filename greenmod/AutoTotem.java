package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** RESTRICTED: keeps a totem in your offhand. Runs only while the module is active (never on public servers). */
public final class AutoTotem {
	private static int cooldown;

	private AutoTotem() {}

	public static void tick(Minecraft mc) {
		if (!Modules.AUTO_TOTEM.isActive()) return;
		LocalPlayer p = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (p == null || gm == null || mc.screen != null) return;
		if (p.containerMenu != p.inventoryMenu || !p.containerMenu.getCarried().isEmpty()) return;
		if (cooldown-- > 0) return;

		if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		double threshold = Modules.AT_HEALTH.value;
		if (threshold > 0 && p.getHealth() > threshold) return;

		int source = -1;
		int pref = Modules.AT_SLOT.i();
		if (pref > 0) {
			int idx = menuSlotFor(pref - 1);
			if (isTotem(p, idx)) source = idx;
		}
		if (source < 0) {
			for (int i = 9; i <= 44; i++) {
				if (isTotem(p, i)) { source = i; break; }
			}
		}
		if (source < 0) return;

		int id = p.inventoryMenu.containerId;
		InventorySorter.click(gm, id, source, p);
		InventorySorter.click(gm, id, 45, p);
		if (!p.containerMenu.getCarried().isEmpty()) InventorySorter.click(gm, id, source, p);
		cooldown = Modules.AT_DELAY.i();
	}

	/** Preferred slot 1..36: 1-9 hotbar, 10-36 inventory rows. */
	private static int menuSlotFor(int zeroBased) {
		return zeroBased < 9 ? 36 + zeroBased : zeroBased;
	}

	private static boolean isTotem(LocalPlayer p, int menuIndex) {
		if (menuIndex < 9 || menuIndex > 44) return false;
		ItemStack s = p.inventoryMenu.slots.get(menuIndex).getItem();
		return s.is(Items.TOTEM_OF_UNDYING);
	}
}
