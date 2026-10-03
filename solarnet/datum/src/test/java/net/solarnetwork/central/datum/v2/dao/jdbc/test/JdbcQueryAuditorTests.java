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
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.JdbcServiceAuditorCount;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcQueryAuditor;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcQueryAuditor} class.
 *
 * <p>
 * The writing behavior comes from the base auditor class, and is tested with
 * it; these tests cover how query results are counted and written.
 * </p>
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
	private static final Long TEST_NODE_ID_2 = -2L;
	private static final String TEST_SOURCE_1 = "test.source.1";
	private static final String TEST_SOURCE_2 = "test.source.2";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;
	private Clock testClock;
	private StatTracker stats;
	private JdbcQueryAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		stats = new StatTracker("QueryAuditor", "", log, 20);
		auditor = new JdbcQueryAuditor(testClock, dataSource, datumCountMap, stats);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	@AfterEach
	public void teardown() {
		// stop any writer a test left running
		auditor.disableWriting();
		auditor.resetCurrentAuditResults();
	}

	/**
	 * Wait for the writer to start its first flush, so it writes the counts
	 * added before it started.
	 */
	private void awaitFirstFlush() throws InterruptedException {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ( stats.get(JdbcServiceAuditorCount.CountsFlushed) < 1
				&& System.nanoTime() < end ) {
			Thread.sleep(10);
		}
	}

	private void givenWriterConnection() throws Exception {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);
	}

	private static GeneralNodeDatumMatch match(Long nodeId, String sourceId) {
		return new GeneralNodeDatumMatch(new GeneralNodeDatumPK(nodeId, Instant.now(), sourceId));
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
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).shouldHaveNoInteractions();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("No count added")
			.isEmpty()
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
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 3);
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
	public void datumFilterResults_manyNodesAndSources() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		filter.setSourceIds(new String[] { TEST_SOURCE_1, TEST_SOURCE_2 });

		List<GeneralNodeDatumFilterMatch> matches = List.of(match(TEST_NODE_ID, TEST_SOURCE_1),
				match(TEST_NODE_ID, TEST_SOURCE_1), match(TEST_NODE_ID, TEST_SOURCE_2),
				match(TEST_NODE_ID_2, TEST_SOURCE_1));
		BasicFilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results = new BasicFilterResults<>(
				matches, 4L, 0L, 4);

		// WHEN
		auditor.auditNodeDatumFilterResults(filter, results);

		// THEN
		final Instant auditDate = testClock.instant();
		// @formatter:off
		and.then(datumCountMap)
			.as("Results counted per node and source, at the audit date")
			.hasSize(3)
			.hasEntrySatisfying(DatumId.nodeId(TEST_NODE_ID, TEST_SOURCE_1, auditDate),
					c -> and.then(c).hasValue(2))
			.hasEntrySatisfying(DatumId.nodeId(TEST_NODE_ID, TEST_SOURCE_2, auditDate),
					c -> and.then(c).hasValue(1))
			.hasEntrySatisfying(DatumId.nodeId(TEST_NODE_ID_2, TEST_SOURCE_1, auditDate),
					c -> and.then(c).hasValue(1))
			;
		and.then(auditor.currentAuditResults())
			.as("Current results counted per node and source, at the audit date")
			.containsOnly(
					Map.entry(new GeneralNodeDatumPK(TEST_NODE_ID, auditDate, TEST_SOURCE_1), 2),
					Map.entry(new GeneralNodeDatumPK(TEST_NODE_ID, auditDate, TEST_SOURCE_2), 1),
					Map.entry(new GeneralNodeDatumPK(TEST_NODE_ID_2, auditDate, TEST_SOURCE_1), 1))
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
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 1);
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
	public void addNodeDatumAuditResults_sameCounterAsAuditedDatum() {
		// GIVEN
		GeneralDatum datum = GeneralDatum.nodeDatum(TEST_NODE_ID, TEST_SOURCE_1, Instant.now(),
				new DatumSamples());

		// WHEN
		auditor.auditNodeDatum(datum);
		auditor.addNodeDatumAuditResults(
				Map.of(new GeneralNodeDatumPK(TEST_NODE_ID, testClock.instant(), TEST_SOURCE_1), 2));

		// THEN
		// @formatter:off
		and.then(datumCountMap)
			.as("Added results counted with the audited datum, as they have the same key")
			.hasSize(1)
			.hasEntrySatisfying(DatumId.nodeId(TEST_NODE_ID, TEST_SOURCE_1, testClock.instant()),
					c -> and.then(c).hasValue(3))
			;
		// @formatter:on
	}

}
