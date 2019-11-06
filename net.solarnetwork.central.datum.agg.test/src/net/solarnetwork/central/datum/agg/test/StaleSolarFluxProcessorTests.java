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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import net.solarnetwork.central.datum.agg.AggregateDatumProcessor;
import net.solarnetwork.central.datum.agg.AggregateSupportDao;
import net.solarnetwork.central.datum.agg.StaleSolarFluxProcessor;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link StaleSolarFluxProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleSolarFluxProcessorTests extends AggTestSupport {

	private static final String TEST_JOB_ID = "Test Stale SolarFlux Datum Processor";

	private static final String TEST_SOURCE_ID = "test.source";

	private static final Long TEST_USER_ID = -9L;

	private static final String SQL_INSERT_STALE_AGG_FLUX = "INSERT INTO solaragg.agg_stale_flux(agg_kind, node_id, source_id) VALUES (?, ?, ?)";

	private GeneralNodeDatumDao datumDao;
	private AggregateDatumProcessor processor;
	private AggregateSupportDao supportDao;
	private TestStaleSolarFluxDatumProcessor job;

	private static final class TestStaleSolarFluxDatumProcessor extends StaleSolarFluxProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleSolarFluxDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
				GeneralNodeDatumDao datumDao, OptionalService<AggregateDatumProcessor> processor,
				AggregateSupportDao supportDao) {
			super(eventAdmin, jdbcOps, datumDao, processor, supportDao);
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

	@Override
	@Before
	public void setup() {
		super.setup();

		datumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		processor = EasyMock.createMock(AggregateDatumProcessor.class);
		supportDao = EasyMock.createMock(AggregateSupportDao.class);

		job = new TestStaleSolarFluxDatumProcessor(null, jdbcTemplate, datumDao,
				new StaticOptionalService<>(processor), supportDao);
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumRowCount(10);
		job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();
	}

	private void replayAll() {
		EasyMock.replay(datumDao, processor, supportDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, processor, supportDao);
	}

	private void insertStaleAggFlux(Aggregation aggKind, long nodeId, String sourceId) {
		jdbcTemplate.execute(SQL_INSERT_STALE_AGG_FLUX, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement stmt)
					throws SQLException, DataAccessException {
				stmt.setString(1, aggKind.getKey());
				stmt.setLong(2, nodeId);
				stmt.setString(3, sourceId);
				stmt.executeUpdate();
				return null;
			}
		});
	}

	@Test
	public void processSingleHourRow_singleTask() throws Exception {
		// GIVEN
		insertStaleAggFlux(Aggregation.Hour, TEST_NODE_ID, TEST_SOURCE_ID);

		ReportingGeneralNodeDatum mostRecentDatum = new ReportingGeneralNodeDatum();
		List<ReportingGeneralNodeDatumMatch> datumResults = Arrays.asList(mostRecentDatum);
		BasicFilterResults<ReportingGeneralNodeDatumMatch> filterResults = new BasicFilterResults<>(
				datumResults);
		Capture<AggregateGeneralNodeDatumFilter> filterCaptor = new Capture<>();
		expect(datumDao.findAggregationFiltered(capture(filterCaptor), isNull(), isNull(), isNull()))
				.andReturn(filterResults);

		expect(supportDao.userIdForNodeId(TEST_NODE_ID)).andReturn(TEST_USER_ID);

		expect(processor.processStaleAggregateDatum(TEST_USER_ID, Aggregation.Hour, mostRecentDatum))
				.andReturn(true);

		// WHEN
		replayAll();
		boolean executed = job.executeJob();

		// THEN
		assertThat("Job executed", executed, equalTo(true));
		assertThat("Job executed in one thread", job.taskThreadCount.get(), equalTo(0));

		AggregateGeneralNodeDatumFilter filter = filterCaptor.getValue();
		assertThat("Filter for most recent datum", filter.isMostRecent(), equalTo(true));
		assertThat("Filter aggregation matched stale row value", filter.getAggregation(),
				equalTo(Aggregation.Hour));
		assertThat("Filter node ID matched stale row value", filter.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Filter source ID matched stale row value", filter.getSourceId(),
				equalTo(TEST_SOURCE_ID));
	}

}
