package io.github.schluid.gearbalance.client;

import java.lang.reflect.Method;

/**
 * Whether a shader pack is in use right now (Iris API v0). Read by reflection, so GearBalance needs no Iris
 * at build time and runs the same without it.
 */
final class Shaders {
	private Shaders() {
	}

	private static boolean looked;
	private static Object api;
	private static Method inUse;

	static boolean active() {
		if (!looked) {
			looked = true;
			try {
				Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				api = type.getMethod("getInstance").invoke(null);
				inUse = type.getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError e) {
				// No Iris, or an Iris without this API
				api = null;
			}
		}
		if (api == null) {
			return false;
		}
		try {
			return (Boolean) inUse.invoke(api);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}
}
