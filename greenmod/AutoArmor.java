package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** RESTRICTED: puts on the best armor pieces from your inventory. */
public final class AutoArmor {
	private static final String[] TYPES = {"_helmet", "_chestplate", "_leggings", "_boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static int cooldown;

	private AutoArmor() {}

	private static int tier(String path) {
		if (path.startsWith("netherite_")) return 6;
		if (path.startsWith("diamond_")) return 5;
		if (path.startsWith("iron_")) return 4;
		if (path.startsWith("chainmail_")) return 3;
		if (path.startsWith("golden_") || path.startsWith("turtle_")) return 2;
		if (path.startsWith("leather_")) return 1;
		return 0;
	}

	/** Higher is better; -1 = not a piece of this type. */
	private static double score(ItemStack st, int type) {
		if (st.isEmpty()) return -1;
		String path = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
		if (!path.endsWith(TYPES[type])) return -1;
		double dur = st.isDamageableItem() && st.getMaxDamage() > 0 ? (st.getMaxDamage() - st.getDamageValue()) / (double) st.getMaxDamage() : 1.0;
		return tier(path) * 100 + dur * 50;
	}

	public static void tick(Minecraft mc) {
		if (!Modules.AUTO_ARMOR.isActive()) return;
		LocalPlayer p = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (p == null || gm == null || mc.screen != null) return;
		if (p.containerMenu != p.inventoryMenu || !p.containerMenu.getCarried().isEmpty()) return;
		if (cooldown-- > 0) return;

		for (int type = 0; type < 4; type++) {
			double current = score(p.getItemBySlot(SLOTS[type]), type);
			int best = -1;
			double bestScore = current;
			for (int i = 9; i <= 44; i++) {
				double sc = score(p.inventoryMenu.slots.get(i).getItem(), type);
				if (sc > bestScore) { bestScore = sc; best = i; }
			}
			if (best < 0) continue;
			int id = p.inventoryMenu.containerId;
			int armorSlot = 5 + type;
			InventorySorter.click(gm, id, best, p);
			InventorySorter.click(gm, id, armorSlot, p);
			if (!p.containerMenu.getCarried().isEmpty()) InventorySorter.click(gm, id, best, p);
			cooldown = Modules.AA_DELAY.i();
			return;
		}
	}
}
