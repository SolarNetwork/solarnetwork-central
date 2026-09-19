/* ==================================================================
 * SqsDaoUserEventAppenderBiz_IntegrationTests.java - 20/03/2026 6:41:01 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.biz.impl.test;

import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.basicTable;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import net.solarnetwork.central.biz.dao.DaoUserEventAppenderBiz;
import net.solarnetwork.central.common.biz.impl.IdentityJsonEntityCodec;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import net.solarnetwork.central.common.dao.GenericWriteOnlyDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.central.support.UserEventBasicDeserializer;
import net.solarnetwork.central.support.UserEventBasicSerializer;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.TimeBasedV7UuidGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test cases for the {@link DaoUserEventAppenderBiz} class using a
 * {@link SqsOverflowQueue} for event collection with actual integration with
 * SQS.
 *
 * <p>
 * Note this test requires a {@code test.sqs} system property be defined with
 * some value, and a {@code sqs-overflow-queue.properties} classpath resource be
 * created with properties that define the SQS connection details to use:
 * </p>
 *
 * <pre>{@code
 * region = us-west-2
 * queueName = user-event-queue-test
 * accessKey = AWS_ACCESS_TOKEN_HERE
 * secretKey = AWS_TOKEN_SECRET_HERE
 * }</pre>
 * 
 * @author matt
 * @version 1.0
 */
@EnabledIfSystemProperty(named = "test.sqs", matches = ".*")
public class SqsDaoUserEventAppenderBiz_IntegrationTests extends BaseSqsIntegrationTestsSupport {

