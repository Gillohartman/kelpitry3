package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** RESTRICTED macro: when a crafting table opens, it fills your chosen layout and (optionally) crafts. */
public final class AutoCrafter {
	private static int lastMenuId = -1;
	private static int phase;       // 0 idle, 1 filled/wait, 2 done
	private static int wait;
	private static int crafted;
	private static int emptyChecks;

	private AutoCrafter() {}

	private static boolean matches(ItemStack st, String id) {
		if (st.isEmpty()) return false;
		String n = id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		if (n.startsWith("minecraft:")) n = n.substring(10);
		return BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals(n);
	}

	private static boolean prevKey;

	public static void tick(Minecraft mc) {
		boolean key = Keys.down(Modules.AC_KEY.key);
		boolean pressed = key && !prevKey;
		prevKey = key;
		if (!Modules.AUTOCRAFT.isActive()) { lastMenuId = -1; return; }
		LocalPlayer p = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (p == null || gm == null || !(p.containerMenu instanceof CraftingMenu menu)) { lastMenuId = -1; return; }

		if (menu.containerId != lastMenuId) {
			lastMenuId = menu.containerId;
			phase = Modules.AC_AUTO.value ? 0 : 2;
			wait = 0; crafted = 0; emptyChecks = 0;
			if (phase == 0) {
				int fields = 0;
				for (int c = 0; c < 9; c++) if (Modules.AC_ON[c].value && !Modules.AC_ITEM[c].value.isBlank()) fields++;
				Notifications.push(fields == 0 ? "Auto Crafter: no fields are set up (menu -> Utility -> Auto Crafter)." : "Auto Crafter: filling " + fields + " field(s).");
			}
		}
		if (pressed && phase == 2) {
			phase = 0; wait = 0; crafted = 0; emptyChecks = 0;
			Notifications.push("Auto Crafter: running.");
		}
		if (phase == 2) return;
		if (wait-- > 0) return;
		wait = Modules.AC_DELAY.i();

		int id = menu.containerId;
		if (phase == 0) {
			boolean any = false, missing = false;
			for (int c = 0; c < 9; c++) {
				if (!Modules.AC_ON[c].value || Modules.AC_ITEM[c].value.isBlank()) continue;
				any = true;
				int target = 1 + c;
				if (!menu.slots.get(target).getItem().isEmpty()) continue;
				int src = -1;
				for (int i = 10; i < menu.slots.size(); i++) {
					if (matches(menu.slots.get(i).getItem(), Modules.AC_ITEM[c].value)) { src = i; break; }
				}
				if (src < 0) { Notifications.push("Auto Crafter: no \"" + Modules.AC_ITEM[c].value + "\" in your inventory."); missing = true; break; }
				InventorySorter.click(gm, id, src, 0, ContainerInput.PICKUP, p);   // pick up the stack
				InventorySorter.click(gm, id, target, 1, ContainerInput.PICKUP, p); // right click: place one
				InventorySorter.click(gm, id, src, 0, ContainerInput.PICKUP, p);   // put the rest back
			}
			if (!any) { phase = 2; return; }
			if (missing) { phase = 2; return; }
			phase = 1;
			emptyChecks = 0;
			return;
		}

		// phase 1: grid is filled, take the result when the server has sent it
		if (!Modules.AC_TAKE.value) { phase = 2; return; }
		if (!menu.slots.get(0).getItem().isEmpty()) {
			InventorySorter.quickMove(gm, id, 0, p);
			crafted++;
			emptyChecks = 0;
			if (crafted >= Modules.AC_COUNT.i()) { phase = 2; Notifications.push("Auto Crafter: done (" + crafted + ")."); return; }
			phase = 0;
		} else if (++emptyChecks > 15) {
			phase = 2;
		}
	}
}
