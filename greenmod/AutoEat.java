package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** RESTRICTED: eats food from the hotbar when your hunger bar is at or below your limit. */
public final class AutoEat {
	private static boolean eating;
	private static int prevSlot = -1;
	private static int ticks;

	private AutoEat() {}

	private static boolean food(ItemStack st) {
		if (st.isEmpty() || !st.has(DataComponents.FOOD)) return false;
		String p = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
		if (p.equals("rotten_flesh") || p.equals("poisonous_potato") || p.equals("pufferfish") || p.equals("spider_eye")
				|| p.equals("suspicious_stew") || p.equals("chorus_fruit")) return false;
		return !(Modules.AE_SKIP_GOLD.value && p.contains("golden_apple"));
	}

	private static void stop(Minecraft mc, LocalPlayer p) {
		mc.options.keyUse.setDown(false);
		if (prevSlot >= 0) p.getInventory().setSelectedSlot(prevSlot);
		prevSlot = -1;
		eating = false;
		ticks = 0;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		if (!Modules.AUTO_EAT.isActive()) {
			if (eating) stop(mc, p);
			return;
		}
		if (mc.screen != null) return;

		if (eating) {
			ticks++;
			if ((ticks > 4 && !p.isUsingItem()) || p.getFoodData().getFoodLevel() >= 20 || ticks > 80) stop(mc, p);
			else mc.options.keyUse.setDown(true);
			return;
		}
		if (p.getFoodData().getFoodLevel() > Modules.AE_LEVEL.value) return;

		for (int i = 0; i < 9; i++) {
			if (!food(p.getInventory().getItem(i))) continue;
			prevSlot = p.getInventory().getSelectedSlot();
			p.getInventory().setSelectedSlot(i);
			mc.options.keyUse.setDown(true);
			eating = true;
			ticks = 0;
			return;
		}
	}
}