	public static final JacksonModule EVENT_MODULE;

	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventBasicSerializer.INSTANCE);
		m.addDeserializer(UserEvent.class, UserEventBasicDeserializer.INSTANCE);
		EVENT_MODULE = m;
	}

	private static final JsonMapper JSON_MAPPER = JsonUtils.JSON_OBJECT_MAPPER.rebuild()
			.addModule(EVENT_MODULE).build();

	private static final IdentityJsonEntityCodec<UserEvent, UserUuidPK> ENTITY_CODEC = new IdentityJsonEntityCodec<>(
			JSON_MAPPER, UserEvent.class);

	private final class TestDao implements GenericWriteOnlyDao<UserEvent, UserUuidPK> {

		private TestDao() {
			super();
		}

		@Override
		public UserUuidPK persist(UserEvent entity) {
			doStore(entity);
			return entity.id();
		}
	}

	private static final int WORK_QUEUE_SIZE = 4;

	private StatTracker stats;
	private BlockingQueue<SqsOverflowQueue.WorkItem<UserEvent, UserUuidPK>> workQueue;
	private BlockingQueue<String> completedSqsMessageHandles;
	private List<UserEvent> stored;
	private SqsOverflowQueue<UserEvent, UserUuidPK> collector;
	private DaoUserEventAppenderBiz service;

	@BeforeEach
	public void setup() {
		stored = Collections.synchronizedList(new ArrayList<>(1000));
		workQueue = new ArrayBlockingQueue<>(WORK_QUEUE_SIZE);
		completedSqsMessageHandles = new LinkedHashSetBlockingQueue<>(9);
		stats = new StatTracker("UserEventQueue", null, log, 50);

		collector = new SqsOverflowQueue<UserEvent, UserUuidPK>(stats, "UserEvent", client,
				SQS_PROPS.getUrl(), workQueue, completedSqsMessageHandles, new TestDao(), ENTITY_CODEC);
		collector.setExceptionHandler(this);
		collector.setReadConcurrency(1);
		collector.setWriteConcurrency(2);
		collector.setWorkItemMaxWaitMs(200);
		collector.setShutdownWaitSecs(3600);
		collector.setReadMaxMessageCount(WORK_QUEUE_SIZE);
		collector.setReadSleepThrottleStepMs(250L);
		collector.setReadSleepMaxMs(2000L);
		collector.serviceDidStartup();

		service = new DaoUserEventAppenderBiz(collector, TimeBasedV7UuidGenerator.INSTANCE_MICROS);
	}

	@AfterEach
	public void teardown() {
		collector.serviceDidShutdown();
	}

	private void doStore(UserEvent o) {
		try {
			// simulate taking some time
			long time = 20;
			if ( RNG.nextBoolean() ) {
				log.trace("Persist: random long thread sleep {}...", o);
				time = RNG.nextLong(20, 200);
			}
			Thread.sleep(time);
		} catch ( InterruptedException e ) {
			// ignore
		}
		stored.add(o);
	}

	/**
	 * Try to simulate a common runtime pattern, with a "rush" of events
	 * produced with variable-speed writer throughput.
	 *
	 * <p>
	 * The goal of the test is to demonstrate that even with a overflowing SQS
	 * queue that eventually all datum are processed as the writer threads catch
	 * up.
	 * </p>
	 *
	 * @throws Exception
	 *         if any error occurs
	 */
	@Test
	public void manyThreads_overflow_continuousAdd() throws Exception {
		// GIVEN
		ExecutorService executor = Executors.newFixedThreadPool(4);

		// WHEN
		final AtomicInteger producerCounter = new AtomicInteger();
		final AtomicInteger addCounter = new AtomicInteger();
		final int producerCount = 4;
		final int maxCount = 1_000;

		final UserEvent[] produced = new UserEvent[maxCount];

		for ( int i = 0; i < producerCount; i++ ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					while ( true ) {
						int count = producerCounter.incrementAndGet();
						if ( count > maxCount ) {
							log.info("Producer: maximum reached: {}", maxCount);
							return;
						}
						Map<String, Object> data = new HashMap<>(8);
						for ( int i = 0, len = RNG.nextInt(8); i < len; i++ ) {
							data.put(randomString(), RNG.nextBoolean() ? randomString() : randomLong());
						}
						LogEventInfo event = new LogEventInfo(new String[] { randomString() },
								randomString(), JsonUtils.getJSONString(data));
						UserEvent userEvent = service.addEvent(randomLong(), event);
						addCounter.incrementAndGet();
						produced[count - 1] = userEvent;
						long sleep = count > maxCount * 0.75 ? 30
								: Math.max(0, (count - maxCount / 2) / 4);
						if ( sleep > 0 ) {
							log.debug("Producer: sleep {}", sleep);
							try {
								Thread.sleep(sleep);
							} catch ( InterruptedException e ) {
								// ignore
							}
						}
					}
				}

			});
		}

		Thread.sleep(1_000);
		PingTest.Result pingResult1 = collector.performPingTest();

		// let producers go until max reached
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		// wait for stores to stop happening...
		final int messageProcessTries = 5;
		final int unchangedTries = 3;
		int storedCount = -1;
		int messageProcessRemaining = messageProcessTries;
		int unchangeRemaining = unchangedTries;
		while ( true ) {
			Thread.sleep(1_000L);
			int newStoredCount = stored.size();
			if ( newStoredCount == storedCount ) {
				if ( --unchangeRemaining < 1 ) {
					// first check size of messages... should drop to 0
					PingTest.Result pr = collector.performPingTest();
					log.info("Ping result\n{}", basicTable(pr.getProperties(), "Property", "Value"));
					Object sqsMessageCount = pr.getProperties()
							.get(SqsOverflowQueue.SQS_QUEUE_MESSAGE_COUNT_STATUS_PROP);
					Object sqsProcessingMessageCount = pr.getProperties()
							.get(SqsOverflowQueue.SQS_QUEUE_PROCESSING_MESSAGE_COUNT_STATUS_PROP);
					if ( pr.isSuccess() && ((sqsMessageCount instanceof Number n && n.longValue() > 0L)
							|| (sqsProcessingMessageCount instanceof Number n2
									&& n2.longValue() > 0L)) ) {
						if ( --messageProcessRemaining < 1 ) {
							log.info("SQS queue approximately not empty ({}, {}); giving up waiting",
									sqsMessageCount, sqsProcessingMessageCount);
							break;
						}
						log.info("SQS queue approximately not empty ({}, {}); will wait to process",
								sqsMessageCount, sqsProcessingMessageCount);
						continue;
					}
					break;
				}
			} else {
				unchangeRemaining = unchangedTries;
			}
			storedCount = newStoredCount;
		}

		collector.shutdownAndWait();

		PingTest.Result pingResult2 = collector.performPingTest();
		log.info("Ping result 1\n{}", basicTable(pingResult1.getProperties(), "Property", "Value"));
		log.info("Ping result 2\n{}", basicTable(pingResult2.getProperties(), "Property", "Value"));

		int addCount = addCounter.get();
		log.info("Add: {}, store: {}", addCount, storedCount);

		// THEN
		then(storedCount).as("Stored at least as many that produced").isGreaterThanOrEqualTo(addCount);

		then(stored).as("Stored every datum produced").containsAll(Arrays.asList(produced));
	}

}
