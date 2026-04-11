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

package net.solarnetwork.central.dnp3.app.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.USER_EVENTS;
import static net.solarnetwork.central.dnp3.app.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.biz.LoggingUserEventAppenderBiz;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.DaoUserEventAppenderBiz;
import net.solarnetwork.central.common.biz.impl.IdentityJsonEntityCodec;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.common.dao.ObservableGenericWriteOnlyDao;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.common.dao.jdbc.JdbcUserEventDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.central.support.SqsOverflowQueueSettings;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.central.support.UserEventBasicSerializer;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.UuidGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Configuration for user event handling.
 *
 * <p>
 * The {@code logging-user-event-appender} profile can be enabled to disable the
 * default DAO appender in favor of one that simply logs the events to the
 * application log. This can be useful in unit tests, for example. The
 * {@code sqs-user-event-appender} profile can be enabled to use an asyncronous
 * SQS-overflow queue.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
@Configuration(proxyBeanMethods = false)
public class UserEventConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Bean
	public JdbcUserEventDao userEventAppenderDao() {
		return new JdbcUserEventDao(jdbcOperations);
	}

	@Profile("mqtt")
	@Bean
	@ConfigurationProperties(prefix = "app.solarflux.user-events")
	@Qualifier(SOLARFLUX)
	public MqttJsonPublisher<UserEvent> userEventSolarFluxPublisher(
			@Qualifier(SOLARFLUX) ObjectMapper solarFluxObjectMapper) {
		return new MqttJsonPublisher<>("UserEvent", solarFluxObjectMapper,
				UserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN, false, MqttQos.AtMostOnce);
	}

	@Profile("!logging-user-event-appender")
	@Configuration(proxyBeanMethods = false)
	public static class NonLoggingUserEventAppenderConfig {

		/**
		 * Direct to DAO {@link UserEventAppenderBiz} (designed for development
		 * environments).
		 */
		@Profile("!sqs-user-event-appender")
		@Configuration(proxyBeanMethods = false)
		public static class DaoUserEventAppenderConfig {

			@Value("${app.user-events.dao.mqtt-publish-timeout:100ms}")
			private Duration mqttTimeout = ObservableGenericWriteOnlyDao.DEFAULT_OBSERVER_TIMEOUT;

			@ConfigurationProperties(prefix = "app.user-events.dao")
			@Bean
			public DaoUserEventAppenderBiz userEventAppenderBizDao(
					UserEventAppenderDao userEventAppenderDao, UuidGenerator uuidGenerator, @Autowired(
							required = false) @Qualifier(SOLARFLUX) MqttJsonPublisher<UserEvent> userEventSolarFluxPublisher) {
				final GenericWriteOnlyDao<UserEvent, UserUuidPK> dao;
				if ( userEventSolarFluxPublisher != null ) {
					var obsDao = new ObservableGenericWriteOnlyDao<>(userEventAppenderDao,
							userEventSolarFluxPublisher);
					obsDao.setObserverTimeout(mqttTimeout);
					dao = obsDao;
				} else {
					dao = userEventAppenderDao;
				}
				return new DaoUserEventAppenderBiz(dao, uuidGenerator);
			}

		}

		/**
		 * SQS overflow {@link UserEventAppenderBiz} (designed for production
		 * environments).
		 */
		@Profile("sqs-user-event-appender")
		@Configuration(proxyBeanMethods = false)
		public static class SqsUserEventAppenderConfig {

			public static final JacksonModule EVENT_MODULE;
			static {
				SimpleModule m = new SimpleModule("SolarFlux");
				m.addSerializer(UserEvent.class, UserEventBasicSerializer.INSTANCE);
				m.addDeserializer(UserEvent.class, UserEventBasicDeserializer.INSTANCE);
				EVENT_MODULE = m;
			}

			private static final IdentityJsonEntityCodec<UserEvent, UserUuidPK> ENTITY_CODEC = new IdentityJsonEntityCodec<>(
					JsonUtils.JSON_OBJECT_MAPPER.rebuild().addModule(EVENT_MODULE).build(),
					UserEvent.class);

			@Value("${app.user-events.dao.mqtt-publish-timeout:100ms}")
			private Duration mqttTimeout = ObservableGenericWriteOnlyDao.DEFAULT_OBSERVER_TIMEOUT;

			@ConfigurationProperties(prefix = "app.user-events.sqs")
			@Qualifier(USER_EVENTS)
			@Bean
			public SqsOverflowQueueSettings userEventsSqsOverflowQueueSettings() {
				return new SqsOverflowQueueSettings();
			}

			@Qualifier(USER_EVENTS)
			@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
			public SqsOverflowQueue<UserEvent, UserUuidPK> userEventsSqsOverflowQueue(
					@Qualifier(USER_EVENTS) SqsOverflowQueueSettings settings,
					UserEventAppenderDao userEventAppenderDao) {
				StatTracker stats = new StatTracker("SqsUserEventsCollector", null,
						LoggerFactory.getLogger(SqsOverflowQueue.class), settings.getStatFrequency());

				var collector = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "UserEventQueue-SQS",
						settings.newAsyncClient(), settings.getUrl(),
						new ArrayBlockingQueue<>(settings.getWorkQueueSize()),
						new LinkedHashSetBlockingQueue<>(9), userEventAppenderDao, ENTITY_CODEC);
				collector.setPingTestName("SQS UserEvent Collector");
				collector.setReadConcurrency(settings.getReadConcurrency());
				collector.setWriteConcurrency(settings.getWriteConcurrency());
				collector.setIgnoredDaoExceptions(Set.of(DuplicateKeyException.class));
				if ( settings.getWorkItemMaxWait() != null ) {
					collector.setWorkItemMaxWaitMs(settings.getWorkItemMaxWait().toMillis());
				}
				collector.setReadMaxMessageCount(settings.getReadMaxMessageCount());
				if ( settings.getReadMaxWaitTime() != null ) {
					collector.setReadMaxWaitTimeSecs((int) settings.getReadMaxWaitTime().toSeconds());
				}
				if ( settings.getReadSleepMin() != null ) {
					collector.setReadSleepMinMs(settings.getReadSleepMin().toMillis());
				}
				if ( settings.getReadSleepMax() != null ) {
					collector.setReadSleepMaxMs(settings.getReadSleepMax().toMillis());
				}
				if ( settings.getReadSleepThrottleStep() != null ) {
					collector.setReadSleepThrottleStepMs(settings.getReadSleepThrottleStep().toMillis());
				}
				if ( settings.getShutdownWait() != null ) {
					collector.setShutdownWaitSecs((int) settings.getShutdownWait().toSeconds());
				}
				return collector;
			}

			@Bean
			public DaoUserEventAppenderBiz userEventAppenderBizSqs(
			// @formatter:off
					@Qualifier(USER_EVENTS)
					SqsOverflowQueue<UserEvent, UserUuidPK> queue,

					UuidGenerator uuidGenerator,

					@Autowired(required = false)
					@Qualifier(SOLARFLUX)
					MqttJsonPublisher<UserEvent> userEventSolarFluxPublisher
					// @formatter:on
			) {
				final GenericWriteOnlyDao<UserEvent, UserUuidPK> dao;
				if ( userEventSolarFluxPublisher != null ) {
					var obsDao = new ObservableGenericWriteOnlyDao<>(queue, userEventSolarFluxPublisher);
					obsDao.setObserverTimeout(mqttTimeout);
					dao = obsDao;
				} else {
					dao = queue;
				}
				return new DaoUserEventAppenderBiz(dao, uuidGenerator);
			}

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
