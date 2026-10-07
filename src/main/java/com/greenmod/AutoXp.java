package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** RESTRICTED: throws XP bottles downward to repair worn gear. */
public final class AutoXp {
	private static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static int cooldown;

	private AutoXp() {}

	public static void tick(Minecraft mc) {
		if (!Modules.AUTO_XP.isActive()) return;
		LocalPlayer p = mc.player;
		if (p == null || mc.gameMode == null || mc.screen != null) return;
		if (cooldown-- > 0) return;

		boolean worn = false;
		for (EquipmentSlot s : GEAR) {
			ItemStack st = p.getItemBySlot(s);
			if (st.isEmpty() || !st.isDamaged() || st.getMaxDamage() <= 0) continue;
			if (st.getDamageValue() * 100.0 / st.getMaxDamage() >= Modules.AX_WEAR.value) { worn = true; break; }
		}
		if (!worn) return;

		int slot = -1;
		for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) { slot = i; break; }
		if (slot < 0) return;

		int prevSlot = p.getInventory().getSelectedSlot();
		float prevPitch = p.getXRot();
		p.getInventory().setSelectedSlot(slot);
		p.setXRot(90f);
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		p.setXRot(prevPitch);
		p.getInventory().setSelectedSlot(prevSlot);
		cooldown = Modules.AX_DELAY.i();
	}
}
