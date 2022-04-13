/* ==================================================================
 * BufferingDelegatingCache.java - 28/03/2020 4:48:01 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.EventType;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import net.solarnetwork.util.UnionIterator;

/**
 * {@link Cache} implementation that uses an in-memory only store for up to a
 * maximum number of keys, then delegates operations to another {@link Cache}
 * for overflow.
 * 
 * <p>
 * This cache has been designed to help minimize the cost required to
 * serialize/deserialize objects as they are placed into an off-heap cache
 * implementation. Keys and values are <b>not</b> serialized while maintained
 * internally to this cache. It can be useful if a cache is mostly being used
 * for temporary persistence where keys are added and removed regularly, and the
 * expected size of elements mostly stays below a certain threshold but
 * sometimes pushes beyond and the cache must accommodate. In that situation,
 * this cache would be configured with an {@code internalCapacity} of the
 * expected threshold, so that most of the time cached elements are managed
 * internally and occasionally excess elements would be pushed into the delegate
 * cache instance.
 * </p>
 * 
 * <p>
 * <b>Note</b> that not all {@link Cache} methods are supported, and will throw
 * an {@link UnsupportedOperationException} if invoked. Additionally only a
 * <b>single</b> {@link CacheEntryCreatedListener} is supported, when calling
 * {@link #registerCacheEntryListener(CacheEntryListenerConfiguration)}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class BufferingDelegatingCache<K, V> implements Cache<K, V> {

	private final Cache<K, V> delegate;
	private final int internalCapacity;
	private final ConcurrentMap<K, V> internalStore;
	private final AtomicInteger size;

	private CacheEntryCreatedListener<? super K, ? super V> createdListener;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * A {@link ConcurrentHashMap} will be created for the internal store.
	 * </p>
	 * 
	 * @param delegate
	 *        the cache to delegate elements to once more than
	 *        {@code internalCapacity} elemements are managed by the cache
	 * @param internalCapacity
	 *        the maximum number of elements to keep in the internal store
	 * @see #BufferingDelegatingCache(Cache, int, ConcurrentMap)
	 */
	public BufferingDelegatingCache(Cache<K, V> delegate, int internalCapacity) {
		this(delegate, internalCapacity, new ConcurrentHashMap<>(internalCapacity));
	}

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the cache to delegate elements to once more than
	 *        {@code internalCapacity} elements are managed by the cache
	 * @param internalCapacity
	 *        the maximum number of elements to keep in the internal store
	 * @param internalStore
	 *        the internal store to use
	 */
	public BufferingDelegatingCache(Cache<K, V> delegate, int internalCapacity,
			ConcurrentMap<K, V> internalStore) {
		super();
		this.delegate = delegate;
		this.internalCapacity = internalCapacity;
		this.internalStore = internalStore;
		this.size = new AtomicInteger(0);
	}

	/**
	 * Get the configured internal capacity.
	 * 
	 * @return the number of elements that can be stored internally, before
	 *         passing them to the delegate {@link Cache}
	 */
	public int getInternalCapacity() {
		return internalCapacity;
	}

	/**
	 * Get the number of elements currently stored internally.
	 * 
	 * @return the internal element count
	 */
	public int getInternalSize() {
		return size.get();
	}

	@Override
	public synchronized void clear() {
		internalStore.clear();
		delegate.clear();
		size.set(0);
	}

	@Override
	public synchronized void close() {
		if ( !delegate.isClosed() ) {
			for ( Map.Entry<K, V> e : internalStore.entrySet() ) {
				delegate.put(e.getKey(), e.getValue());
			}
			internalStore.clear();
			size.set(0);
			delegate.close();
		}
	}

	@Override
	public boolean containsKey(K key) {
		return internalStore.containsKey(key) || delegate.containsKey(key);
	}

	@Override
	public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> listener) {
		delegate.deregisterCacheEntryListener(listener);
	}

	@Override
	public V get(K key) {
		V v = internalStore.get(key);
		return (v != null ? v : delegate.get(key));
	}

	@Override
	public Map<K, V> getAll(Set<? extends K> keys) {
		if ( keys == null || keys.isEmpty() ) {
			return Collections.emptyMap();
		}
		Map<K, V> result = new HashMap<>(keys.size());
		for ( K k : keys ) {
			V v = internalStore.get(k);
			if ( v == null ) {
				v = delegate.get(k);
			}
			if ( v != null ) {
				result.put(k, v);
			}
		}
		return result;
	}

	@Override
	public V getAndPut(K key, V value) {
		int currSize;
		do {
			currSize = size.get();
			if ( currSize < internalCapacity && size.compareAndSet(currSize, currSize + 1) ) {
				V v = internalStore.put(key, value);
				if ( v != null ) {
					// replaced existing value
					size.decrementAndGet();
				} else {
					publishCreatedEvent(key, value);
				}
				return v;
			}
		} while ( currSize < internalCapacity );
		return delegate.getAndPut(key, value);
	}

	private void publishCreatedEvent(K key, V value) {
		if ( createdListener != null ) {
			createdListener.onCreated(singleton(new CacheEntryCreatedEvent(key, value)));
		}
	}

	@Override
	public V getAndRemove(K key) {
		V old = internalStore.remove(key);
		if ( old != null ) {
			size.decrementAndGet();
		} else {
			old = delegate.getAndRemove(key);
		}
		return old;
	}

	@Override
	public V getAndReplace(K key, V value) {
		V old = internalStore.replace(key, value);
		if ( old == null ) {
			old = delegate.getAndReplace(key, value);
		}
		return old;
	}

	@Override
	public CacheManager getCacheManager() {
		return delegate.getCacheManager();
	}

	@Override
	public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
		return delegate.getConfiguration(clazz);
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public <T> T invoke(K key, EntryProcessor<K, V, T> processor, Object... arg2)
			throws EntryProcessorException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> arg0,
			EntryProcessor<K, V, T> arg1, Object... arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	/**
	 * Get an iterator over all cache elements.
	 * 
	 * <p>
	 * The iterator returned by this method will return the cache elements
	 * stored internally first, followed by any elements stored in the delegate
	 * {@link Cache}.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Entry<K, V>> iterator() {
		List<Iterator<Entry<K, V>>> combined = new ArrayList<>(2);
		combined.add(new EntryIteratorAdaptor(internalStore.entrySet().iterator()));
		combined.add(delegate.iterator());
		return new UnionIterator<Entry<K, V>>(combined);
	}

	@Override
	public void loadAll(Set<? extends K> keys, boolean arg1, CompletionListener arg2) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Add an element to the cache.
	 * 
	 * <p>
	 * If less than {@link #getInternalCapacity()} elements are in the cache
	 * when this is invoked, the element will be added to the internal store.
	 * Otherwise it will be put into the delegate {@link Cache}.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void put(K key, V value) {
		int currSize;
		do {
			currSize = size.get();
			if ( currSize < internalCapacity && size.compareAndSet(currSize, currSize + 1) ) {
				if ( internalStore.put(key, value) != null ) {
					// only replaced existing value
					size.decrementAndGet();
				} else {
					publishCreatedEvent(key, value);
				}
				return;
			}
		} while ( currSize < internalCapacity );
		delegate.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		if ( map == null || map.isEmpty() ) {
			return;
		}
		for ( Map.Entry<? extends K, ? extends V> me : map.entrySet() ) {
			put(me.getKey(), me.getValue());
		}
	}

	/**
	 * Add an element to the cache only if its key does not already exist in the
	 * cache.
	 * 
	 * @throws UnsupportedOperationException
	 *         always
	 */
	@Override
	public boolean putIfAbsent(K key, V value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Register a cache entry listener configuration.
	 * 
	 * <p>
	 * <b>Note</b> this method only supports registering a <b>single</b>
	 * {@link CacheEntryCreatedListener} instance. The configuration will be
	 * shared between this instance and the delegate {@link Cache}.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> config) {
		CacheEntryListener<? super K, ? super V> listener = config.getCacheEntryListenerFactory()
				.create();
		if ( listener instanceof CacheEntryCreatedListener<?, ?> ) {
			this.createdListener = (CacheEntryCreatedListener<? super K, ? super V>) listener;
		}
		delegate.registerCacheEntryListener(config);
	}

	@Override
	public boolean remove(K key) {
		if ( internalStore.remove(key) != null ) {
			size.decrementAndGet();
			return true;
		}
		return delegate.remove(key);
	}

	@Override
	public boolean remove(K key, V expected) {
		if ( internalStore.remove(key, expected) ) {
			size.decrementAndGet();
			return true;
		}
		return delegate.remove(key, expected);
	}

	@Override
	public synchronized void removeAll() {
		internalStore.clear();
		size.set(0);
		delegate.removeAll();
	}

	@Override
	public void removeAll(Set<? extends K> keys) {
		for ( K k : keys ) {
			if ( internalStore.remove(k) != null ) {
				size.decrementAndGet();
			}
		}
		delegate.removeAll(keys);
	}

	@Override
	public boolean replace(K key, V value) {
		if ( internalStore.replace(key, value) != null ) {
			return true;
		}
		return delegate.replace(key, value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		if ( internalStore.replace(key, oldValue, newValue) ) {
			return true;
		}
		return delegate.replace(key, oldValue, newValue);
	}

	/**
	 * Unwrap an object.
	 * 
	 * @throws UnsupportedOperationException
	 *         always
	 */
	@Override
	public <T> T unwrap(Class<T> arg0) {
		throw new UnsupportedOperationException();
	}

	private class EntryIteratorAdaptor implements Iterator<Entry<K, V>> {

		private final Iterator<Map.Entry<K, V>> itr;

		private EntryIteratorAdaptor(Iterator<Map.Entry<K, V>> itr) {
			super();
			this.itr = itr;
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public Entry<K, V> next() {
			Map.Entry<K, V> e = itr.next();
			if ( e == null ) {
				throw new NoSuchElementException();
			}
			return new EntryAdaptor(e);
		}
	}

	private class EntryAdaptor implements Entry<K, V> {

		private final Map.Entry<K, V> e;

		private EntryAdaptor(Map.Entry<K, V> e) {
			super();
			this.e = e;
		}

		@Override
		public K getKey() {
			return e.getKey();
		}

		@Override
		public V getValue() {
			return e.getValue();
		}

		@Override
		public <T> T unwrap(Class<T> clazz) {
			throw new UnsupportedOperationException();
		}
	}

	private class CacheEntryCreatedEvent extends CacheEntryEvent<K, V> {

		private static final long serialVersionUID = 6369201595734400543L;

		private final K key;
		private final V value;

		private CacheEntryCreatedEvent(K key, V value) {
			super(BufferingDelegatingCache.this, EventType.CREATED);
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
