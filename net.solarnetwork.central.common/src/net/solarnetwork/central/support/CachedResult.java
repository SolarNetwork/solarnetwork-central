/* ==================================================================
 * CachedResult.java - 28/05/2015 6:46:28 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.util.concurrent.TimeUnit;

/**
 * A cached object holder.
 * 
 * @author matt
 * @version 1.1
 * @param <T>
 *        The type of object that is cached.
 * @deprecated use {@link net.solarnetwork.util.CachedResult} directly
 */
@Deprecated
public class CachedResult<T> extends net.solarnetwork.util.CachedResult<T> {

	/**
	 * Constructor. The current time will be used for the {@code created}
	 * property.
	 * 
	 * @param result
	 *        The result to cache.
	 * @param ttl
	 *        The time to live, after which the result should be considered
	 *        expired.
	 * @param unit
	 *        The time unit for the {@code expiration}.
	 */
	public CachedResult(T result, long ttl, TimeUnit unit) {
		super(result, System.currentTimeMillis(), ttl, unit);
	}

	/**
	 * Constructor.
	 * 
	 * @param result
	 *        The result to cache.
	 * @param created
	 *        The creation time to use for the result.
	 * @param ttl
	 *        The time to live, after which the result should be considered
	 *        expired.
	 * @param unit
	 *        The time unit for the {@code expiration}.
	 */
	public CachedResult(T result, long created, long ttl, TimeUnit unit) {
		super(result, created, ttl, unit);
	}

}
