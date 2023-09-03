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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuditService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link JdbcAuditService} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
public class JdbcAuditServiceTests extends TestSupport {

  private static final long FLUSH_DELAY = 300;
  private static final long UPDATE_DELAY = 0;
  private static final long RECONNECT_DELAY = 300;

  private static final Long TEST_NODE_1 = 1L;
  private static final String TEST_SOURCE_1 = "test.source.1";

  private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;

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
    now = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    topOfHour = Instant.now(now).truncatedTo(ChronoUnit.HOURS);
    datumCountMap = new ConcurrentHashMap<>(8);

    auditor = new JdbcAuditService(dataSource, datumCountMap, now);
    auditor.setFlushDelay(FLUSH_DELAY);
    auditor.setUpdateDelay(UPDATE_DELAY);
    auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  private void stopAuditingAndWaitForFlush() {
    auditor.disableWriting();
    sleep(Math.round(FLUSH_DELAY * 2));
  }

  private static DatumId nodeDatumKey(Instant date, Long nodeId, String sourceId) {
    return DatumId.nodeId(nodeId, sourceId, date);
  }

  private static String topicForNodeSource(Long nodeId, String sourceId) {
    return String.format("node/%d/0/%s", nodeId, sourceId);
  }

  private <K> void assertMapValueZeroOrMissing(Map<K, AtomicInteger> countMap, K key) {
    AtomicInteger l = countMap.get(key);
    if (l != null) {
      assertThat("Count for " + key, l.get(), equalTo(0));
    } else {
      assertThat("Count for " + key, l, nullValue());
    }
  }

  private void verifyStatement(Long nodeId, String sourceId, long ts, int count)
      throws SQLException {
    verify(jdbcStatement).setString(1, JdbcAuditService.DEFAULT_AUDIT_MQTT_SERVICE_NAME);
    verify(jdbcStatement).setObject(2, nodeId);
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
    assertMapValueZeroOrMissing(datumCountMap, nodeDatumKey(topOfHour, TEST_NODE_1, TEST_SOURCE_1));
  }

}
