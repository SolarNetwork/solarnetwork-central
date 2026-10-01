/* ========================================================================
 * Copyright 2021 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.service.impl.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.DeliverRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link JdbcAuditService} class.
 * 
 * @author matt
 * @version 1.3
 */
@ExtendWith(MockitoExtension.class)
public class JdbcAuditServiceTests extends TestSupport {

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;

	private static final Long TEST_NODE_1 = 1L;
	private static final String TEST_SOURCE_1 = "test.source.1";
	private static final Long TEST_USER_ID = 2L;

	private ConcurrentMap<JdbcAuditService.DelayedKey, AtomicInteger> datumCountMap;

	@Mock
	private DataSource dataSource;
	@Mock
	private Connection jdbcConnection;
	@Mock
	private CallableStatement jdbcStatement;

	private Clock now;
	private Instant topOfHour;
	private JdbcAuditService auditor;

	@BeforeEach
	public void setup() {
		now = Clock.tick(Clock.fixed(Instant.now(), ZoneId.systemDefault()), Duration.ofHours(1));
		topOfHour = Instant.now(now);
		datumCountMap = new ConcurrentHashMap<>(8);

		auditor = new JdbcAuditService(dataSource, datumCountMap, now);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	private void stopAuditingAndWaitForFlush() {
		auditor.disableWriting(Duration.ofSeconds(5));
	}

	private static JdbcAuditService.DelayedKey auditKey(Instant date, Long nodeId, String sourceId) {
		return new JdbcAuditService.DelayedKey(nodeId, sourceId, date, 0);
	}

	private static String topicForNodeSource(Long nodeId, String sourceId) {
		return String.format("node/%d/0/%s", nodeId, sourceId);
	}

	private static String topicForUser(Long userId, String topic) {
		return String.format("user/%d/%s", userId, topic);
	}

	private <K> void assertMapValueZeroOrMissing(Map<K, AtomicInteger> countMap, K key) {
		AtomicInteger l = countMap.get(key);
		if ( l != null ) {
			assertThat("Count for " + key, l.get(), equalTo(0));
		} else {
			assertThat("Count for " + key, l, nullValue());
		}
	}

	private void verifyStatement(Long objId, String sourceId, long ts, int count) throws SQLException {
		verify(jdbcStatement).setString(1,
				sourceId != null ? JdbcAuditService.DEFAULT_AUDIT_MQTT_SERVICE_NAME
						: JdbcAuditService.DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME);
		verify(jdbcStatement).setObject(2, objId);
		verify(jdbcStatement).setString(3, sourceId);
		verify(jdbcStatement).setTimestamp(eq(4), eq(new java.sql.Timestamp(ts)));
		verify(jdbcStatement).setInt(5, count);
		verify(jdbcStatement).execute();
	}

	@Test
	public void auditPublishMessage() throws Exception {
		// GIVEN
		given(dataSource.getConnection()).willReturn(jdbcConnection);

		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		jdbcConnection.close();

		given(jdbcStatement.execute()).willReturn(false);

		// WHEN
		PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hello, world.".getBytes()).build();
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		verify(jdbcConnection, atLeastOnce()).setAutoCommit(true);
		verifyStatement(TEST_NODE_1, TEST_SOURCE_1, topOfHour.toEpochMilli(), msg.getPayload().length);
		assertMapValueZeroOrMissing(datumCountMap, auditKey(topOfHour, TEST_NODE_1, TEST_SOURCE_1));
	}

	@Test
	public void auditDeliverMessage() throws Exception {
		// GIVEN
		given(dataSource.getConnection()).willReturn(jdbcConnection);

		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		given(jdbcStatement.execute()).willReturn(false);

		// WHEN
		DeliverRequest msg = DeliverRequest.builder()
				.withTopic(topicForUser(TEST_USER_ID, "event/ocpp/charger/disconnected"))
				.withPayload("Hi there!".getBytes()).build();
		auditor.auditDeliverMessage(msg);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		then(jdbcConnection).should(atLeastOnce()).setAutoCommit(true);
		then(jdbcConnection).should().close();
		verifyStatement(TEST_USER_ID, null, topOfHour.toEpochMilli(), msg.getPayload().length);
		assertMapValueZeroOrMissing(datumCountMap, auditKey(topOfHour, TEST_USER_ID, null));
	}

	@Test
	public void auditPublishMessage_counterRemovedAfterFlush() throws Exception {
		// GIVEN
		given(dataSource.getConnection()).willReturn(jdbcConnection);

		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		given(jdbcStatement.execute()).willReturn(false);

		// WHEN
		PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hello, world.".getBytes()).build();
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		verifyStatement(TEST_NODE_1, TEST_SOURCE_1, topOfHour.toEpochMilli(), msg.getPayload().length);
		and.then(datumCountMap)
				.as("Counter removed once flushed, as nothing queues the key again after the hour")
				.isEmpty();
	}

	@Test
	public void auditPublishMessage_interruptedDuringUpdateDelay_countNotFlushedAgain()
			throws Exception {
		// GIVEN
		given(dataSource.getConnection()).willReturn(jdbcConnection);

		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		final CountDownLatch executed = new CountDownLatch(1);
		given(jdbcStatement.execute()).willAnswer(_ -> {
			executed.countDown();
			return false;
		});

		// delay long enough that the writer is still sleeping after the update when interrupted
		auditor.setUpdateDelay(TimeUnit.MINUTES.toMillis(1));

		// WHEN
		PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hello, world.".getBytes()).build();
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);

		auditor.enableWriting();
		final boolean flushed = executed.await(5, TimeUnit.SECONDS);

		// interrupt the writer while it sleeps after the update
		auditor.disableWriting(Duration.ofMillis(100));

		// start another writer, which would flush the count again if it had been added back
		auditor.enableWriting();
		auditor.disableWriting(Duration.ofMillis(500));

		// THEN
		and.then(flushed).as("Count flushed").isTrue();
		verifyStatement(TEST_NODE_1, TEST_SOURCE_1, topOfHour.toEpochMilli(), msg.getPayload().length);
		and.then(datumCountMap).as("Flushed count not added back when interrupted").isEmpty();
	}

