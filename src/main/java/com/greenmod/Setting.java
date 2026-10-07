package com.greenmod;

import java.util.ArrayList;
import java.util.List;

/** One configurable option of a module. */
public abstract class Setting {
	public final String name;
	/** If true, this single option only works where restricted modules are allowed. */
	public boolean restricted;

	protected Setting(String name) {
		this.name = name;
	}

	public abstract void reset();

	public static final class Bool extends Setting {
		public boolean value;
		private final boolean def;
		public Bool(String name, boolean value) { super(name); this.value = value; this.def = value; }
		@Override public void reset() { value = def; }
	}

	public static final class Num extends Setting {
		public final double min, max, step;
		public double value;
		private final double def;
		public Num(String name, double min, double max, double value, double step) {
			super(name);
			this.min = min; this.max = max; this.value = value; this.step = step; this.def = value;
		}
		public int i() { return (int) Math.round(value); }
		public float f() { return (float) value; }
		@Override public void reset() { value = def; }
	}

	public static final class Color extends Setting {
		public int rgb;
		private final int def;
		public Color(String name, int rgb) { super(name); this.rgb = rgb; this.def = rgb; }
		@Override public void reset() { rgb = def; }
	}

	public static final class Key extends Setting {
		public int key;
		private final int def;
		public Key(String name, int key) { super(name); this.key = key; this.def = key; }
		@Override public void reset() { key = def; }
	}

	public static final class Choice extends Setting {
		public final String[] options;
		public int index;
		private final int def;
		public Choice(String name, int index, String... options) {
			super(name);
			this.options = options; this.index = index; this.def = index;
		}
		public String get() { return options[index]; }
		public void next(int dir) { index = Math.floorMod(index + dir, options.length); }
		@Override public void reset() { index = def; }
	}

	public static final class Text extends Setting {
		public String value;
		private final String def;
		public Text(String name, String value) { super(name); this.value = value; this.def = value; }
		@Override public void reset() { value = def; }
	}

	/** A list of strings. Entries starting with "-" count as disabled when toggleable. */
	public static final class Items extends Setting {
		public final List<String> values = new ArrayList<>();
		private final List<String> def = new ArrayList<>();
		public final boolean toggleable;
		public Items(String name, boolean toggleable, String... defaults) {
			super(name);
			this.toggleable = toggleable;
			for (String s : defaults) { values.add(s); def.add(s); }
		}
		public boolean isOn(String entry) { return !entry.startsWith("-"); }
		public static String clean(String entry) { return entry.startsWith("-") ? entry.substring(1) : entry; }
		@Override public void reset() { values.clear(); values.addAll(def); }
	}

	/** A list of blocks, each with its own color and on/off switch (used by Block ESP). */
	public static final class BlockColors extends Setting {
		public static final class Entry {
			public String id;
			public int rgb;
			public boolean on;
			public Entry(String id, int rgb, boolean on) { this.id = id; this.rgb = rgb; this.on = on; }
		}

		public final List<Entry> values = new ArrayList<>();
		private final List<Entry> def = new ArrayList<>();

		public BlockColors(String name, Entry... defaults) {
			super(name);
			for (Entry e : defaults) { values.add(new Entry(e.id, e.rgb, e.on)); def.add(new Entry(e.id, e.rgb, e.on)); }
		}

		@Override public void reset() {
			values.clear();
			for (Entry e : def) values.add(new Entry(e.id, e.rgb, e.on));
		}
	}
}
