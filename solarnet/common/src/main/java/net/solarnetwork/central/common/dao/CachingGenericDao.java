/* ==================================================================
 * CachingGenericDao.java - 24/02/2024 3:35:36 pm
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

package net.solarnetwork.central.common.dao;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import javax.cache.Cache;
import javax.cache.Cache.Entry;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.ObjectUtils;

/**
 * Proxy implementation of {@link GenericDao} with caching support.
 * 
 * @param <T>
 *        the entity type managed by this DAO
 * @param <K>
 *        the entity primary key type
 * @param <D>
 *        the delegate DAO type
 * @author matt
 * @version 1.0
 */
public class CachingGenericDao<T extends Entity<K>, K, D extends GenericDao<T, K>>
		implements GenericDao<T, K> {

	/** The delegate DAO. */
	protected final D delegate;

	/** The cache. */
	protected final Cache<K, T> cache;

	/** A background task executor. */
	protected final Executor executor;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate DAO
	 * @param cache
	 *        the cache
	 * @param executor
	 *        task executor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CachingGenericDao(D delegate, Cache<K, T> cache, Executor executor) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		this.cache = ObjectUtils.requireNonNullArgument(cache, "cache");
		this.executor = ObjectUtils.requireNonNullArgument(executor, "executor");
	}

	@Override
	public String entityEventTopic(EntityEventType eventType) {
		return delegate.entityEventTopic(eventType);
	}

	@Override
	public Class<? extends T> getObjectType() {
		return delegate.getObjectType();
	}

	@Override
	public K save(T entity) {
		K result = delegate.save(entity);
		if ( result != null ) {
			cache.remove(result);
		}
		return result;
	}

	@Override
	public T get(K id) {
		T result = cache.get(id);
		if ( result == null ) {
			result = delegate.get(id);
			if ( result != null ) {
				cache.put(id, result);
			}
		}
		return result;
	}

	@Override
	public Collection<T> getAll(List<SortDescriptor> sorts) {
		return delegate.getAll(sorts);
	}

	@Override
	public void delete(T entity) {
		delegate.delete(entity);
		if ( entity.getId() != null ) {
			cache.remove(entity.getId());
		}
	}

	/**
	 * Evict all keys matching a predicate.
	 * 
	 * <p>
	 * This method will submit a task to the configured {@link Executor} to
	 * perform the eviction, and then return immediately.
	 * </p>
	 * 
	 * @param filter
	 *        the predicate to match keys of entries to evict from the cache
	 */
	protected void evictKeysMatching(Predicate<K> filter) {
		executor.execute(() -> {
			for ( Iterator<Entry<K, T>> itr = cache.iterator(); itr.hasNext(); ) {
				Entry<K, T> entry = itr.next();
				if ( entry == null ) {
					continue;
				}
				if ( filter.test(entry.getKey()) ) {
					itr.remove();
				}
			}
		});
	}

}
