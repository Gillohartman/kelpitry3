package com.greenmod;

public enum Category {
	COMBAT("Combat"), VISUALS("Visuals"), PLAYER("Player"), MOVEMENT("Movement"), WORLD("World"),
	HUD("HUD"), UTILITY("Utility"), MACRO("Macros"), SETTINGS("Settings");

	public final String title;

	Category(String title) {
		this.title = title;
	}
}
