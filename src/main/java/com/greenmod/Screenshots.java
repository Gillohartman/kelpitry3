package com.greenmod;

import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads screenshots from the game folder and turns them into small pixel grids that the menu can draw. */
public final class Screenshots {
	public static final int TW = 44, TH = 25;       // thumbnail cells
	public static final int PW = 112, PH = 63;      // preview cells

	public static final class Pic {
		public final int w, h;
		public final int[] argb;
		Pic(int w, int h, int[] argb) { this.w = w; this.h = h; this.argb = argb; }
	}

	private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "KelpClient-Screenshots");
		t.setDaemon(true);
		return t;
	});
	private static final Map<String, Pic> CACHE = new HashMap<>();
	private static final Map<String, Boolean> LOADING = new HashMap<>();
	private static List<File> files = new ArrayList<>();
	private static long lastScan;

	private Screenshots() {}

	public static List<File> list() {
		long now = System.currentTimeMillis();
		if (now - lastScan > 3000) {
			lastScan = now;
			File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
			File[] arr = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
			List<File> l = new ArrayList<>();
			if (arr != null) {
				Arrays.sort(arr, Comparator.comparingLong(File::lastModified).reversed());
				l.addAll(Arrays.asList(arr));
			}
			files = l;
		}
		return files;
	}

	/** Returns the pixel grid if ready, otherwise starts loading and returns null. */
	public static Pic get(File f, int cellsW, int cellsH) {
		String key = f.getName() + "@" + cellsW;
		Pic p = CACHE.get(key);
		if (p != null) return p;
		if (LOADING.putIfAbsent(key, true) == null) {
			EXEC.execute(() -> {
				try {
					BufferedImage img = ImageIO.read(f);
					if (img != null) {
						int[] out = new int[cellsW * cellsH];
						for (int y = 0; y < cellsH; y++) {
							for (int x = 0; x < cellsW; x++) {
								int sx = (int) ((x + 0.5) * img.getWidth() / cellsW);
								int sy = (int) ((y + 0.5) * img.getHeight() / cellsH);
								out[y * cellsW + x] = 0xFF000000 | img.getRGB(Math.min(sx, img.getWidth() - 1), Math.min(sy, img.getHeight() - 1));
							}
						}
						synchronized (CACHE) { CACHE.put(key, new Pic(cellsW, cellsH, out)); }
					}
				} catch (Throwable ignored) {
				}
			});
		}
		return null;
	}
}
