package com.greenmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;

/** Plays your own WAV file when you die. The folder is config/kelpclient/sounds. Playback stops after 5 seconds. */
public final class DeathSound {
	private static boolean wasDead;
	private static boolean warned;

	private DeathSound() {}

	public static File folder() {
		File f = FabricLoader.getInstance().getConfigDir().resolve("kelpclient").resolve("sounds").toFile();
		f.mkdirs();
		return f;
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) { wasDead = false; return; }
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead && Modules.DEATH_SOUND.isActive()) play();
		wasDead = dead;
	}

	private static void play() {
		final File file = new File(folder(), Modules.DS_FILE.value.trim());
		final double vol = Modules.DS_VOLUME.value / 100.0;
		if (!file.isFile()) {
			if (!warned) { warned = true; Notifications.push("Death sound: put " + file.getName() + " in config/kelpclient/sounds"); }
			return;
		}
		Thread t = new Thread(() -> {
			try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
				Clip clip = AudioSystem.getClip();
				clip.open(in);
				if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
					FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
					float db = (float) (20.0 * Math.log10(Math.max(0.001, vol)));
					c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), db)));
				}
				clip.start();
				Thread.sleep(5000);
				clip.stop();
				clip.close();
			} catch (Throwable ignored) {
			}
		}, "RuinedClient-DeathSound");
		t.setDaemon(true);
		t.start();
	}
}
