package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Ruined Client menu. Sidebar with categories, search, module cards and a settings page per module.
 * Input is polled via GLFW (mouse + keys); text comes from charTyped and scrolling from mouseScrolled.
 */
public class GreenGui extends Screen {
	private static final int WW = 470, WH = 290, SIDE = 112, TOP = 44, BOTTOM = 282, CX = 124, CW = 336, RH = 28;
	private static final int[] PALETTE = {
			0x39FF14, 0xFF3B30, 0xFF9500, 0xFFD60A, 0x30D158, 0x00E5FF,
			0x0A84FF, 0xBF5AF2, 0xFF2D95, 0xFFFFFF, 0xAAAAAA, 0x000000
	};

	private Category cat = Category.VISUALS;
	private Module open;
	private Macros.Macro openMacro;
	private File shotOpen;

	private float scroll, scrollTarget, contentH, openAnim;
	private float dt;
	private long lastNs = System.nanoTime();

	private boolean prevL, prevR;
	private final boolean[] prevKey = new boolean[349];
	private boolean cL, cR, cLDown;
	private float lx, ly;

	private final StringBuilder search = new StringBuilder();
	private boolean searchFocus;

	private Object listening;      // Module | Setting.Key | Macros.Macro
	private Object inputTarget;    // Setting.Text | Setting.Items | Setting.BlockColors | Macros.Macro | Macros.Step | Waypoints.Waypoint | String
	private final StringBuilder input = new StringBuilder();

	private Setting.Num dragNum;
	private Object pickerKey;
	private final float[] hsv = new float[3];
	private int dragStrip = -1;
	private int paint = -1;
	private boolean confirmReset;

	private final Screen parent;
	private final StringBuilder chatQuery = new StringBuilder();

	public GreenGui() { this(null); }

