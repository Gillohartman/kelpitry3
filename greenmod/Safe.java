package com.greenmod;

import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Runs one module's tick/render step. An exception is logged once and that step is skipped, never crashing the game. */
public final class Safe {
	private static final Set<String> LOGGED = new HashSet<>();

	private Safe() {}

	public static void run(String name, Runnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			if (LOGGED.add(name)) {
				System.err.println("[RuinedClient] Module step \"" + name + "\" failed and is being skipped: " + t);
				t.printStackTrace();
			}
		}
	}

	public static void tick(String name, Consumer<Minecraft> c, Minecraft mc) {
		run(name, () -> c.accept(mc));
	}
}
