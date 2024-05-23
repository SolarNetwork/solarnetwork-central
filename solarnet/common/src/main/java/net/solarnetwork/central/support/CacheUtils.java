/* ==================================================================
 * CacheUtils.java - 23/05/2024 9:45:17 am
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

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.cache.Cache;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.ehcache.jsr107.Eh107Configuration;
import net.solarnetwork.util.ObjectUtils;

/**
 * Utilities for JCache.
 * 
 * @author matt
 * @version 1.0
 */
public final class CacheUtils {

	/**
	 * Listener API for "eviction" events.
	 * 
	 * <p>
	 * JCache does not offer an "eviction" event, when the cache removes an
	 * entry due to a size constraint. This API is provided to work with cache
	 * implementations that do support such events, like EhCache.
	 * </p>
	 * 
	 * @param <K>
	 *        the cache key type
	 * @param <V>
	 *        the cache value type
	 */
	@FunctionalInterface
	public static interface CacheEvictionListener<K, V> {

		/**
		 * Receive a cache eviction notification.
		 * 
		 * @param key
		 *        the evicted key
		 * @param value
		 *        the evicted value
		 */
		void onCacheEviction(K key, V value);

	}

	private static final ConcurrentMap<CacheEvictionListener<?, ?>, CacheEvictionListenerAdapter<?, ?>> REGISTRATIONS = new ConcurrentHashMap<>(
			8);

	private static class CacheEvictionListenerAdapter<K, V> implements CacheEventListener<K, V> {

		private final CacheEvictionListener<K, V> delegate;

		private CacheEvictionListenerAdapter(CacheEvictionListener<K, V> delegate) {
			super();
			this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		}

		@Override
		public void onEvent(CacheEvent<? extends K, ? extends V> event) {
			if ( event.getType() == EventType.EVICTED ) {
				K key = event.getKey();
				V val = event.getOldValue();
				delegate.onCacheEviction(key, val);
			}

		}

	}

	/**
	 * Register a cache eviction listener if possible.
	 * 
	 * @param <K>
	 *        the cache key type
	 * @param <V>
	 *        the cache value type
	 * @param cache
	 *        the cache to register the listener on
	 * @param listener
	 *        the listener to register
	 */
	public static <K, V> void registerCacheEvictionListener(Cache<K, V> cache,
			CacheEvictionListener<K, V> listener) {
		// currently support EhCache only
		var l = new CacheEvictionListenerAdapter<>(listener);
		if ( REGISTRATIONS.putIfAbsent(listener, l) != null ) {
			// already registered
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			CacheRuntimeConfiguration<K, V> config = (CacheRuntimeConfiguration<K, V>) cache
					.getConfiguration(Eh107Configuration.class).unwrap(CacheRuntimeConfiguration.class);
			config.registerCacheEventListener(l, EventOrdering.UNORDERED, EventFiring.ASYNCHRONOUS,
					EnumSet.of(EventType.EVICTED));
		} catch ( IllegalArgumentException e ) {
			// not supported, ignore
		}
	}

	/**
	 * Remove a previously registered cache eviction listener.
	 * 
	 * @param <K>
	 *        the cache key type
	 * @param <V>
	 *        the cache value type
	 * @param cache
	 *        the cache to remove the listener from
	 * @param listener
	 *        the listener to remove
	 */
	public static <K, V> void deregisterCacheEvictionListener(Cache<K, V> cache,
			CacheEvictionListener<K, V> listener) {
		// currently support EhCache only
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final CacheEvictionListenerAdapter<K, V> l = (CacheEvictionListenerAdapter) REGISTRATIONS
				.remove(listener);
		if ( l == null ) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			CacheRuntimeConfiguration<K, V> config = (CacheRuntimeConfiguration<K, V>) cache
					.getConfiguration(Eh107Configuration.class).unwrap(CacheRuntimeConfiguration.class);
			config.deregisterCacheEventListener(l);
		} catch ( IllegalArgumentException e ) {
			// not supported, ignore
		}

	}

}
