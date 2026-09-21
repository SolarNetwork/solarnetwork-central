/* ==================================================================
 * ApiCall.java - 22/09/2026 11:02:47 am
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

/**
 * An invocation of an API method.
 *
 * <p>
 * Unlike {@link java.util.function.Consumer}, an API call can invoke methods
 * that declare checked exceptions.
 * </p>
 *
 * @param <T>
 *        the API type
 * @author matt
 * @version 1.0
 */
@FunctionalInterface
public interface ApiCall<T> {

	/**
	 * Invoke an API method.
	 *
	 * @param api
	 *        the API to invoke
	 * @throws Exception
	 *         if any error occurs
	 */
	void invoke(T api) throws Exception;

}
