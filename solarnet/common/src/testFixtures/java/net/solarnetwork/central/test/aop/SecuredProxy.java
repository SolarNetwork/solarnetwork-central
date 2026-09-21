/* ==================================================================
 * SecuredProxy.java - 22/09/2026 8:39:25 am
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An AOP proxy with security aspects applied, its target, and the mocks the
 * aspects depend on.
 *
 * @param <T>
 *        the proxied API type
 * @param proxy
 *        the proxy
 * @param target
 *        the proxy target
 * @param mocks
 *        the mocks the aspects depend on, by type
 * @author matt
 * @version 1.0
 */
public record SecuredProxy<T>(T proxy, T target, Map<Class<?>, Object> mocks) {

	/**
	 * Get a mock the aspects depend on.
	 *
	 * @param <M>
	 *        the mock type
	 * @param type
	 *        the mock type
	 * @return the mock
	 * @throws IllegalStateException
	 *         if no mock of the given type is available
	 */
	public <M> M mock(Class<M> type) {
		final Object mock = nonnull(mocks.get(type), "Mock of type %s", type);
		return type.cast(mock);
	}

	/**
	 * Get a copy of this instance with an additional mock.
	 *
	 * @param <M>
	 *        the mock type
	 * @param type
	 *        the mock type
	 * @param mock
	 *        the mock
	 * @return the new instance
	 */
	public <M> SecuredProxy<T> withMock(Class<M> type, M mock) {
		final Map<Class<?>, Object> m = new LinkedHashMap<>(mocks);
		m.put(type, mock);
		return new SecuredProxy<>(proxy, target, Map.copyOf(m));
	}

}
