/* ==================================================================
 * StaleDatumStreamProcessorTests.java - 23/11/2020 6:57:16 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static java.util.Collections.singletonList;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.datum.agg.StaleDatumStreamProcessor;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.datum.domain.DatumAppEvent;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId.NodeDatumId;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the [@link StaleDatumStreamProcessor} class.
 * 
 * @author matt
 * @version 2.0
 */
public class StaleDatumStreamProcessorTests {

	private static final String TEST_JOB_ID = "Test Stale Datum Stream Processor";

	private static final Long TEST_NODE_ID = 1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private static final class TestProcessor extends StaleDatumStreamProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);
		private final ThreadPoolTaskExecutor executor;

		private TestProcessor(JdbcOperations jdbcOps) {
			super(jdbcOps);
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(10);
			executor.setAllowCoreThreadTimeOut(true);
			executor.setThreadFactory(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleDatumStreamProcessorTask-" + taskThreadCount.incrementAndGet());
				}
			});
			executor.initialize();
			setParallelTaskExecutor(executor);
		}

	}

	private JdbcOperations jdbcTemplate;
	private TestProcessor job;
	private List<Object> otherMocks;

	@Before
	public void setup() {
		jdbcTemplate = EasyMock.createMock(JdbcOperations.class);

		job = new TestProcessor(jdbcTemplate);
		job.setGroupId("Test");
		job.setId(TEST_JOB_ID);
		job.setMaximumIterations(10);
		job.setAggregateProcessType("h");

		otherMocks = new ArrayList<>();
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(jdbcTemplate);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
			for ( Object m : mocks ) {
				otherMocks.add(m);
			}
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(jdbcTemplate);
		if ( !otherMocks.isEmpty() ) {
			EasyMock.verify(otherMocks.toArray(new Object[otherMocks.size()]));
		}
	}

	@Test
	public void runSingleTask() throws Exception {
		// GIVEN
		Connection con = EasyMock.createMock(Connection.class);
		CallableStatement stmt = EasyMock.createMock(CallableStatement.class);

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

		con.setAutoCommit(true);
		expectLastCall().anyTimes();

		// execute call & indicate a ResultSet is available
		expect(con.prepareCall(StaleDatumStreamProcessor.DEFAULT_SQL)).andReturn(stmt);
		stmt.setString(1, Aggregation.Hour.getKey());
		expect(stmt.execute()).andReturn(true).andReturn(true);

		// give one result row back first time, none second
		ResultSet resultSet1 = EasyMock.createMock(ResultSet.class);
		expect(stmt.getResultSet()).andReturn(resultSet1);
		expect(resultSet1.next()).andReturn(true);
		resultSet1.close();

		ResultSet resultSet2 = EasyMock.createMock(ResultSet.class);
		expect(stmt.getResultSet()).andReturn(resultSet2);
		expect(resultSet2.next()).andReturn(false);
		resultSet2.close();

		stmt.close();

		// WHEN
		replayAll(con, stmt, resultSet1, resultSet2);
		job.run();

		// THEN
		assertThat("Thread count", job.taskThreadCount.get(), equalTo(0));
	}

	@Test
	public void runParallelTasks() throws Exception {
		// GIVEN
		final int parallelism = 3;
		List<Object> mocks = new ArrayList<>();
		job.setParallelism(parallelism);

		for ( int i = 0; i < parallelism; i++ ) {
			Connection con = EasyMock.createMock(Connection.class);
			mocks.add(con);
			con.setAutoCommit(true);
			expectLastCall().anyTimes();

			CallableStatement stmt = EasyMock.createMock(CallableStatement.class);
			mocks.add(stmt);

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

			// execute call & indicate a ResultSet is available
			expect(con.prepareCall(StaleDatumStreamProcessor.DEFAULT_SQL)).andReturn(stmt);
			stmt.setString(1, Aggregation.Hour.getKey());
			expectLastCall();
			expect(stmt.execute()).andReturn(true).andReturn(true);

			// give one result row back first time, none second
			ResultSet resultSet1 = EasyMock.createMock(ResultSet.class);
			mocks.add(resultSet1);
			expect(stmt.getResultSet()).andReturn(resultSet1);
			expect(resultSet1.next()).andReturn(true);
			resultSet1.close();

			// give one result row back first time, none second
			ResultSet resultSet2 = EasyMock.createMock(ResultSet.class);
			mocks.add(resultSet2);
			expect(stmt.getResultSet()).andReturn(resultSet2);
			expect(resultSet2.next()).andReturn(false);
			resultSet2.close();

			stmt.close();
		}

		// WHEN
		replayAll(mocks.toArray(new Object[mocks.size()]));
		job.run();

		// THEN
		assertThat("Thread count", job.taskThreadCount.get(), equalTo(parallelism));
	}

	@Test
	public void emaitDatumAppEvent() throws Exception {
		// GIVEN
		DatumAppEventAcceptor acceptor = EasyMock.createMock(DatumAppEventAcceptor.class);
		job.setDatumAppEventAcceptors(singletonList(acceptor));

		Connection con = EasyMock.createMock(Connection.class);
		CallableStatement stmt = EasyMock.createMock(CallableStatement.class);

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

		con.setAutoCommit(true);
		expectLastCall().anyTimes();

		// execute call & indicate a ResultSet is available
		expect(con.prepareCall(StaleDatumStreamProcessor.DEFAULT_SQL)).andReturn(stmt);
		stmt.setString(1, Aggregation.Hour.getKey());
		expect(stmt.execute()).andReturn(true).andReturn(true);

		// give one result row back first time, none second
		ResultSet resultSet1 = EasyMock.createMock(ResultSet.class);
		expect(stmt.getResultSet()).andReturn(resultSet1);
		expect(resultSet1.next()).andReturn(true);

		// extract AppEvent data from ResultSet
		final NodeDatumId rowId = new NodeDatumId(UUID.randomUUID(), TEST_NODE_ID, TEST_SOURCE_ID,
				ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour);

		expect(resultSet1.getObject(1)).andReturn(rowId.getStreamId());
		expect(resultSet1.getTimestamp(2)).andReturn(Timestamp.from(rowId.getTimestamp()));
		expect(resultSet1.getString(3)).andReturn(rowId.getAggregation().getKey());
		expect(resultSet1.getObject(4)).andReturn(rowId.getObjectId());
		expect(resultSet1.getString(5)).andReturn(rowId.getSourceId());
		expect(resultSet1.getString(6)).andReturn(Character.toString(rowId.getKind().getKey()));
		resultSet1.close();

		ResultSet resultSet2 = EasyMock.createMock(ResultSet.class);
		expect(stmt.getResultSet()).andReturn(resultSet2);
		expect(resultSet2.next()).andReturn(false);
		resultSet2.close();

		stmt.close();

		Capture<DatumAppEvent> eventCaptor = new Capture<>();
		acceptor.offerDatumEvent(capture(eventCaptor));

		// WHEN
		replayAll(acceptor, con, stmt, resultSet1, resultSet2);
		job.run();
		job.executor.setAwaitTerminationSeconds(10);
		job.executor.shutdown();

		// THEN
		ZonedDateTime date = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		DatumAppEvent event = eventCaptor.getValue();
		assertThat("DatumAppEvent published", event, notNullValue());
		assertThat("DatumAppEvent node ID matches", event.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("DatumAppEvent source ID matches", event.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("DatumAppEvent properties available", event.getEventProperties(), notNullValue());
		assertThat("DatumAppEvent prop agg key", event.getEventProperties(),
				hasEntry("aggregationKey", Aggregation.Hour.getKey()));
		assertThat("DatumAppEvent prop agg timestamp", event.getEventProperties(),
				hasEntry("timestamp", date.toInstant().toEpochMilli()));

		AggregateUpdatedEventInfo info = getObjectFromJSON(
				getJSONString(event.getEventProperties(), null), AggregateUpdatedEventInfo.class);
		assertThat("AggregateUpdatedEventInfo info available", info, notNullValue());
		assertThat("Info aggregation", info.getAggregation(), equalTo(Aggregation.Hour));
		assertThat("Info aggregation", info.getTimeStart(), equalTo(date.toInstant()));
	}

}
