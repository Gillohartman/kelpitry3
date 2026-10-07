package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adds timestamps / mention marks to chat lines and keeps a searchable history. */
public final class ChatLog {
	private static final List<String> LINES = new ArrayList<>();
	private static Component lastOut;

	private ChatLog() {}

	public static Component process(Component c) {
		if (c == null || c == lastOut || !Modules.CHAT_TOOLS.isActive()) return c;
		Minecraft mc = Minecraft.getInstance();
		String plain = c.getString();
		LocalTime now = LocalTime.now();
		String stamp = now.format(DateTimeFormatter.ofPattern(Modules.CT_24H.value ? "HH:mm" : "h:mm a"));

		LINES.add("[" + stamp + "] " + plain);
		if (LINES.size() > 600) LINES.remove(0);

		MutableComponent out = Component.empty();
		if (Modules.CT_HIGHLIGHT.value && mc.player != null) {
			String me = mc.player.getName().getString().toLowerCase(Locale.ROOT);
			if (!me.isEmpty() && plain.toLowerCase(Locale.ROOT).contains(me)) {
				int rgb = Modules.CT_HL_COLOR.rgb;
				out.append(Component.literal("> ").withStyle(s -> s.withColor(rgb).withBold(true)));
			}
		}
		if (Modules.CT_TIME.value) {
			out.append(Component.literal("[" + stamp + "] ").withStyle(s -> s.withColor(0x8A8F98)));
		}
		out.append(c);
		lastOut = out;
		return out;
	}

	public static List<String> search(String q, int max) {
		List<String> out = new ArrayList<>();
		String ql = q.toLowerCase(Locale.ROOT);
		for (int i = LINES.size() - 1; i >= 0 && out.size() < max; i--) {
			if (ql.isEmpty() || LINES.get(i).toLowerCase(Locale.ROOT).contains(ql)) out.add(LINES.get(i));
		}
		return out;
	}
}