	public GreenGui(Screen parent) {
		super(Component.literal("Ruined Client"));
		this.parent = parent;
		prevL = Keys.mouse(0);
		prevR = Keys.mouse(1);
		for (int k : Keys.VALID) prevKey[k] = Keys.down(k);
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean shouldCloseOnEsc() { return false; }

	@Override
	public void onClose() {
		Config.flush();
		Waypoints.save();
		CrosshairPixels.save();
		if (parent != null && this.minecraft != null) this.minecraft.setScreen(parent);
		else super.onClose();
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		int cp = event.codepoint();
		if (cp < 32 || cp == 127) return super.charTyped(event);
		if (inputTarget != null) {
			if (input.length() < 120) input.appendCodePoint(cp);
			return true;
		}
		if (searchFocus) {
			if (search.length() < 40) search.appendCodePoint(cp);
			scrollTarget = 0;
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scrollTarget -= (float) scrollY * 28f;
		clampScroll();
		return true;
	}

	private void clampScroll() {
		float max = Math.max(0, contentH - (BOTTOM - TOP));
		scrollTarget = Math.max(0, Math.min(max, scrollTarget));
	}

	private void resetScroll() {
		scroll = 0;
		scrollTarget = 0;
		pickerKey = null;
	}

	// ---------------------------------------------------------------- colors / helpers
	private int acc() { return Ui.accent(); }
	private int rad() { return Modules.GUI_RADIUS.i(); }
	private int txt() { return Modules.GUI_TEXT.rgb; }
	private int dim() { return Ui.mix(Modules.GUI_TEXT.rgb, Modules.GUI_BG.rgb, 0.45); }
	private int panel() { return Ui.mix(Modules.GUI_BG.rgb, 0xFFFFFF, 0.03); }
	private int card() { return Ui.mix(Modules.GUI_BG.rgb, 0xFFFFFF, 0.08); }
	private int cardHover() { return Ui.mix(Modules.GUI_BG.rgb, 0xFFFFFF, 0.14); }
	private int field() { return Ui.mix(card(), 0, 0.25); }

	private boolean busy() { return listening != null || inputTarget != null || searchFocus; }
	private boolean vis(int y, int h) { return y + h > TOP - 2 && y + h <= BOTTOM + 2; }
	private boolean hov(int x, int y, int w, int h) { return lx >= x && lx < x + w && ly >= y && ly < y + h; }
	private boolean hovC(int x, int y, int w, int h) { return hov(x, y, w, h) && y >= TOP - 1 && y + h <= BOTTOM + 4; }
	private boolean click(int x, int y, int w, int h) { return cL && !busy() && hovC(x, y, w, h); }
	private boolean rclick(int x, int y, int w, int h) { return cR && !busy() && hovC(x, y, w, h); }
	private boolean blink() { return System.currentTimeMillis() / 400 % 2 == 0; }

	private void text(GuiGraphicsExtractor g, String s, int x, int y, int rgb) { Ui.text(g, this.font, s, x, y, rgb); }
	private void small(GuiGraphicsExtractor g, String s, int x, int y, int rgb) { Ui.small(g, this.font, s, x, y, rgb, 0.75f); }
	private void textR(GuiGraphicsExtractor g, String s, int rightX, int y, int rgb) { text(g, s, rightX - this.font.width(s), y, rgb); }

	private void sw(GuiGraphicsExtractor g, int x, int y, float anim, boolean ro) {
		int track = Ui.mix(0x2B3038, acc(), anim * 0.85);
		if (ro) track = Ui.mix(track, Modules.GUI_BG.rgb, 0.55);
		Ui.rr(g, x, y, 28, 12, 6, Ui.opaque(track));
		Ui.rrOutline(g, x, y, 28, 12, 6, Ui.a(ro ? 0x888888 : Ui.mix(0xFFFFFF, acc(), anim), ro ? 0.25 : 0.55));
		int kx = x + 2 + Math.round(anim * 16);
		Ui.rr(g, kx, y + 2, 8, 8, 4, Ui.opaque(ro ? 0x7A7A7A : 0xFFFFFF));
	}

	/** A thin-outlined rounded card. */
	private void cardBox(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, boolean hv) {
		cardBox(g, x, y, w, h, r, hv);
		Ui.rrOutline(g, x, y, w, h, r, Ui.a(hv ? acc() : 0xFFFFFF, hv ? 0.5 : 0.09));
	}

	private boolean btn(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean primary) {
		boolean hv = hovC(x, y, w, h);
		int r = Math.min(Math.max(rad(), 4), h / 2);
		int bg = primary ? Ui.mix(Modules.GUI_BG.rgb, acc(), hv ? 0.5 : 0.3) : (hv ? cardHover() : card());
		Ui.rr(g, x, y, w, h, r, Ui.opaque(bg));
		Ui.rrOutline(g, x, y, w, h, r, Ui.a(primary || hv ? acc() : 0xFFFFFF, primary ? 0.7 : hv ? 0.5 : 0.12));
		text(g, label, x + (w - this.font.width(label)) / 2, y + (h - 8) / 2, txt());
		boolean c = click(x, y, w, h);
		if (c) Ui.clickSound();
		return c;
	}

	private void xbtn(GuiGraphicsExtractor g, int x, int y) {
		boolean xh = hovC(x, y, 14, 14);
		Ui.rr(g, x, y, 14, 14, 7, Ui.opaque(xh ? 0x8E3B3B : Ui.mix(card(), 0, 0.25)));
		Ui.rrOutline(g, x, y, 14, 14, 7, Ui.a(xh ? 0xFF7777 : 0xFFFFFF, xh ? 0.6 : 0.12));
		small(g, "x", x + 5, y + 4, 0xFFFFFF);
	}

	private void openModule(Module m) {
		open = m;
		shotOpen = null;
		resetScroll();
	}

	private float smooth(float cur, float target) {
		if (!Modules.GUI_ANIM.value) return target;
		return cur + (target - cur) * Math.min(1f, dt * 14f);
	}

	// ---------------------------------------------------------------- frame
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		dt = Math.min(0.05f, (now - lastNs) / 1e9f);
		lastNs = now;
		openAnim = Modules.GUI_ANIM.value ? openAnim + (1f - openAnim) * Math.min(1f, dt * 12f) : 1f;
		scroll = Modules.GUI_ANIM.value ? scroll + (scrollTarget - scroll) * Math.min(1f, dt * 14f) : scrollTarget;

		// One design unit = a whole number of real screen pixels, so text and lines stay crisp.
		double gs = Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
		int base = Math.max(1, Math.round((float) (this.height * gs) / 560f));
		float unit = Math.max(1f, Math.round(base * (float) Modules.GUI_SCALE.value));
		float sc = (float) (unit / gs);
		float ox = (this.width - WW * sc) / 2f;
		float oy = (this.height - WH * sc) / 2f + (1f - openAnim) * 14f * sc;
		lx = (mouseX - ox) / sc;
		ly = (mouseY - oy) / sc;

		boolean l = Keys.mouse(0), r = Keys.mouse(1);
		cL = l && !prevL;
		cR = r && !prevR;
		cLDown = l;
		prevL = l;
		prevR = r;
		if (!l) { dragNum = null; dragStrip = -1; paint = -1; }
		if (cL && (searchFocus || inputTarget != null)) {
			// clicking anywhere else ends editing; widgets that want it re-focus below
			if (!hov(CX, TOP + 4, CW, 24) || inputTarget != null) { /* handled by widgets */ }
		}

		int pressed = -1;
		for (int k : Keys.VALID) {
			boolean d = Keys.down(k);
			if (d && !prevKey[k] && pressed == -1) pressed = k;
			prevKey[k] = d;
		}
		if (pressed != -1) {
			if (inputTarget != null) {
				if (pressed == GLFW.GLFW_KEY_BACKSPACE && input.length() > 0) input.setLength(input.length() - 1);
				else if (pressed == GLFW.GLFW_KEY_ENTER || pressed == GLFW.GLFW_KEY_KP_ENTER) commitInput();
				else if (pressed == GLFW.GLFW_KEY_ESCAPE) { inputTarget = null; input.setLength(0); }
			} else if (searchFocus) {
				if (pressed == GLFW.GLFW_KEY_BACKSPACE && search.length() > 0) search.setLength(search.length() - 1);
				else if (pressed == GLFW.GLFW_KEY_ENTER || pressed == GLFW.GLFW_KEY_KP_ENTER) searchFocus = false;
				else if (pressed == GLFW.GLFW_KEY_ESCAPE) { searchFocus = false; }
			} else if (listening != null) {
				int bound = pressed == GLFW.GLFW_KEY_ESCAPE ? -1 : pressed;
				if (listening instanceof Module m) m.key = bound;
				else if (listening instanceof Setting.Key ks) ks.key = bound;
				else if (listening instanceof Macros.Macro mm) mm.key = bound;
				listening = null;
			} else if (pressed == GLFW.GLFW_KEY_RIGHT_SHIFT) {
				onClose();
				return;
			} else if (pressed == GLFW.GLFW_KEY_ESCAPE) {
				if (shotOpen != null) { shotOpen = null; resetScroll(); }
				else if (open != null || openMacro != null) { open = null; openMacro = null; resetScroll(); }
				else { onClose(); return; }
			}
		}

		g.fill(0, 0, this.width, this.height, Ui.a(0x000000, 0.5));

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(ox, oy);
		pose.scale(sc, sc);

		Ui.rr(g, 0, 0, WW, WH, rad(), Ui.a(Modules.GUI_BG.rgb, Modules.GUI_OPACITY.value / 100.0));
		Ui.rrOutline(g, 0, 0, WW, WH, rad(), Ui.a(acc(), 0.45));

		drawContent(g);
		clampScroll();

		g.fill(0, rad(), WW, TOP, Ui.opaque(panel()));
		Ui.rr(g, 0, 0, WW, TOP, rad(), Ui.opaque(panel()));
		Ui.hline(g, 1, TOP, WW - 2, Ui.a(acc(), 0.35));
		drawHeader(g);
		drawSidebar(g);

		pose.popMatrix();
	}

	private void drawHeader(GuiGraphicsExtractor g) {
		Ui.orb(g, 12, 9, 22);
		text(g, "Ruined Client", 40, 10, 0xFFFFFF);
		small(g, "v" + GreenModClient.VERSION + "  by ocxh", 40, 24, dim());
		String played = String.format(Locale.ROOT, "Played with Ruined Client: %.1f h", Config.playSeconds / 3600.0);
		textR(g, played, WW - 14, 10, txt());
		String env = "Environment: " + ServerSafety.label() + (ServerSafety.allowsRestricted() ? "" : "  (restricted modules locked)");
		Ui.small(g, this.font, env, WW - 14 - this.font.width(env) * 0.75f, 24, ServerSafety.allowsRestricted() ? 0x7CFC9A : 0xE57373, 0.75f);
	}

	private void drawSidebar(GuiGraphicsExtractor g) {
		g.fill(0, TOP, SIDE, WH - rad(), Ui.opaque(panel()));
		Ui.rr(g, 0, WH - 2 * rad() - 4, SIDE, 2 * rad() + 4, rad(), Ui.opaque(panel()));
		int y = TOP + 8;
		for (Category c : Category.values()) {
			boolean sel = c == cat && search.length() == 0;
			boolean hv = hov(8, y, SIDE - 16, 22);
			if (sel) {
				Ui.rr(g, 8, y, SIDE - 16, 22, Math.min(rad(), 8), Ui.a(acc(), 0.16));
				Ui.rrOutline(g, 8, y, SIDE - 16, 22, Math.min(rad(), 8), Ui.a(acc(), 0.55));
			} else if (hv) {
				Ui.rr(g, 8, y, SIDE - 16, 22, Math.min(rad(), 8), Ui.a(0xFFFFFF, 0.05));
			}
			text(g, c.title, 20, y + 7, sel ? acc() : txt());
			if (cL && !busy() && hv) {
				cat = c;
				search.setLength(0);
				open = null;
				openMacro = null;
				shotOpen = null;
				confirmReset = false;
				resetScroll();
			}
			y += 24;
		}
	}

	private void drawContent(GuiGraphicsExtractor g) {
		int y = TOP + 8 - Math.round(scroll);
		int end;
		if (openMacro != null) end = macroEditor(g, openMacro, y);
		else if (open != null) end = modulePage(g, open, y);
		else end = categoryList(g, y);
		contentH = end + Math.round(scroll) - TOP + 8;
	}

	// ---------------------------------------------------------------- list + search
	private int searchBar(GuiGraphicsExtractor g, int y) {
		if (vis(y, 22)) {
			boolean hv = hovC(CX, y, CW, 22);
			Ui.rr(g, CX, y, CW, 22, rad(), Ui.opaque(searchFocus ? Ui.mix(card(), acc(), 0.25) : (hv ? cardHover() : card())));
			String label = search.length() == 0 && !searchFocus ? "Search modules and settings..." : search + (searchFocus && blink() ? "|" : "");
			text(g, label, CX + 10, y + 7, search.length() == 0 && !searchFocus ? dim() : txt());
			if (search.length() > 0) {
				xbtn(g, CX + CW - 20, y + 4);
				if (click(CX + CW - 20, y + 4, 14, 14)) { search.setLength(0); searchFocus = false; scrollTarget = 0; }
			}
			if (cL && !busy() && hv && !(search.length() > 0 && hov(CX + CW - 20, y + 4, 14, 14))) searchFocus = true;
			else if (cL && searchFocus && !hv) searchFocus = false;
		}
		return y + 28;
	}

	private int categoryList(GuiGraphicsExtractor g, int y) {
		y = searchBar(g, y);

		if (search.length() > 0) {
			String q = search.toString().toLowerCase(Locale.ROOT);
			int found = 0;
			for (Module m : Modules.ALL) {
				if (!matches(m, q)) continue;
				found++;
				y = moduleRow(g, m, y);
			}
			if (found == 0) {
				if (vis(y, 20)) text(g, "Nothing matches \"" + search + "\".", CX + 4, y + 4, dim());
				y += 24;
			}
			return y;
		}

		if (cat == Category.SETTINGS) y = profilesCard(g, y);
		if (cat == Category.HUD) {
			if (vis(y, 24)) {
				if (btn(g, CX, y, CW, 24, "Move HUD elements (drag and drop)", true)) {
					Config.save();
					this.minecraft.setScreen(new HudEditor());
					return y;
				}
			}
			y += 30;
		}
		List<Module> mods = Modules.byCategory(cat);
		for (Module m : mods) y = moduleRow(g, m, y);
		if (cat == Category.MACRO) y = macroList(g, y);
		if (mods.isEmpty()) {
			if (vis(y, 20)) text(g, "Nothing here yet.", CX + 4, y + 4, dim());
			y += 24;
		}
		return y;
	}

	private boolean matches(Module m, String q) {
		if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) return true;
		for (Setting s : m.settings) if (s.name.toLowerCase(Locale.ROOT).contains(q)) return true;
		return false;
	}

