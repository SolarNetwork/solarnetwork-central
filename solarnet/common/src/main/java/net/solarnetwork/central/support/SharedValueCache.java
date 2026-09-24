/* ==================================================================
 * SharedValueCache.java - 23/02/2024 6:52:25 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Cache API where cached values are shared on a secondary key.
 *
 * <p>
 * The idea for this API is for situations where it is desirable to cache a
 * common value that is expensive to compute, but with potentially different
 * expiration times for different uses. Thus, the expensive value can be
 * computed once using a "shared key" but then cached multiple times with
 * different expiration times using a primary key.
 * </p>
 *
 * <p>
 * Implementations must be thread-safe: a single instance is expected to be
 * shared by many application threads, along with a background thread that
 * invokes {@link #prune()} periodically.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface SharedValueCache<K, V, S> {

	/**
	 * Get a cached value by key.
	 *
	 * @param key
	 *        the key of the value to get
	 * @return the value, or {@code null} if not available (or expired)
	 */
	@Nullable
	V get(K key);

	/**
	 * Add a value to the cache.
	 *
	 * <p>
	 * The {@code valueProvider} is invoked only if no value is cached for
	 * {@code shareKey} already, and must not return {@code null}. All primary
	 * keys sharing a given {@code shareKey} resolve to the same value instance.
	 * </p>
	 *
	 * @param key
	 *        the cache key
	 * @param shareKey
	 *        the shared value key
	 * @param valueProvider
	 *        the cache value provider
	 * @param ttl
	 *        the cache value time to live, in seconds
	 * @return the value, either a shared instance or newly computed by
	 *         {@code valueProvider}
	 */
	V put(K key, S shareKey, Function<S, V> valueProvider, long ttl);

	/**
	 * Prune shared values no longer in use.
	 *
	 * <p>
	 * Expired primary cache entries are evicted, along with any shared value no
	 * longer referenced by a remaining entry. This is the only eviction
	 * mechanism: {@link #get(Object)} does not remove expired entries.
	 * </p>
	 */
	void prune();
}
