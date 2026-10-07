package com.greenmod;

import net.minecraft.client.KeyMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Finds the keyboard key a vanilla key binding is bound to, so we can poll it directly. */
public final class KeyCodes {
	private KeyCodes() {}

	public static int of(KeyMapping km, int fallback) {
		try {
			for (Field f : KeyMapping.class.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())) continue;
				if (!f.getType().getSimpleName().equals("Key")) continue;
				f.setAccessible(true);
				Object key = f.get(km);
				if (key == null) continue;
				Object type = call(key, "getType", "type");
				if (type != null && !type.toString().equals("KEYSYM")) return fallback;
				Object value = call(key, "getValue", "value");
				if (value instanceof Number n) return n.intValue();
			}
		} catch (Throwable ignored) {
		}
		return fallback;
	}

	private static Object call(Object o, String... names) {
		for (String n : names) {
			try {
				Method m = o.getClass().getMethod(n);
				return m.invoke(o);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}
}
