/* ==================================================================
 * SimpleCacheTests.java - 9/08/2023 7:28:19 am
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

package net.solarnetwork.central.support.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache.Entry;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.util.CachedResult;

/**
 * Test cases for the {@link SimpleCache}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCacheTests {

	@Test
	public void name() {
		// GIVEN
		String name = UUID.randomUUID().toString();

		// WHEN
		SimpleCache<String, String> cache = new SimpleCache<>(name);

		// THEN
		// @formatter:off
		then(cache).asInstanceOf(type(SimpleCache.class)) // cast from IterableAssert
			.as("Given name")
			.returns(name, SimpleCache::getName)
			;
		// @formatter:on
	}

	private SimpleCache<String, String> cache(long ttl, TimeUnit timeUnit) {
		SimpleCache<String, String> cache = new SimpleCache<>(UUID.randomUUID().toString());
		cache.setTtl(ttl);
		cache.setTimeUnit(timeUnit);
		return cache;
	}

	private SimpleCache<String, String> cache(long ttl, TimeUnit timeUnit,
			ConcurrentMap<String, CachedResult<String>> data) {
		SimpleCache<String, String> cache = new SimpleCache<>(UUID.randomUUID().toString(), data);
		cache.setTtl(ttl);
		cache.setTimeUnit(timeUnit);
		return cache;
	}

	@Test
	public void putAndGet() {
		// GIVEN
		final SimpleCache<String, String> cache = cache(1, MINUTES);

		// WHEN
		final String k1 = UUID.randomUUID().toString();
		final String v1 = UUID.randomUUID().toString();
		cache.put(k1, v1);
		String result = cache.get(k1);

		// THEN
		then(result).as("Cached result returned").isEqualTo(v1);
	}

	@Test
	public void replace() {
		// GIVEN
		final String k1 = UUID.randomUUID().toString();
		final String k2 = UUID.randomUUID().toString();
		final String v1 = UUID.randomUUID().toString();
		final String v2 = UUID.randomUUID().toString();
		final String v3 = UUID.randomUUID().toString();
		final SimpleCache<String, String> cache = cache(1, MINUTES);
		cache.put(k1, v1);

		// WHEN
		boolean result1 = cache.replace(k1, v1, v2);
		boolean result2 = cache.replace(k1, v1, v3);
		boolean result3 = cache.replace(k2, v1);

		// THEN
		then(result1).as("k1 v2 replaces v1").isTrue();
		then(result2).as("k1 v3 does not replace v1, because currently v2").isFalse();
		then(result3).as("k2 v1 does not replace anything because currently unset").isFalse();
		then(cache.get(k1)).as("k1 ends up as v2").isEqualTo(v2);
		then(cache.get(k2)).as("k2 is not present").isNull();
	}

	@Test
	public void getAndReplace() {
		// GIVEN
		final String k1 = UUID.randomUUID().toString();
		final String k2 = UUID.randomUUID().toString();
		final String v1 = UUID.randomUUID().toString();
		final String v2 = UUID.randomUUID().toString();
		final String v3 = UUID.randomUUID().toString();
		final SimpleCache<String, String> cache = cache(1, MINUTES);
		cache.put(k1, v1);

		// WHEN
		String result1 = cache.getAndReplace(k1, v2);
		String result2 = cache.getAndReplace(k1, v3);
		String result3 = cache.getAndReplace(k2, v1);

		// THEN
		then(result1).as("k1 v2 replaces v1").isEqualTo(v1);
		then(result2).as("k1 v3 replaces v2").isEqualTo(v2);
		then(result3).as("k2 v1 does not replace as k2 not present").isNull();
		then(cache.get(k1)).as("k1 ends up as v3").isEqualTo(v3);
		then(cache.get(k2)).as("k2 remains unset").isNull();
	}

	@Test
	public void get_expired() throws Exception {
		// GIVEN
		final SimpleCache<String, String> cache = cache(1L, MILLISECONDS);

		// WHEN
		final String k1 = UUID.randomUUID().toString();
		final String v1 = UUID.randomUUID().toString();
		cache.put(k1, v1);
		Thread.sleep(50L); // let cache expire
		String result = cache.get(k1);

		// THEN
		then(result).as("Expired cached result not returned").isNull();
	}

	@Test
	public void iteration_freeExpired() throws Exception {
		// GIVEN
		final ConcurrentMap<String, CachedResult<String>> data = new ConcurrentHashMap<>();
		final SimpleCache<String, String> cache = cache(1L, MILLISECONDS, data);
		final Map<String, String> longLived = new ConcurrentHashMap<>();
		final ExecutorService pool = Executors.newWorkStealingPool();

		// WHEN
		// populate a bunch of short-lived keys
		List<CompletableFuture<?>> puts = new ArrayList<>(50);
		for ( int i = 0; i < 50; i++ ) {
			puts.add(CompletableFuture.runAsync(() -> {
				String s = UUID.randomUUID().toString();
				cache.put(s, s);
			}, pool));
		}
		CompletableFuture.allOf(puts.toArray(CompletableFuture[]::new)).get();

		Thread.sleep(50L);

		// change TTL to 1 hour, populate some more
		cache.setTimeUnit(TimeUnit.HOURS);
		puts.clear();
		for ( int i = 0; i < 50; i++ ) {
			puts.add(CompletableFuture.runAsync(() -> {
				String s = UUID.randomUUID().toString();
				cache.put(s, s);
				longLived.put(s, s);
			}, pool));
		}
		CompletableFuture.allOf(puts.toArray(CompletableFuture[]::new)).get();

		// iterate over cache; should end up with all long-lived keys, and then
		// data map should only contain those keys because expired entries purged
		final Set<String> iteratedKeys = new HashSet<>();
		for ( Iterator<Entry<String, String>> itr = cache.iterator(); itr.hasNext(); ) {
			Entry<String, String> e = itr.next();
			iteratedKeys.add(e.getKey());
		}

		// THEN
		then(iteratedKeys).as("Iterated keys contains exactly long-lived entries")
				.hasSameElementsAs(longLived.keySet());
		then(data.keySet()).as("Data map purged of short-lived entries")
				.hasSameElementsAs(longLived.keySet());
	}

	@Test
	public void clear() throws Exception {
		// GIVEN
		final ConcurrentMap<String, CachedResult<String>> data = new ConcurrentHashMap<>();
		final SimpleCache<String, String> cache = cache(1L, MILLISECONDS, data);

		// WHEN
		// populate a bunch
		for ( int i = 0; i < 50; i++ ) {
			String s = UUID.randomUUID().toString();
			cache.put(s, s);
		}
		cache.clear();

		// THEN
		then(data).as("Data cleared on cache clear").isEmpty();
	}

	@Test
	public void removeAll() throws Exception {
		// GIVEN
		final ConcurrentMap<String, CachedResult<String>> data = new ConcurrentHashMap<>();
		final SimpleCache<String, String> cache = cache(1L, MILLISECONDS, data);

		// WHEN
		// populate a bunch
		for ( int i = 0; i < 50; i++ ) {
			String s = UUID.randomUUID().toString();
			cache.put(s, s);
		}
		cache.removeAll();

		// THEN
		then(data).as("Data cleared on cache removeAll").isEmpty();
	}

	@Test
	public void removeAllSet() throws Exception {
		// GIVEN
		final ConcurrentMap<String, CachedResult<String>> data = new ConcurrentHashMap<>();
		final SimpleCache<String, String> cache = cache(1L, MILLISECONDS, data);
		final SecureRandom rng = new SecureRandom();

		// WHEN
		// populate a bunch
		Set<String> keysToRemove = new HashSet<>();
		Set<String> keysToKeep = new HashSet<>();
		for ( int i = 0; i < 50; i++ ) {
			String s = UUID.randomUUID().toString();
			cache.put(s, s);
			if ( rng.nextBoolean() ) {
				keysToRemove.add(s);
			} else {
				keysToKeep.add(s);
			}
		}
		cache.removeAll(keysToRemove);

		// THEN
		then(data).as("Keys not specified for removal remain").containsOnlyKeys(keysToKeep);
	}

}
