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
import static java.util.Arrays.sort;
import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SucceededFuture;
import net.solarnetwork.central.biz.UuidGenerator;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz.UserEventStats;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.SolarFluxPublisher;
import net.solarnetwork.central.support.TimeBasedV7UuidGenerator;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.util.StatCounter;

/**
 * Test cases for the {@link AsyncDaoUserEventAppenderBiz}.
 * 
 * @author matt
 * @version 1.1
 */
public class AsyncDaoUserEventAppenderBizTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private UuidGenerator uuidGenerator;
	private ExecutorService executor;
	private UserEventAppenderDao dao;
	private PriorityBlockingQueue<UserEvent> queue;
	private StatCounter stats;
	private AsyncDaoUserEventAppenderBiz biz;
	private ObjectMapper objectMapper;
	private SolarFluxPublisher<UserEvent> solarFluxPublisher;
	private MqttConnection mqttConnection;

	@BeforeEach
	public void setup() {
		uuidGenerator = TimeBasedV7UuidGenerator.INSTANCE;
		executor = Executors.newFixedThreadPool(3);
		dao = EasyMock.createMock(UserEventAppenderDao.class);
		queue = new PriorityBlockingQueue<>(64, AsyncDaoUserEventAppenderBiz.EVENT_SORT);
		stats = new StatCounter("AsyncDaoUserEventAppender",
				"net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz",
				LoggerFactory.getLogger(AsyncDaoUserEventAppenderBiz.class), 10,
				UserEventStats.values());
		biz = new AsyncDaoUserEventAppenderBiz(executor, dao, queue, stats, uuidGenerator);

		objectMapper = new ObjectMapper();
		SimpleModule m = new SimpleModule();
		m.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		objectMapper.registerModule(m);
		objectMapper.registerModule(new JavaTimeModule());

		solarFluxPublisher = new SolarFluxPublisher<>("Test", objectMapper,
				AsyncDaoUserEventAppenderBiz.SOLARFLUX_TOPIC_FN, false, MqttQos.AtMostOnce);
		mqttConnection = EasyMock.createMock(MqttConnection.class);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(dao, mqttConnection);
	}

	private void replayAll() {
		EasyMock.replay(dao, mqttConnection);
	}

	@Test
	public void add() {
		// GIVEN
		Long userId = randomUUID().getMostSignificantBits();

		Capture<UserEvent> eventCaptor = new Capture<>();
		dao.add(capture(eventCaptor));

		// WHEN
		replayAll();
		LogEventInfo info = new LogEventInfo(new String[] { "foo", UUID.randomUUID().toString() },
				UUID.randomUUID().toString(), "{\"foo\":123}");
		UserEvent result = biz.addEvent(userId, info);

		// THEN
		biz.serviceDidShutdown();
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Result same as persisted", eventCaptor.getValue(), is(sameInstance(result)));
		assertThat("Generated event with user ID", result.getUserId(), is(equalTo(userId)));
		assertThat("Generated event has UUID", result.getEventId(), is(notNullValue()));
		assertThat("Generated event took tags from info", result.getTags(),
				arrayContaining(info.getTags()));
		assertThat("Generated event took message from info", result.getMessage(),
				is(equalTo(info.getMessage())));
		assertThat("Generated event took data from info", result.getData(), is(equalTo(info.getData())));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void add_solarFlux() throws IOException {
		// GIVEN
		biz.setSolarFluxPublisher(solarFluxPublisher);
		solarFluxPublisher.onMqttServerConnectionEstablished(mqttConnection, false);

		Long userId = randomUUID().getMostSignificantBits();

		Capture<UserEvent> eventCaptor = new Capture<>();
		dao.add(capture(eventCaptor));

		expect(mqttConnection.isEstablished()).andReturn(true);
		@SuppressWarnings("rawtypes")
		Future f = new SucceededFuture<Void>(null, null);
		Capture<MqttMessage> mqttMsgCaptor = new Capture<>();
		expect(mqttConnection.publish(capture(mqttMsgCaptor))).andReturn(f);

		// WHEN
		replayAll();
		LogEventInfo info = new LogEventInfo(new String[] { "foo", UUID.randomUUID().toString() },
				UUID.randomUUID().toString(), "{\"foo\":123}");
		UserEvent result = biz.addEvent(userId, info);

		// THEN
		biz.serviceDidShutdown();
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Result same as persisted", eventCaptor.getValue(), is(sameInstance(result)));
		assertThat("Generated event with user ID", result.getUserId(), is(equalTo(userId)));
		assertThat("Generated event has UUID", result.getEventId(), is(notNullValue()));
		assertThat("Generated event took tags from info", result.getTags(),
				arrayContaining(info.getTags()));
		assertThat("Generated event took message from info", result.getMessage(),
				is(equalTo(info.getMessage())));
		assertThat("Generated event took data from info", result.getData(), is(equalTo(info.getData())));

		MqttMessage mqttMsg = mqttMsgCaptor.getValue();
		assertThat("MqttMessage published for event", mqttMsg, is(notNullValue()));
		assertThat("MqttMessage topic", mqttMsg.getTopic(),
				is(equalTo(format("user/%d/event", userId))));
		assertThat("MqttMessage retained", mqttMsg.isRetained(), is(equalTo(false)));
		assertThat("MqttMessage QoS", mqttMsg.getQosLevel(), is(equalTo(MqttQos.AtMostOnce)));
		assertThat("MqttMessage payload is UserEvent", Arrays.equals(mqttMsg.getPayload(),
				objectMapper.writeValueAsBytes(eventCaptor.getValue())), is(equalTo(true)));
	}

	@Test
	public void add_concurrent() throws InterruptedException {
		// GIVEN
		final int taskCount = 100;
		Capture<UserEvent> eventsCaptor = new Capture<>(CaptureType.ALL);
		dao.add(capture(eventsCaptor));
		expectLastCall().times(taskCount);

		// WHEN
		replayAll();
		Queue<UserEvent> events = new ConcurrentLinkedQueue<>();
		ExecutorService exec = Executors.newWorkStealingPool(6);
		for ( int i = 0; i < taskCount; i++ ) {
			long userId = i % 10;
			exec.submit(new Runnable() {

				@Override
				public void run() {
					LogEventInfo info = new LogEventInfo(
							new String[] { "foo", UUID.randomUUID().toString() },
							UUID.randomUUID().toString(), "{\"foo\":123}");
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
		assertThat("Stats added count", stats.get(UserEventStats.EventsAdded),
				is(equalTo((long) taskCount)));
		assertThat("Stats stored count", stats.get(UserEventStats.EventsStored),
				is(equalTo((long) taskCount)));

		UserEvent[] added = events.toArray(UserEvent[]::new);
		sort(added, Identity.sortByIdentity());

		UserEvent[] stored = eventsCaptor.getValues().toArray(UserEvent[]::new);
		sort(stored, Identity.sortByIdentity());

		assertThat("Added and stored the same", stored, arrayContaining(added));
	}

}
