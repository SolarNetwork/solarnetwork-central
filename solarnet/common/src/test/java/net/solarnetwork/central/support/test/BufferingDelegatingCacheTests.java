/* ==================================================================
 * BufferingDelegatingCacheTests.java - 28/03/2020 6:06:36 pm
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

package net.solarnetwork.central.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.FactoryBuilder.SingletonFactory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.central.support.JCacheFactoryBean;

/**
 * Test cases for the {@link BufferingDelegatingCache} class.
 * 
 * @author matt
 * @version 2.0
 */
public class BufferingDelegatingCacheTests implements CacheEntryCreatedListener<Integer, Integer>,
		CacheEntryUpdatedListener<Integer, Integer>, CacheEntryRemovedListener<Integer, Integer> {

	private final List<Integer> createdListenerData = Collections.synchronizedList(new ArrayList<>());
	private final List<Integer> updatedListenerData = Collections.synchronizedList(new ArrayList<>());
	private final List<Integer> removedListenerData = Collections.synchronizedList(new ArrayList<>());

	private ExecutorService executor;
	private CacheManager cacheManager;
	private Cache<Integer, Integer> delegate;
	private ConcurrentMap<Integer, Integer> map;
	private BufferingDelegatingCache<Integer, Integer> cache;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@BeforeEach
	public void setup() throws Exception {
		executor = Executors.newWorkStealingPool();
		cacheManager = createCacheManager();
		JCacheFactoryBean<Integer, Integer> factory = new JCacheFactoryBean<>(cacheManager,
				Integer.class, Integer.class);
		factory.setName("Test Buffering Delegate");
		factory.setHeapMaxEntries(10000);
		factory.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		delegate = factory.getObject();
		map = new ConcurrentHashMap<>(50);
	}

	@AfterEach
	public void teardown() throws Throwable {
		if ( cache != null ) {
			cache.close();
		}
		cacheManager.destroyCache("Test Buffering Delegate");
		executor.shutdown();
	}

	@Override
	public void onCreated(Iterable<CacheEntryEvent<? extends Integer, ? extends Integer>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Integer, ? extends Integer> event : events ) {
			createdListenerData.add(event.getKey());
		}
	}

	@Override
	public void onUpdated(Iterable<CacheEntryEvent<? extends Integer, ? extends Integer>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Integer, ? extends Integer> event : events ) {
			updatedListenerData.add(event.getKey());
		}
	}

	@Override
	public void onRemoved(Iterable<CacheEntryEvent<? extends Integer, ? extends Integer>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends Integer, ? extends Integer> event : events ) {
			removedListenerData.add(event.getKey());
		}
	}

	public static CacheManager createCacheManager() {
		try {
			File path = Files.createTempDirectory("net.solarnetwork.central.common.test").toFile();
			path.deleteOnExit();
			EhcacheCachingProvider cachingProvider = (EhcacheCachingProvider) Caching
					.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
			DefaultConfiguration configuration = new DefaultConfiguration(
					cachingProvider.getDefaultClassLoader(), new DefaultPersistenceConfiguration(path));
			return cachingProvider.getCacheManager(cachingProvider.getDefaultURI(), configuration);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	private void addListener(Cache<Integer, Integer> cache) {
		CacheEntryListenerConfiguration<Integer, Integer> config = new MutableCacheEntryListenerConfiguration<>(
				new SingletonFactory<CacheEntryListener<Integer, Integer>>(this), null, false, false);
		cache.registerCacheEntryListener(config);
	}

	@Test
	public void put() {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 10, map);

		// WHEN
		Integer i = 1;
		cache.put(i, i);

		// THEN
		assertThat("Map has entry", map, hasEntry(i, i));
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(1));
		assertThat("Delegate does not have entry", delegate.containsKey(i), equalTo(false));
	}

	@Test
	public void putUpToLimit() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 100, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = i;
			keys.add(k);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					cache.put(k, k);
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertThat("Map has all entries", map.keySet(), equalTo(keys));
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		assertThat("High watermark value", cache.getInternalSizeWatermark(), equalTo(keys.size()));
		assertThat("Delegate does not have any entries", delegate.getAll(keys).keySet(), hasSize(0));
	}

	@Test
	public void putOverLimit() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 50, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = i;
			keys.add(k);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					cache.put(k, k);
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertThat("Map has max entries", map.keySet(), hasSize(50));
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(50));

		Set<Integer> overflow = new LinkedHashSet<>(keys);
		for ( Integer k : map.keySet() ) {
			overflow.remove(k);
		}
		assertThat("Delegate has overflow entries", delegate.getAll(keys).keySet(), equalTo(overflow));
	}

	@Test
	public void putOverLimit_withListener() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 50, map);
		addListener(cache);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = i;
			keys.add(k);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					cache.put(k, k);
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertThat("Map has max entries", map.keySet(), hasSize(50));
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(50));

		Set<Integer> overflow = new LinkedHashSet<>(keys);
		for ( Integer k : map.keySet() ) {
			overflow.remove(k);
		}
		assertThat("Delegate has overflow entries", delegate.getAll(keys).keySet(), equalTo(overflow));
		assertThat("Listener called for all put values", new TreeSet<>(createdListenerData),
				equalTo(keys));
	}

	@Test
	public void mixOfPutAndRemovalsWitinLimit() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 100, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = (int) (Math.random() * 10);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					synchronized ( cache ) {
						if ( Math.random() < 0.5 ) {
							keys.add(k);
							cache.put(k, k);
						} else {
							keys.remove(k);
							cache.remove(k);
						}
					}
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		assertThat("High watermark value", cache.getInternalSizeWatermark(),
				allOf(greaterThan(0), lessThanOrEqualTo(10)));
		assertThat("Map has non-removed entries", map.keySet(), equalTo(keys));
		assertThat("Delegate does not have any entries", delegate.getAll(keys).keySet(), hasSize(0));
	}

	@Test
	public void mixOfPutAndRemovalsOverLimit() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 50, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 10000; i++ ) {
			Integer k = (int) (Math.random() * 200);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					synchronized ( cache ) {
						if ( Math.random() < 0.5 ) {
							keys.add(k);
							cache.put(k, k);
						} else {
							keys.remove(k);
							cache.remove(k);
						}
					}
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		Thread.sleep(1000L);

		Set<Integer> internalKeys = map.keySet();
		Set<Integer> delegateKeys = StreamSupport.stream(delegate.spliterator(), false)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		Set<Integer> commonKeys = new LinkedHashSet<>();
		for ( Integer k : internalKeys ) {
			if ( delegateKeys.contains(k) ) {
				commonKeys.add(k);
			}
		}
		for ( Integer k : delegateKeys ) {
			if ( internalKeys.contains(k) ) {
				commonKeys.add(k);
			}
		}
		SortedSet<Integer> combinedKeys = new TreeSet<>(internalKeys);
		combinedKeys.addAll(delegateKeys);
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(internalKeys.size()));
		assertThat("High watermark value", cache.getInternalSizeWatermark(),
				allOf(greaterThan(0), lessThanOrEqualTo(50)));
		assertThat("No overlapping keys present", commonKeys, hasSize(0));
		assertThat("All keys accounted for between internal and delegate", combinedKeys, equalTo(keys));
	}

	@Test
	public void burstOfPutsWithSlowIterationDrain() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 50, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 1000; i++ ) {
			Integer k = i;
			executor.execute(new Runnable() {

				@Override
				public void run() {
					keys.add(k);
					cache.put(k, k);
				}
			});
		}
		ExecutorService executor2 = Executors.newFixedThreadPool(4);
		ConcurrentMap<Integer, Integer> shared = new ConcurrentHashMap<>(250);
		for ( int i = 0; i < 4; i++ ) {
			executor2.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						Set<Integer> claimed = new LinkedHashSet<>();
						int max = 250;
						synchronized ( cache ) {
							for ( Iterator<Entry<Integer, Integer>> itr = cache.iterator(); itr.hasNext()
									&& max > 0; ) {
								Entry<Integer, Integer> e = itr.next();
								if ( e != null && shared.putIfAbsent(e.getKey(), e.getKey()) == null ) {
									claimed.add(e.getKey());
									max--;
								}
							}
						}
						if ( claimed.isEmpty() ) {
							return;
						}
						for ( Integer k : claimed ) {
							try {
								Thread.sleep(5);
							} catch ( InterruptedException e ) {
								// ignore
							}
							log.trace("Removing key {}", k);
							cache.remove(k);
							shared.remove(k);
						}
					}
				}
			});
		}

		// THEN
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		executor2.shutdown();
		executor2.awaitTermination(10, TimeUnit.SECONDS);

		Set<Integer> internalKeys = map.keySet();
		Set<Integer> delegateKeys = StreamSupport.stream(delegate.spliterator(), false)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(internalKeys.size()));
		assertThat("All internal keys processed", internalKeys, hasSize(0));
		assertThat("All delegate keys processed", delegateKeys, hasSize(0));
	}

	@Test
	public void preloadedCacheDrain() throws Exception {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 50, map);
		addListener(cache);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 1000; i++ ) {
			Integer k = i;
			keys.add(k);
			delegate.put(k, k); // adding to delegate directly
		}
		ExecutorService executor2 = Executors.newFixedThreadPool(4);
		ConcurrentMap<Integer, Integer> shared = new ConcurrentHashMap<>(250);
		for ( int i = 0; i < 4; i++ ) {
			executor2.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						Set<Integer> claimed = new LinkedHashSet<>();
						int max = 250;
						synchronized ( cache ) {
							for ( Iterator<Entry<Integer, Integer>> itr = cache.iterator(); itr.hasNext()
									&& max > 0; ) {
								Entry<Integer, Integer> e = itr.next();
								if ( e != null && shared.putIfAbsent(e.getKey(), e.getKey()) == null ) {
									claimed.add(e.getKey());
									max--;
								}
							}
						}
						if ( claimed.isEmpty() ) {
							return;
						}
						for ( Integer k : claimed ) {
							try {
								Thread.sleep(5);
							} catch ( InterruptedException e ) {
								// ignore
							}
							log.trace("Removing key {}", k);
							cache.remove(k);
							shared.remove(k);
						}
					}
				}
			});
		}

		// THEN
		executor2.shutdown();
		executor2.awaitTermination(60, TimeUnit.SECONDS);

		Set<Integer> internalKeys = map.keySet();
		Set<Integer> delegateKeys = StreamSupport.stream(delegate.spliterator(), false)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(internalKeys.size()));
		assertThat("All internal keys processed", internalKeys, hasSize(0));
		assertThat("All delegate keys processed", delegateKeys, hasSize(0));
		assertThat("Listener called for all removes", removedListenerData, hasSize(keys.size()));
		assertThat("Listener called for all removes", new TreeSet<>(removedListenerData), equalTo(keys));
	}

	@Test
	public void saveToPersistentCacheAtShutdown() throws Exception {
		// GIVEN
		JCacheFactoryBean<Integer, Integer> factory1 = new JCacheFactoryBean<>(cacheManager,
				Integer.class, Integer.class);
		factory1.setName("Test Buffering Delegate Persistence");
		factory1.setHeapMaxEntries(3);
		factory1.setDiskMaxSizeMB(1);
		factory1.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		factory1.setDiskPersistent(true);
		Cache<Integer, Integer> cache1 = factory1.getObject();
		cache = new BufferingDelegatingCache<>(cache1, 100, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 10; i++ ) {
			Integer k = i;
			keys.add(k);
			cache.put(k, k);
		}
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		cache.close();
		assertThat("Reported internal size reset to 0", cache.getInternalSize(), equalTo(0));

		// re-open delegate cache
		JCacheFactoryBean<Integer, Integer> factory2 = new JCacheFactoryBean<>(cacheManager,
				Integer.class, Integer.class);
		factory2.setName("Test Buffering Delegate Persistence");
		factory2.setHeapMaxEntries(3);
		factory2.setDiskMaxSizeMB(1);
		factory2.setExpiryPolicy(JCacheFactoryBean.ExpiryPolicy.Eternal);
		factory2.setDiskPersistent(true);
		Cache<Integer, Integer> cache2 = factory2.getObject();

		// THEN

		Set<Integer> persistedKeys = StreamSupport.stream(cache2.spliterator(), false)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		assertThat("All temp keys persisted to cache", persistedKeys, equalTo(keys));
		cache2.close();
	}

	@Test
	public void fillDrainAndRefill() {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 100, map);

		// WHEN
		SortedSet<Integer> keys = new ConcurrentSkipListSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = i;
			keys.add(k);
			cache.put(k, k);
		}
		for ( int i = 0; i < 100; i += 2 ) {
			Integer k = i;
			keys.remove(k);
			cache.remove(k);
		}
		for ( int i = 200; i < 250; i++ ) {
			Integer k = i;
			keys.add(k);
			cache.put(k, k);
		}

		// THEN
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		assertThat("Map has all entries", map.keySet(), equalTo(keys));
		assertThat("Delegate does not have any entries", delegate.getAll(keys).keySet(), hasSize(0));
	}

	@Test
	public void fillDrainAndRefill_withListener() {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 100, map);
		addListener(cache);

		// WHEN
		SortedSet<Integer> keys = new TreeSet<>();
		SortedSet<Integer> allPuts = new TreeSet<>();
		SortedSet<Integer> allRemoves = new TreeSet<>();
		for ( int i = 0; i < 100; i++ ) {
			Integer k = i;
			keys.add(k);
			allPuts.add(k);
			cache.put(k, k);
		}
		for ( int i = 0; i < 100; i += 2 ) {
			Integer k = i;
			keys.remove(k);
			allRemoves.add(k);
			cache.remove(k);
		}
		for ( int i = 200; i < 250; i++ ) {
			Integer k = i;
			keys.add(k);
			allPuts.add(k);
			cache.put(k, k);
		}

		// THEN
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		assertThat("Map has all entries", map.keySet(), equalTo(keys));
		assertThat("Delegate does not have any entries", delegate.getAll(keys).keySet(), hasSize(0));
		assertThat("Listener called for all puts", new TreeSet<>(createdListenerData), equalTo(allPuts));
		assertThat("Listener called for all removes", new TreeSet<>(removedListenerData),
				equalTo(allRemoves));
	}

	@Test
	public void fillDrainAndRefill_withDuplicates_withListener() {
		// GIVEN
		cache = new BufferingDelegatingCache<>(delegate, 100, map);
		addListener(cache);

		// WHEN
		SortedSet<Integer> keys = new TreeSet<>();
		SortedSet<Integer> allPuts = new TreeSet<>();
		SortedSet<Integer> allUpdates = new TreeSet<>();
		SortedSet<Integer> allRemoves = new TreeSet<>();
		for ( int i = 0; i < 50; i++ ) {
			Integer k = i;
			keys.add(k);
			allPuts.add(k);
			cache.put(k, k);
		}
		assertThat("High watermark value", cache.getInternalSizeWatermark(), equalTo(50));
		for ( int i = 0; i < 50; i += 2 ) {
			Integer k = i;
			allUpdates.add(k);
			cache.put(k, k + 10000);
		}
		assertThat("High watermark value unchanged after updates", cache.getInternalSizeWatermark(),
				equalTo(50));
		for ( int i = 200; i < 250; i++ ) {
			Integer k = i;
			keys.add(k);
			allPuts.add(k);
			cache.put(k, k);
		}
		assertThat("High watermark value updated", cache.getInternalSizeWatermark(), equalTo(100));

		// THEN
		assertThat("Reported internal size", cache.getInternalSize(), equalTo(keys.size()));
		assertThat("Map has all entries", map.keySet(), equalTo(keys));
		assertThat("Delegate does not have any entries", delegate.getAll(keys).keySet(), hasSize(0));
		assertThat("Listener called for all puts", new TreeSet<>(createdListenerData), equalTo(allPuts));
		assertThat("Listener called for all updates", new TreeSet<>(updatedListenerData),
				equalTo(allUpdates));
		assertThat("Listener called for all removes", new TreeSet<>(removedListenerData),
				equalTo(allRemoves));
	}

}
