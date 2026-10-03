/* ==================================================================
 * JdbcQueryAuditorTests.java - 15/02/2018 9:28:21 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

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
import java.util.ArrayList;
import java.util.List;
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
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcQueryAuditor;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcQueryAuditor} class.
 *
 * @author matt
 * @version 2.4
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcQueryAuditorTests {

	private static final Logger log = LoggerFactory.getLogger(JdbcQueryAuditorTests.class);

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_1 = "test.source.1";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> datumCountMap;
	private Clock testClock;
	private JdbcQueryAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		auditor = new JdbcQueryAuditor(testClock, dataSource, datumCountMap,
				new StatTracker("QueryAuditor", "", log, 20));
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	private void stopAuditingAndWaitForFlush() throws InterruptedException {
		auditor.disableWriting();
		Thread.sleep(FLUSH_DELAY * 2);
	}

	private int countFor(Long nodeId, String sourceId) {
		final AtomicInteger counter = datumCountMap
				.get(new GeneralNodeDatumPK(nodeId, testClock.instant(), sourceId));
		return (counter != null ? counter.get() : 0);
	}

	private void givenWriterConnection() throws Exception {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);
	}

	@Test
	public void datumFilterResultsOneNodeAndSourceNoResults() throws Exception {
		// GIVEN
		givenWriterConnection();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results = new BasicFilterResults<>(
				matches, 0L, 0L, 0);

		// WHEN
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		then(jdbcStatement).shouldHaveNoInteractions();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(countFor(TEST_NODE_ID, TEST_SOURCE_1))
			.as("No count added")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void datumFilterResultsOneNodeAndSourceSomeResults() throws Exception {
		// GIVEN
		givenWriterConnection();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results = new BasicFilterResults<>(
				matches, 5L, 0L, 3);

		// WHEN
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 3);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(countFor(TEST_NODE_ID, TEST_SOURCE_1))
			.as("Count flushed")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void auditSingleDatum() throws Exception {
		// GIVEN
		givenWriterConnection();

		GeneralDatum datum = GeneralDatum.nodeDatum(TEST_NODE_ID, TEST_SOURCE_1, Instant.now(),
				new DatumSamples());

		// WHEN
		auditor.auditNodeDatum(datum);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 1);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(countFor(TEST_NODE_ID, TEST_SOURCE_1))
			.as("Count flushed")
			.isZero()
			;
		// @formatter:on
	}

}
