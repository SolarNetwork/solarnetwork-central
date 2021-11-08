/* ==================================================================
 * UserEventServiceSqsConfig.java - 8/11/2021 4:46:58 PM
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

package net.solarnetwork.central.user.event.config;

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.dest.sqs.SqsDestination;
import net.solarnetwork.central.user.event.dest.sqs.SqsDestinationProperties;
import net.solarnetwork.central.user.event.dest.sqs.SqsStats;
import net.solarnetwork.central.user.event.dest.sqs.SqsUserNodeEventHookService;

/**
 * SQS user node event hook configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("user-event-sqs")
public class UserEventServiceSqsConfig {

	/** The cache name for SQS destinations. */
	public static final String SQS_DESTINATION_CACHE = "sqs-destinations";

	@Value("${app.user-event.sqs.stat-frequency:200}")
	private int statFrequency = 200;

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@ConfigurationProperties(prefix = "app.user-event.sqs.destination-cache")
	public CacheSettings sqsDestinationCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the datum cache.
	 * 
	 * @return the actor cache
	 */
	@Qualifier(SQS_DESTINATION_CACHE)
	@Bean
	public Cache<String, SqsDestination> sqsDestinationCache() {
		CacheSettings settings = sqsDestinationCacheSettings();
		return settings.createCache(cacheManager, String.class, SqsDestination.class,
				SQS_DESTINATION_CACHE);
	}

	@Bean
	public UserNodeEventHookService sqsUserNodeEventHookService() {
		SqsStats stats = new SqsStats("SqsNodeEventHook", statFrequency);
		SqsUserNodeEventHookService service = new SqsUserNodeEventHookService(stats);
		service.setDestinationCache(sqsDestinationCache());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SqsDestinationProperties.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
