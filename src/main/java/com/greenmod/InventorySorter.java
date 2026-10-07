package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Locale;

/** Sorts the player inventory (and chests), merges stacks, refills the hotbar. */
public final class InventorySorter {
	private static Method clickMethod;
	private static String lastError = "";
	private static boolean clickFailed;

	private static final String[] TYPE_ORDER = {
			"sword", "pickaxe", "_axe", "shovel", "hoe", "bow", "crossbow", "trident", "shield",
			"helmet", "chestplate", "leggings", "boots", "elytra", "totem", "golden_apple"
	};

	private InventorySorter() {}

	// ---------------- ordering ----------------
	private static String path(ItemStack s) {
		return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
	}

	private static String id(ItemStack s) {
		return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
	}

	private static int typeRank(ItemStack s) {
		String p = path(s);
		for (int i = 0; i < TYPE_ORDER.length; i++) if (p.contains(TYPE_ORDER[i])) return i;
		return TYPE_ORDER.length + 1;
	}

	private static int priorityRank(ItemStack s) {
		String p = path(s);
		int i = 0;
		for (String e : Modules.SORT_PRIORITY.values) {
			String w = e.trim().toLowerCase(Locale.ROOT);
			if (!w.isEmpty() && p.contains(w)) return i;
			i++;
		}
		return 1000;
	}

	private static final Comparator<ItemStack> ORDER = (a, b) -> {
		if (a.isEmpty() && b.isEmpty()) return 0;
		if (a.isEmpty()) return 1;
		if (b.isEmpty()) return -1;
		int c;
		switch (Modules.SORT_MODE.index) {
			case 1:
				c = a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
				break;
			case 2:
				c = Integer.compare(b.getCount(), a.getCount());
				break;
			case 3:
				c = Integer.compare(priorityRank(a), priorityRank(b));
				break;
			default:
				c = Integer.compare(typeRank(a), typeRank(b));
				break;
		}
		if (c == 0) c = id(a).compareTo(id(b));
		if (c == 0) c = Integer.compare(b.getCount(), a.getCount());
		return c;
	};

	// ---------------- sort ----------------
	public static void sort() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (player == null || gm == null) return;
		AbstractContainerMenu menu = player.containerMenu;
		if (!menu.getCarried().isEmpty()) {
			Notifications.push("Put down the item on your cursor first.");
			return;
		}

		clickFailed = false;
		lastError = "";
		int id = menu.containerId;
		int size = menu.slots.size();

		if (menu instanceof InventoryMenu) {
			sortRange(gm, player, id, 9, 35);
		} else if (size >= 36) {
			sortRange(gm, player, id, size - 36, size - 10);
			if (Modules.SORT_CHEST.value && size > 36 && (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) {
				sortRange(gm, player, id, 0, size - 37);
			}
		}

		if (clickFailed) Notifications.push("Sorting failed: " + lastError);
		else Notifications.push("Inventory sorted.");
	}

	private static void sortRange(MultiPlayerGameMode gm, LocalPlayer p, int id, int first, int last) {
		if (Modules.SORT_MERGE.value) {
			for (int i = first; i <= last; i++) {
				for (int j = i + 1; j <= last; j++) {
					ItemStack a = slot(p, i);
					ItemStack b = slot(p, j);
					if (a.isEmpty() || b.isEmpty()) continue;
					if (!ItemStack.isSameItemSameComponents(a, b)) continue;
					if (a.getCount() >= a.getMaxStackSize()) break;
					click(gm, id, j, p);
					click(gm, id, i, p);
					if (!p.containerMenu.getCarried().isEmpty()) click(gm, id, j, p);
				}
			}
		}

		for (int i = first; i <= last; i++) {
			int best = i;
			for (int j = i + 1; j <= last; j++) {
				if (ORDER.compare(slot(p, j), slot(p, best)) < 0) best = j;
			}
			if (best != i && ORDER.compare(slot(p, best), slot(p, i)) != 0) {
				click(gm, id, best, p);
				click(gm, id, i, p);
				if (!p.containerMenu.getCarried().isEmpty()) click(gm, id, best, p);
			}
		}
	}

	// ---------------- hotbar refill ----------------
	private static ItemStack lastHeld = ItemStack.EMPTY;
	private static int lastSlot = -1;

	public static void refillTick(Minecraft mc) {
		LocalPlayer p = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (p == null || gm == null || mc.screen != null) {
			lastHeld = ItemStack.EMPTY;
			lastSlot = -1;
			return;
		}
		int sel = p.getInventory().getSelectedSlot();
		ItemStack cur = p.getInventory().getItem(sel);

		if (sel == lastSlot && cur.isEmpty() && !lastHeld.isEmpty() && sel >= 0 && sel < 9 && Modules.REFILL_SLOTS[sel].value) {
			clickFailed = false;
			for (int i = 9; i <= 35; i++) {
				ItemStack s = slot(p, i);
				if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, lastHeld)) {
					int id = p.inventoryMenu.containerId;
					click(gm, id, i, p);
					click(gm, id, 36 + sel, p);
					if (!p.containerMenu.getCarried().isEmpty()) click(gm, id, i, p);
					break;
				}
			}
			lastHeld = ItemStack.EMPTY;
			return;
		}
		lastSlot = sel;
		lastHeld = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();
	}

	private static ItemStack slot(LocalPlayer p, int index) {
		return p.containerMenu.slots.get(index).getItem();
	}

	/** Left click (pickup). */
	public static void click(MultiPlayerGameMode gm, int containerId, int slot, LocalPlayer p) {
		click(gm, containerId, slot, 0, ContainerInput.PICKUP, p);
	}

	/** Shift click. */
	public static void quickMove(MultiPlayerGameMode gm, int containerId, int slot, LocalPlayer p) {
		ContainerInput qm;
		try {
			qm = ContainerInput.valueOf("QUICK_MOVE");
		} catch (Throwable t) {
			clickFailed = true;
			lastError = "no QUICK_MOVE";
			return;
		}
		click(gm, containerId, slot, 0, qm, p);
	}

	/** Finds the container click method by its parameter types, so it works whatever it is called. */
	public static void click(MultiPlayerGameMode gm, int containerId, int slot, int button, ContainerInput type, LocalPlayer p) {
		try {
			if (clickMethod == null) {
				for (Method m : MultiPlayerGameMode.class.getDeclaredMethods()) {
					Class<?>[] t = m.getParameterTypes();
					if (t.length == 5 && t[0] == int.class && t[1] == int.class && t[2] == int.class
							&& t[3] == ContainerInput.class && Player.class.isAssignableFrom(t[4])) {
						m.setAccessible(true);
						clickMethod = m;
						break;
					}
				}
			}
			if (clickMethod == null) {
				clickFailed = true;
				lastError = "click method not found";
				return;
			}
			clickMethod.invoke(gm, containerId, slot, button, type, p);
		} catch (Throwable e) {
			clickFailed = true;
			Throwable c = e.getCause() != null ? e.getCause() : e;
			lastError = c.getClass().getSimpleName();
		}
	}
}
