package com.greenmod;

import net.minecraft.client.Minecraft;

import java.util.Locale;

/** Loads a config profile automatically when you join the server (or world type) named in your rules. */
public final class ServerProfiles {
	private static String lastKey = "";
	private static int counter;

	private ServerProfiles() {}

	public static void tick(Minecraft mc) {
		if (!Modules.SERVER_PROFILES.isActive() || mc.level == null) { lastKey = ""; return; }
		if (++counter % 20 != 0) return;
		String key = Waypoints.worldKey(mc);
		if (key.equals(lastKey)) return;
		lastKey = key;

		String host = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
		boolean single = key.startsWith("sp:");
		for (String e : Modules.SP_ENTRIES.values) {
			if (!Modules.SP_ENTRIES.isOn(e)) continue;
			String clean = Setting.Items.clean(e);
			int eq = clean.indexOf('=');
			if (eq <= 0) continue;
			String h = clean.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String profile = clean.substring(eq + 1).trim();
			boolean match = single ? h.equals("singleplayer") : h.equals(host) || h.equals(host.replaceAll(":\\d+$", ""));
			if (!match || profile.isEmpty()) continue;
			if (profile.equals(Config.profile)) return;
			if (!Config.profiles().contains(profile)) {
				Notifications.push("Server profile \"" + profile + "\" does not exist.");
				return;
			}
			Config.switchProfile(profile);
			Notifications.push("Loaded profile " + profile + " for this server.");
			return;
		}
	}
}
