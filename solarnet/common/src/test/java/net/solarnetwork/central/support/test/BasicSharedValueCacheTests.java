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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.util.CachedResult;

/**
 * Test cases for the {@link BasicSharedValueCache}.
 * 
 * @author matt
 * @version 1.0
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
		UUID result = cache.put(1, "a", (s) -> val, 60L);
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

		Function<String, UUID> provider = (s) -> val;

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

}
