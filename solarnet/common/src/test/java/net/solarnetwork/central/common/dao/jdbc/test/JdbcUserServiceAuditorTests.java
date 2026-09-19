/* ==================================================================
 * JdbcUserServiceAuditorTests.java - 29/05/2024 4:46:57 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.JdbcUserServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcUserServiceAuditor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserServiceAuditorTests {

	private static final Logger log = LoggerFactory.getLogger(JdbcUserServiceAuditorTests.class);

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_SERVICE_ID = "test";

	private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;
	private DataSource dataSource;
	private Connection jdbcConnection;
	private CallableStatement jdbcStatement;

	private Clock testClock;

	private JdbcUserServiceAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		dataSource = EasyMock.createMock(DataSource.class);
		jdbcConnection = EasyMock.createMock(Connection.class);
		jdbcStatement = EasyMock.createMock(CallableStatement.class);
		datumCountMap = new ConcurrentHashMap<>(8);
		auditor = new JdbcUserServiceAuditor(dataSource, datumCountMap, testClock,
				new StatTracker("UserServiceAuditor", "", log, 20));
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	private void replayAll() {
		EasyMock.replay(dataSource, jdbcConnection, jdbcStatement);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(dataSource, jdbcConnection, jdbcStatement);
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch ( InterruptedException e ) {
			// ignore
		}
	}

	private void stopAuditingAndWaitForFlush() {
		auditor.disableWriting();
		sleep(FLUSH_DELAY * 2);
	}

	private static DatumId nodeDatumKey(Instant date, Long nodeId, String sourceId) {
		return DatumId.nodeId(nodeId, sourceId, date);
	}

	private <K> void assertMapValueZeroOrMissing(Map<K, AtomicInteger> countMap, K key) {
		AtomicInteger l = countMap.get(key);
		if ( l != null ) {
			assertThat("Count for " + key, l.get(), equalTo(0));
		} else {
			assertThat("Count for " + key, l, nullValue());
		}
	}

	@Test
	public void auditUserService_one() throws Exception {
		// GIVEN
		final int count = 123;

		expect(dataSource.getConnection()).andReturn(jdbcConnection);

		jdbcConnection.setAutoCommit(true);
		expectLastCall().anyTimes();

		expect(jdbcConnection.prepareCall(JdbcUserServiceAuditor.DEFAULT_USER_SERVICE_INCREMENT_SQL))
				.andReturn(jdbcStatement);

		jdbcStatement.setObject(1, TEST_USER_ID);
		jdbcStatement.setString(2, TEST_SERVICE_ID);
		jdbcStatement.setTimestamp(3, Timestamp.from(Instant.now(testClock)));
		jdbcStatement.setInt(4, count);
		expect(jdbcStatement.execute()).andReturn(false);

		jdbcConnection.close();

		// WHEN
		replayAll();

		auditor.auditUserService(TEST_USER_ID, TEST_SERVICE_ID, count);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		assertMapValueZeroOrMissing(datumCountMap,
				nodeDatumKey(Instant.now(testClock), TEST_USER_ID, TEST_SERVICE_ID));
	}
}
