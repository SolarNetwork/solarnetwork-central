/* ==================================================================
 * SimpleCache.java - 9/08/2023 6:15:04 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import net.solarnetwork.util.CachedResult;

/**
 * A very loose and basic implementation of {@link Cache}, using a
 * {@link ConcurrentMap} to store the data.
 * 
 * <p>
 * This class does not support any of the cache listener methods, configuration,
 * or the cache manager. Entries are not automatically released from memory, but
 * will not be returned once expired. The {@link #iterator()} method will
 * release expired entries during iteration, so can be used to free memory as
 * needed.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCache<K, V> implements Cache<K, V> {

	public static final long DEFAULT_TTL = 60L;

	public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

	private final String name;
	private final ConcurrentMap<K, CachedValue> data;
	private long ttl = DEFAULT_TTL;
	private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A {@link ConcurrentHashMap} will be used.
	 * </p>
	 * 
	 * @param name
	 *        the name to use
	 */
	public SimpleCache(String name) {
		this(name, new ConcurrentHashMap<>(64, 0.9f, 2));
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the map to use to store the cached data in
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SimpleCache(String name, ConcurrentMap<K, ? extends CachedResult<V>> data) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.data = requireNonNullArgument((ConcurrentMap) data, "data");
	}

	/**
	 * A cached value.
	 */
	public final class CachedValue extends CachedResult<V> implements Entry<K, V> {

		private final K key;

		private CachedValue(K key, V value) {
			super(value, ttl, timeUnit);
			this.key = requireNonNullArgument(key, "key");
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return getResult();
		}

		@Override
		public <T> T unwrap(Class<T> clazz) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Compares the {@link #getKey()} and {@link #getResult()} values
		 * against another cached object's values.
		 */
		@Override
		public boolean equals(Object obj) {
			if ( obj == null ) {
				return false;
			}
			if ( CachedValue.class.isAssignableFrom(obj.getClass()) ) {
				@SuppressWarnings("unchecked")
				CachedValue v = (CachedValue) obj;
				return Objects.equals(key, v.key) && Objects.equals(getResult(), v.getResult());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(key, getResult());
		}

	}

	@Override
	public V get(K key) {
		CachedValue result = data.get(key);
		return (result != null && result.isValid() ? result.getResult() : null);
	}

	@Override
	public Map<K, V> getAll(Set<? extends K> keys) {
		if ( keys == null || keys.isEmpty() ) {
			return Collections.emptyMap();
		}
		Map<K, V> result = new HashMap<>(keys.size());
		for ( K key : keys ) {
			V v = get(key);
			if ( v != null ) {
				result.put(key, v);
			}
		}
		return result;
	}

	@Override
	public boolean containsKey(K key) {
		return get(key) != null;
	}

	@Override
	public void loadAll(Set<? extends K> keys, boolean replaceExistingValues,
			CompletionListener completionListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(K key, V value) {
		data.put(key, new CachedValue(key, value));
	}

	@Override
	public V getAndPut(K key, V value) {
		CachedValue result = data.put(key, new CachedValue(key, value));
		return (result != null && result.isValid() ? result.getResult() : null);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		if ( map == null ) {
			return;
		}
		for ( Map.Entry<? extends K, ? extends V> e : map.entrySet() ) {
			data.put(e.getKey(), new CachedValue(e.getKey(), e.getValue()));
		}
	}

	@Override
	public boolean putIfAbsent(K key, V value) {
		CachedValue result = data.compute(key, (k, v) -> {
			if ( v == null || !v.isValid() ) {
				return new CachedValue(key, value);
			}
			return null;
		});
		return result != null;
	}

	@Override
	public boolean remove(K key) {
		return (data.remove(key) != null);
	}

	@Override
	public boolean remove(K key, V oldValue) {
		return data.remove(key, new CachedValue(key, oldValue));
	}

	@Override
	public V getAndRemove(K key) {
		CachedValue result = data.remove(key);
		return (result != null && result.isValid() ? result.getResult() : null);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return data.replace(key, new CachedValue(key, oldValue), new CachedValue(key, newValue));
	}

	@Override
	public boolean replace(K key, V value) {
		return (data.replace(key, new CachedValue(key, value)) != null);
	}

	@Override
	public V getAndReplace(K key, V value) {
		AtomicReference<V> old = new AtomicReference<>();
		data.compute(key, (k, v) -> {
			if ( v != null && v.isValid() ) {
				old.set(v.getResult());
				return new CachedValue(key, value);
			}
			old.set(null);
			return null;
		});
		return old.get();
	}

	@Override
	public void removeAll(Set<? extends K> keys) {
		if ( keys == null ) {
			return;
		}
		for ( K key : keys ) {
			data.remove(key);
		}
	}

	@Override
	public void removeAll() {
		data.clear();
	}

	@Override
	public void clear() {
		data.clear();
	}

	@Override
	public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
			throws EntryProcessorException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys,
			EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public CacheManager getCacheManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		// nothing
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerCacheEntryListener(
			CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deregisterCacheEntryListener(
			CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return new Iterator<Entry<K, V>>() {

			private final Iterator<Map.Entry<K, CachedValue>> itr = data.entrySet().iterator();
			private Map.Entry<K, CachedValue> e;

			@Override
			public boolean hasNext() {
				e = null;
				while ( itr.hasNext() ) {
					Map.Entry<K, CachedValue> next = itr.next();
					if ( next.getValue().isValid() ) {
						e = next;
						return true;
					} else {
						itr.remove();
					}
				}
				return false;
			}

			@Override
			public Entry<K, V> next() {
				final var entry = e;
				if ( entry == null ) {
					throw new NoSuchElementException();
				}
				return entry.getValue();
			}

		};
	}

	/**
	 * Get the time-to-live.
	 * 
	 * @return the time to live
	 */
	public long getTtl() {
		return ttl;
	}

	/**
	 * Set the time to live.
	 * 
	 * @param ttl
	 *        the time to live to set
	 */
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	/**
	 * Get the TTL time unit.
	 * 
	 * @return the TTL time unit
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	/**
	 * Set the TTL time unit.
	 * 
	 * @param timeUnit
	 *        the TTL time unit to set
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

}
