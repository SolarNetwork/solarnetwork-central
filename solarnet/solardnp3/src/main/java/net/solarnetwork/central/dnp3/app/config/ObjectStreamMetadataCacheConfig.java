/* ==================================================================
 * ObjectStreamMetadataCacheConfig.java - 10/08/2023 2:10:43 pm
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

package net.solarnetwork.central.dnp3.app.config;

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Object stream metadata cache configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class ObjectStreamMetadataCacheConfig {

	/**
	 * A qualifier to use for the datum metadata {@link Cache}.
	 */
	public static final String DATUM_METADATA_CACHE = "metadata-for-datum";

	@Autowired
	private CacheManager cacheManager;

	/**
	 * Get the datum metadata cache settings.
	 * 
	 * @return the settings
	 */
	@Bean
	@ConfigurationProperties(prefix = "app.datum.datum-metadata-cache")
	public CacheSettings metadataCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the datum metadata cache.
	 * 
	 * @return the metadata cache
	 */
	@Bean
	@Qualifier(DATUM_METADATA_CACHE)
	public Cache<DatumId, ObjectDatumStreamMetadata> metadataCache() {
		CacheSettings settings = metadataCacheSettings();
		return settings.createCache(cacheManager, DatumId.class, ObjectDatumStreamMetadata.class,
				DATUM_METADATA_CACHE);
	}

}
