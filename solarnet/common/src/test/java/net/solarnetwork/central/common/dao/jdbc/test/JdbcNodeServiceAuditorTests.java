/* ==================================================================
 * JdbcNodeServiceAuditorTests.java - 21/01/2023 6:15:01 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcNodeServiceAuditor} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcNodeServiceAuditorTests {

	private static final Logger log = LoggerFactory.getLogger(JdbcNodeServiceAuditorTests.class);

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SERVICE_ID = "test";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;
	private Clock testClock;
	private JdbcNodeServiceAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		auditor = new JdbcNodeServiceAuditor(dataSource, datumCountMap, testClock,
				new StatTracker("NodeServiceAuditor", "", log, 20));
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	private void stopAuditingAndWaitForFlush() throws InterruptedException {
		auditor.disableWriting();
		Thread.sleep(FLUSH_DELAY * 2);
	}

	private int countFor(Long nodeId, String service) {
		final AtomicInteger counter = datumCountMap
				.get(DatumId.nodeId(nodeId, service, testClock.instant()));
		return (counter != null ? counter.get() : 0);
	}

	@Test
	public void auditNodeService_one() throws Exception {
		// GIVEN
		final int count = 123;

		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcNodeServiceAuditor.DEFAULT_NODE_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, count);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SERVICE_ID);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, count);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(countFor(TEST_NODE_ID, TEST_SERVICE_ID))
			.as("Count flushed")
			.isZero()
			;
		// @formatter:on
	}

}
