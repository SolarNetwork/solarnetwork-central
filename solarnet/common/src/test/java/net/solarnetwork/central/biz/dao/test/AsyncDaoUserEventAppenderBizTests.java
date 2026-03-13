/* ==================================================================
 * AsyncDaoUserEventAppenderBizTests.java - 2/08/2022 3:22:51 pm
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

package net.solarnetwork.central.biz.dao.test;

import static java.lang.String.format;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SucceededFuture;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz.UserEventStats;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.MqttJsonPublisher;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import net.solarnetwork.util.UuidGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test cases for the {@link AsyncDaoUserEventAppenderBiz}.
 * 
 * @author matt
 * @version 1.4
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AsyncDaoUserEventAppenderBizTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private UserEventAppenderDao dao;

	@Mock
	private MqttConnection mqttConnection;

	@Captor
	private ArgumentCaptor<UserEvent> eventCaptor;

	@Captor
	private ArgumentCaptor<MqttMessage> messageCaptor;

	private UuidGenerator uuidGenerator;
	private ExecutorService executor;
	private StatTracker stats;
	private AsyncDaoUserEventAppenderBiz biz;
	private ObjectMapper objectMapper;
	private MqttJsonPublisher<UserEvent> solarFluxPublisher;

	@BeforeEach
	public void setup() {
		uuidGenerator = TimeBasedV7UuidGenerator.INSTANCE_MICROS;
		executor = Executors.newFixedThreadPool(3);
		stats = new StatTracker("AsyncDaoUserEventAppender",
				"net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz",
				LoggerFactory.getLogger(AsyncDaoUserEventAppenderBiz.class), 10);
		biz = new AsyncDaoUserEventAppenderBiz(executor, dao, stats, uuidGenerator);

		SimpleModule m = new SimpleModule();
		m.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		objectMapper = JsonMapper.builder().addModule(m).build();

		solarFluxPublisher = new MqttJsonPublisher<>("Test", objectMapper,
				AsyncDaoUserEventAppenderBiz.SOLARFLUX_TOPIC_FN, false, MqttQos.AtMostOnce);
	}

	@Test
	public void add() {
		// GIVEN
		final Long userId = randomLong();
		final LogEventInfo info = new LogEventInfo(new String[] { "foo", randomString() },
				randomString(), "{\"foo\":123}");

		// WHEN
		final UserEvent result = biz.addEvent(userId, info);

		// THEN
		biz.serviceDidShutdown();

		then(dao).should().add(eventCaptor.capture());

		// @formatter:off
		and.then(result)
			.as("result provided")
			.isNotNull()
			.as("Result same instance as persisted")
			.isSameAs(eventCaptor.getValue())
			.as("Generated event with user ID")
			.returns(userId, from(UserEvent::getUserId))
			.as("Generated event used tags from info")
			.returns(info.getTags(), from(UserEvent::getTags))
			.as("Generated event used message from info")
			.returns(info.getMessage(), from(UserEvent::getMessage))
			.as("Generated event used data from info")
			.returns(info.getData(), from(UserEvent::getData))
			.extracting(UserEvent::getEventId)
			.as("Event has UUID populated")
			.isNotNull()
			;
		// @formatter:on
	}

	private static class TestAppenderDao implements UserEventAppenderDao {

		private final CountDownLatch latch;
		private final List<UserEvent> events;

		private TestAppenderDao(CountDownLatch latch) {
			super();
			this.latch = latch;
			this.events = new ArrayList<>();
		}

		@Override
		public void add(UserEvent event) {
			try {
				latch.await();
			} catch ( InterruptedException e ) {
				// ignore
			}
			events.add(event);
		}

	}

	@Test
	public void add_queueFull() {
		// GIVEN
		// setup queue with capacity of 1
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(1);
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1L, TimeUnit.MINUTES, queue);
		final CountDownLatch latch = new CountDownLatch(1);
		final var dao = new TestAppenderDao(latch);
		biz = new AsyncDaoUserEventAppenderBiz(executor, dao, stats, uuidGenerator);

		final Long userId = randomLong();

		final int infoCount = 3;
		final List<LogEventInfo> infos = new ArrayList<>(infoCount);
		for ( int i = 0; i < infoCount; i++ ) {
			infos.add(new LogEventInfo(new String[] { "foo", randomString() }, randomString(),
					"{\"foo\":123}"));
		}

		// WHEN
		final List<UserEvent> results = new ArrayList<>(infoCount);
		for ( LogEventInfo info : infos ) {
			results.add(biz.addEvent(userId, info));
		}

		// THEN
		latch.countDown(); // unblock DAO
		biz.serviceDidShutdown();

		// @formatter:off
		and.then(results)
			.as("Events generated for every add")
			.hasSize(infoCount)
			;

		and.then(dao.events)
			.as("Non-rejected events captured (first 2)")
			.containsExactlyElementsOf(results.subList(0, 2))
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void add_solarFlux() throws IOException {
		// GIVEN
		biz.setSolarFluxPublisher(solarFluxPublisher);
		solarFluxPublisher.onMqttServerConnectionEstablished(mqttConnection, false);

		final Long userId = randomLong();

		given(mqttConnection.isEstablished()).willReturn(true);

		@SuppressWarnings("rawtypes")
		Future f = new SucceededFuture<Void>(null, null);
		given(mqttConnection.publish(any())).willReturn(f);

		// WHEN
		LogEventInfo info = new LogEventInfo(new String[] { "foo", randomString() }, randomString(),
				"{\"foo\":123}");
		UserEvent result = biz.addEvent(userId, info);

		// THEN
		biz.serviceDidShutdown();

		then(dao).should().add(eventCaptor.capture());

		then(mqttConnection).should().publish(messageCaptor.capture());

		// @formatter:off
		and.then(result)
			.as("result provided")
			.isNotNull()
			.as("Result same instance as persisted")
			.isSameAs(eventCaptor.getValue())
			.as("Generated event with user ID")
			.returns(userId, from(UserEvent::getUserId))
			.as("Generated event used tags from info")
			.returns(info.getTags(), from(UserEvent::getTags))
			.as("Generated event used message from info")
			.returns(info.getMessage(), from(UserEvent::getMessage))
			.as("Generated event used data from info")
			.returns(info.getData(), from(UserEvent::getData))
			.extracting(UserEvent::getEventId)
			.as("Event has UUID populated")
			.isNotNull()
			;
		
		and.then(messageCaptor.getValue())
			.as("MqttMessage published for event")
			.isNotNull()
			.as("MqttMessage topic")
			.returns(format("user/%d/event", userId), from(MqttMessage::getTopic))
			.as("MqttMessage retained")
			.returns(false, from(MqttMessage::isRetained))
			.as("MqttMessage QoS")
			.returns(MqttQos.AtMostOnce, from(MqttMessage::getQosLevel))
			.as("MqttMessage payload is UserEvent JSON data")
			.returns(objectMapper.writeValueAsBytes(eventCaptor.getValue()), from(MqttMessage::getPayload))
			;
		// @formatter:on
	}

	@Test
	public void add_concurrent() throws InterruptedException {
		// GIVEN
		final int taskCount = 100;

		// WHEN
		Queue<UserEvent> events = new ConcurrentLinkedQueue<>();
		ExecutorService exec = Executors.newWorkStealingPool(6);
		for ( int i = 0; i < taskCount; i++ ) {
			long userId = i % 10;
			exec.submit(new Runnable() {

				@Override
				public void run() {
					LogEventInfo info = new LogEventInfo(new String[] { "foo", randomString() },
							randomString(), "{\"foo\":123}");
					log.info("Adding event info {}", info);
					UserEvent event = biz.addEvent(userId, info);
					events.add(event);
				}
			});
		}
		exec.shutdown();
		exec.awaitTermination(1, TimeUnit.MINUTES);

		// THEN
		biz.serviceDidShutdown();

		then(dao).should(times(taskCount)).add(eventCaptor.capture());

		and.then(stats.get(UserEventStats.EventsAdded)).isEqualTo(taskCount);
		and.then(stats.get(UserEventStats.EventsStored)).isEqualTo(taskCount);

		UserEvent[] added = events.toArray(UserEvent[]::new);
		UserEvent[] stored = eventCaptor.getAllValues().toArray(UserEvent[]::new);
		and.then(added).as("Added and stored the same").containsExactlyInAnyOrder(stored);
	}

	@Test
	public void taggedEventTopic_oneTag() {
		// GIVEN
		final Long userId = UUID.randomUUID().getLeastSignificantBits();
		final UserEvent evt = new UserEvent(userId, uuidGenerator.generate(), new String[] { "a" },
				"msg", "data");

		// WHEN
		final String topic = AsyncDaoUserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(evt);

		// THEN
		and.then(topic).as("Topic generated wihtout tags").isEqualTo("user/%d/event/a", userId);
	}

	@Test
	public void taggedEventTopic_twoTags() {
		// GIVEN
		final Long userId = UUID.randomUUID().getLeastSignificantBits();
		final UserEvent evt = new UserEvent(userId, uuidGenerator.generate(), new String[] { "a", "b" },
				"msg", "data");

		// WHEN
		final String topic = AsyncDaoUserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(evt);

		// THEN
		and.then(topic).as("Topic generated wihtout tags").isEqualTo("user/%d/event/a/b", userId);
	}

	@Test
	public void taggedEventTopic_threeTags() {
		// GIVEN
		final Long userId = UUID.randomUUID().getLeastSignificantBits();
		final UserEvent evt = new UserEvent(userId, uuidGenerator.generate(),
				new String[] { "a", "b", "c" }, "msg", "data");

		// WHEN
		final String topic = AsyncDaoUserEventAppenderBiz.SOLARFLUX_TAGGED_TOPIC_FN.apply(evt);

		// THEN
		and.then(topic).as("Topic generated wihtout tags").isEqualTo("user/%d/event/a/b/c", userId);
	}

}
