package com.greenmod;

import org.lwjgl.glfw.GLFW;

/** Raw GLFW keyboard/mouse polling (works with and without an open screen). */
public final class Keys {
	/** All key codes GLFW accepts for polling. */
	public static final int[] VALID = buildValid();

	private Keys() {}

	private static int[] buildValid() {
		int[] tmp = new int[400];
		int n = 0;
		for (int k = 32; k <= 96; k++) tmp[n++] = k;
		for (int k = 256; k <= 348; k++) tmp[n++] = k;
		return java.util.Arrays.copyOf(tmp, n);
	}

	public static boolean down(int key) {
		if (key < 32 || key > 348) return false;
		if (key > 96 && key < 256) return false;
		long w = GLFW.glfwGetCurrentContext();
		return w != 0L && GLFW.glfwGetKey(w, key) == GLFW.GLFW_PRESS;
	}

	public static boolean mouse(int button) {
		long w = GLFW.glfwGetCurrentContext();
		return w != 0L && GLFW.glfwGetMouseButton(w, button) == GLFW.GLFW_PRESS;
	}

	public static boolean ctrl() {
		return down(GLFW.GLFW_KEY_LEFT_CONTROL) || down(GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	public static boolean shift() {
		return down(GLFW.GLFW_KEY_LEFT_SHIFT) || down(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	public static String name(int key) {
		if (key < 0) return "None";
		if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
		switch (key) {
			case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-Shift";
			case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-Shift";
			case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-Ctrl";
			case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-Ctrl";
			case GLFW.GLFW_KEY_LEFT_ALT: return "L-Alt";
			case GLFW.GLFW_KEY_RIGHT_ALT: return "R-Alt";
			case GLFW.GLFW_KEY_SPACE: return "Space";
			case GLFW.GLFW_KEY_TAB: return "Tab";
			case GLFW.GLFW_KEY_ENTER: return "Enter";
			case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
			case GLFW.GLFW_KEY_UP: return "Up";
			case GLFW.GLFW_KEY_DOWN: return "Down";
			case GLFW.GLFW_KEY_LEFT: return "Left";
			case GLFW.GLFW_KEY_RIGHT: return "Right";
			case GLFW.GLFW_KEY_INSERT: return "Insert";
			case GLFW.GLFW_KEY_DELETE: return "Delete";
			case GLFW.GLFW_KEY_HOME: return "Home";
			case GLFW.GLFW_KEY_END: return "End";
			case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
			case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
			default:
				String n = GLFW.glfwGetKeyName(key, 0);
				return n != null ? n.toUpperCase() : "Key " + key;
		}
	}
}
