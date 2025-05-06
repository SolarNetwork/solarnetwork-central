/* ==================================================================
 * SqsDatumCollector_IntegrationTests.java - 29/04/2025 4:09:10 pm
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

package net.solarnetwork.central.datum.support.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.basicTable;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatumKey;
import net.solarnetwork.central.datum.support.SqsDatumCollector;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.support.SqsProperties;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Test cases for the {@link SqsDatumCollector} with actual integration with
 * SQS.
 *
 * <p>
 * Note this test requires a {@code sqs} system property be defined with some
 * value, and a {@code sqs-datum-queue.properties} classpath resource be created
 * with properties that define the SQS connection details to use:
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
@ExtendWith(MockitoExtension.class)
@EnabledIfSystemProperty(named = "test.sqs", matches = ".*")
public class SqsDatumCollector_IntegrationTests implements UncaughtExceptionHandler {

	private static SqsProperties SQS_PROPS;

	private static final Logger log = LoggerFactory.getLogger(SqsDatumCollector_IntegrationTests.class);

	@BeforeAll
	public static void setupClass() {
		Properties p = new Properties();
		try {
			InputStream in = SqsDatumCollector_IntegrationTests.class.getClassLoader()
					.getResourceAsStream("sqs-datum-queue.properties");
			if ( in != null ) {
				p.load(in);
				in.close();
			} else {
				throw new RuntimeException(
						"The sqs-datum-queue.properties classpath resource is missing.");
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		Map<String, Object> m = new LinkedHashMap<>(p.size());
		for ( Map.Entry<Object, Object> me : p.entrySet() ) {
			m.put(me.getKey().toString(), me.getValue());
		}
		SQS_PROPS = SqsProperties.ofServiceProperties(m);
	}

	private static SqsAsyncClient createSqsAsyncClient() {
		SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
				.region(Region.of(SQS_PROPS.getRegion()));
		String accessKey = SQS_PROPS.getAccessKey();
		String secretKey = SQS_PROPS.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	private static SqsClient createSqsClient() {
		SqsClientBuilder builder = SqsClient.builder().region(Region.of(SQS_PROPS.getRegion()));
		String accessKey = SQS_PROPS.getAccessKey();
		String secretKey = SQS_PROPS.getSecretKey();
		if ( accessKey != null && accessKey.length() > 0 && secretKey != null
				&& secretKey.length() > 0 ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	private static void drainQueue(boolean logMessages) {
		SqsClient client = createSqsClient();
		String queueUrl = null;
		try {
			GetQueueUrlResponse urlRes = client
					.getQueueUrl((b) -> b.queueName(SQS_PROPS.getQueueName()));
			queueUrl = urlRes.queueUrl();
			log.info("SQS queue URL: {}", queueUrl);
		} catch ( QueueDoesNotExistException e ) {
			throw new IllegalArgumentException(
					String.format("Queue [%s] does not exist (using region %s).",
							SQS_PROPS.getQueueName(), SQS_PROPS.getRegion()));
		}

		ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(0)
				.maxNumberOfMessages(10).build();
		while ( true ) {
			ReceiveMessageResponse res = client.receiveMessage(req);
			if ( res == null ) {
				break;
			}
			List<Message> msgs = res.messages();
			log.info("SQS queue {} discovered {} messages to drain.", queueUrl, msgs.size());
			if ( logMessages && msgs.size() > 0 ) {
				Map<String, String> map = new LinkedHashMap<>(msgs.size());
				for ( Message msg : msgs ) {
					map.put(msg.messageId(), msg.body());
				}
				log.info("Purging messages:\n{}", basicTable(map, "Message ID", "Message Body"));
			}
			if ( msgs == null || msgs.isEmpty() ) {
				break;
			}
			List<DeleteMessageBatchRequestEntry> delEntries = new ArrayList<>(8);
			for ( Message msg : msgs ) {
				delEntries.add(DeleteMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
						.receiptHandle(msg.receiptHandle()).build());
			}
			DeleteMessageBatchRequest delReq = DeleteMessageBatchRequest.builder().queueUrl(queueUrl)
					.entries(delEntries).build();
			client.deleteMessageBatch(delReq);
		}
	}

	private final class TestDatumDao implements DatumWriteOnlyDao {

		@Override
		public DatumPK persist(GeneralObjectDatum<? extends GeneralObjectDatumKey> entity) {
			doStore(entity);
			return null;
		}

		@Override
		public DatumPK store(Datum datum) {
			doStore(datum);
			return null;
		}

		@Override
		public DatumPK store(StreamDatum datum) {
			doStore(datum);
			return null;
		}

	}

	private static final int WORK_QUEUE_SIZE = 4;

	private BlockingQueue<SqsDatumCollector.WorkItem> workQueue;
	private TestDatumDao delegateDao;
	private StatTracker stats;
	private SqsDatumCollector collector;

	private List<Throwable> uncaughtExceptions;
	private List<Object> stored;

	@BeforeEach
	public void setup() {
		drainQueue(false);

		SqsAsyncClient client = createSqsAsyncClient();
		workQueue = new ArrayBlockingQueue<>(WORK_QUEUE_SIZE);
		delegateDao = new TestDatumDao();

		stats = new StatTracker("SqsDatumCollector", null, log, 50);

		uncaughtExceptions = new ArrayList<>(2);
		stored = Collections.synchronizedList(new ArrayList<>(1000));

		collector = new SqsDatumCollector(client, SQS_PROPS.getUrl(),
				DatumJsonUtils.newDatumObjectMapper(), workQueue, delegateDao, stats);
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
		drainQueue(true);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		uncaughtExceptions.add(e);
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
	public void addNodeDatumToCache_manyThreads_overflow_continuousAdd() throws Exception {
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
							collector.store(d);
							datum = d;
						} else {
							DatumEntity d = createStreamDatum(streamId, startTs.plusMillis(count));
							collector.store(d);
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
							.get(SqsDatumCollector.SQS_QUEUE_MESSAGE_COUNT_STATUS_PROP);
					Object sqsProcessingMessageCount = pr.getProperties()
							.get(SqsDatumCollector.SQS_QUEUE_PROCESSING_MESSAGE_COUNT_STATUS_PROP);
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
