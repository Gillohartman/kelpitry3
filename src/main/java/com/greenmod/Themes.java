package com.greenmod;

/** One-click color sets for the menu and the main HUD pieces. */
public final class Themes {
	//                        accent    background  text
	private static final int[][] SETS = {
			{0xFFFFFF, 0x0A0A0B, 0xF2F2F2},
			{0x00B4FF, 0x0D1620, 0xE6F1FA},
			{0xFF8A3D, 0x1A1210, 0xF5E9E0},
			{0xB46BFF, 0x14111C, 0xEBE4F5},
			{0xFF4F8B, 0x1A1015, 0xF7E4EA},
			{0xFFFFFF, 0x0E0E0E, 0xDDDDDD}
	};

	private Themes() {}

	public static void apply(int i) {
		int[] t = SETS[Math.floorMod(i, SETS.length)];
		Modules.GUI_ACCENT.rgb = t[0];
		Modules.GUI_BG.rgb = t[1];
		Modules.GUI_TEXT.rgb = t[2];
		Modules.CROSS_COLOR.rgb = t[0];
		Modules.PT_COLOR.rgb = t[0];
		Modules.H_PLAYERS.color.rgb = t[0];
		Modules.KEYS_COLOR.rgb = t[0];
		Modules.TG_COLOR.rgb = t[0];
		Modules.CF_COLOR.rgb = t[0];
		Modules.ONLINE_COLOR.rgb = t[0];
		Notifications.push("Theme applied.");
	}
}
