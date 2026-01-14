/* ==================================================================
 * UserEventConfig.java - 1/08/2022 4:42:46 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.config;

import static net.solarnetwork.central.oscp.fp.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import net.solarnetwork.central.biz.LoggingUserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.DaoUserEventBiz;
import net.solarnetwork.central.common.config.AsyncUserEventAppenderSettings;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.common.dao.UserEventDao;
import net.solarnetwork.central.common.dao.jdbc.JdbcUserEventDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.UuidGenerator;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration for user event handling.
 *
 * <p>
 * The {@code logging-user-event-appender} profile can be enabled to disable the default async DAO
 * appender in favor of one that simply logs the events to the application log. This can be useful in
 * unit tests, for example.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
@Configuration(proxyBeanMethods = false)
public class UserEventConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Bean
	public JdbcUserEventDao userEventAppenderDao() {
		JdbcUserEventDao dao = new JdbcUserEventDao(jdbcOperations);
		return dao;
	}

	@Bean
	public DaoUserEventBiz userEventBiz(UserEventDao userEventDao) {
		return new DaoUserEventBiz(userEventDao);
	}

	/**
	 * Configuration for publishing user events to SolarFlux.
	 */
	@Configuration(proxyBeanMethods = false)
	@Profile("mqtt")
	public static class UserEventSolarFluxConfig {

		@Autowired
		@Qualifier(SOLARFLUX)
		private ObjectMapper solarFluxObjectMapper;

		@Bean
		@ConfigurationProperties(prefix = "app.solarflux.user-events")
		@Qualifier(SOLARFLUX)
		public MqttJsonPublisher<UserEvent> userEventSolarFluxPublisher() {
			return new MqttJsonPublisher<>("UserEvent", solarFluxObjectMapper,
					AsyncDaoUserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN, false, MqttQos.AtMostOnce);
		}

	}

	/**
	 * Configuration for persisting user events.
	 */
	@Profile("!logging-user-event-appender")
	@Configuration(proxyBeanMethods = false)
	public static class AsyncUserEventAppenderConfig {

		@Bean
		@ConfigurationProperties(prefix = "app.user.events.async-appender")
		public AsyncUserEventAppenderSettings asyncUserEventAppenderSettings() {
			return new AsyncUserEventAppenderSettings();
		}

		@Bean(destroyMethod = "serviceDidShutdown")
		public AsyncDaoUserEventAppenderBiz userEventAppenderBiz(AsyncUserEventAppenderSettings settings,
				UserEventAppenderDao dao, UuidGenerator uuidGenerator,
				@Qualifier(SOLARFLUX) @Autowired(required = false) MqttJsonPublisher<UserEvent> solarFluxPublisher) {
			ThreadPoolExecutor executor = new ThreadPoolExecutor(settings.getThreads(),
					settings.getThreads(), 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
					new CustomizableThreadFactory("UserEventAppender-"));
			executor.allowCoreThreadTimeOut(true);
			AsyncDaoUserEventAppenderBiz biz = new AsyncDaoUserEventAppenderBiz(executor, dao,
					new PriorityBlockingQueue<>(64, AsyncDaoUserEventAppenderBiz.EVENT_SORT),
					new StatTracker("AsyncDaoUserEventAppender", null,
							LoggerFactory.getLogger(AsyncDaoUserEventAppenderBiz.class),
							settings.getStatFrequency()), uuidGenerator);
			biz.setQueueLagAlertThreshold(settings.getQueueLagAlertThreshold());
			biz.setSolarFluxPublisher(solarFluxPublisher);
			return biz;
		}

	}

	@Profile("logging-user-event-appender")
	@Configuration(proxyBeanMethods = false)
	public static class LoggingUserEventAppenderConfig {

		@Bean
		public LoggingUserEventAppenderBiz userEventAppenderBiz() {
			return new LoggingUserEventAppenderBiz();
		}
	}

}