	private int moduleRow(GuiGraphicsExtractor g, Module m, int y) {
		m.anim = smooth(m.anim, m.isEnabled() ? 1f : 0f);
		if (vis(y, RH)) {
			boolean noWorld = m.needsWorld && Minecraft.getInstance().level == null;
			boolean locked = m.isLocked();
			boolean blocked = locked || noWorld;
			boolean hv = hovC(CX, y, CW, RH);
			cardBox(g, CX, y, CW, RH, rad(), hv);
			if (m.isEnabled() && !m.noToggle && !blocked) Ui.rr(g, CX + 1, y + 7, 2, RH - 14, 1, Ui.opaque(acc()));
			text(g, m.name, CX + 10, y + 5, blocked ? dim() : txt());
			String sub = locked ? ServerSafety.lockReason() : noWorld ? "Requires a world" : m.description;
			small(g, sub, CX + 10, y + 17, locked ? 0xE57373 : dim());

			boolean inSwitch = false;
			if (!m.noToggle) {
				int sx = CX + CW - 40;
				sw(g, sx, y + 8, m.anim, blocked);
				inSwitch = hov(sx - 2, y + 4, 32, 20);
				int rx = sx - 8;
				if (locked) {
					Ui.lock(g, rx - 7, y + 9, 0xE57373);
					rx -= 14;
				}
				if (m.key >= 0) {
					String k = Keys.name(m.key);
					small(g, k, rx - (int) (this.font.width(k) * 0.75f), y + 10, 0xBBBBBB);
				}
			}
			if (click(CX, y, CW, RH)) {
				if (inSwitch && !m.noToggle) {
					if (locked) Notifications.push("This feature is disabled on this server.");
					else if (noWorld) Notifications.push("Requires a world.");
					else { m.toggle(); Ui.clickSound(); }
				} else openModule(m);
			}
			if (rclick(CX, y, CW, RH)) openModule(m);
		}
		return y + RH + 4;
	}

	// ---------------------------------------------------------------- module page
	private int modulePage(GuiGraphicsExtractor g, Module m, int y) {
		boolean noWorld = m.needsWorld && Minecraft.getInstance().level == null;
		boolean ro = m.isLocked() || noWorld;
		m.anim = smooth(m.anim, m.isEnabled() ? 1f : 0f);

		if (vis(y, 18)) {
			if (btn(g, CX, y, 54, 18, "< Back", false)) { open = null; shotOpen = null; resetScroll(); return y; }
			text(g, m.name, CX + 66, y + 5, acc());
		}
		y += 24;
		if (vis(y, 12)) small(g, m.description, CX + 2, y, dim());
		y += 14;

		if (ro) {
			if (vis(y, 20)) {
				Ui.rr(g, CX, y, CW, 20, rad(), Ui.a(0xE57373, 0.18));
				Ui.lock(g, CX + 8, y + 5, 0xE57373);
				text(g, (noWorld && !m.isLocked() ? "Requires a world" : ServerSafety.lockReason()) + " - settings are read-only", CX + 22, y + 6, 0xE57373);
			}
			y += 26;
		}

		if (m == Modules.SCREENSHOTS) return gallery(g, y);

		if (!m.noToggle) {
			if (vis(y, 26)) {
				cardBox(g, CX, y, CW, 26, rad(), false);
				text(g, "Enabled", CX + 10, y + 9, txt());
				sw(g, CX + CW - 40, y + 7, m.anim, ro);
				if (click(CX, y, CW, 26)) {
					if (ro) Notifications.push("This feature is disabled on this server.");
					else m.toggle();
				}
			}
			y += 30;

			if (vis(y, 26)) {
				cardBox(g, CX, y, CW, 26, rad(), false);
				text(g, "Toggle key", CX + 10, y + 9, txt());
				String label = listening == m ? "press a key..." : Keys.name(m.key);
				int bw = Math.max(54, this.font.width(label) + 14);
				boolean bhv = hovC(CX + CW - bw - 10, y + 4, bw, 18);
				Ui.rr(g, CX + CW - bw - 10, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(listening == m ? Ui.mix(card(), acc(), 0.4) : (bhv ? cardHover() : field())));
				text(g, label, CX + CW - bw - 10 + (bw - this.font.width(label)) / 2, y + 9, txt());
				if (click(CX + CW - bw - 10, y + 4, bw, 18) && !ro) listening = m;
			}
			y += 30;
		}

		// special panels
		if (m == Modules.CROSSHAIR) y = crosshairPanel(g, y, ro);
		if (m == Modules.WAYPOINTS) y = waypointPanel(g, y);
		if (m == Modules.CHUNK_FINDER) y = chunkPanel(g, y);
		if (m == Modules.AUTOCRAFT) y = craftGrid(g, y, ro);
		if (m == Modules.GUI) y = themeRow(g, y);
		if (m == Modules.CHAT_TOOLS) y = chatSearchPanel(g, y);
		if (m == Modules.NOTIF_HISTORY) return historyPanel(g, y);

		for (Setting s : m.settings) {
			if (m == Modules.AUTOCRAFT && s.name.startsWith("Field ")) continue;
			y = settingRow(g, m, s, y, ro);
		}

		if (vis(y, 22)) {
			if (btn(g, CX, y, 120, 22, "Reset " + (m.name.length() > 14 ? "module" : m.name), false) && !ro) {
				m.reset();
				pickerKey = null;
				Notifications.push(m.name + " reset to defaults.");
			}
		}
		return y + 30;
	}

