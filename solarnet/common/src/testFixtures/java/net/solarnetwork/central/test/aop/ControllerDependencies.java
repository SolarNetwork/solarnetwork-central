/* ==================================================================
 * ControllerDependencies.java - 22/09/2026 7:34:52 pm
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

import static java.util.Comparator.comparing;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

/**
 * Find the service dependencies of the controllers of an application.
 *
 * <p>
 * A service dependency is a field or constructor parameter of a controller
 * whose type is a SolarNetwork type whose name ends in {@code Biz},
 * {@code Dao}, or {@code Service}, including as a type argument, for example of
 * {@code Optional} or {@code List}. Controllers that use such a dependency
 * directly bypass any security aspect that does not apply to it, so every
 * dependency must either have a security contract or a review stating why it
 * does not need one.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class ControllerDependencies {

	private static final String SOLARNETWORK_PACKAGE = "net.solarnetwork.central.";

	private static final Pattern SERVICE_NAME = Pattern.compile(".*(Biz|Dao|Service)");

	private ControllerDependencies() {
		// not available
	}

	/**
	 * A service dependency of a controller.
	 *
	 * @param controller
	 *        the controller class
	 * @param type
	 *        the dependency type
	 */
	public record Dependency(Class<?> controller, Class<?> type) {

		@Override
		public String toString() {
			return controller.getName() + " -> " + type.getName();
		}

	}

	/**
	 * A reviewed service dependency, that does not need a security contract.
	 *
	 * @param type
	 *        the dependency type
	 * @param controller
	 *        the controller class the review applies to, or {@code null} for
	 *        all controllers
	 * @param reason
	 *        why the dependency does not need a security contract
	 */
	public record Review(Class<?> type, @Nullable Class<?> controller, String reason) {

		/**
		 * Test if this review covers a dependency.
		 *
		 * @param dependency
		 *        the dependency
		 * @return {@code true} if this review covers the dependency
		 */
		public boolean covers(Dependency dependency) {
			return type.equals(dependency.type())
					&& (controller == null || controller.equals(dependency.controller()));
		}

		@Override
		public String toString() {
			return (controller != null ? controller.getName() + " -> " : "") + type.getName() + " ("
					+ reason + ")";
		}

	}

	/**
	 * Create a review of a dependency for all controllers.
	 *
	 * @param type
	 *        the dependency type
	 * @param reason
	 *        why the dependency does not need a security contract
	 * @return the review
	 */
	public static Review reviewed(Class<?> type, String reason) {
		return new Review(type, null, reason);
	}

	/**
	 * Create a review of a dependency for one controller.
	 *
	 * @param type
	 *        the dependency type
	 * @param controller
	 *        the controller class
	 * @param reason
	 *        why the dependency does not need a security contract
	 * @return the review
	 */
	public static Review reviewed(Class<?> type, Class<?> controller, String reason) {
		return new Review(type, controller, reason);
	}

	/**
	 * Get the service dependencies of all controller beans of an application.
	 *
	 * @param context
	 *        the application context
	 * @return the dependencies, sorted by controller and type name
	 */
	public static Set<Dependency> serviceDependencies(ApplicationContext context) {
		final Set<Dependency> result = new TreeSet<>(
				comparing((Dependency d) -> d.controller().getName())
						.thenComparing(d -> d.type().getName()));
		for ( Object bean : context.getBeansWithAnnotation(Controller.class).values() ) {
			final Class<?> controller = ClassUtils.getUserClass(AopUtils.getTargetClass(bean));
			if ( !controller.getName().startsWith(SOLARNETWORK_PACKAGE) ) {
				continue;
			}
			for ( Class<?> c = controller; c != null && c != Object.class; c = c.getSuperclass() ) {
				for ( Field f : c.getDeclaredFields() ) {
					if ( !Modifier.isStatic(f.getModifiers()) ) {
						addServiceTypes(result, controller, f.getGenericType());
					}
				}
			}
			for ( Constructor<?> ctor : controller.getDeclaredConstructors() ) {
				for ( Type t : ctor.getGenericParameterTypes() ) {
					addServiceTypes(result, controller, t);
				}
			}
		}
		return result;
	}

	/**
	 * Get the service dependencies of all controller beans of an application
	 * that neither have a security contract nor are covered by a review.
	 *
	 * @param context
	 *        the application context
	 * @param contracted
	 *        the APIs that have a security contract
	 * @param reviews
	 *        the reviewed dependencies
	 * @return the dependencies that have not been reviewed
	 */
	public static List<Dependency> unreviewed(ApplicationContext context,
			Collection<Class<?>> contracted, Collection<Review> reviews) {
		return serviceDependencies(context).stream().filter(d -> !contracted.contains(d.type()))
				.filter(d -> reviews.stream().noneMatch(r -> r.covers(d))).toList();
	}

	/**
	 * Get the reviews that do not cover any service dependency of the
	 * controller beans of an application.
	 *
	 * @param context
	 *        the application context
	 * @param reviews
	 *        the reviewed dependencies
	 * @return the reviews that do not cover any dependency
	 */
	public static List<Review> unusedReviews(ApplicationContext context,
			Collection<Review> reviews) {
		final Set<Dependency> dependencies = serviceDependencies(context);
		return reviews.stream().filter(r -> dependencies.stream().noneMatch(r::covers)).toList();
	}

	private static void addServiceTypes(Set<Dependency> result, Class<?> controller, Type type) {
		switch (type) {
			case Class<?> c when c.isArray() -> {
				addServiceTypes(result, controller, c.getComponentType());
			}
			case Class<?> c -> {
				if ( c.getName().startsWith(SOLARNETWORK_PACKAGE)
						&& SERVICE_NAME.matcher(c.getSimpleName()).matches() ) {
					result.add(new Dependency(controller, c));
				}
			}
			case ParameterizedType p -> {
				addServiceTypes(result, controller, p.getRawType());
				for ( Type arg : p.getActualTypeArguments() ) {
					addServiceTypes(result, controller, arg);
				}
			}
			case GenericArrayType a -> addServiceTypes(result, controller, a.getGenericComponentType());
			case WildcardType w -> {
				for ( Type bound : w.getUpperBounds() ) {
					addServiceTypes(result, controller, bound);
				}
			}
			default -> {
				// ignore type variables
			}
		}
	}

}
