package com.greenmod;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** The main overlay: draws every world-space and HUD feature once per frame. Nothing draws outside a world. */
public final class HitboxHud {
	private HitboxHud() {}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer me = mc.player;
		ClientLevel level = mc.level;
		if (me == null || level == null) return;

		Safe.run("zoom", ZoomFeature::update);

		boolean needEntities = Modules.H_PLAYERS.module.isActive() || Modules.H_HOSTILE.module.isActive()
				|| Modules.H_PASSIVE.module.isActive() || Modules.H_ITEMS.module.isActive()
				|| Modules.PLAYER_TAGS.isActive() || Modules.MOB_TAGS.isActive() || Modules.ITEM_TAGS.isActive()
				|| Modules.ESP.isActive();
		boolean needWorld = needEntities || Modules.BLOCK_ESP.isActive() || Modules.CHUNK_FINDER.isActive()
				|| Modules.WAYPOINTS.isActive() || Modules.REDSTONE.isActive() || Modules.BEACON.isActive()
				|| Modules.CONTAINER_PEEK.isActive() || Modules.DAMAGE_NUMBERS.isActive();

		final boolean ok = needWorld && Projector.begin(delta);
		if (ok) {
			if (needEntities) {
				final List<Entity> list = level.getEntities(me, me.getBoundingBox().inflate(128.0), e -> true);
				Safe.run("hitboxes", () -> Hitboxes.render(g, me, list));
				Safe.run("entity-esp", () -> EntityEsp.render(g, me, list));
				Safe.run("nametags", () -> Nametags.render(g, mc, me, list));
			}
			Safe.run("block-esp", () -> BlockEsp.render(g, me));
			Safe.run("chunk-overlay", () -> ChunkFinder.renderWorld(g, me));
			Safe.run("chunk-labels", () -> ChunkFinder.renderLabels(g, mc, me));
			Safe.run("beacons", () -> BeaconInfo.render(g, mc));
			Safe.run("waypoints", () -> Waypoints.render(g, mc, me));
			Safe.run("redstone", () -> RedstoneTweaks.render(g, mc));
			Safe.run("container-peek", () -> ContainerPeek.render(g, mc, me));
			Safe.run("damage-numbers", () -> DamageNumbers.render(g, mc));
		}

		Safe.run("totem-glow", () -> TotemFeatures.renderHotbarGlow(g, mc, me));
		Safe.run("low-fire", () -> LowFire.render(g, mc));
		Safe.run("armor-hud", () -> ArmorHud.render(g, mc, me));
		Safe.run("totem-counter", () -> TotemFeatures.renderCounter(g, mc, me));
		Safe.run("info", () -> InfoHud.render(g, mc, me));
		Safe.run("online", () -> OnlineHub.render(g, mc));
		Safe.run("potions", () -> PotionHud.render(g, mc, me));
		Safe.run("clock", () -> ClockHud.render(g, mc));
		Safe.run("keystrokes", () -> Keystrokes.render(g, mc));
		Safe.run("cps", () -> CpsCounter.render(g, mc));
		Safe.run("bps", () -> BpsHud.render(g, mc));
		Safe.run("module-list", () -> ArrayListHud.render(g, mc));
		Safe.run("spotify", () -> SpotifyPlayer.render(g, mc));
		Safe.run("block-legend", () -> BlockEsp.renderLegend(g, mc));
		Safe.run("chunk-map", () -> ChunkFinder.renderMinimap(g, mc, me));
		Safe.run("minimap", () -> Minimap.render(g, mc, me));
		Safe.run("compass", () -> CompassBar.render(g, mc, me));
		Safe.run("alerts", () -> Alerts.render(g, mc));
		Safe.run("copy-name", () -> CopyName.render(g, mc));
		Safe.run("auto-home", () -> AutoHome.render(g, mc));
		Safe.run("streamer", () -> StreamerMode.render(g, mc));
		Safe.run("notifications", () -> Notifications.render(g, mc));
	}

	/** The crosshair that replaces the vanilla one. Hidden whenever the vanilla one would be hidden. */
	public static void crosshair(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.screen != null || mc.options.hideGui) return;
		if (!mc.options.getCameraType().isFirstPerson() || mc.player.isDeadOrDying() || mc.player.isSpectator()) return;
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		drawCrosshair(g, cx, cy);
	}

	private static int armColor(int arm, boolean outer) {
		double a = Modules.CROSS_ALPHA.value / 100.0;
		switch (Modules.CROSS_MODE.index) {
			case 1: return Ui.a(outer ? Modules.CROSS_C2.rgb : Modules.CROSS_COLOR.rgb, a);
			case 2: {
				int rgb = arm == 0 ? Modules.CROSS_COLOR.rgb : arm == 1 ? Modules.CROSS_C2.rgb : arm == 2 ? Modules.CROSS_C3.rgb : Modules.CROSS_C4.rgb;
				return Ui.a(rgb, a);
			}
			case 3: {
				float hue = (float) (((System.currentTimeMillis() / 1000.0) * 0.12 * Modules.CROSS_RAINBOW.value + arm * 0.25) % 1.0);
				return Ui.a(Ui.hsv(hue, 0.85f, 1f), a);
			}
			default: return Ui.a(Modules.CROSS_COLOR.rgb, a);
		}
	}

	private static void rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color, boolean outline) {
		if (x2 <= x1 || y2 <= y1) return;
		if (outline) g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xB0000000);
		g.fill(x1, y1, x2, y2, color);
	}

	/** Draws the configured crosshair centered on (cx, cy). Also used by the menu preview. */
	public static void drawCrosshair(GuiGraphicsExtractor g, int cx, int cy) {
		if (Modules.CROSS_PIXEL.value) {
			CrosshairPixels.draw(g, cx, cy, Modules.CROSS_PIXEL_SCALE.i(), armColor(0, false));
			return;
		}
		int gap = Modules.CROSS_GAP.i();
		int len = Modules.CROSS_LEN.i();
		int t = Modules.CROSS_THICK.i();
		int lo = t / 2;
		boolean ol = Modules.CROSS_OUTLINE.value;
		int inner = Math.max(1, len / 2), outer = len - inner;
		// arm 0 = top, 1 = right, 2 = bottom, 3 = left (inner half first, then outer half)
		rect(g, cx - lo, cy - gap - inner, cx - lo + t, cy - gap, armColor(0, false), ol);
		rect(g, cx - lo, cy - gap - len, cx - lo + t, cy - gap - inner, armColor(0, true), ol);
		rect(g, cx + gap + 1, cy - lo, cx + gap + 1 + inner, cy - lo + t, armColor(1, false), ol);
		rect(g, cx + gap + 1 + inner, cy - lo, cx + gap + 1 + len, cy - lo + t, armColor(1, true), ol);
		rect(g, cx - lo, cy + gap + 1, cx - lo + t, cy + gap + 1 + inner, armColor(2, false), ol);
		rect(g, cx - lo, cy + gap + 1 + inner, cx - lo + t, cy + gap + 1 + len, armColor(2, true), ol);
		rect(g, cx - gap - inner, cy - lo, cx - gap, cy - lo + t, armColor(3, false), ol);
		rect(g, cx - gap - len, cy - lo, cx - gap - inner, cy - lo + t, armColor(3, true), ol);
		if (Modules.CROSS_DOT.value) rect(g, cx - lo, cy - lo, cx - lo + t, cy - lo + t, armColor(0, false), ol);
	}
}
