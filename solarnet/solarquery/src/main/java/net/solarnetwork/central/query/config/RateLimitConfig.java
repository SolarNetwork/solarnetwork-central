/* ==================================================================
 * RateLimitConfig.java - 19/04/2025 8:55:17 am
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

package net.solarnetwork.central.query.config;

import static net.solarnetwork.central.datum.config.JdbcQueryAuditorConfig.AUDIT;
import java.time.Duration;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;

/**
 * Rate limiting configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(RateLimitConfig.RATE_LIMIT)
@Configuration(proxyBeanMethods = false)
public class RateLimitConfig {

	public static final String RATE_LIMIT = "rate-limit";

	@Autowired
	private DataSource dataSource;

	/**
	 * A read-write non-primary data source, for use in apps where the primary
	 * data source is read-only.
	 */
	@Autowired(required = false)
	@Qualifier(AUDIT)
	private DataSource readWriteDataSource;

	@Value("${app.web.rate-limit.eviction-jitter:15s}")
	private Duration evictionJitter = Duration.ofSeconds(15L);

	@Value("${app.web.rate-limit.capacity:10}")
	private long capacity = 10;

	@Value("${app.web.rate-limit.tokens:10}")
	private long tokens = 10;

	@Value("${app.web.rate-limit.duration:1s}")
	private Duration duration = Duration.ofSeconds(1L);

	@Value("${app.web.rate-limit.table-name:solarcommon.bucket}")
	private String tableName = "solarcommon.bucket";

	@Bean
	@Qualifier(RATE_LIMIT)
	public ProxyManager<Long> rateLimitProxyManager() {
		ExpirationAfterWriteStrategy expiration = ExpirationAfterWriteStrategy
				.basedOnTimeForRefillingBucketUpToMax(evictionJitter);
		return Bucket4jPostgreSQL
				.advisoryLockBasedBuilder(readWriteDataSource != null ? readWriteDataSource : dataSource)
				.expirationAfterWrite(expiration).table(tableName).build();
	}

	@Bean
	@Qualifier(RATE_LIMIT)
	public Supplier<BucketConfiguration> rateLimitBucketConfigurationProvider() {
		return () -> BucketConfiguration.builder()
				.addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(tokens, duration).build())
				.build();
	}

}
