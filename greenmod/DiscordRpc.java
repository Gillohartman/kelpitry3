package com.greenmod;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal Discord Rich Presence over the local Discord IPC socket (no native libraries).
 * You need your own Discord application (name it "Ruined Client") and paste its Application ID in the module.
 * Only local, non-private info is sent unless you enable the address option.
 */
public final class DiscordRpc {
	private interface Conn {
		void write(byte[] b) throws IOException;
		int read(byte[] b, int off, int len) throws IOException;
		void close();
	}

	private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "KelpClient-DiscordRPC");
		t.setDaemon(true);
		return t;
	});

	private static Conn conn;
	private static String connectedId = "";
	private static String lastSent = "";
	private static long lastAttempt;
	private static final long START = System.currentTimeMillis() / 1000L;
	private static int tickCounter;

	private DiscordRpc() {}

	public static void tick(Minecraft mc) {
		if (++tickCounter % 100 != 0) return; // every 5 seconds
		boolean on = Modules.DISCORD.isActive() && !Modules.DC_APP_ID.value.isBlank();
		if (!on) {
			if (conn != null) EXEC.execute(DiscordRpc::disconnect);
			return;
		}
		final String id = Modules.DC_APP_ID.value.trim();
		final String details = "Playing Minecraft" + (Modules.DC_VERSION.value ? " " + mcVersion() : "");
		final String state = stateText(mc);
		final String key = id + "|" + details + "|" + state;
		if (key.equals(lastSent) && conn != null) return;
		EXEC.execute(() -> push(id, details, state, key));
	}

	public static void shutdown() {
		EXEC.execute(DiscordRpc::disconnect);
	}

	private static String mcVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft")
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
	}

	private static String stateText(Minecraft mc) {
		if (mc.level == null) return "In the menus";
		if (!Modules.DC_WORLD.value) return "In game";
		if (mc.hasSingleplayerServer()) return "Singleplayer";
		ServerData sd = mc.getCurrentServer();
		if (sd != null && Modules.DC_SERVER.value && !StreamerMode.hideServer() && !sd.isLan()) return "Playing on " + sd.ip;
		return "Multiplayer";
	}

	// ---------------- background thread ----------------
	private static void push(String id, String details, String state, String key) {
		try {
			long now = System.currentTimeMillis();
			if (conn == null || !id.equals(connectedId)) {
				if (now - lastAttempt < 15000) return;
				lastAttempt = now;
				disconnect();
				connect(id);
			}
			if (conn == null) return;

			JsonObject activity = new JsonObject();
			activity.addProperty("details", details);
			activity.addProperty("state", state);
			JsonObject ts = new JsonObject();
			ts.addProperty("start", START);
			activity.add("timestamps", ts);

			JsonObject args = new JsonObject();
			args.addProperty("pid", ProcessHandle.current().pid());
			args.add("activity", activity);

			JsonObject payload = new JsonObject();
			payload.addProperty("cmd", "SET_ACTIVITY");
			payload.add("args", args);
			payload.addProperty("nonce", UUID.randomUUID().toString());

			send(1, payload.toString());
			readFrame();
			lastSent = key;
		} catch (Throwable t) {
			disconnect();
		}
	}

	private static void connect(String id) throws IOException {
		for (int i = 0; i < 10 && conn == null; i++) {
			try {
				conn = open(i);
			} catch (IOException ignored) {
			}
		}
		if (conn == null) return;
		connectedId = id;
		JsonObject hs = new JsonObject();
		hs.addProperty("v", 1);
		hs.addProperty("client_id", id);
		send(0, hs.toString());
		readFrame();
	}

	private static Conn open(int n) throws IOException {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win")) {
			final RandomAccessFile raf = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + n, "rw");
			return new Conn() {
				public void write(byte[] b) throws IOException { raf.write(b); }
				public int read(byte[] b, int off, int len) throws IOException { return raf.read(b, off, len); }
				public void close() { try { raf.close(); } catch (IOException ignored) { } }
			};
		}
		String base = firstNonNull(System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), System.getenv("TEMP"), "/tmp");
		String[] dirs = {base, base + "/app/com.discordapp.Discord", base + "/snap.discord"};
		IOException last = new IOException("no socket");
		for (String d : dirs) {
			Path p = Path.of(d, "discord-ipc-" + n);
			if (!Files.exists(p)) continue;
			try {
				final SocketChannel ch = SocketChannel.open(UnixDomainSocketAddress.of(p));
				return new Conn() {
					public void write(byte[] b) throws IOException {
						ByteBuffer bb = ByteBuffer.wrap(b);
						while (bb.hasRemaining()) ch.write(bb);
					}
					public int read(byte[] b, int off, int len) throws IOException { return ch.read(ByteBuffer.wrap(b, off, len)); }
					public void close() { try { ch.close(); } catch (IOException ignored) { } }
				};
			} catch (IOException e) {
				last = e;
			}
		}
		throw last;
	}

	private static String firstNonNull(String... v) {
		for (String s : v) if (s != null && !s.isEmpty()) return s;
		return "/tmp";
	}

	private static void send(int op, String json) throws IOException {
		byte[] data = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer bb = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(op).putInt(data.length).put(data);
		conn.write(bb.array());
	}

	private static void readFrame() throws IOException {
		byte[] head = new byte[8];
		readFully(head, 8);
		ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
		hb.getInt();
		int len = hb.getInt();
		if (len < 0 || len > 1 << 20) throw new IOException("bad frame");
		readFully(new byte[len], len);
	}

	private static void readFully(byte[] b, int len) throws IOException {
		int off = 0;
		while (off < len) {
			int r = conn.read(b, off, len - off);
			if (r < 0) throw new IOException("closed");
			off += r;
		}
	}

	private static void disconnect() {
		if (conn != null) {
			conn.close();
			conn = null;
		}
		connectedId = "";
		lastSent = "";
	}
}
