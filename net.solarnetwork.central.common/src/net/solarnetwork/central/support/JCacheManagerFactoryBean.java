/* ==================================================================
 * JCacheManagerFactoryBean.java - 31/08/2017 7:36:03 AM
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

import java.net.URI;
import java.util.Properties;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Re-implementation of Spring's {@code JCacheManagerFactoryBean} to better work
 * in OSGi.
 * 
 * @author matt
 * @version 1.0
 * @since 1.34
 */
public class JCacheManagerFactoryBean
		implements FactoryBean<CacheManager>, BeanClassLoaderAware, InitializingBean, DisposableBean {

	private URI cacheManagerUri;

	private Properties cacheManagerProperties;

	private ClassLoader beanClassLoader;

	private CacheManager cacheManager;

	private String cachingProviderClassName = "org.ehcache.jsr107.EhcacheCachingProvider";

	private ClassLoader cachingProviderClassLoader = getClass().getClassLoader();

	/**
	 * Specify the URI for the desired CacheManager. Default is {@code null}
	 * (i.e. JCache's default).
	 * 
	 * @param cacheManagerUri
	 *        the URI
	 */
	public void setCacheManagerUri(URI cacheManagerUri) {
		this.cacheManagerUri = cacheManagerUri;
	}

	/**
	 * Specify properties for the to-be-created CacheManager. Default is
	 * {@code null} (i.e. no special properties to apply).
	 * 
	 * @param cacheManagerProperties
	 *        the properties
	 * @see javax.cache.spi.CachingProvider#getCacheManager(URI, ClassLoader,
	 *      Properties)
	 */
	public void setCacheManagerProperties(Properties cacheManagerProperties) {
		this.cacheManagerProperties = cacheManagerProperties;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.cacheManager = Caching
				.getCachingProvider(this.cachingProviderClassName, this.cachingProviderClassLoader)
				.getCacheManager(this.cacheManagerUri, this.beanClassLoader,
						this.cacheManagerProperties);
	}

	@Override
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		this.cacheManager.close();
	}

	/**
	 * @param cachingProviderClassName
	 *        the cachingProviderClassName to set
	 */
	public void setCachingProviderClassName(String cachingProviderClassName) {
		this.cachingProviderClassName = cachingProviderClassName;
	}

	/**
	 * @param cachingProviderClassLoader
	 *        the cachingProviderClassLoader to set
	 */
	public void setCachingProviderClassLoader(ClassLoader cachingProviderClassLoader) {
		this.cachingProviderClassLoader = cachingProviderClassLoader;
	}

}
