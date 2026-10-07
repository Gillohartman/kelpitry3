package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Low health flash, worn armor warning and totem used message. */
public final class Alerts {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final boolean[] warned = new boolean[4];
	private static int lastTotems = -1;
	private static long lastLowWarn;
	private static long flashUntil;

	private Alerts() {}

	private static void beep(LocalPlayer p, boolean low) {
		if (!Modules.AL_SOUND.value) return;
		try {
			p.playSound(low ? SoundEvents.ANVIL_LAND : SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, low ? 1.4f : 0.8f);
		} catch (Throwable ignored) {
		}
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || !Modules.ALERTS.isActive()) return;
		long now = System.currentTimeMillis();

		if (p.getHealth() > 0 && p.getHealth() <= Modules.AL_HEALTH.value * 2 && now - lastLowWarn > 4000) {
			lastLowWarn = now;
			flashUntil = now + 900;
			beep(p, true);
		}

		for (int i = 0; i < 4; i++) {
			ItemStack st = p.getItemBySlot(ARMOR[i]);
			if (st.isEmpty() || !st.isDamageableItem() || st.getMaxDamage() <= 0) { warned[i] = false; continue; }
			double pct = (st.getMaxDamage() - st.getDamageValue()) * 100.0 / st.getMaxDamage();
			if (pct <= Modules.AL_ARMOR.value && !warned[i]) {
				warned[i] = true;
				Notifications.push(st.getHoverName().getString() + " is almost broken!");
				beep(p, true);
				flashUntil = now + 700;
			} else if (pct > Modules.AL_ARMOR.value + 5) warned[i] = false;
		}

		int totems = TotemFeatures.count(p);
		if (lastTotems >= 0 && totems < lastTotems && Modules.AL_TOTEM.value) {
			Notifications.push("Totem used! " + totems + " left.");
			beep(p, false);
			flashUntil = now + 600;
		}
		lastTotems = totems;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.ALERTS.isActive() || !Modules.AL_FLASH.value) return;
		long left = flashUntil - System.currentTimeMillis();
		if (left <= 0) return;
		double a = Math.min(1.0, left / 500.0) * (0.35 + 0.15 * Math.sin(System.currentTimeMillis() / 90.0));
		int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
		int rgb = Modules.AL_COLOR.rgb;
		int t = 10;
		for (int i = 0; i < t; i++) {
			int c = Ui.a(rgb, a * (1.0 - i / (double) t));
			g.fill(0, i, w, i + 1, c);
			g.fill(0, h - i - 1, w, h - i, c);
			g.fill(i, 0, i + 1, h, c);
			g.fill(w - i - 1, 0, w - i, h, c);
		}
	}
}
