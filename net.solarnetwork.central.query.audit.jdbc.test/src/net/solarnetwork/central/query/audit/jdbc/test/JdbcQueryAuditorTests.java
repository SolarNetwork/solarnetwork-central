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

package net.solarnetwork.central.query.audit.jdbc.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.query.audit.dao.JdbcQueryAuditor;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link JdbcQueryAuditor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcQueryAuditorTests extends AbstractCentralTest {

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final String TEST_SOURCE_1 = "test.source.1";

	private ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> datumCountMap;
	private DataSource dataSource;
	private Connection jdbcConnection;
	private CallableStatement jdbcStatement;

	private DateTime now;
	private DateTime topOfHour;
	private JdbcQueryAuditor auditor;

	@Before
	public void setup() {
		dataSource = EasyMock.createMock(DataSource.class);
		jdbcConnection = EasyMock.createMock(Connection.class);
		jdbcStatement = EasyMock.createMock(CallableStatement.class);
		datumCountMap = new ConcurrentHashMap<>(8);
		auditor = new JdbcQueryAuditor(new StaticOptionalService<>(dataSource), datumCountMap);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);

		now = new DateTime();
		DateTimeUtils.setCurrentMillisFixed(now.getMillis());
		topOfHour = now.withTime(now.hourOfDay().roundFloorCopy().getHourOfDay(), 0, 0, 0);

	}

	private void replayAll() {
		EasyMock.replay(dataSource, jdbcConnection, jdbcStatement);
	}

	@After
	public void teardown() {
		EasyMock.verify(dataSource, jdbcConnection, jdbcStatement);
		DateTimeUtils.setCurrentMillisSystem();
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
		sleep(Math.round(FLUSH_DELAY * 2));
	}

	private static GeneralNodeDatumPK nodeDatumKey(DateTime date, Long nodeId, String sourceId) {
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK();
		pk.setCreated(date);
		pk.setNodeId(nodeId);
		pk.setSourceId(sourceId);
		return pk;
	}

	@Test
	public void datumFilterResultsOneNodeAndSourceNoResults() throws Exception {
		// given
		expect(dataSource.getConnection()).andReturn(jdbcConnection);

		jdbcConnection.setAutoCommit(true);
		expectLastCall().anyTimes();

		expect(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.andReturn(jdbcStatement);

		jdbcConnection.close();

		// when
		replayAll();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch> results = new BasicFilterResults<>(matches, 0L,
				0, 0);
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// then
		assertMapValueZeroOrMissing(datumCountMap, nodeDatumKey(topOfHour, TEST_NODE_ID, TEST_SOURCE_1));
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
	public void datumFilterResultsOneNodeAndSourceSomeResults() throws Exception {
		// given
		expect(dataSource.getConnection()).andReturn(jdbcConnection);

		jdbcConnection.setAutoCommit(true);
		expectLastCall().anyTimes();

		expect(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.andReturn(jdbcStatement);

		jdbcStatement.setObject(1, TEST_NODE_ID);
		jdbcStatement.setString(2, TEST_SOURCE_1);
		jdbcStatement.setTimestamp(3, new Timestamp(topOfHour.getMillis()));
		jdbcStatement.setInt(4, 3);
		expect(jdbcStatement.execute()).andReturn(false);

		jdbcConnection.close();

		// when
		replayAll();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch> results = new BasicFilterResults<>(matches, 5L,
				0, 3);
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		stopAuditingAndWaitForFlush();

		// then
		//assertMapValueZeroOrMissing(datumCountMap, nodeDatumKey(topOfHour, TEST_NODE_ID, TEST_SOURCE_1));
	}

}
