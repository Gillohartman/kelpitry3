package com.greenmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class GreenModClient implements ClientModInitializer {
	public static final String MOD_ID = "kelpclient";   // kept so existing configs keep working
	public static final String VERSION = "2.0.0";

	private static boolean prevOpen;
	private static boolean prevSort;
	private static Double savedGamma;
	private static int tickCount;

	@Override
	public void onInitializeClient() {
		Config.load();
		CrosshairPixels.load();
		DeathSound.folder();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), HitboxHud::render);

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
			if (Modules.CROSSHAIR.isActive()) Safe.run("crosshair", () -> HitboxHud.crosshair(graphics));
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive()) Safe.run("hearts", () -> HudColors.health(graphics, Minecraft.getInstance()));
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive()) Safe.run("hunger", () -> HudColors.food(graphics, Minecraft.getInstance()));
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive() && Modules.HC_XP.value) Safe.run("xp-bar", () -> HudColors.xpBar(graphics, Minecraft.getInstance()));
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive() && Modules.HC_XP.value) Safe.run("xp-level", () -> HudColors.level(graphics, Minecraft.getInstance()));
			else original.extractRenderState(graphics, delta);
		});

		// freecam needs the movement keys cleared BEFORE the player is ticked
		ClientTickEvents.START_CLIENT_TICK.register(mc -> Safe.tick("freecam-input", FreeView::startTick, mc));
		ClientTickEvents.END_CLIENT_TICK.register(GreenModClient::tick);

		// save as soon as you leave a world, and when the game closes
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Config.flush());

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			Safe.run("restore", () -> {
				ZoomFeature.restore();
				FovFeature.restore(client);
				OptionTweaks.restore(client);
				if (savedGamma != null) {
					client.options.gamma().set(Math.min(1.0, savedGamma));
					savedGamma = null;
				}
			});
			DiscordRpc.shutdown();
			ChunkFinder.save();
			Waypoints.save();
			CrosshairPixels.save();
			Config.flush();
		});
	}

	private static void tick(Minecraft mc) {
		// these run everywhere, also on the title screen and loading screens
		Safe.tick("config", m -> Config.tick(), mc);
		Safe.tick("auto-reconnect", AutoReconnect::tick, mc);

		// the menu opens from the title screen too
		boolean rs = Keys.down(GLFW.GLFW_KEY_RIGHT_SHIFT);
		if (rs && !prevOpen && (mc.screen == null || mc.screen instanceof TitleScreen)) {
			mc.setScreen(new GreenGui(mc.screen));
		}
		prevOpen = rs;

		if (mc.player == null || mc.level == null) return;

		// safety first: decide what may run before anything else does
		Safe.tick("safety", ServerSafety::tick, mc);

		if (++tickCount % 20 == 0) Config.playSeconds++;

		// module toggle keys (setEnabled enforces the safety lock)
		for (Module m : Modules.ALL) {
			if (m.key < 0 || m.noToggle) continue;
			boolean d = Keys.down(m.key);
			if (d && !m.lastDown && mc.screen == null) m.toggle();
			m.lastDown = d;
		}

		// inventory sort key (container screens only)
		boolean sd = Keys.down(Modules.SORT_KEY.key);
		if (Modules.SORT.isActive() && sd && !prevSort && mc.screen instanceof AbstractContainerScreen<?>) {
			if (!Modules.SORT_CTRL.value || Keys.ctrl()) Safe.run("sort", InventorySorter::sort);
		}
		prevSort = sd;

		if (Modules.REFILL.isActive()) Safe.tick("refill", InventorySorter::refillTick, mc);
		if (Modules.NO_BOB.isActive() && mc.options.bobView().get()) mc.options.bobView().set(false);

		Safe.tick("fullbright", GreenModClient::fullbright, mc);
		Safe.tick("fov", FovFeature::tick, mc);
		Safe.tick("zoom", m -> ZoomFeature.update(), mc);
		Safe.tick("freeview", FreeView::tick, mc);
		Safe.tick("movement", MovementFeatures::tick, mc);
		Safe.tick("actions", ActionModules::tick, mc);
		Safe.tick("auto-totem", AutoTotem::tick, mc);
		Safe.tick("auto-xp", AutoXp::tick, mc);
		Safe.tick("auto-crafter", AutoCrafter::tick, mc);
		Safe.tick("chest-tools", ChestTools::tick, mc);
		Safe.tick("aim", AimAssist::tick, mc);
		Safe.tick("block-esp", BlockEsp::tick, mc);
		Safe.tick("chunk-finder", ChunkFinder::tick, mc);
		Safe.tick("waypoints", Waypoints::tick, mc);
		Safe.tick("redstone", RedstoneTweaks::tick, mc);
		Safe.tick("beacons", BeaconInfo::tick, mc);
		Safe.tick("xray", Xray::tick, mc);
		Safe.tick("chat-settings", ChatSettings::tick, mc);
		Safe.tick("online", OnlineHub::tick, mc);
		Safe.tick("copy-name", CopyName::tick, mc);
		Safe.tick("spotify", SpotifyPlayer::tick, mc);
		Safe.tick("macros", Macros::tick, mc);
		Safe.tick("auto-home", AutoHome::tick, mc);
		Safe.tick("server-profiles", ServerProfiles::tick, mc);
		Safe.tick("death-info", DeathInfo::tick, mc);
		Safe.tick("death-sound", DeathSound::tick, mc);
		Safe.tick("alerts", Alerts::tick, mc);
		Safe.tick("minimap", Minimap::tick, mc);
		Safe.tick("cps", CpsCounter::tick, mc);
		Safe.tick("bps", BpsHud::tick, mc);
		Safe.tick("damage-numbers", DamageNumbers::tick, mc);
		Safe.tick("toggle-move", ToggleMove::tick, mc);
		Safe.tick("auto-eat", AutoEat::tick, mc);
		Safe.tick("auto-armor", AutoArmor::tick, mc);
		Safe.tick("auto-tool", AutoTool::tick, mc);
		Safe.tick("option-tweaks", OptionTweaks::tick, mc);
		Safe.tick("discord", DiscordRpc::tick, mc);

		if (Modules.SPRINT.isActive() && mc.screen == null && mc.options.keyUp.isDown()
				&& !mc.player.isShiftKeyDown() && !mc.player.isUsingItem()) {
			mc.options.keySprint.setDown(true);
		}
	}

	private static void fullbright(Minecraft mc) {
		OptionInstance<Double> opt = mc.options.gamma();
		if (Modules.FULLBRIGHT.isActive()) {
			if (savedGamma == null) savedGamma = Math.min(1.0, opt.get());
			if (opt.get() < 15.0) GreenMod.forceOption(opt, 16.0);
		} else if (savedGamma != null && !Xray.active()) {
			opt.set(Math.min(1.0, savedGamma));
			savedGamma = null;
		}
	}
}
