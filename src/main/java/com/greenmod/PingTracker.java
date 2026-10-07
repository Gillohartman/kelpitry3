package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Real server-reported latency, smoothed. Never shows a fake "0 ms". */
public final class PingTracker {
	private static final ArrayDeque<Integer> SAMPLES = new ArrayDeque<>();
	private static long lastSample;
	private static final Map<UUID, long[]> PLAYERS = new HashMap<>();   // uuid -> {latency, time}

	private PingTracker() {}

	/** Text for your own ping: "42 ms", "SP" (singleplayer) or "-" (not known yet). */
	public static String mine(Minecraft mc) {
		ClientPacketListener conn = mc.getConnection();
		if (conn == null || mc.player == null) return "-";
		if (mc.hasSingleplayerServer()) return "SP";
		PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
		if (info == null) return "-";
		long now = System.currentTimeMillis();
		if (now - lastSample >= Modules.INFO_PING_MS.value) {
			lastSample = now;
			int lat = info.getLatency();
			if (lat > 0) {
				SAMPLES.addLast(lat);
				while (SAMPLES.size() > 4) SAMPLES.removeFirst();
			}
		}
		if (SAMPLES.isEmpty()) return "-";
		double sum = 0;
		for (int v : SAMPLES) sum += v;
		return Math.round(sum / SAMPLES.size()) + " ms";
	}

	/** Latency of another player in ms, or -1 if unknown. Refreshed at most every 500 ms per player. */
	public static int forPlayer(Minecraft mc, UUID id) {
		ClientPacketListener conn = mc.getConnection();
		if (conn == null || mc.hasSingleplayerServer()) return -1;
		long now = System.currentTimeMillis();
		long[] e = PLAYERS.get(id);
		if (e == null || now - e[1] >= 500) {
			PlayerInfo info = conn.getPlayerInfo(id);
			int lat = info == null ? -1 : info.getLatency();
			e = new long[] {lat, now};
			PLAYERS.put(id, e);
			if (PLAYERS.size() > 200) PLAYERS.clear();
		}
		return e[0] > 0 ? (int) e[0] : -1;
	}
}
