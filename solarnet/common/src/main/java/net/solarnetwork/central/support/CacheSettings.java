/* ==================================================================
 * CacheSettings.java - 9/10/2021 2:28:43 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.time.Duration;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;

/**
 * A standardized cache settings bean.
 * 
 * @author matt
 * @version 1.0
 */
public class CacheSettings {

	/** The {@code tti} property default value. */
	public static final long DEFAULT_TIME_TO_IDLE = 0L;

	/** The {@code ttl} property default value. */
	public static final long DEFAULT_TIME_TO_LIVE = 60L;

	/** The {@code heapMaxEntries} property default value. */
	public static final long DEFAULT_HEAP_MAX_ENTRIES = 10000L;

	/** The {@code diskMaxSizeMb} property default value. */
	public static final long DEFAULT_DISK_MAX_SIZE_MB = 100L;

	/** The {@code diskPersistent} property default value. */
	public static final boolean DEFAULT_DISK_PERSISTENT = false;

	private long tti = DEFAULT_TIME_TO_IDLE;
	private long ttl = DEFAULT_TIME_TO_LIVE;
	private long heapMaxEntries = DEFAULT_HEAP_MAX_ENTRIES;
	private long diskMaxSizeMb = DEFAULT_DISK_MAX_SIZE_MB;
	private boolean diskPersistent = DEFAULT_DISK_PERSISTENT;

	/**
	 * Create a cache.
	 * 
	 * @param <K>
	 *        the key type
	 * @param <V>
	 *        the value type
	 * @param cacheManager
	 *        the cache manager
	 * @param keyType
	 *        the key class
	 * @param valueType
	 *        the value class
	 * @param settings
	 *        the settings
	 * @param name
	 *        the name
	 * @return the new cache instance
	 */
	public <K, V> Cache<K, V> createCache(CacheManager cacheManager, Class<K> keyType,
			Class<V> valueType, String name) {
		CacheConfigurationBuilder<K, V> cacheConfigBuilder = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(keyType, valueType,
						ResourcePoolsBuilder.heap(Integer.MAX_VALUE));
		ResourcePoolsBuilder poolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
		if ( heapMaxEntries > 0 ) {
			poolsBuilder = poolsBuilder.heap(heapMaxEntries, EntryUnit.ENTRIES);
		}
		if ( diskMaxSizeMb > 0 ) {
			poolsBuilder = poolsBuilder.disk(diskMaxSizeMb, MemoryUnit.MB, diskPersistent);
		}
		cacheConfigBuilder = cacheConfigBuilder.withResourcePools(poolsBuilder);
		if ( tti > 0 ) {
			cacheConfigBuilder
					.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(tti)));
		}
		if ( ttl > 0 ) {
			cacheConfigBuilder
					.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ttl)));
		}
		return cacheManager.createCache(name,
				Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfigBuilder));
	}

	/**
	 * Get the time to idle, in seconds.
	 * 
	 * @return the time to idle, or {@literal 0} for no idle timeout; defaults
	 *         to {@link #DEFAULT_TIME_TO_IDLE}
	 */
	public long getTti() {
		return tti;
	}

	/**
	 * Set the time to idle, in seconds.
	 * 
	 * @param tti
	 *        the time to idle, or {@literal 0} for no idle timeout
	 */
	public void setTti(long tti) {
		this.tti = tti;
	}

	/**
	 * Get the time to live, in seconds.
	 * 
	 * @return the time to live seconds, or {@literal 0} for no life timeout;
	 *         defaults to {@link #DEFAULT_TIME_TO_LIVE}
	 */
	public long getTtl() {
		return ttl;
	}

	/**
	 * Set the time to live, in seconds.
	 * 
	 * @param ttl
	 *        the time to live, or {@literal 0} for no life timeout
	 */
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	/**
	 * Get a on-heap (memory) max number of cached elements.
	 * 
	 * @return the max heap element count, {@literal 0} for no limit; defaults
	 *         to {@link #DEFAULT_HEAP_MAX_ENTRIES}
	 */
	public long getHeapMaxEntries() {
		return heapMaxEntries;
	}

	/**
	 * Set the on-heap (memory) max number of cached elements.
	 * 
	 * @param heapMaxEntries
	 *        the max heap element count
	 */
	public void setHeapMaxEntries(long heapMaxEntries) {
		this.heapMaxEntries = heapMaxEntries;
	}

	/**
	 * Get the on-disk maximum size, in MB.
	 * 
	 * @return the on-disk maximum size; defaults to
	 *         {@link #DEFAULT_DISK_MAX_SIZE_MB}
	 */
	public long getDiskMaxSizeMb() {
		return diskMaxSizeMb;
	}

	/**
	 * Get the on-disk maximum size, in MB.
	 * 
	 * @param diskMaxSizeMb
	 *        the on-disk maximum size, in MB
	 */
	public void setDiskMaxSizeMb(long diskMaxSizeMb) {
		this.diskMaxSizeMb = diskMaxSizeMb;
	}

	/**
	 * Get the disk persistent setting.
	 * 
	 * @return {@literal true} to persist the cache between reboots; defaults to
	 *         {@link #DEFAULT_DISK_PERSISTENT}
	 */
	public boolean isDiskPersistent() {
		return diskPersistent;
	}

	/**
	 * Set the disk persistent setting.
	 * 
	 * @param diskPersistent
	 *        {@literal true} to persist the cache between reboots
	 */
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}

}
