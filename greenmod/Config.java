package com.greenmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * One config, loaded once at startup, saved whenever something changes.
 * - changes are written 500 ms after they happen, on a background thread
 * - flush() writes right away (menu close, disconnect, shutdown)
 * - writes are atomic (temp file, then move) and keep a .bak of the last good file
 * - loading merges the file over the defaults: missing keys keep their defaults, unknown keys are kept as they are
 * - a damaged file falls back to the .bak, then to defaults; it is never silently wiped
 */
public final class Config {
	public static final int VERSION = 4;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "KelpClient-ConfigIO");
		t.setDaemon(true);
		return t;
	});

	public static String profile = "default";
	public static long playSeconds;

	/** The last file contents we read; used to keep keys we do not know about. */
	private static JsonObject loadedRoot = new JsonObject();
	private static String lastWritten = "";
	private static String lastState = "";
	private static long dirtySince;
	private static int counter;

	private Config() {}

	// ---------------------------------------------------------------- paths
	private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("kelpclient"); }
	private static Path profilesDir() { return dir().resolve("profiles"); }
	private static Path profileFile(String name) { return profilesDir().resolve(name + ".json"); }

	private static String clean(String name) {
		String n = name.trim().replaceAll("[^A-Za-z0-9 _-]", "");
		return n.isEmpty() ? "default" : n;
	}

	// ---------------------------------------------------------------- load
	public static void load() {
		try {
			Files.createDirectories(profilesDir());
			Path st = dir().resolve("state.json");
			JsonObject state = readJson(st);
			if (state != null) {
				if (state.has("profile")) profile = clean(state.get("profile").getAsString());
				if (state.has("playSeconds")) playSeconds = state.get("playSeconds").getAsLong();
			}
			loadProfile(profile);
		} catch (Exception e) {
			System.err.println("[RuinedClient] Config load problem, using defaults for what is missing: " + e);
		}
		lastWritten = buildJson(profileSnapshot());
		lastState = stateJson();
		dirtySince = 0;
	}

	/** Reads a JSON object; on failure tries the .bak, and moves a broken file aside instead of overwriting it. */
	private static JsonObject readJson(Path p) {
		JsonObject o = tryRead(p);
		if (o != null) return o;
		if (Files.exists(p)) {
			Path bak = Path.of(p.toString() + ".bak");
			JsonObject b = tryRead(bak);
			try {
				Files.move(p, Path.of(p.toString() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception ignored) {
			}
			if (b != null) {
				System.err.println("[RuinedClient] " + p.getFileName() + " was damaged, restored the backup.");
				return b;
			}
		}
		return null;
	}

	private static JsonObject tryRead(Path p) {
		try {
			if (!Files.exists(p)) return null;
			JsonElement e = JsonParser.parseString(Files.readString(p));
			return e.isJsonObject() ? e.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static void loadProfile(String name) {
		JsonObject root = readJson(profileFile(name));
		loadedRoot = root != null ? root : new JsonObject();
		if (root == null) return;
		try {
			JsonObject mods = root.getAsJsonObject("modules");
			if (mods != null) {
				for (Module m : Modules.ALL) {
					JsonObject o = mods.getAsJsonObject(m.id);
					if (o == null) continue;
					try {
						if (o.has("enabled") && !m.noToggle) m.loadEnabled(o.get("enabled").getAsBoolean());
						if (o.has("key")) m.key = o.get("key").getAsInt();
						JsonObject so = o.getAsJsonObject("settings");
						if (so == null) continue;
						for (Setting s : m.settings) {
							if (!so.has(s.name)) continue;
							try {
								applySetting(s, so.get(s.name));
							} catch (Exception e) {
								// one bad value must not cost the others
							}
						}
					} catch (Exception ignored) {
					}
				}
			}
			Macros.LIST.clear();
			JsonArray macros = root.getAsJsonArray("macros");
			if (macros != null) {
				for (JsonElement me : macros) {
					try {
						JsonObject o = me.getAsJsonObject();
						Macros.Macro m = new Macros.Macro();
						m.name = o.get("name").getAsString();
						m.key = o.get("key").getAsInt();
						m.enabled = o.get("enabled").getAsBoolean();
						for (JsonElement se : o.getAsJsonArray("steps")) {
							JsonObject so = se.getAsJsonObject();
							m.steps.add(new Macros.Step(so.get("text").getAsString(), so.get("delay").getAsInt()));
						}
						Macros.LIST.add(m);
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[RuinedClient] Some settings could not be read and kept their defaults: " + e);
		}
		// files older than version 4 still have the old green look: switch to the black and white theme once
		int fileVersion = root.has("configVersion") ? root.get("configVersion").getAsInt() : 0;
		if (fileVersion < 4) Themes.apply(0);
		// Streamer mode always starts off after a restart.
		Modules.STREAMER.loadEnabled(false);
	}

	private static void applySetting(Setting s, JsonElement e) {
		if (s instanceof Setting.Bool b) b.value = e.getAsBoolean();
		else if (s instanceof Setting.Num n) n.value = Math.max(n.min, Math.min(n.max, e.getAsDouble()));
		else if (s instanceof Setting.Color c) c.rgb = e.getAsInt();
		else if (s instanceof Setting.Key k) k.key = e.getAsInt();
		else if (s instanceof Setting.Choice ch) ch.index = Math.floorMod(e.getAsInt(), ch.options.length);
		else if (s instanceof Setting.Text t) t.value = e.getAsString();
		else if (s instanceof Setting.Items it) {
			it.values.clear();
			for (JsonElement v : e.getAsJsonArray()) it.values.add(v.getAsString());
		} else if (s instanceof Setting.BlockColors bc) {
			bc.values.clear();
			for (JsonElement v : e.getAsJsonArray()) {
				JsonObject eo = v.getAsJsonObject();
				bc.values.add(new Setting.BlockColors.Entry(eo.get("id").getAsString(), eo.get("rgb").getAsInt(), eo.get("on").getAsBoolean()));
			}
		}
	}

	// ---------------------------------------------------------------- build
	private static JsonObject profileSnapshot() {
		JsonObject root = loadedRoot.deepCopy();   // keeps keys we do not know about
		root.addProperty("configVersion", VERSION);
		JsonObject mods = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
		for (Module m : Modules.ALL) {
			JsonObject o = mods.has(m.id) && mods.get(m.id).isJsonObject() ? mods.getAsJsonObject(m.id) : new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			o.addProperty("key", m.key);
			JsonObject so = o.has("settings") && o.get("settings").isJsonObject() ? o.getAsJsonObject("settings") : new JsonObject();
			for (Setting s : m.settings) {
				if (s instanceof Setting.Bool b) so.addProperty(s.name, b.value);
				else if (s instanceof Setting.Num n) so.addProperty(s.name, n.value);
				else if (s instanceof Setting.Color c) so.addProperty(s.name, c.rgb);
				else if (s instanceof Setting.Key k) so.addProperty(s.name, k.key);
				else if (s instanceof Setting.Choice ch) so.addProperty(s.name, ch.index);
				else if (s instanceof Setting.Text t) so.addProperty(s.name, t.value);
				else if (s instanceof Setting.Items it) {
					JsonArray arr = new JsonArray();
					for (String v : it.values) arr.add(v);
					so.add(s.name, arr);
				} else if (s instanceof Setting.BlockColors bc) {
					JsonArray arr = new JsonArray();
					for (Setting.BlockColors.Entry en : bc.values) {
						JsonObject eo = new JsonObject();
						eo.addProperty("id", en.id);
						eo.addProperty("rgb", en.rgb);
						eo.addProperty("on", en.on);
						arr.add(eo);
					}
					so.add(s.name, arr);
				}
			}
			o.add("settings", so);
			mods.add(m.id, o);
		}
		root.add("modules", mods);

		JsonArray macros = new JsonArray();
		for (Macros.Macro m : Macros.LIST) {
			JsonObject o = new JsonObject();
			o.addProperty("name", m.name);
			o.addProperty("key", m.key);
			o.addProperty("enabled", m.enabled);
			JsonArray steps = new JsonArray();
			for (Macros.Step st : m.steps) {
				JsonObject so = new JsonObject();
				so.addProperty("text", st.text);
				so.addProperty("delay", st.delayMs);
				steps.add(so);
			}
			o.add("steps", steps);
			macros.add(o);
		}
		root.add("macros", macros);
		return root;
	}

	private static String buildJson(JsonObject o) { return GSON.toJson(o); }

	private static String stateJson() {
		JsonObject o = new JsonObject();
		o.addProperty("profile", profile);
		o.addProperty("playSeconds", playSeconds);
		return GSON.toJson(o);
	}

	// ---------------------------------------------------------------- save
	/** Call every client tick (also on the title screen). Writes changes 500 ms after they happened. */
	public static void tick() {
		if (++counter % 5 != 0) return;
		try {
			String json = buildJson(profileSnapshot());
			String st = stateJson();
			boolean changed = !json.equals(lastWritten) || (!st.equals(lastState) && counter % 600 == 0);
			if (!changed) { dirtySince = 0; return; }
			long now = System.currentTimeMillis();
			if (dirtySince == 0) dirtySince = now;
			if (now - dirtySince >= 500) writeAsync(json, st);
		} catch (Throwable t) {
			Safe.run("config-tick", () -> { throw new RuntimeException(t); });
		}
	}

	private static void writeAsync(String json, String state) {
		lastWritten = json;
		lastState = state;
		dirtySince = 0;
		final Path pf = profileFile(profile), sf = dir().resolve("state.json");
		IO.execute(() -> {
			atomicWrite(pf, json);
			atomicWrite(sf, state);
		});
	}

	/** Writes right now (menu close, disconnect, shutdown). */
	public static void flush() {
		try {
			Files.createDirectories(profilesDir());
			String json = buildJson(profileSnapshot());
			String st = stateJson();
			lastWritten = json;
			lastState = st;
			dirtySince = 0;
			atomicWrite(profileFile(profile), json);
			atomicWrite(dir().resolve("state.json"), st);
		} catch (Exception e) {
			System.err.println("[RuinedClient] Could not save the config: " + e);
		}
	}

	/** Old name, kept for the other classes. */
	public static void save() { flush(); }

	private static synchronized void atomicWrite(Path target, String content) {
		try {
			Files.createDirectories(target.getParent());
			Path tmp = Path.of(target.toString() + ".tmp");
			Files.writeString(tmp, content);
			if (Files.exists(target)) {
				// keep the last good file
				if (tryRead(target) != null) Files.copy(target, Path.of(target.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			}
			try {
				Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception atomicFailed) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			System.err.println("[RuinedClient] Could not write " + target.getFileName() + ": " + e);
		}
	}

	// ---------------------------------------------------------------- profiles
	public static List<String> profiles() {
		List<String> out = new ArrayList<>();
		try (Stream<Path> s = Files.list(profilesDir())) {
			s.forEach(p -> {
				String n = p.getFileName().toString();
				if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
			});
		} catch (Exception ignored) {
		}
		if (!out.contains(profile)) out.add(profile);
		java.util.Collections.sort(out);
		return out;
	}

	public static void createProfile(String name) {
		flush();
		profile = clean(name);
		flush();
	}

	public static void switchProfile(String name) {
		flush();
		profile = clean(name);
		loadProfile(profile);
		lastWritten = buildJson(profileSnapshot());
		flush();
	}

	public static void deleteProfile(String name) {
		try {
			if (name.equals(profile)) return;
			Files.deleteIfExists(profileFile(name));
		} catch (Exception ignored) {
		}
	}
}
