/* ==================================================================
 * RateLimitMaintenanceConfig.java - 19/04/2025 8:55:17 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.jobs.config;

import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.web.support.RateLimitExpiredCleanupJob;

/**
 * Rate limiting maintenance configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(RateLimitJobsConfig.RATE_LIMIT)
@Configuration(proxyBeanMethods = false)
public class RateLimitJobsConfig {

	public static final String RATE_LIMIT = "rate-limit";

	@Autowired
	private DataSource dataSource;

	@Value("${app.web.rate-limit.eviction-jitter:15s}")
	private Duration evictionJitter = Duration.ofSeconds(15L);

	@Value("${app.web.rate-limit.table-name:solarcommon.bucket}")
	private String tableName = "solarcommon.bucket";

	@Bean
	@Qualifier(RATE_LIMIT)
	public PostgreSQLadvisoryLockBasedProxyManager<Long> rateLimitProxyManager() {
		ExpirationAfterWriteStrategy expiration = ExpirationAfterWriteStrategy
				.basedOnTimeForRefillingBucketUpToMax(evictionJitter);
		return Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource).expirationAfterWrite(expiration)
				.table(tableName).build();
	}

	@ConfigurationProperties(prefix = "app.web.rate-limit.cleaner")
	@Bean
	public ManagedJob rateLimitExpiredCleaner(@Qualifier(RATE_LIMIT) ExpiredEntriesCleaner cleaner,
			@Value("${app.web.rate-limit.cleaner.max-remove-per-transaction:1000}") int maxRemovePerTransaction,
			@Value("${app.web.rate-limit.cleaner.continue-removing-threshold:50}") int continueRemovingThreshold) {
		return new RateLimitExpiredCleanupJob(cleaner, maxRemovePerTransaction,
				continueRemovingThreshold);
	}
}
