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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditorCount;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.DeliverRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcAuditService} class.
 *
 * <p>
 * The writing behavior comes from the base auditor class, and is tested with
 * it; these tests cover how messages are counted and written.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcAuditServiceTests extends TestSupport {

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;

	private static final Long TEST_NODE_1 = 1L;
	private static final String TEST_SOURCE_1 = "test.source.1";
	private static final Long TEST_USER_ID = 2L;

	@Mock
	private DataSource dataSource;
	@Mock
	private Connection jdbcConnection;
	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;
	private Clock testClock;
	private StatTracker stats;
	private JdbcAuditService auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		stats = new StatTracker("JdbcMqttAuditor", "", log, 20);
		auditor = new JdbcAuditService(dataSource, datumCountMap, testClock, stats);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	@AfterEach
	public void teardown() {
		// stop any writer a test left running
		auditor.disableWriting();
	}

	private void givenWriterConnection() throws SQLException {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcAuditService.DEFAULT_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);
	}

	/**
	 * Start writing, write the counts added so far, and stop.
	 */
	private void writeCounts() throws InterruptedException {
		auditor.serviceDidStartup();
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ( stats.get(JdbcNodeServiceAuditorCount.CountsFlushed) < 1 && System.nanoTime() < end ) {
			Thread.sleep(10);
		}
		auditor.serviceDidShutdown();
	}

	private static String topicForNodeSource(Long nodeId, String sourceId) {
		return String.format("node/%d/datum/0/%s", nodeId, sourceId);
	}

	private static String topicForUser(Long userId, String topic) {
		return String.format("user/%d/%s", userId, topic);
	}

	@Test
	public void auditPublishMessage() throws Exception {
		// GIVEN
		givenWriterConnection();

		// WHEN
		PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hello, world.".getBytes()).build();
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);

		writeCounts();

		// THEN
		then(jdbcConnection).should().setAutoCommit(true);
		then(jdbcStatement).should().setString(1, JdbcAuditService.DEFAULT_AUDIT_MQTT_SERVICE_NAME);
		then(jdbcStatement).should().setObject(2, TEST_NODE_1);
		then(jdbcStatement).should().setString(3, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(4, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(5, msg.getPayload().length);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("Counter removed once written")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditPublishMessage_noSourceId_ignored() throws Exception {
		// GIVEN
		PublishRequest msg = PublishRequest.builder().withTopic("foo")
				.withPayload("Hello, world.".getBytes()).build();

		// WHEN
		auditor.auditPublishMessage(null, TEST_NODE_1, null, msg);

		// THEN
		// @formatter:off
		and.then(datumCountMap)
			.as("Publish without a source not counted, as it would be written as a user count")
			.isEmpty()
			;
		and.then(stats.get(JdbcNodeServiceAuditorCount.ResultsAdded))
			.as("No count added")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void auditPublishMessage_noPayload_ignored() throws Exception {
		// GIVEN
		PublishRequest msg = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1)).build();

		// WHEN
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, msg);

		// THEN
		// @formatter:off
		and.then(datumCountMap)
			.as("Publish without a payload not counted")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditDeliverMessage() throws Exception {
		// GIVEN
		givenWriterConnection();

		// WHEN
		DeliverRequest msg = DeliverRequest.builder()
				.withTopic(topicForUser(TEST_USER_ID, "event/ocpp/charger/disconnected"))
				.withPayload("Hi there!".getBytes()).build();
		auditor.auditDeliverMessage(msg);

		writeCounts();

		// THEN
		then(jdbcStatement).should().setString(1,
				JdbcAuditService.DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME);
		then(jdbcStatement).should().setObject(2, TEST_USER_ID);
		then(jdbcStatement).should().setString(3, null);
		then(jdbcStatement).should().setTimestamp(4, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(5, msg.getPayload().length);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("Counter removed once written")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditDeliverMessage_topicWithoutUser_ignored() throws Exception {
		// GIVEN
		DeliverRequest msg = DeliverRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hi there!".getBytes()).build();

		// WHEN
		auditor.auditDeliverMessage(msg);

		// THEN
		// @formatter:off
		and.then(datumCountMap)
			.as("Deliver to a topic without a user not counted")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditMessages_coalesced() {
		// GIVEN
		final PublishRequest pub = PublishRequest.builder()
				.withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
				.withPayload("Hello, world.".getBytes()).build();
		final DeliverRequest del = DeliverRequest.builder()
				.withTopic(topicForUser(TEST_USER_ID, "event/ocpp/charger/disconnected"))
				.withPayload("Hi there!".getBytes()).build();

		// WHEN
		for ( int i = 0; i < 3; i++ ) {
			auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1, pub);
			auditor.auditDeliverMessage(del);
		}

		// THEN
		final DatumId pubKey = DatumId.nodeId(TEST_NODE_1, TEST_SOURCE_1, testClock.instant());
		final DatumId delKey = DatumId.nodeId(TEST_USER_ID, null, testClock.instant());
		// @formatter:off
		and.then(datumCountMap)
			.as("Publish counted per node and source, deliver per user")
			.containsOnlyKeys(pubKey, delKey)
			;
		and.then(datumCountMap.get(pubKey))
			.as("Publish byte counts added together")
			.hasValue(pub.getPayload().length * 3)
			;
		and.then(datumCountMap.get(delKey))
			.as("Deliver byte counts added together")
			.hasValue(del.getPayload().length * 3)
			;
		// @formatter:on
	}

	@Test
	public void auditMessages_serviceNames() throws Exception {
		// GIVEN
		givenWriterConnection();

		final String publishServiceName = "pubx";
		final String deliverServiceName = "delx";
		auditor.setMqttServiceName(publishServiceName);
		auditor.setDeliverMqttServiceName(deliverServiceName);

		// WHEN
		auditor.auditPublishMessage(null, TEST_NODE_1, TEST_SOURCE_1,
				PublishRequest.builder().withTopic(topicForNodeSource(TEST_NODE_1, TEST_SOURCE_1))
						.withPayload("Hello, world.".getBytes()).build());
		auditor.auditDeliverMessage(DeliverRequest.builder()
				.withTopic(topicForUser(TEST_USER_ID, "event/ocpp/charger/disconnected"))
				.withPayload("Hi there!".getBytes()).build());

		writeCounts();

		// THEN
		then(jdbcStatement).should().setString(1, publishServiceName);
		then(jdbcStatement).should().setString(1, deliverServiceName);
		then(jdbcStatement).should(times(2)).execute();
	}

}
