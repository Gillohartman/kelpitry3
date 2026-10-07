package com.greenmod;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Every module with its default settings. */
public final class Modules {
	public static final List<Module> ALL = new ArrayList<>();

	private Modules() {}

	private static Module reg(Module m) {
		ALL.add(m);
		return m;
	}

	public static List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : ALL) if (m.category == c) out.add(m);
		return out;
	}

	public static void resetAll() {
		for (Module m : ALL) m.reset();
		Macros.LIST.clear();
	}


	// =====================================================================
	// VISUALS
	// =====================================================================

	// ---- Nametags ----
	public static final Module PLAYER_TAGS = reg(new Module("player_tags", "Player Nametags", "Small custom nametags for players", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Color PT_COLOR = PLAYER_TAGS.color("Text color", 0xFFFFFF);
	public static final Setting.Num PT_SCALE = PLAYER_TAGS.num("Scale", 0.3, 2.0, 0.7, 0.05);
	public static final Setting.Num PT_RANGE = PLAYER_TAGS.num("Range", 8, 128, 64, 1);
	public static final Setting.Bool PT_HEALTH = PLAYER_TAGS.bool("Show health", true);
	public static final Setting.Bool PT_DIST = PLAYER_TAGS.bool("Show distance", false);
	public static final Setting.Bool PT_ARMOR = PLAYER_TAGS.bool("Show armor", false);
	public static final Setting.Bool PT_HELD = PLAYER_TAGS.bool("Show held item", false);
	public static final Setting.Bool PT_BG = PLAYER_TAGS.bool("Background", true);
	public static final Setting.Num PT_BG_OP = PLAYER_TAGS.num("Background opacity %", 0, 100, 45, 5);
	public static final Setting.Bool PT_SELF = PLAYER_TAGS.bool("Own nametag in F5", true);
	public static final Setting.Bool PT_KELP = PLAYER_TAGS.bool("Badge for Ruined Client users", true);
	public static final Setting.Bool PT_PING = PLAYER_TAGS.bool("Show ping", true);
	public static final Setting.Color PT_PING_COLOR = PLAYER_TAGS.color("Ping color", 0xAAAAAA);
	public static final Setting.Bool PT_ANNOUNCE = PLAYER_TAGS.bool("Mark me as a user (after rejoin)", true);

	public static final Module MOB_TAGS = reg(new Module("mob_tags", "Mob Nametags", "Small nametags with name and optional health", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Color MT_COLOR = MOB_TAGS.color("Text color", 0xFFFFFF);
	public static final Setting.Num MT_SCALE = MOB_TAGS.num("Scale", 0.3, 2.0, 0.6, 0.05);
	public static final Setting.Num MT_RANGE = MOB_TAGS.num("Range", 4, 64, 20, 1);
	public static final Setting.Num MT_MAX = MOB_TAGS.num("Max tags", 1, 64, 16, 1);
	public static final Setting.Bool MT_HEALTH = MOB_TAGS.bool("Show health", true);
	public static final Setting.Bool MT_BG = MOB_TAGS.bool("Background", true);
	public static final Setting.Num MT_BG_OP = MOB_TAGS.num("Background opacity %", 0, 100, 35, 5);

	public static final Module ITEM_TAGS = reg(new Module("item_tags", "Item Nametags", "Names for dropped items", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Color IT_COLOR = ITEM_TAGS.color("Text color", 0xFFE08A);
	public static final Setting.Num IT_SCALE = ITEM_TAGS.num("Scale", 0.3, 2.0, 0.55, 0.05);
	public static final Setting.Num IT_RANGE = ITEM_TAGS.num("Range", 4, 64, 14, 1);
	public static final Setting.Num IT_MAX = ITEM_TAGS.num("Max tags", 1, 64, 16, 1);
	public static final Setting.Bool IT_COUNT = ITEM_TAGS.bool("Show item count", true);
	public static final Setting.Bool IT_BG = ITEM_TAGS.bool("Background", true);
	public static final Setting.Num IT_BG_OP = ITEM_TAGS.num("Background opacity %", 0, 100, 35, 5);

	// ---- Hitboxes ----
	public static final class Hit {
		public final Module module;
		public final Setting.Color color;
		public final Setting.Num thickness, opacity, range;
		public final Setting.Bool visibleOnly;
		public final Setting.Choice style;
		public final Setting.Color hitColor;
		public final Setting.Bool hitFlash;

		Hit(String id, String name, String desc, boolean on, int rgb) {
			module = reg(new Module(id, name, desc, Category.VISUALS, Safety.PUBLIC_SAFE, on));
			color = module.color("Color", rgb);
			thickness = module.num("Line width (px)", 1, 6, 1, 1);
			opacity = module.num("Opacity %", 10, 100, 100, 5);
			range = module.num("Range", 8, 96, 64, 1);
			visibleOnly = module.bool("Only when visible", true);
			style = module.choice("Style", 0, "2D box", "3D box");
			hitFlash = module.bool("Flash a color when hit", true);
			hitColor = module.color("Color when hit", 0xFF3B30);
		}
	}

	public static final Hit H_PLAYERS = new Hit("hit_players", "Hitbox: Players", "2D hitbox box around players", true, 0xFFFFFF);
	public static final Hit H_HOSTILE = new Hit("hit_hostile", "Hitbox: Hostile Mobs", "2D hitbox box around monsters", false, 0xFF3B30);
	public static final Hit H_PASSIVE = new Hit("hit_passive", "Hitbox: Passive Mobs", "2D hitbox box around animals and NPCs", false, 0x4FC3FF);
	public static final Hit H_ITEMS = new Hit("hit_items", "Hitbox: Items", "2D hitbox box around dropped items", false, 0xFFB020);

	// ---- Misc visuals ----
	public static final Module LOW_FIRE = reg(new Module("low_fire", "Low Fire", "Replaces the big fire overlay with a small one", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Num FIRE_HEIGHT = LOW_FIRE.num("Overlay height %", 0, 100, 18, 1);
	public static final Setting.Num FIRE_OPACITY = LOW_FIRE.num("Opacity %", 0, 100, 65, 5);

	public static final Module SMALL_HANDS = reg(new Module("small_hands", "Small Hands", "Smaller hands and held items", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Num HAND_SCALE = SMALL_HANDS.num("Scale", 0.3, 1.0, 0.6, 0.05);

	public static final Module CROSSHAIR = reg(new Module("crosshair", "Custom Crosshair", "Clean crosshair replacing the vanilla one", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Color CROSS_COLOR = CROSSHAIR.color("Color", 0xFFFFFF);
	public static final Setting.Num CROSS_LEN = CROSSHAIR.num("Length", 1, 12, 3, 1);
	public static final Setting.Num CROSS_GAP = CROSSHAIR.num("Gap", 0, 8, 2, 1);
	public static final Setting.Num CROSS_THICK = CROSSHAIR.num("Thickness", 1, 3, 1, 1);
	public static final Setting.Bool CROSS_DOT = CROSSHAIR.bool("Center dot", false);
	public static final Setting.Choice CROSS_MODE = CROSSHAIR.choice("Color mode", 0, "Single", "Two-tone (inner / outer)", "Multi (per arm)", "Rainbow");
	public static final Setting.Color CROSS_C2 = CROSSHAIR.color("Color 2", 0x888888);
	public static final Setting.Color CROSS_C3 = CROSSHAIR.color("Color 3", 0xBBBBBB);
	public static final Setting.Color CROSS_C4 = CROSSHAIR.color("Color 4", 0x555555);
	public static final Setting.Num CROSS_ALPHA = CROSSHAIR.num("Opacity %", 10, 100, 100, 5);
	public static final Setting.Num CROSS_RAINBOW = CROSSHAIR.num("Rainbow speed", 0.1, 5, 1, 0.1);
	public static final Setting.Bool CROSS_OUTLINE = CROSSHAIR.bool("Dark outline", false);
	public static final Setting.Bool CROSS_PIXEL = CROSSHAIR.bool("Use my pixel crosshair (editor below)", false);
	public static final Setting.Num CROSS_PIXEL_SCALE = CROSSHAIR.num("Pixel size", 1, 4, 1, 1);

	public static final Module FULLBRIGHT = reg(new Module("fullbright", "Fullbright", "Maximum brightness everywhere", Category.VISUALS, Safety.PUBLIC_SAFE, false));

	public static final Module NO_FOV = reg(new Module("no_fov", "No FOV Effects", "Turns off sprint, speed, underwater and distortion FOV effects", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Bool NF_FOV = NO_FOV.bool("Disable FOV effects", true);
	public static final Setting.Bool NF_DISTORT = NO_FOV.bool("Disable distortion effects", true);

	public static final Module ZOOM = reg(new Module("zoom", "Zoom", "Hold the key to zoom, scroll to adjust", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Key ZOOM_KEY = ZOOM.keybind("Zoom key (hold)", GLFW.GLFW_KEY_C);
	public static final Setting.Num ZOOM_LEVEL = ZOOM.num("Zoom strength", 1.1, 8.0, 1.5, 0.05);
	public static final Setting.Num ZOOM_MIN = ZOOM.num("Minimum zoom", 1.0, 4.0, 1.1, 0.05);
	public static final Setting.Num ZOOM_MAX = ZOOM.num("Maximum zoom", 1.5, 10.0, 4.0, 0.1);
	public static final Setting.Num ZOOM_STEP = ZOOM.num("Scroll step", 0.05, 1.0, 0.15, 0.05);
	public static final Setting.Num ZOOM_SMOOTH = ZOOM.num("Smoothing speed", 2, 30, 12, 1);
	public static final Setting.Bool ZOOM_REMEMBER = ZOOM.bool("Remember scrolled zoom", false);

	public static final Module TOTEM_GLOW = reg(new Module("totem_glow", "Totem Glow", "Totems glow in your hotbar and inventory", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Color TG_COLOR = TOTEM_GLOW.color("Glow color", 0xFFFFFF);
	public static final Setting.Bool TG_PULSE = TOTEM_GLOW.bool("Pulse", true);
	public static final Setting.Bool TG_HOTBAR = TOTEM_GLOW.bool("Glow in hotbar", true);
	public static final Setting.Bool TG_INV = TOTEM_GLOW.bool("Glow in inventory", true);
	public static final Setting.Bool TG_BADGE = TOTEM_GLOW.bool("Show stack count badge", false);

	public static final Module TOTEM_POP = reg(new Module("totem_pop", "Small Totem Pop", "Shrinks the totem pop animation", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Num TP_SCALE = TOTEM_POP.num("Scale", 0.1, 1.0, 0.45, 0.05);
	public static final Setting.Bool TP_HIDE = TOTEM_POP.bool("Hide completely", false);

	public static final Module ESP = reg(new Module("entity_esp", "Entity ESP", "Highlights entities through walls (restricted)", Category.VISUALS, Safety.RESTRICTED, false));
	public static final Setting.Bool ESP_PLAYERS = ESP.bool("Players", true);
	public static final Setting.Bool ESP_HOSTILE = ESP.bool("Hostile mobs", true);
	public static final Setting.Bool ESP_PASSIVE = ESP.bool("Passive mobs", false);
	public static final Setting.Color ESP_C_PLAYERS = ESP.color("Player color", 0xFFFFFF);
	public static final Setting.Color ESP_C_HOSTILE = ESP.color("Hostile color", 0xFF3B30);
	public static final Setting.Color ESP_C_PASSIVE = ESP.color("Passive color", 0x4FC3FF);
	public static final Setting.Num ESP_LINE = ESP.num("Outline width", 1, 4, 1, 1);
	public static final Setting.Num ESP_RANGE = ESP.num("Range", 8, 128, 64, 1);
	public static final Setting.Choice ESP_STYLE = ESP.choice("Style", 0, "Box 2D", "Box 3D", "Corners", "Filled");
	public static final Setting.Bool ESP_TRACERS = ESP.bool("Tracers", true);
	public static final Setting.Choice ESP_ORIGIN = ESP.choice("Tracer origin", 0, "Middle down", "Middle", "Middle up");

	// =====================================================================
	// PLAYER
	// =====================================================================
	public static final Module NO_BOB = reg(new Module("no_bob", "No View Bobbing", "Keeps view bobbing off", Category.PLAYER, Safety.PUBLIC_SAFE, true));

	public static final Module SLOW_SWING = reg(new Module("slow_swing", "Slow Swing", "Swings your hand slower (visual only)", Category.PLAYER, Safety.PUBLIC_SAFE, false));
	public static final Setting.Num SWING_MULT = SLOW_SWING.num("Slowdown (x)", 1.0, 8.0, 2.0, 0.25);

	public static final Module SPRINT = reg(new Module("auto_sprint", "Auto Sprint", "Sprints while you walk forward (restricted)", Category.PLAYER, Safety.RESTRICTED, false));

	// =====================================================================
	// COMBAT / MOVEMENT (restricted)
	// =====================================================================
	public static final Module AUTO_TOTEM = reg(new Module("auto_totem", "Auto Totem", "Moves a totem into your offhand (restricted)", Category.COMBAT, Safety.RESTRICTED, false));
	public static final Setting.Num AT_HEALTH = AUTO_TOTEM.num("Health threshold (0 = always)", 0, 20, 0, 1);
	public static final Setting.Num AT_SLOT = AUTO_TOTEM.num("Preferred slot (0 = any)", 0, 36, 0, 1);
	public static final Setting.Num AT_DELAY = AUTO_TOTEM.num("Delay (ticks)", 0, 20, 2, 1);

	public static final Module FLIGHT = reg(new Module("flight", "Flight", "Fly freely (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Num FLIGHT_SPEED = FLIGHT.num("Horizontal speed", 0.1, 3.0, 0.6, 0.05);
	public static final Setting.Num FLIGHT_VSPEED = FLIGHT.num("Vertical speed", 0.1, 3.0, 0.5, 0.05);
	public static final Setting.Bool FLIGHT_SCROLL = FLIGHT.bool("Scroll wheel changes speed", true);
	public static final Setting.Bool FLIGHT_HOVER = FLIGHT.bool("Hover in place while a menu is open", true);

	public static final Module ELYTRA_BOOST = reg(new Module("elytra_boost", "Elytra Boost", "Extra thrust while gliding (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Key EB_KEY = ELYTRA_BOOST.keybind("Boost key (hold)", GLFW.GLFW_KEY_LEFT_ALT);
	public static final Setting.Num EB_POWER = ELYTRA_BOOST.num("Thrust", 0.01, 0.2, 0.04, 0.01);
	public static final Setting.Num EB_MAX = ELYTRA_BOOST.num("Max speed", 0.5, 4.0, 2.0, 0.1);

	// =====================================================================
	// HUD
	// =====================================================================
	public static final Module ARMOR = reg(new Module("armor_hud", "Armor HUD", "Armor and durability next to the hotbar", Category.HUD, Safety.PUBLIC_SAFE, true));
	public static final HudPos ARMOR_POS = new HudPos(ARMOR, "Armor HUD", 4, 108, -2, 1.0);
	public static final Setting.Choice ARMOR_ORIENT = ARMOR.choice("Orientation", 0, "Horizontal", "Vertical");
	public static final Setting.Num ARMOR_SPACING = ARMOR.num("Spacing", 0, 16, 2, 1);
	public static final Setting.Choice ARMOR_DUR = ARMOR.choice("Durability", 1, "None", "Bar", "Number", "Percent");
	public static final Setting.Bool ARMOR_BG = ARMOR.bool("Background", false);
	public static final Setting.Num ARMOR_BG_OP = ARMOR.num("Background opacity %", 0, 100, 40, 5);

	public static final Module TOTEM_COUNT = reg(new Module("totem_counter", "Totem Counter", "Shows how many totems you carry", Category.HUD, Safety.PUBLIC_SAFE, true));
	public static final HudPos TOTEM_POS = new HudPos(TOTEM_COUNT, "Totem Counter", 4, -108, -2, 1.0);
	public static final Setting.Color TOTEM_TEXT = TOTEM_COUNT.color("Text color", 0xFFFFFF);
	public static final Setting.Bool TOTEM_BG = TOTEM_COUNT.bool("Background", true);
	public static final Setting.Bool TOTEM_HIDE0 = TOTEM_COUNT.bool("Hide when zero", false);

	public static final Module INFO = reg(new Module("info_hud", "Info Display", "Coordinates, direction and FPS", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos INFO_POS = new HudPos(INFO, "Info Display", 0, 4, 4, 1.0);
	public static final Setting.Color INFO_COLOR = INFO.color("Text color", 0xFFFFFF);
	public static final Setting.Bool INFO_COORDS = INFO.bool("Coordinates", true);
	public static final Setting.Bool INFO_DIR = INFO.bool("Direction", true);
	public static final Setting.Bool INFO_FPS = INFO.bool("FPS", true);
	public static final Setting.Bool INFO_PING = INFO.bool("Ping", true);
	public static final Setting.Num INFO_PING_MS = INFO.num("Ping update (ms)", 500, 5000, 1000, 100);

	public static final Module ONLINE = reg(new Module("online_hub", "Player Online Hub", "Shows watched players who are on your server", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos ONLINE_POS = new HudPos(ONLINE, "Player Online Hub", 2, -4, 4, 0.9);
	public static final Setting.Color ONLINE_COLOR = ONLINE.color("Name color", 0xFFFFFF);
	public static final Setting.Color ONLINE_TITLE = ONLINE.color("Title color", 0xAAAAAA);
	public static final Setting.Bool ONLINE_EMPTY = ONLINE.bool("Show when nobody is online", false);
	public static final Setting.Items ONLINE_LIST = ONLINE.items("Watch list", true);

	public static final Module HUD_COLORS = reg(new Module("hud_colors", "HUD Colors", "Custom colors for hearts, hunger, XP and inventory", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final Setting.Color HC_HEART = HUD_COLORS.color("Heart color", 0xE53935);
	public static final Setting.Color HC_HEART_BG = HUD_COLORS.color("Heart background", 0x3A1414);
	public static final Setting.Color HC_ABSORB = HUD_COLORS.color("Absorption color", 0xFFD60A);
	public static final Setting.Color HC_FOOD = HUD_COLORS.color("Hunger color", 0xD2955A);
	public static final Setting.Color HC_FOOD_BG = HUD_COLORS.color("Hunger background", 0x2B2014);
	public static final Setting.Bool HC_XP = HUD_COLORS.bool("Replace XP bar", true);
	public static final Setting.Color HC_XP_COLOR = HUD_COLORS.color("XP bar color", 0xFFFFFF);
	public static final Setting.Color HC_XP_BG = HUD_COLORS.color("XP bar background", 0x1E1E1E);
	public static final Setting.Color HC_LEVEL = HUD_COLORS.color("Level number color", 0xFFFFFF);
	public static final Setting.Color HC_INV = HUD_COLORS.color("Inventory tint color", 0xFFFFFF);
	public static final Setting.Num HC_INV_OP = HUD_COLORS.num("Inventory tint opacity %", 0, 60, 0, 1);

	// =====================================================================
	// UTILITY
	// =====================================================================
	public static final Module SORT = reg(new Module("inv_sort", "Sort Inventory", "Sorts and merges your inventory (restricted)", Category.UTILITY, Safety.RESTRICTED, true));
	public static final Setting.Key SORT_KEY = SORT.keybind("Sort key", GLFW.GLFW_KEY_R);
	public static final Setting.Bool SORT_CTRL = SORT.bool("Hold Ctrl with key", true);
	public static final Setting.Bool SORT_BUTTON = SORT.bool("Show Sort button", true);
	public static final Setting.Choice SORT_MODE = SORT.choice("Sort by", 0, "Item type", "Alphabetical", "Stack size", "Custom priority");
	public static final Setting.Bool SORT_MERGE = SORT.bool("Merge stacks", true);
	public static final Setting.Bool SORT_CHEST = SORT.bool("Sort chests too", true);
	public static final Setting.Items SORT_PRIORITY = SORT.items("Custom priority (words in item id)", false, "sword", "pickaxe", "axe", "bow", "food", "golden_apple", "totem");

	public static final Module REFILL = reg(new Module("hotbar_refill", "Refill Hotbar", "Refills empty hotbar slots (restricted)", Category.UTILITY, Safety.RESTRICTED, true));
	public static final Setting.Bool[] REFILL_SLOTS = new Setting.Bool[9];
	static {
		for (int i = 0; i < 9; i++) REFILL_SLOTS[i] = REFILL.bool("Refill slot " + (i + 1), true);
	}

	public static final Module SHULKER = reg(new Module("shulker_preview", "Shulker Preview", "Hover a shulker box to see what is inside", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Choice SH_ANCHOR = SHULKER.choice("Position", 0, "Follow mouse", "Top Left", "Top Right", "Bottom Left", "Bottom Right");
	public static final Setting.Num SH_OX = SHULKER.num("Offset X", -300, 300, 0, 1);
	public static final Setting.Num SH_OY = SHULKER.num("Offset Y", -300, 300, 0, 1);
	public static final Setting.Num SH_SCALE = SHULKER.num("Scale", 0.5, 2.0, 1.0, 0.05);
	public static final Setting.Bool SH_COUNTS = SHULKER.bool("Show counts", true);
	public static final Setting.Key SH_FREEZE = SHULKER.keybind("Freeze key (hold)", GLFW.GLFW_KEY_LEFT_ALT);

	public static final Module DISCORD = reg(new Module("discord_rpc", "Discord Rich Presence", "Shows Ruined Client on your Discord profile", Category.UTILITY, Safety.PUBLIC_SAFE, false));
	public static final Setting.Text DC_APP_ID = DISCORD.text("Application ID", "");
	public static final Setting.Bool DC_VERSION = DISCORD.bool("Show Minecraft version", true);
	public static final Setting.Bool DC_WORLD = DISCORD.bool("Show Singleplayer / Multiplayer", true);
	public static final Setting.Bool DC_SERVER = DISCORD.bool("Show server address (private!)", false);

	// =====================================================================
	// MACROS
	// =====================================================================
	public static final Module MACROS = reg(new Module("macros", "Macros", "Keys that send chat messages and commands", Category.MACRO, Safety.PUBLIC_SAFE, true));

	// =====================================================================
	// SETTINGS
	// =====================================================================
	public static final Module GUI = reg(new Module("gui", "Appearance", "Colors, size and shape of the Ruined Client menu", Category.SETTINGS, Safety.PUBLIC_SAFE, true).alwaysOn());
	public static final Setting.Color GUI_ACCENT = GUI.color("Accent color", 0xFFFFFF);
	public static final Setting.Color GUI_BG = GUI.color("Background color", 0x0A0A0B);
	public static final Setting.Color GUI_TEXT = GUI.color("Text color", 0xF2F2F2);
	public static final Setting.Num GUI_OPACITY = GUI.num("Background opacity %", 40, 100, 94, 1);
	public static final Setting.Num GUI_RADIUS = GUI.num("Corner radius", 0, 10, 6, 1);
	public static final Setting.Num GUI_SCALE = GUI.num("Menu scale", 0.6, 1.5, 1.0, 0.05);
	public static final Setting.Bool GUI_ANIM = GUI.bool("Animations", true);
	public static final Setting.Bool GUI_SOUND = GUI.bool("Click sounds", true);
	public static final Setting.Choice GUI_THEME = GUI.choice("Theme (press Apply below)", 0, "Ruined (black and white)", "Ocean", "Sunset", "Purple", "Rose", "Mono");

	public static final Module SAFETY = reg(new Module("server_safety", "Server Safety", "Controls where restricted modules may run", Category.SETTINGS, Safety.PUBLIC_SAFE, true).alwaysOn());
	public static final Setting.Items ALLOWED_SERVERS = SAFETY.items("Allowed servers (host or host:port)", true, "unser.g-portal.rocks");
	public static final Setting.Bool ALLOW_LAN = SAFETY.bool("Allow restricted modules on LAN", true);
	public static final Setting.Bool RESTORE_AFTER_LOCK = SAFETY.bool("Restore modules after a locked server", false);

	// =====================================================================
	// MORE MODULES (part 2)
	// =====================================================================

	// ---- Camera ----
	public static final Module FREELOOK = reg(new Module("freelook", "Freelook", "Hold the key to look around without turning", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Key FL_KEY = FREELOOK.keybind("Freelook key (hold)", GLFW.GLFW_KEY_LEFT_ALT);
	public static final Setting.Num FL_DIST = FREELOOK.num("Camera distance", 1.0, 12.0, 4.0, 0.25);
	public static final Setting.Bool FL_SCROLL = FREELOOK.bool("Scroll wheel changes distance", true);

	public static final Module FREECAM = reg(new Module("freecam", "Freecam", "Detached camera (restricted)", Category.VISUALS, Safety.RESTRICTED, false));
	public static final Setting.Num FC_SPEED = FREECAM.num("Movement speed", 0.1, 5.0, 0.8, 0.05);
	public static final Setting.Bool FC_SCROLL = FREECAM.bool("Scroll wheel changes speed", true);
	public static final Setting.Num FC_VSPEED = FREECAM.num("Vertical speed", 0.1, 5.0, 0.6, 0.05);
	public static final Setting.Num FC_SMOOTH = FREECAM.num("Smoothing (0 = instant)", 0, 0.95, 0.35, 0.05);
	public static final Setting.Num FC_SPRINT = FREECAM.num("Sprint multiplier", 1, 5, 2, 0.1);

	public static final Module CONTAINER_PEEK = reg(new Module("container_peek", "Container Peek", "Freecam: see inside the container you look at (singleplayer only)", Category.VISUALS, Safety.RESTRICTED, false));
	public static final HudPos PEEK_POS = new HudPos(CONTAINER_PEEK, "Container Peek", 6, 0, 0, 1.0);
	public static final Setting.Num PEEK_RANGE = CONTAINER_PEEK.num("Range", 4, 128, 48, 1);

	public static final Module FLAT_ITEMS = reg(new Module("flat_items", "Flat Items", "Dropped items lie flat on the ground", Category.VISUALS, Safety.PUBLIC_SAFE, true));
	public static final Setting.Bool FI_NOSPIN = FLAT_ITEMS.bool("Stop spinning and bobbing", true);
	public static final Setting.Bool FI_FLAT = FLAT_ITEMS.bool("Lay flat", true);

	// ---- ESP ----
	public static final Module BLOCK_ESP = reg(new Module("block_esp", "Block ESP", "Highlights chosen blocks through walls (restricted)", Category.WORLD, Safety.RESTRICTED, false));
	public static final Setting.BlockColors BE_BLOCKS = BLOCK_ESP.blocks("Blocks (each with its own color)",
			new Setting.BlockColors.Entry("diamond_ore", 0x00E5FF, true),
			new Setting.BlockColors.Entry("deepslate_diamond_ore", 0x00E5FF, true),
			new Setting.BlockColors.Entry("ancient_debris", 0xFF9500, true),
			new Setting.BlockColors.Entry("emerald_ore", 0x30D158, true),
			new Setting.BlockColors.Entry("deepslate_emerald_ore", 0x30D158, true),
			new Setting.BlockColors.Entry("gold_ore", 0xFFD60A, false),
			new Setting.BlockColors.Entry("deepslate_gold_ore", 0xFFD60A, false),
			new Setting.BlockColors.Entry("iron_ore", 0xD2A679, false),
			new Setting.BlockColors.Entry("deepslate_iron_ore", 0xD2A679, false),
			new Setting.BlockColors.Entry("coal_ore", 0x9E9E9E, false),
			new Setting.BlockColors.Entry("redstone_ore", 0xFF3B30, false),
			new Setting.BlockColors.Entry("lapis_ore", 0x0A84FF, false),
			new Setting.BlockColors.Entry("nether_quartz_ore", 0xFFFFFF, false),
			new Setting.BlockColors.Entry("spawner", 0xBF5AF2, false));
	public static final Setting.Choice BE_ORIGIN = BLOCK_ESP.choice("Tracer origin", 0, "Middle down", "Middle", "Middle up", "Custom (sliders below)");
	public static final Setting.Choice BE_TRACER_STYLE = BLOCK_ESP.choice("Tracer style", 1, "Full line", "Short stub");
	public static final Setting.Num BE_STUB_LEN = BLOCK_ESP.num("Stub length (px)", 20, 300, 70, 5);
	public static final Setting.Num BE_ANCHOR_X = BLOCK_ESP.num("Tracer anchor X (% of screen)", 0, 100, 50, 1);
	public static final Setting.Num BE_ANCHOR_Y = BLOCK_ESP.num("Tracer anchor Y (% of screen)", 0, 100, 90, 1);
	public static final Setting.Bool BE_EDGE = BLOCK_ESP.bool("Keep tracers on screen (even behind you)", true);
	public static final Setting.Bool BE_LEGEND = BLOCK_ESP.bool("Show legend (what blocks were found)", true);
	public static final HudPos BE_LEGEND_POS = new HudPos(BLOCK_ESP, "Block ESP legend", 0, 4, 60, 0.9);
	public static final Setting.Color BE_COLOR = BLOCK_ESP.color("Color", 0x00E5FF);
	public static final Setting.Num BE_OPACITY = BLOCK_ESP.num("Fill opacity %", 0, 80, 20, 5);
	public static final Setting.Bool BE_OUTLINE = BLOCK_ESP.bool("Outline", true);
	public static final Setting.Num BE_LINE = BLOCK_ESP.num("Line width (px)", 1, 6, 1, 1);
	public static final Setting.Bool BE_TRACERS = BLOCK_ESP.bool("Tracers", false);
	public static final Setting.Num BE_RANGE = BLOCK_ESP.num("Range (blocks)", 8, 64, 32, 1);

	public static final Module CHUNK_FINDER = reg(new Module("chunk_finder", "Chunk Finder", "Marks chunks with growth indicators (restricted)", Category.WORLD, Safety.RESTRICTED, false));
	public static final Setting.Color CF_COLOR = CHUNK_FINDER.color("Highlight color", 0xFFD60A);
	public static final Setting.Bool CF_VINES = CHUNK_FINDER.bool("Indicator: vines", true);
	public static final Setting.Bool CF_KELP = CHUNK_FINDER.bool("Indicator: kelp", true);
	public static final Setting.Bool CF_DRIP = CHUNK_FINDER.bool("Indicator: dripstone", true);
	public static final Setting.Num CF_SENS = CHUNK_FINDER.num("Sensitivity", 1, 10, 5, 1);
	public static final Setting.Num CF_THRESHOLD = CHUNK_FINDER.num("Estimate threshold (indicator blocks)", 4, 400, 40, 1);
	public static final Setting.Bool CF_WORLD = CHUNK_FINDER.bool("World overlay (chunk outlines)", true);
	public static final Setting.Bool CF_MINIMAP = CHUNK_FINDER.bool("Minimap overlay", true);
	public static final Setting.Num CF_RADIUS = CHUNK_FINDER.num("Scan radius (chunks)", 2, 16, 8, 1);
	public static final Setting.Bool CF_AMETHYST = CHUNK_FINDER.bool("Show amethyst (count in geode center)", true);
	public static final Setting.Bool CF_TIME = CHUNK_FINDER.bool("Show time percentage per chunk", true);
	public static final Setting.Num CF_FULL_HOURS = CHUNK_FINDER.num("Hours that count as 100%", 1, 48, 4, 1);
	public static final HudPos CF_POS = new HudPos(CHUNK_FINDER, "Chunk Finder minimap", 2, -4, 60, 1.0);

	// ---- Combat assist (restricted) ----
	public static final Module BOW_AIM = reg(new Module("bow_aim", "Bow Aim Assist", "Aims drawn bows at targets (restricted)", Category.COMBAT, Safety.RESTRICTED, false));
	public static final Setting.Key BA_KEY = BOW_AIM.keybind("Aim key (hold)", GLFW.GLFW_KEY_X);
	public static final Setting.Num BA_RANGE = BOW_AIM.num("Range", 8, 100, 50, 1);
	public static final Setting.Num BA_FOV = BOW_AIM.num("FOV (degrees)", 10, 360, 90, 5);
	public static final Setting.Bool BA_PREDICT = BOW_AIM.bool("Prediction", true);
	public static final Setting.Num BA_SMOOTH = BOW_AIM.num("Smoothing (higher = slower)", 1, 20, 4, 1);
	public static final Setting.Bool BA_PLAYERS = BOW_AIM.bool("Target players", true);
	public static final Setting.Bool BA_HOSTILE = BOW_AIM.bool("Target hostile mobs", true);
	public static final Setting.Bool BA_PASSIVE = BOW_AIM.bool("Target passive mobs", false);

	public static final Module SWORD_AIM = reg(new Module("sword_aim", "Sword Target Assist", "Turns toward the nearest target (restricted)", Category.COMBAT, Safety.RESTRICTED, false));
	public static final Setting.Key SA_KEY = SWORD_AIM.keybind("Target key (hold)", GLFW.GLFW_KEY_Z);
	public static final Setting.Num SA_RANGE = SWORD_AIM.num("Range", 2, 8, 4.5, 0.5);
	public static final Setting.Num SA_FOV = SWORD_AIM.num("FOV (degrees)", 10, 360, 90, 5);
	public static final Setting.Choice SA_PRIORITY = SWORD_AIM.choice("Target priority", 0, "Closest", "Lowest health", "Smallest angle");
	public static final Setting.Num SA_SMOOTH = SWORD_AIM.num("Smoothing (higher = slower)", 1, 20, 5, 1);
	public static final Setting.Bool SA_PLAYERS = SWORD_AIM.bool("Target players", true);
	public static final Setting.Bool SA_HOSTILE = SWORD_AIM.bool("Target hostile mobs", true);
	public static final Setting.Bool SA_PASSIVE = SWORD_AIM.bool("Target passive mobs", false);

	// ---- Player / utility extras ----
	public static final Module AUTO_WALK = reg(new Module("auto_walk", "Auto Walk", "Holds forward for you (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));

	public static final Module KEYSTROKES = reg(new Module("keystrokes", "Keystrokes", "Shows WASD, space and mouse buttons", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos KEYS_POS = new HudPos(KEYSTROKES, "Keystrokes", 3, 8, -8, 1.0);
	public static final Setting.Color KEYS_COLOR = KEYSTROKES.color("Pressed color", 0xFFFFFF);
	public static final Setting.Choice KEYS_STYLE = KEYSTROKES.choice("Style", 1, "Classic", "Creamy");

	public static final Module CHAT = reg(new Module("chat_settings", "Chat Settings", "Applies your chat layout every time you play", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Num CHAT_SCALE = CHAT.num("Text size %", 40, 100, 46, 1);
	public static final Setting.Num CHAT_BG = CHAT.num("Text background %", 0, 100, 0, 1);
	public static final Setting.Num CHAT_H_FOCUSED = CHAT.num("Focused height (px)", 20, 180, 79, 1);
	public static final Setting.Num CHAT_H_UNFOCUSED = CHAT.num("Unfocused height (px)", 20, 180, 90, 1);
	public static final Setting.Num CHAT_WIDTH = CHAT.num("Width (px)", 40, 320, 170, 1);

	public static final Module WAYPOINTS = reg(new Module("waypoints", "Waypoints", "Separate waypoints for every world and server", Category.WORLD, Safety.PUBLIC_SAFE, true));
	public static final Setting.Key WP_ADD_KEY = WAYPOINTS.keybind("Add waypoint here (key)", GLFW.GLFW_KEY_B);
	public static final Setting.Bool WP_DEATH = WAYPOINTS.bool("Waypoint where you die", true);
	public static final Setting.Bool WP_DIST = WAYPOINTS.bool("Show distance", true);
	public static final Setting.Num WP_SCALE = WAYPOINTS.num("Scale", 0.4, 2.0, 0.75, 0.05);
	public static final Setting.Num WP_RANGE = WAYPOINTS.num("Hide beyond (0 = never)", 0, 5000, 0, 50);

	public static final Module MAP_PREVIEW = reg(new Module("map_preview", "Map Preview", "Hover a map in your inventory to see it", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Choice MP_ANCHOR = MAP_PREVIEW.choice("Position", 0, "Follow mouse", "Top Left", "Top Right", "Bottom Left", "Bottom Right");
	public static final Setting.Num MP_SCALE = MAP_PREVIEW.num("Scale", 0.4, 2.0, 0.8, 0.05);
	public static final Setting.Num MP_OX = MAP_PREVIEW.num("Offset X", -300, 300, 0, 1);
	public static final Setting.Num MP_OY = MAP_PREVIEW.num("Offset Y", -300, 300, 0, 1);

	static {
		// nametag extras that must not work on public servers
		PT_ARMOR.restricted = true;
		PT_HELD.restricted = true;
	}

	// =====================================================================
	// MORE MODULES (part 3)
	// =====================================================================
	public static final Module CLOCK = reg(new Module("clock", "Clock", "Shows the real time on screen", Category.HUD, Safety.PUBLIC_SAFE, true));
	public static final HudPos CLOCK_POS = new HudPos(CLOCK, "Clock", 2, -4, 4, 0.9);
	public static final Setting.Color CLOCK_COLOR = CLOCK.color("Text color", 0xFFFFFF);
	public static final Setting.Bool CLOCK_24H = CLOCK.bool("24-hour format", true);
	public static final Setting.Bool CLOCK_SECONDS = CLOCK.bool("Show seconds", false);
	public static final Setting.Bool CLOCK_GAME = CLOCK.bool("Also show in-game time", false);
	public static final Setting.Bool CLOCK_BG = CLOCK.bool("Background", true);

	public static final Module SPOTIFY = reg(new Module("spotify", "Spotify Mini Player", "Shows the current song and controls playback (Windows)", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos SPOTIFY_POS = new HudPos(SPOTIFY, "Spotify", 3, 8, -34, 1.0);
	public static final Setting.Color SP_COLOR = SPOTIFY.color("Accent color", 0x1DB954);
	public static final Setting.Key SP_PREV = SPOTIFY.keybind("Previous track key", -1);
	public static final Setting.Key SP_PLAY = SPOTIFY.keybind("Play / pause key", -1);
	public static final Setting.Key SP_NEXT = SPOTIFY.keybind("Next track key", -1);
	public static final Setting.Bool SP_HIDE_IDLE = SPOTIFY.bool("Hide when Spotify is closed", true);

	public static final Module COPY_NAME = reg(new Module("copy_name", "Copy Player Name", "Copies the name of the nearest player to your clipboard", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Key CN_KEY = COPY_NAME.keybind("Copy key", GLFW.GLFW_KEY_N);
	public static final Setting.Num CN_RANGE = COPY_NAME.num("Range", 2, 64, 10, 1);
	public static final Setting.Bool CN_HINT = COPY_NAME.bool("Show hint under the crosshair", true);

	public static final Module RECONNECT = reg(new Module("auto_reconnect", "Auto Reconnect", "Reconnects after you get disconnected", Category.UTILITY, Safety.PUBLIC_SAFE, false));
	public static final Setting.Num RC_DELAY = RECONNECT.num("Delay (seconds)", 3, 120, 8, 1);
	public static final Setting.Num RC_MAX = RECONNECT.num("Max attempts in a row", 1, 20, 5, 1);

	public static final Module SCREENSHOTS = reg(new Module("screenshots", "Screenshot Gallery", "Browse your screenshots here", Category.UTILITY, Safety.PUBLIC_SAFE, true).alwaysOn());

	public static final Module AUTO_XP = reg(new Module("auto_xp", "Auto XP", "Throws XP bottles to repair worn gear (restricted)", Category.PLAYER, Safety.RESTRICTED, false));
	public static final Setting.Num AX_WEAR = AUTO_XP.num("Throw when gear is worn above %", 5, 95, 40, 5);
	public static final Setting.Num AX_DELAY = AUTO_XP.num("Delay (ticks)", 2, 40, 6, 1);

	public static final Module AUTOCRAFT = reg(new Module("autocrafter", "Auto Crafter", "Fills a crafting table with your layout (restricted macro)", Category.UTILITY, Safety.RESTRICTED, false));
	public static final Setting.Bool[] AC_ON = new Setting.Bool[9];
	public static final Setting.Text[] AC_ITEM = new Setting.Text[9];
	static {
		for (int i = 0; i < 9; i++) {
			AC_ON[i] = AUTOCRAFT.bool("Field " + (i + 1) + " enabled", false);
			AC_ITEM[i] = AUTOCRAFT.text("Field " + (i + 1) + " item", "");
		}
	}
	public static final Setting.Num AC_COUNT = AUTOCRAFT.num("Crafts to make", 1, 64, 1, 1);
	public static final Setting.Bool AC_TAKE = AUTOCRAFT.bool("Take the result automatically", true);
	public static final Setting.Num AC_DELAY = AUTOCRAFT.num("Delay (ticks)", 1, 20, 4, 1);
	public static final Setting.Key AC_KEY = AUTOCRAFT.keybind("Run key (in a crafting table)", -1);
	public static final Setting.Bool AC_AUTO = AUTOCRAFT.bool("Run automatically when a table opens", true);

	public static final Module REDSTONE = reg(new Module("redstone", "Redstone Tweaks", "Shows the signal strength on redstone dust", Category.WORLD, Safety.PUBLIC_SAFE, false));
	public static final Setting.Num RS_RANGE = REDSTONE.num("Range", 3, 16, 8, 1);
	public static final Setting.Color RS_COLOR = REDSTONE.color("Text color", 0xFF3B30);
	public static final Setting.Bool RS_ZERO = REDSTONE.bool("Also show 0", false);
	public static final Setting.Num RS_SCALE = REDSTONE.num("Scale", 0.4, 2.0, 0.7, 0.05);

	public static final Module ALWAYS_DAY = reg(new Module("always_day", "Always Day", "Keeps the sky at daytime (visual only)", Category.WORLD, Safety.PUBLIC_SAFE, false));

	public static final Module XRAY = reg(new Module("xray", "Xray", "Shows only chosen blocks (restricted)", Category.WORLD, Safety.RESTRICTED, false));
	public static final Setting.Items XRAY_BLOCKS = XRAY.items("Visible blocks (ids)", false,
			"diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "gold_ore", "deepslate_gold_ore",
			"iron_ore", "deepslate_iron_ore", "coal_ore", "deepslate_coal_ore", "redstone_ore", "deepslate_redstone_ore",
			"lapis_ore", "deepslate_lapis_ore", "copper_ore", "deepslate_copper_ore", "ancient_debris", "nether_quartz_ore",
			"nether_gold_ore", "spawner", "chest", "lava");
	public static final Setting.Bool XRAY_BRIGHT = XRAY.bool("Also use fullbright", true);

	// =====================================================================
	// GLOBAL
	// =====================================================================
	/** Turns every module that is off on and every module that is on off. Locked modules stay untouched. */
	public static void invertAll() {
		for (Module m : ALL) {
			if (m.noToggle || m.isLocked()) continue;
			m.setEnabled(!m.isEnabled());
		}
	}

	// =====================================================================
	// MORE MODULES (part 4)
	// =====================================================================
	public static final Module POTION = reg(new Module("potion_hud", "Potion HUD", "Active effects with time left, plus your breath underwater", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos POTION_POS = new HudPos(POTION, "Potion HUD", 0, 4, 60, 0.9);
	public static final Setting.Num PO_WARN = POTION.num("Warn when below (seconds)", 5, 120, 20, 5);
	public static final Setting.Bool PO_AIR = POTION.bool("Show breath / air", true);

	public static final Module BEACON = reg(new Module("beacon_info", "Beacon Info", "Shows beacon level and the area it covers", Category.WORLD, Safety.PUBLIC_SAFE, false));
	public static final Setting.Color BC_COLOR = BEACON.color("Color", 0x00E5FF);
	public static final Setting.Bool BC_OUTLINE = BEACON.bool("Draw the covered area", true);
	public static final Setting.Num BC_RANGE = BEACON.num("Look for beacons within (blocks)", 16, 128, 64, 1);

	public static final Module TRIDENT_BOOST = reg(new Module("trident_boost", "Trident Boost", "Stronger riptide launch (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Num TB_POWER = TRIDENT_BOOST.num("Boost (x)", 1.0, 3.0, 1.5, 0.05);

	public static final Module FAST_SPRINT = reg(new Module("fast_sprint", "Fast Sprint", "Walk much faster while sprinting (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Num FS_SPEED = FAST_SPRINT.num("Speed (blocks per tick)", 0.3, 3.0, 0.7, 0.05);

	public static final Module SCAFFOLD = reg(new Module("scaffold", "Scaffold", "Places blocks under your feet (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Choice SC_MODE = SCAFFOLD.choice("Method", 0, "Normal", "Tower (hold jump)", "Expand", "Look ahead");
	public static final Setting.Num SC_EXPAND = SCAFFOLD.num("Expand distance (blocks)", 1, 4, 2, 1);
	public static final Setting.Num SC_DELAY = SCAFFOLD.num("Delay (ticks)", 1, 10, 2, 1);

	public static final Module FAST_PLACE = reg(new Module("fast_place", "Fast Place", "No delay between block placements (restricted)", Category.PLAYER, Safety.RESTRICTED, false));

	public static final Module CHEST_TOOLS = reg(new Module("chest_tools", "Chest Steal / Dump", "Buttons to move everything out of or into a chest (restricted)", Category.UTILITY, Safety.RESTRICTED, false));
	public static final Setting.Key CT_STEAL_KEY = CHEST_TOOLS.keybind("Steal key", -1);
	public static final Setting.Key CT_DUMP_KEY = CHEST_TOOLS.keybind("Dump key", -1);
	public static final Setting.Num CT_DELAY = CHEST_TOOLS.num("Ticks between items", 1, 20, 3, 1);

	public static final Module FAKE_NAME = reg(new Module("fake_name", "Fake Name", "Shows a different name on your own nametag (only on your screen)", Category.PLAYER, Safety.PUBLIC_SAFE, false));
	public static final Setting.Text FN_NAME = FAKE_NAME.text("Name to show", "Player");

	// =====================================================================
	// AUTO SET HOME
	// =====================================================================
	public static final Module AUTO_HOME = reg(new Module("auto_home", "Auto Set Home", "One key runs your commands in steps: 2, then 2, then 1, then 1", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Key AH_KEY = AUTO_HOME.keybind("Run key", -1);
	public static final Setting.Key AH_RESET_KEY = AUTO_HOME.keybind("Reset key (back to step 1)", -1);
	public static final Setting.Text[] AH_CMD = new Setting.Text[6];
	static {
		String[] label = {"Step 1, command 1", "Step 1, command 2", "Step 2, command 1", "Step 2, command 2", "Step 3 command", "Step 4 command"};
		for (int i = 0; i < 6; i++) AH_CMD[i] = AUTO_HOME.text(label[i], "");
	}
	public static final Setting.Num AH_DELAY = AUTO_HOME.num("Delay between commands (ms)", 100, 5000, 500, 50);
	public static final Setting.Bool AH_WRAP = AUTO_HOME.bool("Start over after step 4", false);
	public static final Setting.Bool AH_NOTIFY = AUTO_HOME.bool("Show a message for each step", true);

	// =====================================================================
	// MORE MODULES (part 5)
	// =====================================================================
	public static final Module SERVER_PROFILES = reg(new Module("server_profiles", "Server Profiles", "Loads a config profile automatically when you join a server", Category.UTILITY, Safety.PUBLIC_SAFE, false));
	public static final Setting.Items SP_ENTRIES = SERVER_PROFILES.items("Rules: server=profile (use singleplayer=profile for all worlds)", true);

	public static final Module DEATH_INFO = reg(new Module("death_info", "Death Info", "Chat message with your death coordinates and the cause", Category.UTILITY, Safety.PUBLIC_SAFE, true));

	public static final Module ALERTS = reg(new Module("alerts", "Alerts", "Sound and flash for low health, worn armor and used totems", Category.HUD, Safety.PUBLIC_SAFE, true));
	public static final Setting.Num AL_HEALTH = ALERTS.num("Low health below (hearts)", 1, 10, 4, 1);
	public static final Setting.Num AL_ARMOR = ALERTS.num("Armor nearly broken below %", 1, 50, 10, 1);
	public static final Setting.Bool AL_SOUND = ALERTS.bool("Play a sound", true);
	public static final Setting.Bool AL_FLASH = ALERTS.bool("Flash the screen edges", true);
	public static final Setting.Bool AL_TOTEM = ALERTS.bool("Tell me when a totem is used", true);
	public static final Setting.Color AL_COLOR = ALERTS.color("Flash color", 0xFF2D2D);

	public static final Module MINIMAP = reg(new Module("minimap", "Minimap", "Small top-down map of your surroundings", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos MM_POS = new HudPos(MINIMAP, "Minimap", 2, -6, 6, 1.0);
	public static final Setting.Num MM_RADIUS = MINIMAP.num("Radius (blocks)", 16, 64, 32, 4);
	public static final Setting.Num MM_SIZE = MINIMAP.num("Size (px)", 48, 160, 80, 4);
	public static final Setting.Bool MM_WAYPOINTS = MINIMAP.bool("Show waypoints", true);

	public static final Module CPS = reg(new Module("cps", "CPS Counter", "Clicks per second (left / right)", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos CPS_POS = new HudPos(CPS, "CPS", 2, -6, 100, 1.0);
	public static final Setting.Color CPS_COLOR = CPS.color("Text color", 0xFFFFFF);

	public static final Module DAMAGE_NUMBERS = reg(new Module("damage_numbers", "Damage Numbers", "Floating numbers when something takes damage", Category.VISUALS, Safety.PUBLIC_SAFE, false));
	public static final Setting.Color DN_COLOR = DAMAGE_NUMBERS.color("Color", 0xFF5555);
	public static final Setting.Num DN_SCALE = DAMAGE_NUMBERS.num("Scale", 0.4, 2.0, 0.8, 0.05);

	public static final Module CHAT_TOOLS = reg(new Module("chat_tools", "Chat Tools", "Timestamps, name highlight and a searchable history", Category.UTILITY, Safety.PUBLIC_SAFE, true));
	public static final Setting.Bool CT_TIME = CHAT_TOOLS.bool("Timestamps", true);
	public static final Setting.Bool CT_24H = CHAT_TOOLS.bool("24-hour timestamps", true);
	public static final Setting.Bool CT_HIGHLIGHT = CHAT_TOOLS.bool("Mark messages that mention you", true);
	public static final Setting.Color CT_HL_COLOR = CHAT_TOOLS.color("Mention color", 0xFFD60A);

	public static final Module TOGGLE_MOVE = reg(new Module("toggle_move", "Toggle Sneak / Sprint", "Press once instead of holding", Category.PLAYER, Safety.PUBLIC_SAFE, false));
	public static final Setting.Bool TM_SNEAK = TOGGLE_MOVE.bool("Toggle sneak", true);
	public static final Setting.Bool TM_SPRINT = TOGGLE_MOVE.bool("Toggle sprint", true);

	public static final Module COMPASS = reg(new Module("compass", "Compass Bar", "Direction strip with your waypoints", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos COMPASS_POS = new HudPos(COMPASS, "Compass", 1, 0, 4, 1.0);
	public static final Setting.Bool CMP_WP = COMPASS.bool("Show waypoints", true);
	public static final Setting.Color CMP_COLOR = COMPASS.color("Color", 0xFFFFFF);

	public static final Module AUTO_EAT = reg(new Module("auto_eat", "Auto Eat", "Eats food from your hotbar when you are hungry (restricted)", Category.PLAYER, Safety.RESTRICTED, false));
	public static final Setting.Num AE_LEVEL = AUTO_EAT.num("Eat when hunger is at or below", 1, 19, 14, 1);
	public static final Setting.Bool AE_SKIP_GOLD = AUTO_EAT.bool("Keep golden apples", true);

	public static final Module AUTO_ARMOR = reg(new Module("auto_armor", "Auto Armor", "Wears the best armor in your inventory (restricted)", Category.PLAYER, Safety.RESTRICTED, false));
	public static final Setting.Num AA_DELAY = AUTO_ARMOR.num("Delay (ticks)", 5, 60, 15, 1);

	public static final Module AUTO_TOOL = reg(new Module("auto_tool", "Auto Tool", "Switches to the fastest hotbar tool while mining (restricted)", Category.PLAYER, Safety.RESTRICTED, false));
	public static final Setting.Bool AT_BACK = AUTO_TOOL.bool("Switch back afterwards", true);

	public static final Module SAFE_WALK = reg(new Module("safe_walk", "Safe Walk", "Stops you from walking off edges (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));

	public static final Module ELYTRA_FLY = reg(new Module("elytra_fly", "Elytra Fly", "Hold yourself level while gliding (restricted)", Category.MOVEMENT, Safety.RESTRICTED, false));
	public static final Setting.Num ELF_SPEED = ELYTRA_FLY.num("Speed", 0.2, 3.0, 1.0, 0.05);
	public static final Setting.Num ELF_VSPEED = ELYTRA_FLY.num("Up / down speed", 0.1, 2.0, 0.5, 0.05);

	public static final Module NOTIF_HISTORY = reg(new Module("notif_history", "Notification History", "Everything Ruined Client told you recently", Category.UTILITY, Safety.PUBLIC_SAFE, true).alwaysOn());

	// =====================================================================
	// MORE MODULES (part 6)
	// =====================================================================
	public static final Module NO_HURTCAM = reg(new Module("no_hurtcam", "No Hurt Cam", "No screen shake when you take damage", Category.VISUALS, Safety.PUBLIC_SAFE, true));

	public static final Module PARTICLES = reg(new Module("particles", "Particles", "Show fewer particles", Category.VISUALS, Safety.PUBLIC_SAFE, false));
	public static final Setting.Choice PA_LEVEL = PARTICLES.choice("Amount", 1, "All", "Decreased", "Minimal");

	public static final Module BPS = reg(new Module("bps", "BPS", "Your speed in blocks per second", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos BPS_POS = new HudPos(BPS, "BPS", 2, -6, 120, 1.0);
	public static final Setting.Color BPS_COLOR = BPS.color("Text color", 0xFFFFFF);
	public static final Setting.Bool BPS_BG = BPS.bool("Background", true);

	public static final Module ARRAYLIST = reg(new Module("arraylist", "Module List", "Lists the modules that are switched on", Category.HUD, Safety.PUBLIC_SAFE, false));
	public static final HudPos AL_POS = new HudPos(ARRAYLIST, "Module List", 2, -4, 150, 0.9);
	public static final Setting.Color ARR_COLOR = ARRAYLIST.color("Text color", 0xFFFFFF);
	public static final Setting.Bool ARR_BG = ARRAYLIST.bool("Background", true);
	public static final Setting.Bool ARR_KEYS = ARRAYLIST.bool("Show bound keys", false);

	public static final Module DEATH_SOUND = reg(new Module("death_sound", "Death Sound", "Plays your own sound when you die (WAV, up to 5 seconds)", Category.UTILITY, Safety.PUBLIC_SAFE, false));
	public static final Setting.Text DS_FILE = DEATH_SOUND.text("File name (in config/kelpclient/sounds)", "death.wav");
	public static final Setting.Num DS_VOLUME = DEATH_SOUND.num("Volume %", 5, 100, 80, 5);

	public static final Module STREAMER = reg(new Module("streamer_mode", "Streamer Mode", "Hides private info (coordinates, waypoints, server address) while you record", Category.UTILITY, Safety.PUBLIC_SAFE, false));
	public static final Setting.Bool SM_COORDS = STREAMER.bool("Hide coordinates", true);
	public static final Setting.Bool SM_WAYPOINTS = STREAMER.bool("Hide waypoints", true);
	public static final Setting.Bool SM_SERVER = STREAMER.bool("Hide server address (Discord)", true);
	public static final Setting.Bool SM_INDICATOR = STREAMER.bool("Show STREAMER MODE badge", true);

	public static final Module INV_TINT = reg(new Module("inv_tint", "Inventory Tint", "Colors the background of inventories and chests", Category.VISUALS, Safety.PUBLIC_SAFE, false));
	public static final Setting.Color IT_TINT = INV_TINT.color("Color", 0x1E2A4A);
	public static final Setting.Num IT_ALPHA = INV_TINT.num("Opacity % (max 50)", 0, 50, 35, 1);
}