	private int settingRow(GuiGraphicsExtractor g, Module m, Setting s, int y, boolean roModule) {
		boolean restrictedHere = s.restricted && !ServerSafety.allowsRestricted();
		boolean ro = roModule || restrictedHere;
		boolean can = !ro;
		int rx = CX + CW - 28;

		if (s instanceof Setting.Items it) return itemsRow(g, it, y, ro);
		if (s instanceof Setting.BlockColors bc) return blockColorsRow(g, bc, y, ro);

		int h = s instanceof Setting.Num ? 36 : 26;
		int extra = (pickerKey == s && s instanceof Setting.Color) ? 80 : 0;
		if (!vis(y, h)) return y + h + 4 + extra;

		cardBox(g, CX, y, CW, h, rad(), false);
		String label = s.name + (restrictedHere ? " (restricted)" : "");
		text(g, label, CX + 10, y + 9 - (s instanceof Setting.Num ? 3 : 0), ro ? dim() : txt());
		if (restrictedHere) Ui.lock(g, CX + 12 + this.font.width(label), y + 8 - (s instanceof Setting.Num ? 3 : 0), 0xE57373);

		int bx = CX + CW - 20, by = y + (h - 14) / 2;
		boolean rh = hovC(bx, by, 14, 14);
		Ui.rr(g, bx, by, 14, 14, Math.min(rad(), 7), Ui.opaque(rh ? cardHover() : Ui.mix(card(), 0, 0.2)));
		small(g, "R", bx + 5, by + 4, dim());
		if (click(bx, by, 14, 14) && can) s.reset();

		if (s instanceof Setting.Bool b) {
			sw(g, rx - 28, y + 7, b.value ? 1f : 0f, ro);
			if (click(CX, y, CW - 26, h) && can) b.value = !b.value;
		} else if (s instanceof Setting.Num n) {
			String val = n.step >= 1 ? String.valueOf(n.i()) : String.format(Locale.ROOT, "%.2f", n.value);
			textR(g, val, rx, y + 6, txt());
			int tx = CX + 10, tw = CW - 46, ty = y + 22;
			double frac = (n.value - n.min) / (n.max - n.min);
			Ui.rr(g, tx, ty + 2, tw, 2, 1, Ui.opaque(Ui.mix(card(), 0xFFFFFF, 0.12)));
			int fw = Math.max(4, (int) (tw * frac));
			Ui.rr(g, tx, ty + 2, fw, 2, 1, Ui.opaque(ro ? dim() : acc()));
			Ui.rr(g, tx + fw - 4, ty - 1, 8, 8, 4, Ui.opaque(ro ? 0x7A7A7A : 0xFFFFFF));
			Ui.rrOutline(g, tx + fw - 4, ty - 1, 8, 8, 4, Ui.a(ro ? 0x888888 : acc(), 0.8));
			if (can && cL && !busy() && hovC(tx - 4, ty - 6, tw + 8, 18)) dragNum = n;
			if (can && dragNum == n && cLDown) {
				double f = Math.max(0.0, Math.min(1.0, (lx - tx) / (double) tw));
				double v = n.min + f * (n.max - n.min);
				v = Math.round(v / n.step) * n.step;
				n.value = Math.max(n.min, Math.min(n.max, v));
			}
		} else if (s instanceof Setting.Choice c) {
			String lbl = "<  " + c.get() + "  >";
			int bw = this.font.width(lbl) + 14;
			int px = rx - bw;
			boolean hv = hovC(px, y + 4, bw, 18);
			Ui.rr(g, px, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(hv ? cardHover() : field()));
			text(g, lbl, px + 7, y + 9, txt());
			if (click(px, y + 4, bw, 18) && can) c.next(1);
			if (rclick(px, y + 4, bw, 18) && can) c.next(-1);
		} else if (s instanceof Setting.Key k) {
			String lbl = listening == k ? "press a key..." : Keys.name(k.key);
			int bw = Math.max(54, this.font.width(lbl) + 14);
			int px = rx - bw;
			boolean hv = hovC(px, y + 4, bw, 18);
			Ui.rr(g, px, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(listening == k ? Ui.mix(card(), acc(), 0.4) : (hv ? cardHover() : field())));
			text(g, lbl, px + (bw - this.font.width(lbl)) / 2, y + 9, txt());
			if (click(px, y + 4, bw, 18) && can) listening = k;
		} else if (s instanceof Setting.Text t) {
			boolean editing = inputTarget == t;
			String lbl = editing ? input + (blink() ? "|" : "") : (t.value.isEmpty() ? "(click to edit)" : t.value);
			if (lbl.length() > 28) lbl = "..." + lbl.substring(lbl.length() - 25);
			int bw = Math.max(110, this.font.width(lbl) + 14);
			int px = rx - bw;
			boolean hv = hovC(px, y + 4, bw, 18);
			Ui.rr(g, px, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : (hv ? cardHover() : field())));
			text(g, lbl, px + 7, y + 9, t.value.isEmpty() && !editing ? dim() : txt());
			if (click(px, y + 4, bw, 18) && can) {
				inputTarget = t;
				input.setLength(0);
				input.append(t.value);
			}
		} else if (s instanceof Setting.Color c) {
			int px = rx - 40;
			boolean hv = hovC(px, y + 4, 40, 18);
			Ui.rr(g, px - 1, y + 3, 42, 20, Math.min(rad(), 9), Ui.opaque(hv ? 0xFFFFFF : Ui.mix(card(), 0xFFFFFF, 0.3)));
			Ui.rr(g, px, y + 4, 40, 18, Math.min(rad(), 9), Ui.opaque(c.rgb));
			if (click(px, y + 4, 40, 18) && can) togglePicker(c, c.rgb);
			boolean hexEdit = inputTarget == c;
			String hex = hexEdit ? "#" + input + (blink() ? "|" : "") : String.format("#%06X", c.rgb);
			int hw = this.font.width(hex) + 10;
			boolean hh = hovC(px - hw - 6, y + 4, hw, 18);
			Ui.rr(g, px - hw - 6, y + 4, hw, 18, Math.min(rad(), 9), Ui.opaque(hexEdit ? Ui.mix(card(), acc(), 0.25) : (hh ? cardHover() : field())));
			text(g, hex, px - hw - 1, y + 9, txt());
			if (click(px - hw - 6, y + 4, hw, 18) && can) { inputTarget = c; input.setLength(0); }
			if (pickerKey == c) {
				colorPicker(g, c.rgb, v -> c.rgb = v, y + h + 4, can);
				return y + h + 4 + 76 + 4;
			}
		}
		return y + h + 4;
	}

	private void togglePicker(Object key, int rgb) {
		if (pickerKey == key) { pickerKey = null; return; }
		pickerKey = key;
		float[] v = Ui.toHsv(rgb);
		hsv[0] = v[0]; hsv[1] = v[1]; hsv[2] = v[2];
	}

	private void colorPicker(GuiGraphicsExtractor g, int cur, IntConsumer set, int y, boolean can) {
		if (!vis(y, 76)) return;
		Ui.rr(g, CX, y, CW, 76, rad(), Ui.opaque(Ui.mix(card(), 0, 0.15)));
		for (int i = 0; i < PALETTE.length; i++) {
			int sx = CX + 10 + i * 26;
			Ui.rr(g, sx, y + 6, 22, 14, 4, Ui.opaque(PALETTE[i]));
			if (click(sx, y + 6, 22, 14) && can) {
				set.accept(PALETTE[i]);
				float[] v = Ui.toHsv(PALETTE[i]);
				hsv[0] = v[0]; hsv[1] = v[1]; hsv[2] = v[2];
			}
		}
		int sx0 = CX + 10, sw = CW - 20;
		for (int strip = 0; strip < 3; strip++) {
			int sy = y + 26 + strip * 16;
			for (int i = 0; i < sw; i += 2) {
				float f = i / (float) sw;
				int col = strip == 0 ? Ui.hsv(f, 1f, 1f) : strip == 1 ? Ui.hsv(hsv[0], f, hsv[2]) : Ui.hsv(hsv[0], hsv[1], f);
				g.fill(sx0 + i, sy, sx0 + i + 2, sy + 10, Ui.opaque(col));
			}
			int mx = sx0 + (int) (hsv[strip] * sw);
			g.fill(mx - 1, sy - 2, mx + 1, sy + 12, 0xFFFFFFFF);
			if (can && cL && !busy() && hovC(sx0 - 3, sy - 3, sw + 6, 16)) dragStrip = strip;
			if (can && dragStrip == strip && cLDown) {
				hsv[strip] = Math.max(0f, Math.min(strip == 0 ? 0.999f : 1f, (lx - sx0) / (float) sw));
				set.accept(Ui.hsv(hsv[0], hsv[1], hsv[2]));
			}
		}
	}

	private int blockColorsRow(GuiGraphicsExtractor g, Setting.BlockColors bc, int y, boolean ro) {
		boolean can = !ro;
		if (vis(y, 24)) {
			cardBox(g, CX, y, CW, 24, rad(), false);
			text(g, bc.name + " (" + bc.values.size() + ")", CX + 10, y + 8, ro ? dim() : txt());
			int bx = CX + CW - 20;
			boolean rh = hovC(bx, y + 5, 14, 14);
			Ui.rr(g, bx, y + 5, 14, 14, Math.min(rad(), 7), Ui.opaque(rh ? cardHover() : Ui.mix(card(), 0, 0.2)));
			small(g, "R", bx + 5, y + 9, dim());
			if (click(bx, y + 5, 14, 14) && can) bc.reset();
		}
		y += 26;

		for (int i = 0; i < bc.values.size(); i++) {
			Setting.BlockColors.Entry e = bc.values.get(i);
			boolean open = pickerKey == e;
			if (vis(y, 22)) {
				Ui.rr(g, CX + 8, y, CW - 16, 22, Math.min(rad(), 8), Ui.opaque(Ui.mix(card(), 0, 0.1)));
				boolean sh = hovC(CX + 14, y + 3, 22, 16);
				Ui.rr(g, CX + 14, y + 3, 22, 16, 4, Ui.opaque(sh ? 0xFFFFFF : e.rgb));
				if (sh) Ui.rr(g, CX + 15, y + 4, 20, 14, 3, Ui.opaque(e.rgb));
				text(g, e.id, CX + 44, y + 7, e.on ? txt() : dim());
				int xx = CX + CW - 34;
				sw(g, xx - 36, y + 5, e.on ? 1f : 0f, ro);
				xbtn(g, xx, y + 4);
				if (click(xx - 38, y + 3, 32, 16) && can) e.on = !e.on;
				if (click(xx, y + 4, 14, 14) && can) { bc.values.remove(i); return y + 24; }
				if (click(CX + 14, y + 3, 22, 16) && can) togglePicker(e, e.rgb);
			}
			y += 24;
			if (open) {
				final Setting.BlockColors.Entry ee = e;
				colorPicker(g, e.rgb, v -> ee.rgb = v, y, can);
				y += 80;
			}
		}

		if (vis(y, 22)) {
			boolean editing = inputTarget == bc;
			boolean hv = hovC(CX + 8, y, CW - 16, 22);
			Ui.rr(g, CX + 8, y, CW - 16, 22, Math.min(rad(), 8), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : (hv ? cardHover() : field())));
			String label = editing ? input + (blink() ? "|" : "") : "+ Add block (click, type id like gold_ore, Enter)";
			text(g, label, CX + 16, y + 7, editing ? txt() : dim());
			if (click(CX + 8, y, CW - 16, 22) && can) { inputTarget = bc; input.setLength(0); }
		}
		return y + 30;
	}

	private int itemsRow(GuiGraphicsExtractor g, Setting.Items it, int y, boolean ro) {
		boolean can = !ro;
		if (vis(y, 24)) {
			cardBox(g, CX, y, CW, 24, rad(), false);
			text(g, it.name + " (" + it.values.size() + ")", CX + 10, y + 8, ro ? dim() : txt());
			int bx = CX + CW - 20;
			boolean rh = hovC(bx, y + 5, 14, 14);
			Ui.rr(g, bx, y + 5, 14, 14, Math.min(rad(), 7), Ui.opaque(rh ? cardHover() : Ui.mix(card(), 0, 0.2)));
			small(g, "R", bx + 5, y + 9, dim());
			if (click(bx, y + 5, 14, 14) && can) it.reset();
		}
		y += 26;

		for (int i = 0; i < it.values.size(); i++) {
			String e = it.values.get(i);
			if (vis(y, 20)) {
				boolean on = it.isOn(e);
				Ui.rr(g, CX + 8, y, CW - 16, 20, Math.min(rad(), 8), Ui.opaque(Ui.mix(card(), 0, 0.1)));
				text(g, Setting.Items.clean(e), CX + 16, y + 6, on ? txt() : dim());
				int xx = CX + CW - 34;
				xbtn(g, xx, y + 3);
				if (it.toggleable) {
					sw(g, xx - 36, y + 4, on ? 1f : 0f, ro);
					if (click(xx - 38, y + 2, 32, 16) && can) {
						it.values.set(i, on ? "-" + e : Setting.Items.clean(e));
						return y + 22;
					}
				}
				if (click(xx, y + 3, 14, 14) && can) {
					it.values.remove(i);
					return y + 22;
				}
			}
			y += 22;
		}

		if (vis(y, 22)) {
			boolean editing = inputTarget == it;
			boolean hv = hovC(CX + 8, y, CW - 16, 22);
			Ui.rr(g, CX + 8, y, CW - 16, 22, Math.min(rad(), 8), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : (hv ? cardHover() : field())));
			String label = editing ? input + (blink() ? "|" : "") : "+ Add entry (click, type, press Enter)";
			text(g, label, CX + 16, y + 7, editing ? txt() : dim());
			if (click(CX + 8, y, CW - 16, 22) && can) { inputTarget = it; input.setLength(0); }
		}
		return y + 30;
	}

	// ---------------------------------------------------------------- special panels
	private int crosshairPanel(GuiGraphicsExtractor g, int y, boolean ro) {
		int cell = 10, n = CrosshairPixels.N;
		int h = 22 + n * cell + 34;
		if (vis(y, h) || vis(y, 24)) {
			cardBox(g, CX, y, CW, h, rad(), false);
			text(g, "Crosshair editor", CX + 10, y + 7, acc());
			int gx = CX + 10, gy = y + 22;
			// grid
			for (int row = 0; row < n; row++) for (int col = 0; col < n; col++) {
				int px = gx + col * cell, py = gy + row * cell;
				boolean on = CrosshairPixels.CURRENT[row][col];
				boolean c = row == n / 2 && col == n / 2;
				g.fill(px, py, px + cell - 1, py + cell - 1, on ? Ui.opaque(Modules.CROSS_COLOR.rgb) : (c ? 0xFF2A3038 : 0xFF1B2026));
				if (cL && !busy() && hovC(px, py, cell, cell) && !ro) paint = on ? 0 : 1;
				if (paint >= 0 && cLDown && !busy() && hov(px, py, cell, cell) && !ro) CrosshairPixels.CURRENT[row][col] = paint == 1;
			}
			// preview (3x)
			int prx = CX + 10 + n * cell + 14, pry = y + 22;
			Ui.rr(g, prx, pry, 90, 90, rad(), 0xFF0E1114);
			text(g, "Preview", prx + 4, pry + 4, dim());
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate(prx + 45f, pry + 52f);
			pose.scale(2f, 2f);
			HitboxHud.drawCrosshair(g, 0, 0);
			pose.popMatrix();
			// buttons
			int bx = prx, by = pry + 98;
			if (btn(g, bx, by, 42, 18, "Clear", false) && !ro) CrosshairPixels.clear();
			if (btn(g, bx + 46, by, 44, 18, "Invert", false) && !ro) CrosshairPixels.invert();
			if (btn(g, bx, by + 22, 90, 18, Modules.CROSS_PIXEL.value ? "Pixel crosshair: ON" : "Pixel crosshair: OFF", Modules.CROSS_PIXEL.value) && !ro) {
				Modules.CROSS_PIXEL.value = !Modules.CROSS_PIXEL.value;
			}
			boolean editing = "crosshairName".equals(inputTarget);
			boolean hv = hovC(bx, by + 44, 90, 18);
			Ui.rr(g, bx, by + 44, 90, 18, 9, Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : (hv ? cardHover() : field())));
			text(g, editing ? input + (blink() ? "|" : "") : "Save as...", bx + 6, by + 49, editing ? txt() : dim());
			if (click(bx, by + 44, 90, 18) && !ro) { inputTarget = "crosshairName"; input.setLength(0); }
			// saved list
			int sx = prx + 98;
			int sy = pry;
			text(g, "Saved", sx, sy + 4, dim());
			sy += 16;
			List<String> names = new ArrayList<>(CrosshairPixels.SAVED.keySet());
			for (int i = 0; i < names.size() && i < 7; i++) {
				String nm = names.get(i);
				Ui.rr(g, sx, sy, CW - (sx - CX) - 8, 16, 5, Ui.opaque(field()));
				String shown = nm.length() > 9 ? nm.substring(0, 8) + ".." : nm;
				small(g, shown, sx + 4, sy + 5, txt());
				int lx0 = CX + CW - 8 - 48;
				if (btn(g, lx0, sy + 1, 26, 14, "Use", false) && !ro) {
					CrosshairPixels.decode(CrosshairPixels.SAVED.get(nm));
					Modules.CROSS_PIXEL.value = true;
				}
				xbtn(g, lx0 + 30, sy + 1);
				if (click(lx0 + 30, sy + 1, 14, 14) && !ro) { CrosshairPixels.SAVED.remove(nm); CrosshairPixels.save(); return y + h + 4; }
				sy += 18;
			}
			if (names.isEmpty()) small(g, "No saved crosshairs yet.", sx, sy + 2, dim());
		}
		return y + h + 4;
	}

	private int waypointPanel(GuiGraphicsExtractor g, int y) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return y;
		List<Waypoints.Waypoint> list = Waypoints.forCurrent(mc);
		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			String where = Waypoints.worldKey(mc);
			if (where.length() > 34) where = where.substring(0, 33) + "..";
			text(g, "Waypoints: " + where, CX + 10, y + 9, acc());
			if (btn(g, CX + CW - 96, y + 4, 86, 18, "+ Add here", true)) Waypoints.addHere(mc, null);
		}
		y += 30;
		for (int i = 0; i < list.size(); i++) {
			Waypoints.Waypoint w = list.get(i);
			if (vis(y, 26)) {
				Ui.rr(g, CX + 8, y, CW - 16, 26, Math.min(rad(), 8), Ui.opaque(Ui.mix(card(), 0, 0.1)));
				boolean sh = hovC(CX + 14, y + 5, 16, 16);
				Ui.rr(g, CX + 14, y + 5, 16, 16, 4, Ui.opaque(w.rgb));
				if (click(CX + 14, y + 5, 16, 16)) { w.rgb = Waypoints.nextColor(w.rgb); Waypoints.save(); }
				boolean editing = inputTarget == w;
				String nm = editing ? input + (blink() ? "|" : "") : w.name;
				text(g, nm, CX + 36, y + 4, txt());
				small(g, w.x + ", " + w.y + ", " + w.z + "   " + shortDim(w.dim), CX + 36, y + 15, dim());
				if (click(CX + 36, y + 2, 140, 12)) { inputTarget = w; input.setLength(0); input.append(w.name); }
				int xx = CX + CW - 34;
				xbtn(g, xx, y + 6);
				sw(g, xx - 36, y + 7, w.visible ? 1f : 0f, false);
				if (click(xx - 38, y + 5, 32, 16)) { w.visible = !w.visible; Waypoints.save(); }
				if (click(xx, y + 6, 14, 14)) { list.remove(i); Waypoints.save(); return y + 28; }
			}
			y += 28;
		}
		if (list.isEmpty()) {
			if (vis(y, 16)) small(g, "No waypoints for this world yet. Press the add key or the button above.", CX + 10, y + 3, dim());
			y += 20;
		}
		return y + 4;
	}

	private String shortDim(String d) {
		if (d == null) return "";
		int i = d.lastIndexOf('/');
		String s = i >= 0 ? d.substring(i + 1) : d;
		return s.replace("]", "");
	}

	private int chunkPanel(GuiGraphicsExtractor g, int y) {
		if (vis(y, 60)) {
			cardBox(g, CX, y, CW, 60, rad(), false);
			text(g, "Observed chunks: " + ChunkFinder.observedChunks() + "     Estimated: " + ChunkFinder.estimatedChunks(), CX + 10, y + 8, txt());
			small(g, "Observed = real blocks / your own time. Estimated = counts above your threshold.", CX + 10, y + 22, dim());
			small(g, "Growth indicators also occur naturally, so an estimate is a hint, never proof.", CX + 10, y + 33, dim());
			if (btn(g, CX + CW - 130, y + 40, 120, 16, "Clear saved chunk data", false)) {
				ChunkFinder.clear();
				Notifications.push("Chunk Finder data cleared.");
			}
		}
		return y + 66;
	}

	private int craftGrid(GuiGraphicsExtractor g, int y, boolean ro) {
		int cw = 100, ch = 34;
		if (vis(y, 3 * (ch + 4) + 22)) {
			text(g, "Crafting layout  (left click = on/off, right click = set item)", CX + 4, y + 2, dim());
		}
		y += 16;
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				int i = r * 3 + c;
				int x = CX + c * (cw + 6) + 4;
				int yy = y + r * (ch + 4);
				if (!vis(yy, ch)) continue;
				boolean on = Modules.AC_ON[i].value;
				boolean hv = hovC(x, yy, cw, ch);
				boolean editing = inputTarget == Modules.AC_ITEM[i];
				Ui.rr(g, x, yy, cw, ch, rad(), Ui.opaque(on ? Ui.mix(card(), acc(), hv ? 0.4 : 0.28) : (hv ? cardHover() : card())));
				small(g, "Field " + (i + 1), x + 6, yy + 4, dim());
				String item = editing ? input + (blink() ? "|" : "") : (Modules.AC_ITEM[i].value.isEmpty() ? "(empty)" : Modules.AC_ITEM[i].value);
				if (item.length() > 15) item = item.substring(0, 14) + "..";
				text(g, item, x + 6, yy + 16, on ? txt() : dim());
				if (click(x, yy, cw, ch) && !ro) Modules.AC_ON[i].value = !on;
				if (rclick(x, yy, cw, ch) && !ro) {
					inputTarget = Modules.AC_ITEM[i];
					input.setLength(0);
					input.append(Modules.AC_ITEM[i].value);
				}
			}
		}
		return y + 3 * (ch + 4) + 6;
	}

	private int themeRow(GuiGraphicsExtractor g, int y) {
		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			text(g, "Theme: " + Modules.GUI_THEME.get(), CX + 10, y + 9, txt());
			if (btn(g, CX + CW - 66, y + 4, 56, 18, "Apply", true)) Themes.apply(Modules.GUI_THEME.index);
		}
		return y + 30;
	}

	private int chatSearchPanel(GuiGraphicsExtractor g, int y) {
		if (vis(y, 24)) {
			boolean editing = "chatSearch".equals(inputTarget);
			boolean hv = hovC(CX, y, CW, 22);
			Ui.rr(g, CX, y, CW, 22, rad(), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.2) : (hv ? cardHover() : field())));
			Ui.rrOutline(g, CX, y, CW, 22, rad(), Ui.a(0xFFFFFF, editing ? 0.5 : 0.12));
			String label = editing ? input + (blink() ? "|" : "") : (chatQuery.length() == 0 ? "Search the chat history (click, type, Enter)" : "Search: " + chatQuery);
			text(g, label, CX + 10, y + 7, editing || chatQuery.length() > 0 ? txt() : dim());
			if (click(CX, y, CW, 22)) { inputTarget = "chatSearch"; input.setLength(0); input.append(chatQuery); }
		}
		y += 28;
		List<String> hits = ChatLog.search(chatQuery.toString(), 10);
		for (String line : hits) {
			if (vis(y, 14)) {
				String t = line.length() > 62 ? line.substring(0, 61) + ".." : line;
				small(g, t, CX + 6, y + 3, txt());
			}
			y += 13;
		}
		if (hits.isEmpty()) {
			if (vis(y, 14)) small(g, "No chat lines yet.", CX + 6, y + 3, dim());
			y += 13;
		}
		return y + 8;
	}

	private int historyPanel(GuiGraphicsExtractor g, int y) {
		List<String> h = Notifications.history();
		if (h.isEmpty()) {
			if (vis(y, 14)) small(g, "Nothing yet.", CX + 6, y + 3, dim());
			return y + 20;
		}
		for (int i = h.size() - 1; i >= 0; i--) {
			if (vis(y, 14)) small(g, h.get(i), CX + 6, y + 3, txt());
			y += 13;
		}
		return y + 8;
	}

	// ---------------------------------------------------------------- screenshots
	private void drawPic(GuiGraphicsExtractor g, Screenshots.Pic p, int x, int y, int cell) {
		for (int row = 0; row < p.h; row++) {
			int start = 0;
			int cur = quant(p.argb[row * p.w]);
			for (int col = 1; col <= p.w; col++) {
				int c = col < p.w ? quant(p.argb[row * p.w + col]) : cur ^ 0x01000000;
				if (c != cur) {
					g.fill(x + start * cell, y + row * cell, x + col * cell, y + (row + 1) * cell, cur);
					start = col;
					cur = c;
				}
			}
		}
	}

	private int quant(int argb) { return (argb & 0xFFF0F0F0) | 0x00080808; }

	private int gallery(GuiGraphicsExtractor g, int y) {
		if (shotOpen != null) {
			if (vis(y, 20)) {
				if (btn(g, CX, y, 80, 18, "< Gallery", false)) { shotOpen = null; resetScroll(); return y; }
				text(g, shotOpen.getName(), CX + 90, y + 5, dim());
			}
			y += 24;
			Screenshots.Pic p = Screenshots.get(shotOpen, Screenshots.PW, Screenshots.PH);
			int cell = 3;
			if (vis(y, Screenshots.PH * cell)) {
				if (p == null) text(g, "Loading...", CX + 4, y + 8, dim());
				else drawPic(g, p, CX, y, cell);
			}
			return y + Screenshots.PH * cell + 8;
		}

		List<File> files = Screenshots.list();
		if (files.isEmpty()) {
			if (vis(y, 20)) text(g, "No screenshots found. Press F2 in the game to take one.", CX + 4, y + 4, dim());
			return y + 26;
		}
		int cols = 3, tw = 106, th = 70, cell = 2;
		int shown = Math.min(files.size(), 30);
		for (int i = 0; i < shown; i++) {
			int col = i % cols, row = i / cols;
			int x = CX + col * (tw + 6);
			int yy = y + row * (th + 6);
			if (!vis(yy, th)) continue;
			boolean hv = hovC(x, yy, tw, th);
			cardBox(g, x, yy, tw, th, rad(), hv);
			Screenshots.Pic p = Screenshots.get(files.get(i), Screenshots.TW, Screenshots.TH);
			if (p != null) drawPic(g, p, x + 9, yy + 4, cell);
			else small(g, "...", x + 50, yy + 22, dim());
			String nm = files.get(i).getName().replace(".png", "");
			if (nm.length() > 22) nm = nm.substring(0, 21) + "..";
			small(g, nm, x + 4, yy + 58, dim());
			if (click(x, yy, tw, th)) { shotOpen = files.get(i); resetScroll(); return y; }
		}
		return y + ((shown + cols - 1) / cols) * (th + 6) + 6;
	}

	// ---------------------------------------------------------------- profiles (Settings)
	private int profilesCard(GuiGraphicsExtractor g, int y) {
		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			text(g, "Profile: " + Config.profile, CX + 10, y + 9, acc());
			String played = String.format(Locale.ROOT, "%.1f h played", Config.playSeconds / 3600.0);
			textR(g, played, CX + CW - 10, y + 9, txt());
		}
		y += 30;

		if (vis(y, 22)) {
			if (btn(g, CX, y, 70, 22, "Save", true)) { Config.save(); Notifications.push("Profile saved."); }
			if (btn(g, CX + 76, y, 130, 22, confirmReset ? "Click again to confirm" : "Reset everything", false)) {
				if (confirmReset) {
					Modules.resetAll();
					Notifications.push("Everything was reset to defaults.");
					confirmReset = false;
				} else confirmReset = true;
			}
			if (btn(g, CX + 212, y, 124, 22, "Invert all modules", false)) {
				Modules.invertAll();
				Notifications.push("Every module was flipped (on <-> off).");
			}
		}
		y += 28;

		if (vis(y, 22)) {
			boolean editing = "profile".equals(inputTarget);
			boolean hv = hovC(CX, y, CW, 22);
			Ui.rr(g, CX, y, CW, 22, rad(), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : (hv ? cardHover() : card())));
			String label = editing ? input + (blink() ? "|" : "") : "+ New profile (click, type a name, press Enter)";
			text(g, label, CX + 10, y + 7, editing ? txt() : dim());
			if (click(CX, y, CW, 22)) { inputTarget = "profile"; input.setLength(0); }
		}
		y += 28;

		for (String p : Config.profiles()) {
			if (vis(y, 24)) {
				boolean cur = p.equals(Config.profile);
				cardBox(g, CX, y, CW, 24, rad(), false);
				if (cur) Ui.rr(g, CX, y + 5, 3, 14, 1, Ui.opaque(acc()));
				text(g, p, CX + 10, y + 8, cur ? acc() : txt());
				if (!cur) {
					if (btn(g, CX + CW - 126, y + 3, 56, 18, "Load", false)) {
						Config.switchProfile(p);
						Notifications.push("Loaded profile " + p + ".");
						return y;
					}
					if (btn(g, CX + CW - 64, y + 3, 56, 18, "Delete", false)) {
						Config.deleteProfile(p);
						return y;
					}
				} else small(g, "active", CX + CW - 50, y + 9, dim());
			}
			y += 28;
		}
		return y + 6;
	}

	// ---------------------------------------------------------------- macros
	private int macroList(GuiGraphicsExtractor g, int y) {
		if (vis(y, 22)) {
			text(g, "Your macros", CX + 4, y + 7, txt());
			if (btn(g, CX + CW - 90, y, 90, 22, "+ New macro", true)) {
				Macros.Macro m = new Macros.Macro();
				m.steps.add(new Macros.Step("", 250));
				Macros.LIST.add(m);
				openMacro = m;
				resetScroll();
				return y;
			}
		}
		y += 28;

		for (int i = 0; i < Macros.LIST.size(); i++) {
			Macros.Macro m = Macros.LIST.get(i);
			if (vis(y, RH)) {
				boolean hv = hovC(CX, y, CW, RH);
				cardBox(g, CX, y, CW, RH, rad(), hv);
				text(g, m.name, CX + 10, y + 5, m.enabled ? txt() : dim());
				small(g, m.steps.size() + " step(s)   key: " + Keys.name(m.key), CX + 10, y + 17, dim());
				int sx = CX + CW - 40;
				sw(g, sx, y + 8, m.enabled ? 1f : 0f, false);
				int xx = sx - 24;
				xbtn(g, xx, y + 7);
				if (click(xx, y + 7, 14, 14)) { Macros.LIST.remove(i); return y; }
				if (click(sx - 2, y + 4, 32, 20)) m.enabled = !m.enabled;
				else if (click(CX, y, CW, RH)) { openMacro = m; resetScroll(); return y; }
			}
			y += RH + 4;
		}
		if (Macros.LIST.isEmpty()) {
			if (vis(y, 20)) text(g, "No macros yet. Create one with the button above.", CX + 4, y + 4, dim());
			y += 24;
		}
		return y;
	}

	private int macroEditor(GuiGraphicsExtractor g, Macros.Macro m, int y) {
		if (vis(y, 18)) {
			if (btn(g, CX, y, 54, 18, "< Back", false)) { openMacro = null; resetScroll(); return y; }
			text(g, "Edit macro", CX + 66, y + 5, acc());
		}
		y += 26;

		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			text(g, "Name", CX + 10, y + 9, txt());
			boolean editing = inputTarget == m;
			String label = editing ? input + (blink() ? "|" : "") : m.name;
			int bw = Math.max(110, this.font.width(label) + 14);
			int px = CX + CW - bw - 10;
			Ui.rr(g, px, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : field()));
			text(g, label, px + 7, y + 9, txt());
			if (click(px, y + 4, bw, 18)) { inputTarget = m; input.setLength(0); input.append(m.name); }
		}
		y += 30;

		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			text(g, "Key", CX + 10, y + 9, txt());
			String label = listening == m ? "press a key..." : Keys.name(m.key);
			int bw = Math.max(54, this.font.width(label) + 14);
			int px = CX + CW - bw - 10;
			Ui.rr(g, px, y + 4, bw, 18, Math.min(rad(), 9), Ui.opaque(listening == m ? Ui.mix(card(), acc(), 0.4) : field()));
			text(g, label, px + (bw - this.font.width(label)) / 2, y + 9, txt());
			if (click(px, y + 4, bw, 18)) listening = m;
		}
		y += 30;

		if (vis(y, 26)) {
			cardBox(g, CX, y, CW, 26, rad(), false);
			text(g, "Enabled", CX + 10, y + 9, txt());
			sw(g, CX + CW - 40, y + 7, m.enabled ? 1f : 0f, false);
			if (click(CX, y, CW, 26)) m.enabled = !m.enabled;
		}
		y += 30;

		if (vis(y, 14)) small(g, "Steps run in order. Start a step with / for a command. Delay = wait before the step (ms).", CX + 2, y + 2, dim());
		y += 16;

		for (int i = 0; i < m.steps.size(); i++) {
			Macros.Step st = m.steps.get(i);
			if (vis(y, 26)) {
				cardBox(g, CX, y, CW, 26, rad(), false);
				small(g, String.valueOf(i + 1), CX + 8, y + 10, dim());
				boolean editing = inputTarget == st;
				String label = editing ? input + (blink() ? "|" : "") : (st.text.isEmpty() ? "(click to type)" : st.text);
				int fw = CW - 150;
				if (this.font.width(label) > fw - 12) {
					while (label.length() > 4 && this.font.width("..." + label) > fw - 12) label = label.substring(1);
					label = "..." + label;
				}
				Ui.rr(g, CX + 22, y + 4, fw, 18, Math.min(rad(), 9), Ui.opaque(editing ? Ui.mix(card(), acc(), 0.3) : field()));
				text(g, label, CX + 28, y + 9, st.text.isEmpty() && !editing ? dim() : txt());
				if (click(CX + 22, y + 4, fw, 18)) { inputTarget = st; input.setLength(0); input.append(st.text); }

				int dx = CX + 22 + fw + 6;
				int step = Keys.shift() ? 250 : 50;
				if (btn(g, dx, y + 4, 16, 18, "-", false)) st.delayMs = Math.max(0, st.delayMs - step);
				text(g, st.delayMs + "ms", dx + 20, y + 9, txt());
				if (btn(g, dx + 20 + this.font.width("0000ms") + 2, y + 4, 16, 18, "+", false)) st.delayMs = Math.min(10000, st.delayMs + step);
				int xx = CX + CW - 22;
				xbtn(g, xx, y + 6);
				if (click(xx, y + 6, 14, 14)) { m.steps.remove(i); return y; }
			}
			y += 30;
		}

		if (vis(y, 22)) {
			if (btn(g, CX, y, 100, 22, "+ Add step", true)) m.steps.add(new Macros.Step("", 250));
			if (btn(g, CX + 108, y, 110, 22, "Delete macro", false)) {
				Macros.LIST.remove(m);
				openMacro = null;
				resetScroll();
				return y;
			}
		}
		return y + 30;
	}

	// ---------------------------------------------------------------- text input
	private void commitInput() {
		String v = input.toString().trim();
		if (inputTarget instanceof Setting.Text t) {
			t.value = v;
		} else if (inputTarget instanceof Setting.Items it) {
			boolean dup = false;
			for (String e : it.values) if (Setting.Items.clean(e).equalsIgnoreCase(v)) dup = true;
			if (!v.isEmpty() && !dup) it.values.add(v);
		} else if (inputTarget instanceof Setting.BlockColors bc) {
			String id = v.toLowerCase(Locale.ROOT);
			if (id.startsWith("minecraft:")) id = id.substring(10);
			boolean dup = false;
			for (Setting.BlockColors.Entry e : bc.values) if (e.id.equals(id)) dup = true;
			if (!id.isEmpty() && !dup) bc.values.add(new Setting.BlockColors.Entry(id, PALETTE[bc.values.size() % PALETTE.length], true));
		} else if (inputTarget instanceof Macros.Macro m) {
			if (!v.isEmpty()) m.name = v;
		} else if (inputTarget instanceof Macros.Step s) {
			s.text = v;
		} else if (inputTarget instanceof Setting.Color col) {
			String hx = v.startsWith("#") ? v.substring(1) : v;
			try {
				if (hx.length() == 6) col.rgb = Integer.parseInt(hx, 16);
			} catch (NumberFormatException ignored) {
			}
		} else if ("chatSearch".equals(inputTarget)) {
			chatQuery.setLength(0);
			chatQuery.append(v);
		} else if (inputTarget instanceof Waypoints.Waypoint w) {
			if (!v.isEmpty()) { w.name = v; Waypoints.save(); }
		} else if ("profile".equals(inputTarget)) {
			if (!v.isEmpty()) Config.createProfile(v);
		} else if ("crosshairName".equals(inputTarget)) {
			if (!v.isEmpty()) { CrosshairPixels.SAVED.put(v, CrosshairPixels.encode()); CrosshairPixels.save(); }
		}
		inputTarget = null;
		input.setLength(0);
	}
}
