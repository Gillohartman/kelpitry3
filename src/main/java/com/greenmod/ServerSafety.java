package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

/**
 * Decides where we are and whether RESTRICTED / EXPERIMENTAL modules may run.
 * Default is locked. There is no override, no spoofing and no bypass here.
 */
public final class ServerSafety {
	public enum Env { SINGLEPLAYER, LAN, PRIVATE_ALLOWED_SERVER, PUBLIC_SERVER, UNKNOWN }

	public static Env current = Env.UNKNOWN;

	private ServerSafety() {}

	public static boolean allowsRestricted() {
		switch (current) {
			case SINGLEPLAYER:
			case PRIVATE_ALLOWED_SERVER:
				return true;
			case LAN:
				return Modules.ALLOW_LAN.value;
			default:
				return false;
		}
	}

	public static String lockReason() {
		switch (current) {
			case PUBLIC_SERVER: return "Disabled on public servers";
			case UNKNOWN: return "Disabled: server not verified";
			case LAN: return "Disabled on LAN (see Settings)";
			default: return "Disabled";
		}
	}

	public static String label() {
		switch (current) {
			case SINGLEPLAYER: return "Singleplayer";
			case LAN: return "LAN";
			case PRIVATE_ALLOWED_SERVER: return "Allowed server";
			case PUBLIC_SERVER: return "Public server";
			default: return "Unknown";
		}
	}

	/**
	 * Re-detects the environment and enforces locks. The lock only stops a module from running;
	 * it never changes what the user switched on, so a short "unknown" moment while joining can no longer wipe settings.
	 */
	public static void tick(Minecraft mc) {
		current = detect(mc);
		boolean allowed = allowsRestricted();
		for (Module m : Modules.ALL) {
			if (!m.restricted()) continue;
			if (!allowed) {
				if (!m.locked && m.isEnabled() && mc.level != null && current == Env.PUBLIC_SERVER) {
					Notifications.push(m.name + " is locked on this server.");
				}
				m.locked = true;
			} else if (m.locked) {
				m.locked = false;
			}
		}
	}

	private static Env detect(Minecraft mc) {
		if (mc.level == null || mc.player == null) return Env.UNKNOWN;
		if (mc.hasSingleplayerServer()) {
			IntegratedServer s = mc.getSingleplayerServer();
			return s != null && s.isPublished() ? Env.LAN : Env.SINGLEPLAYER;
		}
		ServerData sd = mc.getCurrentServer();
		if (sd == null) return Env.UNKNOWN;
		if (sd.isLan()) return Env.LAN;
		String host = hostOf(sd.ip);
		if (host.isEmpty()) return Env.UNKNOWN;
		for (String entry : Modules.ALLOWED_SERVERS.values) {
			if (!Modules.ALLOWED_SERVERS.isOn(entry)) continue;
			String e = Setting.Items.clean(entry).trim().toLowerCase(Locale.ROOT);
			if (e.equals(sd.ip.toLowerCase(Locale.ROOT)) || e.equals(host)) return Env.PRIVATE_ALLOWED_SERVER;
		}
		return Env.PUBLIC_SERVER;
	}

	private static String hostOf(String ip) {
		if (ip == null) return "";
		String s = ip.trim().toLowerCase(Locale.ROOT);
		int c = s.lastIndexOf(':');
		if (c > 0 && s.indexOf(':') == c) s = s.substring(0, c);
		return s;
	}
}
