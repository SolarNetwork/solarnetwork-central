/* ==================================================================
 * JCacheFactoryBean.java - 31/08/2017 9:22:34 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Factory bean for {@link Cache} instances.
 * 
 * @author matt
 * @version 1.0
 */
public class JCacheFactoryBean<K, V> implements FactoryBean<Cache<K, V>>, InitializingBean {

	/** Cache expiry policy type. */
	public static enum ExpiryPolicy {
		Accessed,
		Created,
		Updated,
		Touched,
		Eternal,
	}

	private final CacheManager cacheManager;
	private final Class<K> keyType;
	private final Class<V> valueType;

	private String name = "default";
	private boolean storeByValue = false;
	private boolean statisticsEnabled = false;
	private ExpiryPolicy expiryPolicy = null;
	private Duration expiryDuration = null;

	private Factory<? extends CacheLoader<K, V>> readThroughLoaderFactory;
	private boolean readThrough = false;
	private Factory<? extends CacheWriter<K, V>> writeThroughWriterFactory;
	private boolean writeThrough = false;

	private Cache<K, V> cache;

	/**
	 * Constructor.
	 */
	public JCacheFactoryBean(CacheManager cacheManager, Class<K> keyType, Class<V> valueType) {
		super();
		this.cacheManager = cacheManager;
		this.keyType = keyType;
		this.valueType = valueType;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		MutableConfiguration<K, V> configuration = new MutableConfiguration<K, V>()
				.setTypes(keyType, valueType).setStoreByValue(storeByValue)
				.setStatisticsEnabled(statisticsEnabled);
		if ( expiryPolicy != null ) {
			switch (expiryPolicy) {
				case Accessed:
					configuration.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(expiryDuration));
					break;

				case Created:
					configuration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(expiryDuration));
					break;

				case Updated:
					configuration.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(expiryDuration));
					break;

				case Touched:
					configuration.setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(expiryDuration));

				case Eternal:
					configuration.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
					break;
			}
		}
		if ( readThroughLoaderFactory != null && readThrough ) {
			configuration.setCacheLoaderFactory(readThroughLoaderFactory).setReadThrough(true);
		}
		if ( writeThroughWriterFactory != null && writeThrough ) {
			configuration.setCacheWriterFactory(writeThroughWriterFactory).setWriteThrough(true);
		}
		cache = cacheManager.createCache(name, configuration);
	}

	@Override
	public Cache<K, V> getObject() throws Exception {
		if ( cache == null ) {
			afterPropertiesSet();
		}
		return cache;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.cache != null ? this.cache.getClass() : Cache.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Set the cache name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the store-by-value flag.
	 * 
	 * @param storeByValue
	 *        the store-by-value to set
	 */
	public void setStoreByValue(boolean storeByValue) {
		this.storeByValue = storeByValue;
	}

	/**
	 * Set the expiry policy.
	 * 
	 * @param expiryPolicy
	 *        the expiry policy to set
	 */
	public void setExpiryPolicy(ExpiryPolicy expiryPolicy) {
		this.expiryPolicy = expiryPolicy;
	}

	/**
	 * Set the expiry duration.
	 * 
	 * @param expiryDuration
	 *        the expiry duration to set
	 */
	public void setExpiryDuration(Duration expiryDuration) {
		this.expiryDuration = expiryDuration;
	}

	/**
	 * Set the read-through flag.
	 * 
	 * @param readThrough
	 *        the read-through to set
	 */
	public void setReadThrough(boolean readThrough) {
		this.readThrough = readThrough;
	}

	/**
	 * Set the write-through flag.
	 * 
	 * @param writeThrough
	 *        the write-through to set
	 */
	public void setWriteThrough(boolean writeThrough) {
		this.writeThrough = writeThrough;
	}

	/**
	 * Set the statistics-enabled flag.
	 * 
	 * @param statisticsEnabled
	 *        the statistics-enabled to set
	 */
	public void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	/**
	 * Set the read-through loader.
	 * 
	 * @param readThroughLoaderFactory
	 *        the loader to set
	 */
	public void setReadThroughLoaderFactory(Factory<? extends CacheLoader<K, V>> readThroughLoader) {
		this.readThroughLoaderFactory = readThroughLoader;
	}

	/**
	 * Set the write-through writer.
	 * 
	 * @param writeThroughWriterFactory
	 *        the writer to set
	 */
	public void setWriteThroughWriterFactory(Factory<? extends CacheWriter<K, V>> writeThroughWriter) {
		this.writeThroughWriterFactory = writeThroughWriter;
	}

}
