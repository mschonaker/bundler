package io.github.mschonaker.bundler.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

public final class Methods {

	private Methods() {
	}

	public static boolean doesThrow(Method method, Throwable declared) {
		return Arrays.stream(method.getExceptionTypes()).filter(t -> t.isAssignableFrom(declared.getClass())).findAny().isPresent();
	}

	public static boolean returnsList(Method method) {
		return method.getReturnType().equals(List.class);
	}

	public static boolean returnsPrimitive(Method method) {
		return returnsList(method) ? isPrimitive(getReturningTypeComponent(method)) : isPrimitive(method.getReturnType());
	}

	private static boolean isPrimitive(Class<?> type) {
		return type.isPrimitive() || type.isEnum() || type.getClassLoader() == null;
	}

	public static Class<?> getReturningTypeComponent(Method method) {
		try {
			ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
			return (Class<?>) type.getActualTypeArguments()[0];
		} catch (Throwable t) {
			return null;
		}
	}

	public static boolean returnsVoid(Method method) {
		Class<?> type = method.getReturnType();
		return Void.TYPE.equals(type) || Void.class.equals(type);
	}
}
