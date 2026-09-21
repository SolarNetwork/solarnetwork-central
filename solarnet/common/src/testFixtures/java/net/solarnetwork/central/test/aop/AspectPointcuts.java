/* ==================================================================
 * AspectPointcuts.java - 22/09/2026 8:39:25 am
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.AopUtils;

/**
 * Utilities for verifying aspect pointcuts.
 *
 * @author matt
 * @version 1.0
 */
public final class AspectPointcuts {

	private AspectPointcuts() {
		// not available
	}

	/**
	 * Get the names of the pointcuts of an aspect that do not match any method
	 * of a set of target types.
	 *
	 * <p>
	 * A pointcut that matches nothing usually means a method was renamed, or
	 * the pointcut expression is wrong, and some method is not being secured
	 * as intended. Matching is static, so runtime conditions such as
	 * {@code @target(...)} are assumed to match.
	 * </p>
	 *
	 * @param aspectClass
	 *        the {@code @Aspect} class
	 * @param targetTypes
	 *        the types the aspect is meant to apply to, typically service
	 *        implementation classes
	 * @return the names of the {@code @Pointcut} methods that match nothing,
	 *         sorted
	 */
	public static List<String> unmatchedPointcuts(Class<?> aspectClass, Class<?>... targetTypes) {
		final List<String> result = new ArrayList<>();
		for ( Method m : aspectClass.getDeclaredMethods() ) {
			final Pointcut pointcut = m.getAnnotation(Pointcut.class);
			if ( pointcut == null ) {
				continue;
			}
			final String[] paramNames = (pointcut.argNames().isBlank()
					? Arrays.stream(m.getParameters()).map(Parameter::getName).toArray(String[]::new)
					: pointcut.argNames().trim().split("\\s*,\\s*"));
			final AspectJExpressionPointcut pc = new AspectJExpressionPointcut(aspectClass,
					paramNames, m.getParameterTypes());
			pc.setExpression(pointcut.value());
			if ( Arrays.stream(targetTypes).noneMatch(t -> AopUtils.canApply(pc, t)) ) {
				result.add(m.getName());
			}
		}
		result.sort(null);
		return result;
	}

}
