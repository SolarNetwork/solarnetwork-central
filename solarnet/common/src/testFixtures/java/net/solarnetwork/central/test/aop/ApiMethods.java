/* ==================================================================
 * ApiMethods.java - 22/09/2026 8:39:25 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.test.aop;

import static java.util.stream.Collectors.joining;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * Utilities for working with the methods of an API interface.
 *
 * @author matt
 * @version 1.0
 */
public final class ApiMethods {

	private ApiMethods() {
		// not available
	}

	/**
	 * Get the public methods of an API interface.
	 *
	 * <p>
	 * Static, synthetic, and {@link Object} methods are excluded.
	 * </p>
	 *
	 * @param api
	 *        the API interface
	 * @return the methods, sorted by signature
	 */
	public static List<Method> apiMethods(Class<?> api) {
		// @formatter:off
		return Arrays.stream(api.getMethods())
				.filter(m -> !Modifier.isStatic(m.getModifiers()))
				.filter(m -> !m.isSynthetic())
				.filter(m -> m.getDeclaringClass() != Object.class)
				.sorted(Comparator.comparing(ApiMethods::signature))
				.toList();
		// @formatter:on
	}

	/**
	 * Get a display signature for a method.
	 *
	 * @param method
	 *        the method
	 * @return the signature, for example {@code findThings(Long, Filter)}
	 */
	public static String signature(Method method) {
		return method.getName() + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName)
				.collect(joining(", ", "(", ")"));
	}

	/**
	 * Test if two methods have the same name and parameter types.
	 *
	 * @param m1
	 *        the first method
	 * @param m2
	 *        the second method
	 * @return {@code true} if the methods have the same signature
	 */
	public static boolean sameSignature(Method m1, Method m2) {
		return m1.getName().equals(m2.getName())
				&& Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes());
	}

	/**
	 * Get the API method a function invokes.
	 *
	 * @param <T>
	 *        the API type
	 * @param api
	 *        the API interface
	 * @param call
	 *        a function that invokes exactly one API method
	 * @return the invoked method
	 * @throws IllegalArgumentException
	 *         if {@code call} does not invoke exactly one API method
	 */
	@SuppressWarnings("ReferenceEquality")
	public static <T> Method invokedMethod(Class<T> api, ApiCall<? super T> call) {
		final AtomicReference<@Nullable Method> invoked = new AtomicReference<>();
		final Object recorder = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api },
				(proxy, method, args) -> {
					if ( method.getDeclaringClass() == Object.class ) {
						return switch (method.getName()) {
							case "equals" -> proxy == args[0];
							case "hashCode" -> System.identityHashCode(proxy);
							default -> "Recorder{" + api.getSimpleName() + "}";
						};
					}
					if ( !invoked.compareAndSet(null, method) ) {
						throw new IllegalArgumentException(
								"More than one %s method invoked.".formatted(api.getSimpleName()));
					}
					return defaultValue(method.getReturnType());
				});
		try {
			call.invoke(api.cast(recorder));
		} catch ( IllegalArgumentException e ) {
			throw e;
		} catch ( Exception e ) {
			throw new IllegalArgumentException(
					"Error invoking %s method: %s".formatted(api.getSimpleName(), e), e);
		}
		final Method result = invoked.get();
		if ( result == null ) {
			throw new IllegalArgumentException(
					"No %s method invoked.".formatted(api.getSimpleName()));
		}
		return result;
	}

	private static @Nullable Object defaultValue(Class<?> type) {
		if ( !type.isPrimitive() || type == void.class ) {
			return null;
		} else if ( type == boolean.class ) {
			return false;
		} else if ( type == char.class ) {
			return '\0';
		} else if ( type == byte.class ) {
			return (byte) 0;
		} else if ( type == short.class ) {
			return (short) 0;
		} else if ( type == int.class ) {
			return 0;
		} else if ( type == long.class ) {
			return 0L;
		} else if ( type == float.class ) {
			return 0f;
		}
		return 0d;
	}

}