	@Test
	public void auditPublishMessage_concurrentAddAndFlush_noLostCounts() throws Exception {
		// GIVEN
		final int adderCount = 4;
		final int minFlushCount = 5_000;
		final long maxNanos = TimeUnit.SECONDS.toNanos(20);

		// stub-only statement that tallies the flushed counts, to avoid recording every invocation
		final CallableStatement stmt = mock(CallableStatement.class, withSettings().stubOnly());
		final AtomicInteger stmtCount = new AtomicInteger();
		final AtomicInteger flushCount = new AtomicInteger();
		final AtomicLong flushed = new AtomicLong();
		willAnswer(inv -> {
			stmtCount.set(inv.getArgument(1));
			return null;
		}).given(stmt).setInt(eq(5), anyInt());
		given(stmt.execute()).willAnswer(_ -> {
			flushed.addAndGet(stmtCount.get());
			flushCount.incrementAndGet();
			return false;
		});

		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(stmt);

		// flush immediately, so the writer is constantly racing the adders on the same key
		auditor.setFlushDelay(0);

		final PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1)).withPayload(new byte[] { 1 })
				.build();

		// WHEN
		auditor.enableWriting();

		final AtomicLong added = new AtomicLong();
		final long start = System.nanoTime();
		final Thread[] adders = new Thread[adderCount];
		for ( int i = 0; i < adderCount; i++ ) {
			adders[i] = new Thread(() -> {
				// keep adding until the writer has flushed enough times to have raced us
				while ( flushCount.get() < minFlushCount && System.nanoTime() - start < maxNanos ) {
					auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);
					added.incrementAndGet();
				}
			}, "Adder-" + i);
			adders[i].start();
		}
		for ( Thread adder : adders ) {
			adder.join();
		}

		// wait for writer to flush everything added
		final long drainStart = System.nanoTime();
		while ( flushed.get() < added.get()
				&& System.nanoTime() - drainStart < TimeUnit.SECONDS.toNanos(5) ) {
			Thread.sleep(50);
		}
		// the queue is drained now, so do not wait long for the blocked writer to exit
		auditor.disableWriting(Duration.ofMillis(100));

		// THEN
		and.then(flushCount.get()).as("Writer flushed while counts added")
				.isGreaterThanOrEqualTo(minFlushCount);
		and.then(flushed.get()).as("Every count added is flushed").isEqualTo(added.get());
		and.then(datumCountMap).as("Counter removed once flushed").isEmpty();
	}

}
