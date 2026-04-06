/* ==================================================================
 * DatumSqsOverflowQueue_IntegrationTests.java - 19/03/2026 5:32:21 pm
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

package net.solarnetwork.central.datum.support.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.basicTable;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.support.DatumJsonEntityCodec;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDaoGenericAdapter;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.support.LinkedHashSetBlockingQueue;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test cases for the {@link SqsOverflowQueue} for datum collection with actual
 * integration with SQS.
 *
 * <p>
 * Note this test requires a {@code test.sqs} system property be defined with
 * some value, and a {@code sqs-datum-queue.properties} classpath resource be
 * created with properties that define the SQS connection details to use:
 * </p>
 *
 * <pre>{@code
 * region = us-west-2
 * queueName = datum-queue-test
 * accessKey = AWS_ACCESS_TOKEN_HERE
 * secretKey = AWS_TOKEN_SECRET_HERE
 * }</pre>
 *
 * @author matt
 * @version 1.0
 */
@EnabledIfSystemProperty(named = "test.sqs", matches = ".*")
public class DatumSqsOverflowQueue_IntegrationTests extends BaseSqsIntegrationTestsSupport {

	private static final JsonMapper JSON_MAPPER = DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER;

	private final class TestDatumDao implements DatumWriteOnlyDao {

		private final Function<Object, DatumPK> entityIdFn;

		private TestDatumDao(Function<Object, DatumPK> entityIdFn) {
			super();
			this.entityIdFn = requireNonNullArgument(entityIdFn, "entityIdFn");
		}

		@Override
		public DatumPK persist(GeneralObjectDatum<? extends GeneralObjectDatumKey> entity) {
			doStore(entity);
			return entityIdFn.apply(entity);
		}

		@Override
		public DatumPK store(Datum datum) {
			doStore(datum);
			return entityIdFn.apply(datum);
		}

		@Override
		public DatumPK store(StreamDatum datum) {
			doStore(datum);
			return entityIdFn.apply(datum);
		}

	}

	private static final int WORK_QUEUE_SIZE = 4;

	private StatTracker stats;
	private BlockingQueue<SqsOverflowQueue.WorkItem<Object, DatumPK>> workQueue;
	private BlockingQueue<String> completedSqsMessageHandles;
	private List<Object> stored;
	private DatumJsonEntityCodec entityCodec;
	private SqsOverflowQueue<Object, DatumPK> collector;

	@BeforeEach
	public void setup() {
		stored = Collections.synchronizedList(new ArrayList<>(1000));
		workQueue = new ArrayBlockingQueue<>(WORK_QUEUE_SIZE);
		completedSqsMessageHandles = new LinkedHashSetBlockingQueue<>(9);
		stats = new StatTracker("DatumQueue", null, log, 50);
		entityCodec = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		collector = new SqsOverflowQueue<Object, DatumPK>(stats, "Datum", client, SQS_PROPS.getUrl(),
				workQueue, completedSqsMessageHandles,
				new DatumWriteOnlyDaoGenericAdapter(new TestDatumDao(entityCodec::entityId)), entityCodec);
		collector.setExceptionHandler(this);
		collector.setReadConcurrency(1);
		collector.setWriteConcurrency(2);
		collector.setWorkItemMaxWaitMs(200);
		collector.setShutdownWaitSecs(3600);
		collector.setReadMaxMessageCount(WORK_QUEUE_SIZE);
		collector.setReadSleepThrottleStepMs(250L);
		collector.setReadSleepMaxMs(2000L);
		collector.serviceDidStartup();
	}

	@AfterEach
	public void teardown() {
		collector.serviceDidShutdown();
	}

	private void doStore(Object o) {
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

	private GeneralDatum createDatum(Long nodeId, String sourceId, Instant ts) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("f", 1);
		return GeneralDatum.nodeDatum(nodeId, sourceId, ts, s);
	}

	private GeneralDatum createLocationDatum(Long locId, String sourceId, Instant ts) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("f", 1);
		return GeneralDatum.locationDatum(locId, sourceId, ts, s);
	}

	private DatumEntity createStreamDatum(UUID streamId, Instant ts) {
		DatumProperties p = new DatumProperties();
		p.setInstantaneous(new BigDecimal[] { BigDecimal.ONE });
		DatumEntity d = new DatumEntity(streamId, ts, Instant.now(), p);
		return d;
	}

	/**
	 * Try to simulate a common runtime pattern, with a "rush" of datum produced
	 * with variable-speed writer throughput.
	 *
	 * <p>
	 * The goal of the test is to demonstrate that even with a overflowing
	 * buffer cache, spooling to the disk cache, that eventually all datum are
	 * processed as the writer threads catch up.
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
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Long locId = randomLong();
		final String locSourceId = randomString();
		final UUID streamId = randomUUID();
		final Instant startTs = Instant.now().truncatedTo(ChronoUnit.DAYS);
		final AtomicInteger producerCounter = new AtomicInteger();
		final AtomicInteger addCounter = new AtomicInteger();
		final int producerCount = 4;
		final int maxCount = 1_000;

		final Object[] produced = new Object[maxCount];

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
						Object datum;
						if ( RNG.nextBoolean() ) {
							Datum d;
							if ( RNG.nextBoolean() ) {
								d = createDatum(nodeId, sourceId, startTs.plusMillis(count));
							} else {
								d = createLocationDatum(locId, locSourceId, startTs.plusMillis(count));
							}
							collector.persist(d);
							datum = d;
						} else {
							DatumEntity d = createStreamDatum(streamId, startTs.plusMillis(count));
							collector.persist(d);
							datum = d;
						}
						addCounter.incrementAndGet();
						produced[count - 1] = datum;
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

		List<Object> normalizedStored = stored.stream().map(o -> {
			if ( o instanceof StreamDatum d && !(o instanceof DatumEntity) ) {
				// convert to DatumEntity because when overflow gets parsed back as BasicStreamDatum
				return new DatumEntity(d.getStreamId(), d.getTimestamp(), d.getTimestamp(),
						d.getProperties());
			}
			return o;
		}).toList();

		then(normalizedStored).as("Stored every datum produced").containsAll(Arrays.asList(produced));
	}

}
