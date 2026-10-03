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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
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
		and.then(datumCountMap)
			.as("Counter removed once flushed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditNodeService_concurrentAddAndFlush_noLostCounts() throws Exception {
		// GIVEN
		final int adderCount = 4;
		final int nodeCount = 256;
		final long runNanos = TimeUnit.SECONDS.toNanos(3);

		// stub-only statement that tallies the flushed counts, to avoid recording every invocation
		final CallableStatement stmt = mock(CallableStatement.class, withSettings().stubOnly());
		final AtomicInteger stmtCount = new AtomicInteger();
		final AtomicLong flushed = new AtomicLong();
		willAnswer(inv -> {
			stmtCount.set(inv.getArgument(1));
			return null;
		}).given(stmt).setInt(eq(4), anyInt());
		given(stmt.execute()).willAnswer(_ -> {
			flushed.addAndGet(stmtCount.get());
			return false;
		});

		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcNodeServiceAuditor.DEFAULT_NODE_SERVICE_INCREMENT_SQL))
				.willReturn(stmt);

		// flush continuously, so the writer keeps removing the counters being added to
		auditor.setFlushDelay(0);

		// do not log statistics for every few of the many counts added
		auditor.setStatLogUpdateCount(Integer.MAX_VALUE);

		// WHEN
		auditor.enableWriting();

		final AtomicLong added = new AtomicLong();
		final long start = System.nanoTime();
		final Thread[] adders = new Thread[adderCount];
		for ( int i = 0; i < adderCount; i++ ) {
			adders[i] = new Thread(() -> {
				final ThreadLocalRandom rnd = ThreadLocalRandom.current();
				while ( System.nanoTime() - start < runNanos ) {
					auditor.auditNodeService((long) rnd.nextInt(nodeCount), TEST_SERVICE_ID, 1);
					added.incrementAndGet();
					// pause, so most counters hold nothing when the writer reaches them
					LockSupport.parkNanos(rnd.nextInt(20_000));
				}
			}, "Adder-" + i);
			adders[i].start();
		}
		for ( Thread adder : adders ) {
			adder.join();
		}

		// wait for the writer to flush everything added
		final long drainStart = System.nanoTime();
		while ( flushed.get() < added.get()
				&& System.nanoTime() - drainStart < TimeUnit.SECONDS.toNanos(5) ) {
			Thread.sleep(50);
		}
		auditor.disableWriting();

		// THEN
		// @formatter:off
		and.then(flushed.get())
			.as("Every count added is flushed")
			.isEqualTo(added.get())
			;
		and.then(datumCountMap)
			.as("Counters removed once flushed")
			.isEmpty()
			;
		// @formatter:on
	}

}
