/* ==================================================================
 * BasicSharedValueCacheConcurrencyTests.java - 24/09/2026 9:12:04 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.util.CachedResult;

/**
 * Concurrency test cases for the {@link BasicSharedValueCache} class.
 *
 * <p>
 * {@link SharedValueCache} requires implementations to be thread-safe, as every
 * deployment shares a single instance across request threads plus the
 * {@code SharedValueCacheCleaner} job thread. These tests exercise that usage,
 * in particular that a value is computed only once per share key and that a
 * {@code prune()} never discards a shared value an in-flight {@code put()} is
 * about to reference.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class BasicSharedValueCacheConcurrencyTests {

	/** Bound on any latch/barrier/future wait, to fail rather than hang. */
	private static final long TIMEOUT_SECS = 20L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private ConcurrentMap<Integer, CachedResult<UUID>> primaryCache;
	private ConcurrentMap<String, UUID> sharedCache;
	private BasicSharedValueCache<Integer, UUID, String> cache;
	private ExecutorService executor;

	@BeforeEach
	public void setup() {
		primaryCache = new ConcurrentHashMap<>();
		sharedCache = new ConcurrentHashMap<>();
		cache = new BasicSharedValueCache<>(primaryCache, sharedCache);
	}

	@AfterEach
	public void teardown() {
		if ( executor != null ) {
			executor.shutdownNow();
		}
	}

	/**
	 * A counting value provider, to verify how often an "expensive" shared
	 * value is actually computed.
	 */
	private static final class CountingProvider implements Function<String, UUID> {

		private final AtomicInteger count = new AtomicInteger();
		private final ConcurrentMap<String, AtomicInteger> countByShareKey = new ConcurrentHashMap<>(8);
		private final long computeDelayMs;

		private CountingProvider(long computeDelayMs) {
			super();
			this.computeDelayMs = computeDelayMs;
		}

		@Override
		public UUID apply(String shareKey) {
			count.incrementAndGet();
			countByShareKey.computeIfAbsent(shareKey, _ -> new AtomicInteger()).incrementAndGet();
			if ( computeDelayMs > 0 ) {
				try {
					Thread.sleep(computeDelayMs);
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
			}
			return UUID.randomUUID();
		}

		private int count() {
			return count.get();
		}

		private int count(String shareKey) {
			AtomicInteger c = countByShareKey.get(shareKey);
			return (c != null ? c.get() : 0);
		}
	}

	@Test
	public void put_concurrent_singleShareKey_computedOnce() throws Exception {
		// GIVEN
		final int threadCount = 32;
		final var provider = new CountingProvider(25L);
		final var barrier = new CyclicBarrier(threadCount);

		// WHEN
		executor = Executors.newFixedThreadPool(threadCount);
		final List<Future<UUID>> futures = new ArrayList<>(threadCount);
		for ( int i = 0; i < threadCount; i++ ) {
			final int key = i;
			futures.add(executor.submit((Callable<UUID>) () -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				return cache.put(key, "a", provider, 60L);
			}));
		}

		final Set<UUID> results = new HashSet<>(threadCount);
		for ( Future<UUID> f : futures ) {
			results.add(f.get(TIMEOUT_SECS, SECONDS));
		}

		// THEN
		// @formatter:off
		then(provider.count())
			.as("Expensive shared value computed exactly once for %d concurrent put() calls", threadCount)
			.isEqualTo(1)
			;

		then(results)
			.as("Every caller received the one shared instance")
			.hasSize(1)
			;

		then(sharedCache)
			.as("Single shared value cached")
			.containsOnlyKeys("a")
			;

		then(primaryCache)
			.as("Every primary key cached")
			.hasSize(threadCount)
			;

		then(primaryCache.values().stream().map(CachedResult::getResult).distinct().toList())
			.as("Every primary entry references the same shared instance")
			.isEqualTo(List.copyOf(results))
			;
		// @formatter:on
	}

	@Test
	public void put_concurrent_multipleShareKeys_eachComputedOnce() throws Exception {
		// GIVEN
		final int shareKeyCount = 16;
		final int threadsPerShareKey = 4;
		final int threadCount = shareKeyCount * threadsPerShareKey;
		final var provider = new CountingProvider(10L);
		final var barrier = new CyclicBarrier(threadCount);

		// WHEN
		executor = Executors.newFixedThreadPool(threadCount);
		final List<Future<UUID>> futures = new ArrayList<>(threadCount);
		for ( int i = 0; i < threadCount; i++ ) {
			final int key = i;
			final String shareKey = "share-" + (i % shareKeyCount);
			futures.add(executor.submit((Callable<UUID>) () -> {
				barrier.await(TIMEOUT_SECS, SECONDS);
				return cache.put(key, shareKey, provider, 60L);
			}));
		}
		for ( Future<UUID> f : futures ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}

		// THEN
		// @formatter:off
		then(provider.count())
			.as("No more than %d computations for %d share keys", shareKeyCount, shareKeyCount)
			.isEqualTo(shareKeyCount)
			;

		then(sharedCache)
			.as("One shared value per share key")
			.hasSize(shareKeyCount)
			;

		then(primaryCache)
			.as("Every primary key cached")
			.hasSize(threadCount)
			;
		// @formatter:on

		for ( int i = 0; i < shareKeyCount; i++ ) {
			final String shareKey = "share-" + i;
			final UUID shared = sharedCache.get(shareKey);

			// @formatter:off
			then(provider.count(shareKey))
				.as("Share key %s computed exactly once, despite %d concurrent put() calls for it",
						shareKey, threadsPerShareKey)
				.isEqualTo(1)
				;
			// @formatter:on

			for ( int j = i; j < threadCount; j += shareKeyCount ) {
				// @formatter:off
				then(cache.get(j))
					.as("Primary key %d resolves to shared value for %s", j, shareKey)
					.isSameAs(shared)
					;
				// @formatter:on
			}
		}
	}

	@Test
	public void get_concurrent_withPut_neverSeesForeignValue() throws Exception {
		// GIVEN
		final int writerCount = 4;
		final int readerCount = 8;
		final int iterations = 2_000;
		final int keyCount = 32;
		final var provider = new CountingProvider(0L);
		final var stop = new AtomicBoolean(false);
		final var reads = new AtomicInteger();
		final var hits = new AtomicInteger();

		// WHEN
		executor = Executors.newFixedThreadPool(writerCount + readerCount);
		final List<Future<?>> futures = new ArrayList<>(writerCount + readerCount);

		for ( int w = 0; w < writerCount; w++ ) {
			futures.add(executor.submit(() -> {
				for ( int i = 0; i < iterations; i++ ) {
					int key = i % keyCount;
					cache.put(key, "share-" + key, provider, 60L);
				}
			}));
		}

		for ( int r = 0; r < readerCount; r++ ) {
			futures.add(executor.submit(() -> {
				while ( !stop.get() ) {
					for ( int key = 0; key < keyCount; key++ ) {
						UUID val = cache.get(key);
						reads.incrementAndGet();
						if ( val != null ) {
							hits.incrementAndGet();
							// a reader must only ever observe a value the provider produced
							if ( !sharedCache.containsValue(val)
									&& !primaryCacheContainsValue(val) ) {
								throw new IllegalStateException(
										"Reader observed a value present in neither cache: " + val);
							}
						}
					}
				}
			}));
		}

		for ( int i = 0; i < writerCount; i++ ) {
			futures.get(i).get(TIMEOUT_SECS, SECONDS);
		}
		stop.set(true);
		for ( Future<?> f : futures ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}

		log.info("Concurrent get/put: {} reads, {} hits, {} computations", reads.get(), hits.get(),
				provider.count());

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("All %d primary keys cached", keyCount)
			.hasSize(keyCount)
			;

		then(sharedCache)
			.as("All %d shared values cached", keyCount)
			.hasSize(keyCount)
			;

		then(provider.count())
			.as("Each share key computed exactly once despite %d concurrent put() calls",
					writerCount * iterations)
			.isEqualTo(keyCount)
			;
		// @formatter:on
	}

	private boolean primaryCacheContainsValue(UUID val) {
		return primaryCache.values().stream().anyMatch(e -> e.getResult() == val);
	}

	@Test
	public void prune_concurrent_withPutAndGet_staysConsistent() throws Exception {
		// GIVEN
		final int writerCount = 4;
		final int iterations = 2_000;
		final int keyCount = 32;
		final var provider = new CountingProvider(0L);
		final var stop = new AtomicBoolean(false);
		final var pruneCount = new AtomicInteger();

		// WHEN
		executor = Executors.newFixedThreadPool(writerCount + 1);
		final List<Future<?>> futures = new ArrayList<>(writerCount + 1);

		for ( int w = 0; w < writerCount; w++ ) {
			futures.add(executor.submit(() -> {
				for ( int i = 0; i < iterations; i++ ) {
					int key = i % keyCount;
					// half the entries expire immediately, so prune() has work to do
					long ttl = (key % 2 == 0 ? 60L : 0L);
					UUID val = cache.put(key, "share-" + key, provider, ttl);
					if ( val == null ) {
						throw new IllegalStateException("put() returned null for key " + key);
					}
				}
			}));
		}

		final Future<?> pruner = executor.submit(() -> {
			while ( !stop.get() ) {
				cache.prune();
				pruneCount.incrementAndGet();
			}
		});
		futures.add(pruner);

		for ( int i = 0; i < writerCount; i++ ) {
			futures.get(i).get(TIMEOUT_SECS, SECONDS);
		}
		stop.set(true);
		pruner.get(TIMEOUT_SECS, SECONDS);

		// a final quiescent prune, so the expired-entry assertions are deterministic
		cache.prune();

		log.info("Concurrent prune: {} prunes, {} computations (minimum {})", pruneCount.get(),
				provider.count(), keyCount);

		// THEN
		// @formatter:off
		then(pruneCount.get())
			.as("prune() ran concurrently with put()")
			.isPositive()
			;

		then(primaryCache.keySet())
			.as("Only long-lived keys survive a quiescent prune")
			.allSatisfy(key -> then(key % 2).as("Key %d has a non-zero TTL", key).isZero())
			.as("All long-lived keys survive a quiescent prune")
			.hasSize(keyCount / 2)
			;

		then(provider.count())
			.as("Each share key computed at least once")
			.isGreaterThanOrEqualTo(keyCount)
			;

		// the cache is still usable after concurrent pruning
		then(cache.put(1000, "share-1000", provider, 60L))
			.as("Cache still functional after concurrent pruning")
			.isNotNull()
			.isSameAs(cache.get(1000))
			;
		// @formatter:on
	}

	@Test
	public void prune_concurrent_withFreshShareKeys_retainsInUseSharedValues() throws Exception {
		// GIVEN
		// every put() introduces a NEW share key, which is when a shared value is
		// most vulnerable to a concurrent prune(): no other primary entry references
		// it yet. This is what happens when a transform configuration is edited, as
		// the cache key includes the entity modification date and the share key is a
		// digest of the (changed) XSLT.
		final int writerCount = 4;
		final int minPrunes = 10;
		final int maxPutsPerWriter = 10_000;
		final var provider = new CountingProvider(0L);
		final var pruneCount = new AtomicInteger();
		final var keySequence = new AtomicInteger();
		final var putCount = new AtomicInteger();
		final var pruneStarted = new CountDownLatch(1);

		// WHEN
		executor = Executors.newFixedThreadPool(writerCount + 1);

		// the pruner drives the test: it prunes until minPrunes is reached, and the
		// writers keep going until then, so put() and prune() always overlap
		final Future<?> pruner = executor.submit(() -> {
			while ( pruneCount.get() < minPrunes ) {
				cache.prune();
				pruneCount.incrementAndGet();
				pruneStarted.countDown();
				// throttle, as prune() excludes put() and would otherwise starve it
				try {
					Thread.sleep(1L);
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		});

		then(pruneStarted.await(TIMEOUT_SECS, SECONDS)).as("Pruner thread started").isTrue();

		final List<Future<?>> writers = new ArrayList<>(writerCount);
		for ( int w = 0; w < writerCount; w++ ) {
			writers.add(executor.submit(() -> {
				for ( int i = 0; i < maxPutsPerWriter && pruneCount.get() < minPrunes; i++ ) {
					int key = keySequence.getAndIncrement();
					// a long TTL, so nothing should ever be eligible for pruning
					cache.put(key, "fresh-" + key, provider, 600L);
					putCount.incrementAndGet();
				}
			}));
		}
		for ( Future<?> f : writers ) {
			f.get(TIMEOUT_SECS, SECONDS);
		}
		pruner.get(TIMEOUT_SECS, SECONDS);

		// every value held by a valid primary entry must still be reachable from the
		// shared cache, or an in-flight put() lost its shared value to a prune()
		final Set<UUID> sharedValues = Collections.newSetFromMap(new IdentityHashMap<UUID, Boolean>());
		sharedValues.addAll(sharedCache.values());
		final Set<UUID> unreachable = Collections.newSetFromMap(new IdentityHashMap<UUID, Boolean>());
		for ( CachedResult<UUID> entry : primaryCache.values() ) {
			UUID value = entry.getResult();
			if ( entry.isValid() && !sharedValues.contains(value) ) {
				unreachable.add(value);
			}
		}

		log.info("Concurrent prune with fresh share keys: {} puts, {} prunes, {} unreachable",
				putCount.get(), pruneCount.get(), unreachable.size());

		// THEN
		// @formatter:off
		then(pruneCount.get())
			.as("prune() ran concurrently with put()")
			.isGreaterThanOrEqualTo(minPrunes)
			;

		then(putCount.get())
			.as("put() ran while prune() was running")
			.isPositive()
			;

		then(unreachable)
			.as("Every value held by a valid primary entry is still in the shared cache")
			.isEmpty()
			;

		then(provider.count())
			.as("Each fresh share key computed exactly once")
			.isEqualTo(putCount.get())
			;

		then(sharedCache)
			.as("One shared value retained per share key still in use")
			.hasSize(putCount.get())
			;
		// @formatter:on
	}

}
