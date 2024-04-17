/* ==================================================================
 * ExpandedEventsCache.java - 17/04/2024 5:19:08 pm
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

import static java.util.Collections.singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.EventType;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import net.solarnetwork.util.ObjectUtils;

/**
 * Cache with extended events support.
 * 
 * <p>
 * The following methods offer extended event support:
 * </p>
 * 
 * <ul>
 * <li>{@link #getAndRemove(Object)}: remove event issued</li>
 * </ul>
 * 
 * @author matt
 * @version 1.0
 */
public class ExpandedEventsCache<K, V> implements Cache<K, V> {

	private final Cache<K, V> delegate;

	private CacheEntryRemovedListener<? super K, ? super V> removedListener;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate cache
	 */
	public ExpandedEventsCache(Cache<K, V> delegate) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public void forEach(Consumer<? super Entry<K, V>> action) {
		delegate.forEach(action);
	}

	@Override
	public Spliterator<Entry<K, V>> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public V get(K key) {
		return delegate.get(key);
	}

	@Override
	public Map<K, V> getAll(Set<? extends K> keys) {
		return delegate.getAll(keys);
	}

	@Override
	public boolean containsKey(K key) {
		return delegate.containsKey(key);
	}

	@Override
	public void loadAll(Set<? extends K> keys, boolean replaceExistingValues,
			CompletionListener completionListener) {
		delegate.loadAll(keys, replaceExistingValues, completionListener);
	}

	@Override
	public void put(K key, V value) {
		delegate.put(key, value);
	}

	@Override
	public V getAndPut(K key, V value) {
		return delegate.getAndPut(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		delegate.putAll(map);
	}

	@Override
	public boolean putIfAbsent(K key, V value) {
		return delegate.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(K key) {
		return delegate.remove(key);
	}

	@Override
	public boolean remove(K key, V oldValue) {
		return delegate.remove(key, oldValue);
	}

	@Override
	public V getAndRemove(K key) {
		final V old = delegate.getAndRemove(key);
		if ( old != null ) {
			publishRemovedEvent(key, old);
		}
		return old;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return delegate.replace(key, oldValue, newValue);
	}

	@Override
	public boolean replace(K key, V value) {
		return delegate.replace(key, value);
	}

	@Override
	public V getAndReplace(K key, V value) {
		return delegate.getAndReplace(key, value);
	}

	@Override
	public void removeAll(Set<? extends K> keys) {
		delegate.removeAll(keys);
	}

	@Override
	public void removeAll() {
		delegate.removeAll();
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
		return delegate.getConfiguration(clazz);
	}

	@Override
	public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
			throws EntryProcessorException {
		return delegate.invoke(key, entryProcessor, arguments);
	}

	@Override
	public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys,
			EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
		return delegate.invokeAll(keys, entryProcessor, arguments);
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public CacheManager getCacheManager() {
		return delegate.getCacheManager();
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		return delegate.unwrap(clazz);
	}

	@Override
	public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> config) {
		CacheEntryListener<? super K, ? super V> listener = config.getCacheEntryListenerFactory()
				.create();
		if ( listener instanceof CacheEntryRemovedListener<?, ?> ) {
			this.removedListener = (CacheEntryRemovedListener<? super K, ? super V>) listener;
		}
		delegate.registerCacheEntryListener(config);
	}

	@Override
	public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> config) {
		CacheEntryListener<? super K, ? super V> listener = config.getCacheEntryListenerFactory()
				.create();
		if ( listener instanceof CacheEntryRemovedListener<?, ?> ) {
			this.removedListener = null;
		}
		delegate.deregisterCacheEntryListener(config);
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return delegate.iterator();
	}

	private void publishRemovedEvent(K key, V value) {
		if ( removedListener != null ) {
			removedListener.onRemoved(singleton(new CacheEntryEventImpl(key, value, EventType.REMOVED)));
		}
	}

	private final class CacheEntryEventImpl extends CacheEntryEvent<K, V> {

		private static final long serialVersionUID = 6369201595734400543L;

		private final K key;
		private final V value;

		private CacheEntryEventImpl(K key, V value, EventType eventType) {
			super(ExpandedEventsCache.this, eventType);
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public <T> T unwrap(Class<T> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V getOldValue() {
			return null;
		}

		@Override
		public boolean isOldValueAvailable() {
			return false;
		}

	}

}
