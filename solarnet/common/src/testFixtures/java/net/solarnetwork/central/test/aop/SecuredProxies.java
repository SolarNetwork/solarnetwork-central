/* ==================================================================
 * SecuredProxies.java - 22/09/2026 8:39:25 am
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

import java.util.Map;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * Create AOP proxies with security aspects applied.
 *
 * @author matt
 * @version 1.0
 */
public final class SecuredProxies {

	private SecuredProxies() {
		// not available
	}

	/**
	 * Create a class-based proxy of a target with a set of aspects applied, as
	 * Spring Boot does for application beans.
	 *
	 * <p>
	 * The target is typically a Mockito mock of the real service
	 * implementation class, so that pointcuts that match on the target class
	 * (for example {@code @target(Securable)}) behave as they do in
	 * production.
	 * </p>
	 *
	 * @param <T>
	 *        the target type
	 * @param target
	 *        the proxy target
	 * @param aspects
	 *        the {@code @Aspect} instances to apply
	 * @return the proxy
	 */
	public static <T> SecuredProxy<T> securedProxy(T target, Object... aspects) {
		final AspectJProxyFactory factory = new AspectJProxyFactory(target);
		factory.setProxyTargetClass(true);
		for ( Object aspect : aspects ) {
			factory.addAspect(aspect);
		}
		final T proxy = factory.getProxy();
		return new SecuredProxy<>(proxy, target, Map.of());
	}

}
