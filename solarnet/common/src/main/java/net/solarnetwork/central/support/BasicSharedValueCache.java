/* ==================================================================
 * BasicSharedValueCache.java - 23/02/2024 6:58:51 am
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ObjectUtils;

/**
 * Basic implementation of {@link SharedValueCache} using {@link ConcurrentMap}
 * objects for storage.
 *
 * @author matt
 * @version 1.0
 */
public class BasicSharedValueCache<K, V, S> implements SharedValueCache<K, V, S> {

	private final ConcurrentMap<K, CachedResult<V>> cache;
	private final ConcurrentMap<S, V> sharedCache;

	/**
	 * Constructor.
	 */
	public BasicSharedValueCache() {
		this(new ConcurrentHashMap<>(64, 0.9f, 2), new ConcurrentHashMap<>(64, 0.9f, 2));

	}

	/**
	 * Constructor.
	 *
	 * @param cache
	 *        the primary cache
	 * @param sharedCache
	 *        the shared cache
	 */
	public BasicSharedValueCache(ConcurrentMap<K, CachedResult<V>> cache,
			ConcurrentMap<S, V> sharedCache) {
		super();
		this.cache = ObjectUtils.requireNonNullArgument(cache, "cache");
		this.sharedCache = ObjectUtils.requireNonNullArgument(sharedCache, "sharedCache");
	}

	@Override
	public V get(K key) {
		CachedResult<V> entry = cache.get(key);
		return (entry != null && entry.isValid() ? entry.getResult() : null);
	}

	@Override
	public V put(K key, S shareKey, Function<S, V> valueProvider, long ttl) {
		if ( ttl < 1 ) {
			CachedResult<V> entry = cache.remove(key);
			return entry != null ? entry.getResult() : null;
		}
		V sharedValue = sharedCache.computeIfAbsent(shareKey, valueProvider);
		CachedResult<V> entry = new CachedResult<>(sharedValue, ttl, TimeUnit.SECONDS);
		cache.put(key, entry);
		return sharedValue;
	}

	/**
	 * Prune shared values no longer in use.
	 */
	@Override
	public void prune() {
		Set<V> inUse = new HashSet<>(32);
		for ( Iterator<CachedResult<V>> itr = cache.values().iterator(); itr.hasNext(); ) {
			CachedResult<V> entry = itr.next();
			if ( !entry.isValid() ) {
				itr.remove();
				continue;
			}
			V value = entry.getResult();
			if ( !inUse.contains(value) ) {
				inUse.add(value);
			}
		}
		for ( Iterator<V> sharedValueItr = sharedCache.values().iterator(); sharedValueItr.hasNext(); ) {
			V value = sharedValueItr.next();
			if ( !inUse.contains(value) ) {
				sharedValueItr.remove();
			}
		}
	}

}
