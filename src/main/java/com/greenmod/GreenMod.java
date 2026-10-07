package com.greenmod;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

/** Shared helpers. */
public final class GreenMod {
	/** Spare bit in the synced "skin parts" byte used as a "uses Ruined Client" flag. */
	public static final int USER_FLAG = 0x80;

	/** True if the local player is really on fire (vanilla overlay is hidden by Low Fire). */
	public static boolean realBurning;

	private static EntityDataAccessor<Byte> skinPartsAccessor;
	private static boolean accessorSearched;

	private GreenMod() {}

	@SuppressWarnings("unchecked")
	private static EntityDataAccessor<Byte> accessor() {
		if (!accessorSearched) {
			accessorSearched = true;
			for (Class<?> c = Player.class; c != null; c = c.getSuperclass()) {
				try {
					Field f = c.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
					f.setAccessible(true);
					skinPartsAccessor = (EntityDataAccessor<Byte>) f.get(null);
					break;
				} catch (Throwable ignored) {
				}
			}
		}
		return skinPartsAccessor;
	}

	/** True for yourself and for any player whose client sends our flag bit. */
	public static boolean isModUser(Entity e) {
		if (e instanceof LocalPlayer) return true;
		if (!(e instanceof Player p)) return false;
		EntityDataAccessor<Byte> acc = accessor();
		if (acc == null) return false;
		try {
			return (p.getEntityData().get(acc) & USER_FLAG) != 0;
		} catch (Throwable t) {
			return false;
		}
	}

	/** Sets an option value even outside its normal slider range. */
	public static <T> void forceOption(OptionInstance<T> opt, T v) {
		try {
			Field f = OptionInstance.class.getDeclaredField("value");
			f.setAccessible(true);
			f.set(opt, v);
		} catch (Throwable t) {
			try {
				opt.set(v);
			} catch (Throwable ignored) {
			}
		}
	}
}
