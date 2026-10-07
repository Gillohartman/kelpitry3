package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/** Shows watched players that are in your current server's player list. Uses only data the client already has. */
public final class OnlineHub {
	private static List<String> online = new ArrayList<>();
	private static int tickCounter;

	private OnlineHub() {}

	public static void tick(Minecraft mc) {
		if (!Modules.ONLINE.isActive()) return;
		if (++tickCounter % 20 != 0) return;
		List<String> result = new ArrayList<>();
		ClientPacketListener conn = mc.getConnection();
		if (conn != null && !mc.hasSingleplayerServer()) {
			Set<String> watch = new HashSet<>();
			for (String e : Modules.ONLINE_LIST.values) {
				if (Modules.ONLINE_LIST.isOn(e)) watch.add(Setting.Items.clean(e).trim().toLowerCase(Locale.ROOT));
			}
			for (PlayerInfo info : conn.getOnlinePlayers()) {
				String name = info.getProfile().name();
				if (watch.contains(name.toLowerCase(Locale.ROOT))) result.add(name);
			}
		}
		online = result;
	}

	public static void render(GuiGraphicsExtractor g, Minecraft mc) {
		if (!Modules.ONLINE.isActive()) return;
		if (online.isEmpty() && !Modules.ONLINE_EMPTY.value) return;

		int w = 40;
		for (String n : online) w = Math.max(w, mc.font.width(n));
		w += 12;
		int h = 14 + online.size() * 10 + 2;

		Modules.ONLINE_POS.begin(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), w, h);
		Ui.rr(g, 0, 0, w, h, 4, Ui.a(0x000000, 0.45));
		g.text(mc.font, "ONLINE", 6, 3, Ui.opaque(Modules.ONLINE_TITLE.rgb));
		int y = 14;
		for (String n : online) {
			g.text(mc.font, n, 6, y, Ui.opaque(Modules.ONLINE_COLOR.rgb));
			y += 10;
		}
		Modules.ONLINE_POS.end(g);
	}
}
