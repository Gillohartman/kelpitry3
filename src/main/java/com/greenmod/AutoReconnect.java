package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** After a disconnect, counts down and joins the same server again. Esc cancels. */
public final class AutoReconnect {
	private static ServerData lastServer;
	private static long disconnectedAt;
	private static int attempts;
	private static long connectedSince;
	private static boolean cancelled;

	private AutoReconnect() {}

	/** Must run every tick, also on menu screens. */
	public static void tick(Minecraft mc) {
		if (!Modules.RECONNECT.isActive()) return;
		long now = System.currentTimeMillis();

		if (mc.level != null && mc.player != null && !mc.hasSingleplayerServer()) {
			ServerData sd = mc.getCurrentServer();
			if (sd != null && !sd.isLan()) {
				lastServer = sd;
				if (connectedSince == 0) connectedSince = now;
				if (now - connectedSince > 15000) attempts = 0;
			}
			disconnectedAt = 0;
			cancelled = false;
			return;
		}
		connectedSince = 0;

		if (mc.screen instanceof DisconnectedScreen && lastServer != null && !cancelled) {
			if (Keys.down(GLFW.GLFW_KEY_ESCAPE)) { cancelled = true; return; }
			if (disconnectedAt == 0) disconnectedAt = now;
			if (attempts >= Modules.RC_MAX.i()) return;
			if (now - disconnectedAt >= Modules.RC_DELAY.value * 1000) {
				attempts++;
				disconnectedAt = 0;
				reconnect(mc);
			}
		} else if (!(mc.screen instanceof DisconnectedScreen)) {
			disconnectedAt = 0;
		}
	}

	private static void reconnect(Minecraft mc) {
		try {
			for (Method m : ConnectScreen.class.getDeclaredMethods()) {
				if (!m.getName().equals("startConnecting") || !Modifier.isStatic(m.getModifiers())) continue;
				Class<?>[] t = m.getParameterTypes();
				Object[] a = new Object[t.length];
				for (int i = 0; i < t.length; i++) {
					if (Screen.class.isAssignableFrom(t[i])) a[i] = new TitleScreen();
					else if (t[i] == Minecraft.class) a[i] = mc;
					else if (t[i] == ServerAddress.class) a[i] = ServerAddress.parseString(lastServer.ip);
					else if (t[i] == ServerData.class) a[i] = lastServer;
					else if (t[i] == boolean.class) a[i] = false;
					else a[i] = null;
				}
				m.setAccessible(true);
				m.invoke(null, a);
				return;
			}
		} catch (Throwable ignored) {
		}
	}

	/** Countdown text on the disconnect screen (drawn from a screen mixin). */
	public static void drawCountdown(GuiGraphicsExtractor g, Screen screen) {
		if (!Modules.RECONNECT.isActive() || !(screen instanceof DisconnectedScreen) || lastServer == null) return;
		Minecraft mc = Minecraft.getInstance();
		String s;
		if (cancelled) s = "Auto reconnect cancelled";
		else if (attempts >= Modules.RC_MAX.i()) s = "Auto reconnect gave up after " + attempts + " attempts";
		else {
			long left = disconnectedAt == 0 ? (long) Modules.RC_DELAY.value : Math.max(0, (long) Math.ceil(Modules.RC_DELAY.value - (System.currentTimeMillis() - disconnectedAt) / 1000.0));
			s = "Reconnecting in " + left + "s  (Esc to cancel)";
		}
		int w = mc.font.width(s) + 16;
		int x = (mc.getWindow().getGuiScaledWidth() - w) / 2;
		Ui.rr(g, x, 8, w, 16, 6, Ui.a(0x111418, 0.9));
		g.text(mc.font, s, x + 8, 12, Ui.opaque(Ui.accent()));
	}
}
