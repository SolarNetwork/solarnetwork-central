/* ==================================================================
 * StaleSolarFluxProcessorTests.java - 1/11/2019 4:18:33 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg.test;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.agg.StaleSolarFluxProcessor;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleFluxDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.Assertion;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link StaleSolarFluxProcessor} class.
 * 
 * @author matt
 * @version 2.0
 */
public class StaleSolarFluxProcessorTests {

	private static final String TEST_JOB_ID = "Test Stale SolarFlux Datum Processor";

	private static final Long TEST_NODE_ID = 1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private JdbcOperations jdbcTemplate;
	private DatumEntityDao datumDao;
	private DatumProcessor processor;
	private TestStaleSolarFluxDatumProcessor job;
	private List<Object> otherMocks;

	private static final class TestStaleSolarFluxDatumProcessor extends StaleSolarFluxProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleSolarFluxDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
				DatumEntityDao datumDao, OptionalService<DatumProcessor> processor) {
			super(eventAdmin, jdbcOps, datumDao, processor);
			setExecutorService(Executors.newCachedThreadPool(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "StaleSolarFluxDatumTask-" + taskThreadCount.incrementAndGet());
				}
			}));
		}

		/**
		 * Provide way to call {@code handleJob} directly in test cases.
		 * 
		 * @return {@code true} if job completed successfully
		 * @throws Exception
		 *         if any error occurs
		 */
		private boolean executeJob() throws Exception {
			Event jobEvent = new Event(SchedulerConstants.TOPIC_JOB_REQUEST,
					Collections.singletonMap(SchedulerConstants.JOB_ID, TEST_JOB_ID));
			return handleJob(jobEvent);
		}

	}

	@Before
	public void setup() {
		jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		processor = EasyMock.createMock(DatumProcessor.class);

		job = new TestStaleSolarFluxDatumProcessor(null, jdbcTemplate, datumDao,
				new StaticOptionalService<>(processor));
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumIterations(10);
		job.setMaximumWaitMs(15 * 1000L);

		otherMocks = new ArrayList<>();
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(jdbcTemplate, datumDao, processor);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
			for ( Object m : mocks ) {
				otherMocks.add(m);
			}
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(jdbcTemplate, datumDao, processor);
		if ( !otherMocks.isEmpty() ) {
			EasyMock.verify(otherMocks.toArray(new Object[otherMocks.size()]));
		}
	}

	@Test
	public void runSingleTask() throws Exception {
		// GIVEN
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		int[] cbResult = new int[] { -1 };
		expect(jdbcTemplate.execute(assertWith(new Assertion<ConnectionCallback<Integer>>() {

			@Override
			public void check(ConnectionCallback<Integer> cb) throws Throwable {
				Integer res = cb.doInConnection(con);
				if ( res != null ) {
					cbResult[0] = res.intValue();
				}
			}

		}))).andAnswer(new IAnswer<Integer>() {

			@Override
			public Integer answer() throws Throwable {
				return cbResult[0];
			}
		});

		con.setAutoCommit(false);
		expectLastCall().anyTimes();

		// execute call & indicate a ResultSet is available (twice)
		expect(con.prepareStatement(SelectStaleFluxDatum.ANY_ONE_FOR_UPDATE.getSql(),
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(stmt);

		// give one result row back first time, none second
		ResultSet resultSet1 = EasyMock.createMock(ResultSet.class);
		expect(stmt.executeQuery()).andReturn(resultSet1);
		expect(resultSet1.next()).andReturn(true);

		DatumProperties props = DatumProperties.propertiesOf(decimalArray("1.1"),
				NumberUtils.decimalArray("2.1"), null, null);
		DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(
				new BigDecimal[][] { decimalArray("60", "1.0", "1.2") },
				new BigDecimal[][] { decimalArray("2.1", "0", "2.1") });
		UUID streamId = UUID.randomUUID();
		AggregateDatumEntity mostRecentDatum = new AggregateDatumEntity(streamId,
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, props, stats);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "Pacific/Auckland",
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a" },
				new String[] { "b" }, null);

		expect(resultSet1.getObject(1)).andReturn(streamId);
		expect(resultSet1.getString(2)).andReturn(Aggregation.Hour.getKey());

		resultSet1.deleteRow();
		expect(resultSet1.next()).andReturn(false);
		resultSet1.close();
		con.commit();

		ResultSet resultSet2 = EasyMock.createMock(ResultSet.class);
		expect(stmt.executeQuery()).andReturn(resultSet2);
		expect(resultSet2.next()).andReturn(false);
		resultSet2.close();
		con.commit();

		// GIVEN
		List<Datum> datumResults = Collections.singletonList(mostRecentDatum);
		ObjectDatumStreamFilterResults<Datum, DatumPK> filterResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(streamId, meta), datumResults);
		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(filterResults);

		expect(processor.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> pubDatumCaptor = new Capture<>();
		expect(processor.processDatum(capture(pubDatumCaptor), eq(Aggregation.Hour))).andReturn(true);

		stmt.close();

		// WHEN
		replayAll(con, stmt, resultSet1, resultSet2);
		boolean executed = job.executeJob();

		// THEN
		assertThat("Job executed", executed, equalTo(true));
		assertThat("Job executed in one thread", job.taskThreadCount.get(), equalTo(0));

		DatumCriteria filter = filterCaptor.getValue();
		assertThat("Filter for most recent datum", filter.isMostRecent(), equalTo(true));
		assertThat("Filter aggregation matched stale row value", filter.getAggregation(),
				equalTo(Aggregation.Hour));
		assertThat("Filter stream ID matched stale row value", filter.getStreamId(), equalTo(streamId));

		assertThat("Published ReportingGeneralNodeDatum", pubDatumCaptor.getValue(),
				Matchers.instanceOf(ReportingGeneralNodeDatum.class));
		ReportingGeneralNodeDatum pubDatum = (ReportingGeneralNodeDatum) pubDatumCaptor.getValue();
		assertThat("Published node ID from stream meta", pubDatum.getNodeId(),
				equalTo(meta.getObjectId()));
		assertThat("Published source ID from stream meta", pubDatum.getSourceId(),
				equalTo(meta.getSourceId()));
		assertThat("Published timestamp from datum with meta time zone", pubDatum.getCreated(),
				equalTo(mostRecentDatum.getTimestamp()));
	}

}
