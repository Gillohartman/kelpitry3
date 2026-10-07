package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** RESTRICTED: only the listed blocks are drawn. The face-culling mixin asks isVisible(). */
public final class Xray {
	private static Set<Block> visible = new HashSet<>();
	private static String key = "";
	private static boolean wasActive;
	private static Double savedGamma;

	private Xray() {}

	public static boolean active() { return Modules.XRAY.isActive(); }

	public static boolean isVisible(Block b) { return visible.contains(b); }

	public static void tick(Minecraft mc) {
		boolean on = active();
		String k = String.join(",", Modules.XRAY_BLOCKS.values);
		if (on && !k.equals(key)) {
			key = k;
			Set<String> names = new HashSet<>();
			for (String e : Modules.XRAY_BLOCKS.values) {
				String n = e.trim().toLowerCase(Locale.ROOT);
				if (n.startsWith("minecraft:")) n = n.substring(10);
				if (!n.isEmpty()) names.add(n);
			}
			Set<Block> set = new HashSet<>();
			for (Block b : BuiltInRegistries.BLOCK) if (names.contains(BuiltInRegistries.BLOCK.getKey(b).getPath())) set.add(b);
			visible = set;
			mc.levelRenderer.allChanged();
		}
		if (on != wasActive) {
			wasActive = on;
			mc.levelRenderer.allChanged();
		}
		if (on && Modules.XRAY_BRIGHT.value) {
			if (savedGamma == null) savedGamma = Math.min(1.0, mc.options.gamma().get());
			if (mc.options.gamma().get() < 15.0) GreenMod.forceOption(mc.options.gamma(), 16.0);
		} else if (!on && savedGamma != null && !Modules.FULLBRIGHT.isActive()) {
			mc.options.gamma().set(savedGamma);
			savedGamma = null;
		}
	}
}
