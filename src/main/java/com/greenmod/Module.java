package com.greenmod;

import java.util.ArrayList;
import java.util.List;

public final class Module {
	public final String id, name, description;
	public final Category category;
	public final Safety safety;
	public final List<Setting> settings = new ArrayList<>();

	private boolean enabled;
	private final boolean defaultEnabled;
	/** Set by the safety system. Locked modules never run. Restricted modules start locked. */
	public boolean locked;
	public boolean wasEnabledBeforeLock;
	/** Key that toggles the module (-1 = none). */
	public int key = -1;
	public boolean lastDown;
	/** Settings-only entry (no on/off switch). */
	public boolean noToggle;
	/** True if the module only makes sense inside a world (greyed out in the menu on the title screen). */
	public final boolean needsWorld;
	public float anim;

	public Module(String id, String name, String description, Category category, Safety safety, boolean enabled) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.safety = safety;
		this.enabled = enabled;
		this.defaultEnabled = enabled;
		this.locked = safety != Safety.PUBLIC_SAFE;
		this.anim = enabled ? 1f : 0f;
		this.needsWorld = defaultNeedsWorld(category, id);
	}

	private static boolean defaultNeedsWorld(Category c, String id) {
		switch (id) {
			case "spotify": case "clock": case "discord_rpc": case "chat_settings": case "chat_tools": case "screenshots":
			case "auto_reconnect": case "server_profiles": case "crosshair": case "macros": case "auto_home": case "fake_name":
			case "gui": case "server_safety": case "notif_history": case "streamer_mode": case "inv_tint":
				return false;
			default:
		}
		switch (c) {
			case COMBAT: case MOVEMENT: case WORLD: case PLAYER: case HUD:
				return true;
			case VISUALS:
				return id.startsWith("hit_") || id.endsWith("_tags") || id.equals("entity_esp") || id.equals("freecam")
						|| id.equals("freelook") || id.equals("container_peek") || id.equals("damage_numbers");
			case UTILITY:
				return id.equals("inv_sort") || id.equals("hotbar_refill") || id.equals("autocrafter") || id.equals("chest_tools") || id.equals("copy_name");
			default:
				return false;
		}
	}

	public Module alwaysOn() {
		noToggle = true;
		enabled = true;
		return this;
	}

	public boolean restricted() { return safety != Safety.PUBLIC_SAFE; }
	public boolean isEnabled() { return enabled; }
	/** The only check a module's code needs: enabled and not locked. */
	public boolean isActive() { return enabled && !locked; }
	public boolean isLocked() { return restricted() && locked; }

	/** Returns false if the safety system refused. */
	public boolean setEnabled(boolean v) {
		if (v && restricted() && (locked || !ServerSafety.allowsRestricted())) {
			Notifications.push("This feature is disabled on this server.");
			return false;
		}
		enabled = v;
		return true;
	}

	public void toggle() { setEnabled(!enabled); }

	/** Used by config loading; the safety tick re-applies the lock right after. */
	public void loadEnabled(boolean v) { enabled = v; }

	public void reset() {
		if (!noToggle) enabled = defaultEnabled && !isLocked();
		key = -1;
		for (Setting s : settings) s.reset();
	}

	public Setting.Bool bool(String n, boolean def) { Setting.Bool s = new Setting.Bool(n, def); settings.add(s); return s; }
	public Setting.Num num(String n, double min, double max, double def, double step) { Setting.Num s = new Setting.Num(n, min, max, def, step); settings.add(s); return s; }
	public Setting.Color color(String n, int rgb) { Setting.Color s = new Setting.Color(n, rgb); settings.add(s); return s; }
	public Setting.Key keybind(String n, int key) { Setting.Key s = new Setting.Key(n, key); settings.add(s); return s; }
	public Setting.Choice choice(String n, int index, String... options) { Setting.Choice s = new Setting.Choice(n, index, options); settings.add(s); return s; }
	public Setting.Text text(String n, String v) { Setting.Text s = new Setting.Text(n, v); settings.add(s); return s; }
	public Setting.BlockColors blocks(String n, Setting.BlockColors.Entry... defs) { Setting.BlockColors s = new Setting.BlockColors(n, defs); settings.add(s); return s; }
	public Setting.Items items(String n, boolean toggleable, String... defs) { Setting.Items s = new Setting.Items(n, toggleable, defs); settings.add(s); return s; }
}
