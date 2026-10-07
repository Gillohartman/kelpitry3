package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Small custom nametags for players, mobs and dropped items. Vanilla tags are hidden by a mixin. */
public final class Nametags {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private Nametags() {}

	public static void render(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me, List<Entity> list) {
		boolean p = Modules.PLAYER_TAGS.isActive(), m = Modules.MOB_TAGS.isActive(), i = Modules.ITEM_TAGS.isActive();
		if (!p && !m && !i) return;

		List<Entity> mobs = new ArrayList<>();
		List<Entity> items = new ArrayList<>();

		// On servers where restricted modules are locked, tags only show for entities you can actually see.
		final boolean open = ServerSafety.allowsRestricted();

		for (Entity e : list) {
			if (!open && !me.hasLineOfSight(e)) continue;
			if (e instanceof Player pl) {
				if (p && !pl.isSpectator()) player(g, mc, me, pl, open);
			} else if (e instanceof ItemEntity) {
				if (i) items.add(e);
			} else if (e instanceof LivingEntity && !(e instanceof ArmorStand)) {
				if (m) mobs.add(e);
			}
		}

		if (p && Projector.thirdPerson && Modules.PT_SELF.value) player(g, mc, me, me, open);

		if (m) {
			double r = Modules.MT_RANGE.value;
			mobs.removeIf(e -> me.distanceToSqr(e) > r * r);
			mobs.sort(Comparator.comparingDouble(me::distanceToSqr));
			int max = Modules.MT_MAX.i();
			for (int k = 0; k < mobs.size() && k < max; k++) mob(g, mc, me, (LivingEntity) mobs.get(k));
		}
		if (i) {
			double r = Modules.IT_RANGE.value;
			items.removeIf(e -> me.distanceToSqr(e) > r * r);
			items.sort(Comparator.comparingDouble(me::distanceToSqr));
			int max = Modules.IT_MAX.i();
			for (int k = 0; k < items.size() && k < max; k++) item(g, mc, me, (ItemEntity) items.get(k));
		}
	}

	private static void player(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me, Player pl, boolean open) {
		double dist = Math.sqrt(me.distanceToSqr(pl));
		if (pl != me && dist > Modules.PT_RANGE.value) return;

		String shown = (pl == me && Modules.FAKE_NAME.isActive() && !Modules.FN_NAME.value.isBlank()) ? Modules.FN_NAME.value : pl.getName().getString();
		StringBuilder sb = new StringBuilder(shown);
		if (Modules.PT_HEALTH.value) sb.append(" ").append((int) Math.ceil(pl.getHealth()));
		if (Modules.PT_DIST.value && pl != me) sb.append(" ").append((int) dist).append("m");

		List<ItemStack> icons = new ArrayList<>();
		if (open && Modules.PT_ARMOR.value) {
			for (EquipmentSlot s : ARMOR) {
				ItemStack st = pl.getItemBySlot(s);
				if (!st.isEmpty()) icons.add(st);
			}
		}
		if (open && Modules.PT_HELD.value) {
			ItemStack st = pl.getMainHandItem();
			if (!st.isEmpty()) icons.add(st);
		}

		boolean badge = Modules.PT_KELP.value && GreenMod.isModUser(pl);
		String ping = null;
		if (Modules.PT_PING.value && pl != me) {
			int ms = PingTracker.forPlayer(mc, pl.getUUID());
			if (ms > 0) ping = ms + " ms";
		}
		tag(g, mc, pl, sb.toString(), Modules.PT_COLOR.rgb, Modules.PT_SCALE.value, dist,
				Modules.PT_BG.value, Modules.PT_BG_OP.value / 100.0, badge, icons, ping);
	}

	private static void mob(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me, LivingEntity e) {
		double dist = Math.sqrt(me.distanceToSqr(e));
		String name = e.getName().getString();
		if (Modules.MT_HEALTH.value) name += " " + (int) Math.ceil(e.getHealth());
		tag(g, mc, e, name, Modules.MT_COLOR.rgb, Modules.MT_SCALE.value, dist,
				Modules.MT_BG.value, Modules.MT_BG_OP.value / 100.0, false, List.of(), null);
	}

	private static void item(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer me, ItemEntity e) {
		double dist = Math.sqrt(me.distanceToSqr(e));
		ItemStack st = e.getItem();
		String name = st.getHoverName().getString();
		if (Modules.IT_COUNT.value && st.getCount() > 1) name += " x" + st.getCount();
		tag(g, mc, e, name, Modules.IT_COLOR.rgb, Modules.IT_SCALE.value, dist,
				Modules.IT_BG.value, Modules.IT_BG_OP.value / 100.0, false, List.of(), null);
	}

	private static void tag(GuiGraphicsExtractor g, Minecraft mc, Entity e, String text, int rgb, double baseScale,
							double dist, boolean bg, double bgOp, boolean badge, List<ItemStack> icons, String ping) {
		float pt = Projector.pt;
		double x = Mth.lerp(pt, e.xOld, e.getX());
		double y = Mth.lerp(pt, e.yOld, e.getY());
		double z = Mth.lerp(pt, e.zOld, e.getZ());
		if (!Projector.project(x, y + e.getBbHeight() + 0.3, z)) return;

		float scale = (float) (baseScale * Mth.clamp(8.0 / Math.max(dist, 1.0), 0.6, 1.2));
		int tw = mc.font.width(text);
		int pw = ping == null ? 0 : (int) (mc.font.width("  \u2022  " + ping) * 0.72f);
		int bw = tw + pw + (badge ? 10 : 0);
		int iconRow = icons.isEmpty() ? 0 : 10;
		int iw = icons.size() * 9;
		int fullW = Math.max(bw, iw);

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate((float) Projector.sx, (float) Projector.sy);
		pose.scale(scale, scale);

		int left = -fullW / 2;
		if (bg) Ui.rr(g, left - 3, -10 - iconRow, fullW + 6, 11 + iconRow, 3, Ui.a(0x000000, bgOp));

		int tx = -bw / 2;
		if (badge) kelpBlock(g, tx, -9);
		g.text(mc.font, text, tx + (badge ? 10 : 0), -9, Ui.opaque(rgb));
		if (ping != null) Ui.small(g, mc.font, "  \u2022  " + ping, tx + (badge ? 10 : 0) + tw, -7, Modules.PT_PING_COLOR.rgb, 0.72f);

		if (!icons.isEmpty()) {
			int ix = -iw / 2;
			for (ItemStack st : icons) {
				pose.pushMatrix();
				pose.translate((float) ix, -9f - 9f);
				pose.scale(0.5f, 0.5f);
				g.item(st, 0, 0);
				pose.popMatrix();
				ix += 9;
			}
		}
		pose.popMatrix();
	}

	/** Small white sphere that marks other Ruined Client users. */
	private static void kelpBlock(GuiGraphicsExtractor g, int x, int y) {
		Ui.orb(g, x, y, 8);
	}
}
