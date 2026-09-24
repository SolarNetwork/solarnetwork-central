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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.util.CachedResult;

/**
 * Basic implementation of {@link SharedValueCache} using {@link ConcurrentMap}
 * objects for storage.
 *
 * <p>
 * This implementation is thread-safe. The two backing maps are updated
 * independently, so {@link #put(Object, Object, Function, long)} and
 * {@link #prune()} are serialized against each other with a
 * {@link ReadWriteLock}: concurrent {@code put()} calls proceed in parallel
 * under the read lock, while {@code prune()} takes the write lock. Value
 * computation happens <em>outside</em> the lock, so {@code prune()} never waits
 * on an expensive {@code valueProvider} and vice versa. This maintains the
 * invariant that the value of a valid primary cache entry is always reachable
 * from the shared cache; without it a {@code prune()} could discard a shared
 * value published by an in-flight {@code put()}.
 * </p>
 *
 * <p>
 * {@link #get(Object)} acquires no locks.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class BasicSharedValueCache<K, V, S> implements SharedValueCache<K, V, S> {

	/**
	 * The maximum supported time to live, in seconds (about 1,000 years).
	 *
	 * <p>
	 * TTL values are clamped to this maximum so the computed entry expiration
	 * time cannot overflow.
	 * </p>
	 *
	 * @since 1.1
	 */
	public static final long MAX_TTL_SECONDS = TimeUnit.DAYS.toSeconds(365_000L);

	private final ConcurrentMap<K, CachedResult<V>> cache;
	private final ConcurrentMap<S, V> sharedCache;

	/** Serializes {@code prune()} against {@code put()}. */
	private final ReadWriteLock pruneLock = new ReentrantReadWriteLock();

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
		this.cache = requireNonNullArgument(cache, "cache");
		this.sharedCache = requireNonNullArgument(sharedCache, "sharedCache");
	}

	@Override
	public @Nullable V get(K key) {
		CachedResult<V> entry = cache.get(key);
		return (entry != null && entry.isValid() ? entry.getResult() : null);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The {@code valueProvider} is invoked at most once per {@code shareKey},
	 * while holding an internal lock on the shared cache. It must not call back
	 * into this cache, and must not return {@code null}.
	 * </p>
	 *
	 * <p>
	 * A {@code ttl} of zero or less caches an entry that has already expired,
	 * so the value will be returned here but not by a subsequent
	 * {@link #get(Object)}. Values larger than {@link #MAX_TTL_SECONDS} are
	 * clamped to that maximum.
	 * </p>
	 *
	 * @throws IllegalStateException
	 *         if {@code valueProvider} returns {@code null}
	 */
	@Override
	public V put(K key, S shareKey, Function<S, V> valueProvider, long ttl) {
		// compute outside of the prune lock, so prune() never waits on the provider
		final V sharedValue = nonnull(sharedCache.computeIfAbsent(shareKey, valueProvider),
				"shared value for key %s", shareKey);
		final long entryTtl = Math.min(ttl, MAX_TTL_SECONDS);
		pruneLock.readLock().lock();
		try {
			// re-publish the shared value, in case a prune() removed it after the
			// computeIfAbsent() above, and adopt whichever instance is canonical so
			// that only one instance per share key is ever handed out
			final @Nullable V published = sharedCache.putIfAbsent(shareKey, sharedValue);
			final V value = (published != null ? published : sharedValue);
			cache.put(key, new CachedResult<>(value, entryTtl, TimeUnit.SECONDS));
			return value;
		} finally {
			pruneLock.readLock().unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Expired primary cache entries are removed, then shared values no longer
	 * referenced by any remaining entry are removed. Shared values are compared
	 * by identity, so this does not depend on the {@code equals()} semantics of
	 * the value type.
	 * </p>
	 */
	@Override
	public void prune() {
		pruneLock.writeLock().lock();
		try {
			Set<V> inUse = Collections.newSetFromMap(new IdentityHashMap<>(32));
			for ( Iterator<CachedResult<V>> itr = cache.values().iterator(); itr.hasNext(); ) {
				CachedResult<V> entry = itr.next();
				if ( !entry.isValid() ) {
					itr.remove();
					continue;
				}
				V value = entry.getResult();
				if ( value != null ) {
					inUse.add(value);
				}
			}
			sharedCache.values().removeIf(value -> !inUse.contains(value));
		} finally {
			pruneLock.writeLock().unlock();
		}
	}

}
