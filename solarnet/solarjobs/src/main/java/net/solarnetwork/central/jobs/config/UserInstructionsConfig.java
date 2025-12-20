/* ==================================================================
 * UserInstructionsConfig.java - 28/11/2025 11:27:44 am
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

import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.scheduler.ThreadPoolTaskExecutorPingTest;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.PingTest;

/**
 * User instructions general configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserInstructionsConfig implements SolarNetUserConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@ConfigurationProperties(prefix = "app.user-instr.executor")
	@Qualifier(USER_INSTRUCTIONS)
	@Bean
	public ThreadPoolTaskExecutor userNodeInstructionExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-UserInstr-");
		return executor;
	}

	/**
	 * Expose a ping test for the task scheduler.
	 *
	 * @param taskExecutor
	 *        the executor
	 * @return the ping test
	 */
	@Bean
	public PingTest userInstructionsExecutorPingTest(
			@Qualifier(USER_INSTRUCTIONS) ThreadPoolTaskExecutor taskExecutor) {
		return new ThreadPoolTaskExecutorPingTest(taskExecutor);
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS)
	public PrefixedTextEncryptor userInstructionsTextEncryptor(
			@Value("${app.user-instr.encryptor.password}") String password,
			@Value("${app.user-instr.encryptor.salt-hex}") String salt) {
		return PrefixedTextEncryptor.aesTextEncryptor(password, salt);
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS_LOCKS)
	@ConfigurationProperties(prefix = "app.user-instr.cache.locks")
	public CacheSettings userInstructionsLockCacheSettings() {
		CacheSettings settings = new CacheSettings();
		settings.setLoaderWriter(new CacheLoaderWriter<UserLongCompositePK, Lock>() {

			@Override
			public Lock load(UserLongCompositePK key) throws Exception {
				// always return a new lock for any given (missing) key
				return new ReentrantLock();
			}

			@Override
			public void write(UserLongCompositePK key, Lock value) throws Exception {
				// ignore
			}

			@Override
			public void delete(UserLongCompositePK key) throws Exception {
				// ignore
			}
		});
		return settings;
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS_LOCKS)
	public Cache<UserLongCompositePK, Lock> userInstructionsLockCache(
			@Qualifier(USER_INSTRUCTIONS_LOCKS) CacheSettings settings) {
		return settings.createCache(cacheManager, UserLongCompositePK.class, Lock.class,
				USER_INSTRUCTIONS_LOCKS + "-cache");
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS_EXPRESSIONS)
	@ConfigurationProperties(prefix = "app.user-instr.cache.expression-cache")
	public CacheSettings userInstructionsExpressionCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS_EXPRESSIONS)
	public Cache<String, Expression> userInstructionsExpressionCache(
			@Qualifier(USER_INSTRUCTIONS_EXPRESSIONS) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, Expression.class,
				USER_INSTRUCTIONS_EXPRESSIONS + "-cache");
	}

	@Bean
	@Qualifier(USER_INSTRUCTIONS_HTTP)
	@ConfigurationProperties(prefix = "app.user-instr.cache.http-cache")
	public CacheSettings userInstructionsHttpCacheSettings() {
		return new CacheSettings();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	@Qualifier(USER_INSTRUCTIONS_HTTP)
	public Cache<CachableRequestEntity, Result<?>> userInstructionsHttpCache(
			@Qualifier(USER_INSTRUCTIONS_HTTP) CacheSettings settings) {
		return (Cache) settings.createCache(cacheManager, CachableRequestEntity.class, Result.class,
				USER_INSTRUCTIONS_HTTP + "-cache");
	}

}
