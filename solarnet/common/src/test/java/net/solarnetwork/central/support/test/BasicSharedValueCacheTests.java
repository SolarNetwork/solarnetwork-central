/* ==================================================================
 * BasicSharedValueCacheTests.java - 23/02/2024 7:29:48 am
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

package net.solarnetwork.central.support.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.util.CachedResult;

/**
 * Test cases for the {@link BasicSharedValueCache}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicSharedValueCacheTests {

	private ConcurrentMap<Integer, CachedResult<UUID>> primaryCache;
	private ConcurrentMap<String, UUID> sharedCache;
	private BasicSharedValueCache<Integer, UUID, String> cache;

	@BeforeEach
	public void setup() {
		primaryCache = new ConcurrentHashMap<>();
		sharedCache = new ConcurrentHashMap<>();
		cache = new BasicSharedValueCache<>(primaryCache, sharedCache);
	}

	@Test
	public void get_empty() {
		then(cache.get(1)).as("Empty cache returns null").isNull();
	}

	@Test
	public void put() {
		// GIVEN
		UUID val = UUID.randomUUID();

		// WHEN
		UUID result = cache.put(1, "a", (_) -> val, 60L);
		UUID got = cache.get(1);

		// @formatter:off
		then(result)
			.as("Cached instance returned")
			.isSameAs(val)
			;
		
		then(primaryCache)
			.as("Primary cache has key")
			.containsOnlyKeys(1)
			.as("Primary cache has entry")
			.hasEntrySatisfying(1, entry -> {
				then(entry)
					.extracting(CachedResult::getResult)
					.as("Cache entry has shared value")
					.isSameAs(val)
					;
			})
			;
		
		then(sharedCache)
			.as("Secondary cache has key")
			.containsOnlyKeys("a")
			.hasEntrySatisfying("a", sharedVal -> {
				then(sharedVal)
					.as("Shared value cached")
					.isSameAs(val)
					;
			})
			;
		
		then(got)
			.as("Cached shared value returned")
			.isSameAs(val)
			;
		// @formatter:on
	}

	@Test
	public void put_multiShared() {
		// GIVEN
		UUID val = UUID.randomUUID();

		Function<String, UUID> provider = (_) -> val;

		// WHEN
		UUID result1 = cache.put(1, "a", provider, 60L);
		UUID result2 = cache.put(2, "a", provider, 120L);
		UUID got1 = cache.get(1);
		UUID got2 = cache.get(2);

		// @formatter:off
		then(result1)
			.as("Shared instance returned")
			.isSameAs(val)
			;
	
		then(result2)
			.as("Shared instance returned")
			.isSameAs(val)
			;

		then(primaryCache)
			.as("Primary cache has two keys")
			.containsOnlyKeys(1, 2)
			.as("Primary cache has entry 1")
			.hasEntrySatisfying(1, entry -> {
				then(entry)
					.as("TTL for key 1")
					.returns(entry.getCreated() + 60_000L, CachedResult::getExpires)
					.extracting(CachedResult::getResult)
					.as("Cache entry 1 has shared value")
					.isSameAs(val)
					;
			})
			.hasEntrySatisfying(2, entry -> {
				then(entry)
					.as("TTL for key 2")
					.returns(entry.getCreated() + 120_000L, CachedResult::getExpires)
					.extracting(CachedResult::getResult)
					.as("Cache entry 2 has shared value")
					.isSameAs(val)
					;
			})
			;

		then(sharedCache)
			.as("Secondary cache has one key")
			.containsOnlyKeys("a")
			.hasEntrySatisfying("a", sharedVal -> {
				then(sharedVal)
					.as("Shared value cached")
					.isSameAs(val)
					;
			})
			;

		then(got1)
			.as("Cached shared value returned for key 1")
			.isSameAs(val)
			;

		then(got2)
			.as("Cached shared value returned for key 2")
			.isSameAs(val)
			;
		// @formatter:on
	}

	/**
	 * Replace the primary cache entry for a key with an expired entry holding
	 * the same value, so expiration can be tested without waiting.
	 */
	private void expirePrimaryEntry(Integer key) {
		CachedResult<UUID> entry = primaryCache.get(key);
		primaryCache.put(key, new CachedResult<>(entry.getResult(),
				System.currentTimeMillis() - 120_000L, 60L, TimeUnit.SECONDS));
	}

	@Test
	public void get_expired() {
		// GIVEN
		UUID val = UUID.randomUUID();
		cache.put(1, "a", (_) -> val, 60L);

		// WHEN
		expirePrimaryEntry(1);
		UUID got = cache.get(1);

		// THEN
		// @formatter:off
		then(got)
			.as("Expired entry returns null")
			.isNull()
			;

		then(primaryCache)
			.as("Expired entry is not removed by get(); only prune() evicts")
			.containsOnlyKeys(1)
			;

		then(sharedCache)
			.as("Shared value is not removed by get(); only prune() evicts")
			.containsOnlyKeys("a")
			;
		// @formatter:on
	}

	@Test
	public void put_sameKey_newShareKey() {
		// GIVEN
		UUID val1 = UUID.randomUUID();
		UUID val2 = UUID.randomUUID();

		// WHEN
		cache.put(1, "a", (_) -> val1, 60L);
		UUID result = cache.put(1, "b", (_) -> val2, 60L);

		// THEN
		// @formatter:off
		then(result)
			.as("New shared value returned")
			.isSameAs(val2)
			;

		then(cache.get(1))
			.as("Primary key now resolves to the new shared value")
			.isSameAs(val2)
			;

		then(sharedCache)
			.as("Replaced shared value is retained until prune()")
			.containsOnlyKeys("a", "b")
			;
		// @formatter:on
	}

	@Test
	public void put_providerThrows() {
		// GIVEN
		RuntimeException failure = new IllegalStateException("boom");

		// WHEN
		// @formatter:off
		thenThrownBy(() -> cache.put(1, "a", (_) -> {
					throw failure;
				}, 60L))
			.as("Provider exception is propagated")
			.isSameAs(failure)
			;

		// THEN
		then(primaryCache)
			.as("No primary entry cached when provider fails")
			.isEmpty()
			;

		then(sharedCache)
			.as("No shared value cached when provider fails")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void put_zeroTtl() {
		// GIVEN
		UUID val = UUID.randomUUID();

		// WHEN
		UUID result = cache.put(1, "a", (_) -> val, 0L);

		// THEN
		// @formatter:off
		then(result)
			.as("Value returned even with a zero TTL")
			.isSameAs(val)
			;

		then(cache.get(1))
			.as("Zero TTL entry is immediately expired")
			.isNull()
			;

		then(sharedCache)
			.as("Shared value cached despite the zero TTL, until prune()")
			.containsOnlyKeys("a")
			;
		// @formatter:on
	}

	@Test
	public void put_providerReturnsNull() {
		// WHEN
		// @formatter:off
		thenThrownBy(() -> cache.put(1, "a", (_) -> null, 60L))
			.as("Null provider result rejected, to uphold the non-null put() contract")
			.isInstanceOf(IllegalStateException.class)
			;

		// THEN
		then(primaryCache)
			.as("No primary entry cached when provider returns null")
			.isEmpty()
			;

		then(sharedCache)
			.as("No shared value cached when provider returns null")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void put_ttlTooLarge() {
		// GIVEN
		UUID val = UUID.randomUUID();

		// WHEN
		UUID result = cache.put(1, "a", (_) -> val, Long.MAX_VALUE);

		// THEN
		// @formatter:off
		then(result)
			.as("Value returned")
			.isSameAs(val)
			;

		then(cache.get(1))
			.as("Oversized TTL clamped, so the entry is valid rather than overflowed")
			.isSameAs(val)
			;

		then(primaryCache)
			.as("Primary cache has entry")
			.hasEntrySatisfying(1, entry -> {
				then(entry)
					.as("Expiration clamped to MAX_TTL_SECONDS")
					.returns(entry.getCreated()
							+ TimeUnit.SECONDS.toMillis(BasicSharedValueCache.MAX_TTL_SECONDS),
							CachedResult::getExpires)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void prune_empty() {
		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("Primary cache still empty")
			.isEmpty()
			;

		then(sharedCache)
			.as("Shared cache still empty")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void prune_nothingExpired() {
		// GIVEN
		UUID val = UUID.randomUUID();
		cache.put(1, "a", (_) -> val, 60L);
		cache.put(2, "a", (_) -> val, 120L);

		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("Valid primary entries retained")
			.containsOnlyKeys(1, 2)
			;

		then(sharedCache)
			.as("Shared value in use retained")
			.containsEntry("a", val)
			;
		// @formatter:on
	}

	@Test
	public void prune_expired() {
		// GIVEN
		UUID val = UUID.randomUUID();
		cache.put(1, "a", (_) -> val, 60L);
		expirePrimaryEntry(1);

		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("Expired primary entry removed")
			.isEmpty()
			;

		then(sharedCache)
			.as("Shared value no longer in use removed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void prune_expired_sharedValueStillInUse() {
		// GIVEN
		UUID val = UUID.randomUUID();
		Function<String, UUID> provider = (_) -> val;
		cache.put(1, "a", provider, 60L);
		cache.put(2, "a", provider, 120L);
		expirePrimaryEntry(1);

		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("Expired primary entry removed, valid entry retained")
			.containsOnlyKeys(2)
			;

		then(sharedCache)
			.as("Shared value still referenced by key 2 retained")
			.containsEntry("a", val)
			;

		then(cache.get(2))
			.as("Retained key still resolves to the shared value")
			.isSameAs(val)
			;
		// @formatter:on
	}

	@Test
	public void prune_replacedShareKey() {
		// GIVEN
		UUID val1 = UUID.randomUUID();
		UUID val2 = UUID.randomUUID();
		cache.put(1, "a", (_) -> val1, 60L);
		cache.put(1, "b", (_) -> val2, 60L);

		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(primaryCache)
			.as("Valid primary entry retained")
			.containsOnlyKeys(1)
			;

		then(sharedCache)
			.as("Only the shared value still referenced is retained")
			.containsOnlyKeys("b")
			.containsEntry("b", val2)
			;
		// @formatter:on
	}

	@Test
	public void prune_unreferencedSharedValue() {
		// GIVEN
		UUID orphan = UUID.randomUUID();
		sharedCache.put("orphan", orphan);
		UUID val = UUID.randomUUID();
		cache.put(1, "a", (_) -> val, 60L);

		// WHEN
		cache.prune();

		// THEN
		// @formatter:off
		then(sharedCache)
			.as("Shared value never referenced by a primary entry removed")
			.containsOnlyKeys("a")
			;
		// @formatter:on
	}

}
