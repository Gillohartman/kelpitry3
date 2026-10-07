package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/** Applies your chat layout: size, background, widths and heights in pixels. */
public final class ChatSettings {
	private static int counter;

	private ChatSettings() {}

	// Vanilla maps slider values 0..1 to pixels like this.
	private static double heightToSlider(double px) { return Math.max(0, Math.min(1, (px - 20.0) / 160.0)); }
	private static double widthToSlider(double px) { return Math.max(0, Math.min(1, (px - 40.0) / 280.0)); }

	public static void tick(Minecraft mc) {
		if (!Modules.CHAT.isActive()) return;
		if (++counter % 20 != 1) return;
		set(mc.options.chatScale(), Modules.CHAT_SCALE.value / 100.0);
		set(mc.options.textBackgroundOpacity(), Modules.CHAT_BG.value / 100.0);
		set(mc.options.chatHeightFocused(), heightToSlider(Modules.CHAT_H_FOCUSED.value));
		set(mc.options.chatHeightUnfocused(), heightToSlider(Modules.CHAT_H_UNFOCUSED.value));
		set(mc.options.chatWidth(), widthToSlider(Modules.CHAT_WIDTH.value));
	}

	private static void set(OptionInstance<Double> opt, double v) {
		if (Math.abs(opt.get() - v) > 0.0005) opt.set(v);
	}
}
